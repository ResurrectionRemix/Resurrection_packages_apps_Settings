/*
 * Copyright (C) 2013 Android Open Kang Project
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

package com.android.settings.cyanogenmod;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.SwitchPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.PreferenceCategory;
import android.provider.Settings;
import android.content.Intent;
import android.util.Log;
import android.net.ConnectivityManager;
import android.content.Context;
import android.os.UserManager;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.R;

public class ScreenStateToggles extends SettingsPreferenceFragment implements OnPreferenceChangeListener {
    private static final String TAG = "ScreenStateToggles";
    private static final String SCREEN_STATE_TOOGLES_ENABLE = "screen_state_toggles_enable_key";
    private static final String SCREEN_STATE_TOOGLES_TWOG = "screen_state_toggles_twog";
    private static final String SCREEN_STATE_TOOGLES_GPS = "screen_state_toggles_gps";
    private static final String SCREEN_STATE_TOOGLES_MOBILE_DATA = "screen_state_toggles_mobile_data";
    private static final String SCREEN_STATE_CATGEGORY_LOCATION = "screen_state_toggles_location_key";
    private static final String SCREEN_STATE_CATGEGORY_MOBILE_DATA = "screen_state_toggles_mobile_key";

    private SwitchPreference mEnableScreenStateToggles;
    private CheckBoxPreference mEnableScreenStateTogglesTwoG;
    private CheckBoxPreference mEnableScreenStateTogglesGps;
    private CheckBoxPreference mEnableScreenStateTogglesMobileData;
    private PreferenceCategory mMobileDateCategory;
    private PreferenceCategory mLocationCategory;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.screen_state_toggles_title);

        addPreferencesFromResource(R.xml.prefs_screen_state_toggles);
        PreferenceScreen prefSet = getPreferenceScreen();

        mEnableScreenStateToggles = (SwitchPreference) prefSet.findPreference(
                SCREEN_STATE_TOOGLES_ENABLE);

        boolean enabled = Settings.System.getBoolean(getContentResolver(), Settings.System.START_SCREEN_STATE_SERVICE, false);
        mEnableScreenStateToggles.setChecked(enabled);
        mEnableScreenStateToggles.setOnPreferenceChangeListener(this);

        mMobileDateCategory = (PreferenceCategory) prefSet.findPreference(
                SCREEN_STATE_CATGEGORY_MOBILE_DATA);
        mLocationCategory = (PreferenceCategory) prefSet.findPreference(
                SCREEN_STATE_CATGEGORY_LOCATION);

        mEnableScreenStateTogglesTwoG = (CheckBoxPreference) prefSet.findPreference(
                SCREEN_STATE_TOOGLES_TWOG);

        ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (!cm.isNetworkSupported(ConnectivityManager.TYPE_MOBILE)){
            getPreferenceScreen().removePreference(mEnableScreenStateTogglesTwoG);
        } else {
            mEnableScreenStateTogglesTwoG.setChecked(
                Settings.System.getBoolean(getContentResolver(), Settings.System.SCREEN_STATE_TWOG, false));
            mEnableScreenStateTogglesTwoG.setOnPreferenceChangeListener(this);
        }

        mEnableScreenStateTogglesMobileData = (CheckBoxPreference) prefSet.findPreference(
                SCREEN_STATE_TOOGLES_MOBILE_DATA);

        if (!cm.isNetworkSupported(ConnectivityManager.TYPE_MOBILE)){
            getPreferenceScreen().removePreference(mEnableScreenStateTogglesMobileData);
        } else {
            mEnableScreenStateTogglesMobileData.setChecked(
                Settings.System.getBoolean(getContentResolver(), Settings.System.SCREEN_STATE_MOBILE_DATA, false));
            mEnableScreenStateTogglesMobileData.setOnPreferenceChangeListener(this);
        }

        // Only enable these controls if this user is allowed to change location
        // sharing settings.
        final UserManager um = (UserManager) getActivity().getSystemService(Context.USER_SERVICE);
        boolean isLocationChangeAllowed = !um.hasUserRestriction(UserManager.DISALLOW_SHARE_LOCATION);

        // TODO: check if gps is available on this device?
        mEnableScreenStateTogglesGps = (CheckBoxPreference) prefSet.findPreference(
                SCREEN_STATE_TOOGLES_GPS);

        if (!isLocationChangeAllowed){
            getPreferenceScreen().removePreference(mEnableScreenStateTogglesGps);
            mEnableScreenStateTogglesGps = null;
        } else {
            mEnableScreenStateTogglesGps.setChecked(
                Settings.System.getBoolean(getContentResolver(), Settings.System.SCREEN_STATE_GPS, false));
            mEnableScreenStateTogglesGps.setOnPreferenceChangeListener(this);
        }

        mMobileDateCategory.setEnabled(enabled);
        mLocationCategory.setEnabled(enabled);
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mEnableScreenStateToggles) {
            boolean value = ((Boolean) newValue).booleanValue();
            Settings.System.putBoolean(getContentResolver(),
                    Settings.System.START_SCREEN_STATE_SERVICE, value);

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
            Settings.System.putBoolean(getContentResolver(),
                    Settings.System.SCREEN_STATE_TWOG, (Boolean) newValue);

            Intent intent = new Intent("android.intent.action.SCREEN_STATE_SERVICE_UPDATE");
            mContext.sendBroadcast(intent);

            return true;
        } else if (preference == mEnableScreenStateTogglesGps) {
            Settings.System.putBoolean(getContentResolver(),
                    Settings.System.SCREEN_STATE_GPS, (Boolean) newValue);

            Intent intent = new Intent("android.intent.action.SCREEN_STATE_SERVICE_UPDATE");
            mContext.sendBroadcast(intent);

            return true;
        } else if (preference == mEnableScreenStateTogglesMobileData) {
            Settings.System.putBoolean(getContentResolver(),
                    Settings.System.SCREEN_STATE_MOBILE_DATA, (Boolean) newValue);

            Intent intent = new Intent("android.intent.action.SCREEN_STATE_SERVICE_UPDATE");
            mContext.sendBroadcast(intent);

            return true;
        }


        return false;
    }

    private void restartService(){
        Intent service = (new Intent())
                .setClassName("com.android.systemui", "com.android.systemui.screenstate.ScreenStateService");
        getActivity().stopService(service);
        getActivity().startService(service);
    }
}
