package com.tobb.rccar;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.common.util.concurrent.ListenableFuture;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import android.util.Log;
import android.util.Size;
import android.view.View;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import fi.iki.elonen.NanoHTTPD;

public class MainActivity extends AppCompatActivity implements PhoneInfoProvider{

    private CarServer server;
    private PreviewView mPreviewView;
    private ImageCapture imageCapture;
    private LocationManager locationManager;

    private Executor cameraExecutor = Executors.newSingleThreadExecutor();

    private static final Pattern IP_PATTERN = Pattern.compile(
            "^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");


    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        TextView tv = findViewById(R.id.tv_state);
        MyLogger logger = new MyLogger(tv, this);
        mPreviewView = findViewById(R.id.camera);

        locationManager = (LocationManager)
                getSystemService(Context.LOCATION_SERVICE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 10);
        }

        int port = 8080;
        logger.log("Will listen on: " + getLocalIpAddress() + ":" + port);
        server = new CarServer(port, logger, this);

        try {
            server.start();
        } catch(IOException ioe) {
            System.out.println("CAR SERVER " + "The server could not start." + ioe.getMessage());
        }
        System.out.println("CAR SERVER " + "Web server initialized.");

        startCamera();
    }

    public String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && isValidIPv4(inetAddress.getHostAddress())) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException ex) {
            System.out.println("CAR SERVER " + ex.toString());
        }
        return null;
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        if (server != null)
            server.stop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public static boolean isValidIPv4(final String ip) {
        return IP_PATTERN.matcher(ip).matches();
    }

    private boolean allPermissionsGranted(){

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            return false;
        }
        return true;
    }

    private void askPermission(){
        if(ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE}, 101);
        }
    }

    private void startCamera() {
        if(!allPermissionsGranted()){
            System.out.println("Not all permissions");
            askPermission();
            return;
        }

        final ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {

                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);

            } catch (ExecutionException | InterruptedException e) {
            }
        }, ContextCompat.getMainExecutor(this));
    }

    void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {

        Preview preview = new Preview.Builder()
                .build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .build();

        ImageCapture.Builder builder = new ImageCapture.Builder().setTargetResolution(new Size(720/2, 1280/2));


        imageCapture = builder
                .setTargetRotation(this.getWindowManager().getDefaultDisplay().getRotation())
                .build();
        preview.setSurfaceProvider(mPreviewView.getSurfaceProvider());
        Camera camera = cameraProvider.bindToLifecycle((LifecycleOwner)this, cameraSelector, preview, imageAnalysis, imageCapture);
    }

    @Override
    public double[] getLocation(int timeout) {
        final CountDownLatch latch = new CountDownLatch(1);
        double[] position  = new double[3];
        runOnUiThread(() -> {
            try {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    position[2] = -1;
                }
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, location -> {
                    double lat = location.getLatitude();
                    double lon = location.getLongitude();
                    position[0] = lat;
                    position[1] = lon;
                    position[2] = 1;
                    latch.countDown();
                });
            }catch (Exception e){
                System.out.println("CAR SERVER " + e.getMessage());
            }
        });

        try {
            latch.await(timeout, TimeUnit.SECONDS);
        } catch(Exception exception){
            return null;
        }
        return position;
    }

    @Override
    public File getCameraPreview() {
        final CountDownLatch latch = new CountDownLatch(1);
        final File file = new File(getExternalCacheDir(), "pic.jpg");

        runOnUiThread(() -> {
            try (FileOutputStream out = new FileOutputStream(file)) {
                System.out.println("CAR SERVER " + mPreviewView.getBitmap());
                mPreviewView.getBitmap().compress(Bitmap.CompressFormat.PNG, 100, out);
            } catch (IOException e) {
                e.printStackTrace();
            }
            latch.countDown();
        });

        try {
            latch.await(1, TimeUnit.SECONDS);
        } catch(Exception exception){

        }
        return file;
    }

    @Override
    public File getCamera() {
        final CountDownLatch latch = new CountDownLatch(1);
        final File file = new File(getExternalCacheDir(), "pic.jpg");
        final boolean status[] = {false};

        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(file).build();
        imageCapture.takePicture(outputFileOptions, cameraExecutor, new ImageCapture.OnImageSavedCallback () {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                status[0] = true;
                latch.countDown();
            }

            @Override
            public void onError(@NonNull ImageCaptureException error) {
                latch.countDown();
            }
        });

        try {
            latch.await(1, TimeUnit.SECONDS);
        } catch(Exception exception){

        }
        return file;
    }
}