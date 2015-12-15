/**
 * Copyright (C) 2014 The Slimroms Project
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

package com.android.settings.rr;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.internal.logging.MetricsLogger;

import com.android.internal.util.slim.DeviceUtils;

public class NavBarDimensions extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener {

    private static final String LIST_NAVIGATION_BAR_HEIGHT = "navigation_bar_height";
    private static final String LIST_NAVIGATION_BAR_HEIGHT_LANDSCAPE = "navigation_bar_height_landscape";
    private static final String LIST_NAVIGATION_BAR_WIDTH = "navigation_bar_width";

    private static final int MENU_RESET = Menu.FIRST;
    private static final int DLG_RESET = 0;

    ListPreference mNavigationBarHeight;
    ListPreference mNavigationBarHeightLandcape;
    ListPreference mNavigationBarWidth;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.navbar_dimensions_settings);

        PreferenceScreen prefSet = getPreferenceScreen();

        // Height
        mNavigationBarHeight = (ListPreference) findPreference(LIST_NAVIGATION_BAR_HEIGHT);
        mNavigationBarHeight.setOnPreferenceChangeListener(this);

        // Height Landscape
        mNavigationBarHeightLandcape = (ListPreference) findPreference(LIST_NAVIGATION_BAR_HEIGHT_LANDSCAPE);
        if (DeviceUtils.isPhone(getActivity())) {
            prefSet.removePreference(mNavigationBarHeightLandcape);
            mNavigationBarHeightLandcape = null;
        } else {
            mNavigationBarHeightLandcape.setOnPreferenceChangeListener(this);
        }
        // Width
        mNavigationBarWidth = (ListPreference) findPreference(LIST_NAVIGATION_BAR_WIDTH);
        if (!DeviceUtils.isPhone(getActivity())) {
            prefSet.removePreference(mNavigationBarWidth);
            mNavigationBarWidth = null;
        } else {
            mNavigationBarWidth.setOnPreferenceChangeListener(this);
        }

        updateDimension();
        setHasOptionsMenu(true);
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.DONT_TRACK_ME_BRO;
    }

    private void updateDimension() {
        int navigationBarHeight = Settings.System.getInt(getContentResolver(),
                Settings.System.NAVIGATION_BAR_HEIGHT, -2);
        if (navigationBarHeight == -2) {
            navigationBarHeight =
                    (int) (getResources().getDimension(com.android.internal.R.dimen.navigation_bar_height)
                    / getResources().getDisplayMetrics().density);
        }
        mNavigationBarHeight.setValue(String.valueOf(navigationBarHeight));

        if (mNavigationBarHeightLandcape == null) {
            return;
        }
        int navigationBarHeightLandcape = Settings.System.getInt(getContentResolver(),
                Settings.System.NAVIGATION_BAR_HEIGHT_LANDSCAPE, -2);
        if (navigationBarHeightLandcape == -2) {
            navigationBarHeightLandcape =
                    (int) (getResources().getDimension(com.android.internal.R.dimen.navigation_bar_height_landscape)
                    / getResources().getDisplayMetrics().density);
        }
        mNavigationBarHeightLandcape.setValue(String.valueOf(navigationBarHeightLandcape));

        if (mNavigationBarWidth == null) {
            return;
        }
        int navigationBarWidth = Settings.System.getInt(getContentResolver(),
                Settings.System.NAVIGATION_BAR_WIDTH, -2);
        if (navigationBarWidth == -2) {
            navigationBarWidth =
                    (int) (getResources().getDimension(com.android.internal.R.dimen.navigation_bar_width)
                    / getResources().getDisplayMetrics().density);
        }
        mNavigationBarWidth.setValue(String.valueOf(navigationBarWidth));

    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mNavigationBarHeight) {
            String newVal = (String) newValue;
            Settings.System.putInt(getContentResolver(),
                    Settings.System.NAVIGATION_BAR_HEIGHT,
                    Integer.parseInt(newVal));
            return true;
        } else if (preference == mNavigationBarHeightLandcape) {
            String newVal = (String) newValue;
            Settings.System.putInt(getContentResolver(),
                    Settings.System.NAVIGATION_BAR_HEIGHT_LANDSCAPE,
                    Integer.parseInt(newVal));
            return true;
        } else if (preference == mNavigationBarWidth) {
            String newVal = (String) newValue;
            Settings.System.putInt(getContentResolver(),
                    Settings.System.NAVIGATION_BAR_WIDTH,
                    Integer.parseInt(newVal));
            return true;
        }
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, MENU_RESET, 0, R.string.menu_restore)
                .setIcon(R.drawable.ic_menu_reset)
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

        NavBarDimensions getOwner() {
            return (NavBarDimensions) getTargetFragment();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int id = getArguments().getInt("id");
            switch (id) {
                case DLG_RESET:
                    return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.menu_restore)
                    .setMessage(R.string.navigation_bar_dimensions_reset_message)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.dlg_ok,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Settings.System.putInt(getActivity().getContentResolver(),
                                    Settings.System.NAVIGATION_BAR_HEIGHT, -2);
                            Settings.System.putInt(getActivity().getContentResolver(),
                                    Settings.System.NAVIGATION_BAR_HEIGHT_LANDSCAPE, -2);
                            Settings.System.putInt(getActivity().getContentResolver(),
                                    Settings.System.NAVIGATION_BAR_WIDTH, -2);
                            getOwner().updateDimension();
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
