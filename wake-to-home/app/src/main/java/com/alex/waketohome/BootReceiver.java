package com.alex.waketohome;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        WakeService.start(context);
        WakeDeviceAdmin.apply(context);
    }
}
