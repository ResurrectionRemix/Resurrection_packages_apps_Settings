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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.SearchIndexableResource;
import android.provider.Settings.SettingNotFoundException;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.PhoneStateListener;
import android.util.Log;
import android.widget.Toast;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.TelephonyIntents;
import com.android.settings.R;
import com.android.settings.RestrictedSettingsFragment;
import com.android.settings.Utils;
import com.android.settings.notification.DropDownPreference;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;

import java.util.ArrayList;
import java.util.List;

public class SimSettings extends RestrictedSettingsFragment implements Indexable {
    private static final String TAG = "SimSettings";
    private static final boolean DBG = true;

    public static final String CONFIG_LTE_SUB_SELECT_MODE = "config_lte_sub_select_mode";
    private static final String CONFIG_PRIMARY_SUB_SETABLE = "config_primary_sub_setable";

    private static final String DISALLOW_CONFIG_SIM = "no_config_sim";
    private static final String SIM_ENABLER_CATEGORY = "sim_enablers";
    private static final String SIM_ACTIVITIES_CATEGORY = "sim_activities";
    private static final String KEY_CELLULAR_DATA = "sim_cellular_data";
    private static final String KEY_CALLS = "sim_calls";
    private static final String KEY_SMS = "sim_sms";
    private static final String KEY_ACTIVITIES = "activities";
    private static final String KEY_PRIMARY_SUB_SELECT = "select_primary_sub";

    private long mPreferredDataSubscription;

    private static final int EVT_UPDATE = 1;
    private static int mNumSlots = 0;

    /**
     * By UX design we have use only one Subscription Information(SubInfo) record per SIM slot.
     * mAvalableSubInfos is the list of SubInfos we present to the user.
     * mSubInfoList is the list of all SubInfos.
     */
    private List<SubscriptionInfo> mAvailableSubInfos = null;
    private List<SubscriptionInfo> mSubInfoList = null;
    private Preference mPrimarySubSelect = null;

    private static List<MultiSimEnablerPreference> mSimEnablers = null;

    private SubscriptionInfo mCellularData = null;
    private SubscriptionInfo mCalls = null;
    private SubscriptionInfo mSMS = null;

    private int mNumSims;
    private int mPhoneCount;
    private int[] mCallState;
    private PhoneStateListener[] mPhoneStateListener;

    private boolean inActivity;
    private boolean dataDisableToastDisplayed = false;

    private SubscriptionManager mSubscriptionManager;

    public SimSettings() {
        super(DISALLOW_CONFIG_SIM);
    }

    @Override
    public void onCreate(final Bundle bundle) {
        super.onCreate(bundle);
        Log.d(TAG,"on onCreate");

        mSubscriptionManager = SubscriptionManager.from(getActivity());
        final TelephonyManager tm =
                    (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);

        if (mSubInfoList == null) {
            mSubInfoList = mSubscriptionManager.getActiveSubscriptionInfoList();
        }

        mNumSlots = tm.getSimCount();
        mPhoneCount = TelephonyManager.getDefault().getPhoneCount();
        mCallState = new int[mPhoneCount];
        mPhoneStateListener = new PhoneStateListener[mPhoneCount];
        listen();

        mPreferredDataSubscription = mSubscriptionManager.getDefaultDataSubId();

        createPreferences();
        updateAllOptions();
        IntentFilter intentFilter =
                new IntentFilter(TelephonyIntents.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED);
        intentFilter.addAction(TelephonyIntents.ACTION_SUBINFO_CONTENT_CHANGE);
        intentFilter.addAction(TelephonyIntents.ACTION_SUBINFO_RECORD_UPDATED);

        getActivity().registerReceiver(mDdsSwitchReceiver, intentFilter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG,"on onDestroy");
        getActivity().unregisterReceiver(mDdsSwitchReceiver);
        unRegisterPhoneStateListener();
    }

    private void unRegisterPhoneStateListener() {
        TelephonyManager tm =
                (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);
        for (int i = 0; i < mPhoneCount; i++) {
            if (mPhoneStateListener[i] != null) {
                tm.listen(mPhoneStateListener[i], PhoneStateListener.LISTEN_NONE);
                mPhoneStateListener[i] = null;
            }
        }
    }

