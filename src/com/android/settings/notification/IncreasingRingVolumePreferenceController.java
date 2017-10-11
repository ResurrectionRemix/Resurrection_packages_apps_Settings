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

package com.android.settings.notification;

import android.content.Context;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.Utils;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;
import com.android.settingslib.core.lifecycle.events.OnStop;

/**
 * Base class for preference controller that handles VolumeSeekBarPreference
 */
public class IncreasingRingVolumePreferenceController
        extends AdjustVolumeRestrictedPreferenceController
        implements LifecycleObserver, OnResume, OnStop {

    private static final String KEY_INCREASING_RING_VOLUME = "increasing_ring_volume";

    private IncreasingRingVolumePreference mPreference;
    private IncreasingRingVolumePreference.Callback mCallback;
    private AudioHelper mHelper;

    public IncreasingRingVolumePreferenceController(Context context,
            IncreasingRingVolumePreference.Callback callback, Lifecycle lifecycle) {
        super(context);
        mCallback = callback;
        mHelper = new AudioHelper(context);
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        if (isAvailable()) {
            final String key = getPreferenceKey();
            mPreference = (IncreasingRingVolumePreference) screen.findPreference(key);
            mPreference.setCallback(mCallback);
        }
    }

    @Override
    public void onResume() {
        if (mPreference != null) {
            mPreference.onActivityResume();
        }
    }

    @Override
    public void onStop() {
        if (mPreference != null) {
            mPreference.onActivityStop();
        }
    }

    @Override
    public String getPreferenceKey() {
        return KEY_INCREASING_RING_VOLUME;
    }

    @Override
    public boolean isAvailable() {
        return Utils.isVoiceCapable(mContext) && !mHelper.isSingleVolume();
    }
}
