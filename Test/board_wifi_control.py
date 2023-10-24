import socket
import keyboard
import time

PORT = 5101
IP_ADDRESS = ''


def send(steering, motor, ip_address):
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    sock.sendto(bytes([1, steering, motor]), (ip_address, PORT))


def discovery_board():
    print('Waiting for board discovery')
    soc = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    soc.bind(('0.0.0.0', 5102))
    message, sender = soc.recvfrom(10)
    ip_address = sender[0]
    return ip_address


def main():
    ip_address = discovery_board()
    print(f'Sending to {ip_address}')
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
        send(steering, motor, ip_address)
        time.sleep(0.1)


main()
