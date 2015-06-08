/*
 * Copyright (C) 2013-2015 Slimroms
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
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.preference.SlimSeekBarPreference;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import net.margaritov.preference.colorpicker.ColorPickerPreference;

public class PieButtonStyleSettings extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener {

    private static final String TAG = "PieButtonStyleSettings";
    private static final String PREF_PIE_BUTTON_COLOR =
            "pie_button_color";
    private static final String PREF_PIE_BUTTON_PRESSED_COLOR =
            "pie_button_pressed_color";
    private static final String PREF_PIE_BUTTON_LONG_PRESSED_COLOR =
            "pie_button_long_pressed_color";
    private static final String PREF_PIE_BUTTON_OUTLINE_COLOR =
            "pie_button_outline_color";
    private static final String PREF_PIE_ICON_COLOR =
            "pie_icon_color";
    private static final String PREF_PIE_ICON_COLOR_MODE =
            "pie_icon_color_mode";
    private static final String PREF_PIE_BUTTON_ALPHA =
            "pie_button_alpha";
    private static final String PREF_PIE_BUTTON_PRESSED_ALPHA =
            "pie_button_pressed_alpha";

    private static final int MENU_RESET = Menu.FIRST;
    private static final int DLG_RESET = 0;

    Resources mSystemUiResources;
    private boolean mCheckPreferences;

    ColorPickerPreference mPieButtonColor;
    ColorPickerPreference mPieButtonPressedColor;
    ColorPickerPreference mPieButtonLongPressedColor;
    ColorPickerPreference mPieButtonOutlineColor;
    ColorPickerPreference mPieIconColor;
    ListPreference mPieIconColorMode;
    SlimSeekBarPreference mPieButtonAlpha;
    SlimSeekBarPreference mPieButtonPressedAlpha;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.pie_button_style);

        PreferenceScreen prefs = getPreferenceScreen();

        PackageManager pm = getPackageManager();

        if (pm != null) {
            try {
                mSystemUiResources = pm.getResourcesForApplication("com.android.systemui");
            } catch (Exception e) {
                mSystemUiResources = null;
                Log.e("PIEButtonStyle:", "can't access systemui resources",e);
            }
        }

        mPieButtonColor =
                (ColorPickerPreference) findPreference(PREF_PIE_BUTTON_COLOR);
        mPieButtonColor.setOnPreferenceChangeListener(this);

        mPieButtonPressedColor =
                (ColorPickerPreference) findPreference(PREF_PIE_BUTTON_PRESSED_COLOR);
        mPieButtonPressedColor.setOnPreferenceChangeListener(this);

        mPieButtonLongPressedColor =
                (ColorPickerPreference) findPreference(PREF_PIE_BUTTON_LONG_PRESSED_COLOR);
        mPieButtonLongPressedColor.setOnPreferenceChangeListener(this);

        mPieButtonOutlineColor =
                (ColorPickerPreference) findPreference(PREF_PIE_BUTTON_OUTLINE_COLOR);
        mPieButtonOutlineColor.setOnPreferenceChangeListener(this);

        mPieIconColor =
                (ColorPickerPreference) findPreference(PREF_PIE_ICON_COLOR);
        mPieIconColor.setOnPreferenceChangeListener(this);

        mPieButtonAlpha =
                (SlimSeekBarPreference) findPreference(PREF_PIE_BUTTON_ALPHA);
        mPieButtonAlpha.setOnPreferenceChangeListener(this);

        mPieButtonPressedAlpha =
                (SlimSeekBarPreference) findPreference(PREF_PIE_BUTTON_PRESSED_ALPHA);
        mPieButtonPressedAlpha.setOnPreferenceChangeListener(this);

        mPieIconColorMode =
                (ListPreference) prefs.findPreference(PREF_PIE_ICON_COLOR_MODE);
        mPieIconColorMode.setOnPreferenceChangeListener(this);

        setHasOptionsMenu(true);
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
                showDialogInner(DLG_RESET);
                return true;
             default:
                return super.onContextItemSelected(item);
        }
    }

    private void resetToDefault() {
        Settings.System.putInt(getActivity().getContentResolver(),
                Settings.System.PIE_BUTTON_COLOR, -2);
        Settings.System.putInt(getActivity().getContentResolver(),
                Settings.System.PIE_BUTTON_PRESSED_COLOR, -2);
        Settings.System.putInt(getActivity().getContentResolver(),
                Settings.System.PIE_BUTTON_LONG_PRESSED_COLOR, -2);
        Settings.System.putInt(getActivity().getContentResolver(),
                Settings.System.PIE_BUTTON_OUTLINE_COLOR, -2);
        Settings.System.putInt(getActivity().getContentResolver(),
                Settings.System.PIE_ICON_COLOR, -2);
        Settings.System.putFloat(getActivity().getContentResolver(),
                Settings.System.PIE_BUTTON_ALPHA, 0.3f);
        Settings.System.putFloat(getActivity().getContentResolver(),
                Settings.System.PIE_BUTTON_PRESSED_ALPHA, 0.0f);
        Settings.System.putInt(getActivity().getContentResolver(),
                Settings.System.PIE_ICON_COLOR_MODE, 0);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (!mCheckPreferences) {
            return false;
        }
        if (preference == mPieButtonColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.PIE_BUTTON_COLOR, intHex);
            return true;
        } else if (preference == mPieButtonPressedColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.PIE_BUTTON_PRESSED_COLOR, intHex);
            return true;
        } else if (preference == mPieButtonLongPressedColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.PIE_BUTTON_LONG_PRESSED_COLOR, intHex);
            return true;
        } else if (preference == mPieButtonOutlineColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.PIE_BUTTON_OUTLINE_COLOR, intHex);
            return true;
        } else if (preference == mPieIconColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.PIE_ICON_COLOR, intHex);
            return true;
       } else if (preference == mPieButtonAlpha) {
            float val = Float.parseFloat((String) newValue);
            Log.e("R", "value: " + val / 100);
            Settings.System.putFloat(getActivity().getContentResolver(),
                    Settings.System.PIE_BUTTON_ALPHA,
                    val / 100);
            return true;
       } else if (preference == mPieButtonPressedAlpha) {
            float val = Float.parseFloat((String) newValue);
            Log.e("R", "value: " + val / 100);
            Settings.System.putFloat(getActivity().getContentResolver(),
                    Settings.System.PIE_BUTTON_PRESSED_ALPHA,
                    val / 100);
            return true;
       } else if (preference == mPieIconColorMode) {
            int index = mPieIconColorMode.findIndexOfValue((String) newValue);
            int value = Integer.valueOf((String) newValue);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.PIE_ICON_COLOR_MODE,
                    value);
            mPieIconColorMode.setSummary(mPieIconColorMode.getEntries()[index]);
            return true;
        }
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateStyleValues();
    }

    private void updateStyleValues() {
        mCheckPreferences = false;
        String hexColor;
        int intColor;

        intColor = Settings.System.getInt(getActivity().getContentResolver(),
                    Settings.System.PIE_BUTTON_COLOR, -2);
        if (intColor == -2) {
            intColor = mSystemUiResources.getColor(
                    mSystemUiResources.getIdentifier(
                            "pie_background_color", "color", "com.android.systemui"));
            mPieButtonColor.setSummary(getResources().getString(R.string.default_string));
        } else {
            hexColor = String.format("#%08x", (0xffffffff & intColor));
            mPieButtonColor.setSummary(hexColor);
        }
        mPieButtonColor.setNewPreviewColor(intColor);

        intColor = Settings.System.getInt(getActivity().getContentResolver(),
                    Settings.System.PIE_BUTTON_PRESSED_COLOR, -2);
        if (intColor == -2) {
            intColor = mSystemUiResources.getColor(
                    mSystemUiResources.getIdentifier(
                            "pie_selected_color", "color", "com.android.systemui"));
            mPieButtonPressedColor.setSummary(getResources().getString(R.string.default_string));
        } else {
            hexColor = String.format("#%08x", (0xffffffff & intColor));
            mPieButtonPressedColor.setSummary(hexColor);
        }
        mPieButtonPressedColor.setNewPreviewColor(intColor);

        intColor = Settings.System.getInt(getActivity().getContentResolver(),
                    Settings.System.PIE_BUTTON_LONG_PRESSED_COLOR, -2);
        if (intColor == -2) {
            intColor = mSystemUiResources.getColor(
                    mSystemUiResources.getIdentifier(
                            "pie_long_pressed_color", "color", "com.android.systemui"));
            mPieButtonLongPressedColor.setSummary(getResources().getString(R.string.default_string));
        } else {
            hexColor = String.format("#%08x", (0xffffffff & intColor));
            mPieButtonLongPressedColor.setSummary(hexColor);
        }
        mPieButtonLongPressedColor.setNewPreviewColor(intColor);

        intColor = Settings.System.getInt(getActivity().getContentResolver(),
                    Settings.System.PIE_BUTTON_OUTLINE_COLOR, -2);
        if (intColor == -2) {
            intColor = mSystemUiResources.getColor(
                    mSystemUiResources.getIdentifier(
                            "pie_outline_color", "color", "com.android.systemui"));
            mPieButtonOutlineColor.setSummary(getResources().getString(R.string.default_string));
        } else {
            hexColor = String.format("#%08x", (0xffffffff & intColor));
            mPieButtonOutlineColor.setSummary(hexColor);
        }
        mPieButtonOutlineColor.setNewPreviewColor(intColor);

        intColor = Settings.System.getInt(getActivity().getContentResolver(),
                    Settings.System.PIE_ICON_COLOR, -2);
        if (intColor == -2) {
            intColor = mSystemUiResources.getColor(
                    mSystemUiResources.getIdentifier(
                            "pie_foreground_color", "color", "com.android.systemui"));
            mPieIconColor.setSummary(getResources().getString(R.string.default_string));
        } else {
            hexColor = String.format("#%08x", (0xffffffff & intColor));
            mPieIconColor.setSummary(hexColor);
        }
        mPieIconColor.setNewPreviewColor(intColor);

        float defaultAlpha;
        try{
            defaultAlpha = Settings.System.getFloat(getActivity()
                     .getContentResolver(), Settings.System.PIE_BUTTON_ALPHA);
        } catch (Exception e) {
            defaultAlpha = 0.3f;
            Settings.System.putFloat(getActivity().getContentResolver(),
                Settings.System.PIE_BUTTON_ALPHA, defaultAlpha);
        }
        mPieButtonAlpha.setInitValue((int) (defaultAlpha * 100));

        try{
            defaultAlpha = Settings.System.getFloat(getActivity()
                     .getContentResolver(), Settings.System.PIE_BUTTON_PRESSED_ALPHA);
        } catch (Exception e) {
            defaultAlpha = 0.0f;
            Settings.System.putFloat(getActivity().getContentResolver(),
                Settings.System.PIE_BUTTON_PRESSED_ALPHA, defaultAlpha);
        }
        mPieButtonPressedAlpha.setInitValue((int) (defaultAlpha * 100));

        int pieIconColorMode = Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.PIE_ICON_COLOR_MODE, 0);
        mPieIconColorMode.setValue(String.valueOf(pieIconColorMode));
        mPieIconColorMode.setSummary(mPieIconColorMode.getEntry());

        mCheckPreferences = true;
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

        PieButtonStyleSettings getOwner() {
            return (PieButtonStyleSettings) getTargetFragment();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int id = getArguments().getInt("id");
            switch (id) {
                case DLG_RESET:
                    return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.reset)
                    .setMessage(R.string.pie_style_reset_message)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.dlg_ok,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            getOwner().resetToDefault();
                            getOwner().updateStyleValues();
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
