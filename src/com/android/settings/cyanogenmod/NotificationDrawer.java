/*
 * Copyright (C) 2012 The CyanogenMod project
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

package com.android.settings.cyanogenmod;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.RingtonePreference;
import android.provider.Settings;
import android.text.Spannable;
import android.widget.EditText;
import android.os.UserHandle;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.quicklaunch.BookmarkPicker;

import java.net.URISyntaxException;

import com.android.settings.omni.preference.AppSelectListPreference;

public class NotificationDrawer extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String CLOCK_SHORTCUT = "clock_shortcut";
    private static final String CALENDAR_SHORTCUT = "calendar_shortcut";
    private static final String TAG = "NotificationDrawer";

    private static final String UI_COLLAPSE_BEHAVIOUR = "notification_drawer_collapse_on_dismiss";
    private static final String PREF_NOTI_REMINDER_SOUND =  "noti_reminder_sound";
    private static final String PREF_NOTI_REMINDER_ENABLED = "noti_reminder_enabled";
    private static final String PREF_NOTI_REMINDER_RINGTONE = "noti_reminder_ringtone";
    
    private ListPreference mCollapseOnDismiss;
<<<<<<< HEAD
=======
    private CheckBoxPreference mStatusBarCustomHeader;
    private CheckBoxPreference mFullScreenDetection;
    private CheckBoxPreference mShowWifiName;
    private AppSelectListPreference mClockShortcut;
    private AppSelectListPreference mCalendarShortcut;
    private Preference mCustomLabel;
>>>>>>> f91c3ce... [1/2] OmniGears: notification drawer: custom clock & calendar shortcut
    CheckBoxPreference mReminder;
    ListPreference mReminderMode;
    RingtonePreference mReminderRingtone;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.notification_drawer);
        PreferenceScreen prefScreen = getPreferenceScreen();

<<<<<<< HEAD
=======
        // Notification drawer
        int collapseBehaviour = Settings.System.getInt(getContentResolver(),
                Settings.System.STATUS_BAR_COLLAPSE_ON_DISMISS,
                Settings.System.STATUS_BAR_COLLAPSE_IF_NO_CLEARABLE);
        mClockShortcut = (AppSelectListPreference) findPreference(CLOCK_SHORTCUT);
        mClockShortcut.setOnPreferenceChangeListener(this);

        mCalendarShortcut = (AppSelectListPreference) findPreference(CALENDAR_SHORTCUT);
        mCalendarShortcut.setOnPreferenceChangeListener(this);

        updateClockCalendarSummary();
        mCollapseOnDismiss = (ListPreference) findPreference(UI_COLLAPSE_BEHAVIOUR);
        mCollapseOnDismiss.setValue(String.valueOf(collapseBehaviour));
        mCollapseOnDismiss.setOnPreferenceChangeListener(this);
        updateCollapseBehaviourSummary(collapseBehaviour);

        mStatusBarCustomHeader = (CheckBoxPreference) findPreference(STATUS_BAR_CUSTOM_HEADER);
        mStatusBarCustomHeader.setChecked(Settings.System.getInt(getContentResolver(),
                Settings.System.STATUS_BAR_CUSTOM_HEADER, 0) == 1);
        mStatusBarCustomHeader.setOnPreferenceChangeListener(this);

        mFullScreenDetection = (CheckBoxPreference) findPreference(SWIPE_TO_SWITCH_SCREEN_DETECTION);
        mFullScreenDetection.setChecked(Settings.System.getInt(getContentResolver(),
                Settings.System.SWIPE_TO_SWITCH_SCREEN_DETECTION, 0) == 1);
        mFullScreenDetection.setOnPreferenceChangeListener(this);

        mShowWifiName = (CheckBoxPreference) findPreference(PREF_NOTIFICATION_SHOW_WIFI_SSID);
        mShowWifiName.setChecked(Settings.System.getInt(getContentResolver(),
                Settings.System.NOTIFICATION_SHOW_WIFI_SSID, 0) == 1);
        mShowWifiName.setOnPreferenceChangeListener(this);

        // Custom Carrier Label Text
        mCustomLabel = findPreference(PREF_CUSTOM_CARRIER_LABEL);
        updateCustomLabelTextSummary();

        mHideCarrier = (CheckBoxPreference) findPreference(PREF_NOTIFICATION_HIDE_CARRIER);
        boolean hideCarrier = Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.NOTIFICATION_HIDE_CARRIER, 0) == 1;
        mHideCarrier.setChecked(hideCarrier);
        mHideCarrier.setOnPreferenceChangeListener(this);

>>>>>>> f91c3ce... [1/2] OmniGears: notification drawer: custom clock & calendar shortcut
        // Notification Remider
        mReminder = (CheckBoxPreference) findPreference(PREF_NOTI_REMINDER_ENABLED);
        mReminder.setChecked(Settings.System.getIntForUser(getContentResolver(),
                Settings.System.REMINDER_ALERT_ENABLED, 0, UserHandle.USER_CURRENT) == 1);
        mReminder.setOnPreferenceChangeListener(this);

        mReminderMode = (ListPreference) findPreference(PREF_NOTI_REMINDER_SOUND);
        int mode = Settings.System.getIntForUser(getContentResolver(),
                Settings.System.REMINDER_ALERT_NOTIFY, 0, UserHandle.USER_CURRENT);
        mReminderMode.setValue(String.valueOf(mode));
        mReminderMode.setOnPreferenceChangeListener(this);
        updateReminderModeSummary(mode);

        mReminderRingtone =
                (RingtonePreference) findPreference(PREF_NOTI_REMINDER_RINGTONE);
        Uri ringtone = null;
        String ringtoneString = Settings.System.getStringForUser(getContentResolver(),
                Settings.System.REMINDER_ALERT_RINGER, UserHandle.USER_CURRENT);
        if (ringtoneString == null) {
            // Value not set, defaults to Default Ringtone
            ringtone = RingtoneManager.getDefaultUri(
                    RingtoneManager.TYPE_RINGTONE);
        } else {
            ringtone = Uri.parse(ringtoneString);
        }
        Ringtone alert = RingtoneManager.getRingtone(getActivity(), ringtone);
        mReminderRingtone.setSummary(alert.getTitle(getActivity()));
        mReminderRingtone.setOnPreferenceChangeListener(this);
        mReminderRingtone.setEnabled(mode != 0);
        
        // Notification drawer
        int collapseBehaviour = Settings.System.getInt(getContentResolver(),
                Settings.System.STATUS_BAR_COLLAPSE_ON_DISMISS,
                Settings.System.STATUS_BAR_COLLAPSE_IF_NO_CLEARABLE);
        mCollapseOnDismiss = (ListPreference) findPreference(UI_COLLAPSE_BEHAVIOUR);
        mCollapseOnDismiss.setValue(String.valueOf(collapseBehaviour));
        mCollapseOnDismiss.setOnPreferenceChangeListener(this);
        updateCollapseBehaviourSummary(collapseBehaviour);
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference == mClockShortcut) {
            String value = (String) objValue;
            // a value of null means to use the default
            Settings.System.putString(getContentResolver(), Settings.System.CLOCK_SHORTCUT, value);
            updateClockCalendarSummary();
        } else if (preference == mCalendarShortcut) {
            String value = (String) objValue;
            // a value of null means to use the default
            Settings.System.putString(getContentResolver(), Settings.System.CALENDAR_SHORTCUT, value);
            updateClockCalendarSummary();
        } else if (preference == mCollapseOnDismiss) {
            int value = Integer.valueOf((String) objValue);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.STATUS_BAR_COLLAPSE_ON_DISMISS, value);
            updateCollapseBehaviourSummary(value);
            return true;
        } else if (preference == mReminder) {
          Settings.System.putIntForUser(getContentResolver(),
                    Settings.System.REMINDER_ALERT_ENABLED,
                    (Boolean) objValue ? 1 : 0, UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mReminderMode) {
            int mode = Integer.valueOf((String) objValue);
            Settings.System.putIntForUser(getContentResolver(),
                    Settings.System.REMINDER_ALERT_NOTIFY,
                    mode, UserHandle.USER_CURRENT);
            updateReminderModeSummary(mode);
            mReminderRingtone.setEnabled(mode != 0);
            return true;
        } else if (preference == mReminderRingtone) {
            Uri val = Uri.parse((String) objValue);
            Ringtone ringtone = RingtoneManager.getRingtone(getActivity(), val);
            mReminderRingtone.setSummary(ringtone.getTitle(getActivity()));
            Settings.System.putStringForUser(getContentResolver(),
                    Settings.System.REMINDER_ALERT_RINGER,
                    val.toString(), UserHandle.USER_CURRENT);
            return true;
        }

        return false;
    }

    private void updateCollapseBehaviourSummary(int setting) {
        String[] summaries = getResources().getStringArray(
                R.array.notification_drawer_collapse_on_dismiss_summaries);
        mCollapseOnDismiss.setSummary(summaries[setting]);
    }

    private void updateReminderModeSummary(int value) {
        int resId;
        switch (value) {
            case 1:
                resId = R.string.enabled;
                break;
            case 2:
                resId = R.string.noti_reminder_sound_looping;
                break;
            default:
                resId = R.string.disabled;
                break;
        }
        mReminderMode.setSummary(getResources().getString(resId));
    }

    private void updateClockCalendarSummary() {
        final PackageManager packageManager = getPackageManager();

        mClockShortcut.setSummary(getResources().getString(R.string.default_shortcut));
        mCalendarShortcut.setSummary(getResources().getString(R.string.default_shortcut));

        String clockShortcutIntentUri = Settings.System.getString(getContentResolver(), Settings.System.CLOCK_SHORTCUT);
        if (clockShortcutIntentUri != null) {
            Intent clockShortcutIntent = null;
            try {
                clockShortcutIntent = Intent.parseUri(clockShortcutIntentUri, 0);
            } catch (URISyntaxException e) {
                clockShortcutIntent = null;
            }

            if(clockShortcutIntent != null) {
                ResolveInfo info = packageManager.resolveActivity(clockShortcutIntent, 0);
                if (info != null) {
                    mClockShortcut.setSummary(info.loadLabel(packageManager));
                }
            }
        }

        String calendarShortcutIntentUri = Settings.System.getString(getContentResolver(), Settings.System.CALENDAR_SHORTCUT);
        if (calendarShortcutIntentUri != null) {
            Intent calendarShortcutIntent = null;
            try {
                calendarShortcutIntent = Intent.parseUri(calendarShortcutIntentUri, 0);
            } catch (URISyntaxException e) {
                calendarShortcutIntent = null;
            }

            if(calendarShortcutIntent != null) {
                ResolveInfo info = packageManager.resolveActivity(calendarShortcutIntent, 0);
                if (info != null) {
                    mCalendarShortcut.setSummary(info.loadLabel(packageManager));
                }
            }
        }
    }
}
