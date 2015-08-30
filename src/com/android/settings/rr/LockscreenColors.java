/*
 * Copyright (C) 2015 The Dirty Unicorns Project
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

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import net.margaritov.preference.colorpicker.ColorPickerPreference;

public class LockscreenColors extends SettingsPreferenceFragment implements OnPreferenceChangeListener {

    private static final String TAG = "LockscreenColors";

    private static final String LOCKSCREEN_PHONE_ICON_COLOR = "lockscreen_phone_icon_color";
    private static final String LOCKSCREEN_LOCK_ICON_COLOR = "lockscreen_lock_icon_color";
    private static final String LOCKSCREEN_CAMERA_ICON_COLOR = "lockscreen_camera_icon_color";
    private static final String LOCKSCREEN_INDICATION_TEXT_COLOR = "lockscreen_indication_text_color";
    private static final String LOCKSCREEN_CLOCK_COLOR = "lockscreen_clock_color";
    private static final String LOCKSCREEN_CLOCK_DATE_COLOR = "lockscreen_clock_date_color";

    static final int DEFAULT = 0xffffffff;
    private static final int MENU_RESET = Menu.FIRST;

    private ColorPickerPreference mLockscreenPhoneColorPicker;
    private ColorPickerPreference mLockscreenLockColorPicker;
    private ColorPickerPreference mLockscreenCameraColorPicker;
    private ColorPickerPreference mLockscreenIndicationTextColorPicker;
    private ColorPickerPreference mLockscreenClockColorPicker;
    private ColorPickerPreference mLockscreenClockDateColorPicker;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.lockscreen_colors);

        PreferenceScreen prefSet = getPreferenceScreen();
        ContentResolver resolver = getActivity().getContentResolver();

        int intColor;
        String hexColor;

        mLockscreenPhoneColorPicker = (ColorPickerPreference) findPreference(LOCKSCREEN_PHONE_ICON_COLOR);
        mLockscreenPhoneColorPicker.setOnPreferenceChangeListener(this);
        intColor = Settings.System.getInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_PHONE_ICON_COLOR, DEFAULT);
        hexColor = String.format("#%08x", (0xffffffff & intColor));
        mLockscreenPhoneColorPicker.setSummary(hexColor);
        mLockscreenPhoneColorPicker.setNewPreviewColor(intColor);

        mLockscreenLockColorPicker = (ColorPickerPreference) findPreference(LOCKSCREEN_LOCK_ICON_COLOR);
        mLockscreenLockColorPicker.setOnPreferenceChangeListener(this);
        intColor = Settings.System.getInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_LOCK_ICON_COLOR, DEFAULT);
        hexColor = String.format("#%08x", (0xffffffff & intColor));
        mLockscreenLockColorPicker.setSummary(hexColor);
        mLockscreenLockColorPicker.setNewPreviewColor(intColor);

        mLockscreenCameraColorPicker = (ColorPickerPreference) findPreference(LOCKSCREEN_CAMERA_ICON_COLOR);
        mLockscreenCameraColorPicker.setOnPreferenceChangeListener(this);
        intColor = Settings.System.getInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_CAMERA_ICON_COLOR, DEFAULT);
        hexColor = String.format("#%08x", (0xffffffff & intColor));
        mLockscreenCameraColorPicker.setSummary(hexColor);
        mLockscreenCameraColorPicker.setNewPreviewColor(intColor);

        mLockscreenIndicationTextColorPicker = (ColorPickerPreference) findPreference(LOCKSCREEN_INDICATION_TEXT_COLOR);
        mLockscreenIndicationTextColorPicker.setOnPreferenceChangeListener(this);
        intColor = Settings.System.getInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_INDICATION_TEXT_COLOR, DEFAULT);
        hexColor = String.format("#%08x", (0xffffffff & intColor));
        mLockscreenIndicationTextColorPicker.setSummary(hexColor);
        mLockscreenIndicationTextColorPicker.setNewPreviewColor(intColor);

        mLockscreenClockColorPicker = (ColorPickerPreference) findPreference(LOCKSCREEN_CLOCK_COLOR);
        mLockscreenClockColorPicker.setOnPreferenceChangeListener(this);
        intColor = Settings.System.getInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_CLOCK_COLOR, DEFAULT);
        hexColor = String.format("#%08x", (0xffffffff & intColor));
        mLockscreenClockColorPicker.setSummary(hexColor);
        mLockscreenClockColorPicker.setNewPreviewColor(intColor);

        mLockscreenClockDateColorPicker = (ColorPickerPreference) findPreference(LOCKSCREEN_CLOCK_DATE_COLOR);
        mLockscreenClockDateColorPicker.setOnPreferenceChangeListener(this);
        intColor = Settings.System.getInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_CLOCK_DATE_COLOR, DEFAULT);
        hexColor = String.format("#%08x", (0xffffffff & intColor));
        mLockscreenClockDateColorPicker.setSummary(hexColor);
        mLockscreenClockDateColorPicker.setNewPreviewColor(intColor);

        setHasOptionsMenu(true);
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
                ContentResolver resolver = getActivity().getContentResolver();
        if (preference == mLockscreenCameraColorPicker) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.LOCKSCREEN_CAMERA_ICON_COLOR, intHex);
            return true;
        } else if (preference == mLockscreenLockColorPicker) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.LOCKSCREEN_LOCK_ICON_COLOR, intHex);
            return true;
        } else if (preference == mLockscreenPhoneColorPicker) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.LOCKSCREEN_PHONE_ICON_COLOR, intHex);
            return true;
        } else if (preference == mLockscreenIndicationTextColorPicker) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.LOCKSCREEN_INDICATION_TEXT_COLOR, intHex);
            return true;
        } else if (preference == mLockscreenClockColorPicker) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.LOCKSCREEN_CLOCK_COLOR, intHex);
            return true;
        } else if (preference == mLockscreenClockDateColorPicker) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.LOCKSCREEN_CLOCK_DATE_COLOR, intHex);
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
        alertDialog.setTitle(R.string.lockscreen_colors_reset_title);
        alertDialog.setMessage(R.string.lockscreen_colors_reset_message);
        alertDialog.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                resetValues();
            }
        });
        alertDialog.setNegativeButton(R.string.cancel, null);
        alertDialog.create().show();
    }

    private void resetValues() {
        Settings.System.putInt(getContentResolver(),
                Settings.System.LOCKSCREEN_PHONE_ICON_COLOR, DEFAULT);
        mLockscreenPhoneColorPicker.setNewPreviewColor(DEFAULT);
        mLockscreenPhoneColorPicker.setSummary(R.string.default_string);
        Settings.System.putInt(getContentResolver(),
                Settings.System.LOCKSCREEN_LOCK_ICON_COLOR, DEFAULT);
        mLockscreenLockColorPicker.setNewPreviewColor(DEFAULT);
        mLockscreenLockColorPicker.setSummary(R.string.default_string);
        Settings.System.putInt(getContentResolver(),
                Settings.System.LOCKSCREEN_CAMERA_ICON_COLOR, DEFAULT);
        mLockscreenCameraColorPicker.setNewPreviewColor(DEFAULT);
        mLockscreenCameraColorPicker.setSummary(R.string.default_string);
        Settings.System.putInt(getContentResolver(),
                Settings.System.LOCKSCREEN_INDICATION_TEXT_COLOR, DEFAULT);
        mLockscreenIndicationTextColorPicker.setNewPreviewColor(DEFAULT);
        mLockscreenIndicationTextColorPicker.setSummary(R.string.default_string);
        Settings.System.putInt(getContentResolver(),
                Settings.System.LOCKSCREEN_CLOCK_COLOR, DEFAULT);
        mLockscreenClockColorPicker.setNewPreviewColor(DEFAULT);
        mLockscreenClockColorPicker.setSummary(R.string.default_string);
        Settings.System.putInt(getContentResolver(),
                Settings.System.LOCKSCREEN_CLOCK_DATE_COLOR, DEFAULT);
        mLockscreenClockDateColorPicker.setNewPreviewColor(DEFAULT);
        mLockscreenClockDateColorPicker.setSummary(R.string.default_string);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }
}
