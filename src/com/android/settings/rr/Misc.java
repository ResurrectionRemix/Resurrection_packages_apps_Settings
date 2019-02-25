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
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.SearchIndexableResource;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v14.preference.SwitchPreference;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.provider.Settings;
import android.content.Context;
import android.os.UserHandle;

import com.android.settings.R;
import com.android.settings.rr.utils.RRUtils;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable.SearchIndexProvider;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;

import java.util.ArrayList;
import java.util.List;

public class Misc extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String FLASHLIGHT_ON_CALL = "flashlight_on_call";

    private ListPreference mFlashlightOnCall;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.rr_misc);

 	Context mContext = getActivity().getApplicationContext();

        PreferenceScreen prefScreen = getPreferenceScreen();
        PreferenceScreen prefSet = getPreferenceScreen();
		ContentResolver resolver = getActivity().getContentResolver();

        mFlashlightOnCall = (ListPreference) findPreference(FLASHLIGHT_ON_CALL);
        if (!RRUtils.deviceSupportsFlashLight(mContext))
            prefScreen.removePreference(mFlashlightOnCall);

    }


    public boolean onPreferenceChange(Preference preference, Object newValue) {
	ContentResolver resolver = getActivity().getContentResolver();
	return false;
    }


    public static void reset(Context mContext) {
        ContentResolver resolver = mContext.getContentResolver();
        Settings.System.putIntForUser(resolver,
                Settings.System.FLASHLIGHT_ON_CALL, 0, UserHandle.USER_CURRENT);
    }


    @Override
    public int getMetricsCategory() {
        return MetricsEvent.RESURRECTED;
    }

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        new BaseSearchIndexProvider() {
            @Override
            public List < SearchIndexableResource > getXmlResourcesToIndex(Context context,
                boolean enabled) {
                ArrayList < SearchIndexableResource > resources =
                    new ArrayList < SearchIndexableResource > ();
                SearchIndexableResource res = new SearchIndexableResource(context);
                res.xmlResId = R.xml.rr_misc;
                resources.add(res);
                return resources;
            }

            @Override
            public List < String > getNonIndexableKeys(Context context) {
                 List<String> keys = super.getNonIndexableKeys(context);

                 if (!RRUtils.deviceSupportsFlashLight(context))
                     keys.add(FLASHLIGHT_ON_CALL);

                return keys;
            }
        };
}
