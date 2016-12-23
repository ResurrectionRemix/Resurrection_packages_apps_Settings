/*
 * Copyright (C) 2017 The Paranoid Android Project
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

package com.android.settings.display;

import android.annotation.Nullable;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.android.settings.PreviewSeekBarPreferenceFragment;
import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;

import java.util.ArrayList;
import java.util.List;

/**
 * Preference fragment used to theme colors.
 */
public class ThemeSettings extends PreviewSeekBarPreferenceFragment implements Indexable {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mActivityLayoutResId = R.layout.theme_activity;

        mPreviewSampleResIds = new int[]{R.layout.screen_zoom_preview_1,
                R.layout.screen_zoom_preview_2,
                R.layout.screen_zoom_preview_settings};

        mEntries = getResources().getStringArray(R.array.color_name);
    }

    @Override
    protected void commit() {
        // no-op
    }

    @Override
    protected Configuration createConfig(Configuration origConfig, int index) {
        return new Configuration();
    }

    @Override
    protected int getMetricsCategory() {
        return -1;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, 0, 0, R.string.theme_switch_palette)
                .setIcon(R.drawable.switch_colors)
                .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 0) {
            switchPalette();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /** Index provider used to expose this fragment in search. */
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
                    final Resources res = context.getResources();
                    final SearchIndexableRaw data = new SearchIndexableRaw(context);
                    data.title = res.getString(R.string.theme_title);
                    data.screenTitle = res.getString(R.string.theme_title);

                    final List<SearchIndexableRaw> result = new ArrayList<>(1);
                    result.add(data);
                    return result;
                }
            };
}
