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
package com.android.settings.rr.navbar;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.res.Resources;
import android.provider.Settings;
import android.os.Bundle;
import android.os.UserHandle;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.PreferenceScreen;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.rr.SeekBarPreference;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import java.util.ArrayList;
import java.util.List;

public class PixelAnimDurationSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String PIXEL_X = "opa_anim_duration_x";
    private static final String PIXEL_Y = "opa_anim_duration_y";
    private static final String PIXEL_COLLAPSE = "collapse_anim_duration_ry";
    private static final String PIXEL_BG = "collapse_anim_duration_bg";
    private static final String PIXEL_RETRACT = "retract_anim_duration";
    private static final String PIXEL_DIAMOND = "diamond_anim_duration";
    private static final String PIXEL_DOTS = "dots_anim_duration";
    private static final String PIXEL_HOME = "home_resize_anim_duration";

    private SeekBarPreference mPixelx;
    private SeekBarPreference mPixely;
    private SeekBarPreference mCollapse;
    private SeekBarPreference mBg;
    private SeekBarPreference mRetract;
    private SeekBarPreference mDiamond;
    private SeekBarPreference mDots;
    private SeekBarPreference mHome;
    protected Context mContext;
    protected ContentResolver mContentRes;

    private static final int COLLAPSE_ANIMATION_DURATION_RY = 83;
    private static final int COLLAPSE_ANIMATION_DURATION_BG = 100;
    private static final int LINE_ANIMATION_DURATION_Y = 275;
    private static final int LINE_ANIMATION_DURATION_X = 133;
    private static final int RETRACT_ANIMATION_DURATION = 300;
    private static final int DIAMOND_ANIMATION_DURATION = 200;
    private static final int HALO_ANIMATION_DURATION = 100;

    private static final int DOTS_RESIZE_DURATION = 200;
    private static final int HOME_RESIZE_DURATION = 83;

    private static final int MENU_RESET = Menu.FIRST;
    private static final int DLG_RESET = 0;

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.RESURRECTED;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        refreshSettings();
    }

    public void refreshSettings() {

        PreferenceScreen prefs = getPreferenceScreen();
        if (prefs != null) {
            prefs.removeAll();
        }

        addPreferencesFromResource(R.xml.pixel_anim_duration);

		mContext = getActivity().getApplicationContext();

        mContentRes = getActivity().getContentResolver();
	    final Resources res = getResources();
		int defaultValue;

        mPixelx =
                (SeekBarPreference) findPreference(PIXEL_X);
        int xanim = Settings.System.getIntForUser(getContentResolver(),
                Settings.System.OPA_ANIM_DURATION_X, 133, UserHandle.USER_CURRENT);
        mPixelx.setValue(xanim / 1);
        mPixelx.setOnPreferenceChangeListener(this);

        mPixely =
                (SeekBarPreference) findPreference(PIXEL_Y);
        int yanim = Settings.System.getIntForUser(getContentResolver(),
                Settings.System.OPA_ANIM_DURATION_Y, 255, UserHandle.USER_CURRENT);
        mPixely.setValue(yanim / 1);
        mPixely.setOnPreferenceChangeListener(this);

        mCollapse =
                (SeekBarPreference) findPreference(PIXEL_COLLAPSE);
        int xcol = Settings.System.getIntForUser(getContentResolver(),
                Settings.System.COLLAPSE_ANIMATION_DURATION_RY, 83, UserHandle.USER_CURRENT);
        mCollapse.setValue(xcol / 1);
        mCollapse.setOnPreferenceChangeListener(this);

        mBg =
               (SeekBarPreference) findPreference(PIXEL_BG);
        int bg = Settings.System.getIntForUser(getContentResolver(),
                Settings.System.COLLAPSE_ANIMATION_DURATION_BG, 100, UserHandle.USER_CURRENT);
        mBg.setValue(yanim / 1);
        mBg.setOnPreferenceChangeListener(this);

        mRetract =
                (SeekBarPreference) findPreference(PIXEL_RETRACT);
        int ret = Settings.System.getIntForUser(getContentResolver(),
                Settings.System.RETRACT_ANIMATION_DURATION, 300, UserHandle.USER_CURRENT);
        mRetract.setValue(ret/ 1);
        mRetract.setOnPreferenceChangeListener(this);

        mDiamond =
                (SeekBarPreference) findPreference(PIXEL_DIAMOND);
        int diam = Settings.System.getIntForUser(getContentResolver(),
                Settings.System.DIAMOND_ANIMATION_DURATION, 200, UserHandle.USER_CURRENT);
        mDiamond.setValue(diam / 1);
        mDiamond.setOnPreferenceChangeListener(this);

        mDots =
                (SeekBarPreference) findPreference(PIXEL_DOTS);
        int dots = Settings.System.getIntForUser(getContentResolver(),
                Settings.System.DOTS_RESIZE_DURATION, 200, UserHandle.USER_CURRENT);
        mDots.setValue(dots / 1);
        mDots.setOnPreferenceChangeListener(this);

        mHome =
                (SeekBarPreference) findPreference(PIXEL_HOME);
        int home = Settings.System.getIntForUser(getContentResolver(),
                Settings.System.HOME_RESIZE_DURATION, 255, UserHandle.USER_CURRENT);
        mHome.setValue(home / 1);
        mHome.setOnPreferenceChangeListener(this);

    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
		int intValue;
        int index;
		ContentResolver resolver = getActivity().getContentResolver();
		final Resources res = getResources();
        if (preference == mPixelx) {
            int val = (Integer) objValue;
            Settings.System.putIntForUser(getContentResolver(),
                    Settings.System.OPA_ANIM_DURATION_X, val * 1, UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mPixely) {
            int val = (Integer) objValue;
            Settings.System.putIntForUser(getContentResolver(),
                    Settings.System.OPA_ANIM_DURATION_Y, val * 1, UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mCollapse) {
            int val = (Integer) objValue;
            Settings.System.putIntForUser(getContentResolver(),
                    Settings.System.COLLAPSE_ANIMATION_DURATION_RY, val * 1, UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mBg) {
            int val = (Integer) objValue;
            Settings.System.putIntForUser(getContentResolver(),
                    Settings.System.COLLAPSE_ANIMATION_DURATION_BG, val * 1, UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mRetract) {
            int val = (Integer) objValue;
            Settings.System.putIntForUser(getContentResolver(),
                    Settings.System.RETRACT_ANIMATION_DURATION, val * 1, UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mDiamond) {
            int val = (Integer) objValue;
            Settings.System.putIntForUser(getContentResolver(),
                    Settings.System.DIAMOND_ANIMATION_DURATION, val * 1, UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mDots) {
            int val = (Integer) objValue;
            Settings.System.putIntForUser(getContentResolver(),
                    Settings.System.DOTS_RESIZE_DURATION, val * 1, UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mHome) {
            int val = (Integer) objValue;
            Settings.System.putIntForUser(getContentResolver(),
                    Settings.System.HOME_RESIZE_DURATION, val * 1, UserHandle.USER_CURRENT);
            return true;
        }
        return false;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, MENU_RESET, 0, R.string.reset)
                .setIcon(R.drawable.ic_action_reset) // use the KitKat reset icon
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_RESET:
                showDialogInner(DLG_RESET);
                return true;
             default:
                return super.onContextItemSelected(item);
        }
   }


   private void showDialogInner(int id) {
        DialogFragment newFragment = MyAlertDialogFragment.newInstance(id);
        newFragment.setTargetFragment(this, 0);
        newFragment.show(getFragmentManager(), "dialog " + id);
   }

   public static class MyAlertDialogFragment extends DialogFragment {

        public static MyAlertDialogFragment newInstance(int id) {
            MyAlertDialogFragment frag = new MyAlertDialogFragment();
            Bundle args = new Bundle();
            args.putInt("id", id);
            frag.setArguments(args);
            return frag;
        }


        PixelAnimDurationSettings getOwner() {
            return (PixelAnimDurationSettings) getTargetFragment();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int id = getArguments().getInt("id");
            switch (id) {
                case DLG_RESET:
                    return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.reset)
                    .setMessage(R.string.animation_settings_reset_message)
                    .setNegativeButton(R.string.cancel, null)
                    .setNeutralButton(R.string.dlg_reset_android,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Settings.System.putInt(getOwner().mContentRes,
                                    Settings.System.OPA_ANIM_DURATION_X, 
                                    LINE_ANIMATION_DURATION_X);
                            Settings.System.putInt(getOwner().mContentRes,
                                    Settings.System.OPA_ANIM_DURATION_Y, 
                                    LINE_ANIMATION_DURATION_Y);
                            Settings.System.putInt(getOwner().mContentRes,
                                    Settings.System.COLLAPSE_ANIMATION_DURATION_RY, 
                                    COLLAPSE_ANIMATION_DURATION_RY);
                            Settings.System.putInt(getOwner().mContentRes,
                                    Settings.System.COLLAPSE_ANIMATION_DURATION_BG,
                                    COLLAPSE_ANIMATION_DURATION_BG);
                            Settings.System.putInt(getOwner().mContentRes,
                                    Settings.System.RETRACT_ANIMATION_DURATION,
                                    RETRACT_ANIMATION_DURATION);
                            Settings.System.putInt(getOwner().mContentRes,
                                    Settings.System.DIAMOND_ANIMATION_DURATION,
                                    DIAMOND_ANIMATION_DURATION);
                            Settings.System.putInt(getOwner().mContentRes,
                                    Settings.System.DOTS_RESIZE_DURATION, 
                                    DOTS_RESIZE_DURATION);
                            Settings.System.putInt(getOwner().mContentRes,
                                    Settings.System.HOME_RESIZE_DURATION, 
                                    HOME_RESIZE_DURATION);
                            getOwner().refreshSettings();
                        }
                    })
                    .setPositiveButton(R.string.dlg_reset_rr,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Settings.System.putInt(getOwner().mContentRes,
                                    Settings.System.OPA_ANIM_DURATION_X, 
                                    400);
                            Settings.System.putInt(getOwner().mContentRes,
                                    Settings.System.OPA_ANIM_DURATION_Y, 
                                   400);
                            Settings.System.putInt(getOwner().mContentRes,
                                    Settings.System.COLLAPSE_ANIMATION_DURATION_RY, 
                                    500);
                            Settings.System.putInt(getOwner().mContentRes,
                                    Settings.System.COLLAPSE_ANIMATION_DURATION_BG,
                                    300);
                            Settings.System.putInt(getOwner().mContentRes,
                                    Settings.System.RETRACT_ANIMATION_DURATION,
                                    600);
                            Settings.System.putInt(getOwner().mContentRes,
                                    Settings.System.DIAMOND_ANIMATION_DURATION,
                                    450);
                            Settings.System.putInt(getOwner().mContentRes,
                                    Settings.System.DOTS_RESIZE_DURATION, 
                                    500);
                            Settings.System.putInt(getOwner().mContentRes,
                                    Settings.System.HOME_RESIZE_DURATION, 
                                    367);
                            getOwner().refreshSettings();
                        }
                    })
                    .create();
            }
            throw new IllegalArgumentException("unknown id " + id);
        }

        @Override
        public void onCancel(DialogInterface dialog) {

        }
    }
}
