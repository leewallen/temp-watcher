import argparse
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

    args = parser.parse_args()

    print(f"bridge: {args.bridge}\ninterval: {args.interval}\nsensors: {args.sensors}\ntelegraf: {args.telegrafip}:{args.telegrafport}\n")
    if not args.bridgeusername:
        print(f'Unable to read secret.')
        raise ValueError('The bridgeusername wasn''t provided.')
    else:
        print(f'bridgeusername is available.')

    if args.bridge and args.bridgeusername:
        motion_sensor = MotionSensor(args.bridge, args.bridgeusername, args.interval, args.sensors, args.telegrafip, args.telegrafport)
        ticker = threading.Event()
        while not ticker.wait(int(args.interval)):
            motion_sensor.run()


