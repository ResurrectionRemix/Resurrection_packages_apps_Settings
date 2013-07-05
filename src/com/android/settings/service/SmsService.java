package com.android.settings.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.os.IBinder;
import android.provider.Telephony.Sms.Intents;
import android.telephony.SmsMessage;

public class SmsService extends Service {

    private final static String TAG = "SmsAutoReceiver";

    private static final String KEY_AUTO_SMS = "auto_sms";
    private static final String SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";

    private static final int DEFAULT_SMS = 0;

    private static boolean mRegistered = false;

    private BroadcastReceiver smsReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (MessagingHelper.inQuietHours(context)) {
                int userAutoSms = returnUserTextSms(context);
                SmsMessage[] msgs = Intents.getMessagesFromIntent(intent);
                SmsMessage msg = msgs[0];
                String incomingNumber = msg.getOriginatingAddress();
                MessagingHelper.checkSmsQualifiers(context, incomingNumber, userAutoSms);
            }
        }
    };

    @Override
    public void onDestroy() {
        unregisterReceiver(smsReceiver);
        mRegistered = false;
        super.onDestroy();
    }

    public static int returnUserTextSms(Context context) {
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(context);
        return Integer.parseInt(prefs.getString(
                KEY_AUTO_SMS, String.valueOf(DEFAULT_SMS)));
    }

    public static boolean isStarted() {
        return mRegistered;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!mRegistered) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(SMS_RECEIVED);
            registerReceiver(smsReceiver, filter);
            mRegistered = true;
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
