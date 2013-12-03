/*
 * Copyright (C) 2012 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.cmstats;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

public class ReportingServiceManager extends BroadcastReceiver {
    private static final long MILLIS_PER_HOUR = 60L * 60L * 1000L;
    private static final long MILLIS_PER_DAY = 24L * MILLIS_PER_HOUR;
    private static final long UPDATE_INTERVAL = 1L * MILLIS_PER_DAY;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            setAlarm(context, 0);
        } else {
            launchService(context);
        }
    }

    public static void setAlarm(Context context, long millisFromNow) {
        SharedPreferences prefs = AnonymousStats.getPreferences(context);
        boolean optedIn = prefs.getBoolean(AnonymousStats.ANONYMOUS_OPT_IN, true);
        if (!optedIn) {
            return;
        }

        if (millisFromNow <= 0) {
            long lastSynced = prefs.getLong(AnonymousStats.ANONYMOUS_LAST_CHECKED, 0);
            if (lastSynced == 0) {
                // never synced, so let's fake out that the last sync was just now.
                // this will allow the user tFrame time to opt out before it will start
                // sending up anonymous stats.
                lastSynced = System.currentTimeMillis();
                prefs.edit().putLong(AnonymousStats.ANONYMOUS_LAST_CHECKED, lastSynced).apply();
                Log.d(ReportingService.TAG, "Set alarm for first sync.");
            }
            millisFromNow = (lastSynced + UPDATE_INTERVAL) - System.currentTimeMillis();
        }

        Intent intent = new Intent(ConnectivityManager.CONNECTIVITY_ACTION);
        intent.setClass(context, ReportingServiceManager.class);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + millisFromNow,
                PendingIntent.getBroadcast(context, 0, intent, 0));
        Log.d(ReportingService.TAG, "Next sync attempt in : " + millisFromNow / MILLIS_PER_HOUR + " hours");
    }

    public static void launchService(Context context) {
        ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        if (networkInfo == null || !networkInfo.isConnected()) {
            return;
        }

        SharedPreferences prefs = AnonymousStats.getPreferences(context);
        boolean optedIn = prefs.getBoolean(AnonymousStats.ANONYMOUS_OPT_IN, true);
        if (!optedIn) {
            return;
        }
        long lastSynced = prefs.getLong(AnonymousStats.ANONYMOUS_LAST_CHECKED, 0);
        if (lastSynced == 0) {
            setAlarm(context, 0);
            return;
        }
        long timeLeft = System.currentTimeMillis() - lastSynced;
        if (timeLeft < UPDATE_INTERVAL) {
            Log.d(ReportingService.TAG, "Waiting for next sync : " + timeLeft / MILLIS_PER_HOUR + " hours");
            return;
        }

        Intent intent = new Intent();
        intent.setClass(context, ReportingService.class);
        context.startService(intent);
    }
}
