package com.tobb.rccar;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.tobb.rccar.control.ControlActivity;

public class MenuActivity extends AppCompatActivity {

    @SuppressLint("ClickableViewAccessibility")
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_menu);

        findViewById(R.id.but_board).setOnClickListener(view -> startActivity(new Intent(this, BoardActivity.class)));
        findViewById(R.id.but_controller).setOnClickListener(view -> startActivity(new Intent(this, ControlActivity.class)));
    }


}
