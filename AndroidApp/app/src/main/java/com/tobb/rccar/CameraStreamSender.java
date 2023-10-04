package com.tobb.rccar;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;

import androidx.camera.view.PreviewView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.CountDownLatch;

public class CameraStreamSender extends Thread{

    private boolean isRunning = true;
    private String ipAddress;
    private int port;
    private DatagramSocket socket;
    private Activity activity;
    private PreviewView mPreviewView;
    private byte[] byteArray;
    private Bitmap bitmap;

    public CameraStreamSender(Activity activity, PreviewView mPreviewView){
        this.activity = activity;
        this.mPreviewView = mPreviewView;

    }

    private void send(byte[][] buf){
        System.out.println("CAR SERVER sending to " + ipAddress + ", " + port + ", " + buf.length);
        DatagramPacket packet = null;
        for(int i = 0; i < buf.length; i++) {
            try {
                packet = new DatagramPacket(buf[i], buf[i].length, InetAddress.getByName(ipAddress), port);
                socket.send(packet);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void run() {
        while(isRunning) {
            getImage();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void stopStream() {
        isRunning = false;
        socket.close();
    }

    public void setReceiver(String ipAddress, int port) {
        this.ipAddress = ipAddress;
        this.port = port;
    }

    public void prepare() {
        isRunning = true;
        try {
            socket = new DatagramSocket(4445);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    private void getImage(){
        CountDownLatch  countDownLatch = new CountDownLatch(1);
        activity.runOnUiThread(() -> {
            bitmap = mPreviewView.getBitmap();
            countDownLatch.countDown();
        });
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        int[] data = new int[w * h];
        bitmap.getPixels(data, 0, w, 0, 0, w, h);
        byteArray = new byte[data.length * 3 + 4];
        for(int i = 0; i < data.length; i++){
            int redValue = Color.red(data[i]);
            int blueValue = Color.blue(data[i]);
            int greenValue = Color.green(data[i]);
            byteArray[i*3 + 0] = (byte) redValue;
            byteArray[i*3 + 1] = (byte) blueValue;
            byteArray[i*3 + 2] = (byte) greenValue;
        }
        send(generatePackets(w, h, byteArray));
    }

    public byte[][] generatePackets(int w, int h, byte[] pixels){
        int MAX_SIZE = 32001;
        int offset = 0;
        byte count = 0;
        byte[][] bufs = new byte[(int)Math.ceil(1.0 * pixels.length / MAX_SIZE)][];
        while (offset < pixels.length) {
            int size  = Math.min(pixels.length - offset, MAX_SIZE);
            byte[] data = new byte[size + 5];
            data[0] = count;
            data[1] = (byte) (w / 256);
            data[2] = (byte) (w % 256);
            data[3] = (byte) (h / 256);
            data[4] = (byte) (h % 256);
            if (offset + size - offset >= 0)
                System.arraycopy(pixels, offset, data, 4 + offset - offset, offset + size - offset);
            bufs[count] = data;
            count += 1;
            offset += size;
        }
        return bufs;

    }
}
