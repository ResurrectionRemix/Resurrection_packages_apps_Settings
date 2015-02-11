/**
 * Copyright (c) 2014, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *     * Neither the name of The Linux Foundation nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */


package com.android.settings.sim;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.telephony.SubInfoRecord;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.telephony.TelephonyIntents;
import com.android.settings.R;

import java.util.List;

/**
 * SimEnabler is a helper to manage the slot on/off checkbox preference. It is
 * turns on/off slot and ensures the summary of the preference reflects the current state.
 */
public class MultiSimEnablerPreference extends SwitchPreference implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "MultiSimEnablerPreference";
    private static final boolean DBG = true;

    private static final int EVT_UPDATE = 1;
    private static final int EVT_SHOW_RESULT_DLG = 2;
    private static final int EVT_PROGRESS_DLG_TIME_OUT = 3;

    private static final IntentFilter SUB_CHANGE_FILTER =
            new IntentFilter(TelephonyIntents.ACTION_SUBINFO_CONTENT_CHANGE);

    private int mSlotId;
    private SubInfoRecord mSir;

    private Boolean mTargetState = null;
    private Handler mParentHandler = null;
    private AlertDialog mConfirmationDialog = null;
    private ProgressDialog mProgressDialog = null;
    private boolean mReceiverRegistered;
    private boolean mClicking;
    private Switch mSwitch;

    //Delay for progress dialog to dismiss
    private static final int PROGRESS_DLG_TIME_OUT = 30000;
    private static final int MSG_DELAY_TIME = 2000;

    public MultiSimEnablerPreference(Context context, SubInfoRecord sir,
            Handler handler, int slotId) {
        super(context);
        logd("Constructor: " + sir);
        mSlotId = slotId;
        mSir = sir;
        mParentHandler = handler;
        setOnPreferenceChangeListener(this);
        setPersistent(false);
    }

    public void update() {
        logd("update()" + mSir);

        boolean isSubValid = isCurrentSubValid();
        setEnabled(isSubValid);

        logd("update() isSubValid "  + isSubValid);
        if (isSubValid) {
            setTitle(getSimDisplayName());
            updateSummary();
        } else {
            setTitle(getContext().getResources().getString(
                    R.string.sim_card_number_title, mSlotId + 1));
            setSummary(R.string.sim_slot_empty);
        }
    }

    public void cleanUp() {
        if (mReceiverRegistered) {
            getContext().unregisterReceiver(mReceiver);
            mReceiverRegistered = false;
        }
        dismissProgressDialog();
        if (mConfirmationDialog != null) {
            mConfirmationDialog.dismiss();
            mConfirmationDialog = null;
        }
    }

    public void createEditDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        final LayoutInflater inflater = LayoutInflater.from(getContext());

        final View dialogLayout = inflater.inflate(R.layout.multi_sim_dialog, null);
        builder.setView(dialogLayout);

        final EditText nameText = (EditText) dialogLayout.findViewById(R.id.sim_name);
        nameText.setText(mSir.displayName);

        TextView numberView = (TextView)dialogLayout.findViewById(R.id.number);
        numberView.setText(mSir.number);

        TextView carrierView = (TextView)dialogLayout.findViewById(R.id.carrier);
        TelephonyManager tm = (TelephonyManager)
                getContext().getSystemService(Context.TELEPHONY_SERVICE);
        String spn = tm.getSimOperatorName(mSir.subId);
        if (TextUtils.isEmpty(spn) && !tm.isNetworkRoaming(mSir.subId)) {
            // Operator did not write the SPN inside the SIM, so set
            // the current network operator as the SIM name, but only if
            // we're not roaming.
            spn = tm.getNetworkOperatorName(mSir.subId);
        }
        carrierView.setText(spn);

        builder.setTitle(R.string.sim_editor_title);

        builder.setPositiveButton(R.string.okay, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                final Spinner displayNumbers =
                        (Spinner)dialogLayout.findViewById(R.id.display_numbers);

                final int formatSetting = displayNumbers.getSelectedItemPosition() == 0
                        ? SubscriptionManager.DISPLAY_NUMBER_LAST
                        : SubscriptionManager.DISPLAY_NUMBER_FIRST;
                SubscriptionManager.setDisplayNumberFormat(formatSetting, mSir.subId);

                mSir.displayName = nameText.getText().toString();
                SubscriptionManager.setDisplayName(mSir.displayName,
                        mSir.subId, SubscriptionManager.NAME_SOURCE_USER_INPUT);

                update();
            }
        });

        builder.setNegativeButton(R.string.cancel, null);

        builder.show();
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        mSwitch = (Switch) view.findViewById(com.android.internal.R.id.switchWidget);
        mSwitch.setClickable(true);

        update();
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object newValue) {
        if (mClicking) {
            return false;
        }

        boolean checked = (Boolean) newValue;
        logd("onClick: " + checked);

        return configureSubscription(checked);
    }

    @Override
    public void onClick() {
        mClicking = true;
        super.onClick();
        mClicking = false;
    }

    private void sendMessage(int event, Handler handler, int delay) {
        Message message = handler.obtainMessage(event);
        handler.sendMessageDelayed(message, delay);
    }

    private boolean hasCard() {
        return TelephonyManager.getDefault().hasIccCard(mSlotId);
    }

    private boolean isAirplaneModeOn() {
        return (Settings.Global.getInt(getContext().getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) != 0);
    }

    private boolean isCurrentSubValid() {
        if (isAirplaneModeOn() || !hasCard()) {
            return false;
        }

        List<SubInfoRecord> sirList = SubscriptionManager.getActiveSubInfoList();
        if (sirList == null) {
            return false;
        }

        for (SubInfoRecord sir : sirList) {
            if (sir != null && mSlotId == sir.slotId) {
                mSir = sir;
                break;
            }
        }

        return mSir != null && mSir.subId > 0 && mSir.slotId >= 0
                && mSir.mStatus != SubscriptionManager.SUB_CONFIGURATION_IN_PROGRESS;
    }

    private String getSimDisplayName() {
        if (mSir != null) {
            return mSir.displayName;
        }

        return getContext().getString(R.string.sim_enabler_sim);
    }

    private void updateSummary() {
        Resources res = getContext().getResources();
        boolean isActivated = mSir.mStatus == SubscriptionManager.ACTIVE;
        logd("updateSummary: subId " + mSir.subId + " isActivated = " + isActivated +
                " slot id = " + mSlotId);

        String simSlot = res.getString(R.string.sim_card_number_title, mSlotId + 1);
        String summary = res.getString(R.string.sim_settings_summary, simSlot, mSir.number);
        setSummary(summary);
        updateCheckedState(isActivated);
    }

    private void updateCheckedState(boolean checked) {
        setOnPreferenceChangeListener(null);
        if (mSwitch != null) {
            mSwitch.setChecked(checked);
        }
        setOnPreferenceChangeListener(this);
    }

    private int getActivatedSubInfoCount() {
        int activeSubInfoCount = 0;
        List<SubInfoRecord> subInfoLists = SubscriptionManager.getActiveSubInfoList();
        if (subInfoLists != null) {
            for (SubInfoRecord subInfo : subInfoLists) {
                if (subInfo.mStatus == SubscriptionManager.ACTIVE) activeSubInfoCount++;
            }
        }
        return activeSubInfoCount;
    }

    private boolean configureSubscription(boolean enable) {
        if (isAirplaneModeOn()) {
            // do nothing but warning
            logd("APM is on, EXIT!");
            showToast(R.string.sim_enabler_airplane_on);
            return false;
        }
        for (int i = 0; i < TelephonyManager.getDefault().getPhoneCount(); i++) {
            long[] subId = SubscriptionManager.getSubId(i);
            if (TelephonyManager.getDefault().getCallState(subId[0])
                    != TelephonyManager.CALL_STATE_IDLE) {
                logd("Call state for phoneId: " + i + " is not idle, EXIT!");
                showToast(R.string.sim_enabler_in_call);
                return false;
            }
        }

        if (!enable) {
            if (getActivatedSubInfoCount() > 1) {
                logd("More than one sub is active, Deactivation possible.");
                showDisableConfirmationDialog();
            } else {
                logd("Only one sub is active. Deactivation not possible.");
                showToast(R.string.sim_enabler_both_inactive);
                return false;
            }
        } else {
            logd("Activate the sub");
            sendSubConfigurationRequest(true);
        }

        return true;
    }

    private void showToast(int textResId) {
        Toast.makeText(getContext(), textResId, Toast.LENGTH_LONG).show();
    }

    private void sendSubConfigurationRequest(boolean enable) {
        if (mParentHandler == null) {
            return;
        }
        mTargetState = enable;

        showProgressDialog(enable);
        // mSwitch.setEnabled(false);
        if (enable) {
            SubscriptionManager.activateSubId(mSir.subId);
        } else {
            SubscriptionManager.deactivateSubId(mSir.subId);
        }

        if (!mReceiverRegistered) {
            getContext().registerReceiver(mReceiver, SUB_CHANGE_FILTER);
            mReceiverRegistered = true;
        }
    }

    private void processSetUiccDone() {
        sendMessage(EVT_UPDATE, mParentHandler, MSG_DELAY_TIME);
        sendMessage(EVT_SHOW_RESULT_DLG, mHandler, MSG_DELAY_TIME);
        mTargetState = null;
        if (mReceiverRegistered) {
            getContext().unregisterReceiver(mReceiver);
            mReceiverRegistered = false;
        }
    }

    private void showDisableConfirmationDialog() {
        if (mConfirmationDialog != null) {
            mConfirmationDialog.dismiss();
        }

        mConfirmationDialog = new AlertDialog.Builder(getContext())
                .setTitle(getSimDisplayName())
                .setMessage(R.string.sim_enabler_need_disable_sim)
                .setPositiveButton(R.string.sim_enabler_deactivate, mConfirmDialogListener)
                .setNegativeButton(android.R.string.no, mConfirmDialogListener)
                .setOnCancelListener(mConfirmDialogCancelListener)
                .create();

        mConfirmationDialog.setCanceledOnTouchOutside(false);
        mConfirmationDialog.show();
    }

    private void showProgressDialog(boolean enabling) {
        int stateTextResId = enabling
                ? R.string.sim_enabler_enabling : R.string.sim_enabler_disabling;

        dismissProgressDialog();

        mProgressDialog = new ProgressDialog(getContext());
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setMessage(getContext().getString(stateTextResId, getSimDisplayName()));
        mProgressDialog.setCancelable(false);
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.show();

        sendMessage(EVT_PROGRESS_DLG_TIME_OUT, mHandler, PROGRESS_DLG_TIME_OUT);
    }

    private void dismissProgressDialog() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
    }

    private void logd(String msg) {
        if (DBG) Log.d(TAG + "(" + mSlotId + ")", msg);
    }

    private DialogInterface.OnClickListener mConfirmDialogListener =
            new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                sendSubConfigurationRequest(false);
            } else if (which == DialogInterface.BUTTON_NEGATIVE) {
                updateCheckedState(true);
            }
        }
    };

    private DialogInterface.OnCancelListener mConfirmDialogCancelListener =
            new DialogInterface.OnCancelListener() {
        @Override
        public void onCancel(DialogInterface dialog) {
            mConfirmationDialog = null;
            update();
        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (TelephonyIntents.ACTION_SUBINFO_CONTENT_CHANGE.equals(action)) {
                long subId = intent.getLongExtra(SubscriptionManager._ID,
                        SubscriptionManager.INVALID_SUB_ID);
                String column = intent.getStringExtra(TelephonyIntents.EXTRA_COLUMN_NAME);
                int intValue = intent.getIntExtra(TelephonyIntents.EXTRA_INT_CONTENT, 0);
                logd("Received ACTION_SUBINFO_CONTENT_CHANGE on subId: " + subId
                        + "for " + column + " intValue: " + intValue);
                if (mTargetState != null && mSir.subId == subId && column != null
                        && column.equals(SubscriptionManager.SUB_STATE)) {
                    if ((intValue == SubscriptionManager.ACTIVE && mTargetState) ||
                            (intValue == SubscriptionManager.INACTIVE && !mTargetState)) {
                        processSetUiccDone();
                    }
                }
            }
        }
    };

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVT_SHOW_RESULT_DLG:
                    logd("EVT_SHOW_RESULT_DLG");
                    update();
                    dismissProgressDialog();
                    mHandler.removeMessages(EVT_PROGRESS_DLG_TIME_OUT);
                    break;
                case EVT_PROGRESS_DLG_TIME_OUT:
                    logd("EVT_PROGRESS_DLG_TIME_OUT");
                    dismissProgressDialog();
                    break;
                default:
                    break;
            }
        }
    };
}
