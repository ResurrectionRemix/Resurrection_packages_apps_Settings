/*
 * Copyright (C) 2018 The Android Open Source Project
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
import static android.provider.Settings.System.AUTO_BRIGHTNESS_MIN_VALUE;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.SliderPreferenceController;
import com.android.settings.widget.SeekBarPreference;

public class AutoBrightnessMinPreferenceController extends SliderPreferenceController {

    final int mScreenBrightnessSettingMinimum;
    final int mScreenBrightnessSettingMaximum;

    public AutoBrightnessMinPreferenceController(Context context, String key) {
        super(context, key);
        mScreenBrightnessSettingMinimum = mContext.getResources().getInteger(
                com.android.internal.R.integer.config_screenBrightnessSettingMinimum);
        mScreenBrightnessSettingMaximum = mContext.getResources().getInteger(
                com.android.internal.R.integer.config_screenBrightnessSettingMaximum);
    }

    @Override
    @AvailabilityStatus
    public int getAvailabilityStatus() {
        if (!mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_automatic_brightness_available)) {
            return UNSUPPORTED_ON_DEVICE;
        }
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        final SeekBarPreference preference = screen.findPreference(getPreferenceKey());
        preference.setContinuousUpdates(true);
        preference.setMax(getMax());
        preference.setMin(getMin());
    }

    @Override
    public int getSliderPosition() {
        int autoBrightnessMin = Settings.System.getInt(mContext.getContentResolver(),
                AUTO_BRIGHTNESS_MIN_VALUE, -1);
        if (autoBrightnessMin == -1) {
            autoBrightnessMin = mScreenBrightnessSettingMinimum;
        }
        return autoBrightnessMin;
    }

    @Override
    public boolean setSliderPosition(int position) {
        Settings.System.putInt(mContext.getContentResolver(),
                AUTO_BRIGHTNESS_MIN_VALUE, position);
        return true;
    }

    @Override
    public int getMax() {
        return mScreenBrightnessSettingMaximum;
    }

    @Override
    public int getMin() {
        return mScreenBrightnessSettingMinimum;
    }
}
