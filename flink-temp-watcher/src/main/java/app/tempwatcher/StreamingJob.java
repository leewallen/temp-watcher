/**
 * Something profound.
 */
package app.tempwatcher;

import my.house.TemperatureReading;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.connector.base.DeliveryGuarantee;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.connector.kafka.source.reader.deserializer.KafkaRecordDeserializationSchema;
import org.apache.flink.formats.avro.registry.confluent.ConfluentRegistryAvroDeserializationSchema;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.PrintSinkFunction;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;

import java.time.Duration;


/**
 * Starting point for a Flink streaming job using the DataStream API.
 */
public final class StreamingJob {


    private StreamingJob() {
        // prevents calls from subclass
        throw new UnsupportedOperationException();
    }

    public static void main(final String[] args) throws Exception {
        final JobConfig config = JobConfig.create();
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();


        final KafkaSource source = KafkaSource
                .<TemperatureReading>builder()
                .setBootstrapServers(config.brokers())
                .setStartingOffsets(OffsetsInitializer.timestamp(1674836819886L))
                .setTopics("sensor-reading")
                .setDeserializer(KafkaRecordDeserializationSchema.valueOnly(
                        ConfluentRegistryAvroDeserializationSchema.forSpecific(TemperatureReading.class, config.schemaRegistryUrl()))
                )
                .setProperties(config.consumer())
                .build();

        final KafkaSink<String> kafkaSink = KafkaSink
                .<String>builder()
                .setBootstrapServers(config.brokers())
                .setRecordSerializer(
                        KafkaRecordSerializationSchema.builder()
                                .setTopic("destination")
                                .setValueSerializationSchema(new SimpleStringSchema())
                                .build()
                )
                .setDeliverGuarantee(DeliveryGuarantee.NONE)
                .setKafkaProducerConfig(config.producer())
                .build();


        DataStreamSource ds = env.fromSource(source,
                WatermarkStrategy.<TemperatureReading>forBoundedOutOfOrderness(Duration.ofMillis(0))
                    .withIdleness(Duration.ofSeconds(10))
                    .withTimestampAssigner((event, timestamp) -> event.getDatetimeMs()), "Source Topic");

        var stream = ds.uid("sensor-reading")
            .keyBy((KeySelector<TemperatureReading, Tuple2<String, Integer>>) value ->
                new Tuple2<>(value.getName().toString(), value.getSensorId()),
                Types.TUPLE(Types.STRING, Types.INT))
            .window(TumblingEventTimeWindows.of(Time.seconds(5)))
            .aggregate(new AverageAggregate())
            .uid("Aggregating")
            .name("Aggregating")
            .map(new MapTuple3ToString());

        PrintSinkFunction<String> printFunction = new PrintSinkFunction<>(false);
        stream.addSink(printFunction).name("stderr");
        stream.sinkTo(kafkaSink).name("destination");

        env.execute("Kafka Experiment");
    }
}
