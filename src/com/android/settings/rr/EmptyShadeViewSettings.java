/*
 * Copyright (C) 2015 DarkKat
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

package com.android.settings.rr;

import android.content.ContentResolver;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v14.preference.SwitchPreference;
import android.provider.Settings;

import com.android.internal.util.rr.DeviceUtils;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import net.margaritov.preference.colorpicker.ColorPickerPreference;

public class EmptyShadeViewSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String PREF_SHOW_CARRIER_NAME =
            "empty_shade_view_show_carrier_name";
    private static final String PREF_SHOW_WIFI_NAME =
            "empty_shade_view_show_wifi_name";
    private static final String PREF_TEXT_COLOR =
            "empty_shade_view_text_color";

    private static final int WHITE           = 0xffffffff;

    private SwitchPreference mShowCarrierName;
    private SwitchPreference mShowWifiName;
    private ColorPickerPreference mTextColor;

    private ContentResolver mResolver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        refreshSettings();
    }

    public void refreshSettings() {
        PreferenceScreen prefs = getPreferenceScreen();
        if (prefs != null) {
            prefs.removeAll();
        }

        addPreferencesFromResource(R.xml.empty_shade_view_settings);
        mResolver = getContentResolver();

        final boolean showCarrierName = Settings.System.getInt(mResolver,
               Settings.System.EMPTY_SHADE_VIEW_SHOW_CARRIER_NAME, 0) == 1;
        final boolean showWifiName = Settings.System.getInt(mResolver,
               Settings.System.EMPTY_SHADE_VIEW_SHOW_WIFI_NAME, 0) == 1;
        final boolean supportsMobileData = DeviceUtils.deviceSupportsMobileData(getActivity());

        if (supportsMobileData) {
            mShowCarrierName = (SwitchPreference) findPreference(PREF_SHOW_CARRIER_NAME);
            mShowCarrierName.setChecked(showCarrierName);
            mShowCarrierName.setOnPreferenceChangeListener(this);
        } else {
            removePreference(PREF_SHOW_CARRIER_NAME);

        }

        mShowWifiName = (SwitchPreference) findPreference(PREF_SHOW_WIFI_NAME);
        mShowWifiName.setChecked(showWifiName);
        mShowWifiName.setOnPreferenceChangeListener(this);

        if ((showCarrierName && supportsMobileData) || showWifiName) {
            mTextColor =
                    (ColorPickerPreference) findPreference(PREF_TEXT_COLOR);
            int intColor = Settings.System.getInt(mResolver,
                    Settings.System.EMPTY_SHADE_VIEW_TEXT_COLOR, WHITE);
            mTextColor.setNewPreviewColor(intColor);
            String hexColor = String.format("#%08x", (0xffffffff & intColor));
            mTextColor.setSummary(hexColor);
            mTextColor.setOnPreferenceChangeListener(this);
        } else {
            removePreference(PREF_TEXT_COLOR);

        }

    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean value;

        if (preference == mShowCarrierName) {
            value = (Boolean) newValue;
            Settings.System.putInt(mResolver,
                    Settings.System.EMPTY_SHADE_VIEW_SHOW_CARRIER_NAME,
                    value ? 1 : 0);
            refreshSettings();
            return true;
        } else if (preference == mShowWifiName) {
            value = (Boolean) newValue;
            Settings.System.putInt(mResolver,
                    Settings.System.EMPTY_SHADE_VIEW_SHOW_WIFI_NAME,
                    value ? 1 : 0);
            refreshSettings();
            return true;
        } else if (preference == mTextColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(mResolver,
                    Settings.System.EMPTY_SHADE_VIEW_TEXT_COLOR, intHex);
            preference.setSummary(hex);
            return true;
        }
        return false;
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.RESURRECTED;
    }
}
