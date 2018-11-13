/*
 * Copyright (C) 2018 The Dirty Unicorns Project
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
 *
 * Adapter for displaying a list of custom fonts with font Typeface
 */
 package com.android.settings.display;
 import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
 import com.android.settings.R;
 import android.content.Context;
import android.content.FontInfo;
import android.content.IFontService;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
 public class FontListAdapter extends BaseAdapter {
    private static final String TAG = "FontListAdapter";
     private IFontService mFontService;
    private List<FontInfo> mFontInfo = new ArrayList<FontInfo>();
    private Context mContext;
    private LayoutInflater mInflater;
    private PackageManager mPm;
     public FontListAdapter(Context context) {
        mContext = context;
        mInflater = LayoutInflater.from(context);
        mFontService = IFontService.Stub.asInterface(
                ServiceManager.getService("dufont"));
        mPm = context.getPackageManager();
        loadFontList();
    }
     public void loadFontList() {
        mFontInfo.clear();
        try {
            Map<String, List<FontInfo>> fontMap = mFontService.getAllFonts();
            for (Map.Entry<String, List<FontInfo>> entry : fontMap.entrySet()) {
                String packageName = entry.getKey();
                List<FontInfo> fonts = entry.getValue();
                // manually add system font after we sort
                if (TextUtils.equals(packageName, FontInfo.DEFAULT_FONT_PACKAGE)) {
                    continue;
                }
                for (FontInfo font : fonts) {
                    mFontInfo.add(new FontInfo(font));
                }
            }
            Collections.sort(mFontInfo);
        } catch (RemoteException e) {
            Log.e(TAG, "Error in populating list");
        }
        mFontInfo.add(0, FontInfo.getDefaultFontInfo());
        notifyDataSetChanged();
    }
     @Override
    public int getCount() {
        return mFontInfo.size();
    }
     @Override
    public FontInfo getItem(int position) {
        return mFontInfo.get(position);
    }
     @Override
    public long getItemId(int position) {
        return 0;
    }
     @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        final Context ctx = mContext;
        if (convertView != null) {
            holder = (ViewHolder) convertView.getTag();
        } else {
            convertView = mInflater.inflate(R.layout.font_item, null, false);
            holder = new ViewHolder();
            convertView.setTag(holder);
            holder.title = (TextView) convertView.findViewById(com.android.internal.R.id.title);
            holder.summary = (TextView) convertView
                    .findViewById(com.android.internal.R.id.summary);
            holder.icon = (ImageView) convertView.findViewById(R.id.icon);
        }
        FontInfo info = getItem(position);
        Typeface.Builder builder = new Typeface.Builder(info.previewPath);
        Typeface tf = builder.build();
        holder.title.setTypeface(tf);
        holder.title.setText(info.fontName.replace("_", " "));
        holder.summary.setTypeface(tf);
        holder.summary.setText(getPackageLabel(info.packageName).replace("_", " "));
        holder.icon.setImageDrawable(getPackageDrawable(info.packageName));
        return convertView;
    }
     private String getPackageLabel(String packageName) {
        String label = null;
        if (packageName.equals("android")) {
            return "Android";
        }
        try {
            ApplicationInfo info = mPm.getApplicationInfo(packageName, 0);
            label = (String) info.loadLabel(mPm);
        } finally {
            return label;
        }
    }
     private Drawable getPackageDrawable(String packageName) {
        Drawable icon = null;
        try {
            ApplicationInfo info = mPm.getApplicationInfo(packageName, 0);
            icon = info.loadIcon(mPm);
        } finally {
            return icon;
        }
    }
     private static class ViewHolder {
        TextView title;
        TextView summary;
        ImageView icon;
    }
}
