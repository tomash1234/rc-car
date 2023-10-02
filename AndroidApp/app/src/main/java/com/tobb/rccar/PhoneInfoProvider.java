package com.tobb.rccar;

import java.io.File;

public interface PhoneInfoProvider {

    double[] getLocation(int timeout);

    File getCamera();

    File getCameraPreview();

}
