/*
 * Copyright (C) 2017-2019 The LineageOS Project
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
import android.content.Context;
import android.content.ContentResolver;
import android.os.Bundle;
import android.os.UserHandle;
import android.widget.Toast;

import androidx.preference.DropDownPreference;
import androidx.preference.*;
import android.provider.Settings;

import lineageos.preference.LineageSecureSettingSwitchPreference;
import lineageos.providers.LineageSettings;


import android.provider.SearchIndexableResource;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;

import com.android.settings.rr.utils.RRUtils;
import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settingslib.search.SearchIndexable;

import com.android.settings.rr.Preferences.CustomSeekBarPreference;


import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
@SearchIndexable
public class NetworkTrafficSettings extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener, Indexable {

    private static final String TAG = "NetworkTrafficSettings";

    private CustomSeekBarPreference mNetTrafficAutohideThreshold;
    private CustomSeekBarPreference mNetTrafficRefreshInterval;
    private ListPreference mNetTrafficLocation;
    private ListPreference mNetTrafficMode;
    private ListPreference mNetTrafficUnits;
    private SwitchPreference mNetTrafficAutohide;
    private SwitchPreference mNetTrafficShowUnits;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.network_traffic_settings);
        final ContentResolver resolver = getActivity().getContentResolver();

        mNetTrafficAutohideThreshold = (CustomSeekBarPreference)
                findPreference(LineageSettings.Secure.NETWORK_TRAFFIC_AUTOHIDE_THRESHOLD);
        mNetTrafficRefreshInterval = (CustomSeekBarPreference)
                findPreference(LineageSettings.Secure.NETWORK_TRAFFIC_REFRESH_INTERVAL);
        mNetTrafficLocation = (ListPreference)
                findPreference(LineageSettings.Secure.NETWORK_TRAFFIC_LOCATION);
        mNetTrafficLocation.setOnPreferenceChangeListener(this);
        mNetTrafficMode = (ListPreference)
                findPreference(LineageSettings.Secure.NETWORK_TRAFFIC_MODE);
        mNetTrafficUnits = (ListPreference)
                findPreference(LineageSettings.Secure.NETWORK_TRAFFIC_UNITS);
        mNetTrafficAutohide = (SwitchPreference)
                findPreference(LineageSettings.Secure.NETWORK_TRAFFIC_AUTOHIDE);
        mNetTrafficShowUnits = (SwitchPreference)
                findPreference(LineageSettings.Secure.NETWORK_TRAFFIC_SHOW_UNITS);

        int location = LineageSettings.Secure.getIntForUser(resolver,
                LineageSettings.Secure.NETWORK_TRAFFIC_LOCATION, 0, UserHandle.USER_CURRENT);
        updateEnabledStates(location);
    }
  @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mNetTrafficLocation) {
            int location = Integer.valueOf((String) newValue);
            updateEnabledStates(location);
            return true;
        }
        return false;
    }

    private void updateEnabledStates(int location) {
        final boolean enabled = location != 0;
        mNetTrafficMode.setEnabled(enabled);
        mNetTrafficAutohide.setEnabled(enabled);
        mNetTrafficAutohideThreshold.setEnabled(enabled);
        mNetTrafficRefreshInterval.setEnabled(enabled);
        mNetTrafficUnits.setEnabled(enabled);
        mNetTrafficShowUnits.setEnabled(enabled);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.RESURRECTED;
    }

    /**
     * For Search.
     */
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        new BaseSearchIndexProvider() {
            @Override
            public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean enabled) {
                ArrayList<SearchIndexableResource> result =
                    new ArrayList<SearchIndexableResource>();
                    SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.network_traffic_settings;
                    result.add(sir);
                    return result;
            }

            @Override
            public List<String> getNonIndexableKeys(Context context) {
                List<String> keys = super.getNonIndexableKeys(context);
                return keys;
            }
        };
}
