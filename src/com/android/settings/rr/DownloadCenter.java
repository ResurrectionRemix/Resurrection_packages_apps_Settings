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
static final String DOWNLOAD_URL = "https://github-cloud.s3.amazonaws.com/releases/35777460/a3bfcd70-e66d-11e5-83e3-4b50f52d466c.zip?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=AKIAISTNZFOVBIJMK3TQ%2F20160310%2Fus-east-1%2Fs3%2Faws4_request&X-Amz-Date=20160310T161959Z&X-Amz-Expires=300&X-Amz-Signature=160211b69687604c473dd5a596078c543c15ba0189962dbc3f4182d5558a3b4c&X-Amz-SignedHeaders=host&actor_id=3596538&response-content-disposition=attachment%3B%20filename%3Dopen_gapps-arm-6.0-nano-20160310.zip&response-content-type=application%2Foctet-stream";

@Override
public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    addPreferencesFromResource(R.xml.rr_download_center);
    Preference myPref = (Preference) findPreference("download_gapps");
    myPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                 public boolean onPreferenceClick(Preference preference) {
                     download();
                     return true;
                 }
             });
}

protected int getMetricsCategory()
{
  return MetricsLogger.APPLICATION;
}

public void download()
{
  DownloadManager downloadmanager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                        Uri uri = Uri
                                .parse(DOWNLOAD_URL);
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
