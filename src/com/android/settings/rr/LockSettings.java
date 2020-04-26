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
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.RemoteException;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceCategory;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.SwitchPreference;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.os.UserHandle;
import android.provider.Settings;
import android.net.Uri;

import com.android.settings.Utils;
import com.android.settings.rr.Preferences.CustomSeekBarPreference;

import com.android.settings.rr.Preferences.SystemSettingColorPickerPreference;

import android.provider.SearchIndexableResource;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;

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
public class LockSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, Indexable {

    private static final String PULSE_AMBIENT_LIGHT_CUSTOM_COLOR = "ambient_light_custom_color";
    private static final String PULSE_AMBIENT_LIGHT_COLOR = "ambient_light_color";
	
    private SystemSettingColorPickerPreference mEdgeLightColorPreference;
    private ListPreference mColorType;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.rr_lock);
        ContentResolver resolver = getActivity().getContentResolver();

        mEdgeLightColorPreference = (SystemSettingColorPickerPreference) findPreference(PULSE_AMBIENT_LIGHT_CUSTOM_COLOR);
        mEdgeLightColorPreference.setOnPreferenceChangeListener(this);

        mColorType = (ListPreference) findPreference(PULSE_AMBIENT_LIGHT_COLOR);
        int type = Settings.System.getInt(resolver,
            Settings.System.AMBIENT_LIGHT_COLOR, 0);
        mColorType.setValue(String.valueOf(type));
        mColorType.setSummary(mColorType.getEntry());
        updateprefs(type);
        mColorType.setOnPreferenceChangeListener(this);
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        ContentResolver resolver = getActivity().getContentResolver();
        if (preference == mColorType) {
            int val = Integer.parseInt((String) newValue);
            int index = mColorType.findIndexOfValue((String) newValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.AMBIENT_LIGHT_COLOR, val);
            mColorType.setSummary(mColorType.getEntries()[index]);
            updateprefs(val);
            return true;
        }
       return false;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.RESURRECTED;
    }

    public void updateprefs(int type) {
         if (type == 3 ) {
             mEdgeLightColorPreference.setEnabled(true);
         } else {
             mEdgeLightColorPreference.setEnabled(false);
         }
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
                    sir.xmlResId = R.xml.rr_lock;
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
