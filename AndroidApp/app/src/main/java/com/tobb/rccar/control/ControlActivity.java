package com.tobb.rccar.control;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.tobb.rccar.R;

public class ControlActivity extends AppCompatActivity implements ReceiveResponse {

    private TextView tvInfo, tvConnectionInfo;
    private PhoneCommunicator phoneCommunicator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.control_activity);

        tvInfo = findViewById(R.id.tv_info);
        tvConnectionInfo = findViewById(R.id.tv_connect_info);
        phoneCommunicator = new PhoneCommunicator(this, this);
        EditText editText = findViewById(R.id.et_address);
        editText.setText("http://192.168.14.169:8088");

        findViewById(R.id.but_connect).setOnClickListener(view ->
        {
            runOnUiThread(() -> tvConnectionInfo.setText("Connecting..."));
            phoneCommunicator.setUrl(editText.getText().toString());
            phoneCommunicator.testConnection();
        });
    }



    @Override
    public void receive(int tag, String content, int responseCode, boolean error) {
        if(tag == PhoneCommunicator.TAG_INFO){
            if(error){
                content= "Error " + responseCode;
            }
            final String text = content;
            runOnUiThread(() -> tvInfo.setText(text));
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
}
