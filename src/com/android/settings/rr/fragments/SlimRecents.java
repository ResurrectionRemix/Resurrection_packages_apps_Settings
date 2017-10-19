/*
 * Copyright (C) 2017 AICP
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


package com.android.settings.rr.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.support.v7.preference.ListPreference;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.rr.Preferences.CustomSeekBarPreference;
import com.android.settings.R;
import android.provider.Settings;
import android.view.Gravity;

import com.android.settings.rr.Preferences.MasterSwitchPreference;
import com.android.internal.logging.nano.MetricsProto;

public class SlimRecents extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    private static final String RECENT_PANEL_LEFTY_MODE = "recent_panel_lefty_mode";
    private static final String RECENT_PANEL_SHOW_TOPMOST = "recent_panel_show_topmost";

    private SwitchPreference mRecentPanelLeftyMode;
    private SwitchPreference mShowTopmost;
    private MasterSwitchPreference mAppSidebar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.slim_recents);

        mRecentPanelLeftyMode = (SwitchPreference) findPreference(RECENT_PANEL_LEFTY_MODE);
        mRecentPanelLeftyMode.setOnPreferenceChangeListener(this);
        mShowTopmost = (SwitchPreference) findPreference(RECENT_PANEL_SHOW_TOPMOST);
        mAppSidebar =
                (MasterSwitchPreference) findPreference(Settings.System.USE_RECENT_APP_SIDEBAR);
    }

    @Override
    public void onResume() {
        super.onResume();

        boolean recentLeftyMode = Settings.System.getInt(getContext().getContentResolver(),
                Settings.System.RECENT_PANEL_GRAVITY, Gravity.RIGHT) == Gravity.LEFT;
        mRecentPanelLeftyMode.setChecked(recentLeftyMode);

        mShowTopmost.setEnabled(!screenPinningEnabled());

        mAppSidebar.reloadValue();
    }

    @Override
    public void setMasterDependencyState(boolean enabled) {
        super.setMasterDependencyState(enabled);
        if (mShowTopmost != null) {
            mShowTopmost.setEnabled(enabled && !screenPinningEnabled());
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mRecentPanelLeftyMode) {
            Settings.System.putInt(getContext().getContentResolver(),
                    Settings.System.RECENT_PANEL_GRAVITY,
                    ((Boolean) newValue) ? Gravity.LEFT : Gravity.RIGHT);
            return true;
        } else {
            return false;
        }
    }

    private boolean screenPinningEnabled() {
        return Settings.System.getInt(getContext().getContentResolver(),
                Settings.System.LOCK_TO_APP_ENABLED, 0) != 0;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.RESURRECTED;
    }

}
