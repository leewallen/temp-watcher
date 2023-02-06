#!/bin/bash
echo "PWD = $PWD"
echo "CWD = $CWD"

schema_value=$(cat $1 | jq -c '.' | jq -R -s '.')
schema_to_register="{\"schema\":"$schema_value"}"

echo -e $schema_to_register | curl -vs --stderr - -XPOST -i \
  -H "Content-Type: application/vnd.schemaregistry.v1+json" \
  --data @- \
  http://localhost:8000/api/schema-registry/subjects/sensor-reading-value/versions