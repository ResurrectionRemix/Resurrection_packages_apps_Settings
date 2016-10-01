/*Copyright (C) 2015 The ResurrectionRemix Project
     Licensed under the Apache License, Version 2.0 (the "License");
     you may not use this file except in compliance with the License.
     You may obtain a copy of the License at

          http://www.apache.org/licenses/LICENSE-2.0

     Unless required by applicable law or agreed to in writing, software
     distributed under the License is distributed on an "AS IS" BASIS,
     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     See the License for the specific language governing permissions and
     limitations under the License.
*/
package com.android.settings.rr;

import android.content.Context;
import android.content.ContentResolver;
import android.content.res.Resources;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.design.widget.Snackbar;

import android.support.v7.preference.ListPreference;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.PreferenceScreen;

import android.support.v4.view.ViewPager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.ViewAnimationUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.android.settings.util.AbstractAsyncSuCMDProcessor;
import com.android.settings.util.CMDProcessor;
import com.android.settings.util.Helpers;

import com.android.internal.logging.MetricsProto.MetricsEvent;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import com.android.settings.rr.animation.AnimationSettings;
import com.android.settings.rr.input.ButtonSettings;

public class MainSettingsLayout extends SettingsPreferenceFragment {
    private static final String TAG = "MainSettingsLayout";
    ViewPager mViewPager;
    ViewGroup mContainer;
    PagerSlidingTabStrip mTabs;
    SectionsPagerAdapter mSectionsPagerAdapter;
    protected Context mContext;

 	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
   		 mContainer = container;
		 View mFab;

        View view = inflater.inflate(R.layout.rr_main, container, false);
        mFab = view.findViewById(R.id.floating_action_button);
        mViewPager = (ViewPager) view.findViewById(R.id.viewpager);
        mTabs = (PagerSlidingTabStrip) view.findViewById(R.id.tabs);
        mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mTabs.setViewPager(mViewPager);
		
		refreshSettings(mFab);
        setHasOptionsMenu(true);
		return view;

		}
		
		void refreshSettings(View mFab) {
		 mContext = getActivity().getApplicationContext();
		 ContentResolver resolver = getActivity().getContentResolver();

        boolean isShowing =   Settings.System.getInt(resolver,
		Settings.System.RR_OTA_FAB, 1) == 1;
		Animation animation = AnimationUtils.loadAnimation(mContext, R.anim.rotate_around_center);
		Animation animation1 = AnimationUtils.loadAnimation(mContext, R.anim.recent_exit);
        mFab.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View v) {
			 mFab.startAnimation(animation);
             Intent fabIntent = new Intent();
             fabIntent.setClassName("com.rr.ota", "com.rr.center.OTACenter");
             startActivity(fabIntent);
             }
        });
		if (isShowing) {
		mFab.setVisibility(View.VISIBLE);
		} else {
		mFab.setVisibility(View.GONE);
		}
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) 		{

        return true;
    }

 @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.main_menu,menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.restart_ui:
                CMDProcessor.runSuCommand("pkill -f com.android.systemui");
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle saveState) {
        super.onSaveInstanceState(saveState);
    }

    class SectionsPagerAdapter extends FragmentPagerAdapter {

        String titles[] = getTitles();
        private Fragment frags[] = new Fragment[titles.length];

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
            frags[0] = new StatusBarSettings();
            frags[1] = new NotificationDrawerSettings();
            frags[2] = new RecentsSettings();
            frags[3] = new QsPanel();
			frags[4] = new LockScreenSettings();
            frags[5] = new RRGestures();
            frags[6] = new ButtonSettings();
            frags[7] = new AnimationSettings();
            frags[8] = new MiscSettings();
        }

        @Override
        public Fragment getItem(int position) {
            return frags[position];
        }

        @Override
        public int getCount() {
            return frags.length;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return titles[position];
        }
    }

    private String[] getTitles() {
        String titleString[];
        titleString = new String[]{
                getString(R.string.rr_statusbar_title),
                getString(R.string.rr_notification_panel_title),
                getString(R.string.recents_settings_title),
				getString(R.string.rr_qs_title),
				getString(R.string.rr_lockscreen_title),
                getString(R.string.gestures_settings),
                getString(R.string.button_pref_title),
                getString(R.string.animation_title),
                getString(R.string.rr_misc_title) };

        return titleString;
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.RESURRECTED;
     }
}
