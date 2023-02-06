import argparse
import os
import threading
from MotionSensor import MotionSensor

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument("-b", "--bridge", help="Bridge, or hub, IP address")
    parser.add_argument("-u", "--bridgeusername", help="Username created for interacting with the Hue Bridge")
    parser.add_argument("-i", "--interval", help="Interval in seconds to check motion sensor")
    parser.add_argument("-s", "--sensors", help="Comma separated string of sensor IDs to monitor")
    parser.add_argument("-t", "--telegrafip", help="Telegraf IP")
    parser.add_argument("-tp", "--telegrafport", help="Telegraf Port")
    parser.add_argument("-bs", "--bootstrap", help="Bootstrap servers")
    parser.add_argument("-tt", "--targettopic", help="Target topic")
    parser.add_argument("-sru", "--schemaregistry_url", help="Schema registry URL")

    args = parser.parse_args()

    print(
        f"bridge: {args.bridge}\ninterval: {args.interval}\nsensors: {args.sensors}\ntelegraf: {args.telegrafip}:{args.telegrafport}\n")
    if not args.bridgeusername:
        print(f'Unable to read secret.')
        raise ValueError('The bridgeusername wasn''t provided.')
    else:
        print(f'bridgeusername is available.')

    path = os.path.realpath(os.path.dirname(__file__))
    with open(f"{path}/avro/sensor_reading.avsc") as f:
        schema_str = f.read()

    if args.bridge and args.bridgeusername:
        motion_sensor = MotionSensor(
            args.bridge,
            args.bridgeusername,
            args.interval,
            args.sensors,
            args.telegrafip,
            args.telegrafport,
            args.bootstrap,
            args.targettopic,
            args.schemaregistry_url,
            schema_str
        )
        ticker = threading.Event()
        while not ticker.wait(int(args.interval)):
            motion_sensor.run()
