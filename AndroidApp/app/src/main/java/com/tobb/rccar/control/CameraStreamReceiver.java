package com.tobb.rccar.control;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class CameraStreamReceiver {

    private DatagramSocket socket;
    private boolean isRunning = false;
    public static final int PORT_RECEIVER = 4446;
    public static final int BUFF_SIZE = 54003;
    public static final int CONTROL_SIZE = 5;
    private StreamDisplayer streamDisplayer;

    public CameraStreamReceiver(StreamDisplayer streamDisplayer){
        this.streamDisplayer = streamDisplayer;
    }

    public void start(){
        if(socket != null){
            socket.close();
        }
        try {
            socket = new DatagramSocket(PORT_RECEIVER);
        } catch (SocketException e) {
            e.printStackTrace();
            return;
        }
        isRunning = true;
        Thread thread = new Thread(){
            @Override
            public void run() {

                byte [] fullImage = null;
                int width = 0, height = 0;
                int rows = 10;
                while(isRunning){

                    int row = 0;
                    while (row < rows) {
                        byte[] data = new byte[BUFF_SIZE + CONTROL_SIZE];
                        DatagramPacket datagramPacket = new DatagramPacket(data, data.length);
                        try {
                            socket.receive(datagramPacket);
                        } catch (IOException e) {
                            e.printStackTrace();
                            continue;
                        }
                        if(fullImage == null || fullImage.length != width * height * 3){
                            width = ((data[1] & 0xFF) << 8) | (data[2] & 0xFF);
                            height = ((data[3] & 0xFF) << 8) | (data[4] & 0xFF);
                            fullImage = new byte[width * height * 3];
                            rows = width * height * 3 / BUFF_SIZE + 1;
                        }
                        int offset = BUFF_SIZE * data[0];
                        int size = Math.min(width * height * 3 - offset, BUFF_SIZE);
                        row++;
                        if(data[0] >= rows){
                            continue;
                        }
                        System.arraycopy(data, CONTROL_SIZE, fullImage, offset, size);
                    }
                    int[] colors = new int[width * height];
                    for(int i = 0; i < fullImage.length; i += 3){
                        colors[i / 3] = Color.rgb(fullImage[i], fullImage[i + 1], fullImage[i + 2]);
                    }
                    Bitmap bitmap = Bitmap.createBitmap(colors, width, height, Bitmap.Config.RGB_565);
                    streamDisplayer.show(bitmap);
                }
                socket.close();
            }
        };
        thread.start();
    }

    public void stop(){
        isRunning = false;
    }

    public void dispose() {
        stop();
    }
}
