/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.gestures;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.support.v7.preference.Preference;

import com.android.settings.Utils;
import com.android.settingslib.core.lifecycle.Lifecycle;

import static android.provider.Settings.Secure.FP_SWIPE_TO_DISMISS_NOTIFICATIONS;

public class SwipeToDismissPreferenceController extends GesturePreferenceController {

    private static final int ON = 1;
    private static final int OFF = 0;

    private static final String PREF_KEY_VIDEO = "gesture_swipe_dismiss_fingerprint_video";
    private final String mKey;

    private static final String SECURE_KEY = FP_SWIPE_TO_DISMISS_NOTIFICATIONS;

    public SwipeToDismissPreferenceController(Context context, Lifecycle lifecycle,
            String key) {
        super(context, lifecycle);
        mKey = key;
    }

    private static boolean isGestureAvailable(Context context) {
        return Utils.hasFingerprintHardware(context) && context.getResources()
                .getBoolean(com.android.internal.R.bool.config_supportSystemNavigationKeys);
    }

    @Override
    public String getPreferenceKey() {
        return mKey;
    }

    @Override
    protected String getVideoPrefKey() {
        return PREF_KEY_VIDEO;
    }

    @Override
    public boolean isAvailable() {
        return SwipeToDismissPreferenceController.isAvailable(mContext);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        setSwipeToDismiss(mContext, (boolean) newValue);
        return true;
    }

    @Override
    protected boolean isSwitchPrefEnabled() {
        return isSwipeToDismissOn(mContext);
    }

    public static boolean isSwipeToDismissOn(Context context) {
        return Settings.Secure.getInt(context.getContentResolver(), SECURE_KEY, OFF) == ON;
    }

    public static boolean setSwipeToDismiss(Context context, boolean isEnabled) {
        return Settings.Secure.putInt(
                context.getContentResolver(), SECURE_KEY, isEnabled ? ON : OFF);
    }

    public static boolean isAvailable(Context context) {
        return isGestureAvailable(context);
    }
}
