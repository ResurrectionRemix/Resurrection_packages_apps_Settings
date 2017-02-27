/*
 * Copyright (C) 2014 The Android Open Source Project
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
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.PreferenceViewHolder;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.PhoneNumberUtils;
import android.telephony.PhoneStateListener;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyProperties;
import com.android.settings.R;
import com.android.settings.RestrictedSettingsFragment;
import com.android.settings.Utils;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;

import org.codeaurora.internal.IExtTelephony;

import java.util.ArrayList;
import java.util.List;

public class SimSettings extends RestrictedSettingsFragment implements Indexable {
    private static final String TAG = "SimSettings";
    private static final boolean DBG = false;

    // These are the list of  possible values that
    // IExtTelephony.getCurrentUiccCardProvisioningStatus() can return
    private static final int PROVISIONED = 1;
    private static final int NOT_PROVISIONED = 0;
    private static final int INVALID_STATE = -1;
    private static final int CARD_NOT_PRESENT = -2;

    private static final String DISALLOW_CONFIG_SIM = "no_config_sim";
    private static final String SIM_CARD_CATEGORY = "sim_cards";
    private static final String KEY_CELLULAR_DATA = "sim_cellular_data";
    private static final String KEY_CALLS = "sim_calls";
    private static final String KEY_SMS = "sim_sms";
    public static final String EXTRA_SLOT_ID = "slot_id";

    /**
     * By UX design we use only one Subscription Information(SubInfo) record per SIM slot.
     * mAvalableSubInfos is the list of SubInfos we present to the user.
     * mSubInfoList is the list of all SubInfos.
     * mSelectableSubInfos is the list of SubInfos that a user can select for data, calls, and SMS.
     */
    private List<SubscriptionInfo> mAvailableSubInfos = null;
    private List<SubscriptionInfo> mSubInfoList = null;
    private List<SubscriptionInfo> mSelectableSubInfos = null;
    private PreferenceGroup mSimCards = null;
    private SubscriptionManager mSubscriptionManager;
    private int mNumSlots;
    private Context mContext;
    private IExtTelephony mExtTelephony;

    private int mPhoneCount = TelephonyManager.getDefault().getPhoneCount();
    private int[] mCallState = new int[mPhoneCount];
    private PhoneStateListener[] mPhoneStateListener = new PhoneStateListener[mPhoneCount];

    private AlertDialog mAlertDialog = null;
    private ProgressDialog mProgressDialog = null;
    private boolean mNeedsUpdate = false;
    private int[] mUiccProvisionStatus = new int[mPhoneCount];

    private static final String ACTION_UICC_MANUAL_PROVISION_STATUS_CHANGED =
            "org.codeaurora.intent.action.ACTION_UICC_MANUAL_PROVISION_STATUS_CHANGED";
    private static final String EXTRA_NEW_PROVISION_STATE = "newProvisionState";

    public SimSettings() {
        super(DISALLOW_CONFIG_SIM);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.SIM;
    }

    @Override
    public void onCreate(final Bundle bundle) {
        super.onCreate(bundle);
        mContext = getActivity();

        mSubscriptionManager = SubscriptionManager.from(getActivity());
        final TelephonyManager tm =
                (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);
        mExtTelephony = IExtTelephony.Stub.asInterface(ServiceManager.getService("extphone"));

        addPreferencesFromResource(R.xml.sim_settings);

        mNumSlots = tm.getSimCount();
        mSimCards = (PreferenceGroup)findPreference(SIM_CARD_CATEGORY);
        mAvailableSubInfos = new ArrayList<SubscriptionInfo>(mNumSlots);
        mSelectableSubInfos = new ArrayList<SubscriptionInfo>();
        SimSelectNotification.cancelNotification(getActivity());

        IntentFilter intentFilter = new IntentFilter(ACTION_UICC_MANUAL_PROVISION_STATUS_CHANGED);
        mContext.registerReceiver(mReceiver, intentFilter);
    }

    @Override
    public void onDestroy() {
        mContext.unregisterReceiver(mReceiver);
        Log.d(TAG,"on onDestroy");
        super.onDestroy();
    }

    private final SubscriptionManager.OnSubscriptionsChangedListener mOnSubscriptionsChangeListener
            = new SubscriptionManager.OnSubscriptionsChangedListener() {
        @Override
        public void onSubscriptionsChanged() {
            if (DBG) log("onSubscriptionsChanged:");
            if (isAdded()) {
                updateSubscriptions();
            }
        }
    };

    private void updateSubscriptions() {
        mSubInfoList = mSubscriptionManager.getActiveSubscriptionInfoList();
        for (int i = 0; i < mNumSlots; ++i) {
            Preference pref = mSimCards.findPreference("sim" + i);
            if (pref instanceof SimPreference) {
                mSimCards.removePreference(pref);
            }
        }
        mAvailableSubInfos.clear();
        mSelectableSubInfos.clear();

        for (int i = 0; i < mNumSlots; ++i) {
            final SubscriptionInfo sir = mSubscriptionManager
                    .getActiveSubscriptionInfoForSimSlotIndex(i);
            SimPreference simPreference = new SimEnablerPreference(getPrefContext(), sir, i);
            simPreference.setOrder(i-mNumSlots);
            mSimCards.addPreference(simPreference);
            mAvailableSubInfos.add(sir);
            if (sir != null && mUiccProvisionStatus[i] == PROVISIONED) {
                mSelectableSubInfos.add(sir);
            }
        }
        updateAllOptions();
    }

    private void updateAllOptions() {
        updateSimSlotValues();
        updateActivitesCategory();
    }

    private void updateSimSlotValues() {
        final int prefSize = mSimCards.getPreferenceCount();
        for (int i = 0; i < prefSize; ++i) {
            Preference pref = mSimCards.getPreference(i);
            if (pref instanceof SimPreference) {
                ((SimPreference)pref).update();
            }
        }
    }

    private void updateActivitesCategory() {
        updateCellularDataValues();
        updateCallValues();
        updateSmsValues();
    }

    private void updateSmsValues() {
        final Preference simPref = findPreference(KEY_SMS);
        final SubscriptionInfo sir = mSubscriptionManager.getDefaultSmsSubscriptionInfo();
        simPref.setTitle(R.string.sms_messages_title);
        if (DBG) log("[updateSmsValues] mSubInfoList=" + mSubInfoList);

        if (sir != null) {
            simPref.setSummary(sir.getDisplayName());
            simPref.setEnabled(mSelectableSubInfos.size() > 1);
        } else if (sir == null) {
            simPref.setSummary(R.string.sim_selection_required_pref);
            simPref.setEnabled(mSelectableSubInfos.size() >= 1);
        }
    }

    private void updateCellularDataValues() {
        final Preference simPref = findPreference(KEY_CELLULAR_DATA);
        final SubscriptionInfo sir = mSubscriptionManager.getDefaultDataSubscriptionInfo();
        simPref.setTitle(R.string.cellular_data_title);
        if (DBG) log("[updateCellularDataValues] mSubInfoList=" + mSubInfoList);

        boolean callStateIdle = isCallStateIdle();
        final boolean ecbMode = SystemProperties.getBoolean(
                TelephonyProperties.PROPERTY_INECM_MODE, false);
        if (sir != null) {
            simPref.setSummary(sir.getDisplayName());
            // Enable data preference in msim mode and call state idle
            simPref.setEnabled((mSelectableSubInfos.size() > 1) && callStateIdle && !ecbMode);
        } else if (sir == null) {
            simPref.setSummary(R.string.sim_selection_required_pref);
            // Enable data preference in msim mode and call state idle
            simPref.setEnabled((mSelectableSubInfos.size() >= 1) && callStateIdle && !ecbMode);
        }
    }

    private void updateCallValues() {
        final Preference simPref = findPreference(KEY_CALLS);
        final TelecomManager telecomManager = TelecomManager.from(mContext);
        final PhoneAccountHandle phoneAccount =
            telecomManager.getUserSelectedOutgoingPhoneAccount();
        final List<PhoneAccountHandle> allPhoneAccounts =
            telecomManager.getCallCapablePhoneAccounts();

        simPref.setTitle(R.string.calls_title);
        simPref.setSummary(phoneAccount == null
                ? mContext.getResources().getString(R.string.sim_calls_ask_first_prefs_title)
                : (String)telecomManager.getPhoneAccount(phoneAccount).getLabel());
        simPref.setEnabled(allPhoneAccounts.size() > 1);
    }

    @Override
    public void onResume() {
        super.onResume();
        mSubscriptionManager.addOnSubscriptionsChangedListener(mOnSubscriptionsChangeListener);
        updateSubscriptions();
        final TelephonyManager tm =
                (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);
        if (mSelectableSubInfos.size() > 1) {
            Log.d(TAG, "Register for call state change");
            for (int i = 0; i < mPhoneCount; i++) {
                int subId = mSelectableSubInfos.get(i).getSubscriptionId();
                tm.listen(getPhoneStateListener(i, subId),
                        PhoneStateListener.LISTEN_CALL_STATE);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mSubscriptionManager.removeOnSubscriptionsChangedListener(mOnSubscriptionsChangeListener);
        final TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        for (int i = 0; i < mPhoneCount; i++) {
            if (mPhoneStateListener[i] != null) {
                tm.listen(mPhoneStateListener[i], PhoneStateListener.LISTEN_NONE);
                mPhoneStateListener[i] = null;
            }
        }

        for (int i = 0; i < mSimCards.getPreferenceCount(); ++i) {
            Preference pref = mSimCards.getPreference(i);
            if (pref instanceof SimEnablerPreference) {
                // Calling cleanUp() here to dismiss/cleanup any pending dialog exists.
                ((SimEnablerPreference)pref).cleanUpPendingDialogs();
            }
        }
    }

    private PhoneStateListener getPhoneStateListener(int phoneId, int subId) {
        // Disable Sim selection for Data when voice call is going on as changing the default data
        // sim causes a modem reset currently and call gets disconnected
        // ToDo : Add subtext on disabled preference to let user know that default data sim cannot
        // be changed while call is going on
        final int i = phoneId;
        mPhoneStateListener[phoneId]  = new PhoneStateListener(subId) {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                if (DBG) log("PhoneStateListener.onCallStateChanged: state=" + state);
                mCallState[i] = state;
                updateCellularDataValues();
            }
        };
        return mPhoneStateListener[phoneId];
    }

    @Override
    public boolean onPreferenceTreeClick(final Preference preference) {
        final Context context = mContext;
        Intent intent = new Intent(context, SimDialogActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        if (preference instanceof SimPreference) {
            Intent newIntent = new Intent(context, SimPreferenceDialog.class);
            newIntent.putExtra(EXTRA_SLOT_ID, ((SimPreference)preference).getSlotId());
            startActivity(newIntent);
        } else if (findPreference(KEY_CELLULAR_DATA) == preference) {
            intent.putExtra(SimDialogActivity.DIALOG_TYPE_KEY, SimDialogActivity.DATA_PICK);
            context.startActivity(intent);
        } else if (findPreference(KEY_CALLS) == preference) {
            intent.putExtra(SimDialogActivity.DIALOG_TYPE_KEY, SimDialogActivity.CALLS_PICK);
            context.startActivity(intent);
        } else if (findPreference(KEY_SMS) == preference) {
            intent.putExtra(SimDialogActivity.DIALOG_TYPE_KEY, SimDialogActivity.SMS_PICK);
            context.startActivity(intent);
        }

        return true;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (mNeedsUpdate) {
            mNeedsUpdate = false;
            updateAllOptions();
        }
    }

    private void simEnablerUpdate() {
        if (isAdded()) {
            updateAllOptions();
        } else {
            mNeedsUpdate = true;
        }
    }

    private class SimPreference extends Preference {
        SubscriptionInfo mSubInfoRecord;
        int mSlotId;
        Context mContext;

        public SimPreference(Context context, SubscriptionInfo subInfoRecord, int slotId) {
            super(context);
            mContext = context;
            mSubInfoRecord = subInfoRecord;
            mSlotId = slotId;
            setKey("sim" + mSlotId);
            update();
        }

        public void update() {
            final Resources res = mContext.getResources();

            setTitle(String.format(mContext.getResources()
                    .getString(R.string.sim_editor_title), (mSlotId + 1)));
            if (mSubInfoRecord != null) {
                if (TextUtils.isEmpty(getPhoneNumber(mSubInfoRecord))) {
                    setSummary(mSubInfoRecord.getDisplayName());
                } else {
                    setSummary(mSubInfoRecord.getDisplayName() + " - " +
                            PhoneNumberUtils.createTtsSpannable(getPhoneNumber(mSubInfoRecord)));
                    setEnabled(true);
                }
                setSummary(determineSummary());
                setIcon(new BitmapDrawable(res, (mSubInfoRecord.createIconBitmap(mContext))));
            } else {
                setSummary(R.string.sim_slot_empty);
                setFragment(null);
                setEnabled(false);
            }
        }

        protected boolean isValid() {
            return mSubInfoRecord != null;
        }

        protected CharSequence determineSummary() {
            CharSequence number = getPhoneNumber(mSubInfoRecord);
            if (TextUtils.isEmpty(number)) {
                return mSubInfoRecord.getDisplayName();
            } else {
                return mSubInfoRecord.getDisplayName() + " - " +
                        PhoneNumberUtils.createTtsSpannable(number);
            }
        }

        private int getSlotId() {
            return mSlotId;
        }
    }

    // This is to show SIM Enable options on/off on UI for user selection.
    //  User can activate/de-activate through SIM on/off options.
    private class SimEnablerPreference extends SimPreference implements OnCheckedChangeListener {
        private static final int EVT_UPDATE = 1;
        private static final int EVT_SHOW_RESULT_DLG = 2;
        private static final int EVT_SHOW_PROGRESS_DLG = 3;
        private static final int EVT_PROGRESS_DLG_TIME_OUT = 4;

        private static final int CONFIRM_ALERT_DLG_ID = 1;
        private static final int ERROR_ALERT_DLG_ID = 2;
        private static final int RESULT_ALERT_DLG_ID = 3;

        private boolean mCurrentUiccProvisionState;
        private boolean mIsChecked;

        private boolean mCmdInProgress = false;
        private CompoundButton mSwitch;
        //Delay for progress dialog to dismiss
        private static final int PROGRESS_DLG_TIME_OUT = 30000;
        private static final int MSG_DELAY_TIME = 2000;

        public SimEnablerPreference(Context context, SubscriptionInfo sir, int slotId) {
            super(context, sir, slotId);
            setWidgetLayoutResource(R.layout.custom_sim_switch);
        }

        private void sendMessage(int event, Handler handler, int delay) {
            Message message = handler.obtainMessage(event);
            handler.sendMessageDelayed(message, delay);
        }

        private void sendMessage(int event, Handler handler, int delay, int arg1, int arg2) {
            Message message = handler.obtainMessage(event, arg1, arg2);
            handler.sendMessageDelayed(message, delay);
        }

        private boolean hasCard() {
            return TelephonyManager.getDefault().hasIccCard(mSlotId);
        }

        private boolean isAirplaneModeOn() {
            return (Settings.Global.getInt(mContext.getContentResolver(),
                    Settings.Global.AIRPLANE_MODE_ON, 0) != 0);
        }

        private int getProvisionStatus(int slotId) {
            return mUiccProvisionStatus[slotId];
        }

        @Override
        public void onBindViewHolder(PreferenceViewHolder holder) {
            super.onBindViewHolder(holder);
            logd("onBindView....");
            mSwitch = (CompoundButton) holder.findViewById(R.id.sub_switch_widget);
            mSwitch.setOnCheckedChangeListener(this);

            // Hide manual provisioning if the extphone framework
            // is not present, as the operation relies on said framework.
            if (mExtTelephony == null ||
                   !mContext.getResources().getBoolean(R.bool.config_enableManualSubProvisioning)) {
                mSwitch.setVisibility(View.GONE);
            } else {
                mSwitch.setVisibility(View.VISIBLE);
                mSwitch.setEnabled(!isAirplaneModeOn() || isValid());
                setChecked(getProvisionStatus(mSlotId) == PROVISIONED);
            }
        }

        @Override
        public void update() {
            final Resources res = mContext.getResources();
            logd("update()" + mSubInfoRecord);

            if (mExtTelephony != null) {
                try {
                    //get current provision state of the SIM.
                    mUiccProvisionStatus[mSlotId] =
                            mExtTelephony.getCurrentUiccCardProvisioningStatus(mSlotId);
                } catch (RemoteException ex) {
                    mUiccProvisionStatus[mSlotId] = INVALID_STATE;
                    loge("Failed to get pref, slotId: "+ mSlotId +" Exception: " + ex);
                }
            } else {
                // if we don't have telephony-ext, assume provisioned state
                mUiccProvisionStatus[mSlotId] = PROVISIONED;
            }

            super.update();
        }

        // This method returns true if SubScription record corresponds to this
        // Preference screen has a valid SIM and slot index/SubId.
        @Override
        protected boolean isValid() {
            return super.isValid() && getProvisionStatus(mSlotId) >= 0;
        }

        // Based on the received SIM provision state this method
        // sets the check box on Sim Preference UI and updates new
        // state to mCurrentUiccProvisionState.
        private void setChecked(boolean uiccProvisionState) {
            logd("setChecked: uiccProvisionState " + uiccProvisionState + "sir:" + mSubInfoRecord);
            if (mSwitch != null) {
                mSwitch.setOnCheckedChangeListener(null);
                // Do not update update checkstatus again in progress
                if (!mCmdInProgress) {
                    mSwitch.setChecked(uiccProvisionState);
                }
                mSwitch.setOnCheckedChangeListener(this);
                mCurrentUiccProvisionState = uiccProvisionState;
            }
        }

        @Override
        protected CharSequence determineSummary() {
            if (getProvisionStatus(mSlotId) != PROVISIONED) {
                CharSequence state = mContext.getString(
                        hasCard() ? R.string.sim_disabled : R.string.sim_missing);
                return mContext.getString(R.string.sim_enabler_summary,
                        mSubInfoRecord.getDisplayName(), state);
            } else {
                return super.determineSummary();
            }
        }

        /**
        * get number of Subs provisioned on the device
        * @param context
        * @return
        */
        public int getNumOfSubsProvisioned() {
            int activeSubInfoCount = 0;
            List<SubscriptionInfo> subInfoLists =
                    mSubscriptionManager.getActiveSubscriptionInfoList();
            if (subInfoLists != null) {
                for (SubscriptionInfo subInfo : subInfoLists) {
                    if (getProvisionStatus(subInfo.getSimSlotIndex()) == PROVISIONED) {
                        activeSubInfoCount++;
                    }
                }
            }
            return activeSubInfoCount;
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            mIsChecked = isChecked;
            logd("onClick: " + isChecked);

            handleUserRequest();
        }

        // This internal method called when user changes preference from UI
        // 1. For activation/deactivation request from User, if device in APM mode
        //    OR if voice call active on any SIM it dispay error dialog and returns.
        // 2. For deactivation request it returns error dialog if only one SUB in
        //    active state.
        // 3. In other cases it sends user request to framework.
        private void handleUserRequest() {
            if (isAirplaneModeOn()) {
                // do nothing but warning
                logd("APM is on, EXIT!");
                showAlertDialog(ERROR_ALERT_DLG_ID, R.string.sim_enabler_airplane_on);
                return;
            }
            for (int i = 0; i < mPhoneCount; i++) {
                int[] subId = SubscriptionManager.getSubId(i);
                //when voice call in progress, subscription can't be activate/deactivate.
                if (TelephonyManager.getDefault().getCallState(subId[0])
                        != TelephonyManager.CALL_STATE_IDLE) {
                    logd("Call state for phoneId: " + i + " is not idle, EXIT!");
                    showAlertDialog(ERROR_ALERT_DLG_ID, R.string.sim_enabler_in_call);
                    return;
                }
            }

            if (!mIsChecked) {
                if (getNumOfSubsProvisioned() > 1) {
                    logd("More than one sub is active, Deactivation possible.");
                    showAlertDialog(CONFIRM_ALERT_DLG_ID, 0);
                } else {
                    logd("Only one sub is active. Deactivation not possible.");
                    showAlertDialog(ERROR_ALERT_DLG_ID, R.string.sim_enabler_both_inactive);
                    return;
                }
            } else {
                logd("Activate the sub");
                sendUiccProvisioningRequest();
            }
        }

        private void sendUiccProvisioningRequest() {
            if (!mSwitch.isEnabled()) {
                return;
            }
            new SimEnablerDisabler().execute();
        }

        private class SimEnablerDisabler extends AsyncTask<Void, Void, Integer> {

            int newProvisionedState = NOT_PROVISIONED;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                mCmdInProgress = true;
                showProgressDialog();
                setEnabled(false);
            }

            @Override
            protected Integer doInBackground(Void... params) {
                int result = -1;
                newProvisionedState = NOT_PROVISIONED;
                try {
                    if (mIsChecked) {
                        result = mExtTelephony.activateUiccCard(mSlotId);
                        newProvisionedState = PROVISIONED;
                    } else {
                        result = mExtTelephony.deactivateUiccCard(mSlotId);
                    }
                } catch (RemoteException ex) {
                    loge("Activate  sub failed " + result + " phoneId " + mSlotId);
                } catch (NullPointerException ex) {
                    loge("Failed to activate sub Exception: " + ex);
                }
                return result;
            }

            @Override
            protected void onPostExecute(Integer result) {
                processSetUiccDone(result.intValue(), newProvisionedState);
            }
        }

        private void processSetUiccDone(int result, int newProvisionedState) {
            sendMessage(EVT_UPDATE, mHandler, MSG_DELAY_TIME);
            sendMessage(EVT_SHOW_RESULT_DLG, mHandler, MSG_DELAY_TIME, result, newProvisionedState);
            mCmdInProgress = false;
        }

        private void showAlertDialog(int dialogId, int msgId) {
            String title = mSubInfoRecord.getDisplayName().toString();
            // Confirm only one AlertDialog instance to show.
            dismissDialog(mAlertDialog);
            dismissDialog(mProgressDialog);
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext)
                    .setTitle(title);

            switch(dialogId) {
                case CONFIRM_ALERT_DLG_ID:
                    String message;
                    if (mContext.getResources().getBoolean(
                            R.bool.confirm_to_switch_data_service)) {
                        if (SubscriptionManager.getDefaultDataSubscriptionId() ==
                                mSubInfoRecord.getSubscriptionId()) {
                            message = mContext.getString(
                                    R.string.sim_enabler_need_switch_data_service,
                                    getProvisionedSlotId());
                        } else {
                            message = mContext.getString(R.string.sim_enabler_need_disable_sim);
                        }
                        builder.setTitle(R.string.sim_enabler_will_disable_sim_title);
                    } else {
                        message = mContext.getString(R.string.sim_enabler_need_disable_sim);
                    }
                    builder.setMessage(message);
                    builder.setPositiveButton(android.R.string.ok, mDialogClickListener);
                    builder.setNegativeButton(android.R.string.no, mDialogClickListener);
                    builder.setOnCancelListener(mDialogCanceListener);
                    break;

                case ERROR_ALERT_DLG_ID:
                    builder.setMessage(mContext.getString(msgId));
                    builder.setNeutralButton(android.R.string.ok, mDialogClickListener);
                    builder.setOnCancelListener(mDialogCanceListener);
                    break;

                case RESULT_ALERT_DLG_ID:
                    String msg = mCurrentUiccProvisionState ?
                             mContext.getString(R.string.sub_activate_success) :
                            mContext.getString(R.string.sub_deactivate_success);
                    builder.setMessage(msg);
                    builder.setNeutralButton(android.R.string.ok, null);
                    break;
                default:
                    break;
            }

            mAlertDialog = builder.create();
            mAlertDialog.setCanceledOnTouchOutside(false);
            mAlertDialog.show();
        }

        private int getProvisionedSlotId() {
            int activeSlotId = -1;
            List<SubscriptionInfo> subInfoLists =
                    mSubscriptionManager.getActiveSubscriptionInfoList();
            if (subInfoLists != null) {
                for (SubscriptionInfo subInfo : subInfoLists) {
                    if (getProvisionStatus(subInfo.getSimSlotIndex()) == PROVISIONED
                            && subInfo.getSubscriptionId() != mSubInfoRecord.getSubscriptionId())
                        activeSlotId = subInfo.getSimSlotIndex() + 1;
                }
            }
            return activeSlotId;
        }

        private void showProgressDialog() {
            String title = mSubInfoRecord.getDisplayName().toString();

            String msg = mContext.getString(mIsChecked ? R.string.sim_enabler_enabling
                    : R.string.sim_enabler_disabling);
            dismissDialog(mProgressDialog);
            mProgressDialog = new ProgressDialog(mContext);
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setTitle(title);
            mProgressDialog.setMessage(msg);
            mProgressDialog.setCancelable(false);
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.show();

            sendMessage(EVT_PROGRESS_DLG_TIME_OUT, mHandler, PROGRESS_DLG_TIME_OUT);
        }

        private void dismissDialog(Dialog dialog) {
            if((dialog != null) && (dialog.isShowing())) {
                dialog.dismiss();
                dialog = null;
            }
        }

        public void cleanUpPendingDialogs() {
            dismissDialog(mProgressDialog);
            dismissDialog(mAlertDialog);
        }

        private DialogInterface.OnClickListener mDialogClickListener = new DialogInterface
                .OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == DialogInterface.BUTTON_POSITIVE) {
                            dismissDialog(mAlertDialog);
                            sendUiccProvisioningRequest();
                        } else if (which == DialogInterface.BUTTON_NEGATIVE) {
                            update();
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


        private Handler mHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {

                    switch(msg.what) {
                        case EVT_UPDATE:
                            simEnablerUpdate();

                        case EVT_SHOW_RESULT_DLG:
                            int result = msg.arg1;
                            int newProvisionedState = msg.arg2;
                            logd("EVT_SHOW_RESULT_DLG result: " + result +
                                    " new provisioned state " + newProvisionedState);
                            update();
                            if (result != 0) {
                                int msgId = (newProvisionedState == PROVISIONED) ?
                                        R.string.sub_activate_failed :
                                        R.string.sub_deactivate_failed;
                                showAlertDialog(ERROR_ALERT_DLG_ID, msgId);
                            } else {
                                mCurrentUiccProvisionState = newProvisionedState == PROVISIONED;
                                showAlertDialog(RESULT_ALERT_DLG_ID, 0);
                            }
                            mHandler.removeMessages(EVT_PROGRESS_DLG_TIME_OUT);
                            break;

                        case EVT_SHOW_PROGRESS_DLG:
                            logd("EVT_SHOW_PROGRESS_DLG");
                            showProgressDialog();
                            break;

                        case EVT_PROGRESS_DLG_TIME_OUT:
                            logd("EVT_PROGRESS_DLG_TIME_OUT");
                            dismissDialog(mProgressDialog);
                            // Must update UI when time out
                            update();
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

    // Returns the line1Number. Line1number should always be read from TelephonyManager since it can
    // be overridden for display purposes.
    private String getPhoneNumber(SubscriptionInfo info) {
        final TelephonyManager tm =
            (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        return tm.getLine1Number(info.getSubscriptionId());
    }

    private void log(String s) {
        Log.d(TAG, s);
    }

    /**
     * For search
     */
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                        boolean enabled) {
                    ArrayList<SearchIndexableResource> result =
                            new ArrayList<SearchIndexableResource>();

                    if (Utils.showSimCardTile(context)) {
                        SearchIndexableResource sir = new SearchIndexableResource(context);
                        sir.xmlResId = R.xml.sim_settings;
                        result.add(sir);
                    }

                    return result;
                }
            };

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

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "Intent received: " + action);
            if (ACTION_UICC_MANUAL_PROVISION_STATUS_CHANGED.equals(action)) {
                int phoneId = intent.getIntExtra(PhoneConstants.PHONE_KEY,
                        SubscriptionManager.INVALID_SUBSCRIPTION_ID);
                int newProvisionedState = intent.getIntExtra(EXTRA_NEW_PROVISION_STATE,
                        NOT_PROVISIONED);
                 updateSubscriptions();
                 Log.d(TAG, "Received ACTION_UICC_MANUAL_PROVISION_STATUS_CHANGED on phoneId: "
                         + phoneId + " new sub state " + newProvisionedState);
            }
        }
    };
}
