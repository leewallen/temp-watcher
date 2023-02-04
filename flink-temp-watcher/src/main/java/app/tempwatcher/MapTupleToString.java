package app.tempwatcher;

import java.util.Date;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.tuple.Tuple4;

/** MapTupleToString converts the keyed values used for averages into a String for output. */
public class MapTupleToString
    implements MapFunction<Tuple4<String, Integer, Long, Double>, String> {
  @Override
  public String map(Tuple4<String, Integer, Long, Double> value) throws Exception {
    final Date dateTime = new Date(value.f2);
    return String.format(
        "{Location: %s, Sensor ID: %d, Date: %s, Average Temperature: %.2f",
        value.f0, value.f1, dateTime.toString(), value.f3);
  }
}
