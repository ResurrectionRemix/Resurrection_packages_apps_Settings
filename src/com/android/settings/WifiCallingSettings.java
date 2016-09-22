/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.telephony.CarrierConfigManager;
import android.telephony.PhoneStateListener;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Switch;
import android.widget.TextView;

import com.android.ims.ImsConfig;
import com.android.ims.ImsManager;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.internal.telephony.Phone;
import com.android.settings.widget.SwitchBar;

/**
 * "Wi-Fi Calling settings" screen.  This preference screen lets you
 * enable/disable Wi-Fi Calling and change Wi-Fi Calling mode.
 */
public class WifiCallingSettings extends SettingsPreferenceFragment
        implements SwitchBar.OnSwitchChangeListener,
        Preference.OnPreferenceChangeListener {

    private static final String TAG = "WifiCallingSettings";

    //String keys for preference lookup
    private static final String BUTTON_WFC_MODE = "wifi_calling_mode";

    //UI objects
    private SwitchBar mSwitchBar;
    private Switch mSwitch;
    private ListPreference mButtonWfcMode;
    private TextView mEmptyView;

    private int[] mCallState = null;
    private PhoneStateListener[] mPhoneStateListener = null;
    private int mPhoneCount;
    private boolean mValidListener = false;
    private boolean mEditableWfcMode = true;

    private void initPhoneStateListeners(int phoneId) {
        SubscriptionManager subMgr = SubscriptionManager.from(getActivity());
        final SubscriptionInfo subInfo = subMgr.getActiveSubscriptionInfoForSimSlotIndex(phoneId);

        if (subInfo == null) {
            Log.e(TAG, "initPhoneStateListeners subInfo : " + subInfo +
                    " for phone Id: " + phoneId);
            return;
        }

        int subId = subInfo.getSubscriptionId();
        final int i = phoneId;
        /*
         * Enable/disable controls when in/out of a call and depending on
         * TTY mode and TTY support over VoLTE.
         * @see android.telephony.PhoneStateListener#onCallStateChanged(int,
         * java.lang.String)
         */
        mPhoneStateListener[phoneId]  = new PhoneStateListener(subId) {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                final SettingsActivity activity = (SettingsActivity) getActivity();
                boolean isNonTtyOrTtyOnVolteEnabled = ImsManager
                        .isNonTtyOrTtyOnVolteEnabled(activity);
                final SwitchBar switchBar = activity.getSwitchBar();
                boolean isWfcEnabled = switchBar.getSwitch().isChecked()
                        && isNonTtyOrTtyOnVolteEnabled;

                mCallState[i] = state;
                switchBar.setEnabled(isCallStateIdle() && isNonTtyOrTtyOnVolteEnabled);

                Preference pref = getPreferenceScreen().findPreference(BUTTON_WFC_MODE);
                if (pref != null) {
                    pref.setEnabled(isWfcEnabled && isCallStateIdle());
                }
            }
        };
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final SettingsActivity activity = (SettingsActivity) getActivity();

        mSwitchBar = activity.getSwitchBar();
        mSwitch = mSwitchBar.getSwitch();
        mSwitchBar.show();

        mEmptyView = (TextView) getView().findViewById(android.R.id.empty);
        setEmptyView(mEmptyView);
        mEmptyView.setText(R.string.wifi_calling_off_explanation);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mSwitchBar.hide();
    }

    private void showAlert(Intent intent) {
        Context context = getActivity();

        CharSequence title = intent.getCharSequenceExtra(Phone.EXTRA_KEY_ALERT_TITLE);
        CharSequence message = intent.getCharSequenceExtra(Phone.EXTRA_KEY_ALERT_MESSAGE);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(message)
                .setTitle(title)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.ok, null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private IntentFilter mIntentFilter;

    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ImsManager.ACTION_IMS_REGISTRATION_ERROR)) {
                // If this fragment is active then we are immediately
                // showing alert on screen. There is no need to add
                // notification in this case.
                //
                // In order to communicate to ImsPhone that it should
                // not show notification, we are changing result code here.
                setResultCode(Activity.RESULT_CANCELED);

                // UX requirement is to disable WFC in case of "permanent" registration failures.
                mSwitch.setChecked(false);

                showAlert(intent);
            }
        }
    };

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.WIFI_CALLING;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.wifi_calling_settings);

        mButtonWfcMode = (ListPreference) findPreference(BUTTON_WFC_MODE);
        mButtonWfcMode.setOnPreferenceChangeListener(this);

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(ImsManager.ACTION_IMS_REGISTRATION_ERROR);

        CarrierConfigManager configManager = (CarrierConfigManager)
                getSystemService(Context.CARRIER_CONFIG_SERVICE);
        boolean isWifiOnlySupported = true;
        if (configManager != null) {
            PersistableBundle b = configManager.getConfig();
            if (b != null) {
                mEditableWfcMode = b.getBoolean(CarrierConfigManager.KEY_EDITABLE_WFC_MODE_BOOL);
                isWifiOnlySupported = b.getBoolean(
                        CarrierConfigManager.KEY_CARRIER_WFC_SUPPORTS_WIFI_ONLY_BOOL, true);
            }
        }

        if (!isWifiOnlySupported) {
            mButtonWfcMode.setEntries(R.array.wifi_calling_mode_choices_without_wifi_only);
            mButtonWfcMode.setEntryValues(R.array.wifi_calling_mode_values_without_wifi_only);
        }

        mPhoneCount = TelephonyManager.getDefault().getPhoneCount();
        mPhoneStateListener = new PhoneStateListener[mPhoneCount];
        mCallState = new int[mPhoneCount];
    }

    @Override
    public void onResume() {
        super.onResume();

        final Context context = getActivity();

        if (ImsManager.isWfcEnabledByPlatform(context)) {
            registerPhoneStateListeners(context);

            mSwitchBar.addOnSwitchChangeListener(this);

            mValidListener = true;
        }

        // NOTE: Buttons will be enabled/disabled in mPhoneStateListener
        boolean wfcEnabled = ImsManager.isWfcEnabledByUser(context)
                && ImsManager.isNonTtyOrTtyOnVolteEnabled(context);
        mSwitch.setChecked(wfcEnabled);
        int wfcMode = ImsManager.getWfcMode(context);
        mButtonWfcMode.setValue(Integer.toString(wfcMode));
        updateButtonWfcMode(context, wfcEnabled, wfcMode);

        context.registerReceiver(mIntentReceiver, mIntentFilter);

        Intent intent = getActivity().getIntent();
        if (intent.getBooleanExtra(Phone.EXTRA_KEY_ALERT_SHOW, false)) {
            showAlert(intent);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        final Context context = getActivity();

        if (mValidListener) {
            mValidListener = false;

            unRegisterPhoneStateListeners(context);

            mSwitchBar.removeOnSwitchChangeListener(this);
        }

        context.unregisterReceiver(mIntentReceiver);
    }

    private void registerPhoneStateListeners(Context context) {
        TelephonyManager tm =
                (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        for (int i = 0; i < mPhoneCount; i++) {
            initPhoneStateListeners(i);
            if (mPhoneStateListener[i] != null) {
                Log.d(TAG, "Register for call state change for phone Id: " + i);
                tm.listen(mPhoneStateListener[i], PhoneStateListener.LISTEN_CALL_STATE);
            }
        }
    }

    private void unRegisterPhoneStateListeners(Context context) {
        TelephonyManager tm =
               (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        for (int i = 0; i < mPhoneCount; i++) {
            if (mPhoneStateListener[i] != null) {
                Log.d(TAG, "unRegister for call state change for phone Id: " + i);
                tm.listen(mPhoneStateListener[i], PhoneStateListener.LISTEN_NONE);
                mPhoneStateListener[i] = null;
            }
        }
    }

    /**
     * Listens to the state change of the switch.
     */
    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        final Context context = getActivity();

        ImsManager.setWfcSetting(context, isChecked);

        int wfcMode = ImsManager.getWfcMode(context);
        updateButtonWfcMode(context, isChecked, wfcMode);
        if (isChecked) {
            MetricsLogger.action(getActivity(), getMetricsCategory(), wfcMode);
        } else {
            MetricsLogger.action(getActivity(), getMetricsCategory(), -1);
        }
    }

    private void updateButtonWfcMode(Context context, boolean wfcEnabled, int wfcMode) {
        mButtonWfcMode.setSummary(getWfcModeSummary(context, wfcMode));
        mButtonWfcMode.setEnabled(wfcEnabled);

        final PreferenceScreen preferenceScreen = getPreferenceScreen();
        if (wfcEnabled && ImsManager.displayWfcMode(context, false)) {
            preferenceScreen.addPreference(mButtonWfcMode);
        } else {
            preferenceScreen.removePreference(mButtonWfcMode);
        }
        preferenceScreen.setEnabled(mEditableWfcMode);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final Context context = getActivity();
        if (preference == mButtonWfcMode) {
            mButtonWfcMode.setValue((String) newValue);
            int buttonMode = Integer.valueOf((String) newValue);
            int currentMode = ImsManager.getWfcMode(context);
            if (buttonMode != currentMode) {
                ImsManager.setWfcMode(context, buttonMode);
                mButtonWfcMode.setSummary(getWfcModeSummary(context, buttonMode));
                MetricsLogger.action(getActivity(), getMetricsCategory(), buttonMode);
            }
        }
        return true;
    }

    static int getWfcModeSummary(Context context, int wfcMode) {
        int resId = com.android.internal.R.string.wifi_calling_off_summary;
        if (ImsManager.isWfcEnabledByUser(context)) {
            switch (wfcMode) {
                case ImsConfig.WfcModeFeatureValueConstants.WIFI_ONLY:
                    resId = com.android.internal.R.string.wfc_mode_wifi_only_summary;
                    break;
                case ImsConfig.WfcModeFeatureValueConstants.CELLULAR_PREFERRED:
                    resId = com.android.internal.R.string.wfc_mode_cellular_preferred_summary;
                    break;
                case ImsConfig.WfcModeFeatureValueConstants.WIFI_PREFERRED:
                    resId = com.android.internal.R.string.wfc_mode_wifi_preferred_summary;
                    break;
                default:
                    Log.e(TAG, "Unexpected WFC mode value: " + wfcMode);
            }
        }
        return resId;
    }

    private boolean isCallStateIdle() {
        boolean callStateIdle = true;
        for (int i = 0; i < mCallState.length; i++) {
            if (TelephonyManager.CALL_STATE_IDLE != mCallState[i]) {
                callStateIdle = false;
            }
        }
        Log.d(TAG, "isCallStateIdle " + callStateIdle);
        return callStateIdle;
    }
}
