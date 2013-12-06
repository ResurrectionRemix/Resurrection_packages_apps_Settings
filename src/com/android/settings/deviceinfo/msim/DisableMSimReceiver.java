/*
 * Copyright (C) 2013 The CyanogenMod Project
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

package com.android.settings.deviceinfo.msim;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.telephony.MSimTelephonyManager;
import android.util.Log;

// We want to disable multi-SIM activities if it is unsupported. This
// receiver runs when BOOT_COMPLETED intent is received.
public class DisableMSimReceiver extends BroadcastReceiver {
    private static final String TAG = "DisableMSimReceiver";
    private static final String ACTIVITIES[] = {
        "com.android.settings.MultiSimSettings",
    };

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "enabling/disabling multisim activities");
        for (String activity : ACTIVITIES) {
            enableComponent(context, activity,
                    MSimTelephonyManager.getDefault().isMultiSimEnabled());
        }
    }

    private void enableComponent(Context context, String klass, boolean enable) {
        ComponentName name = new ComponentName(context, klass);
        PackageManager pm = context.getPackageManager();

        if (enable) {
            pm.setComponentEnabledSetting(name,
                    PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
                    PackageManager.DONT_KILL_APP);
        } else {
            pm.setComponentEnabledSetting(name,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);
        }
    }
}
