/**
 * Copyright (C) 2013 The Android Open Source Project
 * Copyright (C) 2016 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.android.settings.applications;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceFrameLayout;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.util.Pair;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.DevelopmentSettings;
import com.android.settings.InstrumentedFragment;
import com.android.settings.R;

public class AppOpsSummary extends InstrumentedFragment {
    // layout inflater object used to inflate views
    private LayoutInflater mInflater;

    private ViewGroup mContentContainer;
    private View mRootView;
    private ViewPager mViewPager;

    private MyPagerAdapter mAdapter;

    private Activity mActivity;
    private SharedPreferences mPreferences;

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.APP_OPS_SUMMARY;
    }

    static class MyPagerAdapter extends FragmentPagerAdapter
            implements ViewPager.OnPageChangeListener {
        private List<Pair<CharSequence, AppOpsState.OpsTemplate>> mPageData;
        private int mCurPos;

        public MyPagerAdapter(FragmentManager fm,
                List<Pair<CharSequence, AppOpsState.OpsTemplate>> data) {
            super(fm);
            mPageData = data;
        }

        @Override
        public Fragment getItem(int position) {
            return new AppOpsCategory(mPageData.get(position).second);
        }

        @Override
        public int getCount() {
            return mPageData.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mPageData.get(position).first;
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
            mCurPos = position;
        }

        public int getCurrentPage() {
            return mCurPos;
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            if (state == ViewPager.SCROLL_STATE_IDLE) {
                //updateCurrentTab(mCurPos);
            }
        }
    }

    private void resetAdapter() {
        // trigger adapter load, preserving the selected page
        int curPos = mAdapter.getCurrentPage();
        mViewPager.setAdapter(mAdapter);
        mViewPager.setOnPageChangeListener(mAdapter);
        mViewPager.setCurrentItem(curPos);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // initialize the inflater
        mInflater = inflater;

        View rootView = mInflater.inflate(R.layout.app_ops_summary,
                container, false);
        mContentContainer = container;
        mRootView = rootView;

        CharSequence[] pageNames = getResources().getTextArray(R.array.app_ops_categories_cm);
        AppOpsState.OpsTemplate[] templates = AppOpsState.ALL_TEMPLATES;
        assert(pageNames.length == templates.length);

        int specificTab = -1;
        Bundle bundle = getArguments();
        if (bundle != null) {
            specificTab = Arrays.asList(pageNames).indexOf(bundle.getString("appops_tab", ""));
        }

        List<Pair<CharSequence, AppOpsState.OpsTemplate>> pageData = new ArrayList<>();
        for (int i = 0; i < pageNames.length; i++) {
            pageData.add(Pair.create(pageNames[i], templates[i]));
        }
        filterPageData(pageData, specificTab);

        mViewPager = (ViewPager) rootView.findViewById(R.id.pager);
        mAdapter = new MyPagerAdapter(getChildFragmentManager(), pageData);
        mViewPager.setAdapter(mAdapter);
        mViewPager.setOnPageChangeListener(mAdapter);
        PagerTabStrip tabs = (PagerTabStrip) rootView.findViewById(R.id.tabs);

        Resources.Theme theme = tabs.getContext().getTheme();
        TypedValue typedValue = new TypedValue();
        theme.resolveAttribute(android.R.attr.colorAccent, typedValue, true);
        final int colorAccent = typedValue.resourceId != 0
                ? getContext().getColor(typedValue.resourceId)
                : getContext().getColor(R.color.switch_accent_color);
        tabs.setTabIndicatorColor(colorAccent);

        // We have to do this now because PreferenceFrameLayout looks at it
        // only when the view is added.
        if (container instanceof PreferenceFrameLayout) {
            ((PreferenceFrameLayout.LayoutParams) rootView.getLayoutParams()).removeBorders = true;
        }

        mActivity = getActivity();

        return rootView;
    }

    private void filterPageData(List<Pair<CharSequence, AppOpsState.OpsTemplate>> data, int tab) {
        if (tab >= 0 && tab < data.size()) {
            Pair<CharSequence, AppOpsState.OpsTemplate> item = data.get(tab);
            data.clear();
            data.add(item);
        } else if (!DevelopmentSettings.isRootForAppsEnabled()) {
            for (Pair<CharSequence, AppOpsState.OpsTemplate> item : data) {
                if (item.second == AppOpsState.SU_TEMPLATE) {
                    data.remove(item);
                    return;
                }
            }
        }
    }

    private boolean shouldShowUserApps() {
        return mPreferences.getBoolean("show_user_apps", true);
    }

    private boolean shouldShowSystemApps() {
        return mPreferences.getBoolean("show_system_apps", true) &&
                mActivity.getResources().getBoolean(R.bool.config_showBuiltInAppsForPG);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // get shared preferences
        mPreferences = mActivity.getSharedPreferences("appops_manager", Activity.MODE_PRIVATE);

        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.appops_manager, menu);
        menu.findItem(R.id.show_user_apps).setChecked(shouldShowUserApps());
        if (!mActivity.getResources().getBoolean(R.bool.config_showBuiltInAppsForPG)) {
            menu.removeItem(R.id.show_system_apps);
        } else {
            menu.findItem(R.id.show_system_apps).setChecked(shouldShowSystemApps());
        }
    }

    private void resetCounters() {
        final AppOpsManager appOps =
                (AppOpsManager) mActivity.getSystemService(Context.APP_OPS_SERVICE);
        if (appOps == null) {
            return;
        }
        appOps.resetCounters();
        // reload content
        resetAdapter();
    }

    private void resetCountersConfirm() {
        new AlertDialog.Builder(getActivity())
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setTitle(R.string.app_ops_reset_confirm_title)
            .setMessage(R.string.app_ops_reset_confirm_mesg)
            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        resetCounters();
                    }
                })
            .setNegativeButton(android.R.string.cancel, null)
            .show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.show_user_apps:
                final String prefNameUserApps = "show_user_apps";
                // set the menu checkbox and save it in shared preference
                item.setChecked(!item.isChecked());
                mPreferences.edit().putBoolean(prefNameUserApps, item.isChecked()).commit();
                // reload content
                resetAdapter();
                return true;
            case R.id.show_system_apps:
                final String prefNameSysApps = "show_system_apps";
                // set the menu checkbox and save it in shared preference
                item.setChecked(!item.isChecked());
                mPreferences.edit().putBoolean(prefNameSysApps, item.isChecked()).commit();
                // reload view content
                resetAdapter();
                return true;
            case R.id.reset_counters:
                resetCountersConfirm();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }
}
