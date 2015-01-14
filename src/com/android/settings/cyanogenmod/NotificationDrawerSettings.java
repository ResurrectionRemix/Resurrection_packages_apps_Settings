/*
 * Copyright (C) 2015 The CyanogenMod Project
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
package com.android.settings.cyanogenmod;

import android.os.Bundle;
import android.os.UserHandle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.SwitchPreference;
import android.provider.Settings;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class NotificationDrawerSettings extends SettingsPreferenceFragment
        implements OnPreferenceChangeListener {

    private static final String TAG = "NotificationDrawer";

    private static final String TOGGLE_MAIN_TILES = "qs_main_tiles";

    SwitchPreference mToggleMainTiles;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.notification_drawer_settings);

        mToggleMainTiles = (SwitchPreference) findPreference(TOGGLE_MAIN_TILES);
        mToggleMainTiles.setOnPreferenceChangeListener(this);

        boolean useMainTiles = Settings.Secure.getIntForUser(
                getActivity().getContentResolver(), Settings.Secure.QS_USE_MAIN_TILES,
                1, UserHandle.myUserId()) == 1;

        mToggleMainTiles.setChecked(useMainTiles);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mToggleMainTiles) {
            Settings.Secure.putIntForUser(
                    getActivity().getContentResolver(), Settings.Secure.QS_USE_MAIN_TILES,
                    ((Boolean) newValue) ? 1 : 0, UserHandle.myUserId());
            return true;
        }
        return false;
    }

}
