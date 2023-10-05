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

    BUFF_SIZE = 54003
    CONTROL_BYTES = 5

    buff = None
    while True:
        rows = 10
        r = 0
        while r < rows:
            data, addr = udp_socket.recvfrom(BUFF_SIZE + CONTROL_BYTES)  # 1024 is the buffer size

            if buff is None:
                w = int(data[1] * 256 + data[2])
                h = int(data[3] * 256 + data[4])
                rows = w * h * 3 // BUFF_SIZE + 1
                buff = np.zeros((h * w * 3), 'uint8')

            offset = BUFF_SIZE * data[0]
            size = min(w * h * 3 - offset,  BUFF_SIZE)
            array = np.frombuffer(data, dtype=np.uint8)
            buff[offset:offset + size] = array[CONTROL_BYTES:CONTROL_BYTES + size]
            r += 1

        w = int(data[1] * 256 + data[2])
        h = int(data[3] * 256 + data[4])
        frame = buff.reshape((h, w, 3))
        frame = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)

        cv2.imshow('Live Stream', frame)
        if cv2.waitKey(10) & 0xFF == ord('q'):
            break

request_receiving()
receive_camera()
stop()