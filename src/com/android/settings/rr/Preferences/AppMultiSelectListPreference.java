/*
 *  Copyright (C) 2016 The OmniROM Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.android.settings.rr.Preferences;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settingslib.CustomDialogPreferenceCompat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class AppMultiSelectListPreference extends CustomDialogPreferenceCompat {
    private static final String TAG = "AppMultiSelectList";
    private static final boolean DEBUG = false;

    private final List<PackageItem> mPackageInfoList = new ArrayList<PackageItem>();
    private AppListAdapter mAdapter;
    private List<String> mValues = new ArrayList<String>();
    private PackageManager mPm;

    public AppMultiSelectListPreference(Context context) {
        this(context, null);
    }

    public AppMultiSelectListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDialogLayoutResource(R.layout.preference_app_list);

        mPm = context.getPackageManager();

        final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> installedAppsInfo = getContext().getPackageManager().queryIntentActivities(
                mainIntent, 0);

        for (ResolveInfo info : installedAppsInfo) {
            ComponentName componentName = new ComponentName(
                    info.activityInfo.applicationInfo.packageName,
                    info.activityInfo.name);

            try {
                final PackageItem item = new PackageItem(
                        info.activityInfo.loadLabel(mPm), 0, componentName);
                mPackageInfoList.add(item);
            } catch (Exception e) {
                if (DEBUG) Log.e(TAG, "Load installed apps", e);
            }
        }
        Collections.sort(mPackageInfoList);

        setPositiveButtonText(R.string.action_save);
        setNegativeButtonText(android.R.string.cancel);
    }

    public void setValues(Collection<String> values) {
        mValues.clear();
        mValues.addAll(values);
    }

    public Collection<String> getValues() {
        return mValues;
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        mAdapter = new AppListAdapter(getContext());
        final ListView listView = (ListView) view.findViewById(R.id.app_list);
        listView.setAdapter(mAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final AppViewHolder holder = (AppViewHolder) view.getTag();
                final boolean isChecked = !holder.checkBox.isChecked();

                holder.checkBox.setChecked(isChecked);
                PackageItem info = mAdapter.getItem(position);

                if (isChecked) {
                    mValues.add(info.mValue);
                } else {
                    mValues.remove(info.mValue);
                }
            }
        });
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (positiveResult) {
            callChangeListener(mValues.size() > 0 ? mValues : null);
        }
    }

    public class PackageItem implements Comparable<PackageItem> {
        public final CharSequence mTitle;
        public final int mAppIconResourceId;
        public final ComponentName mComponentName;
        public final String mValue;

        PackageItem(CharSequence title, int iconResourceId, ComponentName componentName) {
            mTitle = title;
            mAppIconResourceId = iconResourceId;
            mComponentName = componentName;
            mValue = componentName.flattenToString();
        }

        PackageItem(CharSequence title, int iconResourceId, String value) {
            mTitle = title;
            mAppIconResourceId = iconResourceId;
            mComponentName = null;
            mValue = value;
        }

        @Override
        public int compareTo(PackageItem another) {
            return mTitle.toString().toUpperCase().compareTo(another.mTitle.toString().toUpperCase());
        }

        @Override
        public int hashCode() {
            return mValue.hashCode();
        }

        @Override
        public boolean equals(Object another) {
            if (another == null || !(another instanceof PackageItem)) {
                return false;
            }
            return mValue.equals(((PackageItem) another).mValue);
        }
    }

    public class AppListAdapter extends ArrayAdapter<PackageItem> {
        private final LayoutInflater mInflater;

        public AppListAdapter(Context context) {
            super(context, 0);
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            addAll(mPackageInfoList);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            AppViewHolder holder = AppViewHolder.createOrRecycle(mInflater, convertView);
            convertView = holder.rootView;
            PackageItem info = getItem(position);
            holder.appName.setText(info.mTitle);
            if (info.mAppIconResourceId != 0) {
                holder.appIcon.setImageResource(info.mAppIconResourceId);
            } else {
                Drawable d = resolveAppIcon(info);
                holder.appIcon.setImageDrawable(d);
            }
            holder.checkBox.setChecked(mValues.contains(info.mValue));
            return convertView;
        }

        @Override
        public PackageItem getItem(int position) {
            return mPackageInfoList.get(position);
        }
    }

    public static class AppViewHolder {
        public View rootView;
        public TextView appName;
        public ImageView appIcon;
        public CheckBox checkBox;

        public static AppViewHolder createOrRecycle(LayoutInflater inflater, View convertView) {
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.app_select_item, null);

                // Creates a ViewHolder and store references to the two children views
                // we want to bind data to.
                AppViewHolder holder = new AppViewHolder();
                holder.rootView = convertView;
                holder.appName = (TextView) convertView.findViewById(R.id.app_name);
                holder.appIcon = (ImageView) convertView.findViewById(R.id.app_icon);
                holder.checkBox = (CheckBox) convertView.findViewById(android.R.id.checkbox);
                convertView.setTag(holder);
                return holder;
            } else {
                // Get the ViewHolder back to get fast access to the TextView
                // and the ImageView.
                return (AppViewHolder) convertView.getTag();
            }
        }
    }

    private Drawable getDefaultActivityIcon() {
        return getContext().getResources().getDrawable(android.R.drawable.sym_def_app_icon);
    }

    private Drawable resolveAppIcon(PackageItem item) {
        Drawable appIcon = null;
        try {
            appIcon = mPm.getActivityIcon(item.mComponentName);
        } catch (PackageManager.NameNotFoundException e) {
            if (DEBUG) Log.e(TAG, "resolveAppIcon", e);
        }
        if (appIcon == null) {
            appIcon = getDefaultActivityIcon();
        }
        return appIcon;
    }
}



