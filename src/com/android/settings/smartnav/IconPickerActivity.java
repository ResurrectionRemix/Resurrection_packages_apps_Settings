/*
 * Copyright (C) 2016 The DirtyUnicorns Project
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
 * Display installed icons packs that we are able to parse and get an icon from
 */

package com.android.settings.smartnav;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.android.internal.utils.ActionHandler;
import com.android.internal.utils.ActionUtils;
import com.android.settings.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class IconPickerActivity extends Activity implements DialogInterface.OnCancelListener {
    private static final int DIALOG_ICON_PACK = 1;
    private static final int ICON_PACK_ICON_RESULT = 69;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createDialog(this, DIALOG_ICON_PACK).show();
    }

    public Dialog createDialog(final Context context, final int id) {
        Dialog dialog;
        switch (id) {
            case DIALOG_ICON_PACK:
                dialog = getIconPackDialog(context);
                break;
            default:
                dialog = getIconPackDialog(context);
        }
        return dialog;

    }

    private Dialog getIconPackDialog(Context context) {
        final LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View view = inflater.inflate(R.layout.dialog_iconpack, null);
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final IconPackageAdapter adapter = new IconPackageAdapter(this);
        final ListView listView = (ListView) view.findViewById(R.id.iconpack_list);
        final Dialog dialog;

        adapter.load();
        dialog = builder.setTitle(getString(R.string.icon_pack_picker_dialog_title))
                .setView(view)
                .setOnCancelListener(this)
                .setNegativeButton(android.R.string.cancel, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        onCancel(dialog);
                    }
                })
                .create();

        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {
                ResolveInfo info = adapter.getItem(position);
                String packageName = info.activityInfo.packageName;
                Intent intent = new Intent();
                intent.setClassName(ActionUtils.PACKAGE_SETTINGS,
                        ActionUtils.CLASS_NAME_ICON_PICKER_GRID_ACTIVITY);
                intent.putExtra(ActionUtils.INTENT_EXTRA_ICON_PACKAGE_NAME, packageName);
                dialog.dismiss();
                startActivityForResult(intent, ICON_PACK_ICON_RESULT);
            }
        });
        return dialog;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        dialog.dismiss();
        sendCancelResultAndFinish();
    }

    private void sendCancelResultAndFinish() {
        Intent intent = new Intent(ActionUtils.INTENT_ICON_PICKER);
        intent.putExtra(ActionUtils.INTENT_EXTRA_RESULT, Activity.RESULT_CANCELED);
        ActionHandler.dispatchNavigationEditorResult(intent);
        setResult(Activity.RESULT_CANCELED);
        finish();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ICON_PACK_ICON_RESULT) {
            if (resultCode == RESULT_OK) {
                String iconType = data.getStringExtra(ActionUtils.INTENT_EXTRA_ICON_DATA_TYPE);
                String iconPackage = data.getStringExtra(ActionUtils.INTENT_EXTRA_ICON_DATA_PACKAGE);
                String iconName = data.getStringExtra(ActionUtils.INTENT_EXTRA_ICON_DATA_NAME);
                Intent resultIntent = new Intent();
                resultIntent.setAction(ActionUtils.INTENT_ICON_PICKER);
                resultIntent.putExtra(ActionUtils.INTENT_EXTRA_RESULT, Activity.RESULT_OK);
                resultIntent.putExtra(ActionUtils.INTENT_EXTRA_ICON_DATA_TYPE, iconType);
                resultIntent.putExtra(ActionUtils.INTENT_EXTRA_ICON_DATA_PACKAGE, iconPackage);
                resultIntent.putExtra(ActionUtils.INTENT_EXTRA_ICON_DATA_NAME, iconName);
                ActionHandler.dispatchNavigationEditorResult(resultIntent);
                setResult(RESULT_OK, resultIntent);
                finish();
            } else {
                sendCancelResultAndFinish();
            }
        }
    }

    private class IconPackageAdapter extends BaseAdapter {
        private LayoutInflater mInflater;
        private PackageManager mPM;
        List<ResolveInfo> mIconPackage = new ArrayList<ResolveInfo>();

        public IconPackageAdapter(Context ctx) {
            mInflater = LayoutInflater.from(ctx);
            mPM = ctx.getPackageManager();
        }

        public void load() {
            List<ResolveInfo> tmpList = new ArrayList<ResolveInfo>();
            Intent i = new Intent("org.adw.launcher.THEMES");
            i.addCategory(Intent.CATEGORY_DEFAULT);
            tmpList.addAll(mPM.queryIntentActivities(i,
                    PackageManager.GET_META_DATA));

            mIconPackage.clear();
            mIconPackage.addAll(tmpList);

            List<ResolveInfo> toRemove = new ArrayList<ResolveInfo>();
            for (ResolveInfo info : mIconPackage) {
                String packageName = info.activityInfo.packageName;
                Resources packageRes;
                try {
                    packageRes = mPM.getResourcesForApplication(packageName);
                    InputStream appfilterstream = packageRes.getAssets().open("drawable.xml");
                    appfilterstream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    toRemove.add(info);
                }
            }
            mIconPackage.removeAll(toRemove);

            Collections.sort(mIconPackage, new Comparator<ResolveInfo>() {
                @Override
                public int compare(ResolveInfo lhs, ResolveInfo rhs) {
                    return (lhs.loadLabel(mPM).toString()).compareTo(rhs.loadLabel(mPM).toString());
                }

            });
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mIconPackage.size();
        }

        @Override
        public ResolveInfo getItem(int position) {
            return mIconPackage.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView != null) {
                holder = (ViewHolder) convertView.getTag();
            } else {
                convertView = mInflater.inflate(R.layout.iconpack_view, null, false);
                holder = new ViewHolder();
                convertView.setTag(holder);
                holder.title = (TextView) convertView.findViewById(R.id.title);
                holder.icon = (ImageView) convertView.findViewById(R.id.icon);
            }

            ResolveInfo info = (ResolveInfo) getItem(position);
            holder.title.setText(info.loadLabel(mPM).toString());
            holder.icon.setImageDrawable(info.loadIcon(mPM));

            return convertView;
        }
    }

    private static class ViewHolder {
        TextView title;
        ImageView icon;
    }
}
