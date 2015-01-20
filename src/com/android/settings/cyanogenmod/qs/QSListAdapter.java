/*
 * Copyright (C) 2015 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.settings.cyanogenmod.qs;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.Utils;

import java.util.List;

public class QSListAdapter extends ArrayAdapter<QSTileHolder> {
    private Resources mSystemUIResources;
    private LayoutInflater mInflater;
    public QSListAdapter(Context context, List<QSTileHolder> objects) {
        super(context, 0, objects);
        mInflater = LayoutInflater.from(context);
        try {
            Context sysUIContext = context.createPackageContext(Utils.SYSTEM_UI_PACKAGE_NAME, 0);
            mSystemUIResources = sysUIContext.getResources();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final Holder holder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.add_qs_list_item, parent, false);
            holder = new Holder();
            holder.entry = (TextView) convertView.findViewById(android.R.id.text1);
            holder.icon = (ImageView) convertView.findViewById(android.R.id.icon);
            convertView.setTag(holder);
        } else {
            holder = (Holder) convertView.getTag();
        }

        QSTileHolder item = getItem(position);

        holder.entry.setText(item.name);
        if (item.resourceName != null) {
            Drawable d = Utils.getNamedDrawableFromSystemUI(mSystemUIResources, item.resourceName);
            d.setColorFilter(getContext().getResources().getColor(R.color.qs_tile_tint_color),
                    PorterDuff.Mode.SRC_ATOP);
            holder.icon.setImageDrawable(d);
            holder.icon.setVisibility(View.VISIBLE);
        } else {
            holder.icon.setVisibility(View.GONE);
        }

        return convertView;
    }

    private static class Holder {
        public TextView entry;
        public ImageView icon;
    }
}
