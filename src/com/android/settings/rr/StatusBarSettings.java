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
import android.content.Context;
import android.app.Fragment;
import android.content.ContentResolver;
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
import android.provider.Settings;
import android.provider.SearchIndexableResource;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;

import com.android.settings.rr.utils.RRUtils;
import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settingslib.search.SearchIndexable;

import com.android.settings.rr.Preferences.SystemSettingSwitchPreference;


import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
@SearchIndexable
public class StatusBarSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, Indexable {
          private static final String STATUSBAR_DUAL = "statusbar_dual";
          private SystemSettingSwitchPreference mStatusbarDual;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.rr_statusbar);

        PreferenceScreen prefSet = getPreferenceScreen();
		ContentResolver resolver = getActivity().getContentResolver();
        int anim = Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.RR_CONFIG_ANIM, 0);
        try {
            if (anim == 0) {
                removePreference("animation");
            } else if (anim == 1) {
                removePreference("preview");
            } else if (anim == 2) {
                removePreference("animation");
                removePreference("preview");
            }
        } catch (Exception e) {}

        mStatusbarDual = (SystemSettingSwitchPreference) findPreference(STATUSBAR_DUAL);
        mStatusbarDual.setChecked((Settings.System.getInt(resolver,
                Settings.System.STATUSBAR_DUAL, 0) == 1));
        mStatusbarDual.setOnPreferenceChangeListener(this);
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
	ContentResolver resolver = getActivity().getContentResolver();
	      if (preference == mStatusbarDual) {
            boolean value = (Boolean) newValue;
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUSBAR_DUAL, value ? 1 : 0);
            RRUtils.showSystemUiRestartDialog(getContext());
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
                    sir.xmlResId = R.xml.rr_statusbar;
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
