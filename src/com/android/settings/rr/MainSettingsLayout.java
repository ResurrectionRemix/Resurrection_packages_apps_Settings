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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.ContentResolver;
import android.content.DialogInterface;
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
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.widget.FrameLayout;

import com.android.settings.util.AbstractAsyncSuCMDProcessor;
import com.android.settings.util.CMDProcessor;
import com.android.settings.util.Helpers;

import com.android.internal.logging.MetricsProto.MetricsEvent;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import com.android.settings.rr.animation.AnimationSettings;
import com.android.settings.rr.input.ButtonSettings;
import com.android.settings.rr.fab.FloatingActionsMenu;
import com.android.settings.rr.fab.FloatingActionButton;


public class MainSettingsLayout extends SettingsPreferenceFragment {
    private static final String TAG = "MainSettingsLayout";
    ViewPager mViewPager;
    ViewGroup mContainer;
    PagerSlidingTabStrip mTabs;
    SectionsPagerAdapter mSectionsPagerAdapter;
    protected Context mContext;

 	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
   		 mContainer = container;

        View view = inflater.inflate(R.layout.rr_main, container, false);
        FloatingActionsMenu mFab = (FloatingActionsMenu) view.findViewById(R.id.fab_menu);
        mViewPager = (ViewPager) view.findViewById(R.id.viewpager);
        mTabs = (PagerSlidingTabStrip) view.findViewById(R.id.tabs);
        mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mTabs.setViewPager(mViewPager);
		
