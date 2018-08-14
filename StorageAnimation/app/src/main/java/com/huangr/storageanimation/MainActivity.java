package com.huangr.storageanimation;

import android.app.Activity;
import android.os.Bundle;

public class MainActivity extends Activity {

    private WaterView waterView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        waterView = (WaterView)findViewById(R.id.waterview);
    }

    @Override
    protected void onStart() {
        super.onStart();
        waterView.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        waterView.reset();
    }
}
