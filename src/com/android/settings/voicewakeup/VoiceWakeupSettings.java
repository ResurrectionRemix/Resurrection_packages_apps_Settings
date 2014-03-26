/*
 * Copyright (C) 2014 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.voicewakeup;

import android.app.ActionBar;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.cyanogenmod.ShortcutPickHelper;

public class VoiceWakeupSettings extends SettingsPreferenceFragment
        implements OnPreferenceChangeListener, ShortcutPickHelper.OnPickListener {
    private static final String TAG = "VoiceWakeupSettings";

    private static final String KEY_RETRAIN = "retrain";
    private static final String KEY_SHORTCUT_PICKER = "voice_wakeup_launch_intent";
    private static final String VOICE_TRAINING_PACKAGE = "com.cyanogenmod.voicewakeup";
    private static final String VOICE_TRAINING_ACTIVITY = "com.cyanogenmod.voicewakeup.VoiceTrainingActivity";

    private VoiceWakeupEnabler mVoiceWakeupEnabler;

    private Switch mActionBarSwitch;
    private boolean mEnabled;

    private ShortcutPickHelper mPicker;
    private String mDefaultActivityString;
    private String mLaunchIntentString;

    ViewGroup mContainer;

    private Preference mRetrainPreference;
    private Preference mPickShortcutPreference;

    private void log(String s) {
        Log.d(TAG, s);
    }

    private ContentObserver mSettingsObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            onChange(selfChange, null);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            refreshEnabledState();
        }
    };

    private void retrain() {
        Intent retrain = new Intent(Intent.ACTION_MAIN);
        retrain.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        retrain.setComponent(new ComponentName(VOICE_TRAINING_PACKAGE, VOICE_TRAINING_ACTIVITY));
        startActivity(retrain);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.voice_wakeup_settings);

        mRetrainPreference = findPreference(KEY_RETRAIN);
        mPickShortcutPreference = findPreference(KEY_SHORTCUT_PICKER);
        mPicker = new ShortcutPickHelper(getActivity(), this);
        mDefaultActivityString = getResources().getString(R.string.voice_wakeup_default_activity);

        getContentResolver().registerContentObserver(
                Settings.System.getUriFor(Settings.System.VOICE_WAKEUP), true, mSettingsObserver);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getContentResolver().unregisterContentObserver(mSettingsObserver);
    }

    private void refreshEnabledState() {
        Activity activity = getActivity();
        mEnabled = Settings.System.getInt(activity.getContentResolver(),
                Settings.System.VOICE_WAKEUP, 0) == 1;
        mLaunchIntentString = Settings.System.getString(activity.getContentResolver(),
                Settings.System.VOICE_LAUNCH_INTENT);

        activity.invalidateOptionsMenu();

        mRetrainPreference.setEnabled(mEnabled);
        mPickShortcutPreference.setEnabled(mEnabled);

        if (mLaunchIntentString == null || mLaunchIntentString.isEmpty()) {
            mPickShortcutPreference.setSummary(mDefaultActivityString);
        } else {
            mPickShortcutPreference.setSummary(mPicker.getFriendlyNameForUri(mLaunchIntentString));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContainer = container;
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        // We don't call super.onActivityCreated() here, since it assumes we
        // already set up
        // Preference (probably in onCreate()), while ProfilesSettings
        // exceptionally set it up in
        // this method.
        // On/off switch
        Activity activity = getActivity();
        // Switch
        mActionBarSwitch = new Switch(activity);

        if (activity instanceof PreferenceActivity) {
            PreferenceActivity preferenceActivity = (PreferenceActivity) activity;
            if (preferenceActivity.onIsHidingHeaders() || !preferenceActivity.onIsMultiPane()) {
                final int padding = activity.getResources().getDimensionPixelSize(
                        R.dimen.action_bar_switch_padding);
                mActionBarSwitch.setPaddingRelative(0, 0, padding, 0);
                activity.getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
                        ActionBar.DISPLAY_SHOW_CUSTOM);
                activity.getActionBar().setCustomView(mActionBarSwitch, new ActionBar.LayoutParams(
                        ActionBar.LayoutParams.WRAP_CONTENT,
                        ActionBar.LayoutParams.WRAP_CONTENT,
                        Gravity.CENTER_VERTICAL | Gravity.END));
            }
        }

        mVoiceWakeupEnabler = new VoiceWakeupEnabler(activity, mActionBarSwitch);

        // After confirming PreferenceScreen is available, we call super.
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mVoiceWakeupEnabler != null) {
            mVoiceWakeupEnabler.pause();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mVoiceWakeupEnabler != null) {
            mVoiceWakeupEnabler.resume();
        }

        refreshEnabledState();

        // If running on a phone, remove padding around tabs
        if (!Utils.isTablet(getActivity())) {
            mContainer.setPadding(0, 0, 0, 0);
        }
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mRetrainPreference) {
            retrain();
            return true;
        } else if (preference == mPickShortcutPreference) {
            String[] names = new String[] {
                mDefaultActivityString
            };
            ShortcutIconResource[] icons = new ShortcutIconResource[] {
                    ShortcutIconResource.fromContext(getActivity(), R.drawable.ic_voice_wakeup)
            };
            mPicker.pickShortcut(names, icons, getId());
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    };

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        String shortcutName = null;
        if (data != null) {
            shortcutName = data.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);
        }

        if (TextUtils.equals(shortcutName, mDefaultActivityString)) {
            shortcutPicked("", mDefaultActivityString, true);
        } else if (requestCode != Activity.RESULT_CANCELED
                && resultCode != Activity.RESULT_CANCELED) {
            mPicker.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void shortcutPicked(String uri, String friendlyName, boolean isApplication) {
        Settings.System.putString(getContentResolver(), Settings.System.VOICE_LAUNCH_INTENT, uri);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return false;
    }
}
