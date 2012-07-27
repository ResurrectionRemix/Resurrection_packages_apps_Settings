/*
 * Copyright (C) 2012 The CyanogenMod Project
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

package com.android.settings.cyanogenmod;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.widget.Toast;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class HardwareKeys extends SettingsPreferenceFragment implements OnPreferenceChangeListener {

    private static final String HARDWARE_KEYS_CATEGORY_BINDINGS = "hardware_keys_bindings";
    private static final String HARDWARE_KEYS_ENABLE_CUSTOM = "hardware_keys_enable_custom";
    private static final String HARDWARE_KEYS_HOME_LONG_PRESS = "hardware_keys_home_long_press";
    private static final String HARDWARE_KEYS_MENU_PRESS = "hardware_keys_menu_press";
    private static final String HARDWARE_KEYS_MENU_LONG_PRESS = "hardware_keys_menu_long_press";
    private static final String HARDWARE_KEYS_ASSIST_PRESS = "hardware_keys_assist_press";
    private static final String HARDWARE_KEYS_ASSIST_LONG_PRESS = "hardware_keys_assist_long_press";
    private static final String HARDWARE_KEYS_APP_SWITCH_PRESS = "hardware_keys_app_switch_press";
    private static final String HARDWARE_KEYS_APP_SWITCH_LONG_PRESS = "hardware_keys_app_switch_long_press";
    private static final String HARDWARE_KEYS_SHOW_OVERFLOW = "hardware_keys_show_overflow";

    // Available custom actions to perform on a key press.
    // Must match values for KEY_HOME_LONG_PRESS_ACTION in:
    // frameworks/base/core/java/android/provider/Settings.java
    private static final int ACTION_NOTHING = 0;
    private static final int ACTION_MENU = 1;
    private static final int ACTION_APP_SWITCH = 2;
    private static final int ACTION_SEARCH = 3;
    private static final int ACTION_VOICE_SEARCH = 4;
    private static final int ACTION_IN_APP_SEARCH = 5;

    // Masks for checking presence of hardware keys.
    // Must match values in frameworks/base/core/res/res/values/config.xml
    private static final int KEY_MASK_HOME = 0x01;
    private static final int KEY_MASK_BACK = 0x02;
    private static final int KEY_MASK_MENU = 0x04;
    private static final int KEY_MASK_ASSIST = 0x08;
    private static final int KEY_MASK_APP_SWITCH = 0x10;

    private CheckBoxPreference mEnableCustomBindings;
    private ListPreference mHomeLongPressAction;
    private ListPreference mMenuPressAction;
    private ListPreference mMenuLongPressAction;
    private ListPreference mAssistPressAction;
    private ListPreference mAssistLongPressAction;
    private ListPreference mAppSwitchPressAction;
    private ListPreference mAppSwitchLongPressAction;
    private CheckBoxPreference mShowActionOverflow;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final int deviceKeys = getResources().getInteger(
                com.android.internal.R.integer.config_deviceHardwareKeys);
        final boolean hasHomeKey = (deviceKeys & KEY_MASK_HOME) != 0;
        final boolean hasMenuKey = (deviceKeys & KEY_MASK_MENU) != 0;
        final boolean hasAssistKey = (deviceKeys & KEY_MASK_ASSIST) != 0;
        final boolean hasAppSwitchKey = (deviceKeys & KEY_MASK_APP_SWITCH) != 0;

        addPreferencesFromResource(R.xml.hardware_keys);
        PreferenceScreen prefSet = getPreferenceScreen();

        mEnableCustomBindings = (CheckBoxPreference) prefSet.findPreference(
                HARDWARE_KEYS_ENABLE_CUSTOM);
        mHomeLongPressAction = (ListPreference) prefSet.findPreference(
                HARDWARE_KEYS_HOME_LONG_PRESS);
        mMenuPressAction = (ListPreference) prefSet.findPreference(
                HARDWARE_KEYS_MENU_PRESS);
        mMenuLongPressAction = (ListPreference) prefSet.findPreference(
                HARDWARE_KEYS_MENU_LONG_PRESS);
        mAssistPressAction = (ListPreference) prefSet.findPreference(
                HARDWARE_KEYS_ASSIST_PRESS);
        mAssistLongPressAction = (ListPreference) prefSet.findPreference(
                HARDWARE_KEYS_ASSIST_LONG_PRESS);
        mAppSwitchPressAction = (ListPreference) prefSet.findPreference(
                HARDWARE_KEYS_APP_SWITCH_PRESS);
        mAppSwitchLongPressAction = (ListPreference) prefSet.findPreference(
                HARDWARE_KEYS_APP_SWITCH_LONG_PRESS);
        mShowActionOverflow = (CheckBoxPreference) prefSet.findPreference(
                HARDWARE_KEYS_SHOW_OVERFLOW);
        PreferenceCategory bindingsCategory = (PreferenceCategory) prefSet.findPreference(
                HARDWARE_KEYS_CATEGORY_BINDINGS);

        if (hasHomeKey) {
            int homeLongPressAction;
            if (hasAppSwitchKey) {
                homeLongPressAction = Settings.System.getInt(getContentResolver(),
                        Settings.System.KEY_HOME_LONG_PRESS_ACTION, ACTION_NOTHING);
            } else {
                homeLongPressAction = Settings.System.getInt(getContentResolver(),
                        Settings.System.KEY_HOME_LONG_PRESS_ACTION, ACTION_APP_SWITCH);
            }
            mHomeLongPressAction.setValue(Integer.toString(homeLongPressAction));
            mHomeLongPressAction.setSummary(mHomeLongPressAction.getEntry());
            mHomeLongPressAction.setOnPreferenceChangeListener(this);
        } else {
            bindingsCategory.removePreference(mHomeLongPressAction);
        }

        if (hasMenuKey) {
            int menuPressAction = Settings.System.getInt(getContentResolver(),
                    Settings.System.KEY_MENU_ACTION, ACTION_MENU);
            mMenuPressAction.setValue(Integer.toString(menuPressAction));
            mMenuPressAction.setSummary(mMenuPressAction.getEntry());
            mMenuPressAction.setOnPreferenceChangeListener(this);

            int menuLongPressAction;
            if (hasAssistKey) {
                menuLongPressAction = Settings.System.getInt(getContentResolver(),
                        Settings.System.KEY_MENU_LONG_PRESS_ACTION, ACTION_NOTHING);
            } else {
                menuLongPressAction = Settings.System.getInt(getContentResolver(),
                        Settings.System.KEY_MENU_LONG_PRESS_ACTION, ACTION_SEARCH);
            }
            mMenuLongPressAction.setValue(Integer.toString(menuLongPressAction));
            mMenuLongPressAction.setSummary(mMenuLongPressAction.getEntry());
            mMenuLongPressAction.setOnPreferenceChangeListener(this);
        } else {
            bindingsCategory.removePreference(mMenuPressAction);
            bindingsCategory.removePreference(mMenuLongPressAction);
        }

        if (hasAssistKey) {
            int assistPressAction = Settings.System.getInt(getContentResolver(),
                    Settings.System.KEY_ASSIST_ACTION, ACTION_SEARCH);
            mAssistPressAction.setValue(Integer.toString(assistPressAction));
            mAssistPressAction.setSummary(mAssistPressAction.getEntry());
            mAssistPressAction.setOnPreferenceChangeListener(this);

            int assistLongPressAction = Settings.System.getInt(getContentResolver(),
                    Settings.System.KEY_ASSIST_LONG_PRESS_ACTION, ACTION_VOICE_SEARCH);
            mAssistLongPressAction.setValue(Integer.toString(assistLongPressAction));
            mAssistLongPressAction.setSummary(mAssistLongPressAction.getEntry());
            mAssistLongPressAction.setOnPreferenceChangeListener(this);
        } else {
            bindingsCategory.removePreference(mAssistPressAction);
            bindingsCategory.removePreference(mAssistLongPressAction);
        }

        if (hasAppSwitchKey) {
            int appSwitchPressAction = Settings.System.getInt(getContentResolver(),
                    Settings.System.KEY_APP_SWITCH_ACTION, ACTION_APP_SWITCH);
            mAppSwitchPressAction.setValue(Integer.toString(appSwitchPressAction));
            mAppSwitchPressAction.setSummary(mAppSwitchPressAction.getEntry());
            mAppSwitchPressAction.setOnPreferenceChangeListener(this);

            int appSwitchLongPressAction = Settings.System.getInt(getContentResolver(),
                    Settings.System.KEY_APP_SWITCH_LONG_PRESS_ACTION, ACTION_NOTHING);
            mAppSwitchLongPressAction.setValue(Integer.toString(appSwitchLongPressAction));
            mAppSwitchLongPressAction.setSummary(mAppSwitchLongPressAction.getEntry());
            mAppSwitchLongPressAction.setOnPreferenceChangeListener(this);
        } else {
            bindingsCategory.removePreference(mAppSwitchPressAction);
            bindingsCategory.removePreference(mAppSwitchLongPressAction);
        }

        mEnableCustomBindings.setChecked((Settings.System.getInt(getActivity().
                getApplicationContext().getContentResolver(),
                Settings.System.HARDWARE_KEY_REBINDING, 0) == 1));
        mShowActionOverflow.setChecked((Settings.System.getInt(getActivity().
                getApplicationContext().getContentResolver(),
                Settings.System.UI_FORCE_OVERFLOW_BUTTON, 0) == 1));
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mHomeLongPressAction) {
            int value = Integer.valueOf((String) newValue);
            int index = mHomeLongPressAction.findIndexOfValue((String) newValue);
            mHomeLongPressAction.setSummary(
                    mHomeLongPressAction.getEntries()[index]);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.KEY_HOME_LONG_PRESS_ACTION, value);
            return true;
        } else if (preference == mMenuPressAction) {
            int value = Integer.valueOf((String) newValue);
            int index = mMenuPressAction.findIndexOfValue((String) newValue);
            mMenuPressAction.setSummary(
                    mMenuPressAction.getEntries()[index]);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.KEY_MENU_ACTION, value);
            return true;
        } else if (preference == mMenuLongPressAction) {
            int value = Integer.valueOf((String) newValue);
            int index = mMenuLongPressAction.findIndexOfValue((String) newValue);
            mMenuLongPressAction.setSummary(
                    mMenuLongPressAction.getEntries()[index]);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.KEY_MENU_LONG_PRESS_ACTION, value);
            return true;
        } else if (preference == mAssistPressAction) {
            int value = Integer.valueOf((String) newValue);
            int index = mAssistPressAction.findIndexOfValue((String) newValue);
            mAssistPressAction.setSummary(
                    mAssistPressAction.getEntries()[index]);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.KEY_ASSIST_ACTION, value);
            return true;
        } else if (preference == mAssistLongPressAction) {
            int value = Integer.valueOf((String) newValue);
            int index = mAssistLongPressAction.findIndexOfValue((String) newValue);
            mAssistLongPressAction.setSummary(
                    mAssistLongPressAction.getEntries()[index]);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.KEY_ASSIST_LONG_PRESS_ACTION, value);
            return true;
        } else if (preference == mAppSwitchPressAction) {
            int value = Integer.valueOf((String) newValue);
            int index = mAppSwitchPressAction.findIndexOfValue((String) newValue);
            mAppSwitchPressAction.setSummary(
                    mAppSwitchPressAction.getEntries()[index]);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.KEY_APP_SWITCH_ACTION, value);
            return true;
        } else if (preference == mAppSwitchLongPressAction) {
            int value = Integer.valueOf((String) newValue);
            int index = mAppSwitchLongPressAction.findIndexOfValue((String) newValue);
            mAppSwitchLongPressAction.setSummary(
                    mAppSwitchLongPressAction.getEntries()[index]);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.KEY_APP_SWITCH_LONG_PRESS_ACTION, value);
            return true;
        }
        return false;
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mEnableCustomBindings) {
            Settings.System.putInt(getContentResolver(), Settings.System.HARDWARE_KEY_REBINDING,
                    mEnableCustomBindings.isChecked() ? 1 : 0);
            return true;
        } else if (preference == mShowActionOverflow) {
            boolean enabled = mShowActionOverflow.isChecked();
            Settings.System.putInt(getContentResolver(), Settings.System.UI_FORCE_OVERFLOW_BUTTON,
                    enabled ? 1 : 0);
            // Show appropriate
            if (enabled) {
                Toast.makeText(getActivity(), R.string.hardware_keys_show_overflow_toast_enable,
                        Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getActivity(), R.string.hardware_keys_show_overflow_toast_disable,
                        Toast.LENGTH_LONG).show();
            }
            return true;
        }
        return false;
    }
}
