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
package com.android.settings.rr.navigation.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.ContentResolver;
import android.content.res.Resources;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.SearchIndexableResource;
import android.support.v7.preference.ListPreference;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.PreferenceScreen;
import com.android.settings.accessibility.ToggleFontSizePreferenceFragment;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.applications.LayoutPreference;
import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable.SearchIndexProvider;
import com.android.settings.SettingsPreferenceFragment;
//import com.android.settings.rr.utils.TelephonyUtils;

import java.util.ArrayList;
import java.util.List;

public class UISettingsNav extends SettingsPreferenceFragment implements
    Preference.OnPreferenceChangeListener {
    private static final String TAG = "UI";
    private static final String RR_FP = "rr_fp";
    private static final String INCALL_VIB_OPTIONS = "rr_incall";
    private static final String SMART_PIXELS = "smart_pixels";
    private Preference mSmartPixels;
    private LayoutPreference mFpFragment;
    private FingerprintManager mFingerprintManager;
    private LayoutPreference mInCallFragment;

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.RESURRECTED;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Activity activity = getActivity(); 
        ContentResolver resolver = getActivity().getContentResolver();
        mFingerprintManager = (FingerprintManager) getActivity().getSystemService(Context.FINGERPRINT_SERVICE);

        addPreferencesFromResource(R.xml.rr_ui_settings_navigation);
        final PreferenceScreen prefScreen = getPreferenceScreen();
        mFpFragment = (LayoutPreference) findPreference(RR_FP);
        if (mFingerprintManager == null || !mFingerprintManager.isHardwareDetected())
            prefScreen.removePreference(mFpFragment);

        mSmartPixels = (Preference) prefScreen.findPreference(SMART_PIXELS);
        boolean mSmartPixelsSupported = getResources().getBoolean(
                com.android.internal.R.bool.config_supportSmartPixels);
        if (!mSmartPixelsSupported)
            prefScreen.removePreference(mSmartPixels);

        mInCallFragment = (LayoutPreference) findPreference(INCALL_VIB_OPTIONS);
       /*
        if (!TelephonyUtils.isVoiceCapable(getActivity())) {
            getPreferenceScreen().removePreference(mInCallFragment);
        } */

    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return false;
    }

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        new BaseSearchIndexProvider() {
            @Override
            public List < SearchIndexableResource > getXmlResourcesToIndex(Context context,
                boolean enabled) {
                ArrayList < SearchIndexableResource > resources =
                    new ArrayList < SearchIndexableResource > ();
                SearchIndexableResource res = new SearchIndexableResource(context);
                res.xmlResId = R.xml.rr_ui_settings_navigation;
                resources.add(res);
                return resources;
            }

            @Override
            public List < String > getNonIndexableKeys(Context context) {
                List < String > keys = super.getNonIndexableKeys(context);
                FingerprintManager mFingerprintManager = (FingerprintManager)
                context.getSystemService(Context.FINGERPRINT_SERVICE);

                if (mFingerprintManager == null || !mFingerprintManager.isHardwareDetected())
                    keys.add(RR_FP);

                if (!context.getResources().getBoolean(com.android.internal.R.bool.config_supportSmartPixels))
                    keys.add(SMART_PIXELS);

                return keys;
            }
        };
}
