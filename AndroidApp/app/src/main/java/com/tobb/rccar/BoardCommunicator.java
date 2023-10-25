package com.tobb.rccar;

import android.app.Activity;

public interface BoardCommunicator {

    void sendDriveCommand(byte motor, byte steering);


    void onResume(Activity activity);
    void onDestroy(Activity activity);
}
