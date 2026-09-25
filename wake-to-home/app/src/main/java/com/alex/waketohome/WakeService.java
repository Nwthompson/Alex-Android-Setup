package com.alex.waketohome;

import android.content.ComponentName;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ServiceInfo;
import android.hardware.display.DisplayManager;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.Display;

public class WakeService extends Service {
    private static final String CHANNEL = "wake";
    private static final String TAG = "WakeToHome";
    private DisplayManager displays;
    private int lastState = Display.STATE_UNKNOWN;

    private final BroadcastReceiver packageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getData() == null) {
                return;
            }
            WakeDeviceAdmin.grantRuntimePermissions(context, intent.getData().getSchemeSpecificPart());
        }
    };

    private final BroadcastReceiver screenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_SCREEN_ON.equals(action)) {
                onScreenOn();
            } else if (Intent.ACTION_POWER_CONNECTED.equals(action)) {
                if (portPowered()) {
                    wakeFromPort();
                } else {
                    turnScreenOff();
                }
            } else if (Intent.ACTION_POWER_DISCONNECTED.equals(action)) {
                if (!portPowered()) {
                    turnScreenOff();
                }
            }
        }
    };

    private final DisplayManager.DisplayListener listener = new DisplayManager.DisplayListener() {
        @Override
        public void onDisplayAdded(int displayId) {
        }

        @Override
        public void onDisplayRemoved(int displayId) {
        }

        @Override
        public void onDisplayChanged(int displayId) {
            if (displayId != Display.DEFAULT_DISPLAY || displays == null) {
                return;
            }
            Display display = displays.getDisplay(displayId);
            if (display == null) {
                return;
            }
            int state = display.getState();
            if (state == Display.STATE_ON && lastState != Display.STATE_ON) {
                onScreenOn();
            }
            lastState = state;
        }
    };

    static void start(Context context) {
        context.startForegroundService(new Intent(context, WakeService.class));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationManager notifications = getSystemService(NotificationManager.class);
        notifications.createNotificationChannel(new NotificationChannel(
                CHANNEL, "Wake to home", NotificationManager.IMPORTANCE_MIN));
        Notification notification = new Notification.Builder(this, CHANNEL)
                .setContentTitle("Wake to home")
                .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
                .build();
        startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED);

        displays = getSystemService(DisplayManager.class);
        Display display = displays.getDisplay(Display.DEFAULT_DISPLAY);
        if (display != null) {
            lastState = display.getState();
        }
        displays.registerDisplayListener(listener, new Handler(Looper.getMainLooper()));
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        registerReceiver(screenReceiver, filter);
        IntentFilter packages = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
        packages.addDataScheme("package");
        registerReceiver(packageReceiver, packages);
        if (portPowered()) {
            wakeFromPort();
        } else {
            turnScreenOff();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(screenReceiver);
        unregisterReceiver(packageReceiver);
        if (displays != null) {
            displays.unregisterDisplayListener(listener);
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void onScreenOn() {
        if (portPowered()) {
            openHome();
        } else {
            turnScreenOff();
        }
    }

    private void wakeFromPort() {
        Display display = displays == null ? null : displays.getDisplay(Display.DEFAULT_DISPLAY);
        if (display != null && display.getState() == Display.STATE_ON) {
            openHome();
            return;
        }
        Log.i(TAG, "Charging port has power; turning the screen on");
        ScreenOnActivity.start(this);
    }

    private void turnScreenOff() {
        DevicePolicyManager dpm = getSystemService(DevicePolicyManager.class);
        if (dpm == null || !dpm.isDeviceOwnerApp(getPackageName())) {
            return;
        }
        Log.i(TAG, "Charging port has no power; turning the screen off");
        dpm.lockNow();
    }

    private boolean portPowered() {
        Intent battery = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (battery == null) {
            return false;
        }
        int plugged = battery.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
        return (plugged & (BatteryManager.BATTERY_PLUGGED_USB | BatteryManager.BATTERY_PLUGGED_AC)) != 0;
    }

    private void openHome() {
        Log.i(TAG, "Opening Alex Phone");
        startActivity(alexPhoneIntent());
    }

    static Intent alexPhoneIntent() {
        Intent app = new Intent(Intent.ACTION_MAIN);
        app.setComponent(new ComponentName(
                "us.ihmc.alexcommands.phone",
                "us.ihmc.alexCommands.phone.PhoneActivity"));
        app.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return app;
    }
}
