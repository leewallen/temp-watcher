import datetime
import requests
import statsd
from confluent_kafka import Producer
from confluent_kafka.schema_registry import SchemaRegistryClient
from confluent_kafka.schema_registry.avro import AvroSerializer
from confluent_kafka.serialization import StringSerializer, SerializationContext, MessageField
from uuid import uuid4


class SensorReading(object):
    """
    SensorReading record
    Args:
        name (str): Sensor name
        sensorId (int): Sensor id
        temperature (float) : Temperature in fahrenheit
        datetimeMs (long): DateTime in milliseconds from when the reading was taken
    """

    def __init__(self, name, sensorId, temperature, datetimeMs):
        self.name = name
        self.sensorId = sensorId
        self.temperature = temperature
        self.datetimeMs = datetimeMs


class MotionSensor(object):
    string_serializer = StringSerializer('utf_8')

    def __init__(self, hub_ip: str = None,
                 bridge_username: str = None,
                 interval: int = 15,
                 sensors: str = None,
                 telegraf_ip: str = None,
                 telegraf_port: str = None,
                 bootstrap_server: str = None,
                 target_topic: str = None,
                 schema_registry_url: str = None,
                 schema_str: str = None):
        self.last_temp = None
        self.hub_ip = hub_ip or None
        self.bridge_username = bridge_username or None
        self.boostrap_server = bootstrap_server or None
        self.target_topic = target_topic or None
        self.telegraf_ip = telegraf_ip or None
        self.telegraf_port = telegraf_port or None
        self.sensors = [int(x) for x in sensors.split(',')] or None
        self.schema_registry_url = schema_registry_url or None
        self.interval = interval
        self.schema_str = schema_str or None
        self.sensor_temps = {}

        self.validate_args()

        if not self.telegraf_ip:
            self.telegraf_ip = "localhost"

        if not self.telegraf_port:
            self.telegraf_port = "8125"

        for sensor in self.sensors:
            self.sensor_temps[sensor] = 0.0

        self.statsd_client = statsd.StatsClient(
            'localhost',
            8125,
            prefix='temp')

        schema_registry_conf = {'url': schema_registry_url}
        self.schema_registry_client = SchemaRegistryClient(schema_registry_conf)

        self.avro_serializer = AvroSerializer(
            self.schema_registry_client,
            self.schema_str,
            self.reading_to_dict)

        producer_conf = {'bootstrap.servers': self.boostrap_server}
        self.producer = Producer(producer_conf)

    def run(self):
        self.check_temp()

    def reading_to_dict(self, sensor_reading: SensorReading, ctx):
        """
        Returns a dict representation of a User instance for serialization.
        Args:
            sensor_reading (SensorReading): SensorReading instance.
            ctx (SerializationContext): Metadata pertaining to the serialization
                operation.
        Returns:
            dict: Dict populated with user attributes to be serialized.
        """
        return dict(name=sensor_reading.name,
                    sensorId=sensor_reading.sensorId,
                    temperature=sensor_reading.temperature,
                    datetimeMs=sensor_reading.datetimeMs)

    def check_temp(self):
        for sensor in self.sensors:
            response = requests.get(f'http://{self.hub_ip}/api/{self.bridge_username}/sensors/{sensor}', timeout=60)

            if response.status_code != 200:
                raise ValueError('Unable to get response from motion/temp sensor')

            sensor_state = response.json()
            if not sensor_state['state']['temperature']:
                return -99.99

            temperature = self.get_temp_from_int(sensor_state['state']['temperature'])

            if self.sensor_temps[sensor] != temperature:
                print(
                    f'Temp changed! Date: {datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")} Temp: {temperature:.2f}')
                self.send_metric(sensor, temperature)
                self.sensor_temps[sensor] = temperature
            else:
                print(f'Date: {datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")} Temp: {temperature:.2f}')
                self.send_metric(sensor, temperature)

    @staticmethod
    def get_temp_from_int(temp: int = 0):
        # (0°C × 9/5) + 32 = 32°F
        float_val = float(temp / 100.0)

        fahrenheit = (float_val * (9 / 5)) + 32.0
        return fahrenheit

    def delivery_report(self, err, msg):
        """
        Reports the failure or success of a message delivery.
        Args:
            err (KafkaError): The error that occurred on None on success.
            msg (Message): The message that was produced or failed.
        Note:
            In the delivery report callback the Message.key() and Message.value()
            will be the binary format as encoded by any configured Serializers and
            not the same object that was passed to produce().
            If you wish to pass the original object(s) for key and value to delivery
            report callback we recommend a bound callback or lambda where you pass
            the objects along.
        """

        if err is not None:
            print("Delivery failed for User record {}: {}".format(msg.key(), err))
            return
        print('User record {} successfully produced to {} [{}] at offset {}'.format(
            msg.key(), msg.topic(), msg.partition(), msg.offset()))

    def send_metric(self, sensor_id: int = 0, temp_value: float = 0.0):
        print(
            f'Date: {datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")} Send metric: sensor.reading,type={sensor_id} : {temp_value:.2f}')
        self.statsd_client.gauge(f'sensor.reading,host={sensor_id},type={sensor_id}', temp_value)

        datetime_ms = datetime.datetime.now().timestamp() * 1000
        sensor_reading = SensorReading(
            name=str(sensor_id),
            sensorId=sensor_id,
            temperature=temp_value,
            datetimeMs=datetime_ms)

        self.producer.produce(topic=self.target_topic,
                              key=self.string_serializer(str(uuid4())),
                              value=self.avro_serializer(
                                  sensor_reading,
                                  SerializationContext(
                                      self.target_topic,
                                      MessageField.VALUE)),
                              on_delivery=self.delivery_report)

    def validate_args(self):
        if not self.hub_ip:
            raise ValueError('The hub IP wasn''t provided.')

        if not self.bridge_username:
            raise ValueError('The bridgeusername wasn''t provided.')

        if not self.sensors:
            raise ValueError("Please provide the motion sensors to monitor.")

        if not self.boostrap_server:
            raise ValueError("Please provide the bootstrap servers for the Kafka cluster.")

        if not self.schema_registry_url:
            raise ValueError("Please provide the schema registry URL.")

        if not self.schema_str:
            raise ValueError("Please provide the schema string.")
