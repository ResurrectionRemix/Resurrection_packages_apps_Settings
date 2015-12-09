/*
* Copyright (C) 2015 ResurrectionRemix Rom
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
package com.android.settings.rr;


import android.app.ActivityManager;
import android.content.Context;
import android.content.ContentResolver;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.view.Gravity;

import com.android.settings.rr.SeekBarPreference;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.internal.logging.MetricsLogger;

public class Slimrecents extends SettingsPreferenceFragment
            implements OnPreferenceChangeListener  {
    
    private static final String USE_SLIM_RECENTS = "use_slim_recents";
    private static final String RECENTS_MAX_APPS = "recents_max_apps";
    private static final String RECENT_PANEL_SCALE = "recent_panel_scale_factor";
    private static final String RECENT_PANEL_EXPANDED_MODE = "recent_panel_expanded_mode";
    private static final String RECENT_PANEL_LEFTY_MODE = "recent_panel_lefty_mode";

    private SwitchPreference mUseSlimRecents;
    private SeekBarPreference mMaxApps;
    private SeekBarPreference mRecentPanelScale;
    private ListPreference mRecentPanelExpandedMode;
    private SwitchPreference mRecentPanelLeftyMode;

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.DEVELOPMENT;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.slim_recents);
        ContentResolver resolver = getActivity().getContentResolver();
        PreferenceScreen prefSet = getPreferenceScreen();

        mUseSlimRecents = (SwitchPreference) prefSet.findPreference(USE_SLIM_RECENTS);
        mUseSlimRecents.setChecked(Settings.System.getInt(resolver,
                Settings.System.USE_SLIM_RECENTS, 0) == 1);
        mUseSlimRecents.setOnPreferenceChangeListener(this);

        mMaxApps = (SeekBarPreference) findPreference(RECENTS_MAX_APPS);
        int maxApps = Settings.System.getInt(resolver,
                Settings.System.RECENTS_MAX_APPS, ActivityManager.getMaxRecentTasksStatic());
        mMaxApps.setValue(maxApps);
        mMaxApps.setOnPreferenceChangeListener(this);

        mRecentPanelScale = (SeekBarPreference) findPreference(RECENT_PANEL_SCALE);
        int recentPanelScale = Settings.System.getInt(resolver,
                Settings.System.RECENT_PANEL_SCALE_FACTOR, 100);
        mRecentPanelScale.setValue(recentPanelScale);
        mRecentPanelScale.setOnPreferenceChangeListener(this);

        mRecentPanelExpandedMode = (ListPreference) prefSet.findPreference(RECENT_PANEL_EXPANDED_MODE);
        int recentPanelExpandedMode = Settings.System.getIntForUser(resolver,
                Settings.System.RECENT_PANEL_EXPANDED_MODE, 0, UserHandle.USER_CURRENT);
        mRecentPanelExpandedMode.setValue(String.valueOf(recentPanelExpandedMode));
        mRecentPanelExpandedMode.setSummary(mRecentPanelExpandedMode.getEntry());
        mRecentPanelExpandedMode.setOnPreferenceChangeListener(this);

        mRecentPanelLeftyMode = (SwitchPreference) prefSet.findPreference(RECENT_PANEL_LEFTY_MODE);
        mRecentPanelLeftyMode.setChecked(Settings.System.getInt(resolver,
                Settings.System.RECENT_PANEL_GRAVITY, Gravity.RIGHT) == Gravity.LEFT);
        mRecentPanelLeftyMode.setOnPreferenceChangeListener(this);

        updatePreference();
}

      @Override
     public void onResume() {
         super.onResume();
        updatePreference();
    }

	private void updatePreference() {
        boolean slimRecent = Settings.System.getInt(getActivity().getContentResolver(),
                    Settings.System.USE_SLIM_RECENTS, 0) == 1;
    }
	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
	ContentResolver resolver = getActivity().getContentResolver();
	if (preference == mUseSlimRecents) {
            Settings.System.putInt(getContentResolver(), Settings.System.USE_SLIM_RECENTS,
                    ((Boolean) newValue) ? 1 : 0);
            updatePreference();
            return true;
        } else if (preference == mMaxApps) {
            int maxApps = (Integer) newValue;
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.RECENTS_MAX_APPS, maxApps);
            return true;
        } else if (preference == mRecentPanelScale) {
            int recentPanelScale = (Integer) newValue;
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.RECENT_PANEL_SCALE_FACTOR, recentPanelScale);
            return true;
        } else if (preference == mRecentPanelExpandedMode) {
            int recentPanelExpandedMode = Integer.valueOf((String) newValue);
            int index = mRecentPanelExpandedMode.findIndexOfValue((String) newValue);
            Settings.System.putIntForUser(getActivity().getContentResolver(),
                    Settings.System.RECENT_PANEL_EXPANDED_MODE,
                recentPanelExpandedMode, UserHandle.USER_CURRENT);
            mRecentPanelExpandedMode.setSummary(mRecentPanelExpandedMode.getEntries()[index]);
            return true;
        } else if (preference == mRecentPanelLeftyMode) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.RECENT_PANEL_GRAVITY,
                    ((Boolean) newValue) ? Gravity.LEFT : Gravity.RIGHT);
            return true;
         }
         return false;
     }
	
}



