/** Something profound. */
package app.tempwatcher;

import java.time.Duration;
import my.house.SensorReading;
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
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.PrintSinkFunction;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;

/** Starting point for a Flink streaming job using the DataStream API. */
public final class StreamingJob {

  public static final String AGGREGATING = "Aggregating";
  public static final String SENSOR_READING = "sensor-reading";
  public static final String SENSOR_READING_AVG = "sensor-reading-avg";
  public static final String SOURCE_TOPIC = "Source Topic";
  public static final String STDERR = "stderr";
  public static final String DESTINATION = "destination";
  public static final String FLINK_EXPERIMENT = "Flink Experiment";
  public static final int WINDOW_IN_SECONDS = 600;

  private StreamingJob() {
    // prevents calls from subclass
    throw new UnsupportedOperationException();
  }

  public static void main(final String[] args) throws Exception {
    final JobConfig config = JobConfig.create();
    final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

    env.enableCheckpointing(300000, CheckpointingMode.EXACTLY_ONCE);

    final KafkaSource source =
        KafkaSource.<SensorReading>builder()
            .setBootstrapServers(config.brokers())
            .setStartingOffsets(OffsetsInitializer.latest())
            .setTopics(SENSOR_READING)
            .setDeserializer(
                KafkaRecordDeserializationSchema.valueOnly(
                    ConfluentRegistryAvroDeserializationSchema.forSpecific(
                        SensorReading.class, config.schemaRegistryUrl())))
            .setProperties(config.consumer())
            .build();

    final KafkaSink<String> kafkaSink =
        KafkaSink.<String>builder()
            .setBootstrapServers(config.brokers())
            .setRecordSerializer(
                KafkaRecordSerializationSchema.builder()
                    .setTopic(SENSOR_READING_AVG)
                    .setValueSerializationSchema(new SimpleStringSchema())
                    .build())
            .setDeliverGuarantee(DeliveryGuarantee.NONE)
            .setKafkaProducerConfig(config.producer())
            .build();

    final var ds =
        env.fromSource(
            source,
            WatermarkStrategy.<SensorReading>forBoundedOutOfOrderness(Duration.ofMillis(0))
                .withIdleness(Duration.ofSeconds(10))
                .withTimestampAssigner((event, timestamp) -> event.getDatetimeMs().toEpochMilli()),
            SOURCE_TOPIC);

    final var stream =
        ds.uid(SENSOR_READING)
            .keyBy(
                (KeySelector<SensorReading, Tuple2<String, Integer>>)
                    value -> new Tuple2<>(value.getName().toString(), value.getSensorId()),
                Types.TUPLE(Types.STRING, Types.INT))
            .window(TumblingEventTimeWindows.of(Time.seconds(WINDOW_IN_SECONDS)))
            .aggregate(new AverageAggregate())
            .uid(AGGREGATING)
            .name(AGGREGATING)
            .map(new MapTupleToString());

    final var printFunction = new PrintSinkFunction<>(false);
    stream.addSink(printFunction).name(STDERR);
    stream.sinkTo(kafkaSink).name(DESTINATION);

    env.execute(FLINK_EXPERIMENT);
  }
}
