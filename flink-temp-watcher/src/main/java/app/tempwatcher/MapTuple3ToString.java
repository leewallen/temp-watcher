package app.tempwatcher;

import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.api.java.tuple.Tuple4;

import java.time.LocalDateTime;
import java.util.Date;

public class MapTuple3ToString implements MapFunction<Tuple4<String, Integer, Long, Double>, String> {
    @Override
    public String map(Tuple4<String, Integer, Long, Double> value) throws Exception {
        Date dateTime = new Date(value.f2);
        return String.format("{Location: %s, Sensor ID: %d, Date: %s, Average Temperature: %.2f",
                value.f0, value.f1, dateTime.toString(), value.f3);
    }
}
