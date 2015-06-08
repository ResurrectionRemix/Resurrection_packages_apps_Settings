/*
 * Copyright (C) 2012-2015 ParanoidAndroid Project
 * Portions (C) 2015 CyanideL & Fusion Project
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

package com.android.settings.paranoid;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.SwitchPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.android.internal.logging.MetricsLogger;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import net.margaritov.preference.colorpicker.ColorPickerPreference;

public class PieColor extends SettingsPreferenceFragment implements OnPreferenceChangeListener {

    private static final String PA_PIE_ENABLE_COLOR = "pa_pie_enable_color";
    private static final String PA_PIE_BACKGROUND = "pa_pie_background";
    private static final String PA_PIE_SELECT = "pa_pie_select";
    private static final String PA_PIE_OUTLINES = "pa_pie_outlines";
    private static final String PA_PIE_STATUS_CLOCK = "pa_pie_status_clock";
    private static final String PA_PIE_STATUS = "pa_pie_status";
    private static final String PA_PIE_CHEVRON = "pa_pie_chevron";
    private static final String PA_PIE_BUTTON_COLOR = "pa_pie_button_color";
    private static final String PA_PIE_JUICE = "pa_pie_juice";

    private static final int DEFAULT_COLOR = 0xffffffff;

    private static final int MENU_RESET = Menu.FIRST;
    private static final int DLG_RESET = 0;

    SwitchPreference mEnableColor;
    ColorPickerPreference mPieBg;
    ColorPickerPreference mSelect;
    ColorPickerPreference mOutlines;
    ColorPickerPreference mStatusClock;
    ColorPickerPreference mStatus;
    ColorPickerPreference mChevron;
    ColorPickerPreference mBtnColor;
    ColorPickerPreference mJuice;

    private ContentResolver mResolver;

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.APPLICATION;
    }

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
        addPreferencesFromResource(R.xml.pa_pie_color);
        mResolver = getActivity().getContentResolver();

        boolean enableColor = Settings.System.getInt(mResolver,
                Settings.System.PA_PIE_ENABLE_COLOR, 0) == 1;

        int intColor;
        String hexColor;

        mEnableColor = (SwitchPreference) findPreference(PA_PIE_ENABLE_COLOR);
        mEnableColor.setChecked(enableColor);
        mEnableColor.setOnPreferenceChangeListener(this);

        mPieBg =
                (ColorPickerPreference) findPreference(PA_PIE_BACKGROUND);
        intColor = Settings.System.getInt(mResolver,
                Settings.System.PA_PIE_BACKGROUND,
                0x04000000);
        mPieBg.setNewPreviewColor(intColor);
        hexColor = String.format("#%08x", (0x04000000 & intColor));
        mPieBg.setSummary(hexColor);
        mPieBg.setOnPreferenceChangeListener(this);
        mPieBg.setAlphaSliderEnabled(true);

        mSelect =
                (ColorPickerPreference) findPreference(PA_PIE_SELECT);
        intColor = Settings.System.getInt(mResolver,
                Settings.System.PA_PIE_SELECT,
                DEFAULT_COLOR);
        mSelect.setNewPreviewColor(intColor);
        hexColor = String.format("#%08x", (0xffffffff & intColor));
        mSelect.setSummary(hexColor);
        mSelect.setOnPreferenceChangeListener(this);
        mSelect.setAlphaSliderEnabled(true);

        mOutlines =
                (ColorPickerPreference) findPreference(PA_PIE_OUTLINES);
        intColor = Settings.System.getInt(mResolver,
                Settings.System.PA_PIE_OUTLINES,
                DEFAULT_COLOR);
        mOutlines.setNewPreviewColor(intColor);
        hexColor = String.format("#%08x", (0xffffffff & intColor));
        mOutlines.setSummary(hexColor);
        mOutlines.setOnPreferenceChangeListener(this);
        mOutlines.setAlphaSliderEnabled(true);

        mStatusClock =
                (ColorPickerPreference) findPreference(PA_PIE_STATUS_CLOCK);
        intColor = Settings.System.getInt(mResolver,
                Settings.System.PA_PIE_STATUS_CLOCK,
                DEFAULT_COLOR);
        mStatusClock.setNewPreviewColor(intColor);
        hexColor = String.format("#%08x", (0xffffffff & intColor));
        mStatusClock.setSummary(hexColor);
        mStatusClock.setOnPreferenceChangeListener(this);
        mStatusClock.setAlphaSliderEnabled(true);

        mStatus =
                (ColorPickerPreference) findPreference(PA_PIE_STATUS);
        intColor = Settings.System.getInt(mResolver,
                Settings.System.PA_PIE_STATUS,
                DEFAULT_COLOR);
        mStatus.setNewPreviewColor(intColor);
        hexColor = String.format("#%08x", (0xffffffff & intColor));
        mStatus.setSummary(hexColor);
        mStatus.setOnPreferenceChangeListener(this);
        mStatus.setAlphaSliderEnabled(true);

        mChevron =
                (ColorPickerPreference) findPreference(PA_PIE_CHEVRON);
        intColor = Settings.System.getInt(mResolver,
                Settings.System.PA_PIE_CHEVRON,
                DEFAULT_COLOR);
        mChevron.setNewPreviewColor(intColor);
        hexColor = String.format("#%08x", (0xffffffff & intColor));
        mChevron.setSummary(hexColor);
        mChevron.setOnPreferenceChangeListener(this);
        mChevron.setAlphaSliderEnabled(true);

        mBtnColor =
                (ColorPickerPreference) findPreference(PA_PIE_BUTTON_COLOR);
        intColor = Settings.System.getInt(mResolver,
                Settings.System.PA_PIE_BUTTON_COLOR,
                DEFAULT_COLOR);
        mBtnColor.setNewPreviewColor(intColor);
        hexColor = String.format("#%08x", (0xffffffff & intColor));
        mBtnColor.setSummary(hexColor);
        mBtnColor.setOnPreferenceChangeListener(this);
        mBtnColor.setAlphaSliderEnabled(true);

        mJuice =
                (ColorPickerPreference) findPreference(PA_PIE_JUICE);
        intColor = Settings.System.getInt(mResolver,
                Settings.System.PA_PIE_JUICE,
                DEFAULT_COLOR);
        mJuice.setNewPreviewColor(intColor);
        hexColor = String.format("#%08x", (0xffffffff & intColor));
        mJuice.setSummary(hexColor);
        mJuice.setOnPreferenceChangeListener(this);
        mJuice.setAlphaSliderEnabled(true);

        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, MENU_RESET, 0, R.string.reset)
                .setIcon(R.drawable.ic_settings_backup_restore)
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

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean value;

        if (preference == mEnableColor) {
            value = (Boolean) newValue;
            Settings.System.putInt(mResolver,
                Settings.System.PA_PIE_ENABLE_COLOR,
                value ? 1 : 0);
            return true;
        } else if (preference == mPieBg) {
            String hex = ColorPickerPreference.convertToARGB(Integer.valueOf(String
                    .valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.PA_PIE_BACKGROUND, intHex);
            return true;
        } else if (preference == mSelect) {
            String hex = ColorPickerPreference.convertToARGB(Integer.valueOf(String
                    .valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.PA_PIE_SELECT, intHex);
            return true;
        } else if (preference == mOutlines) {
            String hex = ColorPickerPreference.convertToARGB(Integer.valueOf(String
                    .valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.PA_PIE_OUTLINES, intHex);
            return true;
        } else if (preference == mStatusClock) {
            String hex = ColorPickerPreference.convertToARGB(Integer.valueOf(String
                    .valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.PA_PIE_STATUS_CLOCK, intHex);
            return true;
        } else if (preference == mStatus) {
            String hex = ColorPickerPreference.convertToARGB(Integer.valueOf(String
                    .valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.PA_PIE_STATUS, intHex);
            return true;
        } else if (preference == mChevron) {
            String hex = ColorPickerPreference.convertToARGB(Integer.valueOf(String
                    .valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.PA_PIE_CHEVRON, intHex);
            return true;
        } else if (preference == mBtnColor) {
            String hex = ColorPickerPreference.convertToARGB(Integer.valueOf(String
                    .valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.PA_PIE_BUTTON_COLOR, intHex);
            return true;
        } else if (preference == mJuice) {
            String hex = ColorPickerPreference.convertToARGB(Integer.valueOf(String
                    .valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.PA_PIE_JUICE, intHex);
            return true;
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

        PieColor getOwner() {
            return (PieColor) getTargetFragment();
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
                                    Settings.System.PA_PIE_ENABLE_COLOR, 1);
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.PA_PIE_BACKGROUND,
                                    0x04000000);
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.PA_PIE_SELECT,
                                    DEFAULT_COLOR);
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.PA_PIE_OUTLINES,
                                    DEFAULT_COLOR);
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.PA_PIE_STATUS_CLOCK,
                                    DEFAULT_COLOR);
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.PA_PIE_STATUS,
                                    DEFAULT_COLOR);
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.PA_PIE_CHEVRON,
                                    DEFAULT_COLOR);
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.PA_PIE_BUTTON_COLOR,
                                    DEFAULT_COLOR);
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.PA_PIE_JUICE,
                                    DEFAULT_COLOR);
                            getOwner().refreshSettings();
                        }
                    })
                    .setPositiveButton(R.string.reset_paranoid,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.PA_PIE_ENABLE_COLOR, 1);
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.PA_PIE_BACKGROUND,
                                    0x04000000);
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.PA_PIE_SELECT,
                                    0x623367e5);
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.PA_PIE_OUTLINES,
                                    0xff00ff00);
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.PA_PIE_STATUS_CLOCK,
                                    0xff1976D2);
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.PA_PIE_STATUS,
                                    0xffff0000);
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.PA_PIE_CHEVRON,
                                    0xff00ff00);
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.PA_PIE_BUTTON_COLOR,
                                    0xffffffff);
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.PA_PIE_JUICE,
                                    0xffff0000);
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
