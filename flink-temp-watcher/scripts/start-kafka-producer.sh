#!/bin/bash

cat test-json-sensor-readings.txt | \
  docker exec -i schema-registry kafka-avro-console-producer \
  --broker-list broker-1:19092 \
  --topic sensor-reading \
  --property "schema.registry.url=http://localhost:8085" \
  --property "value.schema.id=1"
