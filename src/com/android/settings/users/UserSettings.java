/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.settings.users;

import android.app.Activity;
import android.app.ActivityManagerNative;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceClickListener;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;
import android.provider.Settings.Global;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.SimpleAdapter;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.ChooseLockGeneric;
import com.android.settings.DimmableIconPreference;
import com.android.settings.OwnerInfoSettings;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedSwitchPreference;
import com.android.settingslib.drawable.CircleFramedDrawable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;

/**
 * Screen that manages the list of users on the device.
 * Guest user is an always visible entry, even if the guest is not currently
 * active/created. It is meant for controlling properties of a guest user.
 *
 * The first one is always the current user.
 * Owner is the primary user.
 */
public class UserSettings extends SettingsPreferenceFragment
        implements OnPreferenceClickListener, OnClickListener, DialogInterface.OnDismissListener,
        Preference.OnPreferenceChangeListener,
        EditUserInfoController.OnContentChangedCallback, Indexable {

    private static final String TAG = "UserSettings";

    /** UserId of the user being removed */
    private static final String SAVE_REMOVING_USER = "removing_user";
    /** UserId of the user that was just added */
    private static final String SAVE_ADDING_USER = "adding_user";

    private static final String KEY_USER_LIST = "user_list";
    private static final String KEY_USER_ME = "user_me";
    private static final String KEY_ADD_USER = "user_add";
    private static final String KEY_EMERGENCY_INFO = "emergency_info";

    private static final String ACTION_EDIT_EMERGENCY_INFO = "android.settings.EDIT_EMERGENGY_INFO";

    private static final int MENU_REMOVE_USER = Menu.FIRST;

    private static final int DIALOG_CONFIRM_REMOVE = 1;
    private static final int DIALOG_ADD_USER = 2;
    private static final int DIALOG_SETUP_USER = 3;
    private static final int DIALOG_SETUP_PROFILE = 4;
    private static final int DIALOG_USER_CANNOT_MANAGE = 5;
    private static final int DIALOG_CHOOSE_USER_TYPE = 6;
    private static final int DIALOG_NEED_LOCKSCREEN = 7;
    private static final int DIALOG_CONFIRM_EXIT_GUEST = 8;
    private static final int DIALOG_USER_PROFILE_EDITOR = 9;

    private static final int MESSAGE_UPDATE_LIST = 1;
    private static final int MESSAGE_SETUP_USER = 2;
    private static final int MESSAGE_CONFIG_USER = 3;

    private static final int USER_TYPE_USER = 1;
    private static final int USER_TYPE_RESTRICTED_PROFILE = 2;

    private static final int REQUEST_CHOOSE_LOCK = 10;

    private static final String KEY_ADD_USER_LONG_MESSAGE_DISPLAYED =
            "key_add_user_long_message_displayed";

    private static final String KEY_TITLE = "title";
    private static final String KEY_SUMMARY = "summary";

    private PreferenceGroup mUserListCategory;
    private UserPreference mMePreference;
    private DimmableIconPreference mAddUser;
    private PreferenceGroup mLockScreenSettings;
    private RestrictedSwitchPreference mAddUserWhenLocked;
    private Preference mEmergencyInfoPreference;
    private int mRemovingUserId = -1;
    private int mAddedUserId = 0;
    private boolean mAddingUser;
    private String mAddingUserName;
    private UserCapabilities mUserCaps;
    private boolean mShouldUpdateUserList = true;
    private final Object mUserLock = new Object();
    private UserManager mUserManager;
    private SparseArray<Bitmap> mUserIcons = new SparseArray<Bitmap>();

    private EditUserInfoController mEditUserInfoController =
            new EditUserInfoController();

    // A place to cache the generated default avatar
    private Drawable mDefaultIconDrawable;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_UPDATE_LIST:
                updateUserList();
                break;
            case MESSAGE_SETUP_USER:
                onUserCreated(msg.arg1);
                break;
            case MESSAGE_CONFIG_USER:
                onManageUserClicked(msg.arg1, true);
                break;
            }
        }
    };

    private BroadcastReceiver mUserChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_USER_REMOVED)) {
                mRemovingUserId = -1;
            } else if (intent.getAction().equals(Intent.ACTION_USER_INFO_CHANGED)) {
                int userHandle = intent.getIntExtra(Intent.EXTRA_USER_HANDLE, -1);
                if (userHandle != -1) {
                    mUserIcons.remove(userHandle);
                }
            }
            mHandler.sendEmptyMessage(MESSAGE_UPDATE_LIST);
        }
    };

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.USER;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        if (icicle != null) {
            if (icicle.containsKey(SAVE_ADDING_USER)) {
                mAddedUserId = icicle.getInt(SAVE_ADDING_USER);
            }
            if (icicle.containsKey(SAVE_REMOVING_USER)) {
                mRemovingUserId = icicle.getInt(SAVE_REMOVING_USER);
            }
            mEditUserInfoController.onRestoreInstanceState(icicle);
        }
        final Context context = getActivity();
        mUserCaps = UserCapabilities.create(context);
        mUserManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
        if (!mUserCaps.mEnabled) {
            return;
        }

        final int myUserId = UserHandle.myUserId();

        addPreferencesFromResource(R.xml.user_settings);
        mUserListCategory = (PreferenceGroup) findPreference(KEY_USER_LIST);
        mMePreference = new UserPreference(getPrefContext(), null /* attrs */, myUserId,
                null /* settings icon handler */,
                null /* delete icon handler */);
        mMePreference.setKey(KEY_USER_ME);
        mMePreference.setOnPreferenceClickListener(this);
        if (mUserCaps.mIsAdmin) {
            mMePreference.setSummary(R.string.user_admin);
        }
        mAddUser = (DimmableIconPreference) findPreference(KEY_ADD_USER);
        mAddUser.useAdminDisabledSummary(false);
        // Determine if add user/profile button should be visible
        if (mUserCaps.mCanAddUser && Utils.isDeviceProvisioned(getActivity())) {
            mAddUser.setOnPreferenceClickListener(this);
            // change label to only mention user, if restricted profiles are not supported
            if (!mUserCaps.mCanAddRestrictedProfile) {
                mAddUser.setTitle(R.string.user_add_user_menu);
            }
        }
        mLockScreenSettings = (PreferenceGroup) findPreference("lock_screen_settings");
        mAddUserWhenLocked = (RestrictedSwitchPreference) findPreference("add_users_when_locked");
        mEmergencyInfoPreference = findPreference(KEY_EMERGENCY_INFO);
        setHasOptionsMenu(true);
        IntentFilter filter = new IntentFilter(Intent.ACTION_USER_REMOVED);
        filter.addAction(Intent.ACTION_USER_INFO_CHANGED);
        context.registerReceiverAsUser(mUserChangeReceiver, UserHandle.ALL, filter, null, mHandler);
        loadProfile();
        updateUserList();
        mShouldUpdateUserList = false;

        if (Global.getInt(getContext().getContentResolver(), Global.DEVICE_PROVISIONED, 0) == 0) {
            getActivity().finish();
            return;
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!mUserCaps.mEnabled) return;
        if (mShouldUpdateUserList) {
            mUserCaps.updateAddUserCapabilities(getActivity());
            loadProfile();
            updateUserList();
        }
    }

    @Override
    public void onPause() {
        mShouldUpdateUserList = true;
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (!mUserCaps.mEnabled) return;

        getActivity().unregisterReceiver(mUserChangeReceiver);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mEditUserInfoController.onSaveInstanceState(outState);
        outState.putInt(SAVE_ADDING_USER, mAddedUserId);
        outState.putInt(SAVE_REMOVING_USER, mRemovingUserId);
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        mEditUserInfoController.startingActivityForResult();
        super.startActivityForResult(intent, requestCode);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        int pos = 0;
        UserManager um = getContext().getSystemService(UserManager.class);
        boolean allowRemoveUser = !um.hasUserRestriction(UserManager.DISALLOW_REMOVE_USER);
        boolean canSwitchUsers = um.canSwitchUsers();
        if (!mUserCaps.mIsAdmin && allowRemoveUser && canSwitchUsers) {
            String nickname = mUserManager.getUserName();
            MenuItem removeThisUser = menu.add(0, MENU_REMOVE_USER, pos++,
                    getResources().getString(R.string.user_remove_user_menu, nickname));
            removeThisUser.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == MENU_REMOVE_USER) {
            onRemoveUserClicked(UserHandle.myUserId());
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Loads profile information for the current user.
     */
    private void loadProfile() {
        if (mUserCaps.mIsGuest) {
            // No need to load profile information
            mMePreference.setIcon(getEncircledDefaultIcon());
            mMePreference.setTitle(R.string.user_exit_guest_title);
            return;
        }

        new AsyncTask<Void, Void, String>() {
            @Override
            protected void onPostExecute(String result) {
                finishLoadProfile(result);
            }

            @Override
            protected String doInBackground(Void... values) {
                UserInfo user = mUserManager.getUserInfo(UserHandle.myUserId());
                if (user.iconPath == null || user.iconPath.equals("")) {
                    // Assign profile photo.
                    Utils.copyMeProfilePhoto(getActivity(), user);
                }
                return user.name;
            }
        }.execute();
    }

    private void finishLoadProfile(String profileName) {
        if (getActivity() == null) return;
        mMePreference.setTitle(getString(R.string.user_you, profileName));
        int myUserId = UserHandle.myUserId();
        Bitmap b = mUserManager.getUserIcon(myUserId);
        if (b != null) {
            mMePreference.setIcon(encircle(b));
            mUserIcons.put(myUserId, b);
        }
    }

    private boolean hasLockscreenSecurity() {
        LockPatternUtils lpu = new LockPatternUtils(getActivity());
        return lpu.isSecure(UserHandle.myUserId());
    }

    private void launchChooseLockscreen() {
        Intent chooseLockIntent = new Intent(DevicePolicyManager.ACTION_SET_NEW_PASSWORD);
        chooseLockIntent.putExtra(ChooseLockGeneric.ChooseLockGenericFragment.MINIMUM_QUALITY_KEY,
                DevicePolicyManager.PASSWORD_QUALITY_SOMETHING);
        startActivityForResult(chooseLockIntent, REQUEST_CHOOSE_LOCK);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CHOOSE_LOCK) {
            if (resultCode != Activity.RESULT_CANCELED && hasLockscreenSecurity()) {
                addUserNow(USER_TYPE_RESTRICTED_PROFILE);
            }
        } else {
            mEditUserInfoController.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void onAddUserClicked(int userType) {
        synchronized (mUserLock) {
            if (mRemovingUserId == -1 && !mAddingUser) {
                switch (userType) {
                case USER_TYPE_USER:
                    showDialog(DIALOG_ADD_USER);
                    break;
                case USER_TYPE_RESTRICTED_PROFILE:
                    if (hasLockscreenSecurity()) {
                        addUserNow(USER_TYPE_RESTRICTED_PROFILE);
                    } else {
                        showDialog(DIALOG_NEED_LOCKSCREEN);
                    }
                    break;
                }
            }
        }
    }

    private void onRemoveUserClicked(int userId) {
        synchronized (mUserLock) {
            if (mRemovingUserId == -1 && !mAddingUser) {
                mRemovingUserId = userId;
                showDialog(DIALOG_CONFIRM_REMOVE);
            }
        }
    }

    private UserInfo createRestrictedProfile() {
        UserInfo newUserInfo = mUserManager.createRestrictedProfile(mAddingUserName);
        Utils.assignDefaultPhoto(getActivity(), newUserInfo.id);
        return newUserInfo;
    }

    private UserInfo createTrustedUser() {
        UserInfo newUserInfo = mUserManager.createUser(mAddingUserName, 0);
        if (newUserInfo != null) {
            Utils.assignDefaultPhoto(getActivity(), newUserInfo.id);
        }
        return newUserInfo;
    }

    private void onManageUserClicked(int userId, boolean newUser) {
        mAddingUser = false;
        if (userId == UserPreference.USERID_GUEST_DEFAULTS) {
            Bundle extras = new Bundle();
            extras.putBoolean(UserDetailsSettings.EXTRA_USER_GUEST, true);
            ((SettingsActivity) getActivity()).startPreferencePanel(
                    UserDetailsSettings.class.getName(),
                    extras, R.string.user_guest, null, null, 0);
            return;
        }
        UserInfo info = mUserManager.getUserInfo(userId);
        if (info.isRestricted() && mUserCaps.mIsAdmin) {
            Bundle extras = new Bundle();
            extras.putInt(RestrictedProfileSettings.EXTRA_USER_ID, userId);
            extras.putBoolean(RestrictedProfileSettings.EXTRA_NEW_USER, newUser);
            ((SettingsActivity) getActivity()).startPreferencePanel(
                    RestrictedProfileSettings.class.getName(),
                    extras, R.string.user_restrictions_title, null,
                    null, 0);
        } else if (info.id == UserHandle.myUserId()) {
            // Jump to owner info panel
            OwnerInfoSettings.show(this);
        } else if (mUserCaps.mIsAdmin) {
            Bundle extras = new Bundle();
            extras.putInt(UserDetailsSettings.EXTRA_USER_ID, userId);
            ((SettingsActivity) getActivity()).startPreferencePanel(
                    UserDetailsSettings.class.getName(),
                    extras,
                    -1, /* No title res id */
                    info.name, /* title */
                    null, /* resultTo */
                    0 /* resultRequestCode */);
        }
    }

    private void onUserCreated(int userId) {
        mAddedUserId = userId;
        mAddingUser = false;
        if (mUserManager.getUserInfo(userId).isRestricted()) {
            showDialog(DIALOG_SETUP_PROFILE);
        } else {
            showDialog(DIALOG_SETUP_USER);
        }
    }

    @Override
    public void onDialogShowing() {
        super.onDialogShowing();

        setOnDismissListener(this);
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        Context context = getActivity();
        if (context == null) return null;
        switch (dialogId) {
            case DIALOG_CONFIRM_REMOVE: {
                Dialog dlg =
                        UserDialogs.createRemoveDialog(getActivity(), mRemovingUserId,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        removeUserNow();
                                    }
                                }
                        );
                return dlg;
            }
            case DIALOG_USER_CANNOT_MANAGE:
                return new AlertDialog.Builder(context)
                    .setMessage(R.string.user_cannot_manage_message)
                    .setPositiveButton(android.R.string.ok, null)
                    .create();
            case DIALOG_ADD_USER: {
                final SharedPreferences preferences = getActivity().getPreferences(
                        Context.MODE_PRIVATE);
                final boolean longMessageDisplayed = preferences.getBoolean(
                        KEY_ADD_USER_LONG_MESSAGE_DISPLAYED, false);
                final int messageResId = longMessageDisplayed
                        ? R.string.user_add_user_message_short
                        : R.string.user_add_user_message_long;
                final int userType = dialogId == DIALOG_ADD_USER
                        ? USER_TYPE_USER : USER_TYPE_RESTRICTED_PROFILE;
                Dialog dlg = new AlertDialog.Builder(context)
                    .setTitle(R.string.user_add_user_title)
                    .setMessage(messageResId)
                    .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                addUserNow(userType);
                                if (!longMessageDisplayed) {
                                    preferences.edit().putBoolean(
                                            KEY_ADD_USER_LONG_MESSAGE_DISPLAYED, true).apply();
                                }
                            }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .create();
                return dlg;
            }
            case DIALOG_SETUP_USER: {
                Dialog dlg = new AlertDialog.Builder(context)
                    .setTitle(R.string.user_setup_dialog_title)
                    .setMessage(R.string.user_setup_dialog_message)
                    .setPositiveButton(R.string.user_setup_button_setup_now,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                switchUserNow(mAddedUserId);
                            }
                    })
                    .setNegativeButton(R.string.user_setup_button_setup_later, null)
                    .create();
                return dlg;
            }
            case DIALOG_SETUP_PROFILE: {
                Dialog dlg = new AlertDialog.Builder(context)
                    .setMessage(R.string.user_setup_profile_dialog_message)
                    .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                switchUserNow(mAddedUserId);
                            }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .create();
                return dlg;
            }
            case DIALOG_CHOOSE_USER_TYPE: {
                List<HashMap<String, String>> data = new ArrayList<HashMap<String,String>>();
                HashMap<String,String> addUserItem = new HashMap<String,String>();
                addUserItem.put(KEY_TITLE, getString(R.string.user_add_user_item_title));
                addUserItem.put(KEY_SUMMARY, getString(R.string.user_add_user_item_summary));
                HashMap<String,String> addProfileItem = new HashMap<String,String>();
                addProfileItem.put(KEY_TITLE, getString(R.string.user_add_profile_item_title));
                addProfileItem.put(KEY_SUMMARY, getString(R.string.user_add_profile_item_summary));
                data.add(addUserItem);
                data.add(addProfileItem);
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                SimpleAdapter adapter = new SimpleAdapter(builder.getContext(),
                        data, R.layout.two_line_list_item,
                        new String[] {KEY_TITLE, KEY_SUMMARY},
                        new int[] {R.id.title, R.id.summary});
                builder.setTitle(R.string.user_add_user_type_title);
                builder.setAdapter(adapter,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                onAddUserClicked(which == 0
                                        ? USER_TYPE_USER
                                        : USER_TYPE_RESTRICTED_PROFILE);
                            }
                        });
                return builder.create();
            }
            case DIALOG_NEED_LOCKSCREEN: {
                Dialog dlg = new AlertDialog.Builder(context)
                        .setMessage(R.string.user_need_lock_message)
                        .setPositiveButton(R.string.user_set_lock_button,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        launchChooseLockscreen();
                                    }
                                })
                        .setNegativeButton(android.R.string.cancel, null)
                        .create();
                return dlg;
            }
            case DIALOG_CONFIRM_EXIT_GUEST: {
                Dialog dlg = new AlertDialog.Builder(context)
                        .setTitle(R.string.user_exit_guest_confirm_title)
                        .setMessage(R.string.user_exit_guest_confirm_message)
                        .setPositiveButton(R.string.user_exit_guest_dialog_remove,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        exitGuest();
                                    }
                                })
                        .setNegativeButton(android.R.string.cancel, null)
                        .create();
                return dlg;
            }
            case DIALOG_USER_PROFILE_EDITOR: {
                Dialog dlg = mEditUserInfoController.createDialog(
                        this,
                        mMePreference.getIcon(),
                        mMePreference.getTitle(),
                        R.string.profile_info_settings_title,
                        this /* callback */,
                        android.os.Process.myUserHandle());
                return dlg;
            }
            default:
                return null;
        }
    }

    private static boolean emergencyInfoActivityPresent(Context context) {
        Intent intent = new Intent(ACTION_EDIT_EMERGENCY_INFO).setPackage("com.android.emergency");
        List<ResolveInfo> infos = context.getPackageManager().queryIntentActivities(intent, 0);
        if (infos == null || infos.isEmpty()) {
            return false;
        }
        return true;
    }

    private void removeUserNow() {
        if (mRemovingUserId == UserHandle.myUserId()) {
            removeThisUser();
        } else {
            new Thread() {
                public void run() {
                    synchronized (mUserLock) {
                        mUserManager.removeUser(mRemovingUserId);
                        mHandler.sendEmptyMessage(MESSAGE_UPDATE_LIST);
                    }
                }
            }.start();
        }
    }

    private void removeThisUser() {
        if (!mUserManager.canSwitchUsers()) {
            Log.w(TAG, "Cannot remove current user when switching is disabled");
            return;
        }
        try {
            ActivityManagerNative.getDefault().switchUser(UserHandle.USER_SYSTEM);
            getContext().getSystemService(UserManager.class).removeUser(UserHandle.myUserId());
        } catch (RemoteException re) {
            Log.e(TAG, "Unable to remove self user");
        }
    }

    private void addUserNow(final int userType) {
        synchronized (mUserLock) {
            mAddingUser = true;
            mAddingUserName = userType == USER_TYPE_USER ? getString(R.string.user_new_user_name)
                    : getString(R.string.user_new_profile_name);
            //updateUserList();
            new Thread() {
                public void run() {
                    UserInfo user;
                    // Could take a few seconds
                    if (userType == USER_TYPE_USER) {
                        user = createTrustedUser();
                    } else {
                        user = createRestrictedProfile();
                    }
                    if (user == null) {
                        mAddingUser = false;
                        return;
                    }
                    synchronized (mUserLock) {
                        if (userType == USER_TYPE_USER) {
                            mHandler.sendEmptyMessage(MESSAGE_UPDATE_LIST);
                            mHandler.sendMessage(mHandler.obtainMessage(
                                    MESSAGE_SETUP_USER, user.id, user.serialNumber));
                        } else {
                            mHandler.sendMessage(mHandler.obtainMessage(
                                    MESSAGE_CONFIG_USER, user.id, user.serialNumber));
                        }
                    }
                }
            }.start();
        }
    }

    private void switchUserNow(int userId) {
        try {
            ActivityManagerNative.getDefault().switchUser(userId);
        } catch (RemoteException re) {
            // Nothing to do
        }
    }

    /**
     * Erase the current user (guest) and switch to another user.
     */
    private void exitGuest() {
        // Just to be safe
        if (!mUserCaps.mIsGuest) {
            return;
        }
        removeThisUser();
    }

    private void updateUserList() {
        if (getActivity() == null) return;
        List<UserInfo> users = mUserManager.getUsers(true);
        final Context context = getActivity();

        final boolean voiceCapable = Utils.isVoiceCapable(context);
        final ArrayList<Integer> missingIcons = new ArrayList<>();
        final ArrayList<UserPreference> userPreferences = new ArrayList<>();
        userPreferences.add(mMePreference);

        for (UserInfo user : users) {
            if (!user.supportsSwitchToByUser()) {
                // Only users that can be switched to should show up here.
                // e.g. Managed profiles appear under Accounts Settings instead
                continue;
            }
            UserPreference pref;
            if (user.id == UserHandle.myUserId()) {
                pref = mMePreference;
            } else if (user.isGuest()) {
                // Skip over Guest. We add generic Guest settings after this loop
                continue;
            } else {
                // With Telephony:
                //   Secondary user: Settings
                //   Guest: Settings
                //   Restricted Profile: There is no Restricted Profile
                // Without Telephony:
                //   Secondary user: Delete
                //   Guest: Nothing
                //   Restricted Profile: Settings
                final boolean showSettings = mUserCaps.mIsAdmin
                        && (voiceCapable || user.isRestricted());
                final boolean showDelete = mUserCaps.mIsAdmin
                        && (!voiceCapable && !user.isRestricted() && !user.isGuest());
                pref = new UserPreference(getPrefContext(), null, user.id,
                        showSettings ? this : null,
                        showDelete ? this : null);
                pref.setKey("id=" + user.id);
                userPreferences.add(pref);
                if (user.isAdmin()) {
                    pref.setSummary(R.string.user_admin);
                }
                pref.setTitle(user.name);
                pref.setSelectable(false);
            }
            if (pref == null) {
                continue;
            }
            if (!isInitialized(user)) {
                if (user.isRestricted()) {
                    pref.setSummary(R.string.user_summary_restricted_not_set_up);
                } else {
                    pref.setSummary(R.string.user_summary_not_set_up);
                }
                pref.setOnPreferenceClickListener(this);
                pref.setSelectable(true);
            } else if (user.isRestricted()) {
                pref.setSummary(R.string.user_summary_restricted_profile);
            }
            if (user.iconPath != null) {
                if (mUserIcons.get(user.id) == null) {
                    // Icon not loaded yet, print a placeholder
                    missingIcons.add(user.id);
                    pref.setIcon(getEncircledDefaultIcon());
                } else {
                    setPhotoId(pref, user);
                }
            } else {
                // Icon not available yet, print a placeholder
                pref.setIcon(getEncircledDefaultIcon());
            }
        }

        // Add a temporary entry for the user being created
        if (mAddingUser) {
            UserPreference pref = new UserPreference(getPrefContext(), null,
                    UserPreference.USERID_UNKNOWN, null, null);
            pref.setEnabled(false);
            pref.setTitle(mAddingUserName);
            pref.setIcon(getEncircledDefaultIcon());
            userPreferences.add(pref);
        }

        // Check if Guest tile should be added.
        if (!mUserCaps.mIsGuest && (mUserCaps.mCanAddGuest ||
                mUserCaps.mDisallowAddUserSetByAdmin)) {
            // Add a virtual Guest user for guest defaults
            UserPreference pref = new UserPreference(getPrefContext(), null,
                    UserPreference.USERID_GUEST_DEFAULTS,
                    mUserCaps.mIsAdmin && voiceCapable? this : null /* settings icon handler */,
                    null /* delete icon handler */);
            pref.setTitle(R.string.user_guest);
            pref.setIcon(getEncircledDefaultIcon());
            userPreferences.add(pref);
            pref.setDisabledByAdmin(
                    mUserCaps.mDisallowAddUser ? mUserCaps.mEnforcedAdmin : null);
            pref.setSelectable(false);
        }

        // Sort list of users by serialNum
        Collections.sort(userPreferences, UserPreference.SERIAL_NUMBER_COMPARATOR);

        getActivity().invalidateOptionsMenu();

        // Load the icons
        if (missingIcons.size() > 0) {
            loadIconsAsync(missingIcons);
        }

        PreferenceScreen preferenceScreen = getPreferenceScreen();
        preferenceScreen.removeAll();

        // If profiles are supported, userPreferences will be added to the category labeled
        // "User & Profiles", otherwise the category is skipped and elements are added directly
        // to preferenceScreen
        PreferenceGroup groupToAddUsers;
        if (mUserCaps.mCanAddRestrictedProfile) {
            mUserListCategory.removeAll();
            mUserListCategory.setOrder(Preference.DEFAULT_ORDER);
            preferenceScreen.addPreference(mUserListCategory);
            groupToAddUsers = mUserListCategory;
        } else {
            groupToAddUsers = preferenceScreen;
        }
        for (UserPreference userPreference : userPreferences) {
            userPreference.setOrder(Preference.DEFAULT_ORDER);
            groupToAddUsers.addPreference(userPreference);
        }

        // Append Add user to the end of the list
        if ((mUserCaps.mCanAddUser || mUserCaps.mDisallowAddUserSetByAdmin) &&
                Utils.isDeviceProvisioned(getActivity())) {
            boolean moreUsers = mUserManager.canAddMoreUsers();
            mAddUser.setOrder(Preference.DEFAULT_ORDER);
            preferenceScreen.addPreference(mAddUser);
            mAddUser.setEnabled(moreUsers && !mAddingUser);
            if (!moreUsers) {
                mAddUser.setSummary(getString(R.string.user_add_max_count, getMaxRealUsers()));
            } else {
                mAddUser.setSummary(null);
            }
            if (mAddUser.isEnabled()) {
                mAddUser.setDisabledByAdmin(
                        mUserCaps.mDisallowAddUser ? mUserCaps.mEnforcedAdmin : null);
            }
        }
        if (mUserCaps.mIsAdmin &&
                (!mUserCaps.mDisallowAddUser || mUserCaps.mDisallowAddUserSetByAdmin)) {
            mLockScreenSettings.setOrder(Preference.DEFAULT_ORDER);
            preferenceScreen.addPreference(mLockScreenSettings);
            mAddUserWhenLocked.setChecked(Settings.Global.getInt(getContentResolver(),
                    Settings.Global.ADD_USERS_WHEN_LOCKED, 0) == 1);
            mAddUserWhenLocked.setOnPreferenceChangeListener(this);
            mAddUserWhenLocked.setDisabledByAdmin(
                    mUserCaps.mDisallowAddUser ? mUserCaps.mEnforcedAdmin : null);
        }

        if (emergencyInfoActivityPresent(getContext())) {
            mEmergencyInfoPreference.setOnPreferenceClickListener(this);
            mEmergencyInfoPreference.setOrder(Preference.DEFAULT_ORDER);
            preferenceScreen.addPreference(mEmergencyInfoPreference);
        }
    }

    private int getMaxRealUsers() {
        // guest is not counted against getMaxSupportedUsers() number
        final int maxUsersAndGuest = UserManager.getMaxSupportedUsers() + 1;
        final List<UserInfo> users = mUserManager.getUsers();
        // managed profiles are counted against getMaxSupportedUsers()
        int managedProfiles = 0;
        for (UserInfo user : users) {
            if (user.isManagedProfile()) {
                managedProfiles++;
            }
        }
        return maxUsersAndGuest - managedProfiles;
    }

    private void loadIconsAsync(List<Integer> missingIcons) {
        new AsyncTask<List<Integer>, Void, Void>() {
            @Override
            protected void onPostExecute(Void result) {
                updateUserList();
            }

            @Override
            protected Void doInBackground(List<Integer>... values) {
                for (int userId : values[0]) {
                    Bitmap bitmap = mUserManager.getUserIcon(userId);
                    if (bitmap == null) {
                        bitmap = Utils.getDefaultUserIconAsBitmap(userId);
                    }
                    mUserIcons.append(userId, bitmap);
                }
                return null;
            }
        }.execute(missingIcons);
    }

    private Drawable getEncircledDefaultIcon() {
        if (mDefaultIconDrawable == null) {
            mDefaultIconDrawable = encircle(Utils.getDefaultUserIconAsBitmap(UserHandle.USER_NULL));
        }
        return mDefaultIconDrawable;
    }

    private void setPhotoId(Preference pref, UserInfo user) {
        Bitmap bitmap = mUserIcons.get(user.id);
        if (bitmap != null) {
            pref.setIcon(encircle(bitmap));
        }
    }

    @Override
    public boolean onPreferenceClick(Preference pref) {
        if (pref == mMePreference) {
            if (mUserCaps.mIsGuest) {
                showDialog(DIALOG_CONFIRM_EXIT_GUEST);
                return true;
            }
            // If this is a limited user, launch the user info settings instead of profile editor
            if (mUserManager.isLinkedUser()) {
                onManageUserClicked(UserHandle.myUserId(), false);
            } else {
                showDialog(DIALOG_USER_PROFILE_EDITOR);
            }
        } else if (pref instanceof UserPreference) {
            int userId = ((UserPreference) pref).getUserId();
            // Get the latest status of the user
            UserInfo user = mUserManager.getUserInfo(userId);
            if (!isInitialized(user)) {
                mHandler.sendMessage(mHandler.obtainMessage(
                        MESSAGE_SETUP_USER, user.id, user.serialNumber));
            }
        } else if (pref == mAddUser) {
            // If we allow both types, show a picker, otherwise directly go to
            // flow for full user.
            if (mUserCaps.mCanAddRestrictedProfile) {
                showDialog(DIALOG_CHOOSE_USER_TYPE);
            } else {
                onAddUserClicked(USER_TYPE_USER);
            }
        } else if (pref == mEmergencyInfoPreference) {
            Intent intent = new Intent(ACTION_EDIT_EMERGENCY_INFO);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
        return false;
    }

    private boolean isInitialized(UserInfo user) {
        return (user.flags & UserInfo.FLAG_INITIALIZED) != 0;
    }

    private Drawable encircle(Bitmap icon) {
        Drawable circled = CircleFramedDrawable.getInstance(getActivity(), icon);
        return circled;
    }

    @Override
    public void onClick(View v) {
        if (v.getTag() instanceof UserPreference) {
            int userId = ((UserPreference) v.getTag()).getUserId();
            switch (v.getId()) {
            case UserPreference.DELETE_ID:
                final EnforcedAdmin removeDisallowedAdmin =
                        RestrictedLockUtils.checkIfRestrictionEnforced(getContext(),
                                UserManager.DISALLOW_REMOVE_USER, UserHandle.myUserId());
                if (removeDisallowedAdmin != null) {
                    RestrictedLockUtils.sendShowAdminSupportDetailsIntent(getContext(),
                            removeDisallowedAdmin);
                } else {
                    onRemoveUserClicked(userId);
                }
                break;
            case UserPreference.SETTINGS_ID:
                onManageUserClicked(userId, false);
                break;
            }
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        synchronized (mUserLock) {
            mRemovingUserId = -1;
            updateUserList();
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mAddUserWhenLocked) {
            Boolean value = (Boolean) newValue;
            Settings.Global.putInt(getContentResolver(), Settings.Global.ADD_USERS_WHEN_LOCKED,
                    value != null && value ? 1 : 0);
            return true;
        }

        return false;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_users;
    }

    @Override
    public void onPhotoChanged(Drawable photo) {
        mMePreference.setIcon(photo);
    }

    @Override
    public void onLabelChanged(CharSequence label) {
        mMePreference.setTitle(label);
    }

    private static class UserCapabilities {
        boolean mEnabled = true;
        boolean mCanAddUser = true;
        boolean mCanAddRestrictedProfile = true;
        boolean mIsAdmin;
        boolean mIsGuest;
        boolean mCanAddGuest;
        boolean mDisallowAddUser;
        boolean mDisallowAddUserSetByAdmin;
        EnforcedAdmin mEnforcedAdmin;

        private UserCapabilities() {}

        public static UserCapabilities create(Context context) {
            UserManager userManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
            UserCapabilities caps = new UserCapabilities();
            if (!UserManager.supportsMultipleUsers() || Utils.isMonkeyRunning()) {
                caps.mEnabled = false;
                return caps;
            }

            final UserInfo myUserInfo = userManager.getUserInfo(UserHandle.myUserId());
            caps.mIsGuest = myUserInfo.isGuest();
            caps.mIsAdmin = myUserInfo.isAdmin();
            DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(
                    Context.DEVICE_POLICY_SERVICE);
            // No restricted profiles for devices with a device owner.
            if (dpm.isDeviceManaged()) {
                caps.mCanAddRestrictedProfile = false;
            }
            caps.updateAddUserCapabilities(context);
            return caps;
        }

        public void updateAddUserCapabilities(Context context) {
            mEnforcedAdmin = RestrictedLockUtils.checkIfRestrictionEnforced(context,
                    UserManager.DISALLOW_ADD_USER, UserHandle.myUserId());
            final boolean hasBaseUserRestriction = RestrictedLockUtils.hasBaseUserRestriction(
                    context, UserManager.DISALLOW_ADD_USER, UserHandle.myUserId());
            mDisallowAddUserSetByAdmin =
                    mEnforcedAdmin != null && !hasBaseUserRestriction;
            mDisallowAddUser =
                    (mEnforcedAdmin != null || hasBaseUserRestriction);
            mCanAddUser = true;
            if (!mIsAdmin || UserManager.getMaxSupportedUsers() < 2
                    || !UserManager.supportsMultipleUsers()
                    || mDisallowAddUser) {
                mCanAddUser = false;
            }

            final boolean canAddUsersWhenLocked = mIsAdmin || Settings.Global.getInt(
                    context.getContentResolver(), Settings.Global.ADD_USERS_WHEN_LOCKED, 0) == 1;
            mCanAddGuest = !mIsGuest && !mDisallowAddUser && canAddUsersWhenLocked;
        }

        @Override
        public String toString() {
            return "UserCapabilities{" +
                    "mEnabled=" + mEnabled +
                    ", mCanAddUser=" + mCanAddUser +
                    ", mCanAddRestrictedProfile=" + mCanAddRestrictedProfile +
                    ", mIsAdmin=" + mIsAdmin +
                    ", mIsGuest=" + mIsGuest +
                    ", mCanAddGuest=" + mCanAddGuest +
                    ", mDisallowAddUser=" + mDisallowAddUser +
                    ", mEnforcedAdmin=" + mEnforcedAdmin +
                    '}';
        }
    }

    private static class SummaryProvider implements SummaryLoader.SummaryProvider {

        private final Context mContext;
        private final SummaryLoader mSummaryLoader;

        public SummaryProvider(Context context, SummaryLoader summaryLoader) {
            mContext = context;
            mSummaryLoader = summaryLoader;
        }

        @Override
        public void setListening(boolean listening) {
            if (listening) {
                UserInfo info = mContext.getSystemService(UserManager.class).getUserInfo(
                        UserHandle.myUserId());
                mSummaryLoader.setSummary(this, mContext.getString(R.string.user_summary,
                        info.name));
            }
        }
    }

    public static final SummaryLoader.SummaryProviderFactory SUMMARY_PROVIDER_FACTORY
            = new SummaryLoader.SummaryProviderFactory() {
        @Override
        public SummaryLoader.SummaryProvider createSummaryProvider(Activity activity,
                                                                   SummaryLoader summaryLoader) {
            return new SummaryProvider(activity, summaryLoader);
        }
    };

    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableRaw> getRawDataToIndex(Context context,
                        boolean enabled) {
                    final List<SearchIndexableRaw> result = new ArrayList<>();
                    final UserCapabilities userCaps = UserCapabilities.create(context);
                    if (!userCaps.mEnabled) {
                        return result;
                    }
                    final Resources res = context.getResources();
                    SearchIndexableRaw data = new SearchIndexableRaw(context);
                    data.title = res.getString(R.string.user_settings_title);
                    data.screenTitle = res.getString(R.string.user_settings_title);
                    result.add(data);

                    if (userCaps.mCanAddUser || userCaps.mDisallowAddUserSetByAdmin) {
                        data = new SearchIndexableRaw(context);
                        data.title = res.getString(userCaps.mCanAddRestrictedProfile ?
                                R.string.user_add_user_or_profile_menu
                                : R.string.user_add_user_menu);
                        data.screenTitle = res.getString(R.string.user_settings_title);
                        result.add(data);
                    }
                    if (emergencyInfoActivityPresent(context)) {
                        data = new SearchIndexableRaw(context);
                        data.title = res.getString(R.string.emergency_info_title);
                        data.screenTitle = res.getString(R.string.emergency_info_title);
                        result.add(data);
                    }
                    return result;
                }
            };

}
