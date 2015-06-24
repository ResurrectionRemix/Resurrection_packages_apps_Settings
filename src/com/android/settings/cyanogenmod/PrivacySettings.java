/*
 * Copyright (C) 2014 The CyanogenMod Project
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

import android.content.Context;
import android.content.Intent;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.SearchIndexableResource;
import com.android.internal.telephony.util.BlacklistUtils;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Privacy settings
 */
public class PrivacySettings extends SettingsPreferenceFragment implements Indexable {

    private static final String TAG = "PrivacySettings";
    private static final String KEY_BLACKLIST = "blacklist";
    private static final String KEY_WHISPERPUSH = "whisperpush";
    private static final String WHISPERPUSH_ORIGINAL = "org.whispersystems.whisperpush";
    private static final String WHISPERPUSH_UPDATE = "org.whispersystems.whisperpush2";

    private PreferenceScreen mBlacklist;
    private Preference mWhisperPush;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.privacy_settings_cyanogenmod);

        mBlacklist = (PreferenceScreen) findPreference(KEY_BLACKLIST);
        mWhisperPush = (Preference) findPreference(KEY_WHISPERPUSH);

        // Add package manager to check if features are available
        PackageManager pm = getPackageManager();

        // Determine options based on device telephony support
        if (!pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
            // No telephony, remove dependent options
            PreferenceScreen root = getPreferenceScreen();
            root.removePreference(mBlacklist);
            root.removePreference(mWhisperPush);
        } else {
            // TODO: once we are shipping with updated WhisperPush by default
            // all this code can be pulled out - WF
            if (isWhisperPushUpdated(pm, getActivity())) {
                // redirect intent to updated whisperpush2 package
                Intent intent = mWhisperPush.getIntent();
                ComponentName component = intent.getComponent();
                intent.setClassName(WHISPERPUSH_UPDATE, component.getClassName());
                mWhisperPush.setIntent(intent);
                Log.d(TAG, "Using WhisperPush2");
            } else if (!isWhisperPushOriginalOK(pm)) {
                getPreferenceScreen().removePreference(mWhisperPush);
            }
        }

    }

    private static boolean isWhisperPushUpdated(PackageManager pm, Context context) {
        // updated package is present
        if (!Utils.isPackageInstalled(context, WHISPERPUSH_UPDATE)) {
            return false;
        }

        // ...and it has been granted the correct permission
        int result = pm.checkPermission(
            android.Manifest.permission.INTERCEPT_SMS,
            WHISPERPUSH_UPDATE);
        if(result == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            // Stranger Danger!
            Log.e(TAG, "WhisperPush2 package present, but missing required permission!");
            return false;
        }
    }

    private static boolean isWhisperPushOriginalOK(PackageManager pm) {
        try {
            PackageInfo info = pm.getPackageInfo(
                WHISPERPUSH_ORIGINAL, PackageManager.GET_PERMISSIONS);
            if (info != null && info.applicationInfo != null &&
                (info.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                return true;
            } else {
                Log.e(TAG, "WhisperPush package present, but not system app");
                return false;
            }
        } catch (PackageManager.NameNotFoundException e) {
            return false; // not installed
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateBlacklistSummary();
    }

    private void updateBlacklistSummary() {
        if (BlacklistUtils.isBlacklistEnabled(getActivity())) {
            mBlacklist.setSummary(R.string.blacklist_summary);
        } else {
            mBlacklist.setSummary(R.string.blacklist_summary_disabled);
        }
    }

    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                                                                            boolean enabled) {
                    ArrayList<SearchIndexableResource> result =
                            new ArrayList<SearchIndexableResource>();

                    SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.privacy_settings_cyanogenmod;
                    result.add(sir);

                    return result;
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    ArrayList<String> result = new ArrayList<String>();
                    PackageManager pm = context.getPackageManager();

                    // Determine options based on device telephony support
                    if (!pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
                        result.add(KEY_BLACKLIST);
                        result.add(KEY_WHISPERPUSH);
                    } else if (!(isWhisperPushOriginalOK(pm) ||
                                 isWhisperPushUpdated(pm, context))) {
                        result.add(KEY_WHISPERPUSH);
                    }
                    return result;
                }
            };
}
