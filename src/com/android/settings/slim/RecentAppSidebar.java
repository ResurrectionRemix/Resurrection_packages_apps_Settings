/*
 * Copyright (C) 2015 SlimRoms Project
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

package com.android.settings.slim;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.SlimSeekBarPreference;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import com.android.settings.DialogCreatable;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import net.margaritov.preference.colorpicker.ColorPickerPreference;
import com.android.internal.logging.MetricsLogger;

public class RecentAppSidebar extends SettingsPreferenceFragment implements DialogCreatable,
        Preference.OnPreferenceChangeListener {

    private static final String TAG = "RecentAppSidebarSettings";

    // Preferences
    private static final String APP_SIDEBAR_HIDE_LABELS = "recent_app_sidebar_disable_labels";
    private static final String APP_SIDEBAR_LABEL_COLOR = "recent_app_sidebar_label_color";
    private static final String APP_SIDEBAR_BG_COLOR = "recent_app_sidebar_bg_color";
    private static final String APP_SIDEBAR_SCALE = "recent_app_sidebar_scale";
    private static final String APP_SIDEBAR_OPEN_SIMULTANEOUSLY =
            "recent_app_sidebar_open_simultaneously";

    private SlimSeekBarPreference mAppSidebarScale;
    private SwitchPreference mAppSidebarHideLabels;
    private ColorPickerPreference mAppSidebarLabelColor;
    private ColorPickerPreference mAppSidebarBgColor;
    private SwitchPreference mAppSidebarOpenSimultaneously;

    private static final int MENU_RESET = Menu.FIRST;
    private static final int DEFAULT_COLOR = 0x00ffffff;

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.APPLICATION;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.recent_app_sidebar_settings);
        initializeAllPreferences();
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mAppSidebarScale) {
            int value = Integer.parseInt((String) newValue);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.RECENT_APP_SIDEBAR_SCALE_FACTOR, value);
            return true;
        } else if (preference == mAppSidebarHideLabels) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.RECENT_APP_SIDEBAR_DISABLE_LABELS,
                    ((Boolean) newValue) ? 1 : 0);
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
            Settings.System.putInt(getContentResolver(),
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
            Settings.System.putInt(getContentResolver(),
                    Settings.System.RECENT_APP_SIDEBAR_BG_COLOR,
                    intHex);
            return true;
        } else if (preference == mAppSidebarOpenSimultaneously) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.RECENT_APP_SIDEBAR_OPEN_SIMULTANEOUSLY,
                    ((Boolean) newValue) ? 1 : 0);
            return true;
        }
        return false;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, MENU_RESET, 0, R.string.reset)
                .setIcon(R.drawable.ic_settings_reset)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_RESET:
                resetToDefault();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    private void resetToDefault() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
        alertDialog.setTitle(R.string.shortcut_action_reset);
        alertDialog.setMessage(R.string.reset_message);
        alertDialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                resetValues();
            }
        });
        alertDialog.setNegativeButton(R.string.cancel, null);
        alertDialog.create().show();
    }

    private void resetValues() {
        Settings.System.putInt(getContentResolver(),
                Settings.System.RECENT_APP_SIDEBAR_TEXT_COLOR, DEFAULT_COLOR);
        mAppSidebarLabelColor.setNewPreviewColor(DEFAULT_COLOR);
        mAppSidebarLabelColor.setSummary(R.string.default_string);
        Settings.System.putInt(getContentResolver(),
                Settings.System.RECENT_APP_SIDEBAR_BG_COLOR, DEFAULT_COLOR);
        mAppSidebarBgColor.setNewPreviewColor(DEFAULT_COLOR);
        mAppSidebarBgColor.setSummary(R.string.default_string);
    }

    private void initializeAllPreferences() {
        mAppSidebarScale = (SlimSeekBarPreference) findPreference(APP_SIDEBAR_SCALE);
        mAppSidebarScale.setInterval(5);
        mAppSidebarScale.setDefault(100);
        mAppSidebarScale.minimumValue(60);
        mAppSidebarScale.setOnPreferenceChangeListener(this);
        mAppSidebarScale.setInitValue(Settings.System.getInt(getContentResolver(),
                Settings.System.RECENT_APP_SIDEBAR_SCALE_FACTOR, 100) - 60);

        mAppSidebarHideLabels = (SwitchPreference) findPreference(APP_SIDEBAR_HIDE_LABELS);
        mAppSidebarHideLabels.setOnPreferenceChangeListener(this);
        mAppSidebarHideLabels.setChecked(Settings.System.getInt(getContentResolver(),
                Settings.System.RECENT_APP_SIDEBAR_DISABLE_LABELS, 0) == 1);

        mAppSidebarOpenSimultaneously =
                (SwitchPreference) findPreference(APP_SIDEBAR_OPEN_SIMULTANEOUSLY);
        mAppSidebarOpenSimultaneously.setOnPreferenceChangeListener(this);
        mAppSidebarOpenSimultaneously.setChecked(Settings.System.getInt(getContentResolver(),
                Settings.System.RECENT_APP_SIDEBAR_OPEN_SIMULTANEOUSLY, 1) == 1);

        mAppSidebarLabelColor = (ColorPickerPreference) findPreference(APP_SIDEBAR_LABEL_COLOR);
        mAppSidebarLabelColor.setOnPreferenceChangeListener(this);
        final int intColorSidebarLabel = Settings.System.getInt(getContentResolver(),
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
        final int intColorSidebarBg = Settings.System.getInt(getContentResolver(),
                Settings.System.RECENT_APP_SIDEBAR_BG_COLOR, 0x00ffffff);
        String hexColorSidebarBg = String.format("#%08x", (0x00ffffff & intColorSidebarBg));
        if (hexColorSidebarBg.equals("#00ffffff")) {
            mAppSidebarBgColor.setSummary(R.string.default_string);
        } else {
            mAppSidebarBgColor.setSummary(hexColorSidebarBg);
        }
        mAppSidebarBgColor.setNewPreviewColor(intColorSidebarBg);

        // Enable options menu for color reset
        setHasOptionsMenu(true);
    }
}
