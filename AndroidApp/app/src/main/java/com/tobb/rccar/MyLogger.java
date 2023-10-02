package com.tobb.rccar;

import android.app.Activity;
import android.os.AsyncTask;
import android.widget.TextView;

public class MyLogger {

    private String text = "";
    private TextView textView;
    private Activity activity;

    public MyLogger(TextView textView, Activity activity){
        this.textView= textView;
        this.activity = activity;
    }

    public void log(String message){
        text += message + '\n';
        activity.runOnUiThread(() -> textView.setText(text));
    }


}
