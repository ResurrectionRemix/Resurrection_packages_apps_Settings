/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.settings.notification;

import static com.android.settings.notification.SettingPref.TYPE_GLOBAL;
import static com.android.settings.notification.SettingPref.TYPE_SYSTEM;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.SearchIndexableResource;
import android.provider.Settings.Global;
import android.provider.Settings.System;
import android.telephony.TelephonyManager;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.hardware.VibratorIntensity;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OtherSoundSettings extends SettingsPreferenceFragment implements Indexable {
    private static final String TAG = "OtherSoundSettings";

    private static final int DEFAULT_ON = 1;

    private static final int EMERGENCY_TONE_SILENT = 0;
    private static final int EMERGENCY_TONE_ALERT = 1;
    private static final int EMERGENCY_TONE_VIBRATE = 2;
    private static final int DEFAULT_EMERGENCY_TONE = EMERGENCY_TONE_SILENT;

    private static final int DOCK_AUDIO_MEDIA_DISABLED = 0;
    private static final int DOCK_AUDIO_MEDIA_ENABLED = 1;
    private static final int DEFAULT_DOCK_AUDIO_MEDIA = DOCK_AUDIO_MEDIA_DISABLED;

    private static final String KEY_DIAL_PAD_TONES = "dial_pad_tones";
    private static final String KEY_SCREEN_LOCKING_SOUNDS = "screen_locking_sounds";
    private static final String KEY_DOCKING_SOUNDS = "docking_sounds";
    private static final String KEY_VOLUME_ADJUST_SOUNDS = "volume_adjust_sounds";
    private static final String KEY_TOUCH_SOUNDS = "touch_sounds";
    private static final String KEY_VIBRATE_ON_TOUCH = "vibrate_on_touch";
    private static final String KEY_DOCK_AUDIO_MEDIA = "dock_audio_media";
    private static final String KEY_EMERGENCY_TONE = "emergency_tone";
    private static final String KEY_VIBRATION_INTENSITY = "vibration_intensity";

    private static final String KEY_POWER_NOTIFICATIONS = "power_notifications";
    private static final String KEY_POWER_NOTIFICATIONS_VIBRATE = "power_notifications_vibrate";
    private static final String KEY_POWER_NOTIFICATIONS_RINGTONE = "power_notifications_ringtone";

    // Request code for power notification ringtone picker
    private static final int REQUEST_CODE_POWER_NOTIFICATIONS_RINGTONE = 1;

    // Used for power notification uri string if set to silent
    private static final String POWER_NOTIFICATIONS_SILENT_URI = "silent";

    private SwitchPreference mPowerSounds;
    private SwitchPreference mPowerSoundsVibrate;
    private Preference mPowerSoundsRingtone;

    private static final SettingPref PREF_DIAL_PAD_TONES = new SettingPref(
            TYPE_SYSTEM, KEY_DIAL_PAD_TONES, System.DTMF_TONE_WHEN_DIALING, DEFAULT_ON) {
        @Override
        public boolean isApplicable(Context context) {
            return Utils.isVoiceCapable(context);
        }
    };

    private static final SettingPref PREF_SCREEN_LOCKING_SOUNDS = new SettingPref(
            TYPE_SYSTEM, KEY_SCREEN_LOCKING_SOUNDS, System.LOCKSCREEN_SOUNDS_ENABLED, DEFAULT_ON);

    private static final SettingPref PREF_DOCKING_SOUNDS = new SettingPref(
            TYPE_GLOBAL, KEY_DOCKING_SOUNDS, Global.DOCK_SOUNDS_ENABLED, DEFAULT_ON) {
        @Override
        public boolean isApplicable(Context context) {
            return hasDockSettings(context);
        }
    };

    private static final SettingPref PREF_VOLUME_ADJUST_SOUNDS = new SettingPref(
            TYPE_SYSTEM, KEY_VOLUME_ADJUST_SOUNDS, System.VOLUME_ADJUST_SOUNDS_ENABLED,
            DEFAULT_ON) {
        @Override
        public boolean isApplicable(Context context) {
            return Utils.hasVolumeRocker(context);
        }
    };

    private static final SettingPref PREF_TOUCH_SOUNDS = new SettingPref(
            TYPE_SYSTEM, KEY_TOUCH_SOUNDS, System.SOUND_EFFECTS_ENABLED, DEFAULT_ON) {
        @Override
        protected boolean setSetting(Context context, int value) {
            final AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (value != 0) {
                am.loadSoundEffects();
            } else {
                am.unloadSoundEffects();
            }
            return super.setSetting(context, value);
        }
    };

    private static final SettingPref PREF_VIBRATE_ON_TOUCH = new SettingPref(
            TYPE_SYSTEM, KEY_VIBRATE_ON_TOUCH, System.HAPTIC_FEEDBACK_ENABLED, DEFAULT_ON) {
        @Override
        public boolean isApplicable(Context context) {
            return hasHaptic(context);
        }
    };

    private static final SettingPref PREF_VIBRATION_INTENSITY = new SettingPref(
            TYPE_SYSTEM, KEY_VIBRATION_INTENSITY, System.HAPTIC_FEEDBACK_ENABLED, DEFAULT_ON) {
        @Override
        public boolean isApplicable(Context context) {
            return VibratorIntensity.isSupported();
        }
    };


    private static final SettingPref PREF_DOCK_AUDIO_MEDIA = new SettingPref(
            TYPE_GLOBAL, KEY_DOCK_AUDIO_MEDIA, Global.DOCK_AUDIO_MEDIA_ENABLED,
            DEFAULT_DOCK_AUDIO_MEDIA, DOCK_AUDIO_MEDIA_DISABLED, DOCK_AUDIO_MEDIA_ENABLED) {
        @Override
        public boolean isApplicable(Context context) {
            return hasDockSettings(context);
        }

        @Override
        protected String getCaption(Resources res, int value) {
            switch(value) {
                case DOCK_AUDIO_MEDIA_DISABLED:
                    return res.getString(R.string.dock_audio_media_disabled);
                case DOCK_AUDIO_MEDIA_ENABLED:
                    return res.getString(R.string.dock_audio_media_enabled);
                default:
                    throw new IllegalArgumentException();
            }
        }
    };

    private static final SettingPref PREF_EMERGENCY_TONE = new SettingPref(
            TYPE_GLOBAL, KEY_EMERGENCY_TONE, Global.EMERGENCY_TONE, DEFAULT_EMERGENCY_TONE,
            EMERGENCY_TONE_ALERT, EMERGENCY_TONE_VIBRATE, EMERGENCY_TONE_SILENT) {
        @Override
        public boolean isApplicable(Context context) {
            final int activePhoneType = TelephonyManager.getDefault().getCurrentPhoneType();
            return activePhoneType == TelephonyManager.PHONE_TYPE_CDMA;
        }

        @Override
        protected String getCaption(Resources res, int value) {
            switch(value) {
                case EMERGENCY_TONE_SILENT:
                    return res.getString(R.string.emergency_tone_silent);
                case EMERGENCY_TONE_ALERT:
                    return res.getString(R.string.emergency_tone_alert);
                case EMERGENCY_TONE_VIBRATE:
                    return res.getString(R.string.emergency_tone_vibrate);
                default:
                    throw new IllegalArgumentException();
            }
        }
    };

    private static final SettingPref[] PREFS = {
        PREF_DIAL_PAD_TONES,
        PREF_SCREEN_LOCKING_SOUNDS,
        PREF_DOCKING_SOUNDS,
        PREF_VOLUME_ADJUST_SOUNDS,
        PREF_TOUCH_SOUNDS,
        PREF_VIBRATE_ON_TOUCH,
        PREF_DOCK_AUDIO_MEDIA,
        PREF_EMERGENCY_TONE,
        PREF_VIBRATION_INTENSITY,
    };

    private final SettingsObserver mSettingsObserver = new SettingsObserver();

    private Context mContext;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.other_sound_settings);

        mContext = getActivity();

        // power state change notification sounds
        mPowerSounds = (SwitchPreference) findPreference(KEY_POWER_NOTIFICATIONS);
        mPowerSounds.setChecked(Global.getInt(getContentResolver(),
                Global.POWER_NOTIFICATIONS_ENABLED, 0) != 0);
        mPowerSoundsVibrate = (SwitchPreference) findPreference(KEY_POWER_NOTIFICATIONS_VIBRATE);
        mPowerSoundsVibrate.setChecked(Global.getInt(getContentResolver(),
                Global.POWER_NOTIFICATIONS_VIBRATE, 0) != 0);
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator == null || !vibrator.hasVibrator()) {
            removePreference(KEY_POWER_NOTIFICATIONS_VIBRATE);
        }

        mPowerSoundsRingtone = findPreference(KEY_POWER_NOTIFICATIONS_RINGTONE);
        String currentPowerRingtonePath =
                Global.getString(getContentResolver(), Global.POWER_NOTIFICATIONS_RINGTONE);

        // set to default notification if we don't yet have one
        if (currentPowerRingtonePath == null) {
                currentPowerRingtonePath = System.DEFAULT_NOTIFICATION_URI.toString();
                Global.putString(getContentResolver(),
                        Global.POWER_NOTIFICATIONS_RINGTONE, currentPowerRingtonePath);
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

        for (SettingPref pref : PREFS) {
            pref.init(this);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mSettingsObserver.register(true);
    }

    @Override
    public void onPause() {
        super.onPause();
        mSettingsObserver.register(false);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mPowerSounds) {
            Global.putInt(getContentResolver(),
                    Global.POWER_NOTIFICATIONS_ENABLED,
                    mPowerSounds.isChecked() ? 1 : 0);

        } else if (preference == mPowerSoundsVibrate) {
            Global.putInt(getContentResolver(),
                    Global.POWER_NOTIFICATIONS_VIBRATE,
                    mPowerSoundsVibrate.isChecked() ? 1 : 0);

        } else if (preference == mPowerSoundsRingtone) {
            launchNotificationSoundPicker(REQUEST_CODE_POWER_NOTIFICATIONS_RINGTONE,
                    Global.getString(getContentResolver(),
                            Global.POWER_NOTIFICATIONS_RINGTONE));
        } else {
            // If we didn't handle it, let preferences handle it.
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }

        return true;
    }

    private static boolean hasDockSettings(Context context) {
        return context.getResources().getBoolean(R.bool.has_dock_settings);
    }

    private static boolean hasHaptic(Context context) {
        final Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        return vibrator != null && vibrator.hasVibrator();
    }

    // === Callbacks ===

    private final class SettingsObserver extends ContentObserver {
        public SettingsObserver() {
            super(new Handler());
        }

        public void register(boolean register) {
            final ContentResolver cr = getContentResolver();
            if (register) {
                for (SettingPref pref : PREFS) {
                    cr.registerContentObserver(pref.getUri(), false, this);
                }
            } else {
                cr.unregisterContentObserver(this);
            }
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            for (SettingPref pref : PREFS) {
                if (pref.getUri().equals(uri)) {
                    pref.update(mContext);
                    return;
                }
            }
        }
    }

    // === Indexing ===

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {

        public List<SearchIndexableResource> getXmlResourcesToIndex(
                Context context, boolean enabled) {
            final SearchIndexableResource sir = new SearchIndexableResource(context);
            sir.xmlResId = R.xml.other_sound_settings;
            return Arrays.asList(sir);
        }

        public List<String> getNonIndexableKeys(Context context) {
            final ArrayList<String> rt = new ArrayList<String>();
            for (SettingPref pref : PREFS) {
                if (!pref.isApplicable(context)) {
                    rt.add(pref.getKey());
                }
            }
            return rt;
        }
    };

    private void launchNotificationSoundPicker(int code, String currentPowerRingtonePath) {
        final Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);

        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE,
                getString(R.string.power_notifications_ringtone_title));
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE,
                RingtoneManager.TYPE_NOTIFICATION);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
                System.DEFAULT_NOTIFICATION_URI);
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
        Global.putString(getContentResolver(),
                Global.POWER_NOTIFICATIONS_RINGTONE, toneUriPath);
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
}
