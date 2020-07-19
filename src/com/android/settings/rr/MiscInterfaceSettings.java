/*Copyright (C) 2015 The ResurrectionRemix Project
     Licensed under the Apache License, Version 2.0 (the "License");
     you may not use this file except in compliance with the License.
     You may obtain a copy of the License at

          http://www.apache.org/licenses/LICENSE-2.0

     Unless required by applicable law or agreed to in writing, software
     distributed under the License is distributed on an "AS IS" BASIS,
     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     See the License for the specific language governing permissions and
     limitations under the License.
*/
package com.android.settings.rr;

import android.app.Activity;
import android.os.Bundle;
import android.app.Fragment;
import android.content.ContentResolver;
import android.content.Context;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceCategory;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.SwitchPreference;
import android.provider.SearchIndexableResource;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.rr.Preferences.SystemSettingSwitchPreference;
import com.android.settings.rr.utils.RRUtils;
import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settingslib.search.SearchIndexable;
import lineageos.hardware.LineageHardwareManager;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import lineageos.preference.LineageSystemSettingSwitchPreference;

@SearchIndexable
public class MiscInterfaceSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, Indexable {
    private static final String TAG = "MiscInterfaceSettings";
    private static final String KEY_DOZE_ON_CHARGE = "doze_on_charge";
    private static final String KEY_PROX_WAKE = "proximity_on_wake";
    private static final String KEY_HIGH_TOUCH = "high_touch_sensitivity_enable";

    private SystemSettingSwitchPreference mAod;
    private LineageSystemSettingSwitchPreference mWakeProx;
    private LineageSystemSettingSwitchPreference mHighTouch;

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.RESURRECTED;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.rr_interface_other_settings);
        mAod = (SystemSettingSwitchPreference) findPreference(KEY_DOZE_ON_CHARGE);
        mWakeProx = (LineageSystemSettingSwitchPreference) findPreference(KEY_PROX_WAKE);
        mHighTouch = (LineageSystemSettingSwitchPreference) findPreference(KEY_HIGH_TOUCH);
        boolean dozeAlwaysOnDisplayAvailable = getContext().getResources().
                getBoolean(com.android.internal.R.bool.config_dozeAlwaysOnDisplayAvailable);
        if (!dozeAlwaysOnDisplayAvailable && mAod != null) {
            getPreferenceScreen().removePreference(mAod);
        }
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return false;
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
                    sir.xmlResId = R.xml.rr_interface_other_settings;
                    result.add(sir);
                    return result;
            }

            @Override
            public List<String> getNonIndexableKeys(Context context) {
                List<String> keys = super.getNonIndexableKeys(context);
                    LineageHardwareManager hardware = LineageHardwareManager.getInstance(context);
                    if (!context.getResources().getBoolean(
                            org.lineageos.platform.internal.R.bool.config_proximityCheckOnWake)) {
                        keys.add(KEY_PROX_WAKE);
                    }
                    if (!hardware.isSupported(
                            LineageHardwareManager.FEATURE_HIGH_TOUCH_SENSITIVITY)) {
                        keys.add(KEY_HIGH_TOUCH);
                    }
                return keys;
            }
        };
}
