/*
 * Copyright (C) 2014 The Dirty Unicorns Project
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

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Context;
import android.os.Bundle;
import android.os.UserHandle;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceCategory;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.SwitchPreference;
import android.provider.Settings;

import android.provider.SearchIndexableResource;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.rr.Preferences.*;
import com.android.settings.rr.utils.RRUtils;
import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settingslib.search.SearchIndexable;


import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
@SearchIndexable
public class Ticker extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, Indexable {
    private SystemSettingListPreference mTicker;
    private SystemSettingListPreference mType;
    private SystemSettingSeekBarPreference mDuration;

    private static final String KEY_TICKER = "status_bar_show_ticker";
    private static final String KEY_TYPE = "status_bar_ticker_animation_mode";
    private static final String KEY_DURATION = "status_bar_ticker_tick_duration";
          

    private static final String TAG = "Ticker";

    @Override
    public void onCreate(Bundle savedInstanceState) {
       super.onCreate(savedInstanceState);
       addPreferencesFromResource(R.xml.ticker);
       mTicker = (SystemSettingListPreference) findPreference(KEY_TICKER);
       mTicker.setOnPreferenceChangeListener(this);
       mType = (SystemSettingListPreference) findPreference(KEY_TYPE);
       mDuration = (SystemSettingSeekBarPreference) findPreference(KEY_DURATION);
       updatePrefs(getTickerMode());
    }

    public int getTickerMode() {
       return Settings.System.getInt(getContentResolver(),
           Settings.System.STATUS_BAR_SHOW_TICKER,0);
    }

    private void updatePrefs(int enabled) {
       if (enabled == 0) {
           mDuration.setEnabled(false);
           mType.setEnabled(false);
       } else {
           mDuration.setEnabled(true);
           mType.setEnabled(true);
       }
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
         ContentResolver resolver = getActivity().getContentResolver();
         if (preference == mTicker) {
             int val = Integer.parseInt((String) newValue);
             updatePrefs(val);
             return true;
         }  
         return false;
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
                    sir.xmlResId = R.xml.ticker;
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
