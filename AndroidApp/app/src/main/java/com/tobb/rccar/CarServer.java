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

    public CarServer(int port, MyLogger myLogger, PhoneInfoProvider phoneInfoProvider) {
        super(port);
        this.myLogger = myLogger;
        this.phoneInfoProvider = phoneInfoProvider;
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
            }
        } else if (method.equals("POST")) {
            switch (uri) {
                case "/drive":
                    response = postDrive(session);
                    break;
            }
        }
        if (response != null) {
            myLogger.log(method + " - " + uri + " -" + session.getQueryParameterString());
        } else {
            response = newFixedLengthResponse(Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT,
                    "Page \"" + uri + "\" not exists");
        }

        return response;
    }

    private void obtainPosition(){

    }

    private Response postDrive(IHTTPSession session) {
        return newFixedLengthResponse(Response.Status.OK, NanoHTTPD.MIME_PLAINTEXT, "drive");
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
        if(position[2] < 0){
            return  newFixedLengthResponse(Response.Status.CONFLICT,  NanoHTTPD.MIME_PLAINTEXT, "GPS permissions not granted");
        }
        if(position[2] < 0.5){
            return newFixedLengthResponse(Response.Status.CONFLICT,  NanoHTTPD.MIME_PLAINTEXT, "GPS could not be obtained before timeout. [" + timeout + "s]" );
        }
        return newFixedLengthResponse(Response.Status.OK,  "application/json",
                String.format(Locale.US, "{\"lat\": %.8f, \"lon\": %.8f}", position[0], position[1] ));
    }

    private Response getInfo(){
        return newFixedLengthResponse(Response.Status.OK,  NanoHTTPD.MIME_PLAINTEXT, "info");
    }
}
