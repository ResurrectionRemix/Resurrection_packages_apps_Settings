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
package com.android.settings.display;

import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.ListPreference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

/**
 * A controller to manage the preference for showing an alert on optimal battery charge level
 */

public class BatteryChargeAlertPreferenceController extends
        AbstractPreferenceController implements PreferenceControllerMixin,
        Preference.OnPreferenceChangeListener {

    private static final String KEY_ALERT_ON_CHARGED_LEVEL = "alert_on_charged_level";

    public BatteryChargeAlertPreferenceController(Context context) {
        super(context);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_ALERT_ON_CHARGED_LEVEL;
    }

    @Override
    public void updateState(Preference preference) {
        int setting = Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.ALERT_ON_CHARGED_LEVEL, -1, UserHandle.USER_CURRENT);
        ((ListPreference) preference).setValue(Integer.toString(setting));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        int val = Integer.valueOf((String) newValue);
        Settings.System.putIntForUser(mContext.getContentResolver(),
                Settings.System.ALERT_ON_CHARGED_LEVEL,
                val, UserHandle.USER_CURRENT);
        return true;
    }
}
