import numpy as np
import requests
import socket
import cv2

PORT = 42069


def request_receiving(ip='192.168.14.112'):
    local_ip = '192.168.14.105'
    requests.get(f'http://{ip}:8080/setStream?ipAddress={local_ip}&port={PORT}')
    requests.get(f'http://{ip}:8080/startStream')

def stop(ip='192.168.14.112'):
    requests.get(f'http://{ip}:8080/stopStream')

def receive_camera():
    udp_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    host = ''
    udp_socket.bind((host, PORT))

    frame = None
    while True:
        # Receive data from the socket
        for i in range(10):
            data, addr = udp_socket.recvfrom(32006)  # 1024 is the buffer size

            if frame is None:
                w = data[1] * 256 + data[2]
                h = data[3] * 256 + data[4]
                frame = np.zeros((h, w, 3), 'uint8')

            offset = 32001 * data[0] // 3
            pixels = data[5:]
            print(w, h, offset)
            for a in range(0, len(pixels), 3):
                i = (offset) // 3 + a // 3
                frame[i//w, i%w] = (pixels[a], pixels[a + 1], pixels[a + 2])
            # Print received data and sender's address

        cv2.imshow('Live Stream', frame)
        if cv2.waitKey(1) & 0xFF == ord('q'):
            break

request_receiving()
receive_camera()
stop()