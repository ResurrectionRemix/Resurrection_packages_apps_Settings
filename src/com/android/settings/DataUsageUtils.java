/*
 * Copyright (C) 2016 The CyanogenMod project
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

package com.android.settings;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;

import cyanogenmod.providers.DataUsageContract;

/**
 * This class contains utility helper functions for accessing DataUsageProvider
 */
public class DataUsageUtils {
    private static final String TAG = DataUsageUtils.class.getSimpleName();
    private static final String WHERE_CLAUSE = DataUsageContract.UID + " = ? ";
    private static final int DATAUSAGE_SERVICE_ALARM_ID = 0x102030;
    private static boolean DEBUG = true;

    public static void addApp(Context context, int uid, String label) {
        if (DEBUG) {
            Log.v(TAG, "addApp: uid:" + uid + " label:" + label);
        }

        ContentValues values = new ContentValues();

        values.put(DataUsageContract.UID, uid);
        values.put(DataUsageContract.LABEL, label);

        context.getContentResolver().insert(
                DataUsageContract.CONTENT_URI,
                values
        );
    }

    public static void removeApp(Context context, int uid) {
        if (DEBUG) {
            Log.v(TAG, "removeApp: uid:" + uid);
        }
        context.getContentResolver().delete(
                DataUsageContract.CONTENT_URI,
                WHERE_CLAUSE,
                new String [] { String.valueOf(uid) }
        );
    }

    public static void enableApp(Context context, int uid, boolean enable) {
        enableApp(context, uid, enable, null);
    }

    public static void enableApp(Context context, int uid, boolean enable, String label) {
        if (DEBUG) {
            Log.v(TAG, "enableApp: uid:" + uid + " enable:" + enable +
                    (label == null ? "" : " label:" + label));
        }
        ContentValues values = new ContentValues();

        values.put(DataUsageContract.ENABLE, enable);
        values.put(DataUsageContract.ACTIVE, 0);
        if (label != null) {
            values.put(DataUsageContract.LABEL, label);
        }
        context.getContentResolver().update(
                DataUsageContract.CONTENT_URI,
                values,
                WHERE_CLAUSE,
                new String [] { String.valueOf(uid) }
        );
    }

    public static boolean isDbEnabled(Context context) {
        boolean dbEnabled = false;
        try (Cursor cursor = context.getContentResolver().query(
                DataUsageContract.CONTENT_URI,
                null,
                WHERE_CLAUSE,
                new String [] { "0" },
                null)) {
            dbEnabled = true;
        }
        return dbEnabled;
    }

    public static boolean isAppEnabled(Context context, int uid) {
        boolean appEnabled = false;

        try (Cursor cursor = context.getContentResolver().query(
                DataUsageContract.CONTENT_URI,
                null,
                WHERE_CLAUSE,
                new String [] { String.valueOf(uid) },
                null)) {
            if (cursor.moveToFirst()) {
                appEnabled = cursor.getInt(DataUsageContract.COLUMN_OF_ENABLE) == 1;
            }
        }

        if (DEBUG) {
            Log.v(TAG, "isAppEnabled: uid:" + uid + " enabled:" + appEnabled);
        }

        return appEnabled;
    }

    public static void enableDataUsageService(Context context, boolean enable) {
        Intent intent = new Intent();
        intent.setAction("org.cyanogenmod.providers.datausage.enable");
        intent.putExtra("enable", enable);
        context.sendBroadcast(intent);
    }
}
