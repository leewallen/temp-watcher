


# Telegraf, InfluxDB, Grafana (8.5.15 Stack 


![https://influxdata.github.io/branding/img/downloads/telegraf.png](./telegraf.png) <br/>
![https://influxdata.github.io/branding/img/downloads/influxdb.png](./influxdb.png)  <br/>
![https://github.com/grafana/grafana/blob/main/docs/logo-horizontal.png](./grafana_logo.png)

Practice project for learning how to use the TIG stack. 

## Credit where credit is due!

### Initial Starting Point

I started with [Hunter Johnston's](https://github.com/huntabyte) [TIG stack repo](https://github.com/huntabyte/tig-stack.git). I first watched [Hunter's video on YouTube](https://youtu.be/QGG_76OmRnA). The video is concise and has great information. Hunter communicates very clearly, and his YouTube channel looks like it has some great content.

### Provision a Grafana dashboard and sending metrics to InfluxDB

I also used [Benjamin Cremer's](https://github.com/bcremer) [docker-telegraf-influx-grafana-stack repo](https://github.com/bcremer/docker-telegraf-influx-grafana-stack) as an example of how to provision a Grafana dashboard. I had started a simple python project for playing around with my Phillip's Hue motion sensor using the Hue API, so I used that app with statsd in a way similar to Benjamin's python metric producing python app.

## Interesting bits

### Telegraf 1.19

There are many plugins available to use with Telegraf, and InfluxData has made it easy to find example configurations for many of the plugins that you can use. I'm using the Python statsd client to send data to Telegraf / InfluxDB. 

Some of the plugin types that you can use with Telegraf are inputs (I'm using statsd), processors and aggregators, and outputs (I'm using InfluxDB).

Look at the [telegraf.conf](./tig-stack/telegraf/telegraf.conf) to see how I configured the [statsd input](./tig-stack/telegraf/telegraf.conf#L174) and the [InfluxDB output](./tig-stack/telegraf/telegraf.conf#L117) plugins.

### InfluxDB 2.6

The [entrypoint.sh script](./tig-stack/entrypoint.sh) should create the target bucket and set the data retention time, the ORG, username and password, and set the admin token. The admin token was created using the openssl command:

```shell
openssl rand -hex 32
```

I ran the simple Python app that is using the statsd client to send data to Telegraf and ultimately into InfluxDB. I then went into the InfluxDB UI and used the Query Builder to help me create the queries needed to see the temperature data that was being sent from the Python app. 

![InfluxDB Query Builder](./influxdb-query-builder.png)

Clicking on the "Script Editor" button will show the Flux queries that were built using the "Query Builder" view.

Here is an example of one of the Flux queries:

```sql
from(bucket: "tempsensors")
  |> range(start: v.timeRangeStart, stop: v.timeRangeStop)
  |> filter(fn: (r) => r["metric_type"] == "gauge")
  |> filter(fn: (r) => r["type"] == "27" or r["type"] == "34")
  |> filter(fn: (r) => r["host"] == "27" or r["host"] == "34")
  |> aggregateWindow(every: v.windowPeriod, fn: mean, createEmpty: false)
  |> yield(name: "mean")
```

### Grafana 8.5.15

I created a simple dashboard in Grafana for displaying temperature readings from a couple of Phillips Hue motion sensors, and copied the JSON to [./tig-stack/grafana/dashboards/temperatures.json](./tig-stack/grafana/dashboards/temperatures.json) so the dashboard would be added to Grafana when the Grafana instance is brought up. 

![Grafana Dashboard](./grafana-dashboard.png)


The panels are populated with data from InfluxDB using the Flux queries I created in the InfluxDB UI. If you're familiar with SQL and/or Flink SQL, then it shouldn't be hard to figure out how to write future queries from scratch. However, using the InfluxDB Query Builder is probably the easiest option.

#### Adding the InfluxDB datasource in Grafana

The InfluxDB-Flux datasource was added by creating the [./tig-stack/grafana/provisioning/datasources/influxdb.yml](./tig-stack/grafana/provisioning/datasources/influxdb.yml) file. This file is a modified version of the InfluxDB yaml file that Benjamin Cremer created. The main differences are that I'm using environment variables for setting certain properties in the influxdb.yml file, I set the datasource name to `influxdb-flux`, and that the jsonData.version property is set to `flux`.



## ⚡️ Getting Started

Clone the project

```bash
git clone https://github.com/huntabyte/tig-stack.git
```

Navigate to the project directory

```bash
cd tig-stack
```

Change the environment variables define in `.env` that are used to setup and deploy the stack
```bash
├── telegraf/
├── .env         <---
├── docker-compose.yml
├── entrypoint.sh
└── ...
```

Customize the `telegraf.conf` file which will be mounted to the container as a persistent volume

```bash
├── telegraf/
│   ├── telegraf.conf <---
├── .env
├── docker-compose.yml
├── entrypoint.sh
└── ...
```

Start the services
```bash
docker-compose up -d
```
## Docker Images Used (Official & Verified)

[**Telegraf**](https://hub.docker.com/_/telegraf) / `1.19`

[**InfluxDB**](https://hub.docker.com/_/influxdb) / `2.1.1`

[**Grafana-OSS**](https://hub.docker.com/r/grafana/grafana-oss) / `8.4.3`



## Contributing

Contributions are always welcome!

