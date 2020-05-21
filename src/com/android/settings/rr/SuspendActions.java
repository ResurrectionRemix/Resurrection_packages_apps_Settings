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

import android.os.Bundle;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceCategory;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.SwitchPreference;
import android.os.UserHandle;
import android.os.UserManager;
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
public class SuspendActions extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "SuspendActions";
    private static final String SCREEN_STATE_TOOGLES_ENABLE = "screen_state_toggles_enable_key";
    private static final String SCREEN_STATE_TOOGLES_TWOG = "screen_state_toggles_twog";
    private static final String SCREEN_STATE_TOOGLES_3G = "screen_state_threeg";
    private static final String SCREEN_STATE_TOOGLES_GPS = "screen_state_toggles_gps";
    private static final String SCREEN_STATE_TOOGLES_MOBILE_DATA = "screen_state_mobile_data";
    private static final String SCREEN_STATE_CATGEGORY_LOCATION = "screen_state_toggles_location_key";
    private static final String SCREEN_STATE_CATGEGORY_MOBILE_DATA = "screen_state_toggles_mobile_key";

    private Context mContext;

    private SwitchPreference mEnableScreenStateToggles;
    private SwitchPreference mEnableScreenStateTogglesTwoG;
    private SwitchPreference mEnableScreenStateToggles3G;
    private SwitchPreference mEnableScreenStateTogglesGps;
    private SwitchPreference mEnableScreenStateTogglesMobileData;
    private PreferenceCategory mMobileDateCategory;
    private PreferenceCategory mLocationCategory;

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.RESURRECTED;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.suspend_actions);

        PreferenceScreen prefSet = getPreferenceScreen();
        ContentResolver resolver = getActivity().getContentResolver();

        mContext = (Context) getActivity();

        mEnableScreenStateToggles = (SwitchPreference) findPreference(
                SCREEN_STATE_TOOGLES_ENABLE);

        int enabled = Settings.Global.getInt(resolver, Settings.Global.START_SCREEN_STATE_SERVICE, 0);

        mEnableScreenStateToggles.setChecked(enabled != 0);
        mEnableScreenStateToggles.setOnPreferenceChangeListener(this);

        mMobileDateCategory = (PreferenceCategory) findPreference(
                SCREEN_STATE_CATGEGORY_MOBILE_DATA);
        mLocationCategory = (PreferenceCategory) findPreference(
                SCREEN_STATE_CATGEGORY_LOCATION);

        mEnableScreenStateTogglesTwoG = (SwitchPreference) findPreference(
                SCREEN_STATE_TOOGLES_TWOG);

        ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (!cm.isNetworkSupported(ConnectivityManager.TYPE_MOBILE)){
            getPreferenceScreen().removePreference(mEnableScreenStateTogglesTwoG);
        } else {
            mEnableScreenStateTogglesTwoG.setChecked((
                    Settings.System.getInt(resolver, Settings.System.SCREEN_STATE_TWOG, 0) == 1));
            mEnableScreenStateTogglesTwoG.setOnPreferenceChangeListener(this);
        }

        mEnableScreenStateToggles3G = (SwitchPreference) findPreference(
                SCREEN_STATE_TOOGLES_3G);

        if (!cm.isNetworkSupported(ConnectivityManager.TYPE_MOBILE)){
            getPreferenceScreen().removePreference(mEnableScreenStateToggles3G);
        } else {
            mEnableScreenStateToggles3G.setChecked((
                    Settings.System.getInt(resolver, Settings.System.SCREEN_STATE_THREEG, 0) == 1));
            mEnableScreenStateToggles3G.setOnPreferenceChangeListener(this);
        }

        mEnableScreenStateTogglesMobileData = (SwitchPreference) findPreference(
                SCREEN_STATE_TOOGLES_MOBILE_DATA);

        if (!cm.isNetworkSupported(ConnectivityManager.TYPE_MOBILE)){
            getPreferenceScreen().removePreference(mEnableScreenStateTogglesMobileData);
        } else {
            mEnableScreenStateTogglesMobileData.setChecked((
                    Settings.System.getInt(resolver, Settings.System.SCREEN_STATE_MOBILE_DATA, 0) == 1));
            mEnableScreenStateTogglesMobileData.setOnPreferenceChangeListener(this);
        }

        // Only enable these controls if this user is allowed to change location
        // sharing settings.
        final UserManager um = (UserManager) getActivity().getSystemService(Context.USER_SERVICE);
        boolean isLocationChangeAllowed = !um.hasUserRestriction(UserManager.DISALLOW_SHARE_LOCATION);

        // TODO: check if gps is available on this device?
        mEnableScreenStateTogglesGps = (SwitchPreference) findPreference(
                SCREEN_STATE_TOOGLES_GPS);

        if (!isLocationChangeAllowed){
            getPreferenceScreen().removePreference(mEnableScreenStateTogglesGps);
            mEnableScreenStateTogglesGps = null;
        } else {
            mEnableScreenStateTogglesGps.setChecked((
                    Settings.System.getInt(getActivity().getContentResolver(), Settings.System.SCREEN_STATE_GPS, 0) == 1));
            mEnableScreenStateTogglesGps.setOnPreferenceChangeListener(this);
        }

        mMobileDateCategory.setEnabled(enabled != 0);
        mLocationCategory.setEnabled(enabled != 0);
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        ContentResolver resolver = getActivity().getContentResolver();
        if (preference == mEnableScreenStateToggles) {
            boolean value = (Boolean) newValue;
            Settings.Global.putInt(resolver,
                    Settings.Global.START_SCREEN_STATE_SERVICE, value ? 1 : 0);

            Intent service = (new Intent())
                    .setClassName("com.android.systemui", "com.android.systemui.screenstate.ScreenStateService");
            if (value) {
                getActivity().stopService(service);
                getActivity().startService(service);
            } else {
                getActivity().stopService(service);
            }

            mMobileDateCategory.setEnabled(value);
            mLocationCategory.setEnabled(value);

            return true;
        } else if (preference == mEnableScreenStateTogglesTwoG) {
            boolean value = (Boolean) newValue;
            Settings.System.putInt(resolver,
                    Settings.System.SCREEN_STATE_TWOG, value ? 1 : 0);

            Intent intent = new Intent("android.intent.action.SCREEN_STATE_SERVICE_UPDATE");
            mContext.sendBroadcast(intent);

            return true;
        }  else if (preference == mEnableScreenStateToggles3G) {
            boolean value = (Boolean) newValue;
            Settings.System.putInt(resolver,
                    Settings.System.SCREEN_STATE_THREEG, value ? 1 : 0);

            Intent intent = new Intent("android.intent.action.SCREEN_STATE_SERVICE_UPDATE");
            mContext.sendBroadcast(intent);

            return true;
        } else if (preference == mEnableScreenStateTogglesGps) {
            boolean value = (Boolean) newValue;
            Settings.System.putInt(resolver,
                    Settings.System.SCREEN_STATE_GPS, value ? 1 : 0);

            Intent intent = new Intent("android.intent.action.SCREEN_STATE_SERVICE_UPDATE");
            mContext.sendBroadcast(intent);

            return true;
        } else if (preference == mEnableScreenStateTogglesMobileData) {
            boolean value = (Boolean) newValue;
            Settings.System.putInt(resolver,
                    Settings.System.SCREEN_STATE_MOBILE_DATA, value ? 1 : 0);

            Intent intent = new Intent("android.intent.action.SCREEN_STATE_SERVICE_UPDATE");
            mContext.sendBroadcast(intent);

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
                    sir.xmlResId = R.xml.suspend_actions;
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
