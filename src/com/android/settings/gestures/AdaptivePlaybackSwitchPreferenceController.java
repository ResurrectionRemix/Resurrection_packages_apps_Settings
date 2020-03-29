/*
 * Copyright (C) 2019 The Android Open Source Project
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

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.widget.Switch;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.widget.SwitchBar;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.widget.LayoutPreference;

public class AdaptivePlaybackSwitchPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, SwitchBar.OnSwitchChangeListener {

    private static final String KEY = "gesture_adaptive_playback_switch";
    private final Context mContext;
    private SettingObserver mSettingObserver;

    @VisibleForTesting
    SwitchBar mSwitch;

    public AdaptivePlaybackSwitchPreferenceController(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        if (isAvailable()) {
            LayoutPreference pref = screen.findPreference(getPreferenceKey());
            if (pref != null) {
                mSettingObserver = new SettingObserver(pref);
                pref.setOnPreferenceClickListener(preference -> {
                    int adaptivePlayback = Settings.System.getIntForUser(mContext.getContentResolver(),
                            Settings.System.ADAPTIVE_PLAYBACK_ENABLED, 0, UserHandle.USER_CURRENT);
                    boolean isChecked = adaptivePlayback != 0;
                    Settings.System.putIntForUser(mContext.getContentResolver(),
                            Settings.System.ADAPTIVE_PLAYBACK_ENABLED, isChecked
                                    ? 0 : 1, UserHandle.USER_CURRENT);
                    return true;
                });
                mSwitch = pref.findViewById(R.id.switch_bar);
                if (mSwitch != null) {
                    mSwitch.addOnSwitchChangeListener(this);
                    mSwitch.show();
                }
            }
        }
    }

    public void setChecked(boolean isChecked) {
        if (mSwitch != null) {
            mSwitch.setChecked(isChecked);
        }
    }

    @Override
    public void updateState(Preference preference) {
        int adaptivePlayback = Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.ADAPTIVE_PLAYBACK_ENABLED, 0, UserHandle.USER_CURRENT);
        setChecked(adaptivePlayback != 0);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        Settings.System.putIntForUser(mContext.getContentResolver(),
                Settings.System.ADAPTIVE_PLAYBACK_ENABLED, isChecked ? 1 : 0, UserHandle.USER_CURRENT);
    }

    private class SettingObserver extends ContentObserver {
        private final Uri ADAPTIVE_PLAYBACK = Settings.System.getUriFor(
                Settings.System.ADAPTIVE_PLAYBACK_ENABLED);

        private final Preference mPreference;

        public SettingObserver(Preference preference) {
            super(new Handler());
            mPreference = preference;
        }

        public void register(ContentResolver cr) {
            cr.registerContentObserver(ADAPTIVE_PLAYBACK, false, this, UserHandle.USER_ALL);
        }

        public void unregister(ContentResolver cr) {
            cr.unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            if (uri == null || ADAPTIVE_PLAYBACK.equals(uri)) {
                updateState(mPreference);
            }
        }
    }
}
