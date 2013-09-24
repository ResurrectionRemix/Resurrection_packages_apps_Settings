package com.android.settings.cyanogenmod.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.preference.CheckBoxPreference;
import android.provider.Settings;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.widgets.AnimBarPreference;
import com.android.settings.R;

import com.android.internal.util.cm.AwesomeAnimationHelper;

import java.util.Arrays;

public class AnimationControls extends SettingsPreferenceFragment implements OnPreferenceChangeListener {

    private static final String ACTIVITY_OPEN = "activity_open";
    private static final String ACTIVITY_CLOSE = "activity_close";
    private static final String TASK_OPEN = "task_open";
    private static final String TASK_CLOSE = "task_close";
    private static final String TASK_MOVE_TO_FRONT = "task_move_to_front";
    private static final String TASK_MOVE_TO_BACK = "task_move_to_back";
    private static final String ANIMATION_DURATION = "animation_duration";
    private static final String ANIMATION_NO_OVERRIDE = "animation_no_override";
    private static final String WALLPAPER_OPEN = "wallpaper_open";
    private static final String WALLPAPER_CLOSE = "wallpaper_close";
    private static final String WALLPAPER_INTRA_OPEN = "wallpaper_intra_open";
    private static final String WALLPAPER_INTRA_CLOSE = "wallpaper_intra_close";

    ListPreference mActivityOpenPref;
    ListPreference mActivityClosePref;
    ListPreference mTaskOpenPref;
    ListPreference mTaskClosePref;
    ListPreference mTaskMoveToFrontPref;
    ListPreference mTaskMoveToBackPref;
    ListPreference mWallpaperOpen;
    ListPreference mWallpaperClose;
    ListPreference mWallpaperIntraOpen;
    ListPreference mWallpaperIntraClose;
    AnimBarPreference mAnimationDuration;
    CheckBoxPreference mAnimNoOverride;

