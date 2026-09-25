package com.alex.waketohome;

import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class WakeDeviceAdmin extends DeviceAdminReceiver {
    private static final String TAG = "WakeToHome";

    static ComponentName admin(Context context) {
        return new ComponentName(context, WakeDeviceAdmin.class);
    }

    @Override
    public void onEnabled(Context context, Intent intent) {
        apply(context);
        WakeService.start(context);
    }

    static void apply(Context context) {
        DevicePolicyManager dpm = context.getSystemService(DevicePolicyManager.class);
        ComponentName admin = admin(context);
        if (dpm == null || !dpm.isDeviceOwnerApp(context.getPackageName())) {
            Log.i(TAG, "Not device owner yet");
            return;
        }
        try {
            dpm.setKeyguardDisabled(admin, true);
            Log.i(TAG, "Keyguard disabled");
        } catch (SecurityException e) {
            Log.i(TAG, "Keyguard stays up because a credential is set", e);
        }
    }
}
