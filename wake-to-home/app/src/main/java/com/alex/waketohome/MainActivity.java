package com.alex.waketohome;

import android.app.Activity;
import android.os.Bundle;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WakeService.start(this);
        WakeDeviceAdmin.apply(this);
        finish();
    }
}
