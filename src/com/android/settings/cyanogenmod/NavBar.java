/*
 * Copyright (C) 2011 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.cyanogenmod;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.util.ExtendedPropertiesUtils;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.android.settings.R;
import com.android.settings.Utils;

public class NavBar extends Fragment {

    private boolean mEditMode;
    private ViewGroup mContainer;
    private Activity mActivity;
    private MenuItem mEditMenu;
    private final static Intent mIntent = new Intent("android.intent.action.NAVBAR_EDIT");
    private static final int MENU_RESET = Menu.FIRST;
    private static final int MENU_EDIT = Menu.FIRST + 1;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.nav_bar, container, false);
        setHasOptionsMenu(true);
        mContainer = container;
        mActivity = getActivity();
        mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        return view;
    }

    /**
     * Toggles navbar edit mode
     * @param on True to enter edit mode / false to exit
     * @param save True to save changes / false to discard them
     */
    private void toggleEditMode(boolean on, boolean save) {
        mIntent.putExtra("edit", on);
        mIntent.putExtra("save", save);
        mActivity.sendBroadcast(mIntent);
        if (mEditMenu != null) {
            mEditMenu.setTitle(on ? R.string.navigation_bar_menu_editable :  R.string.navigation_bar_menu_locked)
            .setIcon(on ? R.drawable.stat_navbar_edit_on : R.drawable.stat_navbar_edit_off);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // If running on a phone, remove padding around container
        if (!ExtendedPropertiesUtils.isTablet()) {
            mContainer.setPadding(0, 0, 0, 0);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, MENU_RESET, 0, R.string.navigation_reset_title)
        .setIcon(R.drawable.ic_settings_backup)
        .setAlphabeticShortcut('r')
        .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM |
                MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        mEditMenu = menu.add(0, MENU_EDIT, 0, R.string.navigation_bar_menu_locked);
        mEditMenu.setIcon(R.drawable.stat_navbar_edit_off)
        .setAlphabeticShortcut('s')
        .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM |
                MenuItem.SHOW_AS_ACTION_WITH_TEXT);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_RESET:
            new AlertDialog.Builder(mActivity)
            .setTitle(R.string.navigation_reset_title)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setMessage(R.string.navigation_bar_reset_message)
            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    if (mEditMode) {
                        toggleEditMode(false, false);
                    }
                    Settings.System.putString(getActivity().getContentResolver(),
                            Settings.System.NAV_BUTTONS, null);
                    toggleEditMode(true, false);
                    toggleEditMode(false, false);
                    mEditMode = false;
                    Toast.makeText(mActivity, R.string.navigation_bar_reset_toast, Toast.LENGTH_LONG).show();
                }
            }).setNegativeButton(R.string.cancel, null)
            .create().show();
            return true;
        case MENU_EDIT:
            mEditMode = !mEditMode;
            toggleEditMode(mEditMode, true);
            if (!mEditMode) {
                Toast.makeText(mActivity, R.string.navigation_bar_save_message, Toast.LENGTH_LONG).show();
            }
            return true;
        default:
            return false;
        }
    }

    @Override
    public void onPause() {
        toggleEditMode(false, false);
        super.onPause();
    }

    @Override
    public void onStop() {
        toggleEditMode(false, false);
        super.onStop();
    }

    @Override
    public void onDestroy() {
        toggleEditMode(false, false);
        super.onDestroy();
    }
}
