package app.tempwatcher;

import my.house.TemperatureReading;
import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.api.java.tuple.Tuple4;
import org.apache.flink.api.java.tuple.Tuple5;

/** AverageAggregate. */
public class AverageAggregate
    implements AggregateFunction<
        TemperatureReading,
        Tuple5<String, Integer, Long, Float, Long>,
        Tuple4<String, Integer, Long, Double>> {
  @Override
  public Tuple5<String, Integer, Long, Float, Long> createAccumulator() {
    return new Tuple5<>("n/a", 0, 0L, 0.0f, 0L);
  }

  @Override
  public Tuple5<String, Integer, Long, Float, Long> add(
      final TemperatureReading value,
      final Tuple5<String, Integer, Long, Float, Long> accumulator) {
    return new Tuple5<>(
        value.getName().toString(),
        value.getSensorId(),
        value.getDatetimeMs(),
        accumulator.f3 + value.getTemperature(),
        accumulator.f4 + 1L);
  }

  @Override
  public Tuple4<String, Integer, Long, Double> getResult(
      final Tuple5<String, Integer, Long, Float, Long> accumulator) {
    final var average = ((double) accumulator.f3) / accumulator.f4;
    return new Tuple4<>(accumulator.f0, accumulator.f1, accumulator.f2, average);
  }

  @Override
  public Tuple5<String, Integer, Long, Float, Long> merge(
      final Tuple5<String, Integer, Long, Float, Long> a,
      final Tuple5<String, Integer, Long, Float, Long> b) {
    return new Tuple5<>(a.f0, a.f1, a.f2, a.f3 + b.f3, a.f4 + b.f4);
  }
}
