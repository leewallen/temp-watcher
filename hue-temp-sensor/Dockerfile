from python:3.10-slim

WORKDIR /usr/src/app

COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt

COPY ./src/* ./

ARG BRIDGE_IP
ARG INTERVAL=10
ARG SENSOR_IDS
ARG USERNAME
ARG TELEGRAFIP=localhost
ARG TELEGRAFPORT=8125

ENTRYPOINT ["python", "-u", "main.py"]
# CMD ["-b", "$BRIDGE_IP", "-u", "$USERNAME", "-i", "$INTERVAL", "-s", "$SENSOR_IDS", "-t", "$TELEGRAFIP", "-tp", "$TELEGRAFPORT"]
