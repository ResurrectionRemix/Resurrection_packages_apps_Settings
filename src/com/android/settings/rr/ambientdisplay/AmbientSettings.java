/*
 *  Copyright (C) 2015 The OmniROM Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.android.settings.rr.ambientdisplay;

import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.database.ContentObserver;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemProperties;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;
import android.preference.PreferenceCategory;
import android.preference.SwitchPreference;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.text.TextUtils;
import android.widget.Button;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.cyanogenmod.SystemSettingSwitchPreference;
import com.android.settings.rr.ambientdisplay.ShakeSensorManager;

import java.util.ArrayList;
import java.util.List;

public class AmbientSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, OnPreferenceClickListener,
        Indexable, ShakeSensorManager.ShakeListener {
    private static final String TAG = "AmbientSettings";

    private static final String KEY_DOZE = "doze";
    private static final String KEY_DOZE_OVERWRITE_VALUE = "doze_overwrite_value";
    private static final String KEY_DOZE_PULSE_IN = "doze_pulse_in";
    private static final String KEY_DOZE_PULSE_VISIBLE = "doze_pulse_visible";
    private static final String KEY_DOZE_PULSE_OUT = "doze_pulse_out";
    private static final String KEY_DOZE_LIST_MODE = "doze_list_mode";
    private static final String KEY_DOZE_PULSE_MODE = "doze_pulse_on_notifications";
    private static final String KEY_DOZE_SHAKE_CATEGORY = "doze_shake_category";
    private static final String KEY_DOZE_SHAKE_THRESHOLD = "doze_shake_threshold";
    private static final String KEY_DOZE_TIME_MODE = "doze_time_mode";

    private int mAccValue;
    private int mOldAccValue;
    private SwitchPreference mDozePreference;
    private ListPreference mDozeListMode;
    private ListPreference mDozePulseIn;
    private ListPreference mDozePulseVisible;
    private ListPreference mDozePulseOut;
    private ListPreference mDozeShakeThreshold;
    private SystemSettingSwitchPreference mDozeTimeMode;
    private ShakeSensorManager mShakeSensorManager;
    private AlertDialog mDialog;
    private Button mShakeFoundButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Activity activity = getActivity();

        addPreferencesFromResource(R.xml.ambient_settings);

        mDozePreference = (SwitchPreference) findPreference(KEY_DOZE);
        mDozePreference.setOnPreferenceChangeListener(this);

        mDozePulseIn = (ListPreference) findPreference(KEY_DOZE_PULSE_IN);
        mDozePulseIn.setOnPreferenceChangeListener(this);

        mDozePulseVisible = (ListPreference) findPreference(KEY_DOZE_PULSE_VISIBLE);
        mDozePulseVisible.setOnPreferenceChangeListener(this);

        mDozePulseOut = (ListPreference) findPreference(KEY_DOZE_PULSE_OUT);
        mDozePulseOut.setOnPreferenceChangeListener(this);

        if (isAccelerometerAvailable(activity)) {
            mDozeListMode = (ListPreference) findPreference(KEY_DOZE_LIST_MODE);
            mDozeListMode.setOnPreferenceChangeListener(this);

            mDozeTimeMode = (SystemSettingSwitchPreference) findPreference(KEY_DOZE_TIME_MODE);

            mDozeShakeThreshold = (ListPreference) findPreference(KEY_DOZE_SHAKE_THRESHOLD);
            mDozeShakeThreshold.setOnPreferenceChangeListener(this);

            removePreference(KEY_DOZE_PULSE_MODE);
        } else {
            removePreference(KEY_DOZE_LIST_MODE);
            removePreference(KEY_DOZE_TIME_MODE);
            removePreference(KEY_DOZE_SHAKE_THRESHOLD);
            removePreference(KEY_DOZE_SHAKE_CATEGORY);
        }
        updateDozeListMode();
        updateDozeOptions();
        mShakeSensorManager = new ShakeSensorManager(activity, this);
    }

    private static boolean isAccelerometerAvailable(Context context) {
        boolean useAccelerometer = context.getResources().getBoolean(
                com.android.internal.R.bool.config_dozeUseAccelerometer);
        return useAccelerometer;
    }

    private static boolean isDozeAvailable(Context context) {
        String name = Build.IS_DEBUGGABLE ? SystemProperties.get("debug.doze.component") : null;
        if (TextUtils.isEmpty(name)) {
            name = context.getResources().getString(
                    com.android.internal.R.string.config_dozeComponent);
        }
        return !TextUtils.isEmpty(name);
    }

    private void updateDozeListMode() {
        final boolean pocketMode = Settings.System.getInt(getContentResolver(),
                   Settings.System.DOZE_POCKET_MODE, 0) != 0;
        final boolean shakeMode = Settings.System.getInt(getContentResolver(),
                   Settings.System.DOZE_SHAKE_MODE, 0) != 0;
        final boolean pulseMode = Settings.System.getInt(getContentResolver(),
                   Settings.System.DOZE_PULSE_ON_NOTIFICATIONS, 1) != 0;
        int listMode = 6;
        if (pocketMode && shakeMode && pulseMode) {
            listMode = 1;
            enableShakeThreshold(true);
        } else if (pocketMode && shakeMode && !pulseMode) {
            listMode = 2;
            enableShakeThreshold(true);
        } else if (pocketMode && !shakeMode && pulseMode) {
            listMode = 3;
            enableShakeThreshold(false);
        } else if (pocketMode && !shakeMode && !pulseMode) {
            listMode = 4;
            enableShakeThreshold(false);
        } else if (!pocketMode && shakeMode && pulseMode) {
            listMode = 5;
            enableShakeThreshold(true);
        } else if (!pocketMode && !shakeMode && pulseMode) {
            listMode = 6;
            enableShakeThreshold(false);
        }
        enabledTimeMode(pocketMode);
        if (mDozeListMode != null) {
            mDozeListMode.setValue(String.valueOf(listMode));
            int index = mDozeListMode.findIndexOfValue(String.valueOf(listMode));
            if (index != -1) {
                mDozeListMode.setSummary(mDozeListMode.getEntries()[index]);
            }
        }
    }

    private void enabledTimeMode(boolean enable) {
        if (mDozeTimeMode != null) {
            mDozeTimeMode.setEnabled(enable);
        }
    }

    private void showAccDialog() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
        alertDialog.setTitle(R.string.doze_shake_mode_title);
        alertDialog.setMessage(R.string.doze_shake_mode_test_summary);
        alertDialog.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                startAcctest();
            }
        });
        alertDialog.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                stopAcctest();
            }
        });
        mDialog = alertDialog.create();
        mDialog.show();
    }

    @Override
    public synchronized void onShake() {
        String msg1 = getResources().getString(R.string.doze_shake_it);
        String msg2 = getResources().getString(R.string.doze_shake_mode_test_result);

        String msg = msg1 + "\n" + msg2;
        mDialog.setMessage(msg);
        Button shakeCancelButton = mDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        if (shakeCancelButton != null) {
            shakeCancelButton.setText(R.string.no);
        }
        if (mShakeFoundButton != null) {
            mShakeFoundButton.setEnabled(true);
        }
        mShakeSensorManager.disable();
    }

    private void startAcctest() {
        mShakeSensorManager.enable(mAccValue);
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
        alertDialog.setTitle(R.string.doze_shake_mode_title);
        alertDialog.setMessage(R.string.doze_shake_test);
        alertDialog.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                stopAcctest();
            }
        });
        alertDialog.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                revertAcctest();
            }
        });
        mDialog = alertDialog.create();
        mDialog.show();

        mShakeFoundButton = mDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        if (mShakeFoundButton != null) {
            mShakeFoundButton.setEnabled(false);
        }
    }

    private void revertAcctest() {
        mAccValue = mOldAccValue;
        mDozeShakeThreshold.setValue(String.valueOf(mOldAccValue));
        mShakeSensorManager.disable();
    }

    private void stopAcctest() {
        appliedAccTest();
        mShakeSensorManager.disable();
    }

    private void appliedAccTest() {
        Settings.System.putInt(getContentResolver(),
                    Settings.System.DOZE_SHAKE_ACC_THRESHOLD, mAccValue);
        mDozeShakeThreshold.setValue(String.valueOf(mAccValue));
        int index = mDozeShakeThreshold.findIndexOfValue(String.valueOf(mAccValue));
        if (index != -1) {
            mDozeShakeThreshold.setSummary(mDozeShakeThreshold.getEntries()[index]);
        }
    }

    private void enableShakeThreshold(boolean enabled) {
        final boolean writeMode = Settings.System.getInt(getContentResolver(),
                   Settings.System.DOZE_OVERWRITE_VALUE, 0) != 0;
        if (mDozeShakeThreshold != null) {
            mDozeShakeThreshold.setEnabled(enabled && writeMode);
        }
    }

    private void updateDozeListModeValue(int listMode) {
        if (listMode == 1) {
            enableShakeThreshold(true);
            enabledTimeMode(true);
            Settings.System.putInt(getContentResolver(),
                   Settings.System.DOZE_POCKET_MODE, 1);
            Settings.System.putInt(getContentResolver(),
                   Settings.System.DOZE_SHAKE_MODE, 1);
            Settings.System.putInt(getContentResolver(),
                   Settings.System.DOZE_PULSE_ON_NOTIFICATIONS, 1);
        } else if (listMode == 2) {
            enableShakeThreshold(true);
            enabledTimeMode(true);
            Settings.System.putInt(getContentResolver(),
                   Settings.System.DOZE_POCKET_MODE, 1);
            Settings.System.putInt(getContentResolver(),
                   Settings.System.DOZE_SHAKE_MODE, 1);
            Settings.System.putInt(getContentResolver(),
                   Settings.System.DOZE_PULSE_ON_NOTIFICATIONS, 0);
        } else if (listMode == 3) {
            enableShakeThreshold(false);
            enabledTimeMode(true);
            Settings.System.putInt(getContentResolver(),
                   Settings.System.DOZE_POCKET_MODE, 1);
            Settings.System.putInt(getContentResolver(),
                   Settings.System.DOZE_SHAKE_MODE, 0);
            Settings.System.putInt(getContentResolver(),
                   Settings.System.DOZE_PULSE_ON_NOTIFICATIONS, 1);
        } else if (listMode == 4) {
            enableShakeThreshold(false);
            enabledTimeMode(true);
            Settings.System.putInt(getContentResolver(),
                   Settings.System.DOZE_POCKET_MODE, 1);
            Settings.System.putInt(getContentResolver(),
                   Settings.System.DOZE_SHAKE_MODE, 0);
            Settings.System.putInt(getContentResolver(),
                   Settings.System.DOZE_PULSE_ON_NOTIFICATIONS, 0);
        } else if (listMode == 5) {
            enableShakeThreshold(true);
            enabledTimeMode(false);
            Settings.System.putInt(getContentResolver(),
                   Settings.System.DOZE_POCKET_MODE, 0);
            Settings.System.putInt(getContentResolver(),
                   Settings.System.DOZE_SHAKE_MODE, 1);
            Settings.System.putInt(getContentResolver(),
                   Settings.System.DOZE_PULSE_ON_NOTIFICATIONS, 1);
        } else if (listMode == 6) {
            enableShakeThreshold(false);
            enabledTimeMode(false);
            Settings.System.putInt(getContentResolver(),
                   Settings.System.DOZE_POCKET_MODE, 0);
            Settings.System.putInt(getContentResolver(),
                   Settings.System.DOZE_SHAKE_MODE, 0);
            Settings.System.putInt(getContentResolver(),
                   Settings.System.DOZE_PULSE_ON_NOTIFICATIONS, 1);
        }
    }

    private void updateDozeOptions() {
        if (mDozePulseIn != null) {
            final int statusDozePulseIn = Settings.System.getInt(getContentResolver(),
                    Settings.System.DOZE_PULSE_DURATION_IN, 1000);
            mDozePulseIn.setValue(String.valueOf(statusDozePulseIn));
            int index = mDozePulseIn.findIndexOfValue(String.valueOf(statusDozePulseIn));
            if (index != -1) {
                mDozePulseIn.setSummary(mDozePulseIn.getEntries()[index]);
            }
        }
        if (mDozePulseVisible != null) {
            final int statusDozePulseVisible = Settings.System.getInt(getContentResolver(),
                    Settings.System.DOZE_PULSE_DURATION_VISIBLE, 3000);
            mDozePulseVisible.setValue(String.valueOf(statusDozePulseVisible));
            int index = mDozePulseVisible.findIndexOfValue(String.valueOf(statusDozePulseVisible));
            if (index != -1) {
                mDozePulseVisible.setSummary(mDozePulseVisible.getEntries()[index]);
            }
        }
        if (mDozePulseOut != null) {
            final int statusDozePulseOut = Settings.System.getInt(getContentResolver(),
                    Settings.System.DOZE_PULSE_DURATION_OUT, 1000);
            mDozePulseOut.setValue(String.valueOf(statusDozePulseOut));
            int index = mDozePulseOut.findIndexOfValue(String.valueOf(statusDozePulseOut));
            if (index != -1) {
               mDozePulseOut.setSummary(mDozePulseOut.getEntries()[index]);
            }
        }
        if (mDozeShakeThreshold != null) {
            mAccValue = Settings.System.getInt(getContentResolver(),
                    Settings.System.DOZE_SHAKE_ACC_THRESHOLD, 10);
            mDozeShakeThreshold.setValue(String.valueOf(mAccValue));
            int index = mDozeShakeThreshold.findIndexOfValue(String.valueOf(mAccValue));
            if (index != -1) {
                mDozeShakeThreshold.setSummary(mDozeShakeThreshold.getEntries()[index]);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateState();
        updateDozeOptions();
        updateDozeListMode();
    }

    @Override
    public void onPause() {
        super.onPause();
        mShakeSensorManager.disable();
        if (mDialog != null) {
            mDialog.dismiss();
        }
    }

    private void updateState() {
        // Update doze if it is available.
        if (mDozePreference != null) {
            int value = Settings.Secure.getInt(getContentResolver(), Settings.Secure.DOZE_ENABLED, 1);
            mDozePreference.setChecked(value != 0);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == findPreference(KEY_DOZE_OVERWRITE_VALUE)) {
            updateDozeListMode();
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final String key = preference.getKey();
        if (preference == mDozePreference) {
            boolean value = (Boolean) objValue;
            Settings.Secure.putInt(getContentResolver(), Settings.Secure.DOZE_ENABLED, value ? 1 : 0);
        }
        if (preference == mDozePulseIn) {
            int dozePulseIn = Integer.parseInt((String)objValue);
            int index = mDozePulseIn.findIndexOfValue((String) objValue);
            mDozePulseIn.setSummary(mDozePulseIn.getEntries()[index]);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.DOZE_PULSE_DURATION_IN, dozePulseIn);
        }
        if (preference == mDozePulseVisible) {
            int dozePulseVisible = Integer.parseInt((String)objValue);
            int index = mDozePulseVisible.findIndexOfValue((String) objValue);
            mDozePulseVisible.setSummary(mDozePulseVisible.getEntries()[index]);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.DOZE_PULSE_DURATION_VISIBLE, dozePulseVisible);
        }
        if (preference == mDozePulseOut) {
            int dozePulseOut = Integer.parseInt((String)objValue);
            int index = mDozePulseOut.findIndexOfValue((String) objValue);
            mDozePulseOut.setSummary(mDozePulseOut.getEntries()[index]);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.DOZE_PULSE_DURATION_OUT, dozePulseOut);
        }
        if (preference == mDozeShakeThreshold) {
            int accValue = Integer.parseInt((String)objValue);
            if (accValue != mAccValue) {
                mOldAccValue = mAccValue;
                mAccValue = accValue;
                showAccDialog();
            }
        }
        if (preference == mDozeListMode) {
            int dozeListMode = Integer.parseInt((String)objValue);
            updateDozeListModeValue(dozeListMode);
            int index = mDozeListMode.findIndexOfValue((String) objValue);
            mDozeListMode.setSummary(mDozeListMode.getEntries()[index]);
        }
        return true;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        return false;
    }

    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                        boolean enabled) {
                    ArrayList<SearchIndexableResource> result =
                            new ArrayList<SearchIndexableResource>();

                    SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.ambient_settings;
                    result.add(sir);

                    return result;
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    ArrayList<String> result = new ArrayList<String>();
                    if (!isDozeAvailable(context)) {
                        result.add(KEY_DOZE);
                        result.add(KEY_DOZE_LIST_MODE);
                        result.add(KEY_DOZE_TIME_MODE);
                        result.add(KEY_DOZE_OVERWRITE_VALUE);
                        result.add(KEY_DOZE_PULSE_MODE);
                        result.add(KEY_DOZE_PULSE_IN);
                        result.add(KEY_DOZE_PULSE_VISIBLE);
                        result.add(KEY_DOZE_PULSE_OUT);
                        result.add(KEY_DOZE_SHAKE_THRESHOLD);
                    } else {
                       if (!isAccelerometerAvailable(context)) {
                           result.add(KEY_DOZE_LIST_MODE);
                           result.add(KEY_DOZE_SHAKE_THRESHOLD);
                       }
                       if (isAccelerometerAvailable(context)) {
                           result.add(KEY_DOZE_PULSE_MODE);
                       }
                    }
                    return result;
                }
            };
}
