package com.tobb.rccar;

import android.Manifest;
import android.accounts.Account;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import fi.iki.elonen.NanoHTTPD;

public class CarServer extends NanoHTTPD {

    private MyLogger myLogger;
    private PhoneInfoProvider phoneInfoProvider;
    private CarControl carControl;

    public CarServer(int port, MyLogger myLogger, PhoneInfoProvider phoneInfoProvider, CarControl carControl) {
        super(port);
        this.myLogger = myLogger;
        this.phoneInfoProvider = phoneInfoProvider;
        this.carControl = carControl;
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        String method = session.getMethod().name();

        Response response = null;
        if (method.equals("GET")) {
            switch (uri) {
                case "/info":
                    response = getInfo();
                    break;
                case "/position":
                    response = getPosition(session);
                    break;
                case "/camera":
                    response = getCamera();
                    break;
                case "/cameraPreview":
                    response = getCameraPreview();
                    break;
                case "/drive":
                    response = postDrive(session);
                    break;
                case "/setStream":
                    response = postStream(session);
                    break;
                case "/startStream":
                    response = startStream();
                    break;
                case "/stopStream":
                    response = stopStream();
                    break;
            }
        } else if (method.equals("POST")) {
            /*switch (uri) {
                case "/drive":
                    response = postDrive(session);
                    break;
            }*/
        }
        if (response != null) {
            myLogger.log(method + " - " + uri + " -" + session.getQueryParameterString());
        } else {
            response = newFixedLengthResponse(Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT,
                    "Page \"" + uri + "\" not exists");
        }

        return response;
    }

    private Response postStream(IHTTPSession session) {
        if(!session.getParameters().containsKey("ipAddress") || !session.getParameters().containsKey("port")){
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, NanoHTTPD.MIME_PLAINTEXT, "Request has to contain ipAddress and port parameters");
        }
        String ipAddress = String.valueOf(session.getParameters().get("ipAddress").get(0));
        int port = Integer.parseInt(session.getParameters().get("port").get(0));
        phoneInfoProvider.setStream(ipAddress, port);
        return newFixedLengthResponse(Response.Status.OK, NanoHTTPD.MIME_PLAINTEXT, "Set stream to " + ipAddress + ":" + port);
    }


    private Response startStream() {

        boolean ok = phoneInfoProvider.startStream();
        if(ok){
            return newFixedLengthResponse(Response.Status.OK, NanoHTTPD.MIME_PLAINTEXT, "Stream started");
        }
        return newFixedLengthResponse(Response.Status.CONFLICT, NanoHTTPD.MIME_PLAINTEXT, "Cannot start the stream");
    }

    private Response stopStream() {
        phoneInfoProvider.stopStream();
        return newFixedLengthResponse(Response.Status.OK, NanoHTTPD.MIME_PLAINTEXT, "Stream stopped");
    }

    private Response postDrive(IHTTPSession session) {
        if(!session.getParameters().containsKey("motor") || !session.getParameters().containsKey("steering")){
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, NanoHTTPD.MIME_PLAINTEXT, "Request has to contain motor and steering parameters");
        }
        double motor = Double.parseDouble(session.getParameters().get("motor").get(0));
        double steering = Double.parseDouble(session.getParameters().get("steering").get(0));
        carControl.drive(motor, steering);
        return newFixedLengthResponse(Response.Status.OK, NanoHTTPD.MIME_PLAINTEXT, "receive");
    }

    private Response getCamera() {
        File file = phoneInfoProvider.getCamera();
        if(!file.exists()){
            return newFixedLengthResponse(Response.Status.CONFLICT,  NanoHTTPD.MIME_PLAINTEXT, "Failed obtain image");
        }
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            return newFixedLengthResponse(Response.Status.CONFLICT,  NanoHTTPD.MIME_PLAINTEXT, "Failed obtain image");
        }

        return newFixedLengthResponse(Response.Status.OK, "image/jpeg", fis, -1);
    }

    private Response getCameraPreview() {
        File file = phoneInfoProvider.getCameraPreview();
        if(!file.exists()){
            return newFixedLengthResponse(Response.Status.CONFLICT,  NanoHTTPD.MIME_PLAINTEXT, "Failed obtain image");
        }
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            return newFixedLengthResponse(Response.Status.CONFLICT,  NanoHTTPD.MIME_PLAINTEXT, "Failed obtain image");
        }

        return newFixedLengthResponse(Response.Status.OK, "image/jpeg", fis, -1);
    }

    private Response getPosition(IHTTPSession session) {
        int timeout = 5;
        if(session.getParameters().containsKey("timeout")){
            timeout = Integer.parseInt(session.getParameters().get("timeout").get(0));
        }

        double[] position  = phoneInfoProvider.getLocation(timeout);
        if(position == null){
            return newFixedLengthResponse(Response.Status.CONFLICT,  NanoHTTPD.MIME_PLAINTEXT, "GPS could not be obtained");
        }
        if(position[4] < 0){
            return  newFixedLengthResponse(Response.Status.CONFLICT,  NanoHTTPD.MIME_PLAINTEXT, "GPS permissions not granted");
        }
        if(position[4] < 0.5){
            return newFixedLengthResponse(Response.Status.CONFLICT,  NanoHTTPD.MIME_PLAINTEXT, "GPS could not be obtained before timeout. [" + timeout + "s]" );
        }
        return newFixedLengthResponse(Response.Status.OK,  "application/json",
                String.format(Locale.US, "{\"lat\": %.8f, \"lon\": %.8f, \"alt\": %.3f, \"speed\": %.3f}", position[0], position[1], position[2], position[3]));
    }

    private Response getInfo(){
        double[] info = phoneInfoProvider.getPhoneInfo();

        return newFixedLengthResponse(Response.Status.OK,  "application/json",
                String.format(Locale.US, "{\"battery\": %.0f}", info[1]));
    }
}
