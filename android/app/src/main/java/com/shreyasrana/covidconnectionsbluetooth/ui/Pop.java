package com.shreyasrana.covidconnectionsbluetooth.ui;

import android.app.Activity;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.DisplayMetrics;
import android.widget.TextView;

import com.shreyasrana.covidconnectionsbluetooth.R;

public class Pop extends Activity {

    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.popwindow);

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        int width = dm.widthPixels;
        int height = dm.heightPixels;

        TextView link = findViewById(R.id.link);
        link.setMovementMethod(LinkMovementMethod.getInstance());

        getWindow().setLayout((int)(width*0.8), (int)(height*0.8));
    }
}
