/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.settings.accounts;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SyncAdapterType;
import android.content.SyncInfo;
import android.content.SyncStatusInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.UserInfo;
import android.os.Binder;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.support.v7.preference.Preference;
import android.text.TextUtils;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.collect.Lists;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.internal.telephony.OperatorSimInfo;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settingslib.RestrictedLockUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;

public class AccountSyncSettings extends AccountPreferenceBase {

    public static final String ACCOUNT_KEY = "account";
    private static final int MENU_SYNC_NOW_ID       = Menu.FIRST;
    private static final int MENU_SYNC_CANCEL_ID    = Menu.FIRST + 1;
    private static final int MENU_REMOVE_ACCOUNT_ID = Menu.FIRST + 2;
    private static final int REALLY_REMOVE_DIALOG = 100;
    private static final int FAILED_REMOVAL_DIALOG = 101;
    private static final int CANT_DO_ONETIME_SYNC_DIALOG = 102;

    private TextView mUserId;
    private TextView mProviderId;
    private ImageView mProviderIcon;
    private TextView mErrorInfoView;
    private Account mAccount;
    private ArrayList<SyncStateSwitchPreference> mSwitches =
                new ArrayList<SyncStateSwitchPreference>();
    private ArrayList<SyncAdapterType> mInvisibleAdapters = Lists.newArrayList();

    private final String SIM1_ACCOUNT_NAME = "SIM1";
    private final String SIM2_ACCOUNT_NAME = "SIM2";