		refreshSettings(mFab,view);
		return view;

		}
		
		void refreshSettings(View mFab, View view) {
		mContext = getActivity().getApplicationContext();
		ContentResolver resolver = getActivity().getContentResolver();

		FloatingActionButton mFab1 = (FloatingActionButton) view.findViewById(R.id.fab_event);
		FloatingActionButton mFab2 = (FloatingActionButton) view.findViewById(R.id.fab_restart);
		FloatingActionButton mFab3 = (FloatingActionButton) view.findViewById(R.id.fab_reset);
		FloatingActionButton mFab4 = (FloatingActionButton) view.findViewById(R.id.fab_info);

        boolean isShowing =   Settings.System.getInt(resolver,
		Settings.System.RR_OTA_FAB, 1) == 1;

        mFab1.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View v) {
             Intent fabIntent = new Intent();
             fabIntent.setClassName("com.rr.ota", "com.rr.center.OTACenter");
             startActivity(fabIntent);
             }
        });

        mFab2.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View v) {
             CMDProcessor.runSuCommand("pkill -f com.android.systemui");
             }
        });

        mFab3.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View v) {
             AlertDialog alertDialog = new AlertDialog.Builder(getActivity()).create();
             alertDialog.setTitle("Reset Settings");
             alertDialog.setMessage("Reset All RR configurations to Default Values?");

             alertDialog.setButton("Yes", new DialogInterface.OnClickListener() {
               			 public void onClick(DialogInterface dialog, int which) {
                		 stockitems();
                		 }
              		 });
             alertDialog.setButton(Dialog.BUTTON_NEGATIVE ,"Cancel", new DialogInterface.OnClickListener() {
                		 public void onClick(DialogInterface dialog, int which) {
                		 return;
                		}
               		});
             alertDialog.show();
             }
        });

		mFab4.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View v) {
             Intent fabIntent = new Intent();
             fabIntent.setClassName("com.android.settings", "com.android.settings.Settings$AboutSettingsActivity");
             startActivity(fabIntent);
             }
        });

		if (isShowing) {
		mFab.setVisibility(View.VISIBLE);
		} else {
		mFab.setVisibility(View.GONE);
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
		int which = Settings.System.getInt(getActivity().getContentResolver(),
                    Settings.System.RR_CONFIG_STYLE, 0);
			if (which == 0) {
            frags[0] = new StatusBarSettings();
            frags[1] = new NotificationDrawerSettings();
            frags[2] = new RecentsSettings();
            frags[3] = new QsPanel();
			frags[4] = new LockScreenSettings();
            frags[5] = new RRGestures();
            frags[6] = new ButtonSettings();
            frags[7] = new AnimationSettings();
            frags[8] = new MiscSettings();
			frags[9] = new About();
        	} else {
		    frags[0] = new MainSettings();
			}
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
		int which = Settings.System.getInt(getActivity().getContentResolver(),
                    Settings.System.RR_CONFIG_STYLE, 0);
		if (which == 0) {
        titleString = new String[]{
                getString(R.string.rr_statusbar_title),
                getString(R.string.rr_np_title),
                getString(R.string.recents_settings_title),
				getString(R.string.rr_qs_panel_title),
				getString(R.string.rr_lockscreen_title),
                getString(R.string.gestures_settings),
                getString(R.string.button_pref_title),
                getString(R.string.rr_animation_title),
                getString(R.string.rr_misc_title),
                getString(R.string.about_rr_settings)};
		} else {
				titleString = new String[]{
				getString(R.string.rr_title)};
		}
        return titleString;
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.RESURRECTED;
     }


    public void stockitems() {
                ContentResolver mResolver = getActivity().getContentResolver();
                            Settings.System.putInt(mResolver,
                                    Settings.System.STATUS_BAR_RR_LOGO, 0);
                            Settings.System.putInt(mResolver,
                                    Settings.System.TOAST_ANIMATION, 0);
                            Settings.System.putInt(mResolver,
                                    Settings.System.LISTVIEW_ANIMATION, 0);
                            Settings.System.putInt(mResolver,
                                    Settings.System.LISTVIEW_INTERPOLATOR, 0);
                            Settings.System.putInt(mResolver,
                                    Settings.System.SHOW_FOURG, 0);
                            Settings.System.putInt(mResolver,
                                    Settings.System.SHOW_THREEG, 0);
                            Settings.System.putInt(mResolver,
                                    Settings.System.STATUS_BAR_RR_LOGO, 0);
                            Settings.System.putInt(mResolver,
                                    Settings.System.TOAST_ICON, 1);
                            Settings.System.putInt(mResolver,
                                    Settings.System.BLUETOOTH_SHOW_BATTERY, 0);
                            Settings.System.putInt(mResolver,
                                    Settings.System.SYSTEMUI_RECENTS_MEM_DISPLAY, 0);
                            Settings.System.putInt(mResolver,
                                    Settings.System.STATUS_BAR_TRAFFIC, 0);
                            Settings.System.putInt(mResolver,
                                    Settings.System.NETWORK_TRAFFIC_AUTOHIDE, 0);
                            Settings.System.putInt(mResolver,
                                    Settings.System.STATUS_BAR_TRAFFIC, 0);
                            Settings.System.putInt(mResolver,
                                    Settings.System.NETWORK_TRAFFIC_AUTOHIDE, 0);
                            Settings.System.putInt(mResolver,
                                    Settings.System.NETWORK_TRAFFIC_AUTOHIDE_THRESHOLD, 0);
                            Settings.System.putInt(mResolver,
                                    Settings.System.NETWORK_TRAFFIC_HIDEARROW, 0);
                            Settings.System.putInt(mResolver,
                                    Settings.System.NETWORK_TRAFFIC_COLOR, 0xffffffff);
                            Settings.System.putInt(mResolver,
                                    Settings.System.POWER_MENU_ANIMATIONS, 0);
                            Settings.System.putInt(mResolver,
                                    Settings.System.POWER_MENU_LOCKSCREEN, 1);
                            Settings.System.putInt(mResolver,
                                    Settings.System.ANIM_TILE_STYLE, 0);
                            Settings.System.putInt(mResolver,
                                    Settings.System.ANIM_TILE_DURATION, 1500);
                            Settings.System.putInt(mResolver,
                                    Settings.System.ANIM_TILE_INTERPOLATOR, 0);
                            Settings.System.putInt(mResolver,
                                    Settings.System.IMMERSIVE_RECENTS, 0);
                            Settings.System.putInt(mResolver,
                                    Settings.System.GESTURE_ANYWHERE_ENABLED, 0);
                            Settings.System.putInt(mResolver,
                                    Settings.System.GESTURE_ANYWHERE_POSITION, 0);
                            Settings.System.putInt(mResolver,
                                    Settings.System.GESTURE_ANYWHERE_TRIGGER_WIDTH, 10);
                            Settings.System.putInt(mResolver,
                                    Settings.System.GESTURE_ANYWHERE_TRIGGER_TOP, 0);
                            Settings.System.putInt(mResolver,
                                    Settings.System.GESTURE_ANYWHERE_TRIGGER_HEIGHT, 100);
                            Settings.System.putInt(mResolver,
                                    Settings.System.LOCKSCREEN_ENABLE_POWER_MENU, 1);
                            Settings.System.putInt(mResolver,
                                    Settings.System.APP_SIDEBAR_ENABLED, 0);
                            Settings.System.putInt(mResolver,
                                    Settings.System.APP_SIDEBAR_POSITION, 0);
                            Settings.System.putInt(mResolver,
                                    Settings.System.APP_SIDEBAR_TRIGGER_WIDTH, 10);
                            Settings.System.putInt(mResolver,
                                    Settings.System.APP_SIDEBAR_TRIGGER_TOP, 100);
                            Settings.System.putInt(mResolver,
                                    Settings.System.APP_SIDEBAR_TRIGGER_HEIGHT, 0);
                            Settings.System.putInt(mResolver,
                                    Settings.System.DOUBLE_TAP_SLEEP_ANYWHERE, 0);
                            Settings.System.putInt(mResolver,
                                    Settings.System.STATUSBAR_BATTERY_BAR, 0);
                            Settings.System.putInt(mResolver,
                                    Settings.System.STATUSBAR_BATTERY_BAR_COLOR, 0);
                            Settings.System.putInt(mResolver,
                                    Settings.System.STATUSBAR_BATTERY_BAR_THICKNESS, 0);
                            Settings.System.putInt(mResolver,
                                    Settings.System.STATUSBAR_BATTERY_BAR_STYLE, 0);
                            Settings.System.putInt(mResolver,
                                    Settings.System.STATUSBAR_CLOCK_DATE_POSITION, 0);
                            Settings.System.putInt(mResolver,
                                    Settings.System.KEYGUARD_TOGGLE_TORCH, 0);
                            Settings.System.putInt(mResolver,
                                    Settings.System.HIDE_LOCKSCREEN_DATE, 1);
                            Settings.System.putInt(mResolver,
                                    Settings.System.HIDE_LOCKSCREEN_CLOCK, 1);
                            Settings.System.putInt(mResolver,
                                    Settings.System.KEYGUARD_SHOW_CLOCK, 0);
                            Settings.System.putInt(mResolver,
                                    Settings.System.FINGERPRINT_SUCCESS_VIB, 0);
                            Settings.System.putInt(mResolver,
                                    Settings.System.LOCK_CLOCK_FONTS, 0);
                            Settings.System.putInt(mResolver,
                                    Settings.System.LOCKSCREEN_MEDIA_METADATA, 0);
                            Settings.System.putInt(mResolver,
                                    Settings.System.LOCK_SCREEN_CUSTOM_NOTIF, 0);
                            Settings.System.putInt(mResolver,
                                    Settings.System.STATUS_BAR_SHOW_WEATHER_TEMP, 0);
                            Settings.System.putInt(mResolver,
                                    Settings.System.STATUS_BAR_WEATHER_SIZE, 14);
                            Settings.System.putInt(mResolver,
                                    Settings.System.STATUS_BAR_WEATHER_FONT_STYLE, 0);
                            Settings.System.putInt(mResolver,
                                    Settings.System.STATUS_BAR_WEATHER_COLOR, 0xffffffff);
                            Settings.System.putInt(mResolver,
                                    Settings.System.LOCK_SCREEN_SHOW_WEATHER, 0);
                            Settings.System.putInt(mResolver,
                                    Settings.System.LOCK_SCREEN_SHOW_WEATHER_LOCATION, 0);
                            Settings.System.putInt(mResolver,
                                    Settings.System.LOCK_SCREEN_WEATHER_CONDITION_ICON, 0);
                            Settings.System.putInt(mResolver,
                                    Settings.System.LOCK_SCREEN_WEATHER_HIDE_PANEL, 0);
                            Settings.System.putInt(mResolver,
                                    Settings.System.SHOW_CLEAR_ALL_RECENTS, 0);
                            Settings.System.putInt(mResolver,
                                    Settings.System.RECENTS_CLEAR_ALL_LOCATION, 0);
                            Settings.System.putInt(mResolver,
                                    Settings.System.QS_SHOW_BRIGHTNESS_ICON, 0);
                            Settings.System.putInt(mResolver,
                                    Settings.System.QS_SMART_PULLDOWN, 0);
                            Settings.System.putInt(mResolver,
                                    Settings.System.RECENTS_FULL_SCREEN_CLOCK, 0);
                            Settings.System.putInt(mResolver,
                                    Settings.System.RECENTS_FULL_SCREEN_DATE, 0);
                            Settings.System.putInt(mResolver,
                                    Settings.System.STATUSBAR_CLOCK_FONT_STYLE, 0);
                            Settings.System.putInt(mResolver,
                                    Settings.System.STATUSBAR_CLOCK_FONT_SIZE, 14);
                            Settings.System.putInt(mResolver,
                                    Settings.System.STATUS_BAR_DATE, 0);
                            Settings.System.putInt(mResolver,
                                    Settings.System.RECENTS_FULL_SCREEN_DATE, 0);
                            Settings.System.putInt(mResolver,
                                    Settings.System.STATUS_BAR_DATE_STYLE, 0);
                            Settings.System.putInt(mResolver,
                                    Settings.System.STATUS_BAR_DATE_FORMAT, 0);
                            Settings.System.putInt(mResolver,
                                    Settings.System.VOLUME_DIALOG_STROKE, 0);
                            Settings.System.putInt(mResolver,
                                    Settings.System.TRANSPARENT_POWER_MENU,100);
                            Settings.System.putInt(mResolver,
                                    Settings.System.TRANSPARENT_POWER_DIALOG_DIM, 50);
                            Settings.System.putInt(mResolver,
                                    Settings.System.QS_STROKE, 0);
                            Settings.System.putInt(mResolver,
                                    Settings.System.QS_ROWS_PORTRAIT, 3);
                            Settings.System.putInt(mResolver,
                                    Settings.System.QS_COLUMNS_PORTRAIT, 3);
                            Settings.System.putInt(mResolver,
                                    Settings.System.QS_ROWS_LANDSCAPE, 2);
                            Settings.System.putInt(mResolver,
                                    Settings.System.QS_COLUMNS_LANDSCAPE, 4);
                            Settings.System.putInt(mResolver,
                                    Settings.System.QS_TRANSPARENT_SHADE, 255);
                            Settings.System.putInt(mResolver,
                                    Settings.System.TRANSPARENT_VOLUME_DIALOG, 255);
                            Settings.Secure.putInt(mResolver,
                                    Settings.Secure.QS_WIFI_EASY_TOGGLE, 0);
                            Settings.Secure.putInt(mResolver,
                                    Settings.Secure.QS_BT_EASY_TOGGLE, 0);
    }

}
