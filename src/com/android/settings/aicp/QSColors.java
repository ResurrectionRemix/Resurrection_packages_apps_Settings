/*
 * Copyright (C) 2015 DarkKat
 * Copyright (C) 2015 AICP
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.settings.aicp;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.SwitchPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.cyanogenmod.qs.QSTiles;

import net.margaritov.preference.colorpicker.ColorPickerPreference;

public class QSColors extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String PREF_QS_BACKGROUND_COLOR =
            "qs_background_color";
    private static final String PREF_QS_ICON_COLOR =
            "qs_icon_color";
    private static final String PREF_QS_TEXT_COLOR =
            "qs_text_color";
    private static final String PREF_QS_TRANSPARENT_SHADE =
            "qs_transparent_shade";
    private static final String PREF_QS_COLOR_SWITCH =
            "qs_color_switch";

    private static final int DEFAULT_BACKGROUND_COLOR = 0xff263238;
    private static final int WHITE = 0xffffffff;
    private static final int SWAG_TEAL = 0xfff700ff;

    private static final int MENU_RESET = Menu.FIRST;
    private static final int DLG_RESET = 0;

    private boolean mRecreating = false;

    private ColorPickerPreference mQSBackgroundColor;
    private ColorPickerPreference mQSIconColor;
    private ColorPickerPreference mQSTextColor;
    private SwitchPreference mQSShadeTransparency;
    private SwitchPreference mQSSSwitch;

    private ContentResolver mResolver;

    private void recreateStatusBar() {
        mRecreating = true;
    }
    
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        refreshSettings();
    }

    public void refreshSettings() {
        PreferenceScreen prefs = getPreferenceScreen();
        if (prefs != null) {
            prefs.removeAll();
        }

        addPreferencesFromResource(R.xml.qs_color_settings);
        mResolver = getActivity().getContentResolver();

        int intColor;
        String hexColor;

        mQSBackgroundColor =
                (ColorPickerPreference) findPreference(PREF_QS_BACKGROUND_COLOR);
        intColor = Settings.System.getInt(mResolver,
                Settings.System.QS_BACKGROUND_COLOR,
                DEFAULT_BACKGROUND_COLOR);
        mQSBackgroundColor.setNewPreviewColor(intColor);
        hexColor = String.format("#%08x", (0xffffffff & intColor));
        mQSBackgroundColor.setSummary(hexColor);
        mQSBackgroundColor.setAlphaSliderEnabled(true);
        mQSBackgroundColor.setOnPreferenceChangeListener(this);

        mQSIconColor =
                (ColorPickerPreference) findPreference(PREF_QS_ICON_COLOR);
        intColor = Settings.System.getInt(mResolver,
                Settings.System.QS_ICON_COLOR, WHITE);
        mQSIconColor.setNewPreviewColor(intColor);
        hexColor = String.format("#%08x", (0xffffffff & intColor));
        mQSIconColor.setSummary(hexColor);
        mQSIconColor.setOnPreferenceChangeListener(this);

        mQSTextColor =
                (ColorPickerPreference) findPreference(PREF_QS_TEXT_COLOR);
        intColor = Settings.System.getInt(mResolver,
                Settings.System.QS_TEXT_COLOR, WHITE);
        mQSTextColor.setNewPreviewColor(intColor);
        hexColor = String.format("#%08x", (0xffffffff & intColor));
        mQSTextColor.setSummary(hexColor);
        mQSTextColor.setOnPreferenceChangeListener(this);

        mQSShadeTransparency = (SwitchPreference) findPreference(PREF_QS_TRANSPARENT_SHADE);
        mQSShadeTransparency.setChecked((Settings.System.getInt(mResolver,
                Settings.System.QS_TRANSPARENT_SHADE, 0) == 1));
        mQSShadeTransparency.setOnPreferenceChangeListener(this);

        mQSSSwitch = (SwitchPreference) findPreference(PREF_QS_COLOR_SWITCH);
        mQSSSwitch.setChecked((Settings.System.getInt(mResolver,
                Settings.System.QS_COLOR_SWITCH, 0) == 1));
        mQSSSwitch.setOnPreferenceChangeListener(this);

        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, MENU_RESET, 0, R.string.reset)
                .setIcon(R.drawable.ic_settings_reset) // use the KitKat backup icon
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_RESET:
                showDialogInner(DLG_RESET);
                return true;
             default:
                return super.onContextItemSelected(item);
        }
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String hex;
        int intHex;

        if (preference == mQSBackgroundColor) {
            hex = ColorPickerPreference.convertToARGB(
                Integer.valueOf(String.valueOf(newValue)));
            intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(mResolver,
                Settings.System.QS_BACKGROUND_COLOR, intHex);
            preference.setSummary(hex);
            recreateStatusBar();
            return true;
        } else if (preference == mQSIconColor) {
            hex = ColorPickerPreference.convertToARGB(
                Integer.valueOf(String.valueOf(newValue)));
            intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(mResolver,
                Settings.System.QS_ICON_COLOR, intHex);
            preference.setSummary(hex);
            recreateStatusBar();
            return true;
        } else if (preference == mQSTextColor) {
            hex = ColorPickerPreference.convertToARGB(
                Integer.valueOf(String.valueOf(newValue)));
            intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(mResolver,
                Settings.System.QS_TEXT_COLOR, intHex);
            preference.setSummary(hex);
            recreateStatusBar();
            return true;
        } else if (preference == mQSShadeTransparency) {
            boolean value = (Boolean) newValue;
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.QS_TRANSPARENT_SHADE, value ? 1 : 0);
            recreateStatusBar();
            return true;
        } else if (preference == mQSSSwitch) {
            boolean value = (Boolean) newValue;
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.QS_COLOR_SWITCH, value ? 1 : 0);
            refreshSettings();
            recreateStatusBar();
        }
        return false;
    }

    private void showDialogInner(int id) {
        DialogFragment newFragment = MyAlertDialogFragment.newInstance(id);
        newFragment.setTargetFragment(this, 0);
        newFragment.show(getFragmentManager(), "dialog " + id);
    }

    public static class MyAlertDialogFragment extends DialogFragment {

        public static MyAlertDialogFragment newInstance(int id) {
            MyAlertDialogFragment frag = new MyAlertDialogFragment();
            Bundle args = new Bundle();
            args.putInt("id", id);
            frag.setArguments(args);
            return frag;
        }

        QSColors getOwner() {
            return (QSColors) getTargetFragment();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int id = getArguments().getInt("id");
            switch (id) {
                case DLG_RESET:
                    return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.reset)
                    .setMessage(R.string.reset_message)
                    .setNegativeButton(R.string.cancel, null)
                    .setNeutralButton(R.string.reset_android,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.QS_BACKGROUND_COLOR,
                                    DEFAULT_BACKGROUND_COLOR);
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.QS_ICON_COLOR, WHITE);
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.QS_TEXT_COLOR, WHITE);
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.QS_TRANSPARENT_SHADE, 0);
                            getOwner().refreshSettings();
                        }
                    })
                    .setPositiveButton(R.string.reset_swag,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.QS_BACKGROUND_COLOR,
                                    WHITE);
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.QS_ICON_COLOR,
                                    SWAG_TEAL);
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.QS_TEXT_COLOR,
                                    SWAG_TEAL);
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.QS_TRANSPARENT_SHADE, 0);
                            getOwner().refreshSettings();
                        }
                    })
                    .create();
            }
            throw new IllegalArgumentException("unknown id " + id);
        }

        @Override
        public void onCancel(DialogInterface dialog) {

        }
    }
}
