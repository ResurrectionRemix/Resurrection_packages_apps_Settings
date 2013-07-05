package com.android.settings.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

public class CallService extends Service {

    private final static String TAG = "CallAutoReceiver";

    private static final String KEY_AUTO_SMS_CALL = "auto_sms_call";

    private static final int DEFAULT_CALL = 0;

    private boolean mReceived = false;

    private static boolean mRegistered = false;

    private static TelephonyManager mTelephony;

    private Context mContext;

    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            if (state == TelephonyManager.CALL_STATE_RINGING) {
                mReceived = true;
            }
            if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
                // Don't message if call was answered
                mReceived = false;
            }
            if (state == TelephonyManager.CALL_STATE_IDLE && mReceived) {
                // Call Received and now inactive
                mReceived = false;
                if (MessagingHelper.inQuietHours(mContext)) {
                    int userAutoSms = returnUserCallSms(mContext);
                    MessagingHelper.checkSmsQualifiers(mContext, incomingNumber, userAutoSms);
                }
            }
            super.onCallStateChanged(state, incomingNumber);
        }
    };

    @Override
    public void onDestroy() {
        if (mTelephony != null) {
            mTelephony.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
        }
        mPhoneStateListener = null;
        mRegistered = false;
        super.onDestroy();
    }

    public static int returnUserCallSms(Context context) {
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(context);
        return Integer.parseInt(prefs.getString(
                KEY_AUTO_SMS_CALL, String.valueOf(DEFAULT_CALL)));
    }

    public static boolean isStarted() {
        return mRegistered;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!mRegistered) {
            mContext = getApplicationContext();
            mTelephony = (TelephonyManager) mContext.
                    getSystemService(Context.TELEPHONY_SERVICE);
            mTelephony.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
            mRegistered = true;
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
