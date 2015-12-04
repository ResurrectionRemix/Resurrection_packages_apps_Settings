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
package com.android.settings.preference;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.AttributeSet;
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
import com.android.settings.CustomDialogPreference;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AppMultiSelectListPreference extends CustomDialogPreference {
    private final List<MyApplicationInfo> mPackageInfoList = new ArrayList<MyApplicationInfo>();
    private AppListAdapter mAdapter;
    private Set<String> mValues = new HashSet<String>();

    public AppMultiSelectListPreference(Context context) {
        this(context, null);
    }

    public AppMultiSelectListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        setDialogLayoutResource(R.layout.preference_app_list);

        final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> installedAppsInfo = getContext().getPackageManager().queryIntentActivities(
                mainIntent, 0);

        for (ResolveInfo info : installedAppsInfo) {
            MyApplicationInfo myInfo = new MyApplicationInfo();
            myInfo.resolveInfo = info;
            myInfo.label = getResolveInfoTitle(info);
            mPackageInfoList.add(myInfo);
        }
        Collections.sort(mPackageInfoList, sDisplayNameComparator);
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

                MyApplicationInfo myInfo = mAdapter.getItem(position);
                ResolveInfo info = myInfo.resolveInfo;
                Intent intent = getIntentForResolveInfo(info, Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_LAUNCHER);

                String value = intent.toUri(0).toString();
                if (isChecked) {
                    mValues.add(value);
                } else {
                    mValues.remove(value);
                }
            }
        });
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        callChangeListener(null);
    }

    private String getResolveInfoTitle(ResolveInfo info) {
        CharSequence label = info.loadLabel(getContext().getPackageManager());
        if (label == null) label = info.activityInfo.name;
        return label != null ? label.toString() : null;
    }

    private Intent getIntentForResolveInfo(ResolveInfo info, String action) {
        Intent intent = new Intent(action);
        ActivityInfo ai = info.activityInfo;
        intent.setClassName(ai.packageName, ai.name);
        return intent;
    }

    class MyApplicationInfo {
        ApplicationInfo info;
        CharSequence label;
        ResolveInfo resolveInfo;
    }

    public class AppListAdapter extends ArrayAdapter<MyApplicationInfo> {
        private final LayoutInflater mInflater;

        public AppListAdapter(Context context) {
            super(context, 0);
            mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            addAll(mPackageInfoList);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // A ViewHolder keeps references to children views to avoid unnecessary calls
            // to findViewById() on each row.
            AppViewHolder holder = AppViewHolder.createOrRecycle(mInflater, convertView);
            convertView = holder.rootView;
            MyApplicationInfo info = getItem(position);
            holder.appName.setText(info.label);
            Drawable icon = info.resolveInfo.loadIcon(getContext().getPackageManager());
            if (icon != null) {
                holder.appIcon.setImageDrawable(icon);
            } else {
                holder.appIcon.setImageDrawable(null);
            }

            Intent intent = getIntentForResolveInfo(info.resolveInfo, Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            String value = intent.toUri(0).toString();

            holder.checkBox.setChecked(mValues.contains(value));
            return convertView;
        }

        @Override
        public MyApplicationInfo getItem(int position) {
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
                return (AppViewHolder)convertView.getTag();
            }
        }
    }

    private final static Comparator<MyApplicationInfo> sDisplayNameComparator
            = new Comparator<MyApplicationInfo>() {

        private final Collator collator = Collator.getInstance();

        public final int compare(MyApplicationInfo a, MyApplicationInfo b) {
            return collator.compare(a.label, b.label);
        }
    };
}

