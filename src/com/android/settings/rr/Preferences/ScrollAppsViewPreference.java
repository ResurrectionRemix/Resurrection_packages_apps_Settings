/*
 * Copyright (C) 2017 The OmniROM Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.android.settings.rr.Preferences;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.android.settings.R;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ScrollAppsViewPreference extends Preference {
    private static final String TAG = "ScrollAppsPreference";

    private Context mContext;
    private List<String> mValues = new ArrayList<String>();
    private PackageManager mPm;
    private LayoutInflater mInflater;

    public ScrollAppsViewPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initPreference(context);
    }

    public ScrollAppsViewPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initPreference(context);
    }

    public ScrollAppsViewPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        initPreference(context);
    }

    public void setValues(Collection<String> values) {
        mValues.clear();
        mValues.addAll(values);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        LinearLayout linearLayout = (LinearLayout) holder.findViewById(R.id.selected_apps);
        if (linearLayout.getChildCount() > 0) linearLayout.removeAllViews();

        for (String value : mValues) {
            try {
                View v = mInflater.inflate(R.layout.app_grid_item, null);
                ComponentName componentName = ComponentName.unflattenFromString(value);
                Drawable icon = mPm.getActivityIcon(componentName);
                ((ImageView) v.findViewById(R.id.appIcon)).setImageDrawable(icon);
                v.setPadding(10, 5, 10, 5);
                linearLayout.addView(v);
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "Set app icon", e);
            }
        }
    }

    private void initPreference(Context context) {
        mContext = context;
        setLayoutResource(R.layout.preference_selected_apps_view);
        mPm = context.getPackageManager();
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }
}


