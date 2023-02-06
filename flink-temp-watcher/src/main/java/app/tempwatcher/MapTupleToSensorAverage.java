package app.tempwatcher;

import java.time.Instant;
import my.house.SensorReadingAverage;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.tuple.Tuple4;

/** MapTupleToString converts the keyed values used for averages into a String for output. */
public class MapTupleToSensorAverage
    implements MapFunction<Tuple4<String, Integer, Long, Double>, SensorReadingAverage> {
  @Override
  public SensorReadingAverage map(Tuple4<String, Integer, Long, Double> value) throws Exception {
    return new SensorReadingAverage(
        value.f0, value.f1, value.f3.floatValue(), Instant.ofEpochMilli(value.f2));
  }
}
