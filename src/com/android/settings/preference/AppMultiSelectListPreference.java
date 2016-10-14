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
    private CharSequence[] mEntries;
	private CharSequence[] mEntryValues;
    private Set<String> mValues = new HashSet<String>();
	private Set<String> mNewValues = new HashSet<String>();
    private boolean mPreferenceChanged;

    public AppMultiSelectListPreference(Context context) {
        this(context, null);
    }

    public AppMultiSelectListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        setDialogLayoutResource(R.layout.preference_app_list);

        final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ApplicationInfo> pkgs = context.getPackageManager()
                .getInstalledApplications(PackageManager.PERMISSION_GRANTED);
		for (int i=0; i<pkgs.size(); i++) {
		ApplicationInfo ai = pkgs.get(i);
            if(context.getPackageManager().getLaunchIntentForPackage(ai.packageName) == null) {
                continue;
		}

            MyApplicationInfo info = new MyApplicationInfo();
            info.info = ai;
            info.label = info.info.loadLabel(getContext().getPackageManager()).toString();
			mPackageInfoList.add(info);

		}
        List<CharSequence> entries = new ArrayList<CharSequence>();
		List<CharSequence> entryValues = new ArrayList<CharSequence>();
        Collections.sort(mPackageInfoList, sDisplayNameComparator);
        for (MyApplicationInfo info : mPackageInfoList) {
            entries.add(info.label);
            entryValues.add(info.info.packageName);
        }
        MyApplicationInfo info = new MyApplicationInfo();
        mEntries = new CharSequence[entries.size()];
        mEntryValues = new CharSequence[entries.size()];
        entries.toArray(mEntries);
		entryValues.toArray(mEntryValues);
    }

    public void setValues(Collection<String> values) {
        mValues.clear();
        mValues.addAll(values);
    }


    public void setClearValues() {
        mValues.clear();
	}

    public Set<String> getValues() {
        return mValues;
    }


    /**
     * Returns the index of the given value (in the entry values array).
     *
     * @param value The value whose index should be returned.
     * @return The index of the value, or -1 if not found.
     */
    public int findIndexOfValue(String value) {
        if (value != null && mEntryValues != null) {
            for (int i = mEntryValues.length - 1; i >= 0; i--) {
                if (mEntryValues[i].equals(value)) {
                    return i;
                }
            }
        }
        return -1;
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
                if (isChecked) {
                    mPreferenceChanged |= mNewValues.add(mEntryValues[position].toString());
                } else {
                    mPreferenceChanged |= mNewValues.remove(mEntryValues[position].toString());
                }
            }
        });
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (positiveResult && mPreferenceChanged) {
            final Set<String> values = mNewValues;
            if (callChangeListener(values)) {
                setValues(values);
            }
        }
		mPreferenceChanged = false;
    }


    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        final CharSequence[] defaultValues = a.getTextArray(index);
        final int valueCount = defaultValues.length;
        final Set<String> result = new HashSet<String>();

        for (int i = 0; i < valueCount; i++) {
            result.add(defaultValues[i].toString());
        }

        return result;
    }


    /*private String getResolveInfoTitle(ResolveInfo info) {
        CharSequence label = info.loadLabel(getContext().getPackageManager());
        if (label == null) label = info.activityInfo.name;
        return label != null ? label.toString() : null;
    }

    private Intent getIntentForResolveInfo(ResolveInfo info, String action) {
        Intent intent = new Intent(action);
        ActivityInfo ai = info.activityInfo;
        intent.setClassName(ai.packageName, ai.name);
        return intent;
    }*/

    class MyApplicationInfo {
        ApplicationInfo info;
        CharSequence label;
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
            if (info.info != null) {
                holder.appIcon.setImageDrawable(info.info.loadIcon(getContext().getPackageManager()));
            } else {
                holder.appIcon.setImageDrawable(null);
            }
            holder.checkBox.setChecked(mNewValues.contains(mEntryValues[position].toString()));
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


