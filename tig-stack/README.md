Based on these projects:

- https://github.com/huntabyte/tig-stack.git
- https://github.com/bcremer/docker-telegraf-influx-grafana-stack


## Test Input for Telegraf

The following configuration from the `telegraf.conf` file will send sample metrics data to InfluxDB. 

```yaml
[[inputs.file]]
  files = ["/etc/telegraf/sample-metrics.in"]
  data_format = "influx"

[[outputs.file]]
  files = ["stdout"]
```

The `docker-compose.yml` file has a volume mapping for mapping the `sample-metrics.in` file to the `/etc/telegraf/sample-metrics.in` location.

View the sample metrics on a Mac using the following command:

```bash
docker-compose up -d; open http://localhost:3000/d/JlkNiHo4k/room-temperatures?orgId=1&from=1674533400000&to=1674534059000
```

A view of the dashboard with the sample metrics:

![test-metrics.png](./test-metrics.png)

