/*
 * Copyright (C) 2014 The LiquidSmooth Project
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

package com.android.settings.rr;

import android.content.Context;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.Build;
import android.preference.Preference;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.preference.PreferenceScreen;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.Preference.OnPreferenceChangeListener;
import android.provider.Settings;
import android.preference.SwitchPreference;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import java.util.List;
import com.android.settings.Utils;

import com.android.internal.logging.MetricsLogger;

public class MiscSettings extends SettingsPreferenceFragment  implements OnPreferenceChangeListener{

private static final String ENABLE_MULTI_WINDOW_KEY = "enable_multi_window";
private static final String MULTI_WINDOW_SYSTEM_PROPERTY = "persist.sys.debug.multi_window";

private SwitchPreference mEnableMultiWindow;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.rr_misc);
	  final ContentResolver resolver = getActivity().getContentResolver();
        mEnableMultiWindow = (SwitchPreference) findPreference(ENABLE_MULTI_WINDOW_KEY);
    }

  private static boolean showEnableMultiWindowPreference() {
        return !"user".equals(Build.TYPE);
    }

    private void setEnableMultiWindow(boolean value) {
        SystemProperties.set(MULTI_WINDOW_SYSTEM_PROPERTY, String.valueOf(value));
    }

    protected int getMetricsCategory()
    {
	return MetricsLogger.DEVELOPMENT;
    }

	@Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mEnableMultiWindow) {
            if (mEnableMultiWindow.isChecked()) {
                setEnableMultiWindow(true);
            } else {
                setEnableMultiWindow(false);
            }
        } else {
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }
        return false;
    }

    public boolean onPreferenceChange(Preference preference, Object value) {
         return true;
    }
}
