package com.android.settings.rr.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.android.settings.util.CMDProcessor;

import java.io.IOException;


public class OnBoot extends BroadcastReceiver {

    Context settingsContext = null;
    private static final String TAG = "RR_onboot";

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            settingsContext = context.createPackageContext("com.android.settings", 0);
        } catch (Exception e) {
            Log.e(TAG, "Package not found", e);
        }
        SharedPreferences sharedpreferences = settingsContext.getSharedPreferences("com.android.settings_preferences", Context.MODE_PRIVATE);
        if(sharedpreferences.getBoolean("selinux", true)) {
            CMDProcessor.runSuCommand("setenforce 1");
        } else if (!sharedpreferences.getBoolean("selinux", true)) {
            CMDProcessor.runSuCommand("setenforce 0");
        }
    }
}
