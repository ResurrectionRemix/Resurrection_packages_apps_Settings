/*
 * Copyright (C) 2016-2020 crDroid Android Project
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

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.SwitchPreference;

import com.android.settings.SettingsPreferenceFragment;
import com.android.internal.logging.nano.MetricsProto;

import com.android.settings.R;
import java.util.ArrayList;
import java.util.List;

public class CutoutSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String DISPLAY_CUTOUT = "sysui_display_cutout";
    private static final String DISPLAY_CUTOUT_MODE = "display_cutout_mode";
    private static final String STOCK_STATUSBAR = "stock_statusbar_in_hide";

    private Preference mDisplayCutout;
    private Preference mStockStatusbar;
    private ListPreference mImmersiveMode;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.cutout);

        Context mContext = getActivity().getApplicationContext();

        final PreferenceScreen prefScreen = getPreferenceScreen();
        final Resources res = getResources();

        mImmersiveMode = (ListPreference) prefScreen.findPreference(DISPLAY_CUTOUT_MODE);
        mImmersiveMode.setOnPreferenceChangeListener(this);

        int immersiveMode = Settings.System.getInt(getContentResolver(),
                Settings.System.DISPLAY_CUTOUT_MODE, 0);
        mDisplayCutout = (Preference) prefScreen.findPreference(DISPLAY_CUTOUT);
        mDisplayCutout.setEnabled(immersiveMode == 0);

        mStockStatusbar = (Preference) prefScreen.findPreference(STOCK_STATUSBAR);
        mStockStatusbar.setEnabled(immersiveMode == 2);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mImmersiveMode) {
            int value = Integer.valueOf((String) newValue);
            mDisplayCutout.setEnabled(value == 0);
            mStockStatusbar.setEnabled(value == 2);
            return true;
        }
        return false;
    }

    public static void reset(Context mContext) {
        ContentResolver resolver = mContext.getContentResolver();
        Settings.Secure.putIntForUser(resolver,
                Settings.Secure.SYSUI_DISPLAY_CUTOUT, 1, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.DISPLAY_CUTOUT_MODE, 0, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.STOCK_STATUSBAR_IN_HIDE, 1, UserHandle.USER_CURRENT);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.RESURRECTED;
    }
}
