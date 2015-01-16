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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.settings.R;

import java.util.List;

public class QSListAdapter extends ArrayAdapter<QSTileHolder> {
    private LayoutInflater mInflater;
    public QSListAdapter(Context context, List<QSTileHolder> objects) {
        super(context, 0, objects);
        mInflater = LayoutInflater.from(context);
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
        if (item.drawableId != -1) {
            holder.icon.setImageResource(item.drawableId);
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
