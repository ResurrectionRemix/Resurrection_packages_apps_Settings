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
import android.app.Dialog;
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
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionInfo;
import android.telephony.TelephonyManager;
import android.telephony.SubscriptionManager.OnSubscriptionsChangedListener;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Switch;
import android.widget.TextView;

import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.TelephonyIntents;
import com.android.settings.R;
import com.android.settings.Utils;

import java.util.List;


/**
 * SimEnabler is a helper to manage the slot on/off checkbox preference. It is
 * turns on/off slot and ensures the summary of the preference reflects the current state.
 */
public class MultiSimEnablerPreference extends Preference implements OnCheckedChangeListener {
    private final Context mContext;

    private String TAG = "MultiSimEnablerPreference";
    private static final boolean DBG = true;

    private static final int EVT_UPDATE = 1;
    private static final int EVT_SHOW_RESULT_DLG = 2;
    private static final int EVT_SHOW_PROGRESS_DLG = 3;
    private static final int EVT_PROGRESS_DLG_TIME_OUT = 4;

    private static final int CONFIRM_ALERT_DLG_ID = 1;
    private static final int ERROR_ALERT_DLG_ID = 2;

    private int mSlotId;
    private SubscriptionInfo mSir;
    private boolean mCurrentState;

    private boolean mCmdInProgress = false;
    private TextView mSubTitle, mSubSummary;
    private int mSwitchVisibility = View.VISIBLE;
    private Switch mSwitch;
    private Handler mParentHandler = null;
    private AlertDialog mAlertDialog = null;
    private ProgressDialog mProgressDialog = null;
    //Delay for progress dialog to dismiss
    private static final int PROGRESS_DLG_TIME_OUT = 30000;
    private static final int MSG_DELAY_TIME = 2000;

    private static Object mSyncLock = new Object();

    private IntentFilter mIntentFilter = new IntentFilter(
            TelephonyIntents.ACTION_SUBINFO_CONTENT_CHANGE);

    private SubscriptionManager mSubscriptionManager;

