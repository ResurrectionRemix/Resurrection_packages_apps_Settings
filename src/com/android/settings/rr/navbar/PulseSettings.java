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

package com.android.settings.rr.navbar;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import android.app.ActionBar;
import android.graphics.Color;
import android.os.Bundle;
import android.os.UserHandle;
import com.android.settings.rr.SeekBarPreference;

import net.margaritov.preference.colorpicker.ColorPickerPreference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.ListPreference;
import android.provider.Settings;

public class PulseSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String TAG = PulseSettings.class.getSimpleName();
    private static final String CUSTOM_DIMEN = "pulse_custom_dimen";
    private static final String CUSTOM_DIV = "pulse_custom_div";
    private static final String PULSE_BLOCK = "pulse_filled_block_size";
    private static final String EMPTY_BLOCK = "pulse_empty_block_size";
    private static final String FUDGE_FACOR = "pulse_custom_fudge_factor";
    private static final int RENDER_STYLE_FADING_BARS = 0;
    private static final int RENDER_STYLE_SOLID_LINES = 1;
    private static final String SOLID_FUDGE = "pulse_solid_fudge_factor";
    private static final String SOLID_LAVAMP_SPEED = "lavamp_solid_speed";
    private static final String FADING_LAVAMP_SPEED = "fling_pulse_lavalamp_speed";
    private static final String PULSE_SOLID_UNITS_COUNT = "pulse_solid_units_count";
    private static final String PULSE_SOLID_UNITS_OPACITY = "pulse_solid_units_opacity";
    private static final String FADING_BLOCKS_OPACITY = "pulse_fading_blocks_opacity";
    private static final String PULSE_CUSTOM_BUTTONS_OPACITY = "pulse_custom_buttons_opacity";

    private static final String FADING_BLOCK_CAT = "pulse_fading_bars_category";
    private static final String SOLID_CAT = "pulse_2";

    SwitchPreference mShowPulse;
    ListPreference mRenderMode;
    ColorPickerPreference mPulseColor;
    ColorPickerPreference mLavaLampColorFrom;
    ColorPickerPreference mLavaLampColorTo;
    SwitchPreference mLavaLampEnabled;
    SeekBarPreference mCustomDimen;
    SeekBarPreference mCustomDiv;
    SeekBarPreference mFilled;
    SeekBarPreference mEmpty;
    SeekBarPreference mFudge;
    SeekBarPreference mSolidFudge;
    SeekBarPreference mSolidSpeed;
    SeekBarPreference mFadingSpeed;
    SeekBarPreference mSolidCount;
    SeekBarPreference mSolidOpacity;
    SeekBarPreference mFadingOpacity;
    SeekBarPreference mNavButtonsOpacity;

    PreferenceCategory mFadingBlockCat;
    PreferenceCategory mSolidCat;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pulse_settings);

        mShowPulse = (SwitchPreference) findPreference("eos_fling_show_pulse");
        mShowPulse.setChecked(Settings.Secure.getIntForUser(getContentResolver(),
                Settings.Secure.FLING_PULSE_ENABLED, 0, UserHandle.USER_CURRENT) == 1);
        mShowPulse.setOnPreferenceChangeListener(this);

        int renderMode = Settings.Secure.getIntForUser(getContentResolver(),
                Settings.Secure.PULSE_RENDER_STYLE_URI, RENDER_STYLE_SOLID_LINES, UserHandle.USER_CURRENT);
        mRenderMode = (ListPreference) findPreference("pulse_render_mode");
        mRenderMode.setValue(String.valueOf(renderMode));
        mRenderMode.setOnPreferenceChangeListener(this);

        mFadingBlockCat = (PreferenceCategory) findPreference(FADING_BLOCK_CAT);
        mSolidCat = (PreferenceCategory) findPreference(SOLID_CAT);

        int pulseColor = Settings.Secure.getIntForUser(getContentResolver(),
                Settings.Secure.FLING_PULSE_COLOR, Color.WHITE, UserHandle.USER_CURRENT);
        mPulseColor = (ColorPickerPreference) findPreference("eos_fling_pulse_color");
        mPulseColor.setNewPreviewColor(pulseColor);
        mPulseColor.setOnPreferenceChangeListener(this);

        int lavaLampColorFrom = Settings.Secure.getIntForUser(getContentResolver(),
                Settings.Secure.FLING_PULSE_LAVALAMP_COLOR_FROM, 0xffff8080, UserHandle.USER_CURRENT);
        mLavaLampColorFrom = (ColorPickerPreference) findPreference("fling_lavalamp_color_from");
        mLavaLampColorFrom.setNewPreviewColor(lavaLampColorFrom);
        mLavaLampColorFrom.setOnPreferenceChangeListener(this);

        int lavaLampColorTo = Settings.Secure.getIntForUser(getContentResolver(),
                Settings.Secure.FLING_PULSE_LAVALAMP_COLOR_TO, 0xff8080ff, UserHandle.USER_CURRENT);
        mLavaLampColorTo = (ColorPickerPreference) findPreference("fling_lavalamp_color_to");
        mLavaLampColorTo.setNewPreviewColor(lavaLampColorTo);
        mLavaLampColorTo.setOnPreferenceChangeListener(this);

        mLavaLampEnabled = (SwitchPreference) findPreference("eos_fling_lavalamp");
        mLavaLampEnabled.setChecked(Settings.Secure.getIntForUser(getContentResolver(),
                Settings.Secure.FLING_PULSE_LAVALAMP_ENABLED, 1, UserHandle.USER_CURRENT) == 1);
        mLavaLampEnabled.setOnPreferenceChangeListener(this);

        int customdimen = Settings.Secure.getIntForUser(getContentResolver(),
                Settings.Secure.PULSE_CUSTOM_DIMEN, 14, UserHandle.USER_CURRENT);
        mCustomDimen = (SeekBarPreference) findPreference(CUSTOM_DIMEN);
        mCustomDimen.setValue(customdimen);
        mCustomDimen.setOnPreferenceChangeListener(this);

        int customdiv = Settings.Secure.getIntForUser(getContentResolver(),
                Settings.Secure.PULSE_CUSTOM_DIV, 16, UserHandle.USER_CURRENT);
        mCustomDiv = (SeekBarPreference) findPreference(CUSTOM_DIV);
        mCustomDiv.setValue(customdiv);
        mCustomDiv.setOnPreferenceChangeListener(this);

        int filled = Settings.Secure.getIntForUser(getContentResolver(),
                Settings.Secure.PULSE_FILLED_BLOCK_SIZE, 4, UserHandle.USER_CURRENT);
        mFilled = (SeekBarPreference) findPreference(PULSE_BLOCK);
        mFilled.setValue(filled);
        mFilled.setOnPreferenceChangeListener(this);

        int empty = Settings.Secure.getIntForUser(getContentResolver(),
                Settings.Secure.PULSE_EMPTY_BLOCK_SIZE, 1, UserHandle.USER_CURRENT);
        mEmpty = (SeekBarPreference) findPreference(EMPTY_BLOCK);
        mEmpty.setValue(empty);
        mEmpty.setOnPreferenceChangeListener(this);

        int fudge = Settings.Secure.getIntForUser(getContentResolver(),
                Settings.Secure.PULSE_CUSTOM_FUDGE_FACTOR, 4, UserHandle.USER_CURRENT);
        mFudge = (SeekBarPreference) findPreference(FUDGE_FACOR);
        mFudge.setValue(fudge);
        mFudge.setOnPreferenceChangeListener(this);

        int solidfudge = Settings.Secure.getIntForUser(getContentResolver(),
                Settings.Secure.PULSE_SOLID_FUDGE_FACTOR, 5,
                UserHandle.USER_CURRENT);
        mSolidFudge = (SeekBarPreference) findPreference(SOLID_FUDGE);
        mSolidFudge.setValue(solidfudge);
        mSolidFudge.setOnPreferenceChangeListener(this);

        int speed = Settings.Secure.getIntForUser(getContentResolver(),
                Settings.Secure.PULSE_LAVALAMP_SOLID_SPEED, 10000, UserHandle.USER_CURRENT);
        mSolidSpeed =
                (SeekBarPreference) findPreference(SOLID_LAVAMP_SPEED);
        mSolidSpeed.setValue(speed);
        mSolidSpeed.setOnPreferenceChangeListener(this);

        int fspeed = Settings.Secure.getIntForUser(getContentResolver(),
                Settings.Secure.FLING_PULSE_LAVALAMP_SPEED, 10000, UserHandle.USER_CURRENT);
        mFadingSpeed =
                (SeekBarPreference) findPreference(FADING_LAVAMP_SPEED);
        mFadingSpeed.setValue(fspeed);
        mFadingSpeed.setOnPreferenceChangeListener(this);

        int count = Settings.Secure.getIntForUser(getContentResolver(),
                Settings.Secure.PULSE_SOLID_UNITS_COUNT, 64, UserHandle.USER_CURRENT);
        mSolidCount =
                (SeekBarPreference) findPreference(PULSE_SOLID_UNITS_COUNT);
        mSolidCount.setValue(count);
        mSolidCount.setOnPreferenceChangeListener(this);

        int opacity = Settings.Secure.getIntForUser(getContentResolver(),
                Settings.Secure.PULSE_SOLID_UNITS_OPACITY, 200, UserHandle.USER_CURRENT);
        mSolidOpacity =
                (SeekBarPreference) findPreference(PULSE_SOLID_UNITS_OPACITY);
        mSolidOpacity.setValue(opacity);
        mSolidOpacity.setOnPreferenceChangeListener(this);

        int buttonsOpacity = Settings.Secure.getIntForUser(getContentResolver(),
                Settings.Secure.PULSE_CUSTOM_BUTTONS_OPACITY, 200, UserHandle.USER_CURRENT);
        mNavButtonsOpacity =
                (SeekBarPreference) findPreference(PULSE_CUSTOM_BUTTONS_OPACITY);
        mNavButtonsOpacity.setValue(buttonsOpacity);
        mNavButtonsOpacity.setOnPreferenceChangeListener(this);

        int fopacity = Settings.Secure.getIntForUser(getContentResolver(),
                Settings.Secure.PULSE_FADING_BLOCKS_OPACITY, 150, UserHandle.USER_CURRENT);
        mFadingOpacity =
                (SeekBarPreference) findPreference(FADING_BLOCKS_OPACITY);
        mFadingOpacity.setValue(fopacity);
        mFadingOpacity.setOnPreferenceChangeListener(this);
        UpdateSettings(renderMode);

    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference.equals(mRenderMode)) {
            int mode = Integer.valueOf((String) newValue);
            Settings.Secure.putIntForUser(getContentResolver(),
                    Settings.Secure.PULSE_RENDER_STYLE_URI, mode, UserHandle.USER_CURRENT);
            PreferenceCategory fadingBarsCat = (PreferenceCategory)findPreference("pulse_fading_bars_category");
            fadingBarsCat.setEnabled(mode == RENDER_STYLE_FADING_BARS);
            PreferenceCategory solidBarsCat = (PreferenceCategory)findPreference("pulse_2");
            solidBarsCat.setEnabled(mode == RENDER_STYLE_SOLID_LINES);
            UpdateSettings(mode);
            return true;
        } else if (preference.equals(mShowPulse)) {
            boolean enabled = ((Boolean) newValue).booleanValue();
            Settings.Secure.putIntForUser(getContentResolver(),
                    Settings.Secure.FLING_PULSE_ENABLED, enabled ? 1 : 0, UserHandle.USER_CURRENT);
            return true;
        } else if (preference.equals(mPulseColor)) {
            int color = ((Integer) newValue).intValue();
            Settings.Secure.putIntForUser(getContentResolver(),
                    Settings.Secure.FLING_PULSE_COLOR, color, UserHandle.USER_CURRENT);
            return true;
        } else if (preference.equals(mLavaLampColorFrom)) {
            int color = ((Integer) newValue).intValue();
            Settings.Secure.putIntForUser(getContentResolver(),
                    Settings.Secure.FLING_PULSE_LAVALAMP_COLOR_FROM, color, UserHandle.USER_CURRENT);
            return true;
        } else if (preference.equals(mLavaLampColorTo)) {
            int color = ((Integer) newValue).intValue();
            Settings.Secure.putIntForUser(getContentResolver(),
                    Settings.Secure.FLING_PULSE_LAVALAMP_COLOR_TO, color, UserHandle.USER_CURRENT);
            return true;
        } else if (preference.equals(mLavaLampEnabled)) {
            boolean enabled = ((Boolean) newValue).booleanValue();
            Settings.Secure.putIntForUser(getContentResolver(),
                    Settings.Secure.FLING_PULSE_LAVALAMP_ENABLED, enabled ? 1 : 0,
                    UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mCustomDimen) {
            int val = (Integer) newValue;
            Settings.Secure.putIntForUser(getContentResolver(),
                    Settings.Secure.PULSE_CUSTOM_DIMEN, val, UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mCustomDiv) {
            int val = (Integer) newValue;
            Settings.Secure.putIntForUser(getContentResolver(),
                    Settings.Secure.PULSE_CUSTOM_DIV, val, UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mFilled) {
            int val = (Integer) newValue;
            Settings.Secure.putIntForUser(getContentResolver(),
                    Settings.Secure.PULSE_FILLED_BLOCK_SIZE, val, UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mEmpty) {
            int val = (Integer) newValue;
            Settings.Secure.putIntForUser(getContentResolver(),
                    Settings.Secure.PULSE_EMPTY_BLOCK_SIZE, val, UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mFudge) {
            int val = (Integer) newValue;
            Settings.Secure.putIntForUser(getContentResolver(),
                    Settings.Secure.PULSE_CUSTOM_FUDGE_FACTOR, val, UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mSolidFudge) {
            int val = (Integer) newValue;
            Settings.Secure.putIntForUser(
                    getContentResolver(),
                    Settings.Secure.PULSE_SOLID_FUDGE_FACTOR, val,
                    UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mSolidSpeed) {
            int val = (Integer) newValue;
            Settings.Secure.putIntForUser(getContentResolver(),
                    Settings.Secure.PULSE_LAVALAMP_SOLID_SPEED, val, UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mFadingSpeed) {
            int val = (Integer) newValue;
            Settings.Secure.putIntForUser(getContentResolver(),
                    Settings.Secure.FLING_PULSE_LAVALAMP_SPEED, val, UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mSolidCount) {
            int val = (Integer) newValue;
            Settings.Secure.putIntForUser(getContentResolver(),
                    Settings.Secure.PULSE_SOLID_UNITS_COUNT, val, UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mSolidOpacity) {
            int val = (Integer) newValue;
            Settings.Secure.putIntForUser(getContentResolver(),
                    Settings.Secure.PULSE_SOLID_UNITS_OPACITY, val, UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mNavButtonsOpacity) {
            int val = (Integer) newValue;
            Settings.Secure.putIntForUser(getContentResolver(),
                    Settings.Secure.PULSE_CUSTOM_BUTTONS_OPACITY, val, UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mFadingOpacity) {
            int val = (Integer) newValue;
            Settings.Secure.putIntForUser(getContentResolver(),
                    Settings.Secure.PULSE_FADING_BLOCKS_OPACITY, val, UserHandle.USER_CURRENT);
            return true;
        }
        return false;
    }


    public void UpdateSettings(int mode) {
        if (mode == 0) {
           mFadingBlockCat.setEnabled(true);
           mSolidCat.setEnabled(false);
        } else {
           mFadingBlockCat.setEnabled(false);
           mSolidCat.setEnabled(true);
        }
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.RESURRECTED;
    }
}
