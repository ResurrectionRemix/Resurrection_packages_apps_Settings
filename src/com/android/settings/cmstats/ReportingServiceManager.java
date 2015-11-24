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
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.UserHandle;
import android.util.Log;
import cyanogenmod.providers.CMSettings;

public class ReportingServiceManager extends BroadcastReceiver {
    private static final long MILLIS_PER_HOUR = 60L * 60L * 1000L;
    private static final long MILLIS_PER_DAY = 24L * MILLIS_PER_HOUR;
    private static final long UPDATE_INTERVAL = 1L * MILLIS_PER_DAY;

    private static final String TAG = ReportingServiceManager.class.getSimpleName();

    public static final String ACTION_LAUNCH_SERVICE =
            "com.android.settings.action.TRIGGER_REPORT_METRICS";
    public static final String EXTRA_FORCE = "force";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            setAlarm(context);
        } else if (intent.getAction().equals(ACTION_LAUNCH_SERVICE)){
            launchService(context, intent.getBooleanExtra(EXTRA_FORCE, false));
        }
    }

    /**
     * opt out if we haven't yet
     */
    public static void initiateOptOut(Context context) {
        final boolean optOutReported = CMSettings.Secure.getIntForUser(context.getContentResolver(),
                CMSettings.Secure.STATS_COLLECTION_REPORTED, 0, UserHandle.USER_OWNER) == 1;
        if (!optOutReported) {
            Intent intent = new Intent();
            intent.setClass(context, ReportingService.class);
            intent.putExtra(ReportingService.EXTRA_OPTING_OUT, true);
            context.startServiceAsUser(intent, UserHandle.OWNER);
        }
    }

    public static void setAlarm(Context context) {
        SharedPreferences prefs = AnonymousStats.getPreferences(context);
        if (prefs.contains(AnonymousStats.ANONYMOUS_OPT_IN)) {
            migrate(context, prefs);
        }
        if (!Utilities.isStatsCollectionEnabled(context)) {
            initiateOptOut(context);
            return;
        }
        long lastSynced = prefs.getLong(AnonymousStats.ANONYMOUS_LAST_CHECKED, 0);
        if (lastSynced == 0) {
            launchService(context, true); // service will reschedule the next alarm
            return;
        }
        long millisFromNow = (lastSynced + UPDATE_INTERVAL) - System.currentTimeMillis();

        Intent intent = new Intent(ACTION_LAUNCH_SERVICE);
        intent.setClass(context, ReportingServiceManager.class);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + millisFromNow,
                PendingIntent.getBroadcast(context, 0, intent, 0));
        Log.d(TAG, "Next sync attempt in : "
                + (millisFromNow / MILLIS_PER_HOUR) + " hours");
    }

    public static void launchService(Context context, boolean force) {
        SharedPreferences prefs = AnonymousStats.getPreferences(context);

        if (!Utilities.isStatsCollectionEnabled(context)) {
            return;
        }

        if (!force) {
            long lastSynced = prefs.getLong(AnonymousStats.ANONYMOUS_LAST_CHECKED, 0);
            if (lastSynced == 0) {
                setAlarm(context);
                return;
            }
            long timeElapsed = System.currentTimeMillis() - lastSynced;
            if (timeElapsed < UPDATE_INTERVAL) {
                long timeLeft = UPDATE_INTERVAL - timeElapsed;
                Log.d(TAG, "Waiting for next sync : "
                        + timeLeft / MILLIS_PER_HOUR + " hours");
                return;
            }
        }

        Intent intent = new Intent();
        intent.setClass(context, ReportingService.class);
        context.startServiceAsUser(intent, UserHandle.OWNER);
    }

    private static void migrate(Context context, SharedPreferences prefs) {
        Utilities.setStatsCollectionEnabled(context,
                prefs.getBoolean(AnonymousStats.ANONYMOUS_OPT_IN, true));
        prefs.edit().remove(AnonymousStats.ANONYMOUS_OPT_IN).commit();
    }

}
