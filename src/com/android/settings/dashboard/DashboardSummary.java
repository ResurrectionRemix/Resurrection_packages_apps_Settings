/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.settings.dashboard;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.PorterDuff.Mode;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.HelpUtils;
import com.android.settings.InstrumentedFragment;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.Utils;
import com.android.settings.widget.SwitchBar;

import java.util.List;

public class DashboardSummary extends InstrumentedFragment {
    private static final String LOG_TAG = "DashboardSummary";

    private LayoutInflater mLayoutInflater;
    private ViewGroup mDashboard;
    private boolean mCustomColors;
    private int mTextcolor;
    private int mIconColor;
    
    private static final int MSG_REBUILD_UI = 1;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_REBUILD_UI: {
                    final Context context = getActivity();
                    rebuildUI(context);
                } break;
            }
        }
    };

    private class HomePackageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            rebuildUI(context);
        }
    }
    private HomePackageReceiver mHomePackageReceiver = new HomePackageReceiver();

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.DASHBOARD_SUMMARY;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        HelpUtils.prepareHelpMenuItem(getActivity(), menu, R.string.help_uri_dashboard,
                getClass().getName());
    }

    @Override
    public void onResume() {
        super.onResume();

        sendRebuildUI();

        final IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        filter.addDataScheme("package");
        getActivity().registerReceiver(mHomePackageReceiver, filter);

        final IntentFilter airplaneModeFilter
                = new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        getActivity().registerReceiver(mHomePackageReceiver, airplaneModeFilter);
    }

    @Override
    public void onPause() {
        super.onPause();

        getActivity().unregisterReceiver(mHomePackageReceiver);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mLayoutInflater = inflater;

        final View rootView = inflater.inflate(R.layout.dashboard, container, false);
        mDashboard = (ViewGroup) rootView.findViewById(R.id.dashboard_container);

        return rootView;
    }

    private void rebuildUI(Context context) {
        if (!isAdded()) {
            Log.w(LOG_TAG, "Cannot build the DashboardSummary UI yet as the Fragment is not added");
            return;
        }
	mCustomColors = Settings.System.getInt(context.getContentResolver(),
                Settings.System.DASHBOARD_CUSTOM_COLORS, 0) == 1;
        long start = System.currentTimeMillis();
        final Resources res = getResources();

        mDashboard.removeAllViews();

        List<DashboardCategory> categories =
                ((SettingsActivity) context).getDashboardCategories(true);

        final int count = categories.size();

        for (int n = 0; n < count; n++) {
            DashboardCategory category = categories.get(n);

            View categoryView = mLayoutInflater.inflate(R.layout.dashboard_category, mDashboard,
                    false);
                           

            TextView categoryLabel = (TextView) categoryView.findViewById(R.id.category_title);
            categoryLabel.setText(category.getTitle(res));
            
            if(mCustomColors){        
            categoryView.setBackgroundResource(R.drawable.dashboard_tile_background);
            categoryView.setBackgroundColor(Settings.System.getInt(context.getContentResolver(),
                    Settings.System.SETTINGS_BG_COLOR, 0xff000000)); 
            categoryLabel.setTextColor(Settings.System.getInt(context.getContentResolver(),
                    Settings.System.SETTINGS_CATEGORY_TEXT_COLOR, 0xff1976D2));
            categoryLabel.setTextSize(Settings.System.getIntForUser(context.getContentResolver(),
                    Settings.System.SETTINGS_CATEGORY_TEXT_SIZE, 14,
                    UserHandle.USER_CURRENT));
            } 

            ViewGroup categoryContent =
                    (ViewGroup) categoryView.findViewById(R.id.category_content);

            final int tilesCount = category.getTilesCount();
            for (int i = 0; i < tilesCount; i++) {
                DashboardTile tile = category.getTile(i);

                DashboardTileView tileView = new DashboardTileView(context);
                updateTileView(context, res, tile, tileView.getImageView(),
                        tileView.getTitleTextView(), tileView.getStatusTextView(),
                        tileView.getSwitchView());

                tileView.setTile(tile);

                if (tile.id == R.id.mobile_networks) {
                    tileView.setEnabledTile(!Utils.isAirplaneModeEnabled(context));
                }
                categoryContent.addView(tileView);
            }

            // Add the category
            mDashboard.addView(categoryView);
        }
        long delta = System.currentTimeMillis() - start;
        Log.d(LOG_TAG, "rebuildUI took: " + delta + " ms");
    }

    public void updateTileView(Context context, Resources res, DashboardTile tile,
            ImageView tileIcon, TextView tileTextView, TextView statusTextView, Switch switchBar) {

        if (!TextUtils.isEmpty(tile.iconPkg)) {
            try {
                Drawable drawable = context.getPackageManager()
                        .getResourcesForApplication(tile.iconPkg).getDrawable(tile.iconRes, null);
                if (!tile.iconPkg.equals(context.getPackageName()) && drawable != null) {
                    // If this drawable is coming from outside Settings, tint it to match the color.
                    TypedValue tintColorValue = new TypedValue();
                    context.getResources().getValue(R.color.external_tile_icon_tint_color,
                            tintColorValue, true);
                    // If tintColorValue is TYPE_ATTRIBUTE, resolve it
                    if (tintColorValue.type == TypedValue.TYPE_ATTRIBUTE) {
                        context.getTheme().resolveAttribute(tintColorValue.data,
                                tintColorValue, true);
                    }
                    drawable.setTintMode(android.graphics.PorterDuff.Mode.SRC_ATOP);
                    drawable.setTint(tintColorValue.data);
                }
                tileIcon.setImageDrawable(drawable);
            } catch (NameNotFoundException | Resources.NotFoundException e) {
                tileIcon.setImageDrawable(null);
                tileIcon.setBackground(null);
            }
        } else if (tile.iconRes > 0) {
            tileIcon.setImageResource(tile.iconRes);
        } else {
            tileIcon.setImageDrawable(null);
            tileIcon.setBackground(null);
        }

        tileTextView.setText(tile.getTitle(res));

        CharSequence summary = tile.getSummary(res);
        if (!TextUtils.isEmpty(summary)) {
            statusTextView.setVisibility(View.VISIBLE);
            statusTextView.setText(summary);
        } else {
            statusTextView.setVisibility(View.GONE);
        }

        if (tile.switchControl != null) {
	    boolean isPrimary = UserHandle.getCallingUserId() == UserHandle.USER_OWNER;
        int dashboardSwitches = isPrimary ? getDashboardSwitches(context) : 0;

        if (dashboardSwitches == 0) {
            switchBar.setVisibility(View.GONE);
        }
        if (dashboardSwitches == 1) {
            switchBar.setVisibility(View.VISIBLE);
	  }
        } else {
            // do nothing
        }
        setcolors(tileIcon,context,tile,tileTextView, statusTextView, switchBar);
    }

    private static int getDashboardSwitches(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.DASHBOARD_SWITCHES, 0);
    }

    private void sendRebuildUI() {
        if (!mHandler.hasMessages(MSG_REBUILD_UI)) {
            mHandler.sendEmptyMessage(MSG_REBUILD_UI);
        }
    }
    
      public void setcolors( ImageView tileIcon, Context context ,DashboardTile tile,TextView tileTextView , TextView statusTextView, Switch switchBar) {
        mCustomColors = Settings.System.getInt(context.getContentResolver(),
                Settings.System.DASHBOARD_CUSTOM_COLORS, 0) == 1;
        mIconColor = Settings.System.getInt(context.getContentResolver(),
                Settings.System.DB_ICON_COLOR, 0xFFFFFFFF);         
	mTextcolor = Settings.System.getInt(context.getContentResolver(),
                Settings.System.DB_TEXT_COLOR, 0xFFFFFFFF); 
        if (mCustomColors) {
		if (tileTextView  !=null) {
		tileTextView.setTextColor(mTextcolor);      
		}		
		if (tileIcon != null) {
		tileIcon.setColorFilter(mIconColor, Mode.SRC_ATOP);		
		}
        }
    } 
}
