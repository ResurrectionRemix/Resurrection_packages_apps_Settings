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

import static android.provider.Settings.System.VOLUME_BUTTON_MUSIC_CONTROL;

import android.content.Context;
import android.provider.Settings;

public class VolumeSkipTrackPreferenceController extends GesturePreferenceController {

    private final String SYSTEM_KEY = VOLUME_BUTTON_MUSIC_CONTROL;
    static final int ON = 1;
    static final int OFF = 0;

    public VolumeSkipTrackPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    protected String getVideoPrefKey() {
        return "volume_skiptrack_video"; //null for now
    }

    @Override
    public boolean isChecked() {
        final int enabled = Settings.System.getInt(mContext.getContentResolver(),
                SYSTEM_KEY, ON);
        return enabled == ON;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.System.putInt(mContext.getContentResolver(), SYSTEM_KEY,
                isChecked ? ON : OFF);
    }
}
