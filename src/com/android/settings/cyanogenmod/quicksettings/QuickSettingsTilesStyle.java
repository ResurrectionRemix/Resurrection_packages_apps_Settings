/*
 * Copyright (C) 2013 Slimroms
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

package com.android.settings.cyanogenmod.quicksettings;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.util.Helpers;

public class QuickSettingsTilesStyle extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener {

    private static final String PREF_TILES_PER_ROW = "tiles_per_row";
    private static final String PREF_TILES_PER_ROW_DUPLICATE_LANDSCAPE = "tiles_per_row_duplicate_landscape";

    private ListPreference mTilesPerRow;
    private CheckBoxPreference mDuplicateColumnsLandscape;

    private boolean mCheckPreferences;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        refreshSettings();
    }

    private PreferenceScreen refreshSettings() {
        mCheckPreferences = false;
        PreferenceScreen prefs = getPreferenceScreen();
        if (prefs != null) {
            prefs.removeAll();
        }

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.quicksettings_tiles_style);

        prefs = getPreferenceScreen();

        mTilesPerRow = (ListPreference) prefs.findPreference(PREF_TILES_PER_ROW);
        int tilesPerRow = Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.QUICK_TILES_PER_ROW, 3);
        mTilesPerRow.setValue(String.valueOf(tilesPerRow));
        mTilesPerRow.setSummary(mTilesPerRow.getEntry());
        mTilesPerRow.setOnPreferenceChangeListener(this);

        mDuplicateColumnsLandscape = (CheckBoxPreference) findPreference(PREF_TILES_PER_ROW_DUPLICATE_LANDSCAPE);
        mDuplicateColumnsLandscape.setChecked(Settings.System.getInt(
                getActivity().getContentResolver(),
                Settings.System.QUICK_TILES_PER_ROW_DUPLICATE_LANDSCAPE, 1) == 1);

        setHasOptionsMenu(true);
        mCheckPreferences = true;
        return prefs;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        if (preference == mDuplicateColumnsLandscape) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.QUICK_TILES_PER_ROW_DUPLICATE_LANDSCAPE,
                    ((CheckBoxPreference) preference).isChecked() ? 1 : 0);
            Helpers.restartSystemUI();
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (!mCheckPreferences) {
            return false;
        }
        if (preference == mTilesPerRow) {
            int index = mTilesPerRow.findIndexOfValue((String) newValue);
            int value = Integer.valueOf((String) newValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.QUICK_TILES_PER_ROW,
                    value);
            mTilesPerRow.setSummary(mTilesPerRow.getEntries()[index]);
            Helpers.restartSystemUI();
            return true;
        }
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

}
