package com.android.settings.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (CallService.returnUserCallSms(context) != 0) {
            context.startService(new Intent(context, CallService.class));
        }
        if (SmsService.returnUserTextSms(context) != 0) {
            context.startService(new Intent(context, SmsService.class));
        }
    }

}
