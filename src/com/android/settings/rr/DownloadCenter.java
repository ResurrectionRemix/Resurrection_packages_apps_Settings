/*
 * Copyright (C) 2016 Resurrection Remix
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

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.SearchIndexableResource;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import java.util.List;
import java.util.ArrayList;

import com.android.internal.logging.MetricsLogger;

public class DownloadCenter extends SettingsPreferenceFragment  implements Indexable {

private static final String TAG = "DownloadCenter";
static final String BANKS_DOWNLOAD_URL = "http://download.dirtyunicorns.com/files/gapps/banks_gapps/BANKS_GAPPS_6.XX.zip";

@Override
public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    addPreferencesFromResource(R.xml.rr_download_center);
    Preference myPref = (Preference) findPreference("download_banks_gapps");
    myPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                 public boolean onPreferenceClick(Preference preference) {
                     downloadBanks();
                     return true;
                 }
             });
}

protected int getMetricsCategory()
{
  return MetricsLogger.APPLICATION;
}

public void downloadBanks()
{
  DownloadManager downloadmanager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                        Uri uri = Uri
                                .parse(BANKS_DOWNLOAD_URL);
                        DownloadManager.Request request = new DownloadManager.Request(uri);
                        Long reference = downloadmanager.enqueue(request);
}
public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        new BaseSearchIndexProvider() {
            @Override
            public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                                                                        boolean enabled) {
                ArrayList<SearchIndexableResource> result =
                        new ArrayList<SearchIndexableResource>();

                SearchIndexableResource sir = new SearchIndexableResource(context);
               sir.xmlResId = R.xml.rr_download_center;
                result.add(sir);

                return result;
            }

            @Override
            public List<String> getNonIndexableKeys(Context context) {
                final List<String> keys = new ArrayList<String>();
                return keys;
            }
    };
}
