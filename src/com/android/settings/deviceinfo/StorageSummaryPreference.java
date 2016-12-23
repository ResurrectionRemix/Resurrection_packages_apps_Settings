/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.settings.deviceinfo;

import android.content.Context;
import android.graphics.Color;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.view.View;
import android.util.TypedValue;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.Utils;

public class StorageSummaryPreference extends Preference {
    private int mPercent = -1;
    private int mSecondaryColor;
    private int mAccentColor;

    private boolean mThemeEnabled;

    public StorageSummaryPreference(Context context) {
        super(context);

        setLayoutResource(R.layout.storage_summary);
        setEnabled(false);

        mThemeEnabled = Settings.Secure.getInt(context.getContentResolver(),
                Settings.Secure.THEME_ACCENT_COLOR, 1) != 0;

        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.textColorSecondary, typedValue, true);
        mSecondaryColor = context.getResources().getColor(typedValue.resourceId);
        mAccentColor = Utils.getColorAccent(context);
    }

    public void setPercent(int percent) {
        mPercent = percent;
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        final ProgressBar progress = (ProgressBar) view.findViewById(android.R.id.progress);
        if (mPercent != -1) {
            progress.setVisibility(View.VISIBLE);
            progress.setProgress(mPercent);
            progress.setScaleY(7f);
        } else {
            progress.setVisibility(View.GONE);
        }

        if (mThemeEnabled) {
            final TextView title = (TextView) view.findViewById(android.R.id.title);
            title.setTextColor(mAccentColor);
        }
        final TextView summary = (TextView) view.findViewById(android.R.id.summary);
        summary.setTextColor(mSecondaryColor);

        super.onBindViewHolder(view);
    }
}
