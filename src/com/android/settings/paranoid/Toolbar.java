/*
 * Copyright (C) 2012 ParanoidAndroid Project
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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceClickListener;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

public class Toolbar extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    private static final String KEY_AM_PM_STYLE = "am_pm_style";
    private static final String KEY_SHOW_CLOCK = "show_clock";
    private static final String KEY_STATUS_BAR_NOTIF_COUNT = "status_bar_notif_count";
    private static final String STATUS_BAR_MAX_NOTIF = "status_bar_max_notifications";
    private static final String NAV_BAR_TABUI_MENU = "nav_bar_tabui_menu";
    private static final String STATUS_BAR_DONOTDISTURB = "status_bar_donotdisturb";
    private static final String NAV_BAR_CATEGORY = "toolbar_navigation";
    private static final String NAV_BAR_CONTROLS = "navigation_bar_controls";
    private static final String PIE_GRAVITY = "pie_gravity";
    private static final String PIE_MODE = "pie_mode";
    private static final String PIE_SIZE = "pie_size";
    private static final String PIE_TRIGGER = "pie_trigger";
    private static final String PIE_GAP = "pie_gap";

    private CheckBoxPreference mShowClock;
    private ListPreference mAmPmStyle;
    private ListPreference mStatusBarMaxNotif;
    private ListPreference mPieMode;
    private ListPreference mPieSize;
    private ListPreference mPieGravity;
    private ListPreference mPieTrigger;
    private ListPreference mPieGap;
    private CheckBoxPreference mStatusBarNotifCount;
    private CheckBoxPreference mMenuButtonShow;
    private CheckBoxPreference mStatusBarDoNotDisturb;
    private PreferenceScreen mNavigationBarControls;
    private PreferenceCategory mNavigationCategory;

    private Context mContext;
    private int mAllowedLocations;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.tool_bar_settings);
        PreferenceScreen prefSet = getPreferenceScreen();
        mContext = getActivity();

        mShowClock = (CheckBoxPreference) prefSet.findPreference(KEY_SHOW_CLOCK);
        mShowClock.setChecked(Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.STATUS_BAR_SHOW_CLOCK, 1) == 1);

        mAmPmStyle = (ListPreference) prefSet.findPreference(KEY_AM_PM_STYLE);
        int amPmStyle = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.STATUS_BAR_AM_PM_STYLE, 2);
        mAmPmStyle.setValue(String.valueOf(amPmStyle));
        mAmPmStyle.setSummary(mAmPmStyle.getEntry());
        mAmPmStyle.setOnPreferenceChangeListener(this);

        mStatusBarMaxNotif = (ListPreference) prefSet.findPreference(STATUS_BAR_MAX_NOTIF);
        int maxNotIcons = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.MAX_NOTIFICATION_ICONS, 2);
        mStatusBarMaxNotif.setValue(String.valueOf(maxNotIcons));
        mStatusBarMaxNotif.setOnPreferenceChangeListener(this);

        mNavigationCategory = (PreferenceCategory) prefSet.findPreference(NAV_BAR_CATEGORY);

        mMenuButtonShow = (CheckBoxPreference) prefSet.findPreference(NAV_BAR_TABUI_MENU);
        mMenuButtonShow.setChecked((Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.NAV_BAR_TABUI_MENU, 0) == 1));

        mNavigationBarControls = (PreferenceScreen) prefSet.findPreference(NAV_BAR_CONTROLS);

        mPieGravity = (ListPreference) prefSet.findPreference(PIE_GRAVITY);
        int pieGravity = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.PIE_GRAVITY, 3);
        mPieGravity.setValue(String.valueOf(pieGravity));
        mPieGravity.setOnPreferenceChangeListener(this);

        mPieMode = (ListPreference) prefSet.findPreference(PIE_MODE);
        int pieMode = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.PIE_MODE, 2);
        mPieMode.setValue(String.valueOf(pieMode));
        mPieMode.setOnPreferenceChangeListener(this);

        mPieSize = (ListPreference) prefSet.findPreference(PIE_SIZE);
        String pieSize = Settings.System.getString(mContext.getContentResolver(),
                Settings.System.PIE_SIZE);
        mPieSize.setValue(pieSize != null && !pieSize.isEmpty() ? pieSize : "1");
        mPieSize.setOnPreferenceChangeListener(this);

        mPieTrigger = (ListPreference) prefSet.findPreference(PIE_TRIGGER);
        String pieTrigger = Settings.System.getString(mContext.getContentResolver(),
                Settings.System.PIE_TRIGGER);
        mPieTrigger.setValue(pieTrigger != null && !pieTrigger.isEmpty() ? pieTrigger : "1");
        mPieTrigger.setOnPreferenceChangeListener(this);

        mPieGap = (ListPreference) prefSet.findPreference(PIE_GAP);
        int pieGap = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.PIE_GAP, 1);
        mPieGap.setValue(String.valueOf(pieGap));
        mPieGap.setOnPreferenceChangeListener(this);

        try {
            if (Settings.System.getInt(getActivity().getContentResolver(),
                    Settings.System.TIME_12_24) != 12) {
                mAmPmStyle.setEnabled(false);
                mAmPmStyle.setSummary(R.string.status_bar_am_pm_info);
            }
        } catch (SettingNotFoundException e) {
            // This will hurt you, run away
        }

        mStatusBarNotifCount = (CheckBoxPreference) prefSet.findPreference(KEY_STATUS_BAR_NOTIF_COUNT);
        mStatusBarNotifCount.setChecked(Settings.System.getInt(getActivity().getContentResolver(), 
                Settings.System.STATUS_BAR_NOTIF_COUNT, 0) == 1);

        mStatusBarDoNotDisturb = (CheckBoxPreference) prefSet.findPreference(STATUS_BAR_DONOTDISTURB);
        mStatusBarDoNotDisturb.setChecked((Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.STATUS_BAR_DONOTDISTURB, 0) == 1));

        if (!Utils.isTablet()) {
            prefSet.removePreference(mStatusBarMaxNotif);
            prefSet.removePreference(mMenuButtonShow);
            prefSet.removePreference(mStatusBarDoNotDisturb);

            if(!Utils.hasNavigationBar()) {
                prefSet.removePreference(mNavigationCategory);
            }
        } else {
            mNavigationCategory.removePreference(mNavigationBarControls);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mShowClock) {
            Settings.System.putInt(mContext.getContentResolver(),
                    Settings.System.STATUS_BAR_SHOW_CLOCK, mShowClock.isChecked()
                    ? 1 : 0);
        } else if (preference == mStatusBarNotifCount) {	
            Settings.System.putInt(mContext.getContentResolver(),
                    Settings.System.STATUS_BAR_NOTIF_COUNT,	mStatusBarNotifCount.isChecked()
                    ? 1 : 0);	
        }else if (preference == mMenuButtonShow) {
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.NAV_BAR_TABUI_MENU, mMenuButtonShow.isChecked() ? 1 : 0);
            return true;
        } else if (preference == mStatusBarDoNotDisturb) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUS_BAR_DONOTDISTURB,
                    mStatusBarDoNotDisturb.isChecked() ? 1 : 0);
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mAmPmStyle) {
            int statusBarAmPmSize = Integer.valueOf((String) newValue);
            int index = mAmPmStyle.findIndexOfValue((String) newValue);
            Settings.System.putInt(mContext.getContentResolver(),
                    Settings.System.STATUS_BAR_AM_PM_STYLE, statusBarAmPmSize);
            mAmPmStyle.setSummary(mAmPmStyle.getEntries()[index]);
            return true;
        } else if (preference == mStatusBarMaxNotif) {
            int maxNotIcons = Integer.valueOf((String) newValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.MAX_NOTIFICATION_ICONS, maxNotIcons);
            return true;
        } else if (preference == mPieMode) {
            int pieMode = Integer.valueOf((String) newValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.PIE_MODE, pieMode);
            return true;
        } else if (preference == mPieSize) {
            float pieSize = Float.valueOf((String) newValue);
            Settings.System.putFloat(getActivity().getContentResolver(),
                    Settings.System.PIE_SIZE, pieSize);
            return true;
        } else if (preference == mPieGravity) {
            int pieGravity = Integer.valueOf((String) newValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.PIE_GRAVITY, pieGravity);
            return true;
        } else if (preference == mPieGap) {
            int pieGap = Integer.valueOf((String) newValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.PIE_GAP, pieGap);
            return true;
        } else if (preference == mPieTrigger) {
            float pierigger = Float.valueOf((String) newValue);
            Settings.System.putFloat(getActivity().getContentResolver(),
                    Settings.System.PIE_TRIGGER, pierigger);
            return true;
        }
        return false;
    }
}
