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

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import com.android.internal.logging.MetricsLogger;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.cyanogenmod.BaseSystemSettingSwitchBar;
import com.android.settings.cyanogenmod.ShortcutPickHelper;
import org.cyanogenmod.internal.util.ScreenType;

import java.net.URISyntaxException;

public class VoiceWakeupSettings extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener, ShortcutPickHelper.OnPickListener,
        BaseSystemSettingSwitchBar.SwitchBarChangeCallback {
    private static final String TAG = "VoiceWakeupSettings";

    public static final int REQUEST_CALL_PERMS = 111;

    private static final String KEY_RETRAIN = "retrain";
    private static final String KEY_SHORTCUT_PICKER = "voice_wakeup_launch_intent";

    public static final String VOICE_WAKEUP_PACKAGE = "com.cyanogenmod.voicewakeup";
    private static final ComponentName VOICE_TRAINING_COMPONENT = new ComponentName(
            "com.cyanogenmod.voicewakeup", "com.cyanogenmod.voicewakeup.VoiceTrainingActivity");
    private static final ComponentName VOICE_TRAINING_SERVICE = new ComponentName(
            "com.cyanogenmod.voicewakeup", "com.cyanogenmod.voicewakeup.VoiceWakeupEngine");
    private static final String ACTION_REQUEST_DIAL_PERMISSION
            = "com.cyanogenmod.voicewakeup.ACTION_REQUEST_DIAL_PERMISSION";

    private BaseSystemSettingSwitchBar mVoiceWakeupEnabler;

    private ShortcutPickHelper mPicker;
    private String mDefaultActivityString;
    private String mLaunchIntentString;

    ViewGroup mContainer;

    private Preference mRetrainPreference;
    private Preference mPickShortcutPreference;

    private void log(String s) {
        Log.d(TAG, s);
    }

    private void retrain() {
        Intent retrain = new Intent(Intent.ACTION_MAIN);
        retrain.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        retrain.setComponent(VOICE_TRAINING_COMPONENT);
        startActivity(retrain);
    }

    private void restartService() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setComponent(VOICE_TRAINING_SERVICE);
        getActivity().startService(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.voice_wakeup_settings);

        mRetrainPreference = findPreference(KEY_RETRAIN);
        mPickShortcutPreference = findPreference(KEY_SHORTCUT_PICKER);
        mPicker = new ShortcutPickHelper(getActivity(), this);
        mDefaultActivityString = getResources().getString(R.string.voice_wakeup_default_activity);
    }

    @Override
    public void onStart() {
        super.onStart();
        final SettingsActivity activity = (SettingsActivity) getActivity();
        mVoiceWakeupEnabler = new BaseSystemSettingSwitchBar(activity, activity.getSwitchBar(),
                Settings.System.VOICE_WAKEUP, false, this);
    }

    @Override
    public void onDestroyView() {
        if (mVoiceWakeupEnabler != null) {
            mVoiceWakeupEnabler.teardownSwitchBar();
        }
        super.onDestroyView();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mContainer = container;
        return super.onCreateView(inflater, container, savedInstanceState);
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
            mVoiceWakeupEnabler.resume(getActivity());
        }

        // If running on a phone, remove padding around tabs
        if (!ScreenType.isTablet(getActivity())) {
            mContainer.setPadding(0, 0, 0, 0);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        if (preference == mRetrainPreference) {
            retrain();
            return true;
        } else if (preference == mPickShortcutPreference) {
            final Activity activity = getActivity();
            String[] names = new String[] {
                mDefaultActivityString
            };
            ShortcutIconResource[] icons = new ShortcutIconResource[] {
                ShortcutIconResource.fromContext(activity, R.drawable.ic_settings_voice_wakeup)
            };
            mPicker.pickShortcut(names, icons, getId());
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    };

    private boolean voiceWakeupHasCallPerms() {
        return getPackageManager().checkPermission(Manifest.permission.CALL_PHONE,
                VOICE_WAKEUP_PACKAGE) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CALL_PERMS) {
            if (resultCode != Activity.RESULT_OK || !voiceWakeupHasCallPerms()) {
                Toast.makeText(getActivity(), R.string.voice_wakeup_needs_dial_permission_warning,
                        Toast.LENGTH_SHORT).show();
                // reset to default
                shortcutPicked("", mDefaultActivityString, true);
            }
            return;
        }
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
        try {
            final Intent intent = Intent.parseUri(uri, 0);
            if (intent.getAction().equals(Intent.ACTION_CALL)) {
                Intent requestCallPerms = new Intent(ACTION_REQUEST_DIAL_PERMISSION);
                requestCallPerms.setPackage(VOICE_WAKEUP_PACKAGE);
                startActivityForResult(requestCallPerms, REQUEST_CALL_PERMS);
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        Settings.System.putString(getContentResolver(), Settings.System.VOICE_LAUNCH_INTENT, uri);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return false;
    }

    @Override
    public void onEnablerChanged(boolean isEnabled) {
        Activity activity = getActivity();
        mLaunchIntentString = Settings.System.getString(activity.getContentResolver(),
                Settings.System.VOICE_LAUNCH_INTENT);

        activity.invalidateOptionsMenu();

        mRetrainPreference.setEnabled(isEnabled);
        mPickShortcutPreference.setEnabled(isEnabled);

        if (mLaunchIntentString == null || mLaunchIntentString.isEmpty()) {
            mPickShortcutPreference.setSummary(mDefaultActivityString);
        } else {
            mPickShortcutPreference.setSummary(mPicker.getFriendlyNameForUri(mLaunchIntentString));
        }
        if (isEnabled) {
            restartService();
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsLogger.VOICE_INPUT;
    }
}
