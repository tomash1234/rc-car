import socket
import keyboard
import time
import requests

IP_ADDRESS = '192.168.14.112'
PORT = 8080



def send(steering, motor):
    ret = requests.get(f'http://{IP_ADDRESS}:{PORT}/drive', {'motor': motor, 'steering': steering})


def main():
    while True:
        steering = 0
        motor = 0
        if keyboard.is_pressed("w"):
            motor = 1
        elif keyboard.is_pressed("s"):
            motor = -1
        if keyboard.is_pressed("a"):
            steering = 1
        elif keyboard.is_pressed("d"):
            steering = -1
        if keyboard.is_pressed("q"):
            print('QUIT')
            break
        send(steering, motor)
        time.sleep(0.1)
    

main()


