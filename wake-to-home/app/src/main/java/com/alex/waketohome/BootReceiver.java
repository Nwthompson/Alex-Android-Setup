package com.alex.waketohome;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        WakeDeviceAdmin.apply(context);
        WakeService.start(context);
    }
}
