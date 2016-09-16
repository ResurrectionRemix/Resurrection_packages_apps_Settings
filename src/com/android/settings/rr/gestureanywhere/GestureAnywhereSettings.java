/*
 * Copyright (C) 2013 The ChameleonOS Project
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

package com.android.settings.rr.gestureanywhere;

import android.app.ActionBar;
import android.content.ContentResolver;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import android.support.v7.preference.ListPreference;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.PreferenceScreen;
import android.provider.Settings;
import android.view.Gravity;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import com.android.internal.logging.MetricsProto.MetricsEvent;

public class GestureAnywhereSettings extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener {
    private static final String TAG = "GestureAnywhereSettings";

    private static final String KEY_ENABLED = "gesture_anywhere_enabled";
    private static final String KEY_POSITION = "gesture_anywhere_position";
    private static final String KEY_GESTURES = "gesture_anywhere_gestures";
    private static final String KEY_TRIGGER_WIDTH = "gesture_anywhere_trigger_width";
    private static final String KEY_TRIGGER_TOP = "gesture_anywhere_trigger_top";
    private static final String KEY_TRIGGER_BOTTOM = "gesture_anywhere_trigger_bottom";

    private SwitchPreference mEnabledPref;
    private ListPreference mPositionPref;
    private ListPreference mTriggerWidthPref;
    private ListPreference mTriggerTopPref;
    private ListPreference mTriggerBottomPref;

    private CharSequence mPreviousTitle;

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.APPLICATION;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.gesture_anywhere);

        mEnabledPref = (SwitchPreference) findPreference(KEY_ENABLED);
        mEnabledPref.setChecked((Settings.System.getInt(getContentResolver(),
                Settings.System.GESTURE_ANYWHERE_ENABLED, 0) == 1));
        mEnabledPref.setOnPreferenceChangeListener(this);

        PreferenceScreen prefSet = getPreferenceScreen();
        mPositionPref = (ListPreference) prefSet.findPreference(KEY_POSITION);
        mPositionPref.setOnPreferenceChangeListener(this);
        int position = Settings.System.getInt(getContentResolver(),
                Settings.System.GESTURE_ANYWHERE_POSITION, Gravity.LEFT);
        mPositionPref.setValue(String.valueOf(position));
        updatePositionSummary(position);

        mTriggerWidthPref = (ListPreference) findPreference(KEY_TRIGGER_WIDTH);
        mTriggerWidthPref.setOnPreferenceChangeListener(this);
		int width = Settings.System.getInt(getContentResolver(),
                Settings.System.GESTURE_ANYWHERE_TRIGGER_WIDTH, 10);
        mTriggerWidthPref.setValue(String.valueOf(width));
		mTriggerWidthPref.setSummary(mTriggerWidthPref.getEntry());

        mTriggerTopPref = (ListPreference) findPreference(KEY_TRIGGER_TOP);
        mTriggerTopPref.setOnPreferenceChangeListener(this);
		int twidth = Settings.System.getInt(getContentResolver(),
                Settings.System.GESTURE_ANYWHERE_TRIGGER_TOP, 0);
        mTriggerTopPref.setValue(String.valueOf(twidth));
		mTriggerTopPref.setSummary(mTriggerWidthPref.getEntry());

        mTriggerBottomPref = (ListPreference) findPreference(KEY_TRIGGER_BOTTOM);
		mTriggerBottomPref.setOnPreferenceChangeListener(this);
		int height = Settings.System.getInt(getContentResolver(),
                Settings.System.GESTURE_ANYWHERE_TRIGGER_HEIGHT, 100);
        mTriggerBottomPref.setValue(String.valueOf(height));
		mTriggerBottomPref.setSummary(mTriggerBottomPref.getEntry());

        Preference pref = findPreference(KEY_GESTURES);
        pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(getActivity(), GestureAnywhereBuilderActivity.class));
                return true;
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        final ActionBar bar = getActivity().getActionBar();
        mPreviousTitle = bar.getTitle();
        bar.setTitle(R.string.gesture_anywhere_title);
    }

    @Override
    public void onStop() {
        super.onStop();
        getActivity().getActionBar().setTitle(mPreviousTitle);
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
		ContentResolver resolver = getActivity().getContentResolver();
        if (preference == mPositionPref) {
            int position = Integer.valueOf((String) newValue);
            updatePositionSummary(position);
            return true;
        } else if (preference == mEnabledPref) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.GESTURE_ANYWHERE_ENABLED,
                    ((Boolean) newValue).booleanValue() ? 1 : 0);
            return true;
        } else if (preference == mTriggerWidthPref) {
                int val = Integer.parseInt((String) newValue);
                int width = mTriggerWidthPref.findIndexOfValue((String) newValue);
                Settings.System.putIntForUser(resolver,
                        Settings.System.GESTURE_ANYWHERE_TRIGGER_WIDTH, val, UserHandle.USER_CURRENT);
                mTriggerWidthPref.setSummary(mTriggerWidthPref.getEntries()[width]);
            return true;
        } else if (preference == mTriggerTopPref) {
                int val = Integer.parseInt((String) newValue);
                int width = mTriggerTopPref.findIndexOfValue((String) newValue);
                Settings.System.putIntForUser(resolver,
                        Settings.System.GESTURE_ANYWHERE_TRIGGER_TOP, val, UserHandle.USER_CURRENT);
                mTriggerTopPref.setSummary(mTriggerTopPref.getEntries()[width]);
            return true;
        } else if (preference == mTriggerBottomPref) {
                int val = Integer.parseInt((String) newValue);
                int width = mTriggerBottomPref.findIndexOfValue((String) newValue);
                Settings.System.putIntForUser(resolver,
                        Settings.System.GESTURE_ANYWHERE_TRIGGER_HEIGHT, val, UserHandle.USER_CURRENT);
                mTriggerBottomPref.setSummary(mTriggerBottomPref.getEntries()[width]);
            return true;
        }
        return false;
    }

    private void updatePositionSummary(int value) {
        mPositionPref.setSummary(mPositionPref.getEntries()[mPositionPref.findIndexOfValue("" + value)]);
        Settings.System.putInt(getContentResolver(),
                Settings.System.GESTURE_ANYWHERE_POSITION, value);
    }

    @Override
    public void onPause() {
        super.onPause();
        Settings.System.putInt(getContentResolver(),
                Settings.System.GESTURE_ANYWHERE_SHOW_TRIGGER, 0);
    }

    @Override
    public void onResume() {
        super.onResume();
        Settings.System.putInt(getContentResolver(),
                Settings.System.GESTURE_ANYWHERE_SHOW_TRIGGER, 1);
    }
}
