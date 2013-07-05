package com.android.settings.service;

import android.provider.ContactsContract.PhoneLookup;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.net.Uri;
import android.telephony.SmsManager;

import java.util.Calendar;

import com.android.settings.R;

public class MessagingHelper {

    private final static String TAG = "MessagingHelper";

    private static final String KEY_AUTO_SMS_MESSAGE = "auto_sms_message";

    private static final int DEFAULT_DISABLED = 0;
    private static final int ALL_NUMBERS = 1;
    private static final int CONTACTS_ONLY = 2;

    public static boolean isContact(Context context, String phoneNumber) {
        Uri lookupUri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
        String[] numberProject = {
                PhoneLookup._ID,
                PhoneLookup.NUMBER,
                PhoneLookup.DISPLAY_NAME };
        Cursor c = context.getContentResolver().query(lookupUri, numberProject, null, null, null);
        try {
            if (c.moveToFirst()) {
                return true;
            }
        } finally {
            if (c != null) {
               c.close();
            }
        }
        return false;
    }

    public static boolean inQuietHours(Context context) {
        boolean quietHoursEnabled = Settings.System.getInt(context.getContentResolver(),
                Settings.System.QUIET_HOURS_ENABLED, 0) != 0;
        int quietHoursStart = Settings.System.getInt(context.getContentResolver(),
                Settings.System.QUIET_HOURS_START, 0);
        int quietHoursEnd = Settings.System.getInt(context.getContentResolver(),
                Settings.System.QUIET_HOURS_END, 0);

        if (quietHoursEnabled) {
            if (quietHoursStart == quietHoursEnd) {
                return true;
            }
            // Get the date in "quiet hours" format.
            Calendar calendar = Calendar.getInstance();
            int minutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);
            if (quietHoursEnd < quietHoursStart) {
                // Starts at night, ends in the morning.
                return (minutes > quietHoursStart) || (minutes < quietHoursEnd);
            } else {
                return (minutes > quietHoursStart) && (minutes < quietHoursEnd);
            }
        }
        return false;
    }

    public static void checkSmsQualifiers(Context context, String incomingNumber, int userAutoSms) {
        if (userAutoSms == DEFAULT_DISABLED) {
            return;
        }
        String message = null;
        String defaultSms = context.getResources().getString(
                R.string.quiet_hours_auto_sms_null);
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(context);
        message = prefs.getString(KEY_AUTO_SMS_MESSAGE, defaultSms);
        switch (userAutoSms) {
            case ALL_NUMBERS:
                sendAutoReply(message, incomingNumber);
                break;
            case CONTACTS_ONLY:
                if (isContact(context, incomingNumber)) {
                    sendAutoReply(message, incomingNumber);
                }
                break;
        }
    }

    private static void sendAutoReply(String message, String phoneNumber) {
        SmsManager sms = SmsManager.getDefault();
        try {
            sms.sendTextMessage(phoneNumber, null, message, null, null);
        } catch (IllegalArgumentException e) {
        }
    }
}
