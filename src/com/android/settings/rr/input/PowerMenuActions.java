/*
 * Copyright (C) 2014-2015 The CyanogenMod Project
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

package com.android.settings.rr.input;

import android.content.Context;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.internal.util.cm.PowerMenuConstants;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import com.android.internal.logging.MetricsProto.MetricsEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cyanogenmod.providers.CMSettings;

import static com.android.internal.util.cm.PowerMenuConstants.*;

public class PowerMenuActions extends SettingsPreferenceFragment  implements OnPreferenceChangeListener {
    final static String TAG = "PowerMenuActions";

    private static final String POWER_MENU_ONTHEGO_ENABLED = "power_menu_onthego_enabled";

    private CheckBoxPreference mRebootPref;
    private CheckBoxPreference mScreenshotPref;
    private CheckBoxPreference mAirplanePref;
    private CheckBoxPreference mUsersPref;
    private CheckBoxPreference mSettingsPref;
    private CheckBoxPreference mLockdownPref;
    private CheckBoxPreference mBugReportPref;
    private CheckBoxPreference mSilentPref;
    private CheckBoxPreference mVoiceAssistPref;
    private CheckBoxPreference mAssistPref;
	private CheckBoxPreference mScreenrecordPref;
	private CheckBoxPreference mFlashlightPref;
	private CheckBoxPreference mOnTheGoPowerMenu;
	private CheckBoxPreference mEmergencyPref;

    Context mContext;
    private ArrayList<String> mLocalUserConfig = new ArrayList<String>();
    private String[] mAvailableActions;
    private String[] mAllActions;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.power_menu_settings);
        mContext = getActivity().getApplicationContext();

        mAvailableActions = getActivity().getResources().getStringArray(
                R.array.power_menu_actions_array);
        mAllActions = PowerMenuConstants.getAllActions();

        for (String action : mAllActions) {
        // Remove preferences not present in the overlay
            if (!isActionAllowed(action)) {
                getPreferenceScreen().removePreference(findPreference(action));
                continue;
            }

            if (action.equals(GLOBAL_ACTION_KEY_RESTART)) {
                mRebootPref = (CheckBoxPreference) findPreference(GLOBAL_ACTION_KEY_RESTART);
            } else if (action.equals(GLOBAL_ACTION_KEY_SCREENSHOT)) {
                mScreenshotPref = (CheckBoxPreference) findPreference(GLOBAL_ACTION_KEY_SCREENSHOT);
            } else if (action.equals(GLOBAL_ACTION_KEY_AIRPLANE)) {
                mAirplanePref = (CheckBoxPreference) findPreference(GLOBAL_ACTION_KEY_AIRPLANE);
            } else if (action.equals(GLOBAL_ACTION_KEY_USERS)) {
                mUsersPref = (CheckBoxPreference) findPreference(GLOBAL_ACTION_KEY_USERS);
            } else if (action.equals(GLOBAL_ACTION_KEY_SETTINGS)) {
                mSettingsPref = (CheckBoxPreference) findPreference(GLOBAL_ACTION_KEY_SETTINGS);
            } else if (action.equals(GLOBAL_ACTION_KEY_LOCKDOWN)) {
                mLockdownPref = (CheckBoxPreference) findPreference(GLOBAL_ACTION_KEY_LOCKDOWN);
            } else if (action.equals(GLOBAL_ACTION_KEY_BUGREPORT)) {
                mBugReportPref = (CheckBoxPreference) findPreference(GLOBAL_ACTION_KEY_BUGREPORT);
            } else if (action.equals(GLOBAL_ACTION_KEY_SILENT)) {
                mSilentPref = (CheckBoxPreference) findPreference(GLOBAL_ACTION_KEY_SILENT);
            } else if (action.equals(GLOBAL_ACTION_KEY_VOICEASSIST)) {
                mSilentPref = (CheckBoxPreference) findPreference(GLOBAL_ACTION_KEY_VOICEASSIST);
            } else if (action.equals(GLOBAL_ACTION_KEY_ASSIST)) {
                mSilentPref = (CheckBoxPreference) findPreference(GLOBAL_ACTION_KEY_ASSIST);
            }  else if (action.equals(GLOBAL_ACTION_KEY_TORCH)) {
                mFlashlightPref = (CheckBoxPreference) findPreference(GLOBAL_ACTION_KEY_TORCH);
			} else if (action.equals(GLOBAL_ACTION_KEY_SCREENRECORD)) {
                mScreenrecordPref = (CheckBoxPreference) findPreference(GLOBAL_ACTION_KEY_SCREENRECORD);
			} else if  (action.equals(GLOBAL_ACTION_KEY_EMERGENCY)) {
                mEmergencyPref = (CheckBoxPreference) findPreference(GLOBAL_ACTION_KEY_EMERGENCY);
			}

        mOnTheGoPowerMenu = (CheckBoxPreference) findPreference(POWER_MENU_ONTHEGO_ENABLED);
        mOnTheGoPowerMenu.setChecked((Settings.System.getInt(getContentResolver(),
                Settings.System.POWER_MENU_ONTHEGO_ENABLED, 0) == 1));
        mOnTheGoPowerMenu.setOnPreferenceChangeListener(this);
        }

        getUserConfig();
    }

    @Override
    public void onStart() {
        super.onStart();

        if (mRebootPref != null) {
            mRebootPref.setChecked(settingsArrayContains(GLOBAL_ACTION_KEY_RESTART));
        }

        if (mScreenshotPref != null) {
            mScreenshotPref.setChecked(settingsArrayContains(GLOBAL_ACTION_KEY_SCREENSHOT));
        }

        if (mScreenrecordPref != null) {
            mScreenrecordPref.setChecked(settingsArrayContains(GLOBAL_ACTION_KEY_SCREENRECORD));
		}

        if (mAirplanePref != null) {
            mAirplanePref.setChecked(settingsArrayContains(GLOBAL_ACTION_KEY_AIRPLANE));
        }

        if (mUsersPref != null) {
            if (!UserHandle.MU_ENABLED || !UserManager.supportsMultipleUsers()) {
                getPreferenceScreen().removePreference(findPreference(GLOBAL_ACTION_KEY_USERS));
                mUsersPref = null;
            } else {
                List<UserInfo> users = ((UserManager) mContext.getSystemService(
                        Context.USER_SERVICE)).getUsers();
                boolean enabled = (users.size() > 1);
                mUsersPref.setChecked(settingsArrayContains(GLOBAL_ACTION_KEY_USERS) && enabled);
                mUsersPref.setEnabled(enabled);
            }
        }

        if (mSettingsPref != null) {
            mSettingsPref.setChecked(settingsArrayContains(GLOBAL_ACTION_KEY_SETTINGS));
        }

        if (mLockdownPref != null) {
            mLockdownPref.setChecked(settingsArrayContains(GLOBAL_ACTION_KEY_LOCKDOWN));
        }

        if (mBugReportPref != null) {
            mBugReportPref.setChecked(settingsArrayContains(GLOBAL_ACTION_KEY_BUGREPORT));
        }

        if (mSilentPref != null) {
            mSilentPref.setChecked(settingsArrayContains(GLOBAL_ACTION_KEY_SILENT));
        }

        if (mVoiceAssistPref != null) {
            mVoiceAssistPref.setChecked(settingsArrayContains(GLOBAL_ACTION_KEY_VOICEASSIST));
        }

        if (mAssistPref != null) {
            mAssistPref.setChecked(settingsArrayContains(GLOBAL_ACTION_KEY_ASSIST));
        }

        if (mFlashlightPref != null) {
            mFlashlightPref.setChecked(settingsArrayContains(GLOBAL_ACTION_KEY_TORCH));
		}

        if (mEmergencyPref != null) {
            mEmergencyPref.setChecked(settingsArrayContains(GLOBAL_ACTION_KEY_EMERGENCY));
        }

        updatePreferences();
    }

    @Override
    public void onResume() {
        super.onResume();
        updatePreferences();
    }


     @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        ContentResolver resolver = getActivity().getContentResolver();
        if (preference == mOnTheGoPowerMenu) {
            boolean value = ((Boolean)newValue).booleanValue();
            Settings.System.putInt(getContentResolver(), Settings.System.POWER_MENU_ONTHEGO_ENABLED, value ? 1 : 0);
            return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        boolean value;

        if (preference == mRebootPref) {
            value = mRebootPref.isChecked();
            updateUserConfig(value, GLOBAL_ACTION_KEY_RESTART);

        } else if (preference == mScreenshotPref) {
            value = mScreenshotPref.isChecked();
            updateUserConfig(value, GLOBAL_ACTION_KEY_SCREENSHOT);

        } else if (preference == mAirplanePref) {
            value = mAirplanePref.isChecked();
            updateUserConfig(value, GLOBAL_ACTION_KEY_AIRPLANE);

        } else if (preference == mUsersPref) {
            value = mUsersPref.isChecked();
            updateUserConfig(value, GLOBAL_ACTION_KEY_USERS);

        } else if (preference == mSettingsPref) {
            value = mSettingsPref.isChecked();
            updateUserConfig(value, GLOBAL_ACTION_KEY_SETTINGS);

        } else if (preference == mLockdownPref) {
            value = mLockdownPref.isChecked();
            updateUserConfig(value, GLOBAL_ACTION_KEY_LOCKDOWN);

        } else if (preference == mBugReportPref) {
            value = mBugReportPref.isChecked();
            updateUserConfig(value, GLOBAL_ACTION_KEY_BUGREPORT);

        } else if (preference == mSilentPref) {
            value = mSilentPref.isChecked();
            updateUserConfig(value, GLOBAL_ACTION_KEY_SILENT);

        } else if (preference == mVoiceAssistPref) {
            value = mVoiceAssistPref.isChecked();
            updateUserConfig(value, GLOBAL_ACTION_KEY_VOICEASSIST);

        } else if (preference == mAssistPref) {
            value = mAssistPref.isChecked();
            updateUserConfig(value, GLOBAL_ACTION_KEY_ASSIST);

        } else if (preference == mFlashlightPref) {
            value = mFlashlightPref.isChecked();
            updateUserConfig(value, GLOBAL_ACTION_KEY_TORCH);

		}  else if (preference == mScreenrecordPref) {
            value = mScreenrecordPref.isChecked();
            updateUserConfig(value, GLOBAL_ACTION_KEY_SCREENRECORD);

		}  else if (preference == mEmergencyPref) {
            value = mEmergencyPref.isChecked();
            updateUserConfig(value, GLOBAL_ACTION_KEY_EMERGENCY);

		}  else {
            return super.onPreferenceTreeClick(preference);
        }
        return true;
    }

    private boolean settingsArrayContains(String preference) {
        return mLocalUserConfig.contains(preference);
    }

    private boolean isActionAllowed(String action) {
        if (Arrays.asList(mAvailableActions).contains(action)) {
            return true;
        }
        return false;
    }

    private void updateUserConfig(boolean enabled, String action) {
        if (enabled) {
            if (!settingsArrayContains(action)) {
                mLocalUserConfig.add(action);
            }
        } else {
            if (settingsArrayContains(action)) {
                mLocalUserConfig.remove(action);
            }
        }
        saveUserConfig();
    }

    private void updatePreferences() {
        boolean bugreport = Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.BUGREPORT_IN_POWER_MENU, 0) != 0;

        if (mBugReportPref != null) {
            mBugReportPref.setEnabled(bugreport);
            if (bugreport) {
                mBugReportPref.setSummary(null);
            } else {
                mBugReportPref.setSummary(R.string.power_menu_bug_report_disabled);
            }
        }
    }

    private void getUserConfig() {
        mLocalUserConfig.clear();
        String[] defaultActions;
        String savedActions = CMSettings.Secure.getStringForUser(mContext.getContentResolver(),
                CMSettings.Secure.POWER_MENU_ACTIONS, UserHandle.USER_CURRENT);

        if (savedActions == null) {
            defaultActions = mContext.getResources().getStringArray(
                    com.android.internal.R.array.config_globalActionsList);
            for (String action : defaultActions) {
                mLocalUserConfig.add(action);
            }
        } else {
            for (String action : savedActions.split("\\|")) {
                mLocalUserConfig.add(action);
            }
        }
    }

    private void saveUserConfig() {
        StringBuilder s = new StringBuilder();

        // TODO: Use DragSortListView
        ArrayList<String> setactions = new ArrayList<String>();
        for (String action : mAllActions) {
            if (settingsArrayContains(action) && isActionAllowed(action)) {
                setactions.add(action);
            } else {
                continue;
            }
        }

        for (int i = 0; i < setactions.size(); i++) {
            s.append(setactions.get(i).toString());
            if (i != setactions.size() - 1) {
                s.append("|");
            }
        }

        CMSettings.Secure.putStringForUser(getContentResolver(),
                CMSettings.Secure.POWER_MENU_ACTIONS, s.toString(), UserHandle.USER_CURRENT);
        updatePowerMenuDialog();
    }

    private void updatePowerMenuDialog() {
        Intent u = new Intent();
        u.setAction(Intent.UPDATE_POWER_MENU);
        mContext.sendBroadcastAsUser(u, UserHandle.ALL);
    }


    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.RESURRECTED;
	}
}
