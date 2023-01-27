Flink Temp Watcher
===================

This Flink project is using the [Apache Flink Starter](https://github.com/aedenj/apache-flink-starter) project as the starting point made by [Aeden James](https://github.com/aedenj).

***

This project is using Apache Flink 1.16 and the [wurstmeister/kafka Kafka image](https://hub.docker.com/r/wurstmeister/kafka) to process temperature data from Philips Hue motion sensor.

***

## Up & Running

Let's first clone the repo and fire up our system,

```
git clone git@github.com:aedenj/apache-flink-starter.git ~/projects/apache-flink-starter
cd ~/projects/apache-flink-starter;./gradlew kafkaUp
```
Now you have a single node Kafka cluster with various admin tools to make life a little easier. See the [Kafka cluster repo](https://github.com/aedenj/kafka-cluster-starter) for its operating details.

## Running the App

The sample job in this repo will read from the `sensor-reaading` topic, aggregate the sensor data to get the average temperature over a 5 minute period, and write the aggregated data to the `sensor-reading-aggregated` topic.

First, let's setup the kafka topics. Run `./gradlew createTopics`. 

This will create the two topics:
- sensor-reading
- sensor-reading-aggregated

The sensor-reading topic will expect Avro messages using the follwoing schema:

```json
{
  "type": "record",
  "name": "TemperatureReading",
  "namespace": "my.house",
  "doc": "This is a sample Avro schema for holding a temperature sensor reading.",
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
    }
  ]
}
```

Example of registering the schema:

```shell

// Register new schema
curl -vs --stderr - -XPOST -i -H "Content-Type: application/vnd.schemaregistry.v1+json" --data '{"schema":"\"{\n \\"type\\": \\"record\\",\n \\"name\\": \\"evolution\\",\n \\"doc\\": \\"This is a sample Avro schema to get you started. Please edit\\",\n \\"namespace\\": \\"com.landoop\\",\n \\"fields\\": [\n {\n \\"name\\": \\"name\\",\n \\"type\\": \\"string\\"\n },\n {\n \\"name\\": \\"number1\\",\n \\"type\\": \\"int\\"\n },\n {\n \\"name\\": \\"number2\\",\n \\"type\\": \\"float\\"\n }\n ]\n}\""}' /api/schema-registry/subjects/FILL_IN_SUBJECT/versions
```

### Locally

For quick feedback it's easiest to run the job locally,

1. If you're using Intellij, use the usual methods.
1. On the command line run `./gradlew shadowJar run`

### Using the Job Cluster

Run `./gradlew shadowJar startJob`. This will run the job within a job cluster that is setup in `flink-job-cluster.yml`. That cluster will run against the Kafka cluster started earlier.

### Observing the Job in Action

After starting the job with one of the methods above, let's observe it reading an writing a message from one Kafak topic to another.

1. Start the job using one of the methods above.
1. In a new terminal start a Kafka producer by running `./scripts/start-kafka-producer.sh sensor-reading`
1. You'll see the prompt `>`. Enter the message `1:{ message: "Hello World!" }`
1. Navigate to the [Kafdrop](http://localhost:8001/#/) and view the messages both the `source` and `destination` topics. Be sure to change format to default or else you will not see any messages.

You should see the message `1:{ message: "Hello World!" }` in both topics.
