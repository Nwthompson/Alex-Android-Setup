package com.alex.waketohome;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;

/** Turns the display on, then opens Alex Phone. */
public class ScreenOnActivity extends Activity {
    static void start(Context context) {
        Intent intent = new Intent(context, ScreenOnActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTurnScreenOn(true);
        setShowWhenLocked(true);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
    }

    @Override
    protected void onResume() {
        super.onResume();
        getWindow().getDecorView().post(this::openHome);
    }

    private void openHome() {
        startActivity(WakeService.alexPhoneIntent());
        finish();
    }
}
