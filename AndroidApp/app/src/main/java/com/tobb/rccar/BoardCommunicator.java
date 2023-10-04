package com.tobb.rccar;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.os.Handler;

import me.aflak.arduino.Arduino;
import me.aflak.arduino.ArduinoListener;

public class BoardCommunicator implements ArduinoListener {

    private Arduino arduino;

    public BoardCommunicator(Context context){
        arduino = new Arduino(context, 9600);
        arduino.setArduinoListener(this);
        String hello = "Message";

        System.out.println("CAR SERVER " + arduino.isOpened());
        arduino.send(hello.getBytes());
    }

    @Override
    public void onArduinoAttached(UsbDevice device) {
        System.out.println("CAR SERVER serial attached " + device.getDeviceName());
        arduino.open(device);

    }

    @Override
    public void onArduinoDetached() {
        System.out.println("CAR SERVER serial detached");

    }

    @Override
    public void onArduinoMessage(byte[] bytes) {

    }

    @Override
    public void onArduinoOpened() {
        System.out.println("CAR SERVER serial attached");
    }

    @Override
    public void onUsbPermissionDenied() {

        System.out.println("No permission");
        new Handler().postDelayed(() -> arduino.reopen(), 3000);
    }
}
