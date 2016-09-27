/*
 *  Copyright (C) 2015 The OmniROM Project
 *  Copyright (C) 2015 The SlimROM Project
 *  Copyright (C) 2016 The Pure Nexus Project
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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v14.preference.SwitchPreference;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class AmbientSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "AmbientSettings";

    private static final String KEY_DOZE = "doze";
    private static final String KEY_DOZE_OVERWRITE_VALUE = "doze_overwrite_value";
    private static final String KEY_DOZE_PULSE_IN = "doze_pulse_in";
    private static final String KEY_DOZE_PULSE_VISIBLE = "doze_pulse_visible";
    private static final String KEY_DOZE_PULSE_OUT = "doze_pulse_out";
    private static final String KEY_DOZE_PULSE_PICKUP = "doze_pulse_on_pickup";
    private static final String KEY_DOZE_PULSE_ON_NOTIFICATION = "doze_pulse_on_notifications";
    private static final String KEY_DOZE_BRIGHTNESS_LEVEL = "doze_brightness_level";

    private static final String SYSTEMUI_METADATA_NAME = "com.android.systemui";

    private SwitchPreference mDozePreference;
    private ListPreference mDozePulseIn;
    private ListPreference mDozePulseVisible;
    private ListPreference mDozePulseOut;
    private SwitchPreference mDozeTriggerPickup;
    private SwitchPreference mDozePulseOnNotification;

    private DozeBrightnessDialog mDozeBrightnessDialog;
    private Preference mDozeBrightness;

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.RESURRECTED;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Activity activity = getActivity();
        ContentResolver resolver = getActivity().getContentResolver();

        addPreferencesFromResource(R.xml.ambient_settings);

        mDozePreference = (SwitchPreference) findPreference(KEY_DOZE);
        mDozePreference.setOnPreferenceChangeListener(this);

        mDozePulseIn = (ListPreference) findPreference(KEY_DOZE_PULSE_IN);
        mDozePulseIn.setOnPreferenceChangeListener(this);

        mDozePulseVisible = (ListPreference) findPreference(KEY_DOZE_PULSE_VISIBLE);
        mDozePulseVisible.setOnPreferenceChangeListener(this);

        mDozePulseOut = (ListPreference) findPreference(KEY_DOZE_PULSE_OUT);
        mDozePulseOut.setOnPreferenceChangeListener(this);

        mDozeTriggerPickup = (SwitchPreference) findPreference(KEY_DOZE_PULSE_PICKUP);
        mDozeTriggerPickup.setOnPreferenceChangeListener(this);

        mDozePulseOnNotification = (SwitchPreference) findPreference(KEY_DOZE_PULSE_ON_NOTIFICATION);
        mDozePulseOnNotification.setOnPreferenceChangeListener(this);

        updateDozeOptions();

        mDozeBrightness = (Preference) findPreference(KEY_DOZE_BRIGHTNESS_LEVEL);
    }


    private void updateDozeOptions() {
        if (mDozePulseIn != null) {
            final int statusDozePulseIn = Settings.System.getInt(getContentResolver(),
                    Settings.System.DOZE_PULSE_DURATION_IN, 500);
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
                    Settings.System.DOZE_PULSE_DURATION_OUT, 500);
            mDozePulseOut.setValue(String.valueOf(statusDozePulseOut));
            int index = mDozePulseOut.findIndexOfValue(String.valueOf(statusDozePulseOut));
            if (index != -1) {
               mDozePulseOut.setSummary(mDozePulseOut.getEntries()[index]);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateState();
        updateDozeOptions();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    private void updateState() {
        // Update doze if it is available.
        if (mDozePreference != null) {
            int value = Settings.Secure.getInt(getContentResolver(), Settings.Secure.DOZE_ENABLED,
                    getActivity().getResources().getBoolean(
                    com.android.internal.R.bool.config_doze_enabled_by_default) ? 1 : 0);
            mDozePreference.setChecked(value != 0);
        }
        if (mDozeTriggerPickup != null) {
            int value = Settings.System.getInt(getContentResolver(),
                    Settings.System.DOZE_PULSE_ON_PICKUP, 0);
            mDozeTriggerPickup.setChecked(value != 0);
        }
        if (mDozePulseOnNotification != null) {
            int value = Settings.System.getInt(getContentResolver(),
                    Settings.System.DOZE_PULSE_ON_NOTIFICATIONS, 0);
            mDozePulseOnNotification.setChecked(value != 0);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
       if (preference == mDozeBrightness) {
            showDozeBrightnessDialog();
            return true;
        }
        return super.onPreferenceTreeClick(preference);
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
        if (preference == mDozeTriggerPickup) {
            boolean value = (Boolean) objValue;
            Settings.System.putInt(getContentResolver(),
                    Settings.System.DOZE_PULSE_ON_PICKUP, value ? 1 : 0);
        }
        if (preference == mDozePulseOnNotification) {
            boolean value = (Boolean) objValue;
            Settings.System.putInt(getContentResolver(),
                    Settings.System.DOZE_PULSE_ON_NOTIFICATIONS, value ? 1 : 0);
        }
        return true;
    }

    private static boolean isPickupSensorUsedByDefault(Context context) {
        return getConfigBoolean(context, "doze_pulse_on_pick_up");
    }

    private static Boolean getConfigBoolean(Context context, String configBooleanName) {
        int resId = -1;
        Boolean b = true;
        PackageManager pm = context.getPackageManager();
        if (pm == null) {
            return null;
        }

        Resources systemUiResources;
        try {
            systemUiResources = pm.getResourcesForApplication(SYSTEMUI_METADATA_NAME);
        } catch (Exception e) {
            Log.e("DozeSettings:", "can't access systemui resources",e);
            return null;
        }

        resId = systemUiResources.getIdentifier(
            SYSTEMUI_METADATA_NAME + ":bool/" + configBooleanName, null, null);
        if (resId > 0) {
            b = systemUiResources.getBoolean(resId);
        }
        return b;
    }

    private void showDozeBrightnessDialog() {
        if (mDozeBrightnessDialog != null && mDozeBrightnessDialog.isShowing()) {
            return;
        }

        mDozeBrightnessDialog = new DozeBrightnessDialog(getActivity());
        mDozeBrightnessDialog.show();
    }

    private class DozeBrightnessDialog extends AlertDialog implements DialogInterface.OnClickListener {

        private SeekBar mBacklightBar;
        private EditText mBacklightInput;
        private int mCurrentBrightness;
        private int mMaxBrightness;

        public DozeBrightnessDialog(Context context) {
            super(context);
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            final View v = getLayoutInflater().inflate(R.layout.dialog_doze_brightness, null);
            final Context context = getContext();

            mBacklightBar = (SeekBar) v.findViewById(R.id.doze_seek);
            mBacklightInput = (EditText) v.findViewById(R.id.doze_input);

            setTitle(R.string.doze_brightness_level_title);
            setCancelable(true);
            setView(v);

            final int dozeBrightnessConfig = getResources().getInteger(
                    com.android.internal.R.integer.config_screenBrightnessDoze);
            mCurrentBrightness = Settings.System.getInt(getContentResolver(),
                    Settings.System.DOZE_SCREEN_BRIGHTNESS, dozeBrightnessConfig);

            final PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
            mMaxBrightness = pm.getMaximumScreenBrightnessSetting();
            mBacklightBar.setMax(mMaxBrightness);
            mBacklightBar.setProgress(mCurrentBrightness);
            mBacklightInput.setText(String.valueOf(mCurrentBrightness));

            initListeners();

            setButton(DialogInterface.BUTTON_POSITIVE, getResources().getString(R.string.okay), this);
            setButton(DialogInterface.BUTTON_NEGATIVE, getResources().getString(R.string.cancel), this);

            super.onCreate(savedInstanceState);
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                try {
                    int newBacklight = Integer.valueOf(mBacklightInput.getText().toString());
                    Settings.System.putInt(getContext().getContentResolver(),
                            Settings.System.DOZE_SCREEN_BRIGHTNESS, newBacklight);
                } catch (NumberFormatException e) {
                    Log.d(TAG, "NumberFormatException " + e);
                }
            }
        }

        private void initListeners() {
            mBacklightBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (seekBar.getProgress() > 0) {
                        mBacklightInput.setText(String.valueOf(seekBar.getProgress()));
                    }
                }
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });

            mBacklightInput.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }
                @Override
                public void afterTextChanged(Editable s) {
                    boolean ok = false;
                    try {
                        int minValue = 1;
                        int maxValue = mMaxBrightness;
                        int newBrightness = Integer.valueOf(s.toString());

                        if (newBrightness >= minValue && newBrightness <= maxValue) {
                            ok = true;
                            mBacklightBar.setProgress(newBrightness);
                        }
                    } catch (NumberFormatException e) {
                        //ignored, ok is false ayway
                    }

                    Button okButton = mDozeBrightnessDialog.getButton(DialogInterface.BUTTON_POSITIVE);
                    if (okButton != null) {
                        okButton.setEnabled(ok);
                    }
                }
            });
        }
    }
}
