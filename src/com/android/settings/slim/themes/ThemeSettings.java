/*
 * Copyright (C) 2013 Slimroms
 * Copyright (C) 2015 The Fusion project
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

package com.android.settings.slim.themes;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.IUiModeManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.preference.ListPreference;
import android.preference.SwitchPreference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Switch;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class ThemeSettings extends SettingsPreferenceFragment
    implements OnPreferenceChangeListener {

    private static final String THEME_AUTO_MODE =
        "pref_theme_auto_mode";
    private static final String THEME_MODE =
        "pref_theme_mode";
    private static final String OVERRIDE_CUSTOM_COLORS =
        "pref_override_custom_colors";

    private ListPreference mThemeAutoMode;
    private SwitchPreference mThemeMode;
    private ThemeEnabler mThemeEnabler;
    private SwitchPreference mOverrideCustomColor;

    private int mCurrentState = 0;
    private static final int MENU_RESET = Menu.FIRST;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.theme_settings);

        PreferenceScreen prefSet = getPreferenceScreen();

        mThemeAutoMode = (ListPreference) prefSet.findPreference(THEME_AUTO_MODE);
        mThemeAutoMode.setValue(String.valueOf(
                Settings.Secure.getIntForUser(getContentResolver(),
                Settings.Secure.UI_THEME_AUTO_MODE, 0,
                UserHandle.USER_CURRENT)));
        mThemeAutoMode.setSummary(mThemeAutoMode.getEntry());

        mThemeAutoMode.setOnPreferenceChangeListener(
            new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference,
                        Object newValue) {
                String val = (String) newValue;
                Settings.Secure.putInt(getContentResolver(),
                    Settings.Secure.UI_THEME_AUTO_MODE,
                    Integer.valueOf(val));
                int index = mThemeAutoMode.findIndexOfValue(val);
                mThemeAutoMode.setSummary(
                    mThemeAutoMode.getEntries()[index]);
                return true;
            }
        });

        mThemeMode = (SwitchPreference) prefSet.findPreference(THEME_MODE);

        mOverrideCustomColor = (SwitchPreference) prefSet.findPreference(OVERRIDE_CUSTOM_COLORS);
        mOverrideCustomColor.setChecked((Settings.System.getInt(getContentResolver(),
                Settings.System.OVERRIDE_CUSTOM_COLORS, 1) == 1));
        mOverrideCustomColor.setOnPreferenceChangeListener(this);

        final Activity activity = getActivity();

        mThemeEnabler = new ThemeEnabler(activity, mThemeMode);

        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mThemeEnabler != null) {
            mThemeEnabler.resume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mThemeEnabler != null) {
            mThemeEnabler.pause();
        }
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mOverrideCustomColor) {
            boolean value = (Boolean) newValue;
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.OVERRIDE_CUSTOM_COLORS, value ? 1 : 0);
            return true;
        }
        return false;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (newConfig.uiThemeMode != mCurrentState && mThemeEnabler != null) {
            mCurrentState = newConfig.uiThemeMode;
            mThemeEnabler.setSwitchState();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, MENU_RESET, 0, R.string.reset_default_message)
                .setIcon(R.drawable.ic_settings_backup_restore)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_RESET:
                resetToDefault();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    private void resetToDefault() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
        alertDialog.setTitle(R.string.shortcut_action_reset);
        alertDialog.setMessage(R.string.theme_reset_message);
        alertDialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                resetValues();
            }
        });
        alertDialog.setNegativeButton(R.string.cancel, null);
        alertDialog.create().show();
    }

    private void resetValues() {
        // reset theme to normal
        final IUiModeManager uiModeManagerService = IUiModeManager.Stub.asInterface(
                ServiceManager.getService(Context.UI_MODE_SERVICE));
        try {
            uiModeManagerService.setUiThemeMode(
                    Configuration.UI_THEME_MODE_NORMAL);
        } catch (RemoteException e) {
        }
    }
}