    public MultiSimEnablerPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        mSubscriptionManager = SubscriptionManager.from(context);
        setWidgetLayoutResource(R.layout.custom_checkbox);
        setSwitchVisibility(View.VISIBLE);
    }

    public MultiSimEnablerPreference(Context context, SubscriptionInfo sir, Handler handler,
            int slotId) {
        this(context, null, com.android.internal.R.attr.checkBoxPreferenceStyle);
        logd("Contructor..Enter" + sir);
        mSlotId = slotId;
        mSir = sir;
        mParentHandler = handler;
        mSubscriptionManager = SubscriptionManager.from(context);
    }

    private void sendMessage(int event, Handler handler, int delay) {
        Message message = handler.obtainMessage(event);
        handler.sendMessageDelayed(message, delay);
    }

    private boolean hasCard() {
        return TelephonyManager.getDefault().hasIccCard(mSlotId);
    }

    private boolean isAirplaneModeOn() {
        return (Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) != 0);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        mSubTitle = (TextView) view.findViewById(R.id.subtitle);
        mSubSummary = (TextView) view.findViewById(R.id.subsummary);
        mSwitch = (Switch) view.findViewById(R.id.subSwitchWidget);
        mSwitch.setOnCheckedChangeListener(this);
        update();
        // now use other config screen to active/deactive sim card\
        mSwitch.setVisibility(mSwitchVisibility);
    }

    public void update() {
        logd("update()" + mSir);

        final Resources res = mContext.getResources();
        boolean isSubValid = isCurrentSubValid();
        setEnabled(isSubValid);

        logd("update() isSubValid "  + isSubValid);
        if (isSubValid) {
            updateTitle();
            updateSummary();
        } else {
            if (mSubTitle != null) {
                mSubTitle.setText(res.getString(R.string.sim_card_number_title, mSlotId + 1));
            }
            if (mSubSummary != null) {
                mSubSummary.setText(R.string.sim_slot_empty);
            }
        }
    }

    private boolean isCurrentSubValid() {
        boolean isSubValid = false;
        if (!isAirplaneModeOn() && hasCard()) {
            List<SubscriptionInfo> sirList = SubscriptionManager.from(mContext)
                    .getActiveSubscriptionInfoList();
            if (sirList != null ) {
                for (SubscriptionInfo sir : sirList) {
                    if (sir != null && mSlotId == sir.getSimSlotIndex()) {
                        mSir = sir;
                        break;
                    }
                }
                if (mSir != null && mSir.getSubscriptionId() > 0 && mSir.getSimSlotIndex() >= 0 &&
                        mSir.mStatus != SubscriptionManager.SUB_CONFIGURATION_IN_PROGRESS) {
                    isSubValid = true;
                }
            }
        }
        return isSubValid;
    }

    private void updateTitle() {
        if (mSubTitle == null) return;
        mSubTitle.setText(mSir == null ? "SUB" : mSir.getDisplayName().toString());
    }

    public void setSwitchVisibility (int visibility) {
        mSwitchVisibility = visibility;
    }

    private void setChecked(boolean state) {
        logd("setChecked: state " + state + "sir:" + mSir);
        if (mSwitch != null) {
            mSwitch.setOnCheckedChangeListener(null);
            mSwitch.setChecked(state);
            mSwitch.setOnCheckedChangeListener(this);
            mCurrentState = state;
        }
    }

    public void setEnabled(boolean isEnabled) {
        if (mSwitch != null) {
            mSwitch.setEnabled(isEnabled);
        }
    }

    private void updateSummary() {
        Resources res = mContext.getResources();
        String summary;
        boolean isActivated = (mSir.mStatus == SubscriptionManager.ACTIVE);
        logd("updateSummary: subId " + mSir.getSubscriptionId() + " isActivated = "
                + isActivated + " slot id = " + mSlotId);

        if (isActivated) {
            summary = mContext.getString(R.string.sim_enabler_summary,
                    res.getString(R.string.sim_enabled));
        } else {
            summary = mContext.getString(R.string.sim_enabler_summary,
                    res.getString(hasCard() ? R.string.sim_disabled : R.string.sim_missing));
        }

        if (mSubSummary != null) {
            mSubSummary.setText(summary);
        }
        setChecked(isActivated);
    }


    /**
     * get count of active SubInfo on the device
     * @param context
     * @return
     */
    public static int getActivatedSubInfoCount(Context context) {
        int activeSubInfoCount = 0;
        List<SubscriptionInfo> subInfoLists = SubscriptionManager.from(context)
                .getActiveSubscriptionInfoList();
        SubscriptionController subController = SubscriptionController.getInstance();
        if (subInfoLists != null) {
            for (SubscriptionInfo subInfo : subInfoLists) {
                if (subInfo.getStatus() == SubscriptionManager.ACTIVE) activeSubInfoCount++;
            }
        }
        return activeSubInfoCount;
    }

    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        mCurrentState = isChecked;
        logd("onClick: " + isChecked);

        synchronized (mSyncLock) {
            configureSubscription();
        }
    }

    private void configureSubscription() {
        if (isAirplaneModeOn()) {
            // do nothing but warning
            logd("APM is on, EXIT!");
            showAlertDialog(ERROR_ALERT_DLG_ID, R.string.sim_enabler_airplane_on);
            return;
        }
        for (int i = 0; i < TelephonyManager.getDefault().getPhoneCount(); i++) {
            int[] subId = SubscriptionManager.getSubId(i);
            if (TelephonyManager.getDefault().getCallState(subId[0])
                != TelephonyManager.CALL_STATE_IDLE) {
                logd("Call state for phoneId: " + i + " is not idle, EXIT!");
                showAlertDialog(ERROR_ALERT_DLG_ID, R.string.sim_enabler_in_call);
                return;
            }
        }

        if (!mCurrentState) {
            if (getActivatedSubInfoCount(mContext) > 1) {
                logd("More than one sub is active, Deactivation possible.");
                showAlertDialog(CONFIRM_ALERT_DLG_ID, 0);
            } else {
                logd("Only one sub is active. Deactivation not possible.");
                showAlertDialog(ERROR_ALERT_DLG_ID, R.string.sim_enabler_both_inactive);
                return;
            }
        } else {
            logd("Activate the sub");
            sendSubConfigurationRequest();
        }

    }

    private void sendSubConfigurationRequest() {
        if (mParentHandler == null || !mSwitch.isEnabled()) {
            return;
        }
        mCmdInProgress = true;

        showProgressDialog();
        mSwitch.setEnabled(false);
        if (mCurrentState) {
            SubscriptionManager.activateSubId(mSir.getSubscriptionId());
        } else {
            SubscriptionManager.deactivateSubId(mSir.getSubscriptionId());
        }

        mSubscriptionManager.addOnSubscriptionsChangedListener(mSubscriptionListener);
    }

    private void processSetUiccDone() {
        sendMessage(EVT_UPDATE, mParentHandler, MSG_DELAY_TIME);
        sendMessage(EVT_SHOW_RESULT_DLG, mHandler, MSG_DELAY_TIME);
        mCmdInProgress = false;
        unregisterReceiver();
    }

    private void showAlertDialog(int dialogId, int msgId) {
        String title = mSir == null ? "SUB" : mSir.getDisplayName().toString();
        // Confirm only one AlertDialog instance to show.
        dismissDialog(mAlertDialog);
        dismissDialog(mProgressDialog);
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        switch(dialogId) {
            case CONFIRM_ALERT_DLG_ID:
                builder.setTitle(title);
                builder.setMessage(mContext.getString(R.string.sim_enabler_need_disable_sim));
                builder.setPositiveButton(R.string.sim_enabler_deactivate, mDialogClickListener);
                builder.setNegativeButton(android.R.string.no, mDialogClickListener);
                builder.setOnCancelListener(mDialogCanceListener);
                break;
            case ERROR_ALERT_DLG_ID:
                builder.setMessage(mContext.getString(msgId));
                builder.setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int which) {
                                update();
                            }
                        });
                builder.setCancelable(false);
                break;
            default:
                break;
        }

        mAlertDialog = builder.create();
        mAlertDialog.setCanceledOnTouchOutside(false);
        mAlertDialog.show();
    }

    private void showProgressDialog() {
        String simName = mSir == null ? mContext.getString(R.string.sim_enabler_sim)
                : mSir.displayName;

        String msg = mContext.getString(mCurrentState ? R.string.sim_enabler_enabling
                : R.string.sim_enabler_disabling, simName);
        dismissDialog(mProgressDialog);
        mProgressDialog = new ProgressDialog(mContext);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setMessage(msg);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.show();

        sendMessage(EVT_PROGRESS_DLG_TIME_OUT, mHandler, PROGRESS_DLG_TIME_OUT);
    }

    private void dismissDialog(Dialog dialog) {
        if(dialog != null) {
            dialog.dismiss();
            dialog = null;
        }
    }

    public void cleanUp() {
        unregisterReceiver();
        dismissDialog(mProgressDialog);
        dismissDialog(mAlertDialog);
    }

    private void unregisterReceiver() {
        try {
            mSubscriptionManager.removeOnSubscriptionsChangedListener(mSubscriptionListener);
        } catch (Exception ex) {}
    }

    private DialogInterface.OnClickListener mDialogClickListener = new DialogInterface
            .OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    if (which == DialogInterface.BUTTON_POSITIVE) {
                        sendSubConfigurationRequest();
                    } else if (which == DialogInterface.BUTTON_NEGATIVE) {
                        setChecked(true);
                        mSubSummary.setText(mContext.getString(
                                R.string.sim_enabler_summary,
                                mContext.getString(R.string.sim_enabled)));
                    } else if (which == DialogInterface.BUTTON_NEUTRAL) {
                        update();
                    }
                }
            };

    private DialogInterface.OnCancelListener mDialogCanceListener = new DialogInterface
            .OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    update();
                }
            };

    private final OnSubscriptionsChangedListener mSubscriptionListener =
            new OnSubscriptionsChangedListener() {
        public void onSubscriptionChanged() {
            logd("Received onSubscriptionChanged");
             SubscriptionInfo sir = Utils.findRecordBySubId(mContext, mSir.getSubscriptionId());
             // When sir is not null, the sub state will be ACTIVE. 
             if ((sir != null && mCurrentState == true) ||
                     ( (sir == null) && mCurrentState == false)) {
                 processSetUiccDone();
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
                    dismissDialog(mProgressDialog);
                    mHandler.removeMessages(EVT_PROGRESS_DLG_TIME_OUT);
                    break;
                case EVT_SHOW_PROGRESS_DLG:
                    logd("EVT_SHOW_PROGRESS_DLG");
                    showProgressDialog();
                    break;
                case EVT_PROGRESS_DLG_TIME_OUT:
                    logd("EVT_PROGRESS_DLG_TIME_OUT");
                    dismissDialog(mProgressDialog);
                    break;
                default:
                    break;
            }
        }
    };

    private void logd(String msg) {
        if (DBG) Log.d(TAG + "(" + mSlotId + ")", msg);
    }

    private void loge(String msg) {
        if (DBG) Log.e(TAG + "(" + mSlotId + ")", msg);
    }

}
