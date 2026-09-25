package com.alex.waketohome;

import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
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
        try {
            dpm.setPermissionPolicy(admin, DevicePolicyManager.PERMISSION_POLICY_AUTO_GRANT);
            grantRuntimePermissions(context, dpm, admin, null);
            Log.i(TAG, "Runtime permissions are granted without a prompt");
        } catch (SecurityException e) {
            Log.w(TAG, "Could not auto-grant runtime permissions", e);
        }
    }

    /** Grants every dangerous runtime permission. A null package grants them for every installed app. */
    static void grantRuntimePermissions(Context context, String packageName) {
        DevicePolicyManager dpm = context.getSystemService(DevicePolicyManager.class);
        ComponentName admin = admin(context);
        if (dpm == null || !dpm.isDeviceOwnerApp(context.getPackageName())) {
            return;
        }
        grantRuntimePermissions(context, dpm, admin, packageName);
    }

    private static void grantRuntimePermissions(Context context, DevicePolicyManager dpm,
            ComponentName admin, String packageName) {
        PackageManager packages = context.getPackageManager();
        for (PackageInfo info : packages.getInstalledPackages(PackageManager.GET_PERMISSIONS)) {
            if (packageName != null && !packageName.equals(info.packageName)) {
                continue;
            }
            if (info.requestedPermissions == null) {
                continue;
            }
            for (String permission : info.requestedPermissions) {
                try {
                    PermissionInfo declared = packages.getPermissionInfo(permission, 0);
                    if (declared.getProtection() != PermissionInfo.PROTECTION_DANGEROUS) {
                        continue;
                    }
                    dpm.setPermissionGrantState(admin, info.packageName, permission,
                            DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED);
                } catch (PackageManager.NameNotFoundException ignored) {
                    // A requested permission may belong to an app that is not installed.
                }
            }
        }
    }
}
