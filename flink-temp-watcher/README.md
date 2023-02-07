Flink Temp Watcher
===================

This Flink project is using the [Apache Flink Starter](https://github.com/aedenj/apache-flink-starter) project as the starting point made by [Aeden James](https://github.com/aedenj).

***

This project is using Apache Flink 1.16 and the [wurstmeister/kafka Kafka image](https://hub.docker.com/r/wurstmeister/kafka) to process temperature data from Philips Hue motion sensor.

***

## Up & Running

Let's first clone the repo and start the Kafka cluster.

```shell
git clone git@github.com:leewallen/tig-stack-with-temp-watcher.git ~/projects/temp-watcher
cd ~/projects/temp-watcher/flink-temp-watcher
./gradlew kafkaUp
```

Now you have a single node Kafka cluster with various admin tools to make life a little easier. See Aeden's [Kafka cluster repo](https://github.com/aedenj/kafka-cluster-starter) for its operating details.

The next thing you'll need to do is create a couple Kafka topics and register some Avro schemas in the schema repository used by the Flink app.

```shell
./gradlew createTopics
./gradlew registerSchemas
```

This will create two topics - `sensor-reading`, and `sensor-reading-avg`. The Avro schemas are currently identical, but the point is to show how to pull in the schema from the schema registry for both a source and target topic.

The sensor-reading topic will expect Avro messages using the follwoing schema:

```json
{
  "type": "record",
  "name": "SensorReading",
  "namespace": "my.house",
  "doc": "Avro schema for holding a temperature sensor reading.",
  "fields": [
    {
      "name": "name",
      "type": "string"
    },
    {
      "name": "sensorId",
      "type": "int"
    },
    {
      "name": "temperature",
      "type": "float"
    },
    {
      "name": "datetimeMs",
      "type": {
        "type": "long",
        "logicalType": "timestamp-millis"
      }
    }
  ]
}
```

## Running the Flink Application

You can either run the hue-temp-sensor application or use the [Avro Random Generator](https://github.com/confluentinc/avro-random-generator) to generate data to use with the Flink application.

### Running Hue Temp Sensor

If you decide to run the hue-temp-sensor python application, then you'll need to get a `username` from your Philips Hue bridge, and also have one or more Philips Hue motion sensors.

The hue-temp-sensor app is currently using telegraf to send metrics to influxdb, and also sending metrics to a Kafka topic named `sensor-reading`.

Here is a list of the arguments that the hue-temp-sensor app is expecting:

## Command Line Args

| Short Option |   Long Option    | Description                                                              |
|:------------:|:----------------:|:-------------------------------------------------------------------------|
|     `-b`     |    `--bridge`    | Bridge, or hub, IP address                                               |
|     `-u`     |   `--username`   | Username created for interacting with the Hue Bridge                     |
|     `-i`     |   `--interval`   | Interval in seconds to check motion sensor temperature value             |
|     `-s`     |   `--sensors`    | Comma separated list of Sensor IDs to monitor                            |
|     `-t`     |  `--telegrafip`  | IP address of telegraf instance                                          |
|    `-tp`     | `--telegrafport` | Port that telegraf is using                                              |
|    `-bs`     |   `--bootstrap`    | Bootstrap server for Kafka                                               |
|    `-tt`     |   `--targettopic`    | A target Kafka topic for sensor metrics                                  |
|    `-sru`    |   `--schemaregistry_url`    | A scheme registry URL where the sensor metrics Avro schema is registered |

Below is a command line with some example values. 

```bash
python3 src/main.py \
  -b 192.168.0.254 \
  -u fakebridgeusername1234 \
  -i 60 \
  -s 10 \
  -t localhost \
  -tp 8125 \
  -tt sensor-reading \
  -bs localhost:9092 \
  -sru http://localhost:8085
```

### Avro Random Generator

Clone the [Avro Random Generator](https://github.com/confluentinc/avro-random-generator) and compile it as a standalone app:

```bash
./gradlew standalone
```

You can provide a JSON file that describes your Avro schema, and includes rules for how to generate data for the fields.

Here is a command line for running the Avro Random Generator:

```bash
./arg -f ./sensor-reading.json \
  -j \
  -c \
  -i 100000 \
  -o ./path/to/flink-temp-watcher/scripts/test-json-sensor-readings.txt
```

The `-j` option will output the data as JSON, and the `-c` option says to output the data in a compact format (one line per record).

The `-i <number>` option is for specifying the number of iterations. In this case it will output 100000 records.

The `-o` option specifies the output location of the data.

Here is an example JSON file for generating data for the SensorReading and SensorReadingAvg schemas:

**sensor-reading.json**

```json
{
  "type": "record",
  "name": "sensor_reading",
  "fields": [
    {
      "name": "name",
      "type" : {
        "type": "string",
          "arg.properties": {
            "options": [
              "Garage"
            ]
          }
       }
    },
    {
      "name": "sensorId",
      "type" : {
        "type": "int",
          "arg.properties": {
            "options": [
              10
            ]
          }
       }
    },
    {
      "name": "temperature",
      "type": {
        "type": "float",
          "arg.properties": {
            "range": {
              "min": 40.00,
              "max": 90.00
            }
         }
      }
    },
    {
        "name": "datetimeMs",
        "type": {
            "type": "long",
            "logicalType": "timestamp-millis",
            "arg.properties": {
              "iteration": {
                "start": 1672605296000,
                "step": 5000
              }
            }
        }
    }]
}
```

You can send an example Avro message to the sensor-reading topic by running the `./scripts/start-kafka-producer.sh` script.

The script will run the following command:

```shell
cd ./scripts
cat test-json-sensor-readings.txt | \
  docker exec -i schema-registry kafka-avro-console-producer \
  --broker-list broker-1:19092 \
  --topic sensor-reading \
  --property "schema.registry.url=http://localhost:8085" \
  --property "value.schema.id=1"
```

## Running Flink Temp Watcher 

### Locally

For quick feedback it's easiest to run the job locally,

1. If you're using Intellij, use the usual methods.
1. On the command line run `./gradlew shadowJar run`

### Using the Job Cluster

Run `./gradlew shadowJar startJob`. This will run the job within a job cluster that is setup in `flink-job-cluster.yml`. That cluster will run against the Kafka cluster started earlier.

### Observing the Job in Action

After starting the job with one of the methods above, let's observe it reading an writing a message from one Kafak topic to another.

1. Start the job using one of the methods above.
1. In a new terminal send data to the source Kafka topic by running `./scripts/start-kafka-producer.sh sensor-reading`
1. To see the Avro schemas in the schema registry, navigate to the [schema registry UI](http://localhost:8000/).
1. Navigate to the [Kafdrop](http://localhost:8001/) and view the messages for both the `sensor-reading` and `sensor-reading-avg` topics.
1. Watch the Flink application operation metrics in the [Sensor Ops Grafana dashboard](http://localhost:3000)