    private int[] mAnimations;
    private String[] mAnimationsStrings;
    private String[] mAnimationsNum;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.title_animation_controls);
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.prefs_animation_controls);

        PreferenceScreen prefs = getPreferenceScreen();
        mAnimations = AwesomeAnimationHelper.getAnimationsList();
        int animqty = mAnimations.length;
        mAnimationsStrings = new String[animqty];
        mAnimationsNum = new String[animqty];
        for (int i = 0; i < animqty; i++) {
            mAnimationsStrings[i] = AwesomeAnimationHelper.getProperName(mContext, mAnimations[i]);
            mAnimationsNum[i] = String.valueOf(mAnimations[i]);
        }

        mAnimNoOverride = (CheckBoxPreference) findPreference(ANIMATION_NO_OVERRIDE);
        mAnimNoOverride.setChecked(Settings.System.getBoolean(mContentRes,
                Settings.System.ANIMATION_CONTROLS_NO_OVERRIDE, false));

        mActivityOpenPref = (ListPreference) findPreference(ACTIVITY_OPEN);
        mActivityOpenPref.setOnPreferenceChangeListener(this);
        mActivityOpenPref.setSummary(getProperSummary(mActivityOpenPref));
        mActivityOpenPref.setEntries(mAnimationsStrings);
        mActivityOpenPref.setEntryValues(mAnimationsNum);

        mActivityClosePref = (ListPreference) findPreference(ACTIVITY_CLOSE);
        mActivityClosePref.setOnPreferenceChangeListener(this);
        mActivityClosePref.setSummary(getProperSummary(mActivityClosePref));
        mActivityClosePref.setEntries(mAnimationsStrings);
        mActivityClosePref.setEntryValues(mAnimationsNum);

        mTaskOpenPref = (ListPreference) findPreference(TASK_OPEN);
        mTaskOpenPref.setOnPreferenceChangeListener(this);
        mTaskOpenPref.setSummary(getProperSummary(mTaskOpenPref));
        mTaskOpenPref.setEntries(mAnimationsStrings);
        mTaskOpenPref.setEntryValues(mAnimationsNum);

        mTaskClosePref = (ListPreference) findPreference(TASK_CLOSE);
        mTaskClosePref.setOnPreferenceChangeListener(this);
        mTaskClosePref.setSummary(getProperSummary(mTaskClosePref));
        mTaskClosePref.setEntries(mAnimationsStrings);
        mTaskClosePref.setEntryValues(mAnimationsNum);

        mTaskMoveToFrontPref = (ListPreference) findPreference(TASK_MOVE_TO_FRONT);
        mTaskMoveToFrontPref.setOnPreferenceChangeListener(this);
        mTaskMoveToFrontPref.setSummary(getProperSummary(mTaskMoveToFrontPref));
        mTaskMoveToFrontPref.setEntries(mAnimationsStrings);
        mTaskMoveToFrontPref.setEntryValues(mAnimationsNum);

        mTaskMoveToBackPref = (ListPreference) findPreference(TASK_MOVE_TO_BACK);
        mTaskMoveToBackPref.setOnPreferenceChangeListener(this);
        mTaskMoveToBackPref.setSummary(getProperSummary(mTaskMoveToBackPref));
        mTaskMoveToBackPref.setEntries(mAnimationsStrings);
        mTaskMoveToBackPref.setEntryValues(mAnimationsNum);

        mWallpaperOpen = (ListPreference) findPreference(WALLPAPER_OPEN);
        mWallpaperOpen.setOnPreferenceChangeListener(this);
        mWallpaperOpen.setSummary(getProperSummary(mWallpaperOpen));
        mWallpaperOpen.setEntries(mAnimationsStrings);
        mWallpaperOpen.setEntryValues(mAnimationsNum);

        mWallpaperClose = (ListPreference) findPreference(WALLPAPER_CLOSE);
        mWallpaperClose.setOnPreferenceChangeListener(this);
        mWallpaperClose.setSummary(getProperSummary(mWallpaperClose));
        mWallpaperClose.setEntries(mAnimationsStrings);
        mWallpaperClose.setEntryValues(mAnimationsNum);

        mWallpaperIntraOpen = (ListPreference) findPreference(WALLPAPER_INTRA_OPEN);
        mWallpaperIntraOpen.setOnPreferenceChangeListener(this);
        mWallpaperIntraOpen.setSummary(getProperSummary(mWallpaperIntraOpen));
        mWallpaperIntraOpen.setEntries(mAnimationsStrings);
        mWallpaperIntraOpen.setEntryValues(mAnimationsNum);

        mWallpaperIntraClose = (ListPreference) findPreference(WALLPAPER_INTRA_CLOSE);
        mWallpaperIntraClose.setOnPreferenceChangeListener(this);
        mWallpaperIntraClose.setSummary(getProperSummary(mWallpaperIntraClose));
        mWallpaperIntraClose.setEntries(mAnimationsStrings);
        mWallpaperIntraClose.setEntryValues(mAnimationsNum);

        int defaultDuration = Settings.System.getInt(mContentRes,
                Settings.System.ANIMATION_CONTROLS_DURATION, 0);
        mAnimationDuration = (AnimBarPreference) findPreference(ANIMATION_DURATION);
        mAnimationDuration.setInitValue((int) (defaultDuration));
        mAnimationDuration.setOnPreferenceChangeListener(this);
    }
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
                                         Preference preference) {
       if (preference == mAnimNoOverride) {
            Settings.System.putBoolean(mContentRes,
                    Settings.System.ANIMATION_CONTROLS_NO_OVERRIDE,
                        mAnimNoOverride.isChecked());
            return true;
        }
        return false;
    }
    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean result = false;

        if (preference == mActivityOpenPref) {

            int val = Integer.parseInt((String) newValue);
            result = Settings.System.putInt(mContentRes,
                    Settings.System.ACTIVITY_ANIMATION_CONTROLS[0], val);

        } else if (preference == mActivityClosePref) {

            int val = Integer.parseInt((String) newValue);
            result = Settings.System.putInt(mContentRes,
                    Settings.System.ACTIVITY_ANIMATION_CONTROLS[1], val);

        } else if (preference == mTaskOpenPref) {

            int val = Integer.parseInt((String) newValue);
            result = Settings.System.putInt(mContentRes,
                    Settings.System.ACTIVITY_ANIMATION_CONTROLS[2], val);

        } else if (preference == mTaskClosePref) {

            int val = Integer.parseInt((String) newValue);
            result = Settings.System.putInt(mContentRes,
                    Settings.System.ACTIVITY_ANIMATION_CONTROLS[3], val);

        } else if (preference == mTaskMoveToFrontPref) {

            int val = Integer.parseInt((String) newValue);
            result = Settings.System.putInt(mContentRes,
                    Settings.System.ACTIVITY_ANIMATION_CONTROLS[4], val);

        } else if (preference == mTaskMoveToBackPref) {

            int val = Integer.parseInt((String) newValue);
            result = Settings.System.putInt(mContentRes,
                    Settings.System.ACTIVITY_ANIMATION_CONTROLS[5], val);
        } else if (preference == mWallpaperOpen) {

            int val = Integer.parseInt((String) newValue);
            result = Settings.System.putInt(mContentRes,
                    Settings.System.ACTIVITY_ANIMATION_CONTROLS[6], val);
        } else if (preference == mWallpaperClose) {

            int val = Integer.parseInt((String) newValue);
            result = Settings.System.putInt(mContentRes,
                    Settings.System.ACTIVITY_ANIMATION_CONTROLS[7], val);
        } else if (preference == mWallpaperIntraOpen) {

            int val = Integer.parseInt((String) newValue);
            result = Settings.System.putInt(mContentRes,
                    Settings.System.ACTIVITY_ANIMATION_CONTROLS[8], val);
        } else if (preference == mWallpaperIntraClose) {

            int val = Integer.parseInt((String) newValue);
            result = Settings.System.putInt(mContentRes,
                    Settings.System.ACTIVITY_ANIMATION_CONTROLS[9], val);
        } else if (preference == mAnimationDuration) {
            int val = Integer.parseInt((String) newValue);
            Settings.System.putInt(mContentRes,
                    Settings.System.ANIMATION_CONTROLS_DURATION,
                    val);
        }
        preference.setSummary(getProperSummary(preference));
        return result;
    }
    private String getProperSummary(Preference preference) {
        String mString = "";
        if (preference == mActivityOpenPref) {
            mString = Settings.System.ACTIVITY_ANIMATION_CONTROLS[0];
        } else if (preference == mActivityClosePref) {
            mString = Settings.System.ACTIVITY_ANIMATION_CONTROLS[1];
        } else if (preference == mTaskOpenPref) {
            mString = Settings.System.ACTIVITY_ANIMATION_CONTROLS[2];
        } else if (preference == mTaskClosePref) {
            mString = Settings.System.ACTIVITY_ANIMATION_CONTROLS[3];
        } else if (preference == mTaskMoveToFrontPref) {
            mString = Settings.System.ACTIVITY_ANIMATION_CONTROLS[4];
        } else if (preference == mTaskMoveToBackPref) {
            mString = Settings.System.ACTIVITY_ANIMATION_CONTROLS[5];
        } else if (preference == mWallpaperOpen) {
            mString = Settings.System.ACTIVITY_ANIMATION_CONTROLS[6];
        } else if (preference == mWallpaperClose) {
            mString = Settings.System.ACTIVITY_ANIMATION_CONTROLS[7];
        } else if (preference == mWallpaperIntraOpen) {
            mString = Settings.System.ACTIVITY_ANIMATION_CONTROLS[8];
        } else if (preference == mWallpaperIntraClose) {
            mString = Settings.System.ACTIVITY_ANIMATION_CONTROLS[9];
        }

        int mNum = Settings.System.getInt(mContentRes, mString, 0);
        return mAnimationsStrings[mNum];
    }
}
