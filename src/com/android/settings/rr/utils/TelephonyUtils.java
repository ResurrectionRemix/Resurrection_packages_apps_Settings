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
package com.android.settings.rr.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.telephony.TelephonyManager;
import android.util.Log;

public class TelephonyUtils {

    private static final String TAG = TelephonyUtils.class.getSimpleName();

    /**
     * Returns whether the device is voice-capable (meaning, it is also a phone).
     */
    public static boolean isVoiceCapable(Context context) {
        TelephonyManager telephony = context.getSystemService(TelephonyManager.class);
        return telephony != null && telephony.isVoiceCapable();
    }

    private static Resources getPhoneResources(Context context) {
        try {
            final Context packageContext = context.createPackageContext("com.android.phone", 0);
            return packageContext.getResources();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        Log.w(TAG, "couldn't locate resources for com.android.phone!");
        return null;
    }
}
