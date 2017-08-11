/*
 * Copyright (C) 2014-2016 The CyanogenMod Project
 * Copyright (C) 2017 The LineageOS Project
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

package com.android.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class CarrierSelection extends Activity {
    public static final String EXTRA_SUB_ID = "sub_id";
    public static final int INVALID_SUB_ID = -1000;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        final SubscriptionManager subscriptionManager = SubscriptionManager.from(this);
        final List<SubscriptionInfo> subInfoList =
                subscriptionManager.getActiveSubscriptionInfoList();
        if (subInfoList == null) {
            startNetworkSelection(INVALID_SUB_ID);
        } else if (subInfoList.size() == 1) {
            startNetworkSelection(subInfoList.get(0).getSubscriptionId());
        } else {
            createDialog(this, subInfoList).show();
        }
    }

    public Dialog createDialog(final Context context, final List<SubscriptionInfo> subInfoList) {
        final ArrayList<String> list = new ArrayList<String>();
        final int selectableSubInfoLength = subInfoList == null ? 0 : subInfoList.size();

        final DialogInterface.OnClickListener selectionListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int value) {
                        final SubscriptionInfo sir = subInfoList.get(value);
                        startNetworkSelection(sir.getSubscriptionId());
                    }
                };

        Dialog.OnKeyListener keyListener = new Dialog.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface arg0, int keyCode,
                                 KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    finish();
                }
                return true;
            }
        };

        for (int i = 0; i < selectableSubInfoLength; ++i) {
            final SubscriptionInfo sir = subInfoList.get(i);
            CharSequence displayName = sir.getDisplayName();
            if (displayName == null) {
                displayName = "";
            }
            list.add(displayName.toString());
        }

        String[] arr = list.toArray(new String[0]);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        ListAdapter adapter = new CarrierSelection.SelectAccountListAdapter(
                subInfoList,
                builder.getContext(),
                R.layout.select_account_list_item,
                arr);
        builder.setTitle(R.string.sim_select_card);

        Dialog dialog = builder.setAdapter(adapter, selectionListener).create();
        dialog.setOnKeyListener(keyListener);

        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                finish();
            }
        });

        return dialog;
    }

    private void startNetworkSelection(int subId) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setComponent(new ComponentName("com.android.phone",
                "com.android.phone.NetworkSetting"));
        if (subId != INVALID_SUB_ID) {
            intent.putExtra(EXTRA_SUB_ID, subId);
        }
        startActivity(intent);
        finish();
    }

    private class SelectAccountListAdapter extends ArrayAdapter<String> {
        private Context mContext;
        private int mResId;
        private final float OPACITY = 0.54f;
        private List<SubscriptionInfo> mSubInfoList;

        public SelectAccountListAdapter(List<SubscriptionInfo> subInfoList,
                                        Context context, int resource, String[] arr) {
            super(context, resource, arr);
            mContext = context;
            mResId = resource;
            mSubInfoList = subInfoList;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater)
                    mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView;
            final CarrierSelection.SelectAccountListAdapter.ViewHolder holder;

            if (convertView == null) {
                // Cache views for faster scrolling
                rowView = inflater.inflate(mResId, null);
                holder = new CarrierSelection.SelectAccountListAdapter.ViewHolder();
                holder.title = (TextView) rowView.findViewById(R.id.title);
                holder.summary = (TextView) rowView.findViewById(R.id.summary);
                holder.icon = (ImageView) rowView.findViewById(R.id.icon);
                rowView.setTag(holder);
            } else {
                rowView = convertView;
                holder = (CarrierSelection.SelectAccountListAdapter.ViewHolder) rowView.getTag();
            }

            final SubscriptionInfo sir = mSubInfoList.get(position);
            if (sir == null) {
                holder.title.setText(getItem(position));
                holder.summary.setText("");
                holder.icon.setImageDrawable(getResources()
                        .getDrawable(R.drawable.ic_live_help));
                holder.icon.setAlpha(OPACITY);
            } else {
                holder.title.setText(sir.getDisplayName());
                holder.summary.setText(sir.getNumber());
                holder.icon.setImageBitmap(sir.createIconBitmap(mContext));
            }
            return rowView;
        }

        private class ViewHolder {
            TextView title;
            TextView summary;
            ImageView icon;
        }
    }
}
