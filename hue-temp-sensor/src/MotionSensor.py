import threading
import requests
import datetime
import statsd


class MotionSensor(object):

    def __init__(self, hub_ip: str = None, username: str = None, interval: int = 15, sensors: str = None, telegrafip: str = None, telegrafport: str = None):
        self.last_temp = None
        self.hub_ip = hub_ip or None
        self.username = username or None
        
        if not hub_ip:
            raise ValueError('The hub IP wasn''t provided.')

        if not username:
            raise ValueError('The username wasn''t provided.')

        if not sensors:
            raise ValueError("Please provide the motion sensors to monitor.")
        else:
            sensor_list = [int(x) for x in sensors.split(',')]

        if telegrafip:
            self.telegrafip = telegrafip
        else:
            self.telegrafip = "localhost"

        if telegrafport:
            self.telegrafport = telegrafport
        else:
            self.telegrafport = "8125"

        self.sensors = sensor_list
        self.sensor_temps = {}
        for sensor in self.sensors:
            self.sensor_temps[sensor] = 0.0

        print(f'interval: {interval}')
        self.interval = interval

        self.statsd_client = statsd.StatsClient(
                'localhost',
                8125,
                prefix='temp')


    def run(self):
        self.check_temp()

    def check_temp(self):
        for sensor in self.sensors:
            response = requests.get(f'http://{self.hub_ip}/api/{self.username}/sensors/{sensor}', timeout=60)

            if response.status_code != 200:
                raise ValueError('Unable to get response from motion/temp sensor')

            sensor_state = response.json()
            if not sensor_state['state']['temperature']:
                return -99.99

            temperature = self.get_temp_from_int(sensor_state['state']['temperature'])

            if self.sensor_temps[sensor] != temperature:
                print(f'Temp changed! Date: {datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")} Temp: {temperature:.2f}')
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

    def send_metric(self, sensor_id: int = 0, temp_value: float = 0.0):
        print(f'Date: {datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")} Send metric: sensor.reading,type={sensor_id} : {temp_value:.2f}')
        self.statsd_client.gauge(f'sensor.reading,host={sensor_id},type={sensor_id}', temp_value)