    @Override
    public Dialog onCreateDialog(final int id) {
        Dialog dialog = null;
        if (id == REALLY_REMOVE_DIALOG) {
            dialog = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.really_remove_account_title)
                .setMessage(R.string.really_remove_account_message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.remove_account_label,
                        new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Activity activity = getActivity();
                        AccountManager.get(activity)
                                .removeAccountAsUser(mAccount, activity,
                                new AccountManagerCallback<Bundle>() {
                            @Override
                            public void run(AccountManagerFuture<Bundle> future) {
                                // If already out of this screen, don't proceed.
                                if (!AccountSyncSettings.this.isResumed()) {
                                    return;
                                }
                                boolean failed = true;
                                try {
                                    if (future.getResult()
                                            .getBoolean(AccountManager.KEY_BOOLEAN_RESULT)) {
                                        failed = false;
                                    }
                                } catch (OperationCanceledException e) {
                                    // handled below
                                } catch (IOException e) {
                                    // handled below
                                } catch (AuthenticatorException e) {
                                    // handled below
                                }
                                if (failed && getActivity() != null &&
                                        !getActivity().isFinishing()) {
                                    showDialog(FAILED_REMOVAL_DIALOG);
                                } else {
                                    finish();
                                }
                            }
                        }, null, mUserHandle);
                    }
                })
                .create();
        } else if (id == FAILED_REMOVAL_DIALOG) {
            dialog = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.really_remove_account_title)
                .setPositiveButton(android.R.string.ok, null)
                .setMessage(R.string.remove_account_failed)
                .create();
        } else if (id == CANT_DO_ONETIME_SYNC_DIALOG) {
            dialog = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.cant_sync_dialog_title)
                .setMessage(R.string.cant_sync_dialog_message)
                .setPositiveButton(android.R.string.ok, null)
                .create();
        }
        return dialog;
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.ACCOUNTS_ACCOUNT_SYNC;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setPreferenceScreen(null);
        addPreferencesFromResource(R.xml.account_sync_settings);
        getPreferenceScreen().setOrderingAsAdded(false);
        setAccessibilityTitle();

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.account_sync_screen, container, false);

        final ViewGroup prefs_container = (ViewGroup) view.findViewById(R.id.prefs_container);
        Utils.prepareCustomPreferencesList(container, view, prefs_container, false);
        View prefs = super.onCreateView(inflater, prefs_container, savedInstanceState);
        prefs_container.addView(prefs);

        initializeUi(view);

        return view;
    }

    protected void initializeUi(final View rootView) {
        mErrorInfoView = (TextView) rootView.findViewById(R.id.sync_settings_error_info);
        mErrorInfoView.setVisibility(View.GONE);

        mUserId = (TextView) rootView.findViewById(R.id.user_id);
        mProviderId = (TextView) rootView.findViewById(R.id.provider_id);
        mProviderIcon = (ImageView) rootView.findViewById(R.id.provider_icon);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Bundle arguments = getArguments();
        if (arguments == null) {
            Log.e(TAG, "No arguments provided when starting intent. ACCOUNT_KEY needed.");
            finish();
            return;
        }
        mAccount = (Account) arguments.getParcelable(ACCOUNT_KEY);
        if (!accountExists(mAccount)) {
            Log.e(TAG, "Account provided does not exist: " + mAccount);
            finish();
            return;
        }
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
          Log.v(TAG, "Got account: " + mAccount);
        }
        mUserId.setText(mAccount.name);
        mProviderId.setText(mAccount.type);
    }

    private void setAccessibilityTitle() {
        final UserManager um = (UserManager) getSystemService(Context.USER_SERVICE);
        UserInfo user = um.getUserInfo(mUserHandle.getIdentifier());
        boolean isWorkProfile = user != null ? user.isManagedProfile() : false;
        CharSequence currentTitle = getActivity().getTitle();
        String accessibilityTitle =
                getString(isWorkProfile
                        ? R.string.accessibility_work_account_title
                        : R.string.accessibility_personal_account_title, currentTitle);
        getActivity().setTitle(Utils.createAccessibleSequence(currentTitle, accessibilityTitle));
    }

    @Override
    public void onResume() {
        removePreference("dummy");
        mAuthenticatorHelper.listenToAccountUpdates();
        updateAuthDescriptions();
        onAccountsUpdate(Binder.getCallingUserHandle());
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mAuthenticatorHelper.stopListeningToAccountUpdates();
    }

    private void addSyncStateSwitch(Account account, String authority,
            String packageName, int uid) {
        SyncStateSwitchPreference item = (SyncStateSwitchPreference) getCachedPreference(authority);
        if (item == null) {
            item = new SyncStateSwitchPreference(getPrefContext(), account, authority,
                    packageName, uid);
            getPreferenceScreen().addPreference(item);
        } else {
            item.setup(account, authority, packageName, uid);
        }
        item.setPersistent(false);
        final ProviderInfo providerInfo = getPackageManager().resolveContentProviderAsUser(
                authority, 0, mUserHandle.getIdentifier());
        if (providerInfo == null) {
            return;
        }
        CharSequence providerLabel = providerInfo.loadLabel(getPackageManager());
        if (TextUtils.isEmpty(providerLabel)) {
            Log.e(TAG, "Provider needs a label for authority '" + authority + "'");
            return;
        }
        String title = getString(R.string.sync_item_title, providerLabel);
        item.setTitle(title);
        item.setKey(authority);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        MenuItem syncNow = menu.add(0, MENU_SYNC_NOW_ID, 0,
                getString(R.string.sync_menu_sync_now))
                .setIcon(R.drawable.ic_menu_refresh_holo_dark);
        MenuItem syncCancel = menu.add(0, MENU_SYNC_CANCEL_ID, 0,
                getString(R.string.sync_menu_sync_cancel))
                .setIcon(com.android.internal.R.drawable.ic_menu_close_clear_cancel);

        MenuItem removeAccount = menu.add(0, MENU_REMOVE_ACCOUNT_ID, 0,
                getString(R.string.remove_account_label))
                .setIcon(R.drawable.ic_menu_delete);
        removeAccount.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER |
                MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        if (RestrictedLockUtils.hasBaseUserRestriction(getPrefContext(),
                UserManager.DISALLOW_MODIFY_ACCOUNTS, mUserHandle.getIdentifier())) {
            removeAccount.setEnabled(false);
        } else {
            EnforcedAdmin admin = RestrictedLockUtils.checkIfRestrictionEnforced(
                    getPrefContext(), UserManager.DISALLOW_MODIFY_ACCOUNTS,
                    mUserHandle.getIdentifier());
            if (admin == null) {
                admin = RestrictedLockUtils.checkIfAccountManagementDisabled(
                        getPrefContext(), mAccount.type, mUserHandle.getIdentifier());
            }
            RestrictedLockUtils.setMenuItemAsDisabledByAdmin(getPrefContext(),
                    removeAccount, admin);
        }

        syncNow.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER |
                MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        syncCancel.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER |
                MenuItem.SHOW_AS_ACTION_WITH_TEXT);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // Note that this also counts accounts that are not currently displayed
        boolean syncActive = !ContentResolver.getCurrentSyncsAsUser(
                mUserHandle.getIdentifier()).isEmpty();
        menu.findItem(MENU_SYNC_NOW_ID).setVisible(!syncActive);
        menu.findItem(MENU_SYNC_CANCEL_ID).setVisible(syncActive);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_SYNC_NOW_ID:
                startSyncForEnabledProviders();
                return true;
            case MENU_SYNC_CANCEL_ID:
                cancelSyncForEnabledProviders();
                return true;
            case MENU_REMOVE_ACCOUNT_ID:
                showDialog(REALLY_REMOVE_DIALOG);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            final int uid = requestCode;
            final int count = getPreferenceScreen().getPreferenceCount();
            for (int i = 0; i < count; i++) {
                Preference preference = getPreferenceScreen().getPreference(i);
                if (preference instanceof SyncStateSwitchPreference) {
                    SyncStateSwitchPreference syncPref = (SyncStateSwitchPreference) preference;
                    if (syncPref.getUid() == uid) {
                        onPreferenceTreeClick(syncPref);
                        return;
                    }
                }
            }
        }
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (getActivity() == null) {
            return false;
        }
        if (preference instanceof SyncStateSwitchPreference) {
            SyncStateSwitchPreference syncPref = (SyncStateSwitchPreference) preference;
            String authority = syncPref.getAuthority();
            Account account = syncPref.getAccount();
            final int userId = mUserHandle.getIdentifier();
            String packageName = syncPref.getPackageName();

            boolean syncAutomatically = ContentResolver.getSyncAutomaticallyAsUser(account,
                    authority, userId);
            if (syncPref.isOneTimeSyncMode()) {
                // If the sync adapter doesn't have access to the account we either
                // request access by starting an activity if possible or kick off the
                // sync which will end up posting an access request notification.
                if (requestAccountAccessIfNeeded(packageName)) {
                    return true;
                }
                requestOrCancelSync(account, authority, true);
            } else {
                boolean syncOn = syncPref.isChecked();
                boolean oldSyncState = syncAutomatically;
                if (syncOn != oldSyncState) {
                    // Toggling this switch triggers sync but we may need a user approval.
                    // If the sync adapter doesn't have access to the account we either
                    // request access by starting an activity if possible or kick off the
                    // sync which will end up posting an access request notification.
                    if (syncOn && requestAccountAccessIfNeeded(packageName)) {
                        return true;
                    }
                    // if we're enabling sync, this will request a sync as well
                    ContentResolver.setSyncAutomaticallyAsUser(account, authority, syncOn, userId);
                    // if the master sync switch is off, the request above will
                    // get dropped.  when the user clicks on this toggle,
                    // we want to force the sync, however.
                    if (!ContentResolver.getMasterSyncAutomaticallyAsUser(userId) || !syncOn) {
                        requestOrCancelSync(account, authority, syncOn);
                    }
                }
            }
            return true;
        } else {
            return super.onPreferenceTreeClick(preference);
        }
    }

    private boolean requestAccountAccessIfNeeded(String packageName) {
        if (packageName == null) {
            return false;
        }

        final int uid;
        try {
            uid = getContext().getPackageManager().getPackageUidAsUser(
                    packageName, mUserHandle.getIdentifier());
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Invalid sync ", e);
            return false;
        }

        AccountManager accountManager = getContext().getSystemService(AccountManager.class);
        if (!accountManager.hasAccountAccess(mAccount, packageName, mUserHandle)) {
            IntentSender intent = accountManager.createRequestAccountAccessIntentSenderAsUser(
                    mAccount, packageName, mUserHandle);
            if (intent != null) {
                try {
                    startIntentSenderForResult(intent, uid, null, 0, 0, 0, null);
                    return true;
                } catch (IntentSender.SendIntentException e) {
                    Log.e(TAG, "Error requesting account access", e);
                }
            }
        }
        return false;
    }

    private void startSyncForEnabledProviders() {
        requestOrCancelSyncForEnabledProviders(true /* start them */);
        final Activity activity = getActivity();
        if (activity != null) {
            activity.invalidateOptionsMenu();
        }
    }

    private void cancelSyncForEnabledProviders() {
        requestOrCancelSyncForEnabledProviders(false /* cancel them */);
        final Activity activity = getActivity();
        if (activity != null) {
            activity.invalidateOptionsMenu();
        }
    }

    private void requestOrCancelSyncForEnabledProviders(boolean startSync) {
        // sync everything that the user has enabled
        int count = getPreferenceScreen().getPreferenceCount();
        for (int i = 0; i < count; i++) {
            Preference pref = getPreferenceScreen().getPreference(i);
            if (! (pref instanceof SyncStateSwitchPreference)) {
                continue;
            }
            SyncStateSwitchPreference syncPref = (SyncStateSwitchPreference) pref;
            if (!syncPref.isChecked()) {
                continue;
            }
            requestOrCancelSync(syncPref.getAccount(), syncPref.getAuthority(), startSync);
        }
        // plus whatever the system needs to sync, e.g., invisible sync adapters
        if (mAccount != null) {
            for (SyncAdapterType syncAdapter : mInvisibleAdapters) {
                  requestOrCancelSync(mAccount, syncAdapter.authority, startSync);
            }
        }
    }

    private void requestOrCancelSync(Account account, String authority, boolean flag) {
        if (flag) {
            Bundle extras = new Bundle();
            extras.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
            ContentResolver.requestSyncAsUser(account, authority, mUserHandle.getIdentifier(),
                    extras);
        } else {
            ContentResolver.cancelSyncAsUser(account, authority, mUserHandle.getIdentifier());
        }
    }

    private boolean isSyncing(List<SyncInfo> currentSyncs, Account account, String authority) {
        for (SyncInfo syncInfo : currentSyncs) {
            if (syncInfo.account.equals(account) && syncInfo.authority.equals(authority)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onSyncStateUpdated() {
        if (!isResumed()) return;
        setFeedsState();
        final Activity activity = getActivity();
        if (activity != null) {
            activity.invalidateOptionsMenu();
        }
    }

    private void setFeedsState() {
        // iterate over all the preferences, setting the state properly for each
        Date date = new Date();
        final int userId = mUserHandle.getIdentifier();
        List<SyncInfo> currentSyncs = ContentResolver.getCurrentSyncsAsUser(userId);
        boolean syncIsFailing = false;

        // Refresh the sync status switches - some syncs may have become active.
        updateAccountSwitches();

        for (int i = 0, count = getPreferenceScreen().getPreferenceCount(); i < count; i++) {
            Preference pref = getPreferenceScreen().getPreference(i);
            if (! (pref instanceof SyncStateSwitchPreference)) {
                continue;
            }
            SyncStateSwitchPreference syncPref = (SyncStateSwitchPreference) pref;

            String authority = syncPref.getAuthority();
            Account account = syncPref.getAccount();

            SyncStatusInfo status = ContentResolver.getSyncStatusAsUser(account, authority, userId);
            boolean syncEnabled = ContentResolver.getSyncAutomaticallyAsUser(account, authority,
                    userId);
            boolean authorityIsPending = status == null ? false : status.pending;
            boolean initialSync = status == null ? false : status.initialize;

            boolean activelySyncing = isSyncing(currentSyncs, account, authority);
            boolean lastSyncFailed = status != null
                    && status.lastFailureTime != 0
                    && status.getLastFailureMesgAsInt(0)
                       != ContentResolver.SYNC_ERROR_SYNC_ALREADY_IN_PROGRESS;
            if (!syncEnabled) lastSyncFailed = false;
            if (lastSyncFailed && !activelySyncing && !authorityIsPending) {
                syncIsFailing = true;
            }
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.d(TAG, "Update sync status: " + account + " " + authority +
                        " active = " + activelySyncing + " pend =" +  authorityIsPending);
            }

            final long successEndTime = (status == null) ? 0 : status.lastSuccessTime;
            if (!syncEnabled) {
                syncPref.setSummary(R.string.sync_disabled);
            } else if (activelySyncing) {
                syncPref.setSummary(R.string.sync_in_progress);
            } else if (successEndTime != 0) {
                date.setTime(successEndTime);
                final String timeString = formatSyncDate(date);
                syncPref.setSummary(getResources().getString(R.string.last_synced, timeString));
            } else {
                syncPref.setSummary("");
            }
            int syncState = ContentResolver.getIsSyncableAsUser(account, authority, userId);

            syncPref.setActive(activelySyncing && (syncState >= 0) &&
                    !initialSync);
            syncPref.setPending(authorityIsPending && (syncState >= 0) &&
                    !initialSync);

            syncPref.setFailed(lastSyncFailed);
            final boolean oneTimeSyncMode = !ContentResolver.getMasterSyncAutomaticallyAsUser(
                userId);
            syncPref.setOneTimeSyncMode(oneTimeSyncMode);
            syncPref.setChecked(oneTimeSyncMode || syncEnabled);
        }
        mErrorInfoView.setVisibility(syncIsFailing ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onAccountsUpdate(final UserHandle userHandle) {
        super.onAccountsUpdate(userHandle);
        if (!accountExists(mAccount)) {
            // The account was deleted
            finish();
            return;
        }
        updateAccountSwitches();
        onSyncStateUpdated();
    }

    private boolean accountExists(Account account) {
        if (account == null) return false;

        Account[] accounts = AccountManager.get(getActivity()).getAccountsByTypeAsUser(
                account.type, mUserHandle);
        final int count = accounts.length;
        for (int i = 0; i < count; i++) {
            if (accounts[i].equals(account)) {
                return true;
            }
        }
        return false;
    }

    private void updateAccountSwitches() {
        mInvisibleAdapters.clear();

        SyncAdapterType[] syncAdapters = ContentResolver.getSyncAdapterTypesAsUser(
                mUserHandle.getIdentifier());
        ArrayList<SyncAdapterType> authorities = new ArrayList<>();
        for (int i = 0, n = syncAdapters.length; i < n; i++) {
            final SyncAdapterType sa = syncAdapters[i];
            // Only keep track of sync adapters for this account
            if (!sa.accountType.equals(mAccount.type)) continue;
            if (sa.isUserVisible()) {
                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.d(TAG, "updateAccountSwitches: added authority " + sa.authority
                            + " to accountType " + sa.accountType);
                }
                authorities.add(sa);
            } else {
                // keep track of invisible sync adapters, so sync now forces
                // them to sync as well.
                mInvisibleAdapters.add(sa);
            }
        }

        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.d(TAG, "looking for sync adapters that match account " + mAccount);
        }
        cacheRemoveAllPrefs(getPreferenceScreen());
        for (int j = 0, m = authorities.size(); j < m; j++) {
            final SyncAdapterType syncAdapter = authorities.get(j);
            // We could check services here....
            int syncState = ContentResolver.getIsSyncableAsUser(mAccount, syncAdapter.authority,
                    mUserHandle.getIdentifier());
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.d(TAG, "  found authority " + syncAdapter.authority + " " + syncState);
            }
            if (syncState > 0) {
                final int uid;
                try {
                    uid = getContext().getPackageManager().getPackageUidAsUser(
                            syncAdapter.getPackageName(), mUserHandle.getIdentifier());
                    addSyncStateSwitch(mAccount, syncAdapter.authority,
                            syncAdapter.getPackageName(), uid);
                } catch (PackageManager.NameNotFoundException e) {
                    Log.e(TAG, "No uid for package" + syncAdapter.getPackageName(), e);
                }
            }
        }
        removeCachedPrefs(getPreferenceScreen());
    }

    /**
     * Updates the titlebar with an icon for the provider type.
     */
    @Override
    protected void onAuthDescriptionsUpdated() {
        super.onAuthDescriptionsUpdated();
        if (mAccount != null) {
            mProviderIcon.setImageDrawable(getDrawableForType(mAccount.type));
            mProviderId.setText(getLabelForType(mAccount.type));
            //Sim Icon Customisation feature change
            OperatorSimInfo operatorSimInfo = new OperatorSimInfo(getActivity().
                    getApplicationContext());
            boolean isCustomSimFeatureEnabled = operatorSimInfo.
                    isOperatorFeatureEnabled();
            if (isCustomSimFeatureEnabled) {
                String accountName = mAccount.name;
                if (accountName.equalsIgnoreCase(SIM1_ACCOUNT_NAME)) {
                    showOperatorSimIcon(0, operatorSimInfo);
                } else if (accountName.equalsIgnoreCase(SIM2_ACCOUNT_NAME)) {
                    showOperatorSimIcon(1, operatorSimInfo);
                }
            }

        }
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_url_accounts;
    }

    private void showOperatorSimIcon(int slotIndex, OperatorSimInfo operatorSimInfo) {
       boolean isSimTypeOperator = operatorSimInfo.isSimTypeOperator(slotIndex);
       if (isSimTypeOperator) {
           mProviderIcon.setImageDrawable(operatorSimInfo.getOperatorDrawable());
           mUserId.setText(operatorSimInfo.getOperatorDisplayName());
       } else {
           mProviderIcon.setImageDrawable(operatorSimInfo.getGenericSimDrawable());
           int subId = SubscriptionManager.getSubId(slotIndex)[0];
           String operatorName = TelephonyManager.from(getActivity().
                   getApplicationContext()).getSimOperator(subId);
           mUserId.setText(operatorName);
       }
   }
}