    private BroadcastReceiver mDdsSwitchReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "Intent received: " + action);
            if (TelephonyIntents.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED.equals(action)) {
                updateCellularDataValues();
                int preferredDataSubscription = mSubscriptionManager.getDefaultDataSubId();
                if (preferredDataSubscription != mPreferredDataSubscription) {
                    mPreferredDataSubscription = preferredDataSubscription;
                    String status = getResources().getString(R.string.switch_data_subscription,
                            mSubscriptionManager.getSlotId(preferredDataSubscription) + 1);
                    Toast.makeText(getActivity(), status, Toast.LENGTH_SHORT).show();
                }
            } else if (TelephonyIntents.ACTION_SUBINFO_CONTENT_CHANGE.equals(action)
                    || TelephonyIntents.ACTION_SUBINFO_RECORD_UPDATED.equals(action)) {
                mAvailableSubInfos.clear();
                mNumSims = 0;
                mSubInfoList = mSubscriptionManager.getActiveSubscriptionInfoList();
                for (int i = 0; i < mNumSlots; ++i) {
                    final SubscriptionInfo sir = findRecordBySlotId(i);
                    // Do not display deactivated subInfo in preference list
                    if ((sir != null) && (sir.mStatus == mSubscriptionManager.ACTIVE)) {
                        mNumSims++;
                        mAvailableSubInfos.add(sir);
                    }
                }
                // Refresh UI whenever subinfo record gets changed
                updateAllOptions();
            }
        }
    };

    private void createPreferences() {
        addPreferencesFromResource(R.xml.sim_settings);

        mPrimarySubSelect = (Preference) findPreference(KEY_PRIMARY_SUB_SELECT);
        final PreferenceCategory simEnablers =
                (PreferenceCategory)findPreference(SIM_ENABLER_CATEGORY);

        mAvailableSubInfos = new ArrayList<SubscriptionInfo>(mNumSlots);
        mSimEnablers = new ArrayList<MultiSimEnablerPreference>(mNumSlots);
        for (int i = 0; i < mNumSlots; ++i) {
            final SubscriptionInfo sir = findRecordBySlotId(i);
            if (mNumSlots > 1) {
                mSimEnablers.add(i, new MultiSimEnablerPreference(
                        getActivity(), sir, mHandler, i));
                simEnablers.addPreference(mSimEnablers.get(i));
            } else {
                removePreference(SIM_ENABLER_CATEGORY);
            }
            // Do not display deactivated subInfo in preference list
            if ((sir != null) && (sir.mStatus == mSubscriptionManager.ACTIVE)) {
                mNumSims++;
                mAvailableSubInfos.add(sir);
            }
        }
    }

    private void updateAllOptions() {
        Log.d(TAG,"updateAllOptions");
        mSubInfoList = mSubscriptionManager.getActiveSubscriptionInfoList();
        updateActivitesCategory();
        updateSimEnablers();
    }

    private void listen() {
        TelephonyManager tm =
                (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);
        for (int i = 0; i < mPhoneCount; i++) {
            int[] subId = mSubscriptionManager.getSubId(i);
            if (subId != null) {
                if (subId[0] > 0) {
                    mCallState[i] = tm.getCallState(subId[0]);
                    tm.listen(getPhoneStateListener(i, subId[0]),
                            PhoneStateListener.LISTEN_CALL_STATE);
                }
            }
        }
    }

    private PhoneStateListener getPhoneStateListener(int phoneId, int subId) {
        final int i = phoneId;
        mPhoneStateListener[phoneId]  = new PhoneStateListener(subId) {
            @Override
            public void onCallStateChanged(int state, String ignored) {
                Log.d(TAG, "onCallStateChanged: " + state);
                mCallState[i] = state;
                updateCellularDataPreference();
            }
        };
        return mPhoneStateListener[phoneId];
    }

    private void updateActivitesCategory() {
        createDropDown((DropDownPreference) findPreference(KEY_CELLULAR_DATA));
        createDropDown((DropDownPreference) findPreference(KEY_CALLS));
        createDropDown((DropDownPreference) findPreference(KEY_SMS));
        updateCellularDataValues();
        updateCallValues();
        updateSmsValues();
    }

    /**
     * finds a record with subId.
     * Since the number of SIMs are few, an array is fine.
     */
    private SubscriptionInfo findRecordBySubId(final long subId) {
        final int availableSubInfoLength = mAvailableSubInfos.size();

        for (int i = 0; i < availableSubInfoLength; ++i) {
            final SubscriptionInfo sir = mAvailableSubInfos.get(i);
            if (sir != null && sir.getSubscriptionId() == subId) {
                return sir;
            }
        }
        return null;
    }

    /**
     * finds a record with slotId.
     * Since the number of SIMs are few, an array is fine.
     */
    private SubscriptionInfo findRecordBySlotId(final int slotId) {
        if (mSubInfoList != null) {
            final int availableSubInfoLength = mSubInfoList.size();

            for (int i = 0; i < availableSubInfoLength; ++i) {
                final SubscriptionInfo sir = mSubInfoList.get(i);
                if (sir.getSimSlotIndex() == slotId) {
                    //Right now we take the first subscription on a SIM.
                    return sir;
                }
            }
        }

        return null;
    }

    private void updateSmsValues() {
        final DropDownPreference simPref = (DropDownPreference) findPreference(KEY_SMS);
        long subId = mSubscriptionManager.isSMSPromptEnabled() ?
                0 : mSubscriptionManager.getDefaultSmsSubId();
        final SubscriptionInfo sir = findRecordBySubId(subId);
        if (sir != null) {
            simPref.setSelectedValue(sir, false);
        }
        simPref.setEnabled(mNumSims > 1);
    }

    private void updateCellularDataValues() {
        final DropDownPreference simPref = (DropDownPreference) findPreference(KEY_CELLULAR_DATA);
        final SubscriptionInfo sir = findRecordBySubId(mSubscriptionManager.getDefaultDataSubId());
        if (sir != null) {
            simPref.setSelectedValue(sir, false);
        }
        updateCellularDataPreference();
    }

    private void updateCellularDataPreference() {
        final DropDownPreference simPref = (DropDownPreference) findPreference(KEY_CELLULAR_DATA);
        boolean callStateIdle = isCallStateIdle();
        // Enable data preference in msim mode and call state idle
        simPref.setEnabled((mNumSims > 1) && callStateIdle);
        // Display toast only once when the user enters the activity even though the call moves
        // through multiple call states (eg - ringing to offhook for incoming calls)
        if (callStateIdle == false && inActivity && dataDisableToastDisplayed == false) {
            Toast.makeText(getActivity(), R.string.data_disabled_in_active_call,
                    Toast.LENGTH_SHORT).show();
            dataDisableToastDisplayed = true;
        }
        // Reset dataDisableToastDisplayed
        if (callStateIdle == true) {
            dataDisableToastDisplayed = false;
        }
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

    private void updateCallValues() {
        final DropDownPreference simPref = (DropDownPreference) findPreference(KEY_CALLS);
        long subId = mSubscriptionManager.isVoicePromptEnabled() ?
                0 : mSubscriptionManager.getDefaultVoiceSubId();
        final SubscriptionInfo sir = findRecordBySubId(subId);
        if (sir != null) {
            simPref.setSelectedValue(sir, false);
        }
        simPref.setEnabled(mNumSims > 1);
    }

    @Override
    public void onPause() {
        super.onPause();
        inActivity = false;
        Log.d(TAG,"on Pause");
        dataDisableToastDisplayed = false;
        for (int i = 0; i < mSimEnablers.size(); ++i) {
            MultiSimEnablerPreference simEnabler = mSimEnablers.get(i);
            if (simEnabler != null) simEnabler.cleanUp();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        inActivity = true;
        Log.d(TAG,"on Resume, number of slots = " + mNumSlots);
        initLTEPreference();
        updateAllOptions();
    }

    private void initLTEPreference() {
        boolean isPrimarySubFeatureEnable = SystemProperties
                .getBoolean("persist.radio.primarycard", false);

        boolean primarySetable = android.provider.Settings.Global.getInt(
                this.getContentResolver(), CONFIG_PRIMARY_SUB_SETABLE, 0) == 1;

        logd("isPrimarySubFeatureEnable :" + isPrimarySubFeatureEnable +
                " primarySetable :" + primarySetable);

        if (!isPrimarySubFeatureEnable || !primarySetable) {
            final PreferenceCategory simActivities =
                    (PreferenceCategory) findPreference(SIM_ACTIVITIES_CATEGORY);
            simActivities.removePreference(mPrimarySubSelect);
            return;
        }

        int primarySlot = getCurrentPrimarySlot();

        boolean isManualMode = android.provider.Settings.Global.getInt(
                this.getContentResolver(), CONFIG_LTE_SUB_SELECT_MODE, 1) == 0;

        logd("init LTE primary slot : " + primarySlot + " isManualMode :" + isManualMode);
        if (-1 != primarySlot) {
            SubscriptionInfo subInfo = findRecordBySlotId(primarySlot);
            CharSequence lteSummary = (subInfo == null ) ? null : subInfo.getDisplayName();
            mPrimarySubSelect.setSummary(lteSummary);
        } else {
            mPrimarySubSelect.setSummary("");
        }
        mPrimarySubSelect.setEnabled(isManualMode);
    }

    public int getCurrentPrimarySlot() {
        for (int index = 0; index < mNumSlots; index++) {
            int current = getPreferredNetwork(index);
            if (current == Phone.NT_MODE_TD_SCDMA_GSM_WCDMA_LTE
                    || current == Phone.NT_MODE_TD_SCDMA_GSM_WCDMA) {
                return index;
            }
        }
        return -1;
    }

    private int getPreferredNetwork(int sub) {
        int nwMode = -1;
        try {
            nwMode = TelephonyManager.getIntAtIndex(this.getContentResolver(),
                    android.provider.Settings.Global.PREFERRED_NETWORK_MODE, sub);
        } catch (SettingNotFoundException snfe) {
        }
        return nwMode;
    }

    @Override
    public boolean onPreferenceTreeClick(final PreferenceScreen preferenceScreen,
            final Preference preference) {
        if (preference instanceof MultiSimEnablerPreference) {
            ((MultiSimEnablerPreference) preference).createEditDialog();
        }  else if (preference == mPrimarySubSelect) {
            startActivity(mPrimarySubSelect.getIntent());
        }
        return true;
    }

    public void createDropDown(DropDownPreference preference) {
        final DropDownPreference simPref = preference;
        final String keyPref = simPref.getKey();
        int mActCount = 0;
        final boolean askFirst = keyPref.equals(KEY_CALLS) || keyPref.equals(KEY_SMS);
        //If Fragment not yet attached to Activity, return
        if (!isAdded()) {
            Log.d(TAG,"Fragment not yet attached to Activity, EXIT!!" );
            return;
        }
        simPref.clearItems();

        //Get num of activated Subs
        for (SubscriptionInfo subInfo : mSubInfoList) {
            if (subInfo != null && subInfo.mStatus == mSubscriptionManager.ACTIVE) mActCount++;
        }

        if (askFirst && mActCount > 1) {
            simPref.addItem(getResources().getString(
                    R.string.sim_calls_ask_first_prefs_title), null);
        }

        final int subAvailableSize = mAvailableSubInfos.size();
        for (int i = 0; i < subAvailableSize; ++i) {
            final SubscriptionInfo sir = mAvailableSubInfos.get(i);
            if(sir != null){
                simPref.addItem(sir.getDisplayName().toString(), sir);
            }
        }

        simPref.setCallback(new DropDownPreference.Callback() {
            @Override
            public boolean onItemSelected(int pos, Object value) {
                final int subId = value == null ? 0 :
                        ((SubscriptionInfo)value).getSubscriptionId();

                Log.d(TAG,"calling setCallback: " + simPref.getKey() + "subId: " + subId);
                if (simPref.getKey().equals(KEY_CELLULAR_DATA)) {
                    if (mSubscriptionManager.getDefaultDataSubId() != subId) {
                        mSubscriptionManager.setDefaultDataSubId(subId);
                    }
                } else if (simPref.getKey().equals(KEY_CALLS)) {
                    //subId 0 is meant for "Ask First"/"Prompt" option as per AOSP
                    if (subId == 0) {
                        mSubscriptionManager.setVoicePromptEnabled(true);
                    } else {
                        mSubscriptionManager.setVoicePromptEnabled(false);
                        if (mSubscriptionManager.getDefaultVoiceSubId() != subId) {
                            mSubscriptionManager.setDefaultVoiceSubId(subId);
                        }
                    }
                } else if (simPref.getKey().equals(KEY_SMS)) {
                    if (subId == 0) {
                        mSubscriptionManager.setSMSPromptEnabled(true);
                    } else {
                        mSubscriptionManager.setSMSPromptEnabled(false);
                        if (mSubscriptionManager.getDefaultSmsSubId() != subId) {
                            mSubscriptionManager.setDefaultSmsSubId(subId);
                        }
                    }
                }

                return true;
            }
        });
    }

    private void setActivity(Preference preference, SubscriptionInfo sir) {
        final String key = preference.getKey();

        if (key.equals(KEY_CELLULAR_DATA)) {
            mCellularData = sir;
        } else if (key.equals(KEY_CALLS)) {
            mCalls = sir;
        } else if (key.equals(KEY_SMS)) {
            mSMS = sir;
        }

        updateActivitesCategory();
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

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            logd("msg.what = " + msg.what);
            switch(msg.what) {
                case EVT_UPDATE:
                    updateAllOptions();
                    break;
                default:
                    break;
            }
        }
    };

    private void updateSimEnablers() {
        for (int i = 0; i < mSimEnablers.size(); ++i) {
            MultiSimEnablerPreference simEnabler = mSimEnablers.get(i);
            if (simEnabler != null) simEnabler.update();
        }
    }

    private void logd(String msg) {
        if (DBG) Log.d(TAG, msg);
    }

    private void loge(String s) {
        Log.e(TAG, s);
    }
}
