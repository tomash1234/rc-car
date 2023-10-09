package com.tobb.rccar;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.nio.charset.StandardCharsets;

public class BoardCommunicator{

    private final MyLogger myLogger;
    private final Context context;
    private UsbDevice device;
    private UsbManager usbManager;
    private UsbSerialDevice serialDevice;

    public static final String ACTION_USB_PERMISSION = "cz.tobb.usb";

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case ACTION_USB_PERMISSION:
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        openSerialPort();
                    } else {
                        myLogger.log("Permission not granted");
                    }
                    break;
                case UsbManager.ACTION_USB_DEVICE_ATTACHED:
                    myLogger.log("ACTION_USB_DEVICE_ATTACHED");
                    askForPermission();
                    break;
                case UsbManager.ACTION_USB_DEVICE_DETACHED:
                    myLogger.log("ACTION_USB_DEVICE_DETACHED");
                    if(serialDevice != null){
                        serialDevice.close();
                    }
                    break;
            }
        }
    };

    public BoardCommunicator(Context context, MyLogger myLogger) {
        this.myLogger = myLogger;
        this.context = context;
        usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
    }

    public void askForPermission(){
        if(usbManager.getDeviceList().isEmpty()){
            this.myLogger.log("No connected devices");
            return;
        }
        for(String key : usbManager.getDeviceList().keySet()){
            device = usbManager.getDeviceList().get(key);
            break;
        }
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_MUTABLE);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        context.registerReceiver(broadcastReceiver, filter);
        usbManager.requestPermission(device, pi);
    }


    public BroadcastReceiver getReceiver() {
        return broadcastReceiver;
    }

    public void openSerialPort() {
        UsbDeviceConnection connection = usbManager.openDevice(device);
        serialDevice = UsbSerialDevice.createUsbSerialDevice(device, connection);
        if (serialDevice == null) {
            myLogger.log("Serial Device is null");
            return;
        }
        if (serialDevice.open()) {
            serialDevice.setBaudRate(9600);
            serialDevice.setDataBits(UsbSerialInterface.DATA_BITS_8);
            serialDevice.setStopBits(UsbSerialInterface.STOP_BITS_1);
            serialDevice.setParity(UsbSerialInterface.PARITY_NONE);
            serialDevice.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
            serialDevice.read(data -> myLogger.log(">Board: " + new String(data, StandardCharsets.UTF_8)));
        } else {
            myLogger.log("Serial port could not be opened");
        }
    }

    public void sendDriveCommand(byte motor, byte steering){
        if(serialDevice == null){
            return;
        }
        byte[] packet = new byte[]{1, steering, motor};
        serialDevice.write(packet);
    }
}
