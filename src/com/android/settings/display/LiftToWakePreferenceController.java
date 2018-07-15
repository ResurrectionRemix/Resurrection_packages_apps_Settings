/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.android.settings.display;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

import static android.provider.Settings.Secure.WAKE_GESTURE_ENABLED;

public class LiftToWakePreferenceController extends AbstractPreferenceController implements
        PreferenceControllerMixin, Preference.OnPreferenceChangeListener {

    private static final String KEY_LIFT_TO_WAKE = "lift_to_wake";

    public LiftToWakePreferenceController(Context context) {
        super(context);
    }

    @Override
    public boolean isAvailable() {
        SensorManager sensors = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        return mContext.getResources().getBoolean(
                com.android.settings.R.bool.config_supportsLiftToWake) &&
                sensors != null && sensors.getDefaultSensor(Sensor.TYPE_WAKE_GESTURE) != null;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_LIFT_TO_WAKE;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean value = (Boolean) newValue;
        Settings.Secure.putInt(mContext.getContentResolver(), WAKE_GESTURE_ENABLED, value ? 1 : 0);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        int value = Settings.Secure.getInt(mContext.getContentResolver(), WAKE_GESTURE_ENABLED, 0);
        ((SwitchPreference) preference).setChecked(value != 0);
    }
}
