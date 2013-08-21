/*
 * Copyright (C) 2013 Android Open Kang Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.service;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import com.android.settings.R;

public class BypassAlarm extends Activity implements OnDismissListener {

    private KeyguardManager mKeyguardManager;

    private boolean mFirstRun;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mFirstRun = true;
        mKeyguardManager =
                (KeyguardManager) this.getSystemService(Context.KEYGUARD_SERVICE);

        final Window win = getWindow();
        win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);

        Bundle extras = getIntent().getExtras();
        String phoneNumber = extras.getString("number");
        startAlertDialog(phoneNumber);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mFirstRun) {
            startService();
        }
        mFirstRun = false;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        startService();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopService();
    }

    @Override
    public void onPause() {
        keyguardStop();
        super.onPause();
    }

    @Override
    public void onStop() {
        keyguardStop();
        super.onStop();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        finish();
    }

    private void startAlertDialog(String phoneNumber) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(R.string.quiet_hours_alarm_dialog_title);
        alert.setMessage(phoneNumber + getResources().getString(
                R.string.quiet_hours_alarm_message));
        alert.setPositiveButton(getResources().getString(R.string.quiet_hours_alarm_dismiss),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        finish();
                    }
                });
        alert.setOnDismissListener(this);
        alert.show();
    }

    private void startService() {
        Intent serviceIntent = new Intent(this, AlarmService.class);
        this.startService(serviceIntent);
    }

    private void stopService() {
        Intent serviceIntent = new Intent(this, AlarmService.class);
        this.stopService(serviceIntent);
    }

    private void keyguardStop() {
        boolean screenLocked =
                mKeyguardManager != null && mKeyguardManager.isKeyguardLocked();
        if (!screenLocked) {
            stopService();
        }
    }
}
