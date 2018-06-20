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
import android.graphics.drawable.ColorDrawable;
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
import android.support.annotation.NonNull;
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
import com.android.settings.rr.utils.Helpers;
import com.android.settings.rr.fab.FloatingActionsMenu;
import com.android.settings.rr.fab.FloatingActionButton;
import com.android.settings.rr.fragments.Buttons;
import com.android.settings.rr.fragments.NavigationBar;
import com.android.settings.rr.animation.AnimationSettings;
import com.android.settings.rr.navigation.BottomNavigationViewCustom;
import com.android.settings.rr.navigation.fragments.StatusBarSettingsNav;
import com.android.settings.rr.navigation.fragments.PanelSettingsNav;
import com.android.settings.rr.navigation.fragments.ButtonSettingsNav;
import com.android.settings.rr.navigation.fragments.UISettingsNav;
import com.android.settings.rr.navigation.fragments.MiscSettingsNav;

import java.util.Random;

import java.util.Random;


import com.android.settings.rr.transforms.*;

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
    private SettingsObserver mSettingsObserver;
    private View view;
    private int mStyle;
    MenuItem menuitem;
    PagerAdapter mPagerAdapter;
    LayoutInflater mInflater;
    private static final int MENU_RESET = Menu.FIRST;
    private static final int DLG_RESET  = 0;

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
        mSettingsObserver = new SettingsObserver(new Handler());
        mSettingsObserver.observe();
        return view;
    }

    public void createTabsLayout() {
        view = mInflater.inflate(R.layout.rr_main, mContainer, false);
        FloatingActionButton mFab1 = (FloatingActionButton) view.findViewById(R.id.fab_event);
        FloatingActionButton mFab2 = (FloatingActionButton) view.findViewById(R.id.fab_restart);
        FloatingActionButton mFab3 = (FloatingActionButton) view.findViewById(R.id.fab_reset);
        FloatingActionButton mFab4 = (FloatingActionButton) view.findViewById(R.id.fab_info);
        FloatingActionButton mFab5 = (FloatingActionButton) view.findViewById(R.id.fab_config);
        mFab = (FloatingActionsMenu) view.findViewById(R.id.fab_menu);
        mLayout = (LinearLayout) view.findViewById(R.id.main_content);
        mViewPager = (ViewPager) view.findViewById(R.id.viewpager);
        mTabs = (PagerSlidingTabStrip) view.findViewById(R.id.tabs);
        mInterceptorFrame = (FrameLayout) view.findViewById(R.id.fl_interceptor);
        mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mTabs.setViewPager(mViewPager);
        mContext = getActivity().getApplicationContext();
        ContentResolver resolver = getActivity().getContentResolver();
        mInterceptorFrame.getBackground().setAlpha(0);
        boolean isShowing =   Settings.System.getInt(resolver,
                 Settings.System.RR_OTA_FAB, 1) == 1;
        if (mStyle == 0) {
            mTabs.setVisibility(View.VISIBLE);
            mFab5.setTitle(getString(R.string.fab_layout_update));
        } else if (mStyle == 1) {
            mTabs.setVisibility(View.GONE);
            mFab5.setTitle(getString(R.string.fab_layout_toggle));
        }

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
             Helpers.showSystemUIrestartDialog(getActivity());
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

        if (isShowing) {
            mFab.setVisibility(View.VISIBLE);
        } else {
            mFab.setVisibility(View.GONE);
        }
    }


    public void createNavigationLayout() {
        view = mInflater.inflate(R.layout.main_settings_navigation, mContainer, false);
        final BottomNavigationViewCustom navigation = view.findViewById(R.id.navigation);
        mViewPager = view.findViewById(R.id.viewpager);
        navigation.setBackground(new ColorDrawable(getResources().getColor(R.color.BottomBarBackgroundColor)));
        mPagerAdapter = new PagerAdapter(getFragmentManager());
        mViewPager.setAdapter(mPagerAdapter);
        ContentResolver resolver = getActivity().getContentResolver();

        navigation.setOnNavigationItemSelectedListener(new BottomNavigationViewCustom.OnNavigationItemSelectedListener() {
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
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if(menuitem != null) {
                    menuitem.setChecked(false);
                } else {
                    navigation.getMenu().getItem(0).setChecked(false);
                }
                navigation.getMenu().getItem(position).setChecked(true);
                menuitem = navigation.getMenu().getItem(position);
            }
            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle saveState) {
        super.onSaveInstanceState(saveState);
    }

    private static class SummaryProvider implements SummaryLoader.SummaryProvider {

        private final Context mContext;
        private final SummaryLoader mSummaryLoader;

        public SummaryProvider(Context context, SummaryLoader summaryLoader) {
            mContext = context;
            mSummaryLoader = summaryLoader;
        }

        @Override
        public void setListening(boolean listening) {
            String mCustomSummary = Settings.System.getString(
                    mContext.getContentResolver(), Settings.System.RR_SETTINGS_SUMMARY);
            boolean mRandSum = Settings.System.getInt(
                    mContext.getContentResolver(), Settings.System.RR_SETTINGS_RANDOM_SUMMARY, 0) == 1;
            final String[] summariesArray = mContext.getResources().getStringArray(R.array.custom_summaries);
            String chosenSum = randomSummary(summariesArray);

            if (listening) {
                if (TextUtils.isEmpty(mCustomSummary) && !mRandSum) {
                    mSummaryLoader.setSummary(this, mContext.getString(R.string.rr_title_summary));
                } else if (!TextUtils.isEmpty(mCustomSummary) && !mRandSum) { //Random is off, Use User's input
                    mSummaryLoader.setSummary(this, mCustomSummary);
                } else if (TextUtils.isEmpty(mCustomSummary) && mRandSum) { //Random is on, User Input is blank
                    mSummaryLoader.setSummary(this, chosenSum);
                } else if (!TextUtils.isEmpty(mCustomSummary) && mRandSum) { //Random is on, but User has input
                    mSummaryLoader.setSummary(this, chosenSum); //Override Text from user input
                }
            }
       }

        public static String randomSummary(String[] array) {
            int rand = new Random().nextInt(array.length);
            return array[rand];
        }
   }

    public static final SummaryLoader.SummaryProviderFactory SUMMARY_PROVIDER_FACTORY
            = new SummaryLoader.SummaryProviderFactory() {
        @Override
        public SummaryLoader.SummaryProvider createSummaryProvider(Activity activity,
                                                                   SummaryLoader summaryLoader) {
            return new SummaryProvider(activity, summaryLoader);
        }
    };

    class SectionsPagerAdapter extends FragmentPagerAdapter {
        int mStyle = Settings.System.getInt(getActivity().getContentResolver(),
                    Settings.System.RR_CONFIG_STYLE, 0);
        String titles[] = getTitles();
        private Fragment frags[] = new Fragment[titles.length];

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        	if (mStyle == 0) {
            frags[0] = new StatusBarSettings();
            frags[1] = new PanelSettings();
            frags[2] = new QsPanel();
            frags[3] = new RecentsSettings();
            frags[4] = new UISettings();
            frags[5] = new LockScreenSettings();
            frags[6] = new ButtonSettings();
            frags[7] = new NavigationBar();
            frags[8] = new AnimationSettings();
            frags[9] = new RRGestures();
            frags[10] = new MiscSettings();
            frags[11] = new About();
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
                getString(R.string.rr_qs_title),
                getString(R.string.rr_recents_title),
                getString(R.string.rr_ui_title),
                getString(R.string.rr_lockscreen_title),
                getString(R.string.rr_buttons_title),
                getString(R.string.rr_navigation_title),
                getString(R.string.animation_title),
                getString(R.string.gesture_anywhere_gestures_title),
                getString(R.string.rr_misc_title),
                getString(R.string.about_rr_settings)};
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
                ContentResolver mResolver = getActivity().getContentResolver();
    }



   private class SettingsObserver extends ContentObserver {
            SettingsObserver(Handler handler) {
                super(handler);
            }
    
            void observe() {
                ContentResolver resolver = getActivity().getContentResolver();
                resolver.registerContentObserver(Settings.System.getUriFor(
                        Settings.System.RR_SETTINGS_TABS_EFFECT),
                        false, this, UserHandle.USER_ALL);
                resolver.registerContentObserver(Settings.System.getUriFor(
                        Settings.System.RR_CONFIG_STYLE),
                        false, this, UserHandle.USER_ALL);
                update();
            }
    
            void unobserve() {
                ContentResolver resolver = getActivity().getContentResolver();
                resolver.unregisterContentObserver(this);
            }
    
            @Override
            public void onChange(boolean selfChange) {
                super.onChange(selfChange);
                update();
            }
    
            @Override
            public void onChange(boolean selfChange, Uri uri) {
                update();
            }
    
            public void update() {
            try {
                ContentResolver resolver = getActivity().getContentResolver();
                int effect = Settings.System.getIntForUser(resolver,
                    Settings.System.RR_SETTINGS_TABS_EFFECT, 0,
                    UserHandle.USER_CURRENT);
                switch (effect) {
                    case 0:
                        mViewPager.setPageTransformer(true, new DefaultTransformer());
                        break;
                    case 1:
                        mViewPager.setPageTransformer(true, new AccordionTransformer());
                        break;
                    case 2:
                        mViewPager.setPageTransformer(true, new BackgroundToForegroundTransformer());
                        break;
                    case 3:
                        mViewPager.setPageTransformer(true, new CubeInTransformer());
                        break;
                    case 4:
                        mViewPager.setPageTransformer(true, new CubeOutTransformer());
                        break;
                    case 5:
                        mViewPager.setPageTransformer(true, new DepthPageTransformer());
                        break;
                    case 6:
                        mViewPager.setPageTransformer(true, new FlipHorizontalTransformer());
                        break;
                    case 7:
                        mViewPager.setPageTransformer(true, new FlipVerticalTransformer());
                        break;
                    case 8:
                        mViewPager.setPageTransformer(true, new ForegroundToBackgroundTransformer());
                        break;
                    case 9:
                        mViewPager.setPageTransformer(true, new RotateDownTransformer());
                        break;
                    case 10:
                        mViewPager.setPageTransformer(true, new RotateUpTransformer());
                        break;
                    case 11:
                        mViewPager.setPageTransformer(true, new ScaleInOutTransformer());
                        break;
                    case 12:
                        mViewPager.setPageTransformer(true, new StackTransformer());
                        break;
                    case 13:
                        mViewPager.setPageTransformer(true, new TabletTransformer());
                        break;
                    case 14:
                        mViewPager.setPageTransformer(true, new ZoomInTransformer());
                        break;
                    case 15:
                        mViewPager.setPageTransformer(true, new ZoomOutSlideTransformer());
                        break;
                    case 16:
                        mViewPager.setPageTransformer(true, new ZoomOutTranformer());
                        break;
                    default:
                        break;
                }
              } catch (Exception e){}
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
