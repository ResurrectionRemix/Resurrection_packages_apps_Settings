/*
 * Copyright (C) 2007 The Android Open Source Project
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

import com.android.settings.bluetooth.DockEventReceiver;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.media.audiofx.AudioEffect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.MediaStore;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.VolumePanel;

import java.util.Date;
import java.util.Calendar;
import java.util.List;

public class SoundSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "SoundSettings";

    private static final int DIALOG_NOT_DOCKED = 1;

    /** If there is no setting in the provider, use this. */
    private static final int FALLBACK_EMERGENCY_TONE_VALUE = 0;

    private static final String KEY_VOLUME_OVERLAY = "volume_overlay";
    private static final String KEY_RING_MODE = "ring_mode";
    private static final String KEY_VOLUME_STEPS_ALARM = "volume_steps_alarm";
    private static final String KEY_VOLUME_STEPS_DTMF = "volume_steps_dtmf";
    private static final String KEY_VOLUME_STEPS_MUSIC = "volume_steps_music";
    private static final String KEY_VOLUME_STEPS_NOTIFICATION = "volume_steps_notification";
    private static final String KEY_VOLUME_STEPS_RING = "volume_steps_ring";
    private static final String KEY_VOLUME_STEPS_SYSTEM = "volume_steps_system";
    private static final String KEY_VOLUME_STEPS_VOICE_CALL = "volume_steps_voice_call";
    private static final String KEY_VIBRATE = "vibrate_when_ringing";
    private static final String KEY_RING_VOLUME = "ring_volume";
    private static final String KEY_INCREASING_RING = "increasing_ring";
    private static final String KEY_MUSICFX = "musicfx";
    private static final String KEY_DTMF_TONE = Settings.System.DTMF_TONE_WHEN_DIALING;
    private static final String KEY_SOUND_EFFECTS = Settings.System.SOUND_EFFECTS_ENABLED;
    private static final String KEY_HAPTIC_FEEDBACK = Settings.System.HAPTIC_FEEDBACK_ENABLED;
    private static final String KEY_EMERGENCY_TONE = "emergency_tone";
    private static final String KEY_SOUND_SETTINGS = "sound_settings";
    private static final String KEY_LOCK_SOUNDS = Settings.System.LOCKSCREEN_SOUNDS_ENABLED;
    private static final String KEY_RINGTONE = "ringtone";
    private static final String KEY_NOTIFICATION_SOUND = "notification_sound";
    private static final String KEY_CATEGORY_CALLS = "category_calls_and_notification";
    private static final String KEY_DOCK_CATEGORY = "dock_category";
    private static final String KEY_DOCK_AUDIO_SETTINGS = "dock_audio";
    private static final String KEY_DOCK_SOUNDS = "dock_sounds";
    private static final String KEY_DOCK_AUDIO_MEDIA_ENABLED = "dock_audio_media_enabled";
    private static final String KEY_QUIET_HOURS = "quiet_hours";
    private static final String KEY_CONVERT_SOUND_TO_VIBRATE =
            Settings.System.NOTIFICATION_CONVERT_SOUND_TO_VIBRATION;
    private static final String KEY_VIBRATE_DURING_CALLS =
            Settings.System.NOTIFICATION_VIBRATE_DURING_ALERTS_DISABLED;
    private static final String KEY_VOLUME_ADJUST_SOUNDS = "volume_adjust_sounds_enabled";
    private static final String KEY_POWER_NOTIFICATIONS = "power_notifications";
    private static final String KEY_POWER_NOTIFICATIONS_VIBRATE = "power_notifications_vibrate";
    private static final String KEY_POWER_NOTIFICATIONS_RINGTONE = "power_notifications_ringtone";
    
    private static final String KEY_CAMERA_SOUNDS = "camera_sounds";
    private static final String PROP_CAMERA_SOUND = "persist.sys.camera-sound";
    private static final String RING_MODE_NORMAL = "normal";
    private static final String RING_MODE_VIBRATE = "vibrate";
    private static final String RING_MODE_MUTE = "mute";

    private static final String[] NEED_VOICE_CAPABILITY = {
            KEY_RINGTONE, KEY_DTMF_TONE, KEY_CATEGORY_CALLS,
            KEY_EMERGENCY_TONE, KEY_INCREASING_RING, KEY_VIBRATE
    };

    private static final int MSG_UPDATE_RINGTONE_SUMMARY = 1;
    private static final int MSG_UPDATE_NOTIFICATION_SUMMARY = 2;

    // Request code for power notification ringtone picker
    private static final int REQUEST_CODE_POWER_NOTIFICATIONS_RINGTONE = 1;

    // Used for power notification uri string if set to silent
    private static final String POWER_NOTIFICATIONS_SILENT_URI = "silent";

    private ListPreference mVolumeOverlay;
    private ListPreference mRingMode;
    private ListPreference mVolumeStepsAlarm;
    private ListPreference mVolumeStepsDTMF;
    private ListPreference mVolumeStepsMusic;
    private ListPreference mVolumeStepsNotification;
    private ListPreference mVolumeStepsRing;
    private ListPreference mVolumeStepsSystem;
    private ListPreference mVolumeStepsVoiceCall;
    private CheckBoxPreference mSoundEffects;
    private Preference mMusicFx;
    private Preference mRingtonePreference;
    private Preference mNotificationPreference;
    private PreferenceScreen mQuietHours;
    private CheckBoxPreference mCameraSounds;
    private Runnable mRingtoneLookupRunnable;

    private AudioManager mAudioManager;

    private Preference mDockAudioSettings;
    private CheckBoxPreference mDockSounds;
    private Intent mDockIntent;
    private CheckBoxPreference mDockAudioMediaEnabled;

    private CheckBoxPreference mPowerSounds;
    private CheckBoxPreference mPowerSoundsVibrate;
    private Preference mPowerSoundsRingtone;

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MSG_UPDATE_RINGTONE_SUMMARY:
                mRingtonePreference.setSummary((CharSequence) msg.obj);
                break;
            case MSG_UPDATE_NOTIFICATION_SUMMARY:
                mNotificationPreference.setSummary((CharSequence) msg.obj);
                break;
            }
        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_DOCK_EVENT)) {
                handleDockChange(intent);
            } else if (intent.getAction().equals(AudioManager.RINGER_MODE_CHANGED_ACTION)) {
                updateState(false);
            }
        }
    };

    private PreferenceGroup mSoundSettings;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ContentResolver resolver = getContentResolver();
        int activePhoneType = TelephonyManager.getDefault().getCurrentPhoneType();

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        addPreferencesFromResource(R.xml.sound_settings);

        if (TelephonyManager.PHONE_TYPE_CDMA != activePhoneType) {
            // device is not CDMA, do not display CDMA emergency_tone
            getPreferenceScreen().removePreference(findPreference(KEY_EMERGENCY_TONE));
        }

        mVolumeOverlay = (ListPreference) findPreference(KEY_VOLUME_OVERLAY);
        mVolumeOverlay.setOnPreferenceChangeListener(this);
        int volumeOverlay = Settings.System.getInt(getContentResolver(),
                Settings.System.MODE_VOLUME_OVERLAY,
                VolumePanel.VOLUME_OVERLAY_EXPANDABLE);
        mVolumeOverlay.setValue(Integer.toString(volumeOverlay));
        mVolumeOverlay.setSummary(mVolumeOverlay.getEntry());

        mRingMode = (ListPreference) findPreference(KEY_RING_MODE);
        if (!getResources().getBoolean(R.bool.has_silent_mode)) {
            getPreferenceScreen().removePreference(mRingMode);
            findPreference(KEY_RING_VOLUME).setDependency(null);
        } else {
            mRingMode.setOnPreferenceChangeListener(this);
        }

        if (getResources().getBoolean(com.android.internal.R.bool.config_useFixedVolume)) {
            // device with fixed volume policy, do not display volumes submenu
            getPreferenceScreen().removePreference(findPreference(KEY_RING_VOLUME));
        }
        
		boolean isPhone = activePhoneType != TelephonyManager.PHONE_TYPE_NONE;
		PreferenceCategory audioCat = (PreferenceCategory) getPreferenceScreen().findPreference("category_volume");
		
		mVolumeStepsAlarm = (ListPreference) findPreference(KEY_VOLUME_STEPS_ALARM);
		updateVolumeSteps(mVolumeStepsAlarm.getKey(),mAudioManager.getStreamMaxVolume(mAudioManager.STREAM_ALARM));
		mVolumeStepsAlarm.setOnPreferenceChangeListener(this);
		
		
		
		mVolumeStepsDTMF = (ListPreference) findPreference(KEY_VOLUME_STEPS_DTMF);
		if(isPhone){
			updateVolumeSteps(mVolumeStepsDTMF.getKey(),mAudioManager.getStreamMaxVolume(mAudioManager.STREAM_DTMF));
			mVolumeStepsDTMF .setOnPreferenceChangeListener(this);
		}
		else
			audioCat.removePreference(mVolumeStepsDTMF);
		
		mVolumeStepsMusic = (ListPreference) findPreference(KEY_VOLUME_STEPS_MUSIC);
		updateVolumeSteps(mVolumeStepsMusic.getKey(),mAudioManager.getStreamMaxVolume(mAudioManager.STREAM_MUSIC));
		mVolumeStepsMusic .setOnPreferenceChangeListener(this);
		
		mVolumeStepsNotification = (ListPreference) findPreference(KEY_VOLUME_STEPS_NOTIFICATION);
		updateVolumeSteps(mVolumeStepsNotification.getKey(),mAudioManager.getStreamMaxVolume(mAudioManager.STREAM_NOTIFICATION));
		mVolumeStepsNotification .setOnPreferenceChangeListener(this);
		
		mVolumeStepsRing = (ListPreference) findPreference(KEY_VOLUME_STEPS_RING);
		if(isPhone){
			updateVolumeSteps(mVolumeStepsRing.getKey(),mAudioManager.getStreamMaxVolume(mAudioManager.STREAM_RING));
			mVolumeStepsRing .setOnPreferenceChangeListener(this);
		}
		else
			audioCat.removePreference(mVolumeStepsRing);
		
		mVolumeStepsSystem = (ListPreference) findPreference(KEY_VOLUME_STEPS_SYSTEM);
		updateVolumeSteps(mVolumeStepsSystem.getKey(),mAudioManager.getStreamMaxVolume(mAudioManager.STREAM_SYSTEM));
		mVolumeStepsSystem .setOnPreferenceChangeListener(this);
		
		mVolumeStepsVoiceCall = (ListPreference) findPreference(KEY_VOLUME_STEPS_VOICE_CALL);
		if(isPhone){
			updateVolumeSteps(mVolumeStepsVoiceCall.getKey(),mAudioManager.getStreamMaxVolume(mAudioManager.STREAM_VOICE_CALL));
			mVolumeStepsVoiceCall .setOnPreferenceChangeListener(this);
		}
		else
			audioCat.removePreference(mVolumeStepsVoiceCall);
		
		
		
        mQuietHours = (PreferenceScreen) findPreference(KEY_QUIET_HOURS);
        if (Settings.System.getInt(resolver, Settings.System.QUIET_HOURS_ENABLED, 0) == 1) {
            mQuietHours.setSummary(getString(R.string.quiet_hours_active_from) + " " +
                    returnTime(Settings.System.getString(resolver, Settings.System.QUIET_HOURS_START))
                    + " " + getString(R.string.quiet_hours_active_to) + " " +
                    returnTime(Settings.System.getString(resolver, Settings.System.QUIET_HOURS_END)));
        } else {
            mQuietHours.setSummary(getString(R.string.quiet_hours_summary));
        }

        mSoundEffects = (CheckBoxPreference) findPreference(KEY_SOUND_EFFECTS);

        if (!Utils.hasVolumeRocker(getActivity())) {
            getPreferenceScreen().removePreference(findPreference(KEY_VOLUME_ADJUST_SOUNDS));
        }

        mRingtonePreference = findPreference(KEY_RINGTONE);
        mNotificationPreference = findPreference(KEY_NOTIFICATION_SOUND);

        mCameraSounds = (CheckBoxPreference) findPreference(KEY_CAMERA_SOUNDS);
        mCameraSounds.setPersistent(false);
        mCameraSounds.setChecked(SystemProperties.getBoolean(
                PROP_CAMERA_SOUND, true));
                
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator == null || !vibrator.hasVibrator()) {
            removePreference(KEY_VIBRATE);
            removePreference(KEY_HAPTIC_FEEDBACK);
            removePreference(KEY_CONVERT_SOUND_TO_VIBRATE);
            removePreference(KEY_VIBRATE_DURING_CALLS);
        }

        if (TelephonyManager.PHONE_TYPE_CDMA == activePhoneType) {
            ListPreference emergencyTonePreference =
                (ListPreference) findPreference(KEY_EMERGENCY_TONE);
            emergencyTonePreference.setValue(String.valueOf(Settings.Global.getInt(
                resolver, Settings.Global.EMERGENCY_TONE, FALLBACK_EMERGENCY_TONE_VALUE)));
            emergencyTonePreference.setOnPreferenceChangeListener(this);
        }

        mSoundSettings = (PreferenceGroup) findPreference(KEY_SOUND_SETTINGS);

        mMusicFx = mSoundSettings.findPreference(KEY_MUSICFX);
        Intent i = new Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL);
        mMusicFx.setIntent(i);
        PackageManager p = getPackageManager();
        List<ResolveInfo> ris = p.queryIntentActivities(i, 0);
        if (ris.size() == 0) {
            mSoundSettings.removePreference(mMusicFx);
        } else if (ris.size() == 1) {
            mMusicFx.setSummary(ris.get(0).loadLabel(p));
        }

        if (!Utils.isVoiceCapable(getActivity())) {
            for (String prefKey : NEED_VOICE_CAPABILITY) {
                Preference pref = findPreference(prefKey);
                if (pref != null) {
                    getPreferenceScreen().removePreference(pref);
                }
            }
        }

        mRingtoneLookupRunnable = new Runnable() {
            public void run() {
                if (mRingtonePreference != null) {
                    updateRingtoneName(RingtoneManager.TYPE_RINGTONE, mRingtonePreference,
                            MSG_UPDATE_RINGTONE_SUMMARY);
                }
                if (mNotificationPreference != null) {
                    updateRingtoneName(RingtoneManager.TYPE_NOTIFICATION, mNotificationPreference,
                            MSG_UPDATE_NOTIFICATION_SUMMARY);
                }
            }
        };

        // power state change notification sounds
        mPowerSounds = (CheckBoxPreference) findPreference(KEY_POWER_NOTIFICATIONS);
        mPowerSounds.setChecked(Settings.Global.getInt(resolver,
                Settings.Global.POWER_NOTIFICATIONS_ENABLED, 0) != 0);
        mPowerSoundsVibrate = (CheckBoxPreference) findPreference(KEY_POWER_NOTIFICATIONS_VIBRATE);
        mPowerSoundsVibrate.setChecked(Settings.Global.getInt(resolver,
                Settings.Global.POWER_NOTIFICATIONS_VIBRATE, 0) != 0);
        if (vibrator == null || !vibrator.hasVibrator()) {
            removePreference(KEY_POWER_NOTIFICATIONS_VIBRATE);
        }

        mPowerSoundsRingtone = findPreference(KEY_POWER_NOTIFICATIONS_RINGTONE);
        String currentPowerRingtonePath =
                Settings.Global.getString(resolver, Settings.Global.POWER_NOTIFICATIONS_RINGTONE);

        // set to default notification if we don't yet have one
        if (currentPowerRingtonePath == null) {
                currentPowerRingtonePath = Settings.System.DEFAULT_NOTIFICATION_URI.toString();
                Settings.Global.putString(getContentResolver(),
                        Settings.Global.POWER_NOTIFICATIONS_RINGTONE, currentPowerRingtonePath);
        }
        // is it silent ?
        if (currentPowerRingtonePath.equals(POWER_NOTIFICATIONS_SILENT_URI)) {
            mPowerSoundsRingtone.setSummary(
                    getString(R.string.power_notifications_ringtone_silent));
        } else {
            final Ringtone ringtone =
                    RingtoneManager.getRingtone(getActivity(), Uri.parse(currentPowerRingtonePath));
            if (ringtone != null) {
                mPowerSoundsRingtone.setSummary(ringtone.getTitle(getActivity()));
            }
        }

        initDockSettings();
    }

    @Override
    public void onResume() {
        super.onResume();

        updateState(true);
        lookupRingtoneNames();

        IntentFilter filter = new IntentFilter(Intent.ACTION_DOCK_EVENT);
        getActivity().registerReceiver(mReceiver, filter);

        filter = new IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION);
        getActivity().registerReceiver(mReceiver, filter);

    }

    @Override
    public void onPause() {
        super.onPause();

        getActivity().unregisterReceiver(mReceiver);
    }

    private void setPhoneRingModeValue(String value) {
        int ringerMode = AudioManager.RINGER_MODE_NORMAL;
        if (value.equals(RING_MODE_MUTE)) {
            ringerMode = AudioManager.RINGER_MODE_SILENT;
        } else if (value.equals(RING_MODE_VIBRATE)) {
            ringerMode = AudioManager.RINGER_MODE_VIBRATE;
        }
        mAudioManager.setRingerMode(ringerMode);
    }

    private String getPhoneRingModeSettingValue() {
        switch (mAudioManager.getRingerMode()) {
        case AudioManager.RINGER_MODE_NORMAL:
            return RING_MODE_NORMAL;
        case AudioManager.RINGER_MODE_VIBRATE:
            return RING_MODE_VIBRATE;
        case AudioManager.RINGER_MODE_SILENT:
            return RING_MODE_MUTE;
        }
        // Shouldn't happen
        return RING_MODE_NORMAL;
    }

    // updateState in fact updates the UI to reflect the system state
    private void updateState(boolean force) {
        if (getActivity() == null) return;
        ContentResolver resolver = getContentResolver();

        mRingMode.setValue(getPhoneRingModeSettingValue());

        if (Settings.System.getInt(resolver, Settings.System.QUIET_HOURS_ENABLED, 0) == 1) {
            mQuietHours.setSummary(getString(R.string.quiet_hours_active_from) + " " +
                    returnTime(Settings.System.getString(resolver, Settings.System.QUIET_HOURS_START))
                    + " " + getString(R.string.quiet_hours_active_to) + " " +
                    returnTime(Settings.System.getString(resolver, Settings.System.QUIET_HOURS_END)));
        } else {
            mQuietHours.setSummary(getString(R.string.quiet_hours_summary));
        }

        mRingMode.setSummary(mRingMode.getEntry());
    }

    private void updateRingtoneName(int type, Preference preference, int msg) {
        if (preference == null) return;
        Context context = getActivity();
        if (context == null) return;
        Uri ringtoneUri = RingtoneManager.getActualDefaultRingtoneUri(context, type);
        CharSequence summary = context.getString(com.android.internal.R.string.ringtone_unknown);
        // Is it a silent ringtone?
        if (ringtoneUri == null) {
            summary = context.getString(com.android.internal.R.string.ringtone_silent);
        } else {
            // Fetch the ringtone title from the media provider
            try {
                Cursor cursor = context.getContentResolver().query(ringtoneUri,
                        new String[] { MediaStore.Audio.Media.TITLE }, null, null, null);
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        summary = cursor.getString(0);
                    }
                    cursor.close();
                }
            } catch (SQLiteException sqle) {
                // Unknown title for the ringtone
            }
        }
        mHandler.sendMessage(mHandler.obtainMessage(msg, summary));
    }
    
    private void updateVolumeSteps(int streamType, int steps)
    {	
        //Change the setting live
        mAudioManager.setStreamMaxVolume(streamType, steps);
        
	}
	private void updateVolumeSteps(String settingsKey, int steps){
		
		int streamType = -1;
		if (settingsKey.equals(KEY_VOLUME_STEPS_ALARM))
				streamType = mAudioManager.STREAM_ALARM;
				
		else if (settingsKey.equals(KEY_VOLUME_STEPS_DTMF)) 
				streamType = mAudioManager.STREAM_DTMF;
				
		else if (settingsKey.equals(KEY_VOLUME_STEPS_MUSIC))
				streamType = mAudioManager.STREAM_MUSIC;
				
		else if (settingsKey.equals(KEY_VOLUME_STEPS_NOTIFICATION))
				streamType = mAudioManager.STREAM_NOTIFICATION;
				
		else if (settingsKey.equals(KEY_VOLUME_STEPS_RING))
				streamType = mAudioManager.STREAM_RING;
				
		else if (settingsKey.equals(KEY_VOLUME_STEPS_SYSTEM))
				streamType = mAudioManager.STREAM_SYSTEM;
				
		else if (settingsKey.equals(KEY_VOLUME_STEPS_VOICE_CALL))
				streamType = mAudioManager.STREAM_VOICE_CALL;
		
		//Save the setting for next boot
		Settings.System.putInt(getContentResolver(),
                settingsKey, steps);
		
		((ListPreference)findPreference(settingsKey)).setSummary(String.valueOf(steps));
				
		updateVolumeSteps(streamType, steps);
		
		Log.i(TAG, "Volume steps:" + settingsKey + "" +String.valueOf(steps));	
			
	}

    private void lookupRingtoneNames() {
        new Thread(mRingtoneLookupRunnable).start();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mSoundEffects) {
            if (mSoundEffects.isChecked()) {
                mAudioManager.loadSoundEffects();
            } else {
                mAudioManager.unloadSoundEffects();
            }

        } else if (preference == mCameraSounds) {
            SystemProperties.set(PROP_CAMERA_SOUND, mCameraSounds.isChecked() ? "1" : "0");
            
        } else if (preference == mMusicFx) {
            // let the framework fire off the intent
            return false;
        } else if (preference == mDockAudioSettings) {
            int dockState = mDockIntent != null
                    ? mDockIntent.getIntExtra(Intent.EXTRA_DOCK_STATE, 0)
                    : Intent.EXTRA_DOCK_STATE_UNDOCKED;

            if (dockState == Intent.EXTRA_DOCK_STATE_UNDOCKED) {
                showDialog(DIALOG_NOT_DOCKED);
            } else {
                boolean isBluetooth = mDockIntent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE) != null;

                if (isBluetooth) {
                    Intent i = new Intent(mDockIntent);
                    i.setAction(DockEventReceiver.ACTION_DOCK_SHOW_UI);
                    i.setClass(getActivity(), DockEventReceiver.class);
                    getActivity().sendBroadcast(i);
                } else {
                    PreferenceScreen ps = (PreferenceScreen)mDockAudioSettings;
                    Bundle extras = ps.getExtras();
                    extras.putBoolean("checked",
                            Settings.Global.getInt(getContentResolver(),
                                    Settings.Global.DOCK_AUDIO_MEDIA_ENABLED, 0) == 1);
                    super.onPreferenceTreeClick(ps, ps);
                }
            }
        } else if (preference == mDockSounds) {
            Settings.Global.putInt(getContentResolver(), Settings.Global.DOCK_SOUNDS_ENABLED,
                    mDockSounds.isChecked() ? 1 : 0);
        } else if (preference == mDockAudioMediaEnabled) {
            Settings.Global.putInt(getContentResolver(), Settings.Global.DOCK_AUDIO_MEDIA_ENABLED,
                    mDockAudioMediaEnabled.isChecked() ? 1 : 0);
        } else if (preference == mPowerSounds) {
            Settings.Global.putInt(getContentResolver(),
                    Settings.Global.POWER_NOTIFICATIONS_ENABLED,
                    mPowerSounds.isChecked() ? 1 : 0);

        } else if (preference == mPowerSoundsVibrate) {
            Settings.Global.putInt(getContentResolver(),
                    Settings.Global.POWER_NOTIFICATIONS_VIBRATE,
                    mPowerSoundsVibrate.isChecked() ? 1 : 0);

        } else if (preference == mPowerSoundsRingtone) {
            launchNotificationSoundPicker(REQUEST_CODE_POWER_NOTIFICATIONS_RINGTONE,
                    Settings.Global.getString(getContentResolver(),
                            Settings.Global.POWER_NOTIFICATIONS_RINGTONE));
        } else {
            // If we didn't handle it, let preferences handle it.
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }

        return true;
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final String key = preference.getKey();
        if (KEY_EMERGENCY_TONE.equals(key)) {
            try {
                int value = Integer.parseInt((String) objValue);
                Settings.Global.putInt(getContentResolver(),
                        Settings.Global.EMERGENCY_TONE, value);
            } catch (NumberFormatException e) {
                Log.e(TAG, "could not persist emergency tone setting", e);
            }
        } else if (preference == mRingMode) {
            setPhoneRingModeValue(objValue.toString());
        } else if (preference == mVolumeStepsAlarm) {
            updateVolumeSteps(preference.getKey(),Integer.parseInt(objValue.toString()));
        } else if (preference == mVolumeStepsDTMF) {
            updateVolumeSteps(preference.getKey(),Integer.parseInt(objValue.toString()));
        } else if (preference == mVolumeStepsMusic) {
            updateVolumeSteps(preference.getKey(),Integer.parseInt(objValue.toString()));
        } else if (preference == mVolumeStepsNotification) {
            updateVolumeSteps(preference.getKey(),Integer.parseInt(objValue.toString()));
        } else if (preference == mVolumeStepsRing) {
            updateVolumeSteps(preference.getKey(),Integer.parseInt(objValue.toString()));
        } else if (preference == mVolumeStepsSystem) {
            updateVolumeSteps(preference.getKey(),Integer.parseInt(objValue.toString()));
        } else if (preference == mVolumeStepsVoiceCall) {
            updateVolumeSteps(preference.getKey(),Integer.parseInt(objValue.toString()));
        } else if (preference == mVolumeOverlay) {
            final int value = Integer.valueOf((String) objValue);
            final int index = mVolumeOverlay.findIndexOfValue((String) objValue);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.MODE_VOLUME_OVERLAY, value);
            mVolumeOverlay.setSummary(mVolumeOverlay.getEntries()[index]);
        }

        return true;
    }

    private String returnTime(String t) {
        if (t == null || t.equals("")) {
            return "";
        }
        int hr = Integer.parseInt(t.trim());
        int mn = hr;

        hr = hr / 60;
        mn = mn % 60;
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hr);
        cal.set(Calendar.MINUTE, mn);
        Date date = cal.getTime();
        return DateFormat.getTimeFormat(getActivity()).format(date);
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_url_sound;
    }

    private boolean needsDockSettings() {
        return getResources().getBoolean(R.bool.has_dock_settings);
    }

    private void initDockSettings() {
        ContentResolver resolver = getContentResolver();

        if (needsDockSettings()) {
            mDockSounds = (CheckBoxPreference) findPreference(KEY_DOCK_SOUNDS);
            mDockSounds.setPersistent(false);
            mDockSounds.setChecked(Settings.Global.getInt(resolver,
                    Settings.Global.DOCK_SOUNDS_ENABLED, 0) != 0);
            mDockAudioSettings = findPreference(KEY_DOCK_AUDIO_SETTINGS);
            mDockAudioSettings.setEnabled(false);
        } else {
            getPreferenceScreen().removePreference(findPreference(KEY_DOCK_CATEGORY));
            getPreferenceScreen().removePreference(findPreference(KEY_DOCK_AUDIO_SETTINGS));
            getPreferenceScreen().removePreference(findPreference(KEY_DOCK_SOUNDS));
            Settings.Global.putInt(resolver, Settings.Global.DOCK_AUDIO_MEDIA_ENABLED, 1);
        }
    }

    private void handleDockChange(Intent intent) {
        if (mDockAudioSettings != null) {
            int dockState = intent.getIntExtra(Intent.EXTRA_DOCK_STATE, 0);

            boolean isBluetooth =
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE) != null;

            mDockIntent = intent;

            if (dockState != Intent.EXTRA_DOCK_STATE_UNDOCKED) {
                // remove undocked dialog if currently showing.
                try {
                    removeDialog(DIALOG_NOT_DOCKED);
                } catch (IllegalArgumentException iae) {
                    // Maybe it was already dismissed
                }

                if (isBluetooth) {
                    mDockAudioSettings.setEnabled(true);
                } else {
                    if (dockState == Intent.EXTRA_DOCK_STATE_LE_DESK) {
                        ContentResolver resolver = getContentResolver();
                        mDockAudioSettings.setEnabled(true);
                        if (Settings.Global.getInt(resolver,
                                Settings.Global.DOCK_AUDIO_MEDIA_ENABLED, -1) == -1) {
                            Settings.Global.putInt(resolver,
                                    Settings.Global.DOCK_AUDIO_MEDIA_ENABLED, 0);
                        }
                        mDockAudioMediaEnabled =
                                (CheckBoxPreference) findPreference(KEY_DOCK_AUDIO_MEDIA_ENABLED);
                        mDockAudioMediaEnabled.setPersistent(false);
                        mDockAudioMediaEnabled.setChecked(
                                Settings.Global.getInt(resolver,
                                        Settings.Global.DOCK_AUDIO_MEDIA_ENABLED, 0) != 0);
                    } else {
                        mDockAudioSettings.setEnabled(false);
                    }
                }
            } else {
                mDockAudioSettings.setEnabled(false);
            }
        }
    }

    private void launchNotificationSoundPicker(int code, String currentPowerRingtonePath) {
        final Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);

        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE,
                getString(R.string.power_notifications_ringtone_title));
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE,
                RingtoneManager.TYPE_NOTIFICATION);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
                Settings.System.DEFAULT_NOTIFICATION_URI);
        if (currentPowerRingtonePath != null &&
                !currentPowerRingtonePath.equals(POWER_NOTIFICATIONS_SILENT_URI)) {
            Uri uri = Uri.parse(currentPowerRingtonePath);
            if (uri != null) {
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, uri);
            }
        }
        startActivityForResult(intent, code);
    }

    private void setPowerNotificationRingtone(Intent intent) {
        final Uri uri = intent.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);

        final String toneName;
        final String toneUriPath;

        if ( uri != null ) {
            final Ringtone ringtone = RingtoneManager.getRingtone(getActivity(), uri);
            toneName = ringtone.getTitle(getActivity());
            toneUriPath = uri.toString();
        } else {
            // silent
            toneName = getString(R.string.power_notifications_ringtone_silent);
            toneUriPath = POWER_NOTIFICATIONS_SILENT_URI;
        }

        mPowerSoundsRingtone.setSummary(toneName);
        Settings.Global.putString(getContentResolver(),
                Settings.Global.POWER_NOTIFICATIONS_RINGTONE, toneUriPath);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_POWER_NOTIFICATIONS_RINGTONE:
                if (resultCode == Activity.RESULT_OK) {
                    setPowerNotificationRingtone(data);
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    @Override
    public Dialog onCreateDialog(int id) {
        if (id == DIALOG_NOT_DOCKED) {
            return createUndockedMessage();
        }
        return null;
    }

    private Dialog createUndockedMessage() {
        final AlertDialog.Builder ab = new AlertDialog.Builder(getActivity());
        ab.setTitle(R.string.dock_not_found_title);
        ab.setMessage(R.string.dock_not_found_text);
        ab.setPositiveButton(android.R.string.ok, null);
        return ab.create();
    }
}
