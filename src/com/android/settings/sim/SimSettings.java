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
import android.preference.SwitchPreference;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SubInfoRecord;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

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
    private static final String SIM_DATA_CATEGORY = "sim_data_category";
    private static final String SIM_DATA_KEY = "sim_data";

    private static final int EVT_UPDATE = 1;
    private static int mNumSlots = 0;

    /**
     * By UX design we have use only one Subscription Information(SubInfo) record per SIM slot.
     * mAvalableSubInfos is the list of SubInfos we present to the user.
     * mSubInfoList is the list of all SubInfos.
     */
    private List<SubInfoRecord> mAvailableSubInfos = null;
    private List<SubInfoRecord> mSubInfoList = null;
    private Preference mPrimarySubSelect = null;

    private static List<MultiSimEnablerPreference> mSimEnablers = null;

    private SubInfoRecord mCellularData = null;
    private SubInfoRecord mCalls = null;
    private SubInfoRecord mSMS = null;

    private int mNumSims;
    private TelephonyManager mTelephonyManager;

    public SimSettings() {
        super(DISALLOW_CONFIG_SIM);
    }

    @Override
    public void onCreate(final Bundle bundle) {
        super.onCreate(bundle);
        Log.d(TAG,"on onCreate");
        mTelephonyManager = (TelephonyManager) getActivity()
                .getSystemService(Context.TELEPHONY_SERVICE);

        if (mSubInfoList == null) {
            mSubInfoList = SubscriptionManager.getActiveSubInfoList();
        }

        mNumSlots = mTelephonyManager.getSimCount();

        createPreferences();
        updateAllOptions();
        IntentFilter intentFilter =
                new IntentFilter(TelephonyIntents.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED);
        intentFilter.addAction(TelephonyIntents.ACTION_SUBINFO_CONTENT_CHANGE);

        getActivity().registerReceiver(mDdsSwitchReceiver, intentFilter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG,"on onDestroy");
        getActivity().unregisterReceiver(mDdsSwitchReceiver);
    }

    private BroadcastReceiver mDdsSwitchReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "Intent received: " + action);
            if (TelephonyIntents.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED.equals(action)) {
                updateCellularDataValues();
            } else if (TelephonyIntents.ACTION_SUBINFO_CONTENT_CHANGE.equals(action)) {
                mAvailableSubInfos.clear();
                mNumSims = 0;
                mSubInfoList = SubscriptionManager.getActiveSubInfoList();
                for (int i = 0; i < mNumSlots; ++i) {
                    final SubInfoRecord sir = findRecordBySlotId(i);
                    // Do not display deactivated subInfo in preference list
                    if ((sir != null) && (sir.mStatus == SubscriptionManager.ACTIVE)) {
                        mNumSims++;
                        mAvailableSubInfos.add(sir);
                    }
                }
                // Refresh UI whenever subinfo record gets changed
                updateAllOptions();
            }
            SwitchPreference dataToggle = (SwitchPreference) findPreference(SIM_DATA_KEY);
            if (dataToggle != null) {
                dataToggle.setChecked(isMobileDataEnabled(-1));
            }
        }
    };

    private void updateDSDAPreferences() {
        PreferenceCategory category = (PreferenceCategory) findPreference(SIM_DATA_CATEGORY);

        // Remove global data toggle
        category.removeAll();

        // More than one active data sim
        for (int i = 0; i < mNumSlots; ++i) {
            final SubInfoRecord sir = findRecordBySlotId(i);
            if (sir != null && sir.mStatus == SubscriptionManager.ACTIVE) {
                final SwitchPreference preference = new SwitchPreference(getActivity());
                preference.setChecked(isMobileDataEnabled(sir.slotId));
                preference.setTitle(R.string.cellular_data_title);
                preference.setSummary(sir.displayName + " - " + Integer.toString(i + 1)
                        + " " + sir.number);
                preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference2) {
                        int value = !preference.isChecked() ? 0 : 1;
                        Settings.Global.putInt(getActivity().getContentResolver(),
                                android.provider.Settings.Global.MOBILE_DATA + sir.slotId, value);
                        return true;
                    }
                });
                category.addPreference(preference);
            }
        }
    }

    private void addDataPreferences() {
        if (isDSDA()) {
            updateDSDAPreferences();
        } else {
            PreferenceCategory category = (PreferenceCategory) findPreference(SIM_DATA_CATEGORY);
            // Only one active data sim
            DropDownPreference preference = new DropDownPreference(getActivity());
            preference.setKey(KEY_CELLULAR_DATA);
            preference.setTitle(R.string.cellular_data_title);
            category.addPreference(preference);

            SwitchPreference dataToggle = (SwitchPreference) findPreference(SIM_DATA_KEY);
            dataToggle.setDependency(KEY_CELLULAR_DATA);
            dataToggle.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    final boolean dataEnabled = (Boolean) newValue;
                    mTelephonyManager.setDataEnabled(dataEnabled);
                    return true;
                }
            });
        }
    }

    private boolean isMobileDataEnabled(int id) {
        if (isDSDA()) {
            try {
                return TelephonyManager.getIntAtIndex(getActivity().getContentResolver(),
                        Settings.Global.MOBILE_DATA, id) != 0;
            } catch (SettingNotFoundException e) {
                return false;
            }
        } else {
            return mTelephonyManager.getDataEnabled();
        }
    }

    private boolean isDSDA() {
        TelephonyManager.MultiSimVariants configuration = TelephonyManager.getDefault()
                .getMultiSimConfiguration();
        return configuration == TelephonyManager.MultiSimVariants.DSDA;
    }

    private void createPreferences() {
        addPreferencesFromResource(R.xml.sim_settings);

        addDataPreferences();

        mPrimarySubSelect = (Preference) findPreference(KEY_PRIMARY_SUB_SELECT);
        final PreferenceCategory simEnablers =
                (PreferenceCategory)findPreference(SIM_ENABLER_CATEGORY);

        mAvailableSubInfos = new ArrayList<SubInfoRecord>(mNumSlots);
        mSimEnablers = new ArrayList<MultiSimEnablerPreference>(mNumSlots);
        for (int i = 0; i < mNumSlots; ++i) {
            final SubInfoRecord sir = findRecordBySlotId(i);
            if (mNumSlots > 1) {
                mSimEnablers.add(i, new MultiSimEnablerPreference(
                        getActivity(), sir, mHandler, i));
                simEnablers.addPreference(mSimEnablers.get(i));
            } else {
                removePreference(SIM_ENABLER_CATEGORY);
            }
            // Do not display deactivated subInfo in preference list
            if ((sir != null) && (sir.mStatus == SubscriptionManager.ACTIVE)) {
                mNumSims++;
                mAvailableSubInfos.add(sir);
            }
        }
    }

    private void updateAllOptions() {
        Log.d(TAG,"updateAllOptions");
        mSubInfoList = SubscriptionManager.getActiveSubInfoList();
        updateActivitesCategory();
        updateSimEnablers();
    }

    private void updateActivitesCategory() {
        if (!isDSDA()) {
            createDropDown((DropDownPreference) findPreference(KEY_CELLULAR_DATA));
        }
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
    private SubInfoRecord findRecordBySubId(final long subId) {
        final int availableSubInfoLength = mAvailableSubInfos.size();

        for (int i = 0; i < availableSubInfoLength; ++i) {
            final SubInfoRecord sir = mAvailableSubInfos.get(i);
            if (sir != null && sir.subId == subId) {
                return sir;
            }
        }
        return null;
    }

    /**
     * finds a record with slotId.
     * Since the number of SIMs are few, an array is fine.
     */
    private SubInfoRecord findRecordBySlotId(final int slotId) {
        if (mSubInfoList != null) {
            final int availableSubInfoLength = mSubInfoList.size();

            for (int i = 0; i < availableSubInfoLength; ++i) {
                final SubInfoRecord sir = mSubInfoList.get(i);
                if (sir.slotId == slotId) {
                    //Right now we take the first subscription on a SIM.
                    return sir;
                }
            }
        }

        return null;
    }

    private void updateSmsValues() {
        final DropDownPreference simPref = (DropDownPreference) findPreference(KEY_SMS);
        long subId = SubscriptionManager.isSMSPromptEnabled() ?
                0 : SubscriptionManager.getDefaultSmsSubId();
        final SubInfoRecord sir = findRecordBySubId(subId);
        if (sir != null) {
            simPref.setSelectedValue(sir, false);
        }
        simPref.setEnabled(mNumSims > 1);
    }

    private void updateCellularDataValues() {
        if (isDSDA()) {
            updateDSDAPreferences();
            return;
        }
        final DropDownPreference simPref = (DropDownPreference) findPreference(KEY_CELLULAR_DATA);
        final SubInfoRecord sir = findRecordBySubId(SubscriptionManager.getDefaultDataSubId());
        if (sir != null) {
            simPref.setSelectedValue(sir, false);
        }
        simPref.setEnabled(mNumSims > 1);
    }

    private void updateCallValues() {
        final DropDownPreference simPref = (DropDownPreference) findPreference(KEY_CALLS);
        long subId = SubscriptionManager.isVoicePromptEnabled() ?
                0 : SubscriptionManager.getDefaultVoiceSubId();
        final SubInfoRecord sir = findRecordBySubId(subId);
        if (sir != null) {
            simPref.setSelectedValue(sir, false);
        }
        simPref.setEnabled(mNumSims > 1);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG,"on Pause");
        for (int i = 0; i < mSimEnablers.size(); ++i) {
            MultiSimEnablerPreference simEnabler = mSimEnablers.get(i);
            if (simEnabler != null) simEnabler.cleanUp();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
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
            SubInfoRecord subInfo = findRecordBySlotId(primarySlot);
            CharSequence lteSummary = (subInfo == null ) ? null : subInfo.displayName;
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
        for (SubInfoRecord subInfo : mSubInfoList) {
            if (subInfo != null && subInfo.mStatus == SubscriptionManager.ACTIVE) mActCount++;
        }

        if (askFirst && mActCount > 1) {
            simPref.addItem(getResources().getString(
                    R.string.sim_calls_ask_first_prefs_title), null);
        }

        final int subAvailableSize = mAvailableSubInfos.size();
        for (int i = 0; i < subAvailableSize; ++i) {
            final SubInfoRecord sir = mAvailableSubInfos.get(i);
            if(sir != null){
                simPref.addItem(sir.displayName + " - " + Integer.toString(i+1), sir);
            }
        }

        simPref.setCallback(new DropDownPreference.Callback() {
            @Override
            public boolean onItemSelected(int pos, Object value) {
                final long subId = value == null ? 0 : ((SubInfoRecord)value).subId;

                Log.d(TAG,"calling setCallback: " + simPref.getKey() + "subId: " + subId);
                if (simPref.getKey().equals(KEY_CELLULAR_DATA)) {
                    if (SubscriptionManager.getDefaultDataSubId() != subId) {
                        SubscriptionManager.setDefaultDataSubId(subId);
                    }
                } else if (simPref.getKey().equals(KEY_CALLS)) {
                    //subId 0 is meant for "Ask First"/"Prompt" option as per AOSP
                    if (subId == 0) {
                        SubscriptionManager.setVoicePromptEnabled(true);
                    } else {
                        SubscriptionManager.setVoicePromptEnabled(false);
                        if (SubscriptionManager.getDefaultVoiceSubId() != subId) {
                            SubscriptionManager.setDefaultVoiceSubId(subId);
                        }
                    }
                } else if (simPref.getKey().equals(KEY_SMS)) {
                    if (subId == 0) {
                        SubscriptionManager.setSMSPromptEnabled(true);
                    } else {
                        SubscriptionManager.setSMSPromptEnabled(false);
                        if (SubscriptionManager.getDefaultSmsSubId() != subId) {
                            SubscriptionManager.setDefaultSmsSubId(subId);
                        }
                    }
                }

                return true;
            }
        });
    }

    private void setActivity(Preference preference, SubInfoRecord sir) {
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
