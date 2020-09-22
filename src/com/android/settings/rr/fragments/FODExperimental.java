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

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.settings.rr.utils.RRContextConstants;
import android.provider.SearchIndexableResource;
import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.rr.Preferences.*;
import com.android.settings.rr.utils.RRUtils;
import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settingslib.search.SearchIndexable;

import com.android.internal.widget.LockPatternUtils;
import android.provider.Settings;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

public class FODExperimental extends SettingsPreferenceFragment{
    private static final String SCREEN_OFF = "screenoff";
    private static final String FOD_ANIM = "fod_anim_lockscreen";
    private SystemSettingSwitchPreference mAnim;
    private PreferenceCategory mScreenoff;
    private boolean mSupported;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.fod_exp);
        mAnim = (SystemSettingSwitchPreference) findPreference (FOD_ANIM);
        mScreenoff = (PreferenceCategory) findPreference(SCREEN_OFF);
        boolean isenabled = Settings.System.getInt(getContentResolver(),
                Settings.System.FOD_RECOGNIZING_ANIMATION, 0) != 0;
        mSupported =  getResources().getBoolean(
                R.bool.config_supportScreenOffFod);
        if (!mSupported) {
            if (mScreenoff != null) mScreenoff.setVisible(false);
        }
        if (!isenabled) {
            mAnim.setEnabled(false);
            mAnim.setSummary(R.string.fod_anim_disabled_warning);
        }
        mFooterPreferenceMixin.createFooterPreference()
                .setTitle(R.string.fod_exp_warning);

    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.RESURRECTED;
    }
}
