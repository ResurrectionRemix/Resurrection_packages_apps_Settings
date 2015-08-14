package com.android.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class DateChangeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        DateTimeSettings.updateLocaleStrings();
    }
}
