package com.shreyasrana.covidconnectionsbluetooth.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.shreyasrana.covidconnectionsbluetooth.R;

public class IntroActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);
    }

    public void help(View v){
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }
}
