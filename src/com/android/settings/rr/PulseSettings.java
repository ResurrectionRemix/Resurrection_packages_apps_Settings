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

import java.util.ArrayList;

import net.margaritov.preference.colorpicker.ColorPickerPreference;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import android.app.ActionBar;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.SwitchPreference;
import android.preference.Preference;
import android.preference.ListPreference;
import android.provider.Settings;

import android.provider.SearchIndexableResource;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;

import java.util.ArrayList;
import java.util.List;

public class PulseSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener , Indexable {
    private static final String TAG = PulseSettings.class.getSimpleName();
    private static final String CUSTOM_DIMEN = "pulse_custom_dimen";
    private static final String CUSTOM_DIV = "pulse_custom_div";
    private static final String PULSE_BLOCK = "pulse_filled_block_size";
    private static final String EMPTY_BLOCK = "pulse_empty_block_size";
    private static final String FUDGE_FACOR = "pulse_custom_fudge_factor";

    SwitchPreference mShowPulse;
    SwitchPreference mLavaLampEnabled;
    ColorPickerPreference mPulseColor;
    ListPreference mCustomDimen;
    ListPreference mCustomDiv;
    ListPreference mFilled;
    ListPreference mEmpty;
    ListPreference mFudge;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pulse_settings);

        ActionBar bar = getActivity().getActionBar();
        if (bar != null) {
            bar.setTitle(R.string.pulse_settings);
        }

        mShowPulse = (SwitchPreference) findPreference("eos_fling_show_pulse");
        mShowPulse.setChecked(Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.FLING_PULSE_ENABLED, 0) == 1);
        mShowPulse.setOnPreferenceChangeListener(this);

        int pulseColor = Settings.Secure.getIntForUser(getContentResolver(),
                Settings.Secure.FLING_PULSE_COLOR, Color.WHITE, UserHandle.USER_CURRENT);
        mPulseColor = (ColorPickerPreference) findPreference("eos_fling_pulse_color");
        mPulseColor.setNewPreviewColor(pulseColor);
        mPulseColor.setOnPreferenceChangeListener(this);

        mLavaLampEnabled = (SwitchPreference) findPreference("eos_fling_lavalamp");
        mLavaLampEnabled.setChecked(Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.FLING_PULSE_LAVALAMP_ENABLED, 1) == 1);
        mLavaLampEnabled.setOnPreferenceChangeListener(this);
        
        mCustomDimen = (ListPreference) findPreference(CUSTOM_DIMEN);
        int customdimen = Settings.Secure.getIntForUser(getContentResolver(),
                    Settings.Secure.PULSE_CUSTOM_DIMEN, 0,
                    UserHandle.USER_CURRENT);
        mCustomDimen.setValue(String.valueOf(customdimen));
        mCustomDimen.setSummary(mCustomDimen.getEntry());
        mCustomDimen.setOnPreferenceChangeListener(this);
        
        mCustomDiv = (ListPreference) findPreference(CUSTOM_DIV);
        int customdiv = Settings.Secure.getIntForUser(getContentResolver(),
                    Settings.Secure.PULSE_CUSTOM_DIV, 0,
                    UserHandle.USER_CURRENT);
        mCustomDiv.setValue(String.valueOf(customdiv));
        mCustomDiv.setSummary(mCustomDiv.getEntry());
        mCustomDiv.setOnPreferenceChangeListener(this);
        
        mFilled = (ListPreference) findPreference(PULSE_BLOCK);
        int filled = Settings.Secure.getIntForUser(getContentResolver(),
                    Settings.Secure.PULSE_FILLED_BLOCK_SIZE, 0,
                    UserHandle.USER_CURRENT);
        mFilled.setValue(String.valueOf(filled));
        mFilled.setSummary(mFilled.getEntry());
        mFilled.setOnPreferenceChangeListener(this);
        
        mEmpty = (ListPreference) findPreference(EMPTY_BLOCK);
        int empty = Settings.Secure.getIntForUser(getContentResolver(),
                    Settings.Secure.PULSE_EMPTY_BLOCK_SIZE, 0,
                    UserHandle.USER_CURRENT);
        mEmpty.setValue(String.valueOf(empty));
        mEmpty.setSummary(mEmpty.getEntry());
        mEmpty.setOnPreferenceChangeListener(this);
        
        mFudge = (ListPreference) findPreference(FUDGE_FACOR);
        int fudge = Settings.Secure.getIntForUser(getContentResolver(),
                    Settings.Secure.PULSE_CUSTOM_FUDGE_FACTOR, 0,
                    UserHandle.USER_CURRENT);
        mFudge.setValue(String.valueOf(fudge));
        mFudge.setSummary(mFudge.getEntry());
        mFudge.setOnPreferenceChangeListener(this);

    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference.equals(mShowPulse)) {
            boolean enabled = ((Boolean) newValue).booleanValue();
            Settings.Secure.putInt(getContentResolver(),
                    Settings.Secure.FLING_PULSE_ENABLED, enabled ? 1 : 0);
            return true;
        } else if (preference.equals(mPulseColor)) {
            int color = ((Integer) newValue).intValue();
            Settings.Secure.putInt(getContentResolver(),
                    Settings.Secure.FLING_PULSE_COLOR, color);
            return true;
        } else if (preference.equals(mLavaLampEnabled)) {
            boolean enabled = ((Boolean) newValue).booleanValue();
            Settings.Secure.putInt(getContentResolver(),
                    Settings.Secure.FLING_PULSE_LAVALAMP_ENABLED, enabled ? 1 : 0);
            return true;
        } else if (preference == mCustomDimen) {
                int customdimen = Integer.valueOf((String) newValue);
                int index = mCustomDimen.findIndexOfValue((String) newValue);
                Settings.Secure.putIntForUser(
                       getContentResolver(), 
		Settings.Secure.PULSE_CUSTOM_DIMEN, customdimen,
                        UserHandle.USER_CURRENT);
                mCustomDimen.setSummary(
                        mCustomDimen.getEntries()[index]);
                return true;
	}  else if (preference == mCustomDiv) {
                int customdiv = Integer.valueOf((String) newValue);
                int index = mCustomDiv.findIndexOfValue((String) newValue);
                Settings.Secure.putIntForUser(
                       getContentResolver(), 
		Settings.Secure.PULSE_CUSTOM_DIV, customdiv,
                        UserHandle.USER_CURRENT);
                mCustomDiv.setSummary(
                        mCustomDiv.getEntries()[index]);
                return true;
	} else if (preference == mFilled) {
                int filled = Integer.valueOf((String) newValue);
                int index = mFilled.findIndexOfValue((String) newValue);
                Settings.Secure.putIntForUser(
                       getContentResolver(), 
		Settings.Secure.PULSE_FILLED_BLOCK_SIZE, filled,
                        UserHandle.USER_CURRENT);
                mFilled.setSummary(
                        mFilled.getEntries()[index]);
                return true;
	}  else if (preference == mEmpty) {
                int empty = Integer.valueOf((String) newValue);
                int index = mEmpty.findIndexOfValue((String) newValue);
                Settings.Secure.putIntForUser(
                       getContentResolver(), 
		Settings.Secure.PULSE_EMPTY_BLOCK_SIZE, empty,
                        UserHandle.USER_CURRENT);
                mEmpty.setSummary(
                        mEmpty.getEntries()[index]);
                return true;
	} else if (preference == mFudge) {
                int fudge = Integer.valueOf((String) newValue);
                int index = mFudge.findIndexOfValue((String) newValue);
                Settings.Secure.putIntForUser(
                       getContentResolver(), 
		Settings.Secure.PULSE_CUSTOM_FUDGE_FACTOR, fudge,
                        UserHandle.USER_CURRENT);
                mFudge.setSummary(
                        mFudge.getEntries()[index]);
                return true;
	}
        return false;
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.DONT_TRACK_ME_BRO;
    }
    
    	    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                                                                             boolean enabled) {
                     ArrayList<SearchIndexableResource> result =
                             new ArrayList<SearchIndexableResource>();
 
                     SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.pulse_settings;
                     result.add(sir);
 
                     return result;
                 }
 
                 @Override
                 public List<String> getNonIndexableKeys(Context context) {
                     final List<String> keys = new ArrayList<String>();
                     return keys;
                 }
         };
}
