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

import static com.android.settings.Utils.SETTINGS_PACKAGE_NAME;

import android.app.AlertDialog;
import android.app.ActionBar;
import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.ContentResolver;
import android.database.ContentObserver;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.content.DialogInterface;
import android.content.res.Resources;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.os.Process;
import android.provider.Settings;
import androidx.fragment.app.FragmentPagerAdapter;
import com.google.android.material.snackbar.Snackbar;

import androidx.preference.ListPreference;
import androidx.preference.SwitchPreference;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceScreen;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.ViewPager;
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
import android.view.ViewConfiguration;
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

import com.android.settings.rr.navigation.BottomNavigationViewCustom;
import com.android.settings.rr.navigation.fragments.StatusBarSettingsNav;
import com.android.settings.rr.navigation.fragments.PanelSettingsNav;
import com.android.settings.rr.navigation.fragments.ButtonSettingsNav;
import com.android.settings.rr.navigation.fragments.UISettingsNav;
import com.android.settings.rr.navigation.fragments.MiscSettingsNav;

import lineageos.providers.LineageSettings;

import java.util.Random;
import com.android.settings.Utils;

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
    private View view;
    private int mStyle;
    MenuItem menuitem;
    PagerAdapter mPagerAdapter;
    LayoutInflater mInflater;
    private static final int MENU_RESET = Menu.FIRST;
    private static final int DLG_RESET  = 0;
    private MenuItem mMenuItem;
    private static final String UPDATER_PACKAGE_NAME = "org.lineageos.updater";
    private static final String UPDATER_PACKAGE_ACTIVITY = "org.lineageos.updater.UpdatesActivity";
    private static final String UPDATER_PACKAGE_MAIN = "android.settings.SYSTEM_UPDATE_SETTINGS";
    private static final String EXTRA_SHOW_FRAGMENT = ":android:show_fragment";
    private static final String SETTINGS_FRAG = "com.android.settings";
    private static final String SETTINGS_ACTION = "com.android.settings.action.IA_SETTINGS";
    private static final String ABOUT_FRAG = "com.android.settings.rr.About";
    private static final String ABOUT_FRAG_ACT = "com.android.settings.Settings$AboutSettingsActivity";
    private static final String SETTINGS_ACCENT = "com.android.settings.Settings$ThemeSettingsActivity";
    static final int DEFAULT_ACCENT_COLOR = 0xff4285f4;

 	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mStyle = Settings.System.getInt(getActivity().getContentResolver(),
                    Settings.System.RR_CONFIG_STYLE, 0);
        mContainer = container;
        mInflater = inflater;
        if (mStyle == 0 || mStyle == 1) { 
            createTabsLayout();
        } else {
            createNavigationLayout();
            setHasOptionsMenu(true);
        }
        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().setTitle(R.string.rr_title);
    }

    public void createTabsLayout() {
        view = mInflater.inflate(R.layout.rr_main, mContainer, false);
        FloatingActionButton mFab1 = (FloatingActionButton) view.findViewById(R.id.fab_event);
        FloatingActionButton mFab2 = (FloatingActionButton) view.findViewById(R.id.fab_restart);
        FloatingActionButton mFab3 = (FloatingActionButton) view.findViewById(R.id.fab_reset);
        FloatingActionButton mFab4 = (FloatingActionButton) view.findViewById(R.id.fab_info);
        FloatingActionButton mFab5 = (FloatingActionButton) view.findViewById(R.id.fab_config);
        FloatingActionButton mFab6 = (FloatingActionButton) view.findViewById(R.id.fab_picker);
        mFab = (FloatingActionsMenu) view.findViewById(R.id.fab_menu);
        mLayout = (LinearLayout) view.findViewById(R.id.main_content);
        mViewPager = (ViewPager) view.findViewById(R.id.viewpager);
        mTabs = (PagerSlidingTabStrip) view.findViewById(R.id.tabs);
        mInterceptorFrame = (FrameLayout) view.findViewById(R.id.fl_interceptor);
        final ActionBar ab = getActivity().getActionBar();
        if (ab != null) {
            ab.setTitle(R.string.rr_title);
        }
        mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setClipChildren(true);
        mViewPager.setClipToPadding(true);
        mTabs.setViewPager(mViewPager);
        mContext = getActivity().getApplicationContext();
        ContentResolver resolver = getActivity().getContentResolver();
        mInterceptorFrame.getBackground().setAlpha(0);
        if (mStyle == 0) {
            mTabs.setVisibility(View.VISIBLE);
            mFab5.setTitle(getString(R.string.fab_layout_update));
        } else if (mStyle == 1) {
            mTabs.setVisibility(View.GONE);
            mFab5.setTitle(getString(R.string.fab_layout_toggle));
        }
        int accent = Utils.getColorAccentDefaultColor(getContext());
        mFab1.setColorFilter(accent);
        mFab2.setColorFilter(accent);
        mFab3.setColorFilter(accent);
        mFab4.setColorFilter(accent);
        mFab5.setColorFilter(accent);
        mFab6.setColorFilter(accent);

        mFab1.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View v) {
             Intent settings = new Intent(Intent.ACTION_MAIN);
             settings.setClassName(UPDATER_PACKAGE_NAME, UPDATER_PACKAGE_ACTIVITY);
             startActivity(settings);
             }
        });

        mFab2.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View v) {
             AlertDialog alertDialog = new AlertDialog.Builder(getActivity()).create();
             alertDialog.setTitle(getString(R.string.restart_systemui));
             alertDialog.setMessage(getString(R.string.restart_settings_warning));

             alertDialog.setButton(getString(R.string.rr_reset_yes), new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int which) {
                          Process.killProcess(Process.myPid());
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
        mFab3.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View v) {
             AlertDialog alertDialog = new AlertDialog.Builder(getActivity()).create();
             alertDialog.setTitle(getString(R.string.rr_reset_settings));
             alertDialog.setMessage(getString(R.string.rr_reset_message));

             alertDialog.setButton(getString(R.string.rr_reset_yes), new DialogInterface.OnClickListener() {
                         public void onClick(DialogInterface dialog, int which) {
                                      AlertDialog alertDialog = new AlertDialog.Builder(getActivity()).create();
                                      alertDialog.setTitle(getString(R.string.rr_reset_settings));
                                      alertDialog.setMessage(getString(R.string.rr_reset_message_warning));

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
                   Intent settings = new Intent(Intent.ACTION_MAIN);
                   settings.setClassName(SETTINGS_FRAG, ABOUT_FRAG_ACT);
                   startActivity(settings);
           }
        });

        mFab6.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View v) {
                   Intent settings = new Intent(Intent.ACTION_MAIN);
                   settings.setClassName(SETTINGS_FRAG, SETTINGS_ACCENT);
                   startActivity(settings);
           }
        });

        mFab5.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View v) {
                        if (mStyle == 0) {
                            showDialogForClassic(getActivity());
                        } else if(mStyle == 1) {
                            showDialogForNav(getActivity());
                        }
             }
        });

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
        mFab.setVisibility(View.VISIBLE);
    }


    public void createNavigationLayout() {
        view = mInflater.inflate(R.layout.main_settings_navigation, mContainer, false);
        final BottomNavigationViewCustom navigation = view.findViewById(R.id.navigation);
        mViewPager = view.findViewById(R.id.viewpager);
        mPagerAdapter = new PagerAdapter(getFragmentManager());
        final ActionBar ab = getActivity().getActionBar();
        if (ab != null) {
            ab.setTitle(R.string.rr_title);
        }
        mViewPager.setAdapter(mPagerAdapter);
        mViewPager.setClipChildren(true);
        mViewPager.setClipToPadding(true);
        ContentResolver resolver = getActivity().getContentResolver();


        navigation.setOnNavigationItemSelectedListener(
                new BottomNavigationViewCustom.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                        switch (item.getItemId()) {
                    case R.id.rr_statusbar_navigation:
                        mViewPager.setCurrentItem(0);
                        return true;
                    case R.id.rr_panels_navigation:
                        mViewPager.setCurrentItem(1);
                        return true;
                    case R.id.rr_buttons_navigation:
                        mViewPager.setCurrentItem(2);
                        return true;
                    case R.id.rr_ui_navigation:
                        mViewPager.setCurrentItem(3);
                        return true;
                    case R.id.rr_misc_navigation:
                        mViewPager.setCurrentItem(4);
                        return true;
                        }
                        return false;
                    }
                });
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset,
                                       int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                if (mMenuItem != null) {
                    mMenuItem.setChecked(false);
                }
                
                navigation.getMenu().getItem(position).setChecked(true);

                mMenuItem = navigation.getMenu().getItem(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });

        setHasOptionsMenu(true);
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
        int mStyle = Settings.System.getInt(getActivity().getContentResolver(),
                    Settings.System.RR_CONFIG_STYLE, 0);
        String titles[] = getTitles();
        private Fragment frags[] = new Fragment[titles.length];

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        	if (mStyle == 0) {
            frags[0] = new StatusBarSettings();
            frags[1] = new Panels();
            frags[2] = new SystemSettings();
            frags[3] = new Interface();
            frags[4] = new LockSettings();
            frags[5] = new Animations();
            frags[6] = new Misc();
        	} else if (mStyle == 1) {
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
        int mStyle = Settings.System.getInt(getActivity().getContentResolver(),
                    Settings.System.RR_CONFIG_STYLE, 0);
        if (mStyle == 0) {
        titleString = new String[]{
                getString(R.string.rr_statusbar_title),
                getString(R.string.rr_panels_title),
		        getString(R.string.rr_system_settings),
		        getString(R.string.rr_ui_title),
		        getString(R.string.rr_lockscreen_title),
                getString(R.string.animation_title),
                getString(R.string.rr_misc_title),};
        } else {
                titleString = new String[]{
                getString(R.string.rr_title)};
        }
        return titleString;
    }


    class PagerAdapter extends FragmentPagerAdapter {

        String titles[] = getTitlesForNav();
        private Fragment frags[] = new Fragment[titles.length];
        public PagerAdapter(FragmentManager fm) {
            super(fm);
            frags[0] = new StatusBarSettingsNav();
            frags[1] = new PanelSettingsNav();
            frags[2] = new ButtonSettingsNav();
            frags[3] = new UISettingsNav();
            frags[4] = new MiscSettingsNav();
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

    private String[] getTitlesForNav() {
        String titleString[];
        titleString = new String[]{
                getString(R.string.rr_statusbar_title),
                getString(R.string.rr_notification_panel_title),
                getString(R.string.rr_buttons_title),
                getString(R.string.rr_ui_title),
                getString(R.string.rr_misc_title)};

        return titleString;
    }

    @Override
     public int getMetricsCategory() {
        return MetricsEvent.RESURRECTED;
     }

    public void stockitems() {
        ContentResolver resolver = getActivity().getContentResolver();
        int screenshotChordKeyTimeout = getContext().getResources().getInteger(
                com.android.internal.R.integer.config_screenshotChordKeyTimeout);
        Settings.System.putIntForUser(resolver,
                Settings.System.SMART_PIXELS_ENABLE, 0, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.SMART_PIXELS_ON_POWER_SAVE, 0, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.SMART_PIXELS_PATTERN, 3, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.SMART_PIXELS_SHIFT_TIMEOUT, 5, UserHandle.USER_CURRENT);
        Settings.Secure.putIntForUser(resolver,
                Settings.Secure.SYSUI_ROUNDED_FWVALS, 1, UserHandle.USER_CURRENT);
        Settings.Global.putInt(resolver,
                Settings.Global.LISTVIEW_ANIMATION, 0);
        Settings.Global.putInt(resolver,
                Settings.Global.LISTVIEW_INTERPOLATOR, 0);
        Settings.System.putIntForUser(resolver,
                Settings.System.ANIM_TILE_STYLE, 0, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.ANIM_TILE_DURATION, 2000, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.ANIM_TILE_INTERPOLATOR, 0, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.STATUSBAR_BATTERY_BAR, 0, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.AUDIO_PANEL_VIEW_RINGER, 0, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.AUDIO_PANEL_VIEW_NOTIFICATION, 0, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.AUDIO_PANEL_VIEW_ALARM, 0, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.AUDIO_PANEL_VIEW_VOICE, 0, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.AUDIO_PANEL_VIEW_BT_SCO, 0, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.HAPTIC_FEEDBACK_ENABLED, 0, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.INCALL_FEEDBACK_VIBRATE, 0, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.NOTIFICATION_VIBRATION_INTENSITY, 2, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.RING_VIBRATION_INTENSITY, 2, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.RINGTONE_VIBRATION_PATTERN, 0, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.VIBRATE_ON_NOTIFICATIONS, 1, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.VIBRATE_WHEN_RINGING, 0, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.STATUS_BAR_SHOW_TICKER, 0, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.STATUS_BAR_TICKER_ANIMATION_MODE, 1, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.STATUS_BAR_TICKER_TICK_DURATION, 3000, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.SMART_CHARGING, 0, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.SMART_CHARGING_RESET_STATS, 0, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.SMART_CHARGING_LEVEL, 80, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.SMART_CHARGING_RESUME_LEVEL, 60, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.GAMING_MODE_ACTIVE, 0, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.GAMING_MODE_ENABLED, 0, UserHandle.USER_CURRENT);
        Settings.System.putStringForUser(resolver,
                Settings.System.GAMING_MODE_VALUES, "", UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.GAMING_MODE_HEADSUP_TOGGLE, 1, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.GAMING_MODE_RINGER_MODE, 0, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.GAMING_MODE_NOTIFICATIONS, 3, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.GAMING_MODE_MANUAL_BRIGHTNESS_TOGGLE, 1, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.GAMING_MODE_DYNAMIC_STATE, 0, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.BERRY_DARK_STYLE, 0, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.STATUS_BAR_CLOCK_AUTO_HIDE, 0, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.STATUS_BAR_CLOCK_AUTO_HIDE_HDURATION, 60, UserHandle.USER_CURRENT);
        LineageSettings.Secure.putIntForUser(resolver,
                LineageSettings.Secure.NETWORK_TRAFFIC_LOCATION, 0, UserHandle.USER_CURRENT);
        LineageSettings.Secure.putIntForUser(resolver,
                LineageSettings.Secure.NETWORK_TRAFFIC_MODE, 0, UserHandle.USER_CURRENT);
        LineageSettings.Secure.putIntForUser(resolver,
                LineageSettings.Secure.NETWORK_TRAFFIC_AUTOHIDE, 1, UserHandle.USER_CURRENT);
        LineageSettings.Secure.putIntForUser(resolver,
                LineageSettings.Secure.NETWORK_TRAFFIC_AUTOHIDE_THRESHOLD, 0, UserHandle.USER_CURRENT);
        LineageSettings.Secure.putIntForUser(resolver,
                LineageSettings.Secure.NETWORK_TRAFFIC_UNITS, 1, UserHandle.USER_CURRENT);
        LineageSettings.Secure.putIntForUser(resolver,
                LineageSettings.Secure.NETWORK_TRAFFIC_SHOW_UNITS, 1, UserHandle.USER_CURRENT);
        LineageSettings.Secure.putIntForUser(resolver,
                LineageSettings.Secure.NETWORK_TRAFFIC_REFRESH_INTERVAL, 2, UserHandle.USER_CURRENT);
        LineageSettings.Secure.putIntForUser(resolver,
                LineageSettings.Secure.NETWORK_TRAFFIC_HIDEARROW, 0, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.AUDIO_PANEL_VIEW_RINGER, 0, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.AUDIO_PANEL_VIEW_NOTIFICATION, 0, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.AUDIO_PANEL_VIEW_ALARM, 0, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.AUDIO_PANEL_VIEW_VOICE, 0, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.AUDIO_PANEL_VIEW_BT_SCO, 0, UserHandle.USER_CURRENT);
        LineageSettings.Secure.putIntForUser(resolver,
                LineageSettings.Secure.VOLUME_PANEL_ON_LEFT, isAudioPanelOnLeftSide(mContext) ? 1 : 0,
                UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.AUDIO_PANEL_VIEW_TIMEOUT, 3, UserHandle.USER_CURRENT);
        Settings.Secure.putIntForUser(resolver,
                Settings.Secure.VOLUME_LINK_NOTIFICATION, 1, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.LOCKSCREEN_WEATHER_STYLE, 1, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.A2DP_SOURCE_CODEC_PRIORITY, 0, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.SBC_HD_PRIORITY, 0, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.ACCENT_COLOR, DEFAULT_ACCENT_COLOR,
                UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.OMNI_STATUS_BAR_CUSTOM_HEADER, 0, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.STATUS_BAR_CUSTOM_HEADER_HEIGHT, 25, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.SCREENSHOT_DELAY,
                screenshotChordKeyTimeout, UserHandle.USER_CURRENT);
        CutoutSettings.reset(mContext);
    }

    private static boolean isAudioPanelOnLeftSide(Context context) {
        try {
            Context con = context.createPackageContext("com.android.systemui", 0);
            int id = con.getResources().getIdentifier("config_audioPanelOnLeftSide",
                    "bool", "com.android.systemui");
            return con.getResources().getBoolean(id);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, MENU_RESET, 0, R.string.fab_layout)
                .setIcon(R.drawable.rr_reset_icon_conf)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_RESET:
                 chooseMode();
                return true;
             default:
                return super.onContextItemSelected(item);
        }
    }

    public void chooseMode() {
        int mStyle = Settings.System.getInt(getActivity().getContentResolver(),
                    Settings.System.RR_CONFIG_STYLE, 0);
        switch (mStyle) {
            case 0:
                showDialogForClassic(getActivity());
                break;
            case 1:
                showDialogForNav(getActivity());
                break;
            case 2:
                showDialogForTabs(getActivity());
                break;
            default:
                break;
        }
    }

    public void showDialogForTabs(Activity a) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(a);
        builder.setTitle(R.string.fab_layout_tabs);
        builder.setMessage(R.string.rr_config_tabs_update_message);
        builder.setPositiveButton(R.string.print_restart,
                new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                 Settings.System.putInt(getActivity().getContentResolver(),
                 Settings.System.RR_CONFIG_STYLE, 0);
                 createTabsLayout();
                 finish();
                 startActivity(getIntent());
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.show();
     }

    public void showDialogForNav(Activity a) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(a);
        builder.setTitle(R.string.fab_layout_toggle);
        builder.setMessage(R.string.rr_config_navigation_update_message);
        builder.setPositiveButton(R.string.print_restart,
                new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                 Settings.System.putInt(getActivity().getContentResolver(),
                 Settings.System.RR_CONFIG_STYLE, 2);
                 createNavigationLayout();
                 finish();
                 startActivity(getIntent());
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.show();

     }

    public void showDialogForClassic(Activity a) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(a);
        builder.setTitle(R.string.fab_layout_update);
        builder.setMessage(R.string.rr_config_classic_update_message);
        builder.setPositiveButton(R.string.print_restart,
                new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                 Settings.System.putInt(getActivity().getContentResolver(),
                 Settings.System.RR_CONFIG_STYLE, 1);
                 createTabsLayout();
                 finish();
                 startActivity(getIntent());
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.show();

     }
}
