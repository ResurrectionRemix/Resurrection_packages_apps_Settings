/*
 *  Copyright (C) 2013 The OmniROM Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.android.settings.rr;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.util.Log;
import android.text.TextUtils;

import com.android.settings.rr.SeekBarPreferenceCham;
import com.android.internal.util.rr.AwesomeAnimationHelper;
import com.android.settings.SettingsPreferenceFragment;
import com.android.internal.logging.MetricsLogger;

import com.android.settings.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;


    public class KeyboardAnimationInterfaceSettings extends  SettingsPreferenceFragment
            implements OnPreferenceChangeListener {

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.DEVELOPMENT;
    }

        private static final String TAG = "KeyboardAnimationInterfaceSettings";


        private static final String IME_ENTER_ANIMATION = "ime_enter_animation";
        private static final String IME_EXIT_ANIMATION = "ime_exit_animation";
        private static final String IME_INTERPOLATOR = "ime_interpolator";
        private static final String IME_ANIM_DURATION = "ime_anim_duration";
        private static final String IME_ANIM_RESET = "ime_anim_reset";

        private ListPreference mAnimationImeEnter;
        private ListPreference mAnimationImeExit;
        private ListPreference mAnimationImeInterpolator;
        private SeekBarPreferenceCham mAnimationImeDuration;
        private Preference mAnimationImeReset;

        private int[] mAnimations;
        private String[] mAnimationsStrings;
        private String[] mAnimationsNum;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.keyboard_animation_interface_settings);

            PreferenceScreen prefSet = getPreferenceScreen();
            ContentResolver resolver = getActivity().getContentResolver();

            mAnimations = AwesomeAnimationHelper.getAnimationsList();
            int animqty = mAnimations.length;
            mAnimationsStrings = new String[animqty];
            mAnimationsNum = new String[animqty];
            for (int i = 0; i < animqty; i++) {
                mAnimationsStrings[i] = AwesomeAnimationHelper.getProperName(getActivity().getResources(), mAnimations[i]);
                mAnimationsNum[i] = String.valueOf(mAnimations[i]);
            }

            mAnimationImeEnter = (ListPreference) prefSet.findPreference(IME_ENTER_ANIMATION);
            mAnimationImeEnter.setOnPreferenceChangeListener(this);
            if (getProperVal(mAnimationImeEnter) != null) {
                mAnimationImeEnter.setValue(getProperVal(mAnimationImeEnter));
                mAnimationImeEnter.setSummary(getProperSummary(mAnimationImeEnter));
            }
            mAnimationImeEnter.setEntries(mAnimationsStrings);
            mAnimationImeEnter.setEntryValues(mAnimationsNum);

            mAnimationImeExit = (ListPreference) prefSet.findPreference(IME_EXIT_ANIMATION);
            mAnimationImeExit.setOnPreferenceChangeListener(this);
            if (getProperVal(mAnimationImeExit) != null) {
                mAnimationImeExit.setValue(getProperVal(mAnimationImeExit));
                mAnimationImeExit.setSummary(getProperSummary(mAnimationImeExit));
            }
            mAnimationImeExit.setEntries(mAnimationsStrings);
            mAnimationImeExit.setEntryValues(mAnimationsNum);

            mAnimationImeInterpolator = (ListPreference) prefSet.findPreference(IME_INTERPOLATOR);
            if (getProperVal(mAnimationImeInterpolator) != null) {
                mAnimationImeInterpolator.setValue(getProperVal(mAnimationImeInterpolator));
                mAnimationImeInterpolator.setSummary(getListInterpolatorName(Integer.valueOf(getProperVal(mAnimationImeInterpolator))));
            }
            mAnimationImeInterpolator.setOnPreferenceChangeListener(this);

            int imeDuration = Settings.System.getInt(resolver,
                    Settings.System.ANIMATION_IME_DURATION, 0);
            mAnimationImeDuration = (SeekBarPreferenceCham) prefSet.findPreference(IME_ANIM_DURATION);
            mAnimationImeDuration.setValue(imeDuration);
            mAnimationImeDuration.setOnPreferenceChangeListener(this);

            mAnimationImeReset = prefSet.findPreference(IME_ANIM_RESET);

        }

        private void resetToDefault() {
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
            alertDialog.setTitle(R.string.reset);
            alertDialog.setMessage(R.string.animation_settings_reset_message);
            alertDialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    resetAllValues();
                    resetAllSettings();
                }
            });
            alertDialog.setNegativeButton(R.string.cancel, null);
            alertDialog.create().show();
        }

        private void resetAllValues() {
            mAnimationImeEnter.setValue("0");
            mAnimationImeExit.setValue("0");
            mAnimationImeInterpolator.setValue("0");
            mAnimationImeDuration.setValue(0);
        }

        private void resetAllSettings() {
            setProperVal(mAnimationImeEnter, 0);
            mAnimationImeEnter.setSummary(getProperSummary(mAnimationImeEnter));
            setProperVal(mAnimationImeExit, 0);
            mAnimationImeExit.setSummary(getProperSummary(mAnimationImeExit));
            setProperVal(mAnimationImeInterpolator, 0);
            mAnimationImeInterpolator.setSummary(getListInterpolatorName(0));
            setProperVal(mAnimationImeDuration, 0);
        }

        @Override
        public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
            if (preference == mAnimationImeReset) {
                resetToDefault();
            } else {
                return super.onPreferenceTreeClick(preferenceScreen, preference);
            }
            return false;
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object objValue) {
            ContentResolver resolver = getActivity().getContentResolver();
            if (preference == mAnimationImeDuration) {
                int val = ((Integer) objValue).intValue();
                Settings.System.putInt(resolver,
                        Settings.System.ANIMATION_IME_DURATION,
                        val);
            } else if (preference == mAnimationImeEnter) {
                int val = Integer.parseInt((String) objValue);
                Settings.System.putInt(resolver,
                        Settings.System.ANIMATION_IME_ENTER,
                        val);
                mAnimationImeEnter.setSummary(getProperSummary(mAnimationImeEnter));
            } else if (preference == mAnimationImeExit) {
                int val = Integer.parseInt((String) objValue);
                Settings.System.putInt(resolver,
                        Settings.System.ANIMATION_IME_EXIT,
                        val);
                mAnimationImeExit.setSummary(getProperSummary(mAnimationImeExit));
            } else if (preference == mAnimationImeInterpolator) {
                int val = Integer.parseInt((String) objValue);
                Settings.System.putInt(resolver,
                        Settings.System.ANIMATION_IME_INTERPOLATOR,
                        val);
                mAnimationImeInterpolator.setSummary(getListInterpolatorName(val));
            } else {
                return false;
            }
            return true;
        }

        private void setProperVal(Preference preference, int val) {
            String mString = "";
            if (preference == mAnimationImeEnter) {
                mString = Settings.System.ANIMATION_IME_ENTER;
            } else if (preference == mAnimationImeExit) {
                mString = Settings.System.ANIMATION_IME_EXIT;
            } else if (preference == mAnimationImeInterpolator) {
                mString = Settings.System.ANIMATION_IME_INTERPOLATOR;
            } else if (preference == mAnimationImeDuration) {
                mString = Settings.System.ANIMATION_IME_DURATION;
            }

            Settings.System.putInt(getActivity().getContentResolver(), mString, val);
        }

        private String getProperSummary(Preference preference) {
            String mString = "";
            if (preference == mAnimationImeEnter) {
                mString = Settings.System.ANIMATION_IME_ENTER;
            } else if (preference == mAnimationImeExit) {
                mString = Settings.System.ANIMATION_IME_EXIT;
            }

            String mNum = Settings.System.getString(getActivity().getContentResolver(), mString);
            return AwesomeAnimationHelper.getProperName(getActivity().getResources(), Integer.valueOf(mNum));
        }

        private String getProperVal(Preference preference) {
            String mString = "";
            if (preference == mAnimationImeEnter) {
                mString = Settings.System.ANIMATION_IME_ENTER;
            } else if (preference == mAnimationImeExit) {
                mString = Settings.System.ANIMATION_IME_EXIT;
            } else if (preference == mAnimationImeInterpolator) {
                mString = Settings.System.ANIMATION_IME_INTERPOLATOR;
            }

            return Settings.System.getString(getActivity().getContentResolver(), mString);
        }

        private String getListInterpolatorName(int index) {
            String[] str = getActivity().getResources().getStringArray(R.array.listview_interpolator_entries);
            return str[index];
        }
  
}
