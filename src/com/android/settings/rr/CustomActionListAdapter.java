/*
 * Copyright (C) 2015 TeamEos project
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
 * Adapter for displaying custom actions in a list
 */

package com.android.settings.rr;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.internal.utils.du.ActionHandler;
import com.android.internal.utils.du.Config.ActionConfig;
import com.android.settings.R;

public class CustomActionListAdapter extends BaseAdapter {
    private LayoutInflater mInflater;
    private Context mContext;
    private List<ActionConfig> mCustomActions = new ArrayList<ActionConfig>();

    public CustomActionListAdapter(Context context) {
        mContext = context;
        mInflater = LayoutInflater.from(context);
        reloadActions();
    }

    private void reloadActions() {
        mCustomActions.clear();
        mCustomActions.addAll(ActionHandler.getSystemActions(mContext));
        notifyDataSetChanged();
    }

    public void removeAction(String action) {
        int index = -1;
        for (int i = 0; i < mCustomActions.size(); i++) {
            if (TextUtils.equals(mCustomActions.get(i).getAction(), action)) {
                index = i;
                break;
            }
        }
        if (index > -1) {
            mCustomActions.remove(index);
            notifyDataSetChanged();
        }
    }

    @Override
    public int getCount() {
        return mCustomActions.size();
    }

    @Override
    public ActionConfig getItem(int position) {
        return mCustomActions.get(position);
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
            convertView = mInflater.inflate(R.layout.preference_icon, null, false);
            holder = new ViewHolder();
            convertView.setTag(holder);
            holder.title = (TextView) convertView.findViewById(com.android.internal.R.id.title);
            holder.summary = (TextView) convertView
                    .findViewById(com.android.internal.R.id.summary);
            holder.icon = (ImageView) convertView.findViewById(R.id.icon);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) holder.icon.getLayoutParams();
            params.width = holder.icon.getMaxWidth();
            params.height = holder.icon.getMaxHeight();
            holder.icon.setLayoutParams(params);
            holder.icon.setScaleType(ScaleType.CENTER);
            holder.icon.setCropToPadding(true);
        }

        ActionConfig config = getItem(position);
        holder.title.setText(config.getLabel());
        holder.icon.setBackgroundResource(R.drawable.fab_accent);
        holder.icon.setImageDrawable(config.getDefaultIcon(ctx));
        holder.summary.setVisibility(View.GONE);

        return convertView;

    }

    private static class ViewHolder {
        TextView title;
        TextView summary;
        ImageView icon;
    }
}
