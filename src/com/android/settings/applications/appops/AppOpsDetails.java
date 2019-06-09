/**
 * Copyright (C) 2013 The Android Open Source Project
 * Copyright (C) 2017-2018 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.android.settings.applications.appops;

import android.app.Activity;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.UserHandle;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.IconDrawableFactory;
import android.util.Log;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.applications.manageapplications.ManageApplications;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.widget.EntityHeaderController;
import com.android.settingslib.Utils;

import java.util.HashMap;
import java.util.List;
import java.util.StringJoiner;

public class AppOpsDetails extends SettingsPreferenceFragment {
    static final String TAG = "AppOpsDetails";

    public static final String ARG_PACKAGE_NAME = "package";
    public static final String ARG_PACKAGE_UID = "package_uid";
    private static final String KEY_HEADER = "header";

    private AppOpsState mState;
    private PackageManager mPm;
    private AppOpsManager mAppOps;
    private PackageInfo mPackageInfo;
    private PreferenceScreen mPreferenceScreen;

    private final int MODE_ALLOWED = 0;
    private final int MODE_IGNORED = 1;
    private final int MODE_ASK     = 2;

    private final String[] MODE_ENTRIES = {
            String.valueOf(MODE_ALLOWED),
            String.valueOf(MODE_IGNORED),
            String.valueOf(MODE_ASK)
    };

    private int modeToPosition(int mode) {
        switch (mode) {
            case AppOpsManager.MODE_ALLOWED:
                return MODE_ALLOWED;
            case AppOpsManager.MODE_IGNORED:
                return MODE_IGNORED;
            case AppOpsManager.MODE_ASK:
                return MODE_ASK;
            default:
                return MODE_IGNORED;
        }
    }

    private int positionToMode(int position) {
        switch (position) {
            case MODE_ALLOWED:
                return AppOpsManager.MODE_ALLOWED;
            case MODE_IGNORED:
                return AppOpsManager.MODE_IGNORED;
            case MODE_ASK:
                return AppOpsManager.MODE_ASK;
            default:
                return AppOpsManager.MODE_IGNORED;
        }
    }

    private static HashMap<Integer, Integer> OP_ICONS = new HashMap<>();

    static {
        OP_ICONS.put(AppOpsManager.OP_ACTIVATE_VPN, R.drawable.ic_perm_vpn);
        OP_ICONS.put(AppOpsManager.OP_AUDIO_ALARM_VOLUME, R.drawable.ic_perm_alarm);
        OP_ICONS.put(AppOpsManager.OP_AUDIO_MEDIA_VOLUME, R.drawable.ic_perm_audio);
        OP_ICONS.put(AppOpsManager.OP_BLUETOOTH_CHANGE, R.drawable.ic_perm_bluetooth);
        OP_ICONS.put(AppOpsManager.OP_BOOT_COMPLETED, R.drawable.ic_perm_boot);
        OP_ICONS.put(AppOpsManager.OP_CHANGE_WIFI_STATE, R.drawable.ic_perm_wifi);
        OP_ICONS.put(AppOpsManager.OP_DATA_CONNECT_CHANGE, R.drawable.ic_perm_data);
        OP_ICONS.put(AppOpsManager.OP_GET_USAGE_STATS, R.drawable.ic_perm_data);
        OP_ICONS.put(AppOpsManager.OP_GPS, R.drawable.ic_perm_location);
        OP_ICONS.put(AppOpsManager.OP_MUTE_MICROPHONE, R.drawable.ic_perm_microphone);
        OP_ICONS.put(AppOpsManager.OP_NFC_CHANGE, R.drawable.ic_perm_nfc);
        OP_ICONS.put(AppOpsManager.OP_POST_NOTIFICATION, R.drawable.ic_perm_notifications);
        OP_ICONS.put(AppOpsManager.OP_READ_CLIPBOARD, R.drawable.ic_perm_clipboard);
        OP_ICONS.put(AppOpsManager.OP_RUN_IN_BACKGROUND, R.drawable.ic_perm_background);
        OP_ICONS.put(AppOpsManager.OP_SU, R.drawable.ic_perm_su);
        OP_ICONS.put(AppOpsManager.OP_SYSTEM_ALERT_WINDOW, R.drawable.ic_perm_drawontop);
        OP_ICONS.put(AppOpsManager.OP_TAKE_AUDIO_FOCUS, R.drawable.ic_perm_audio);
        OP_ICONS.put(AppOpsManager.OP_TOAST_WINDOW, R.drawable.ic_perm_notifications);
        OP_ICONS.put(AppOpsManager.OP_TURN_SCREEN_ON, R.drawable.ic_perm_turnscreenon);
        OP_ICONS.put(AppOpsManager.OP_VIBRATE, R.drawable.ic_perm_vibrate);
        OP_ICONS.put(AppOpsManager.OP_WAKE_LOCK, R.drawable.ic_perm_nosleep);
        OP_ICONS.put(AppOpsManager.OP_WIFI_SCAN, R.drawable.ic_perm_wifi);
        OP_ICONS.put(AppOpsManager.OP_WRITE_CLIPBOARD, R.drawable.ic_perm_clipboard);
        OP_ICONS.put(AppOpsManager.OP_WRITE_SETTINGS, R.drawable.ic_perm_settings);
        OP_ICONS.put(AppOpsManager.OP_WRITE_SMS , R.drawable.ic_perm_sms);
    }

    private boolean isPlatformSigned() {
        final int match = mPm.checkSignatures("android", mPackageInfo.packageName);
        return match >= PackageManager.SIGNATURE_MATCH;
    }

    // Utility method to set application label and icon.
    private void setAppHeader(PackageInfo pkgInfo) {
        ApplicationInfo appInfo = pkgInfo.applicationInfo;
        String appLabel = mPm.getApplicationLabel(appInfo).toString();
        String label;
        try {
            label = appInfo.loadLabel(mPm).toString();
        } catch (Throwable t) {
            Log.e(TAG, "Error loading application label for " + appLabel, t);
            label = appLabel;
        }

        final Activity activity = getActivity();
        final Preference pref = EntityHeaderController
                .newInstance(getActivity(), this /* fragment */, null /* header */)
                .setIcon(IconDrawableFactory.newInstance(getContext())
                        .getBadgedIcon(appInfo))
                .setLabel(label)
                .setPackageName(appInfo.packageName)
                .setUid(appInfo.uid)
                .setHasAppInfoLink(true)
                .setButtonActions(EntityHeaderController.ActionType.ACTION_NONE,
                        EntityHeaderController.ActionType.ACTION_NONE)
                .done(getActivity(), getPrefContext());
        pref.setKey(KEY_HEADER);
        getPreferenceScreen().addPreference(pref);
    }

    private String retrieveAppEntry() {
        final Bundle args = getArguments();
        String packageName = (args != null) ? args.getString(ARG_PACKAGE_NAME) : null;
        int packageUserId = (args != null) ? UserHandle.getUserId(args.getInt(ARG_PACKAGE_UID)) : 0;

        if (packageName == null) {
            Intent intent = (args == null) ?
                    getActivity().getIntent() : (Intent) args.getParcelable("intent");
            if (intent != null) {
                packageName = intent.getData().getSchemeSpecificPart();
            }
        }
        try {
            mPackageInfo = mPm.getPackageInfoAsUser(packageName,
                    PackageManager.MATCH_DISABLED_COMPONENTS |
                    PackageManager.MATCH_ANY_USER,
                    packageUserId);
        } catch (NameNotFoundException e) {
            Log.e(TAG, "Exception when retrieving package:" + packageName, e);
            mPackageInfo = null;
        }

        return packageName;
    }

    private boolean refreshUi() {
        if (mPackageInfo == null) {
            return false;
        }

        mPreferenceScreen.removeAll();
        setAppHeader(mPackageInfo);

        AppOpsState.OpsTemplate[] allTemplates = getTemplates();
        for (AppOpsState.OpsTemplate tpl : allTemplates) {
            List<AppOpsState.AppOpEntry> entries = mState.buildState(tpl,
                    mPackageInfo.applicationInfo.uid, mPackageInfo.packageName, true);
            for (final AppOpsState.AppOpEntry entry : entries) {
                String perm = null;
                int op = -1;
                // Find the first permission with a known name
                for (int i = 0; i < entry.getNumOpEntry() && perm == null; i++) {
                    op = entry.getOpEntry(i).getOp();
                    perm = AppOpsManager.opToPermission(op);
                }
                Drawable icon = getIconByPermission(perm);
                if (icon == null && op != -1 && OP_ICONS.containsKey(op)) {
                    icon = getActivity().getDrawable(OP_ICONS.get(op));
                }
                if (icon == null) {
                    Log.e(TAG, "Failed to retrieve icon for permission: " + perm);
                } else {
                    icon.setTint(Utils.getColorAttr(getActivity(),
                            android.R.attr.colorControlNormal));
                }

                final AppOpsManager.OpEntry firstOp = entry.getOpEntry(0);
                final int switchOp = AppOpsManager.opToSwitch(firstOp.getOp());

                // ListPreference for 3 states: ask, allow, deny
                if (AppOpsManager.isStrictOp(switchOp)) {
                    ListPreference listPref = getListPrefForEntry(entry, icon);
                    mPreferenceScreen.addPreference(listPref);
                } else {
                    SwitchPreference switchPref = getSwitchPrefForEntry(entry, icon);
                    mPreferenceScreen.addPreference(switchPref);
                }
            }
        }

        if (mPreferenceScreen.getPreferenceCount() == 0) {
            Preference noBlockablePermissionsPref = getNoBlockablePermissionsPref();
            mPreferenceScreen.addPreference(noBlockablePermissionsPref);
        }

        return true;
    }

    private AppOpsState.OpsTemplate[] getTemplates() {
        /* If we are platform signed, only show the root switch, this
         * one is safe to toggle while other permission-based ones could
         * certainly cause system-wide problems
         */
        if (isPlatformSigned()) {
            return new AppOpsState.OpsTemplate[]{ AppOpsState.SU_TEMPLATE };
        }

        int length = AppOpsState.ALL_PERMS_TEMPLATES.length;
        AppOpsState.OpsTemplate[] allTemplates = new AppOpsState.OpsTemplate[length];
        // Loop all existing templates and set the visibility of each perm to true
        for (int i = 0; i < length; i++) {
            AppOpsState.OpsTemplate tpl = AppOpsState.ALL_PERMS_TEMPLATES[i];
            for (int j = 0; j < tpl.ops.length; j++) {
                // we only want to use the template's orderings, not the visibility
                tpl.showPerms[j] = true;
            }

            allTemplates[i] = tpl;
        }

        return allTemplates;
    }

    private Drawable getIconByPermission(String perm) {
        Drawable icon = null;
        if (perm != null) {
            try {
                PermissionInfo pi = mPm.getPermissionInfo(perm, 0);
                if (pi.group != null) {
                    PermissionGroupInfo pgi = mPm.getPermissionGroupInfo(pi.group, 0);
                    if (pgi.icon != 0) {
                        icon = pgi.loadIcon(mPm);
                    }
                }
            } catch (NameNotFoundException e) {
            }
        }
        return icon;
    }

    private ListPreference getListPrefForEntry(final AppOpsState.AppOpEntry entry, Drawable icon) {
        final Resources res = getActivity().getResources();

        final AppOpsManager.OpEntry firstOp = entry.getOpEntry(0);
        final AppOpsManager.PackageOps pkgOps = entry.getPackageOps();
        final int uid = pkgOps.getUid();
        final String pkgName = pkgOps.getPackageName();
        final int switchOp = AppOpsManager.opToSwitch(firstOp.getOp());
        final int mode = mAppOps.checkOpNoThrow(switchOp, uid, pkgName);
        final CharSequence opName = entry.getSwitchText(mState);

        ListPreference listPref = new ListPreference(getActivity());
        listPref.setLayoutResource(R.layout.preference_appops);
        listPref.setIcon(icon);
        listPref.setTitle(opName);
        listPref.setDialogTitle(opName);
        listPref.setEntries(R.array.app_ops_permissions);
        listPref.setEntryValues(MODE_ENTRIES);
        listPref.setValueIndex(modeToPosition(mode));
        String summary = getSummary(listPref.getEntry(), entry.getCountsText(res),
                entry.getTimeText(res, true));
        listPref.setSummary(summary);
        listPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                ListPreference listPref = (ListPreference) preference;
                String value = newValue.toString();
                int selectedIndex = listPref.findIndexOfValue(value);
                mAppOps.setMode(switchOp, uid, pkgName, positionToMode(selectedIndex));
                String summary = getSummary(listPref.getEntries()[selectedIndex],
                        entry.getCountsText(res), entry.getTimeText(res, true));
                listPref.setSummary(summary);
                return true;
            }
        });

        return listPref;
    }

    private SwitchPreference getSwitchPrefForEntry(final AppOpsState.AppOpEntry entry,
                                                   Drawable icon) {
        final Resources res = getActivity().getResources();

        final AppOpsManager.OpEntry firstOp = entry.getOpEntry(0);
        final AppOpsManager.PackageOps pkgOps = entry.getPackageOps();
        final int uid = pkgOps.getUid();
        final String pkgName = pkgOps.getPackageName();
        final int switchOp = AppOpsManager.opToSwitch(firstOp.getOp());
        final int mode = mAppOps.checkOpNoThrow(switchOp, uid, pkgName);
        final CharSequence opName = entry.getSwitchText(mState);

        SwitchPreference switchPref = new SwitchPreference(getActivity());
        switchPref.setLayoutResource(R.layout.preference_appops);
        switchPref.setIcon(icon);
        switchPref.setTitle(opName);
        String summary = getSummary(entry.getCountsText(res), entry.getTimeText(res, true));
        switchPref.setSummary(summary);
        switchPref.setChecked(mode == AppOpsManager.MODE_ALLOWED);
        switchPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference,
                                              Object newValue) {
                Boolean isChecked = (Boolean) newValue;
                mAppOps.setMode(switchOp, uid, pkgName,
                        isChecked ? AppOpsManager.MODE_ALLOWED
                                : AppOpsManager.MODE_IGNORED);
                return true;
            }
        });

        return switchPref;
    }

    private Preference getNoBlockablePermissionsPref() {
        Preference emptyPref = new Preference(getActivity());
        emptyPref.setTitle(R.string.app_ops_no_blockable_permissions);
        emptyPref.setSelectable(false);
        emptyPref.setEnabled(false);
        return emptyPref;
    }


    private void setIntentAndFinish(boolean finish, boolean appChanged) {
        Intent intent = new Intent();
        intent.putExtra(ManageApplications.APP_CHG, appChanged);
        SettingsActivity sa = (SettingsActivity)getActivity();
        sa.finishPreferencePanel(Activity.RESULT_OK, intent);
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mState = new AppOpsState(getActivity());
        mPm = getActivity().getPackageManager();
        mAppOps = (AppOpsManager)getActivity().getSystemService(Context.APP_OPS_SERVICE);
        mPreferenceScreen = getPreferenceManager().createPreferenceScreen(getActivity());
        retrieveAppEntry();

        setPreferenceScreen(mPreferenceScreen);
        setHasOptionsMenu(true);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.APP_OPS_DETAILS;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!refreshUi()) {
            setIntentAndFinish(true, true);
        }
    }

    private String getSummary(CharSequence... lines) {
        StringJoiner sj = new StringJoiner("\n");
        for (CharSequence line : lines) {
            if (!TextUtils.isEmpty(line)) {
                sj.add(line);
            }
        }
        return sj.toString();
    }
}
