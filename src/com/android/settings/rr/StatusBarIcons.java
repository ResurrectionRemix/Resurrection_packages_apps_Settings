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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.UserHandle;
import androidx.preference.Preference;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.util.Log;
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
public class StatusBarIcons extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, Indexable {
    private static final String TAG = "StatusBarIcons";
    private static final String SMS_BREATH = "sms_breath";
    private static final String MISSED_CALL_BREATH = "missed_call_breath";
    private static final String VOICEMAIL_BREATH = "voicemail_breath";
    private SwitchPreference mSmsBreath;
    private SwitchPreference mMissedCallBreath;
    private SwitchPreference mVoicemailBreath;

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.RESURRECTED;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.rr_sb_icons);

        ContentResolver resolver = getActivity().getContentResolver();
           // Breathing Notifications
           mSmsBreath = (SwitchPreference) findPreference(SMS_BREATH);
           mMissedCallBreath = (SwitchPreference) findPreference(MISSED_CALL_BREATH);
           mVoicemailBreath = (SwitchPreference) findPreference(VOICEMAIL_BREATH);
           ConnectivityManager cm = (ConnectivityManager)
                   getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
           if (cm.isNetworkSupported(ConnectivityManager.TYPE_MOBILE)) {
               mSmsBreath.setChecked(Settings.Global.getInt(resolver,
                       Settings.Global.KEY_SMS_BREATH, 0) == 1);
               mSmsBreath.setOnPreferenceChangeListener(this);
               mMissedCallBreath.setChecked(Settings.Global.getInt(resolver,
                       Settings.Global.KEY_MISSED_CALL_BREATH, 0) == 1);
               mMissedCallBreath.setOnPreferenceChangeListener(this);
               mVoicemailBreath.setChecked(Settings.System.getInt(resolver,
                       Settings.System.KEY_VOICEMAIL_BREATH, 0) == 1);
               mVoicemailBreath.setOnPreferenceChangeListener(this);
           } else {
               prefSet.removePreference(mSmsBreath);
               prefSet.removePreference(mMissedCallBreath);
               prefSet.removePreference(mVoicemailBreath);
           }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
	    if (preference == mSmsBreath) {
            boolean value = (Boolean) newValue;
            Settings.Global.putInt(getContentResolver(), SMS_BREATH, value ? 1 : 0);
            return true;
        } else if (preference == mMissedCallBreath) {
            boolean value = (Boolean) newValue;
            Settings.Global.putInt(getContentResolver(), MISSED_CALL_BREATH, value ? 1 : 0);
            return true;
        } else if (preference == mVoicemailBreath) {
            boolean value = (Boolean) newValue;
            Settings.System.putInt(getContentResolver(), VOICEMAIL_BREATH, value ? 1 : 0);
            return true;
	    }
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
                    sir.xmlResId = R.xml.rr_sb_icons;
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
