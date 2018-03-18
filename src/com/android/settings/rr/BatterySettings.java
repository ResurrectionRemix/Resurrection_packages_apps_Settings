/*Copyright (C) 2015 The ResurrectionRemix Project
     Licensed under the Apache License, Version 2.0 (the "License");
     you may not use this file except in compliance with the License.
     You may obtain a copy of the License at

          http://www.apache.org/licenses/LICENSE-2.0

     Unless required by applicable law or agreed to in writing, software
     distributed under the License is distributed on an "AS IS" BASIS,
     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     See the License for the specific language governing permissions and
     limitations under the License.
*/
package com.android.settings.rr;

import android.app.Activity;
import android.content.Context;
import android.content.ContentResolver;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.app.Fragment;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.ListPreference;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import net.margaritov.preference.colorpicker.ColorPickerPreference;
import com.android.settings.rr.Preferences.SystemSettingMasterSwitchPreference;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;

public class BatterySettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private SystemSettingMasterSwitchPreference mBatteryBarPosition;
    private static final String BATTERY_STYLE = "battery_style";
    private static final String BATTERY_PERCENT = "show_battery_percent";

    private ListPreference mBatteryIconStyle;
    private ListPreference mBatteryPercentage;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.rr_battery);

        int batteryStyle = Settings.Secure.getInt(getActivity().getContentResolver(),
                Settings.Secure.STATUS_BAR_BATTERY_STYLE, 0);

       mBatteryIconStyle = (ListPreference) findPreference(BATTERY_STYLE);
       mBatteryIconStyle.setValue(Integer.toString(batteryStyle));
       mBatteryIconStyle.setOnPreferenceChangeListener(this);
       int valueIndex = mBatteryIconStyle.findIndexOfValue(String.valueOf(batteryStyle));
       mBatteryIconStyle.setSummary(mBatteryIconStyle.getEntries()[valueIndex]);
       int showPercent = Settings.System.getInt(getActivity().getContentResolver(),
                         Settings.System.SHOW_BATTERY_PERCENT, 1);
        mBatteryPercentage = (ListPreference) findPreference(BATTERY_PERCENT);
        mBatteryPercentage.setValue(Integer.toString(showPercent));
                        valueIndex = mBatteryPercentage.findIndexOfValue(String.valueOf(showPercent));
                        mBatteryPercentage.setSummary(mBatteryPercentage.getEntries()[valueIndex]);
        mBatteryPercentage.setOnPreferenceChangeListener(this);
        boolean hideForcePercentage = batteryStyle == 7; /*text or hidden style*/
        mBatteryPercentage.setEnabled(!hideForcePercentage);

    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
         if (preference == mBatteryIconStyle) {
            int value = Integer.valueOf((String) newValue);
            Settings.Secure.putInt(getContentResolver(),
                    Settings.Secure.STATUS_BAR_BATTERY_STYLE, value);
                    int valueIndex = mBatteryIconStyle
                             .findIndexOfValue((String) newValue);
                     mBatteryIconStyle
                             .setSummary(mBatteryIconStyle.getEntries()[valueIndex]);
                    boolean hideForcePercentage = value == 7;/*text or hidden style*/
            mBatteryPercentage.setEnabled(!hideForcePercentage);
            return true;
        } else  if (preference == mBatteryPercentage) {
            int value = Integer.valueOf((String) newValue);
            Settings.System.putInt(getActivity().getContentResolver(),
            Settings.System.SHOW_BATTERY_PERCENT, value);
            int valueIndex = mBatteryPercentage
                    .findIndexOfValue((String) newValue);
            mBatteryPercentage
                    .setSummary(mBatteryPercentage.getEntries()[valueIndex]);
            return true;
         }
        return false;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.RESURRECTED;
    }

}
