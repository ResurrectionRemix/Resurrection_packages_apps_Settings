/*
 * Copyright (C) 2013 ParanoidAndroid Project
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
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.INotificationManager;
import android.content.Context;
import android.content.ContentResolver;
import android.database.ContentObserver;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.ServiceManager;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceClickListener;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.util.Helpers;
import net.margaritov.preference.colorpicker.ColorPickerPreference;

public class Halo extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    private static final String KEY_HALO_STATE = "halo_state";
    private static final String KEY_HALO_HIDE = "halo_hide";
    private static final String KEY_HALO_REVERSED = "halo_reversed";
    private static final String KEY_HALO_SIZE = "halo_size";
    private static final String KEY_HALO_PAUSE = "halo_pause";
    private static final String KEY_HALO_ENABLED = "halo_enabled";
    private static final String PREF_HALO_COLORS = "halo_colors";
    private static final String PREF_HALO_CIRCLE_COLOR = "halo_circle_color";
    private static final String PREF_HALO_EFFECT_COLOR = "halo_effect_color";
    private static final String PREF_HALO_BUBBLE_COLOR = "halo_bubble_color";
    private static final String PREF_HALO_BUBBLE_TEXT_COLOR = "halo_bubble_text_color";

    private ListPreference mHaloState;
    private ListPreference mHaloSize;
    private CheckBoxPreference mHaloHide;
    private CheckBoxPreference mHaloReversed;
    private CheckBoxPreference mHaloPause;
    private CheckBoxPreference mHaloEnabled;
    private CheckBoxPreference mHaloColors;
    private ColorPickerPreference mHaloCircleColor;
    private ColorPickerPreference mHaloEffectColor;
    private ColorPickerPreference mHaloBubbleColor;
    private ColorPickerPreference mHaloBubbleTextColor;

    private Context mContext;
    private INotificationManager mNotificationManager; 

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getActivity();

        addPreferencesFromResource(R.xml.halo_settings);

        mNotificationManager = INotificationManager.Stub.asInterface(
                ServiceManager.getService(Context.NOTIFICATION_SERVICE));

        mHaloEnabled = (CheckBoxPreference) findPreference(KEY_HALO_ENABLED);
        mHaloEnabled.setChecked(Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.HALO_ENABLED, 0) == 1);

        mHaloState = (ListPreference) findPreference(KEY_HALO_STATE);
        mHaloState.setValue(String.valueOf((isHaloPolicyBlack() ? "1" : "0")));
        mHaloState.setOnPreferenceChangeListener(this);

        mHaloHide = (CheckBoxPreference) findPreference(KEY_HALO_HIDE);
        mHaloHide.setChecked(Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.HALO_HIDE, 0) == 1);

        mHaloReversed = (CheckBoxPreference) findPreference(KEY_HALO_REVERSED);
        mHaloReversed.setChecked(Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.HALO_REVERSED, 1) == 1);

        int isLowRAM = (ActivityManager.isLargeRAM()) ? 0 : 1;
        mHaloPause = (CheckBoxPreference) findPreference(KEY_HALO_PAUSE);
        mHaloPause.setChecked(Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.HALO_PAUSE, isLowRAM) == 1);

        mHaloColors = (CheckBoxPreference) findPreference(PREF_HALO_COLORS);
        mHaloColors.setChecked(Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.HALO_COLORS, 0) == 1);

        mHaloEffectColor = (ColorPickerPreference) findPreference(PREF_HALO_EFFECT_COLOR);
        mHaloEffectColor.setOnPreferenceChangeListener(this);

        mHaloCircleColor = (ColorPickerPreference) findPreference(PREF_HALO_CIRCLE_COLOR);
        mHaloCircleColor.setOnPreferenceChangeListener(this);

        mHaloBubbleColor = (ColorPickerPreference) findPreference(PREF_HALO_BUBBLE_COLOR);
        mHaloBubbleColor.setOnPreferenceChangeListener(this);

        mHaloBubbleTextColor = (ColorPickerPreference) findPreference(PREF_HALO_BUBBLE_TEXT_COLOR);
        mHaloBubbleTextColor.setOnPreferenceChangeListener(this);

        mHaloSize = (ListPreference) findPreference(KEY_HALO_SIZE);
        try {
            float haloSize = Settings.System.getFloat(mContext.getContentResolver(),
                    Settings.System.HALO_SIZE, 1.0f);
            mHaloSize.setValue(String.valueOf(haloSize));  
        } catch(Exception ex) {
            // So what
        }
        mHaloSize.setOnPreferenceChangeListener(this);
        }

    private boolean isHaloPolicyBlack() {
        try {
            return mNotificationManager.isHaloPolicyBlack();
        } catch (android.os.RemoteException ex) {
                // System dead
        }
        return true;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if  (preference == mHaloEnabled) {  
            Settings.System.putInt(mContext.getContentResolver(),
                    Settings.System.HALO_ENABLED, mHaloEnabled.isChecked()
                    ? 1 : 0);  
        } else if  (preference == mHaloHide) {
            Settings.System.putInt(mContext.getContentResolver(),
                    Settings.System.HALO_HIDE, mHaloHide.isChecked()
                    ? 1 : 0);
        } else if (preference == mHaloReversed) {
            Settings.System.putInt(mContext.getContentResolver(),
                    Settings.System.HALO_REVERSED, mHaloReversed.isChecked()
                    ? 1 : 0);
        } else if (preference == mHaloPause) {
            Settings.System.putInt(mContext.getContentResolver(),
                    Settings.System.HALO_PAUSE, mHaloPause.isChecked()
                    ? 1 : 0);
        } else if (preference == mHaloColors) {
            Settings.System.putInt(mContext.getContentResolver(),
                    Settings.System.HALO_COLORS, mHaloColors.isChecked()
                    ? 1 : 0);
            Helpers.restartSystemUI();
        }	
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    public boolean onPreferenceChange(Preference preference, Object Value) {
        if (preference == mHaloState) {
            boolean state = Integer.valueOf((String) Value) == 1;
            try {
                mNotificationManager.setHaloPolicyBlack(state);
            } catch (android.os.RemoteException ex) {
                // System dead
            }
            return true;
        } else if (preference == mHaloSize) {
            float haloSize = Float.valueOf((String) Value);
            Settings.System.putFloat(getActivity().getContentResolver(),
                    Settings.System.HALO_SIZE, haloSize);
            return true;
        } else if (preference == mHaloCircleColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(Value)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.HALO_CIRCLE_COLOR, intHex);
            return true;
        } else if (preference == mHaloEffectColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(Value)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.HALO_EFFECT_COLOR, intHex);
            Helpers.restartSystemUI();
            return true;
        } else if (preference == mHaloBubbleColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(Value)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.HALO_BUBBLE_COLOR, intHex);
            return true;
        } else if (preference == mHaloBubbleTextColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(Value)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.HALO_BUBBLE_TEXT_COLOR, intHex);
            return true;
        }
        return false;
    }
}
