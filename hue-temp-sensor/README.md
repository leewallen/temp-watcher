# motion-sensor

Python example using the Philips Hue API and the Philips Hue motion sensor. The motion sensor this is using has temperature information in celcius available in the sensor's state information.

The Philips Hue API has a list of sensors that are supported. The Philips Hue motion sensor includes the ZLLTemperature sensor. You can [find details about the sensor here](https://developers.meethue.com/develop/hue-api/supported-devices/#supportred-sensors). The information that we want to retrieve from the sensor state is in the `temperature` property.

| Property    | Data Type | Description                                                                                                        |
|-------------|-----------|--------------------------------------------------------------------------------------------------------------------|
| temperature | int32     | Current temperature in 0.01 degrees Celsius. (3000 is 30.00 degree) Bridge does not verify the range of the value. |

## Command Line Args

| Short Option |    Long Option     | Description                                                  |
|:------------:|:------------------:|:-------------------------------------------------------------|
|     `-b`     |     `--bridge`     | Hue Bridge IP address |
|     `-u`     | `--bridgeusername` | Username created for interacting with the Hue Bridge |
|     `-i`     |    `--interval`    | Interval in seconds to check motion sensor temperature value |
|     `-s`     |    `--sensors`     | Comma separated list of Sensor IDs to monitor |
|     `-t`     |   `--telegrafip`   | IP or hostname of the Telegraf service |
|     `-tp`     |  `--telegrafport`  | Port that Telegraf is listening to for incoming messages from statsd |
