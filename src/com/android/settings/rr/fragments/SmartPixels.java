/*
 * Copyright (C) 2018 crDroid Android Project
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

import android.content.Context;
import android.content.ContentResolver;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.SearchIndexableResource;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.preference.PreferenceScreen;
import android.support.v14.preference.SwitchPreference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.provider.Settings;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.SettingsPreferenceFragment;

import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable.SearchIndexProvider;
import com.android.settings.rr.Preferences.SystemSettingListPreference;

import java.util.ArrayList;
import java.util.List;

public class SmartPixels extends SettingsPreferenceFragment {

    private static final String TAG = "SmartPixels";

    private static final String SMART_PIXELS_SHIFT_TIMEOUT = "smart_pixels_shift_timeout";

    private SystemSettingListPreference mShiftTimeout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.smart_pixels);

        final PreferenceScreen prefScreen = getPreferenceScreen();

        mShiftTimeout = (SystemSettingListPreference) prefScreen.findPreference(SMART_PIXELS_SHIFT_TIMEOUT);

        boolean mShiftTimeoutSupported = getResources().getBoolean(
                com.android.internal.R.bool.config_enableBurnInProtection);
        if (!mShiftTimeoutSupported)
            prefScreen.removePreference(mShiftTimeout);

        mFooterPreferenceMixin.createFooterPreference().setTitle(R.string.smart_pixels_warning_text);
    }

    public static void reset(Context mContext) {
        ContentResolver resolver = mContext.getContentResolver();
        Settings.System.putIntForUser(resolver,
                Settings.System.SMART_PIXELS_ENABLE, 0, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.SMART_PIXELS_ON_POWER_SAVE, 0, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.SMART_PIXELS_PATTERN, 3, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.SMART_PIXELS_SHIFT_TIMEOUT, 5, UserHandle.USER_CURRENT);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.RESURRECTED;
    }

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        new BaseSearchIndexProvider() {
            @Override
            public List < SearchIndexableResource > getXmlResourcesToIndex(Context context,
                boolean enabled) {
                ArrayList < SearchIndexableResource > resources =
                    new ArrayList < SearchIndexableResource > ();
                SearchIndexableResource res = new SearchIndexableResource(context);
                res.xmlResId = R.xml.smart_pixels;
                resources.add(res);
                return resources;
            }

            @Override
            public List < String > getNonIndexableKeys(Context context) {
                List < String > keys = super.getNonIndexableKeys(context);

                if (!(context.getResources().getBoolean(com.android.internal.R.bool.config_enableBurnInProtection)))
                    keys.add(SMART_PIXELS_SHIFT_TIMEOUT);

                return keys;
            }
        };
}
