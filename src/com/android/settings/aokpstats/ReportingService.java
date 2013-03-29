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

package com.android.settings.aokpstats;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.Settings;

public class ReportingService extends Service {
    protected static final String TAG = "AOKPStats";

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand (Intent intent, int flags, int startId) {
        if (intent.getBooleanExtra("firstBoot", false)) {
            promptUser();
            Log.d(TAG, "Prompting user for opt-in.");
        } else {
            Log.d(TAG, "User has opted in -- reporting.");
            Thread thread = new Thread() {
                @Override
                public void run() {
                    report();
                }
            };
            thread.start();
        }
        return Service.START_REDELIVER_INTENT;
    }

    private void report() {
        String deviceId = Utilities.getUniqueID(getApplicationContext());
        String deviceName = Utilities.getDevice();
        String deviceVersion = Utilities.getModVersion();
        String deviceCountry = Utilities.getCountryCode(getApplicationContext());
        String deviceCarrier = Utilities.getCarrier(getApplicationContext());
        String deviceCarrierId = Utilities.getCarrierId(getApplicationContext());

        Log.d(TAG, "SERVICE: Device ID=" + deviceId);
        Log.d(TAG, "SERVICE: Device Name=" + deviceName);
        Log.d(TAG, "SERVICE: Device Version=" + deviceVersion);
        Log.d(TAG, "SERVICE: Country=" + deviceCountry);
        Log.d(TAG, "SERVICE: Carrier=" + deviceCarrier);
        Log.d(TAG, "SERVICE: Carrier ID=" + deviceCarrierId);

        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost("http://stats.aokp.co/submit.php");
        try {
            List<NameValuePair> kv = new ArrayList<NameValuePair>(5);
            kv.add(new BasicNameValuePair("hash", deviceId));
            kv.add(new BasicNameValuePair("device", deviceName));
            kv.add(new BasicNameValuePair("aokp_version", deviceVersion));
            kv.add(new BasicNameValuePair("country", deviceCountry));
            kv.add(new BasicNameValuePair("carrier", deviceCarrier));
            kv.add(new BasicNameValuePair("carrier_id", deviceCarrierId));
            httppost.setEntity(new UrlEncodedFormEntity(kv));
            httpclient.execute(httppost);
            getSharedPreferences("AOKPStats", 0).edit().putLong(AnonymousStats.ANONYMOUS_LAST_CHECKED,
                    System.currentTimeMillis()).apply();
            getSharedPreferences("AOKPStats", 0).edit().putString(AnonymousStats.ANONYMOUS_REPORTED_VERSION,
                    deviceVersion).apply();
        } catch (Exception e) {
            Log.e(TAG, "Got Exception", e);
        }
        ReportingServiceManager.setAlarm(this);
        stopSelf();
    }

    private void promptUser() {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Intent nI = new Intent();
        nI.setComponent(new ComponentName(getPackageName(),Settings.AnonymousStatsActivity.class.getName()));
        PendingIntent pI = PendingIntent.getActivity(this, 0, nI, 0);
        Notification.Builder builder = new Notification.Builder(this)
        .setSmallIcon(R.drawable.ic_aokp_stats_notif)
        .setAutoCancel(true)
        .setTicker(getString(R.string.anonymous_statistics_title))
        .setContentIntent(pI)
        .setWhen(0)
        .setContentTitle(getString(R.string.anonymous_statistics_title))
        .setContentText(getString(R.string.anonymous_notification_desc));
        nm.notify(1, builder.getNotification());
    }
}

