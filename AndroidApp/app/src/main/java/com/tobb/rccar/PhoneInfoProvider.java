package com.tobb.rccar;

import java.io.File;

public interface PhoneInfoProvider {

    double[] getLocation(int timeout);

    File getCamera();

    File getCameraPreview();

    double[] getPhoneInfo();

    void setStream(String ipAddress, int port);

    boolean startStream();

    void stopStream();


}
