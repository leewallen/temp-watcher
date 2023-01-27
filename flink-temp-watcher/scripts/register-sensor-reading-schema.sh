#!/bin/bash

curl -vs --stderr - -XPOST -i \
  -H "Content-Type: application/vnd.schemaregistry.v1+json" \
  --data '{"schema":"{\"type\":\"record\",\"name\":\"TemperatureReading\",\"namespace\":\"my.house\",\"doc\":\"Avro schema for holding a temperature sensor reading.\",\"fields\":[{\"name\":\"name\",\"type\":\"string\"},{\"name\":\"sensorId\",\"type\":\"int\"},{\"name\":\"temperature\",\"type\":\"float\"},{\"name\":\"datetimeMs\",\"type\":\"long\"}]}"}' \
  http://localhost:8000/api/schema-registry/subjects/sensor-reading-value/versions