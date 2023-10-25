package com.tobb.rccar;

import android.app.Activity;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class WifiBoardCommunicator implements BoardCommunicator{

    public static final int DISCOVERY_PORT = 5102;
    public static final int BOARD_PORT = 5101;
    private MyLogger myLogger;
    private DiscoveryBoardThread discoveryBoardThread;
    private boolean run = true;
    private byte[] drivePacket;
    private final Object lock = new Object();


    public WifiBoardCommunicator(MyLogger myLogger) {
        this.myLogger = myLogger;
        discoveryBoardThread = new DiscoveryBoardThread();
        discoveryBoardThread.start();
    }

    @Override
    public void sendDriveCommand(byte motor, byte steering) {
        drivePacket = new byte[]{1, steering, motor};
        synchronized(lock) {
            lock.notify();
        }
    }

    @Override
    public void onResume(Activity activity) {

    }

    @Override
    public void onDestroy(Activity activity) {
        run = false;
    }

    public class DiscoveryBoardThread extends Thread{

        private DatagramSocket discoverySocket;

        @Override
        public void run() {
            try {
                this.discoverySocket = new DatagramSocket(DISCOVERY_PORT, InetAddress.getByName("0.0.0.0"));
            } catch (SocketException | UnknownHostException e) {
                e.printStackTrace();
            }
            byte[] buff = new byte[16];
            DatagramPacket packet = new DatagramPacket(buff, buff.length);
            try {
                discoverySocket.receive(packet);
                InetAddress boardAddress = packet.getAddress();
                myLogger.log("Board discover: " + boardAddress.toString());

                DatagramSocket sendSocket = new DatagramSocket();
                while (run){
                    synchronized(lock) {
                        lock.wait();
                    }
                    myLogger.log("Sending: " + drivePacket[1] + " - " + drivePacket[2]);
                    DatagramPacket datagramPacket = new DatagramPacket(drivePacket, 3, boardAddress, BOARD_PORT);
                    sendSocket.send(datagramPacket);
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
