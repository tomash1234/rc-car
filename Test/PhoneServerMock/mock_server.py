import json
import re
import socket
import socketserver
import threading
import time
from shutil import copyfileobj
from http.server import BaseHTTPRequestHandler
from PIL import Image



def read_file(file):
    with open(file) as f:
        return f.read()


def load_json(file):
    with open(file) as f:
        return json.load(f)


def get_kv_params(params):
    kv_params = {}
    for kv in params.split('&'):
        if len(kv) == 0:
            break
        [k, v] = kv.split('=')
        kv_params[k] = v
    return kv_params


def generate_packets(path):
    packet_size = 54003
    im = Image.open(path)
    pix = im.load()
    w, h = im.size
    pixels = [pix[i, j][k] for j in range(h) for i in range(w) for k in range(3)]
    offset = 0
    count = 0
    pix_count = w * h * 3
    ret = []
    while offset < pix_count:
        size = min(pix_count - offset, packet_size)
        data = [count, w // 256, w % 256, h // 256, h % 256]
        data += pixels[offset: offset + size]
        ret.append(bytes(data))
        count += 1
        offset += size
    return ret


class ThreadStopper:

    def __init__(self):
        self.running = True

    def should_run(self):
        return self.running

    def stop(self):
        self.running = False

THREAD_STOPPER = None

def camera_thread(ip_address, port, stopper):
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    receiver = (ip_address, int(port))
    counter = 0
    settings = load_json('configs/config_stream.json')
    imgs = settings['images']
    while stopper.should_run():
        packets = generate_packets(imgs[counter % len(imgs)])
        for pack in packets:
            sock.sendto(pack, receiver)
        time.sleep(settings['delay'])
        print('Image sent')
        counter += 1


class PhoneServer(BaseHTTPRequestHandler):

    def do_GET(self):
        _, url, _ = self.requestline.split()
        [endpoint, params] = re.split('[?]', url) if '?' in url else [url, '']
        if endpoint == '/info':
            self.serve_info_endpoint()
        elif endpoint == '/position':
            self.serve_position(params)
        elif endpoint == '/camera':
            self.serve_camera()
        elif endpoint == '/cameraPreview':
            self.serve_camera_preview()
        elif endpoint == '/drive':
            self.serve_drive(params)
        elif endpoint == '/setStream':
            self.serve_set_stream(params)
        elif endpoint == '/startStream':
            self.serve_start_stream()
        elif endpoint == '/stopStream':
            self.serve_stop_stream()
        else:
            self.reply(message=f'Invalid endpoint {endpoint}', status_code=404)

    def do_POST(self):
        print('Receive post')

    def serve_info_endpoint(self):
        self.reply(read_file('configs/config_info.json'), content_type='application/json')

    def serve_position(self, params):
        settings = load_json('configs/config_position_fail.json')
        timeout = int(params.replace('timeout=', '')) if 'timeout' in params else 5
        if settings['shouldFail'] or timeout < settings['delay']:
            time.sleep(timeout)
            self.reply(f"GPS could not be obtained before timeout. [{timeout}s]", status_code=409)
            return
        time.sleep(settings['delay'])
        self.reply(read_file('configs/config_position.json'), content_type='application/json')

    def serve_camera(self):
        self.reply_image('imgs/camera.jpg')

    def serve_camera_preview(self):
        self.reply_image('imgs/camPreview.jpg')

    def serve_drive(self, params):
        kv_params = get_kv_params(params)
        if 'motor' not in kv_params or 'steering' not in kv_params:
            self.reply("Request has to contain motor and steering parameters", status_code=400)
            return
        data = load_json('configs/config_store_data.json')
        with open(data['driveData'], 'w') as f:
            json.dump(kv_params, f)
        self.reply('receive')

    def serve_set_stream(self, params):
        kv_params = get_kv_params(params)
        if 'ipAddress' not in kv_params or 'port' not in kv_params:
            self.reply("Request has to contain ipAddress and port parameters", status_code=400)
            return
        data = load_json('configs/config_store_data.json')
        with open(data['streamData'], 'w') as f:
            json.dump(kv_params, f)
        self.reply(f'Set stream to {kv_params["ipAddress"]}:{kv_params["port"]}')

    def serve_start_stream(self):
        storeSettings = load_json('configs/config_store_data.json')
        settings = load_json(storeSettings['streamData'])
        global THREAD_STOPPER
        THREAD_STOPPER = ThreadStopper()
        thread = threading.Thread(target=camera_thread, args=(settings['ipAddress'], settings['port'], THREAD_STOPPER))
        thread.start()
        self.reply('Stream started')


    def serve_stop_stream(self):
        THREAD_STOPPER.stop()
        self.reply('Stream stopped')

    def reply(self, message='', status_code=200, content_type='text/plain'):
        self.send_response(status_code)
        self.send_header('Content-type', f'{content_type}; charset=utf-8')
        self.send_header('Content-length', str(len(message)))
        self.end_headers()
        self.wfile.write(bytes(message, 'utf8'))

    def reply_image(self, path, status_code=200, content_type='text/plain'):
        self.send_response(status_code)
        self.send_header('Content-type', 'image/jpeg; charset=utf-8')
        self.end_headers()
        with open(path, 'rb') as f:
            copyfileobj(f, self.wfile)


def run(port=8088):
    with socketserver.TCPServer(("", port), PhoneServer) as httpd:
        print("serving at port", port)
        httpd.serve_forever()


if __name__ == '__main__':
    run()
