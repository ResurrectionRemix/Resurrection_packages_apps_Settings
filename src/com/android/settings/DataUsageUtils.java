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
                DataUsageContract.UID + " = ? ",
                new String [] { String.valueOf(uid)}
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
                DataUsageContract.UID + " = ? ",
                new String [] { String.valueOf(uid)}
        );
    }

    public static boolean isDbEnabled(Context context) {
        boolean dbEnabled = false;
        Cursor cursor = context.getContentResolver().query(
                DataUsageContract.CONTENT_URI,
                null,
                DataUsageContract.UID + " = ? ",
                new String [] { String.valueOf("0") },
                null
        );

        if (cursor != null) {
            cursor.close();
            dbEnabled = true;
        }
        return dbEnabled;
    }


    public static boolean isAppEnabled(Context context, int uid) {
        boolean appEnabled = false;
        Cursor cursor = context.getContentResolver().query(
                DataUsageContract.CONTENT_URI,
                null,
                DataUsageContract.UID + " = ? ",
                new String [] { String.valueOf(uid) },
                null
        );
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                appEnabled = cursor.getInt(DataUsageContract.COLUMN_OF_ENABLE) == 1;
            }
            cursor.close();
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
