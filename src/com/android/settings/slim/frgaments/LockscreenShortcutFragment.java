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

package com.android.settings.slim.fragments;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.internal.logging.MetricsLogger;

public class LockscreenShortcutFragment extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener {

    private static final String PREF_LOCKSCREEN_SHORTCUTS_LONGPRESS =
            "lockscreen_shortcuts_longpress";

    private SwitchPreference mLockscreenShortcutsLongpress;

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.APPLICATION;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.lockscreen_shortcut_fragment);

        PreferenceScreen prefSet = getPreferenceScreen();

        mLockscreenShortcutsLongpress = (SwitchPreference) findPreference(
                PREF_LOCKSCREEN_SHORTCUTS_LONGPRESS);
        mLockscreenShortcutsLongpress.setChecked(Settings.System.getInt(getContentResolver(),
                Settings.System.LOCKSCREEN_SHORTCUTS_LONGPRESS, 1) == 1);
        mLockscreenShortcutsLongpress.setOnPreferenceChangeListener(this);

        setHasOptionsMenu(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
            ViewGroup container, Bundle savedInstanceState) {
        final View view = super.onCreateView(inflater, container, savedInstanceState);
        final ListView list = (ListView) view.findViewById(android.R.id.list);
        // our container already takes care of the padding
        if (list != null) {
            int paddingTop = list.getPaddingTop();
            int paddingBottom = list.getPaddingBottom();
            list.setPadding(0, paddingTop, 0, paddingBottom);
        }
        return view;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mLockscreenShortcutsLongpress) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_SHORTCUTS_LONGPRESS,
                    (Boolean) newValue ? 1 : 0);
        }
        return true;
    }
}
