/*
 * Copyright (C) 2012 The CyanogenMod Project
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

package com.android.settings;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
<<<<<<< HEAD
=======
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
>>>>>>> 34a5662... Settings: QuietHours AutoSMSReply && QuietHours Bypass (2/2)
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.RingtonePreference;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.InputFilter;
import android.text.TextUtils;
import android.widget.EditText;

import com.android.settings.R;
<<<<<<< HEAD
import com.android.settings.service.CallService;
import com.android.settings.service.SmsService;
=======
import com.android.settings.service.SmsCallHelper;
>>>>>>> 34a5662... Settings: QuietHours AutoSMSReply && QuietHours Bypass (2/2)
import com.android.settings.SettingsPreferenceFragment;

public class QuietHours extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener  {

    private static final String TAG = "QuietHours";

    private static final String KEY_QUIET_HOURS_ENABLED = "quiet_hours_enabled";

    private static final String KEY_QUIET_HOURS_NOTIFICATIONS = "quiet_hours_notifications";
    
    private static final String KEY_QUIET_HOURS_RINGER = "quiet_hours_ringer";

    private static final String KEY_QUIET_HOURS_STILL = "quiet_hours_still";

    private static final String KEY_QUIET_HOURS_DIM = "quiet_hours_dim";

    private static final String KEY_LOOP_BYPASS_RINGTONE = "loop_bypass_ringtone";

    private static final String KEY_QUIET_HOURS_TIMERANGE = "quiet_hours_timerange";

    private static final String KEY_AUTO_SMS = "auto_sms";

    private static final String KEY_AUTO_SMS_CALL = "auto_sms_call";

    private static final String KEY_AUTO_SMS_MESSAGE = "auto_sms_message";

<<<<<<< HEAD
=======
    private static final String KEY_CALL_BYPASS = "call_bypass";

    private static final String KEY_SMS_BYPASS = "sms_bypass";

    private static final String KEY_REQUIRED_CALLS = "required_calls";

    private static final String KEY_SMS_BYPASS_CODE = "sms_bypass_code";

    private static final String KEY_BYPASS_RINGTONE = "bypass_ringtone";

>>>>>>> 34a5662... Settings: QuietHours AutoSMSReply && QuietHours Bypass (2/2)
    private CheckBoxPreference mQuietHoursEnabled;

    private CheckBoxPreference mQuietHoursNotifications;
    
    private CheckBoxPreference mQuietHoursRinger;

    private CheckBoxPreference mQuietHoursStill;

    private CheckBoxPreference mQuietHoursDim;

<<<<<<< HEAD
=======
    private CheckBoxPreference mRingtoneLoop;

>>>>>>> 34a5662... Settings: QuietHours AutoSMSReply && QuietHours Bypass (2/2)
    private ListPreference mAutoSms;

    private ListPreference mAutoSmsCall;

<<<<<<< HEAD
    private Preference mAutoSmsMessage;

=======
    private ListPreference mSmsBypass;

    private ListPreference mCallBypass;

    private ListPreference mCallBypassNumber;

    private Preference mSmsBypassCode;

    private Preference mAutoSmsMessage;

    private RingtonePreference mBypassRingtone;

>>>>>>> 34a5662... Settings: QuietHours AutoSMSReply && QuietHours Bypass (2/2)
    private TimeRangePreference mQuietHoursTimeRange;

    protected Handler mHandler;
    private SettingsObserver mSettingsObserver;
    private Context mContext;

    private int mSmsPref;
    private int mCallPref;
<<<<<<< HEAD

    private String mAutoText = null;
    private String mDefaultText = null;

    private SharedPreferences mPrefs;
=======
    private int mSmsBypassPref;
    private int mCallBypassPref;

    private SharedPreferences mPrefs;
    private OnSharedPreferenceChangeListener mPreferencesChangeListener;
>>>>>>> 34a5662... Settings: QuietHours AutoSMSReply && QuietHours Bypass (2/2)

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getPreferenceManager() != null) {
            addPreferencesFromResource(R.xml.quiet_hours_settings);

            mContext = getActivity().getApplicationContext();
            ContentResolver resolver = mContext.getContentResolver();
            PreferenceScreen prefSet = getPreferenceScreen();
            mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
<<<<<<< HEAD
            mDefaultText = getResources()
                    .getString(R.string.quiet_hours_auto_sms_null);
=======
>>>>>>> 34a5662... Settings: QuietHours AutoSMSReply && QuietHours Bypass (2/2)

            // Load the preferences
            mQuietHoursEnabled = (CheckBoxPreference) prefSet.findPreference(KEY_QUIET_HOURS_ENABLED);
            mQuietHoursTimeRange = (TimeRangePreference) prefSet.findPreference(KEY_QUIET_HOURS_TIMERANGE);
            mQuietHoursNotifications = (CheckBoxPreference) prefSet.findPreference(KEY_QUIET_HOURS_NOTIFICATIONS);
            mQuietHoursRinger = (CheckBoxPreference) prefSet.findPreference(KEY_QUIET_HOURS_RINGER);
            mQuietHoursStill = (CheckBoxPreference) prefSet.findPreference(KEY_QUIET_HOURS_STILL);
            mQuietHoursDim = (CheckBoxPreference) findPreference(KEY_QUIET_HOURS_DIM);
<<<<<<< HEAD
            mAutoSms = (ListPreference) findPreference(KEY_AUTO_SMS);
            mAutoSmsCall = (ListPreference) findPreference(KEY_AUTO_SMS_CALL);
            mAutoSmsMessage = (Preference) findPreference(KEY_AUTO_SMS_MESSAGE);
=======
            mRingtoneLoop = (CheckBoxPreference) findPreference(KEY_LOOP_BYPASS_RINGTONE);
            mAutoSms = (ListPreference) findPreference(KEY_AUTO_SMS);
            mAutoSmsCall = (ListPreference) findPreference(KEY_AUTO_SMS_CALL);
            mAutoSmsMessage = (Preference) findPreference(KEY_AUTO_SMS_MESSAGE);
            mSmsBypass = (ListPreference) findPreference(KEY_SMS_BYPASS);
            mCallBypass = (ListPreference) findPreference(KEY_CALL_BYPASS);
            mCallBypassNumber = (ListPreference) findPreference(KEY_REQUIRED_CALLS);
            mSmsBypassCode = (Preference) findPreference(KEY_SMS_BYPASS_CODE);
            mBypassRingtone = (RingtonePreference) findPreference(KEY_BYPASS_RINGTONE);
>>>>>>> 34a5662... Settings: QuietHours AutoSMSReply && QuietHours Bypass (2/2)

            mSettingsObserver = new SettingsObserver(new Handler());

            // Set the preference state and listeners where applicable
            mQuietHoursTimeRange.setTimeRange(Settings.System.getInt(resolver, Settings.System.QUIET_HOURS_START, 0),
                    Settings.System.getInt(resolver, Settings.System.QUIET_HOURS_END, 0));
            mQuietHoursTimeRange.setOnPreferenceChangeListener(this);
            mQuietHoursNotifications.setChecked(Settings.System.getInt(resolver, Settings.System.QUIET_HOURS_NOTIFICATIONS, 0) == 1);
            mQuietHoursRinger.setChecked(Settings.System.getInt(resolver, Settings.System.QUIET_HOURS_RINGER, 0) == 1);
            mQuietHoursStill.setChecked(Settings.System.getInt(resolver, Settings.System.QUIET_HOURS_STILL, 0) == 1);
<<<<<<< HEAD
            mAutoSms.setOnPreferenceChangeListener(this);
            mAutoSms.setValue((mPrefs.getString(KEY_AUTO_SMS, "0")));
            mAutoSmsCall.setOnPreferenceChangeListener(this);
            mAutoSmsCall.setValue((mPrefs.getString(KEY_AUTO_SMS_CALL, "0")));
            mSmsPref = Integer.parseInt(mPrefs.getString(KEY_AUTO_SMS, "0"));
            mCallPref = Integer.parseInt(mPrefs.getString(KEY_AUTO_SMS_CALL, "0"));
            shouldDisplayTextPref();
=======
            mAutoSms.setValue(mPrefs.getString(KEY_AUTO_SMS, "0"));
            mAutoSms.setOnPreferenceChangeListener(this);
            mAutoSmsCall.setValue(mPrefs.getString(KEY_AUTO_SMS_CALL, "0"));
            mAutoSmsCall.setOnPreferenceChangeListener(this);
            mSmsBypass.setValue(mPrefs.getString(KEY_SMS_BYPASS, "0"));
            mSmsBypass.setOnPreferenceChangeListener(this);
            mCallBypass.setValue(mPrefs.getString(KEY_CALL_BYPASS, "0"));
            mCallBypass.setOnPreferenceChangeListener(this);
            mCallBypassNumber.setValue(mPrefs.getString(KEY_REQUIRED_CALLS, "2"));
            mCallBypassNumber.setOnPreferenceChangeListener(this);
            mBypassRingtone.setOnPreferenceChangeListener(this);

            TelephonyManager telephonyManager =
                    (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
            if (telephonyManager.getPhoneType() == TelephonyManager.PHONE_TYPE_NONE) {
                prefSet.removePreference((PreferenceGroup) findPreference("sms_respond"));
                prefSet.removePreference((PreferenceGroup) findPreference("quiethours_bypass"));
            } else {
                int callBypassNumber = Integer.parseInt(mPrefs.getString(KEY_REQUIRED_CALLS, "2"));
                boolean loopRingtone = mPrefs.getBoolean(KEY_LOOP_BYPASS_RINGTONE, true);
                mSmsBypassPref = Integer.parseInt(mPrefs.getString(KEY_SMS_BYPASS, "0"));
                mSmsPref = Integer.parseInt(mPrefs.getString(KEY_AUTO_SMS, "0"));
                mCallPref = Integer.parseInt(mPrefs.getString(KEY_AUTO_SMS_CALL, "0"));
                mCallBypassPref = Integer.parseInt(mPrefs.getString(KEY_CALL_BYPASS, "0"));
                Uri alertSoundUri = SmsCallHelper.returnUserRingtone(mContext);
                Ringtone ringtoneAlarm = RingtoneManager.getRingtone(mContext, alertSoundUri);
                mBypassRingtone.setSummary(ringtoneAlarm.getTitle(mContext));
                mRingtoneLoop.setChecked(loopRingtone);
                mRingtoneLoop.setSummary(loopRingtone
                        ? R.string.quiet_hours_bypass_ringtone_loop_summary_on
                        : R.string.quiet_hours_bypass_ringtone_loop_summary_off);
                mSmsBypass.setSummary(mSmsBypass.getEntries()[mSmsBypassPref]);
                mCallBypass.setSummary(mCallBypass.getEntries()[mCallBypassPref]);
                mCallBypassNumber.setSummary(mCallBypassNumber.getEntries()[callBypassNumber-2]
                        + getResources().getString(R.string.quiet_hours_calls_required_summary));
                mAutoSms.setSummary(mAutoSms.getEntries()[mSmsPref]);
                mAutoSmsCall.setSummary(mAutoSmsCall.getEntries()[mCallPref]);
                mCallBypassNumber.setEnabled(mCallBypassPref != 0);
                mSmsBypassCode.setEnabled(mSmsBypassPref != 0);
                shouldDisplayRingerPrefs();
                shouldDisplayTextPref();
                setSmsBypassCodeSummary();
            }
>>>>>>> 34a5662... Settings: QuietHours AutoSMSReply && QuietHours Bypass (2/2)

            TelephonyManager telephonyManager =
                    (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
            if (telephonyManager.getPhoneType() == TelephonyManager.PHONE_TYPE_NONE) {
                prefSet.removePreference((PreferenceGroup) findPreference("sms_respond"));
            }
            // Remove the notification light setting if the device does not support it 
            if (mQuietHoursDim != null && getResources().getBoolean(com.android.internal.R.bool.config_intrusiveNotificationLed) == false) {
                getPreferenceScreen().removePreference(mQuietHoursDim);
            } else {
                mQuietHoursDim.setChecked(Settings.System.getInt(resolver, Settings.System.QUIET_HOURS_DIM, 0) == 1);
            }
<<<<<<< HEAD
            updateAutoText();
=======

            mPreferencesChangeListener = new OnSharedPreferenceChangeListener() {
                public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                    if (key.equals(KEY_AUTO_SMS_CALL)
                            || key.equals(KEY_AUTO_SMS)
                            || key.equals(KEY_CALL_BYPASS)
                            || key.equals(KEY_SMS_BYPASS)) {
                        SmsCallHelper.scheduleService(mContext);
                    }
                    if (key.equals(KEY_SMS_BYPASS_CODE)) {
                        setSmsBypassCodeSummary();
                    }
                }
            };
>>>>>>> 34a5662... Settings: QuietHours AutoSMSReply && QuietHours Bypass (2/2)
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mPrefs.registerOnSharedPreferenceChangeListener(mPreferencesChangeListener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mPrefs.unregisterOnSharedPreferenceChangeListener(mPreferencesChangeListener);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        ContentResolver resolver = getActivity().getApplicationContext().getContentResolver();

        if (preference == mQuietHoursEnabled) {
            Settings.System.putInt(resolver, Settings.System.QUIET_HOURS_ENABLED,
                    mQuietHoursEnabled.isChecked() ? 1 : 0);
            return true;
        } else if (preference == mQuietHoursNotifications) {
            Settings.System.putInt(resolver, Settings.System.QUIET_HOURS_NOTIFICATIONS,
                    mQuietHoursNotifications.isChecked() ? 1 : 0);
            return true;
        } else if (preference == mQuietHoursRinger) {
            Settings.System.putInt(resolver, Settings.System.QUIET_HOURS_RINGER,
                    mQuietHoursRinger.isChecked() ? 1 : 0);
            return true;
        } else if (preference == mQuietHoursStill) {
            Settings.System.putInt(resolver, Settings.System.QUIET_HOURS_STILL,
                    mQuietHoursStill.isChecked() ? 1 : 0);
            return true;
        } else if (preference == mQuietHoursDim) {
            Settings.System.putInt(resolver, Settings.System.QUIET_HOURS_DIM,
                    mQuietHoursDim.isChecked() ? 1 : 0);
            return true;
<<<<<<< HEAD
        } else if (preference == mAutoSmsMessage) {
=======
        } else if (preference == mRingtoneLoop) {
            mRingtoneLoop.setSummary(mRingtoneLoop.isChecked()
                    ? R.string.quiet_hours_bypass_ringtone_loop_summary_on
                    : R.string.quiet_hours_bypass_ringtone_loop_summary_off);
            return true;
        } else if (preference == mAutoSmsMessage) {
            final String defaultText = getResources().getString(R.string.quiet_hours_auto_sms_null);
            final String autoText = mPrefs.getString(KEY_AUTO_SMS_MESSAGE, defaultText);

>>>>>>> 34a5662... Settings: QuietHours AutoSMSReply && QuietHours Bypass (2/2)
            AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
            alert.setTitle(R.string.quiet_hours_auto_string_title);
            alert.setMessage(R.string.quiet_hours_auto_string_explain);

            final EditText input = new EditText(getActivity());
            InputFilter[] filter = new InputFilter[1];
            // No multi/split messages for ease of compatibility
            filter[0] = new InputFilter.LengthFilter(160);
<<<<<<< HEAD
            input.setText(mAutoText);
=======
            input.append(autoText);
            input.setFilters(filter);
            alert.setView(input);
            alert.setPositiveButton(getResources().getString(R.string.ok),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            String value = input.getText().toString();
                            if (TextUtils.isEmpty(value)) {
                                value = defaultText;
                            }
                            SharedPreferences.Editor editor = mPrefs.edit();
                            editor.putString(KEY_AUTO_SMS_MESSAGE, value).apply();
                        }
                    });
            alert.show();
            return true;
        } else if (preference == mSmsBypassCode) {
            final String defaultCode = getResources().getString(R.string.quiet_hours_sms_code_null);
            final String code = mPrefs.getString(KEY_SMS_BYPASS_CODE, defaultCode);
            AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
            alert.setTitle(R.string.quiet_hours_sms_code_title);
            alert.setMessage(R.string.quiet_hours_sms_code_explain);

            final EditText input = new EditText(getActivity());
            InputFilter[] filter = new InputFilter[1];
            filter[0] = new InputFilter.LengthFilter(20);
            input.append(code);
>>>>>>> 34a5662... Settings: QuietHours AutoSMSReply && QuietHours Bypass (2/2)
            input.setFilters(filter);
            alert.setView(input);
            alert.setPositiveButton(getResources().getString(R.string.ok),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            String value = input.getText().toString();
                            if (TextUtils.isEmpty(value)) {
<<<<<<< HEAD
                                value = mDefaultText;
                            }
                            SharedPreferences.Editor editor = mPrefs.edit();
                            editor.putString(KEY_AUTO_SMS_MESSAGE, value).commit();
                            updateAutoText();
=======
                                value = defaultCode;
                            }
                            SharedPreferences.Editor editor = mPrefs.edit();
                            editor.putString(KEY_SMS_BYPASS_CODE, value).apply();
>>>>>>> 34a5662... Settings: QuietHours AutoSMSReply && QuietHours Bypass (2/2)
                        }
                    });
            alert.show();
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        ContentResolver resolver = getActivity().getApplicationContext().getContentResolver();
        if (preference == mQuietHoursTimeRange) {
            Settings.System.putInt(resolver, Settings.System.QUIET_HOURS_START,
                    mQuietHoursTimeRange.getStartTime());
            Settings.System.putInt(resolver, Settings.System.QUIET_HOURS_END,
                    mQuietHoursTimeRange.getEndTime());
            SmsCallHelper.scheduleService(mContext);
            return true;
        } else if (preference == mAutoSms) {
            mSmsPref = Integer.parseInt((String) newValue);
            mAutoSms.setSummary(mAutoSms.getEntries()[mSmsPref]);
            shouldDisplayTextPref();
            return true;
        } else if (preference == mAutoSmsCall) {
            mCallPref = Integer.parseInt((String) newValue);
            mAutoSmsCall.setSummary(mAutoSmsCall.getEntries()[mCallPref]);
            shouldDisplayTextPref();
            return true;
        } else if (preference == mSmsBypass) {
            mSmsBypassPref = Integer.parseInt((String) newValue);
            mSmsBypass.setSummary(mSmsBypass.getEntries()[mSmsBypassPref]);
            mSmsBypassCode.setEnabled(mSmsBypassPref != 0);
            shouldDisplayRingerPrefs();
            return true;
        } else if (preference == mCallBypass) {
            mCallBypassPref = Integer.parseInt((String) newValue);
            mCallBypass.setSummary(mCallBypass.getEntries()[mCallBypassPref]);
            mCallBypassNumber.setEnabled(mCallBypassPref != 0);
            shouldDisplayRingerPrefs();
            return true;
        } else if (preference == mCallBypassNumber) {
            int val = Integer.parseInt((String) newValue);
            mCallBypassNumber.setSummary(mCallBypassNumber.getEntries()[val-2]
                    + getResources().getString(R.string.quiet_hours_calls_required_summary));
            return true;
        } else if (preference == mBypassRingtone) {
            Uri val = Uri.parse((String) newValue);
            SharedPreferences.Editor editor = mPrefs.edit();
            Ringtone ringtone = RingtoneManager.getRingtone(mContext, val);
            if (ringtone != null) {
                editor.putString(KEY_BYPASS_RINGTONE, val.toString()).apply();
                mBypassRingtone.setSummary(ringtone.getTitle(mContext));
            } else {
                // No silent option, won't reach here
                editor.putString(KEY_BYPASS_RINGTONE, null).apply();
            }
            return true;
        } else if (preference == mAutoSms) {
            mSmsPref = Integer.parseInt((String) newValue);
            if (mSmsPref != 0) {
                toggleSmsService();
            } else {
                mContext.stopService(new Intent(mContext, SmsService.class));
            }
            shouldDisplayTextPref();
            return true;
        } else if (preference == mAutoSmsCall) {
            mCallPref = Integer.parseInt((String) newValue);
            if (mCallPref != 0) {
                toggleCallService();
            } else {
                mContext.stopService(new Intent(mContext, CallService.class));
            }
            shouldDisplayTextPref();
            return true;
        }
        return false;
    }

<<<<<<< HEAD
    private void toggleSmsService() {
        if (SmsService.isStarted()) {
            mContext.stopService(new Intent(mContext, SmsService.class));
        }
        mContext.startService(new Intent(mContext, SmsService.class));
    }

    private void toggleCallService() {
        if (CallService.isStarted()) {
            mContext.stopService(new Intent(mContext, CallService.class));
        }
        mContext.startService(new Intent(mContext, CallService.class));
    }

    private void shouldDisplayTextPref() {
        if (mSmsPref != 0 || mCallPref != 0) {
            mAutoSmsMessage.setEnabled(true);
        } else {
            mAutoSmsMessage.setEnabled(false);
        }
    }

    private void updateAutoText() {
        mAutoText = mPrefs.getString(KEY_AUTO_SMS_MESSAGE, mDefaultText);
        if (TextUtils.isEmpty(mAutoText)) {
            mAutoText = mDefaultText;
        }

=======
    private void shouldDisplayTextPref() {
        mAutoSmsMessage.setEnabled(mSmsPref != 0 || mCallPref != 0);
    }

    private void shouldDisplayRingerPrefs() {
        mBypassRingtone.setEnabled(mSmsBypassPref != 0 || mCallBypassPref != 0);
        mRingtoneLoop.setEnabled(mSmsBypassPref != 0 || mCallBypassPref != 0);
    }

    private void setSmsBypassCodeSummary() {
        final String defaultCode = getResources().getString(R.string.quiet_hours_sms_code_null);
        final String code = mPrefs.getString(KEY_SMS_BYPASS_CODE, defaultCode);
        mSmsBypassCode.setSummary(code);
>>>>>>> 34a5662... Settings: QuietHours AutoSMSReply && QuietHours Bypass (2/2)
    }

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
            observe();
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.QUIET_HOURS_ENABLED), false,
                    this);
            updateSettings();
        }

        @Override
        public void onChange(boolean selfChange) {
            updateSettings();
        }
    }

    protected void updateSettings() {
        ContentResolver resolver = mContext.getContentResolver();
        mQuietHoursEnabled.setChecked(Settings.System.getInt(resolver,
                Settings.System.QUIET_HOURS_ENABLED, 0) == 1);
        SmsCallHelper.scheduleService(mContext);
    }
}
