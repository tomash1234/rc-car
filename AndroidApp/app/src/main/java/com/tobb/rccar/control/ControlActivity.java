package com.tobb.rccar.control;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.tobb.rccar.R;

public class ControlActivity extends AppCompatActivity implements ReceiveResponse {

    private TextView tvInfo, tvConnectionInfo, tvGPS;
    private PhoneCommunicator phoneCommunicator;
    private CountDownTimer countDownInfoUpdater, countDownDriveUpdater;

    private int motor = 0;
    private int steering = 0;
    private static final int[] BUTTON_IDS = {R.id.but_forward, R.id.but_backward, R.id.but_go_left, R.id.but_go_right};
    private boolean[] buttonPressed = new boolean[BUTTON_IDS.length];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.control_activity);

        tvInfo = findViewById(R.id.tv_info);
        tvConnectionInfo = findViewById(R.id.tv_connect_info);
        tvGPS = findViewById(R.id.tv_position);
        phoneCommunicator = new PhoneCommunicator(this, this);
        EditText editText = findViewById(R.id.et_address);
        editText.setText("http://192.168.14.169:8088");

        findViewById(R.id.but_connect).setOnClickListener(view ->
        {
            runOnUiThread(() -> tvConnectionInfo.setText("Connecting..."));
            phoneCommunicator.setUrl(editText.getText().toString());
            phoneCommunicator.testConnection();
        });

        countDownInfoUpdater = new CountDownTimer(Long.MAX_VALUE, 1000){

            @Override
            public void onTick(long l) {
                if(phoneCommunicator != null && phoneCommunicator.isConnected()){
                    updateGpsAndInfo();
                }
            }

            @Override
            public void onFinish() {}
        };
        countDownInfoUpdater.start();

        countDownDriveUpdater = new CountDownTimer(Long.MAX_VALUE, 250){

            @Override
            public void onTick(long l) {
                if(phoneCommunicator != null && phoneCommunicator.isConnected()){
                    updateDrive();
                }
            }

            @Override
            public void onFinish() {}
        };
        countDownDriveUpdater.start();

        for(int i = 0; i < BUTTON_IDS.length; i++) {
            int id = BUTTON_IDS[i];
            final int index = i;
            Button button = findViewById(id);
            button.setOnTouchListener((view, motionEvent) -> {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    buttonPressed[index] = true;
                } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    buttonPressed[index] = false;
                }
                return true;
            });
        }
    }

    private void updateGpsAndInfo(){
        phoneCommunicator.getInfo();
        phoneCommunicator.getPosition();
    }

    private void updateDrive() {
        int motor = 0;
        int steering = 0;
        if (buttonPressed[0]) {
            motor = 1;
        } else if (buttonPressed[1]){
            motor = -1;
        }
        if (buttonPressed[2]) {
            steering = 1;
        } else if (buttonPressed[3]){
            steering = -1;
        }
        phoneCommunicator.drive(motor, steering);
    }


    @Override
    public void receive(int tag, String content, int responseCode, boolean error) {
        if(tag == PhoneCommunicator.TAG_INFO){
            if(error){
                content= "Error " + responseCode;
            }
            final String text = content;
            runOnUiThread(() -> tvInfo.setText(text));
        }else if(tag == PhoneCommunicator.TAG_POSITION){
            if(error){
                content= "Error " + responseCode;
            }
            final String text = content;
            runOnUiThread(() -> tvGPS.setText(text));
        }else if(tag == PhoneCommunicator.TAG_TEST){
            if(error){
                content = "Connection failed " + content;
            }else{
                content = "Connected!";
            }
            final String text = content;
            runOnUiThread(() -> tvConnectionInfo.setText(text));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        countDownInfoUpdater.cancel();
        countDownDriveUpdater.cancel();
    }

    @Override
    protected void onResume() {
        super.onResume();
        countDownInfoUpdater.start();
        countDownDriveUpdater.start();
    }
}
