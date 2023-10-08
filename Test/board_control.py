import socket
import keyboard
import time

IP_ADDRESS = '192.168.137.134'
PORT = 5101

def send(steering, motor):
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    sock.sendto(bytes([1, steering, motor]), (IP_ADDRESS, PORT))


def main():
    while True:
        steering = 3
        motor = 3
        if keyboard.is_pressed("w"):
            motor = 1
        elif keyboard.is_pressed("s"):
            motor = 2
        if keyboard.is_pressed("a"):
            steering = 1
        elif keyboard.is_pressed("d"):
            steering = 2
        if keyboard.is_pressed("q"):
            print('QUIT')
            break
        send(steering, motor)
        time.sleep(0.1)
    

main()


