package com.alex.waketohome;

import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.provider.Settings;
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
        // USB-C chargers report as USB or AC. Wireless stays out so the screen can sleep.
        int portPower = BatteryManager.BATTERY_PLUGGED_AC | BatteryManager.BATTERY_PLUGGED_USB;
        try {
            dpm.setGlobalSetting(admin, Settings.Global.STAY_ON_WHILE_PLUGGED_IN,
                    Integer.toString(portPower));
            Log.i(TAG, "Screen stays on while the charging port has power");
        } catch (SecurityException e) {
            Log.w(TAG, "Could not set stay-on-while-plugged-in", e);
        }
    }
}
