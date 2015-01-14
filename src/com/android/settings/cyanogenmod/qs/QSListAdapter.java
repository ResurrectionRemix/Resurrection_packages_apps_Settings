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
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.settings.R;

import java.util.List;

public class QSListAdapter extends ArrayAdapter<QSTileHolder> {

    public QSListAdapter(Context context, int textViewResourceId, List<QSTileHolder> objects) {
        super(context, textViewResourceId, objects);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Holder holder = null;
        if (convertView == null) {
            convertView = View.inflate(getContext(), R.layout.add_qs_list_item, null);
            holder = new Holder();
            holder.entry = (TextView) convertView.findViewById(android.R.id.text1);
            holder.icon = (ImageView) convertView.findViewById(android.R.id.icon);
            convertView.setTag(holder);
        } else {
            holder = (Holder) convertView.getTag();
        }

        holder.entry.setText(getItem(position).getName());
        Drawable icon = null;
        if (getItem(position).getDrawableId() != -1) {
            icon = getContext().getResources().getDrawable(getItem(position).getDrawableId());
        }
        holder.icon.setImageDrawable(icon);

        return convertView;
    }

    private static class Holder {
        public TextView entry;
        public ImageView icon;
    }
}
