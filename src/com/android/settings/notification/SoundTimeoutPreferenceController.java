/*
 * Copyright (C) 2019 The LineageOS Project
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
import android.provider.Settings;
import android.support.v7.preference.Preference;

import com.android.settings.RestrictedListPreference;
import com.android.settings.core.PreferenceControllerMixin;

public class SoundTimeoutPreferenceController extends NotificationPreferenceController
        implements PreferenceControllerMixin, Preference.OnPreferenceChangeListener {

    private static final String TAG = "SoundTimeoutPreferenceController";
    private static final String KEY_SOUND_TIMEOUT = "sound_timeout";

    public SoundTimeoutPreferenceController(Context context,
            NotificationBackend backend) {
        super(context, backend);
    }

    @Override
    public String getPreferenceKey() {
        return KEY_SOUND_TIMEOUT;
    }

    @Override
    public boolean isAvailable() {
        if (!super.isAvailable()) {
            return false;
        }
        if (mAppRow == null) {
            return false;
        }
        return true;
    }

    public void updateState(Preference preference) {
        if (mAppRow != null) {
            RestrictedListPreference pref = (RestrictedListPreference) preference;
            pref.setDisabledByAdmin(mAdmin);

            pref.setSummary("%s");
            pref.setValue(Long.toString(mAppRow.soundTimeout));
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (mAppRow != null) {
            mAppRow.soundTimeout = Long.valueOf((String) newValue);
            mBackend.setNotificationSoundTimeout(mAppRow.pkg, mAppRow.uid, mAppRow.soundTimeout);
        }
        return true;
    }

}
