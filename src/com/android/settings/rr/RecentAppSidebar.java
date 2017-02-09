/*
 * Copyright (C) 2015-2017 Android Ice Cold Project
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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v14.preference.PreferenceFragment;
import android.provider.Settings;
import net.margaritov.preference.colorpicker.ColorPickerPreference;

import com.android.settings.R;

public class RecentAppSidebar extends PreferenceFragment
            implements Preference.OnPreferenceChangeListener {

    private static final String TAG = "RecentAppSidebarSettings";

    // Preferences
    private static final String APP_SIDEBAR_HIDE_LABELS = "recent_app_sidebar_disable_labels";
    private static final String APP_SIDEBAR_LABEL_COLOR = "recent_app_sidebar_label_color";
    private static final String APP_SIDEBAR_BG_COLOR = "recent_app_sidebar_bg_color";
    private static final String APP_SIDEBAR_SCALE = "recent_app_sidebar_scale";

    private SeekBarPreference mAppSidebarScale;
    private ColorPickerPreference mAppSidebarLabelColor;
    private ColorPickerPreference mAppSidebarBgColor;

    private static final int DEFAULT_COLOR = 0x00ffffff;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.recent_app_sidebar_settings);
        initializeAllPreferences();
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mAppSidebarScale) {
            Settings.System.putInt(getContext().getContentResolver(),
                Settings.System.RECENT_APP_SIDEBAR_SCALE_FACTOR, Integer.valueOf(String.valueOf(newValue)));
            return true;
        } else if (preference == mAppSidebarLabelColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            if (hex.equals("#00ffffff")) {
                preference.setSummary(R.string.default_string);
            } else {
                preference.setSummary(hex);
            }
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getContext().getContentResolver(),
                    Settings.System.RECENT_APP_SIDEBAR_TEXT_COLOR,
                    intHex);
            return true;
        } else if (preference == mAppSidebarBgColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            if (hex.equals("#00ffffff")) {
                preference.setSummary(R.string.default_string);
            } else {
                preference.setSummary(hex);
            }
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getContext().getContentResolver(),
                    Settings.System.RECENT_APP_SIDEBAR_BG_COLOR,
                    intHex);
            return true;
        }
        return false;
    }

    private void initializeAllPreferences() {
        mAppSidebarScale = (SeekBarPreference) findPreference(APP_SIDEBAR_SCALE);
        mAppSidebarScale.setOnPreferenceChangeListener(this);
        mAppSidebarScale.setValue(Settings.System.getInt(getContext().getContentResolver(),
                Settings.System.RECENT_APP_SIDEBAR_SCALE_FACTOR, 100));


        mAppSidebarLabelColor = (ColorPickerPreference) findPreference(APP_SIDEBAR_LABEL_COLOR);
        mAppSidebarLabelColor.setOnPreferenceChangeListener(this);
        final int intColorSidebarLabel = Settings.System.getInt(getContext().getContentResolver(),
                Settings.System.RECENT_APP_SIDEBAR_TEXT_COLOR, 0x00ffffff);
        String hexColorSidebarLabel = String.format("#%08x", (0x00ffffff & intColorSidebarLabel));
        if (hexColorSidebarLabel.equals("#00ffffff")) {
            mAppSidebarLabelColor.setSummary(R.string.default_string);
        } else {
            mAppSidebarLabelColor.setSummary(hexColorSidebarLabel);
        }
        mAppSidebarLabelColor.setNewPreviewColor(intColorSidebarLabel);

        mAppSidebarBgColor =
                (ColorPickerPreference) findPreference(APP_SIDEBAR_BG_COLOR);
        mAppSidebarBgColor.setOnPreferenceChangeListener(this);
        final int intColorSidebarBg = Settings.System.getInt(getContext().getContentResolver(),
                Settings.System.RECENT_APP_SIDEBAR_BG_COLOR, 0x00ffffff);
        String hexColorSidebarBg = String.format("#%08x", (0x00ffffff & intColorSidebarBg));
        if (hexColorSidebarBg.equals("#00ffffff")) {
            mAppSidebarBgColor.setSummary(R.string.default_string);
        } else {
            mAppSidebarBgColor.setSummary(hexColorSidebarBg);
        }
        mAppSidebarBgColor.setNewPreviewColor(intColorSidebarBg);
    }
}
