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
import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.ContentResolver;
import android.database.ContentObserver;
import android.net.Uri;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.design.widget.Snackbar;

import android.support.v7.preference.ListPreference;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.PreferenceScreen;

import android.support.v4.view.ViewPager;
import android.text.TextUtils;
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
import android.widget.LinearLayout;

import com.android.settings.dashboard.SummaryLoader;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.rr.fab.FloatingActionsMenu;
import com.android.settings.rr.fab.FloatingActionButton;
import com.android.settings.rr.fragments.Buttons;
import com.android.settings.rr.fragments.NavigationBar;
import com.android.settings.rr.animation.AnimationSettings;


import java.util.Random;


public class MainSettingsLayout extends SettingsPreferenceFragment {
    private static final String TAG = "MainSettingsLayout";
    ViewPager mViewPager;
    ViewGroup mContainer;
    PagerSlidingTabStrip mTabs;
    SectionsPagerAdapter mSectionsPagerAdapter;
    protected Context mContext;
	private LinearLayout mLayout;
	private FloatingActionsMenu mFab;
	private FrameLayout mInterceptorFrame;

 	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContainer = container;
        View view = inflater.inflate(R.layout.rr_main, container, false);
        mFab = (FloatingActionsMenu) view.findViewById(R.id.fab_menu);
        mLayout = (LinearLayout) view.findViewById(R.id.main_content);
        mViewPager = (ViewPager) view.findViewById(R.id.viewpager);
        mTabs = (PagerSlidingTabStrip) view.findViewById(R.id.tabs);
        mInterceptorFrame = (FrameLayout) view.findViewById(R.id.fl_interceptor);
        FloatingActionButton mFab1 = (FloatingActionButton) view.findViewById(R.id.fab_event);
        FloatingActionButton mFab2 = (FloatingActionButton) view.findViewById(R.id.fab_restart);
        FloatingActionButton mFab3 = (FloatingActionButton) view.findViewById(R.id.fab_reset);
        FloatingActionButton mFab4 = (FloatingActionButton) view.findViewById(R.id.fab_info);
        FloatingActionButton mFab5 = (FloatingActionButton) view.findViewById(R.id.fab_config);
        mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mTabs.setViewPager(mViewPager);
        mContext = getActivity().getApplicationContext();
        ContentResolver resolver = getActivity().getContentResolver();
        mInterceptorFrame.getBackground().setAlpha(0);
        int which = Settings.System.getInt(getActivity().getContentResolver(),
                    Settings.System.RR_CONFIG_STYLE, 0);
        if (which == 1) {
        mTabs.setVisibility(View.GONE);
        mFab5.setTitle(getString(R.string.fab_layout_toggle));
        } else if (which == 0) {
        mTabs.setVisibility(View.VISIBLE);
        mFab5.setTitle(getString(R.string.fab_layout_update));
        }

        boolean isShowing =  true;

        mFab1.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View v) {
             Intent fabIntent = new Intent();
             fabIntent.setClassName("com.resurrection.ota", "com.resurrection.ota.MainActivity");
             startActivity(fabIntent);
             }
        });

        mFab2.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View v) {
             //Helpers.showSystemUIrestartDialog(getActivity());
             }
        });

        mFab3.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View v) {
             AlertDialog alertDialog = new AlertDialog.Builder(getActivity()).create();
             alertDialog.setTitle(getString(R.string.rr_reset_settings));
             alertDialog.setMessage(getString(R.string.rr_reset_message));

             alertDialog.setButton(getString(R.string.rr_reset_yes), new DialogInterface.OnClickListener() {
                         public void onClick(DialogInterface dialog, int which) {
                         stockitems();
                         }
                    });
             alertDialog.setButton(Dialog.BUTTON_NEGATIVE ,getString(R.string.rr_reset_cancel), new DialogInterface.OnClickListener() {
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

        mFab5.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View v) {
                        if (which == 0) {
                        Settings.System.putInt(getActivity().getContentResolver(),
                        Settings.System.RR_CONFIG_STYLE, 1);
                        } else if(which == 1) {
                        Settings.System.putInt(getActivity().getContentResolver(),
                        Settings.System.RR_CONFIG_STYLE, 0);
                        }
             finish();
             startActivity(getIntent());
             }
        });

        if (isShowing) {
        mFab.setVisibility(View.VISIBLE);
        } else {
        mFab.setVisibility(View.GONE);
        }

        mFab.setOnFloatingActionsMenuUpdateListener(new FloatingActionsMenu.OnFloatingActionsMenuUpdateListener() {
        @Override
        public void onMenuExpanded() {
        mInterceptorFrame.getBackground().setAlpha(240);
        mInterceptorFrame.setOnTouchListener(new View.OnTouchListener() {
             @Override
             public boolean onTouch(View v, MotionEvent event) {
                   mFab.collapse();
                   return true;
                   }
             });
        }

        @Override
        public void onMenuCollapsed() {
                    mInterceptorFrame.getBackground().setAlpha(0);
                    mInterceptorFrame.setOnTouchListener(null);
    	            }
        });

        mInterceptorFrame.setOnTouchListener(new View.OnTouchListener() {
             @Override
             public boolean onTouch(View v, MotionEvent event) {
                if (mFab.isExpanded()) {
                    mFab.collapse();
                    return true;
                }
                return false;
            }
        });

    return view;
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
            frags[1] = new QsPanel();
            frags[2] = new RecentsSettings();
            frags[3] = new UISettings();
            frags[4] = new LockScreenSettings();
            frags[5] = new ButtonSettings();
            frags[6] = new NavigationBar();
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
                getString(R.string.rr_qs_title),
                getString(R.string.rr_recents_title),
                getString(R.string.rr_ui_title),
                getString(R.string.rr_lockscreen_title),
                getString(R.string.rr_buttons_title),
                getString(R.string.rr_navigation_title),
                getString(R.string.animations_title),
                getString(R.string.rr_misc_title),
                getString(R.string.about_rr_settings)};
        } else {
                titleString = new String[]{
                getString(R.string.rr_title)};
        }
        return titleString;
    }

    @Override
     public int getMetricsCategory() {
        return MetricsEvent.RESURRECTED;
     }

    public void stockitems() {
                ContentResolver mResolver = getActivity().getContentResolver();
    }

}
