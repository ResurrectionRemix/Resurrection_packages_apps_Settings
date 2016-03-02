/*
 * Copyright (C) 2015 The Dirty Unicorns project
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

package com.android.settings.rr.fragments;

import android.content.ContentResolver;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;

import com.android.settings.R;
import com.android.internal.logging.MetricsLogger;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import net.margaritov.preference.colorpicker.ColorPickerPreference;

public class CustomLogo extends SettingsPreferenceFragment implements OnPreferenceChangeListener {

    public static final String TAG = "Logo";

    private static final String KEY_CUSTOM_LOGO_COLOR = "custom_logo_color";
    private static final String KEY_CUSTOM_LOGO_STYLE = "custom_logo_style";

    private ColorPickerPreference mCustomLogoColor;
    private ListPreference mCustomLogoStyle;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.rr_custom_logos);

        PreferenceScreen prefSet = getPreferenceScreen();

        // custom logo color
        mCustomLogoColor =
            (ColorPickerPreference) prefSet.findPreference(KEY_CUSTOM_LOGO_COLOR);
        mCustomLogoColor.setOnPreferenceChangeListener(this);
        int intColor = Settings.System.getInt(getContentResolver(),
                Settings.System.CUSTOM_LOGO_COLOR, 0xffffffff);
        String hexColor = String.format("#%08x", (0xffffffff & intColor));
            mCustomLogoColor.setSummary(hexColor);
            mCustomLogoColor.setNewPreviewColor(intColor);


            mCustomLogoStyle = (ListPreference) findPreference(KEY_CUSTOM_LOGO_STYLE);
            int rrLogoStyle = Settings.System.getIntForUser(getContentResolver(),
                    Settings.System.CUSTOM_LOGO_STYLE, 0,
                    UserHandle.USER_CURRENT);
            mCustomLogoStyle.setValue(String.valueOf(rrLogoStyle));
            mCustomLogoStyle.setSummary(mCustomLogoStyle.getEntry());
            mCustomLogoStyle.setOnPreferenceChangeListener(this);

    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mCustomLogoColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.CUSTOM_LOGO_COLOR, intHex);
            return true;
        }  else if (preference == mCustomLogoStyle) {
                int rrLogoStyle = Integer.valueOf((String) newValue);
                int index = mCustomLogoStyle.findIndexOfValue((String) newValue);
                Settings.System.putIntForUser(
                       getContentResolver(), 
		Settings.System.CUSTOM_LOGO_STYLE, rrLogoStyle,
                        UserHandle.USER_CURRENT);
                mCustomLogoStyle.setSummary(
                        mCustomLogoStyle.getEntries()[index]);
                return true;
	}
        return false;
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.DONT_TRACK_ME_BRO;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

}
