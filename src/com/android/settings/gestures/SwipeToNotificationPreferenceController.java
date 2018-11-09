/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.gestures;

import static android.provider.Settings.Secure.SYSTEM_NAVIGATION_KEYS_ENABLED;
import static android.provider.Settings.Secure.FP_SWIPE_TO_DISMISS_NOTIFICATIONS;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;
import android.text.TextUtils;

import com.android.internal.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.widget.VideoPreference;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnCreate;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;
import com.android.settingslib.core.lifecycle.events.OnSaveInstanceState;

import com.android.settings.Utils;

public class SwipeToNotificationPreferenceController extends BasePreferenceController
        implements PreferenceControllerMixin, Preference.OnPreferenceChangeListener,
        LifecycleObserver, OnResume, OnPause, OnCreate, OnSaveInstanceState {

    private static final int ON = 1;
    private static final int OFF = 0;

    private VideoPreference mVideoPreference;
    boolean mVideoPaused;
    private static final String PREF_KEY_VIDEO = "gesture_swipe_down_fingerprint_video";
    static final String KEY_VIDEO_PAUSED = "key_video_paused";

    private static final String PREF_SWIPE_DOWN = "gesture_swipe_down_fingerprint";
    private static final String PREF_SWIPE_DISMISS = "gesture_swipe_dismiss_fingerprint";


    private PreferenceScreen mPreferenceScreen;

    public SwipeToNotificationPreferenceController(Context context, String key) {
        super(context, key);
    }

    public static boolean isSuggestionComplete(Context context, SharedPreferences prefs) {
        return !isGestureAvailable(context)
                || prefs.getBoolean(SwipeToNotificationSettings.PREF_KEY_SUGGESTION_COMPLETE,
                false);
    }

    private static boolean isGestureAvailable(Context context) {
        return Utils.hasFingerprintHardware(context)
                && context.getResources()
                .getBoolean(com.android.internal.R.bool.config_supportSystemNavigationKeys);
    }

    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        if (isAvailable()) {
            mVideoPreference = (VideoPreference) screen.findPreference(PREF_KEY_VIDEO);
        }
        mPreferenceScreen = screen;
    }

    @Override
    public int getAvailabilityStatus() {
        return isAvailable(mContext) ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    /*public boolean isSliceable() {
        return TextUtils.equals(getPreferenceKey(), "gesture_swipe_down_fingerprint");
    }*/

    public void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mVideoPaused = savedInstanceState.getBoolean(KEY_VIDEO_PAUSED, false);
        }
    }

     @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(KEY_VIDEO_PAUSED, mVideoPaused);
    }

    @Override
    public void onPause() {
        if (mVideoPreference != null) {
            mVideoPaused = mVideoPreference.isVideoPaused();
            mVideoPreference.onViewInvisible();
        }
    }

     @Override
    public void onResume() {
        if (mVideoPreference != null) {
            mVideoPreference.onViewVisible(mVideoPaused);
        }
    }

    public static boolean isAvailable(Context context) {
        return isGestureAvailable(context);
    }

    public void updateState(Preference preference) {
        super.updateState(preference);
        if (preference != null && preference instanceof SwitchPreference) {
            SwitchPreference pref = (SwitchPreference) preference;
            if (TextUtils.equals(pref.getKey(), PREF_SWIPE_DOWN)) {
                boolean enabled = Settings.Secure.getInt(
                        mContext.getContentResolver(),
                        SYSTEM_NAVIGATION_KEYS_ENABLED, OFF) == ON;
                pref.setChecked(enabled);
            } else if (TextUtils.equals(pref.getKey(), PREF_SWIPE_DISMISS)) {
                boolean enabled = Settings.Secure.getInt(
                        mContext.getContentResolver(),
                        FP_SWIPE_TO_DISMISS_NOTIFICATIONS, OFF) == ON;
                pref.setChecked(enabled);
                pref.setEnabled(swipeDownEnabled());
            }
        }
    }

     private boolean swipeDownEnabled() {
        return Settings.Secure.getInt(mContext.getContentResolver(),
                SYSTEM_NAVIGATION_KEYS_ENABLED, OFF) == ON;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        SwitchPreference pref = (SwitchPreference) preference;
        if (TextUtils.equals(pref.getKey(), PREF_SWIPE_DOWN)) {
            boolean enabled = ((Boolean) newValue).booleanValue();
            Settings.Secure.putInt(mContext.getContentResolver(),
                    SYSTEM_NAVIGATION_KEYS_ENABLED, enabled ? ON : OFF);
            pref.setChecked(enabled);
            SwitchPreference dismissPref = (SwitchPreference) mPreferenceScreen.findPreference(PREF_SWIPE_DISMISS);
            if (dismissPref != null) {
                dismissPref.setEnabled(enabled);
            }
        } else if (TextUtils.equals(pref.getKey(), PREF_SWIPE_DISMISS)) {
            boolean enabled = ((Boolean) newValue).booleanValue();
            Settings.Secure.putInt(mContext.getContentResolver(),
                    FP_SWIPE_TO_DISMISS_NOTIFICATIONS, enabled ? ON : OFF);
            pref.setChecked(enabled);
        }
          return true;
    }
}
