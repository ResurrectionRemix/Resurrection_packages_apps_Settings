/**
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.settings.applications;

import android.Manifest;
import android.app.Activity;
import android.app.AppOpsManager;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.NetworkPolicyManager;
import android.os.Bundle;
import android.os.IDeviceIdleController;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.ArraySet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.InstrumentedFragment;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.net.NetworkPolicyManager.POLICY_REJECT_ON_DATA;
import static android.net.NetworkPolicyManager.POLICY_REJECT_ON_WLAN;

public class AppOpsDetails extends InstrumentedFragment {
    static final String TAG = "AppOpsDetails";

    public static final String ARG_PACKAGE_NAME = "package";

    private AppOpsState mState;
    private PackageManager mPm;
    private AppOpsManager mAppOps;
    private PackageInfo mPackageInfo;
    private LayoutInflater mInflater;
    private View mRootView;
    private LinearLayout mOperationsSection;
    private NetworkPolicyManager mPolicyManager;

    private final int MODE_ALLOWED = 0;
    private final int MODE_IGNORED = 1;
    private final int MODE_ASK     = 2;

    private int modeToPosition (int mode) {
        switch(mode) {
        case AppOpsManager.MODE_ALLOWED:
            return MODE_ALLOWED;
        case AppOpsManager.MODE_IGNORED:
            return MODE_IGNORED;
        case AppOpsManager.MODE_ASK:
            return MODE_ASK;
        };

        return MODE_IGNORED;
    }

    private int positionToMode (int position) {
        switch(position) {
        case MODE_ALLOWED:
            return AppOpsManager.MODE_ALLOWED;
        case MODE_IGNORED:
            return AppOpsManager.MODE_IGNORED;
        case MODE_ASK:
            return AppOpsManager.MODE_ASK;
        };

        return AppOpsManager.MODE_IGNORED;
    }

    private boolean isPlatformSigned() {
        final int match = mPm.checkSignatures("android", mPackageInfo.packageName);
        return match >= PackageManager.SIGNATURE_MATCH;
    }

    // Utility method to set application label and icon.
    private void setAppLabelAndIcon(PackageInfo pkgInfo) {
        final View appSnippet = mRootView.findViewById(R.id.app_snippet);
        CharSequence label = mPm.getApplicationLabel(pkgInfo.applicationInfo);
        Drawable icon = mPm.getApplicationIcon(pkgInfo.applicationInfo);
        InstalledAppDetails.setupAppSnippet(appSnippet, label, icon,
                pkgInfo != null ? pkgInfo.versionName : null, null);
    }

    private String retrieveAppEntry() {
        final Bundle args = getArguments();
        String packageName = (args != null) ? args.getString(ARG_PACKAGE_NAME) : null;
        if (packageName == null) {
            Intent intent = (args == null) ?
                    getActivity().getIntent() : (Intent) args.getParcelable("intent");
            if (intent != null) {
                packageName = intent.getData().getSchemeSpecificPart();
            }
        }
        try {
            mPackageInfo = mPm.getPackageInfo(packageName,
                    PackageManager.GET_DISABLED_COMPONENTS |
                    PackageManager.GET_UNINSTALLED_PACKAGES);
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

        setAppLabelAndIcon(mPackageInfo);

        Resources res = getActivity().getResources();

        final IDeviceIdleController iDeviceIdleController = IDeviceIdleController.Stub.asInterface(
                ServiceManager.getService(Context.DEVICE_IDLE_CONTROLLER));
        List<String> allowInPowerSave;
        if (iDeviceIdleController != null) {
            try {
                allowInPowerSave = Arrays.asList(iDeviceIdleController.getSystemPowerWhitelist());
            } catch (RemoteException e) {
                Log.e(TAG, "couldn't get system power white list", e);
                allowInPowerSave = new ArrayList<>();
            }
        } else {
            allowInPowerSave = new ArrayList<>();
        }

        mOperationsSection.removeAllViews();
        String lastPermGroup = "";
        boolean isPlatformSigned = isPlatformSigned();
        for (AppOpsState.OpsTemplate tpl : AppOpsState.ALL_TEMPLATES) {
            /* If we are platform signed, only show the root switch, this
             * one is safe to toggle while other permission-based ones could
             * certainly cause system-wide problems
             */
            if (isPlatformSigned && tpl != AppOpsState.SU_TEMPLATE) {
                 continue;
            }
            List<AppOpsState.AppOpEntry> entries = mState.buildState(tpl,
                    mPackageInfo.applicationInfo.uid, mPackageInfo.packageName);
            for (final AppOpsState.AppOpEntry entry : entries) {
                final AppOpsManager.OpEntry firstOp = entry.getOpEntry(0);
                final View view = mInflater.inflate(R.layout.app_ops_details_item,
                        mOperationsSection, false);
                mOperationsSection.addView(view);
                String perm = AppOpsManager.opToPermission(firstOp.getOp());
                if (perm != null) {
                    try {
                        PermissionInfo pi = mPm.getPermissionInfo(perm, 0);
                        if (pi.group != null && !lastPermGroup.equals(pi.group)) {
                            lastPermGroup = pi.group;
                            PermissionGroupInfo pgi = mPm.getPermissionGroupInfo(pi.group, 0);
                            if (pgi.icon != 0) {
                                ((ImageView)view.findViewById(R.id.op_icon)).setImageDrawable(
                                        pgi.loadIcon(mPm));
                            }
                        }
                    } catch (NameNotFoundException e) {
                    }
                }

                Spinner sp = (Spinner) view.findViewById(R.id.spinnerWidget);
                sp.setVisibility(View.GONE);
                Switch sw = (Switch) view.findViewById(R.id.switchWidget);
                sw.setVisibility(View.GONE);

                final int switchOp = AppOpsManager.opToSwitch(firstOp.getOp());
                int mode = mAppOps.checkOp(switchOp, entry.getPackageOps().getUid(),
                        entry.getPackageOps().getPackageName());

                final TextView opNameText = (TextView) view.findViewById(R.id.op_name);
                final TextView opCountText = (TextView) view.findViewById(R.id.op_counts);
                final TextView opTimeText = (TextView) view.findViewById(R.id.op_time);

                opNameText.setText(entry.getSwitchText(mState));

                if (switchOp == AppOpsManager.OP_WAKE_LOCK
                        && allowInPowerSave.contains(entry.getPackageOps().getPackageName())) {
                    // sooper special case; app is marked to be allowed in power save; it is
                    // probably critical to functionality, don't allow user to change it, because
                    // we'll ignore it either way
                    sw.setVisibility(View.VISIBLE);
                    sw.setChecked(true);
                    sw.setEnabled(false);

                    opCountText.setVisibility(View.GONE);
                    opTimeText.setText(R.string.app_ops_disabled_by_optimization);

                    continue;
                }

                opCountText.setText(entry.getCountsText(res));
                opTimeText.setText(entry.getTimeText(res, true));

                sp.setSelection(modeToPosition(mode));
                sp.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
                    boolean firstMode = true;

                    @Override
                    public void onItemSelected(AdapterView<?> parentView, View selectedItemView,
                            int position, long id) {
                        if (firstMode) {
                            firstMode = false;
                            return;
                        }
                        mAppOps.setMode(switchOp, entry.getPackageOps().getUid(),
                                entry.getPackageOps().getPackageName(), positionToMode(position));
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parentView) {
                        // Do nothing
                    }
                });

                sw.setChecked(mAppOps.checkOp(switchOp, entry.getPackageOps()
                        .getUid(), entry.getPackageOps().getPackageName()) == AppOpsManager.MODE_ALLOWED);
                sw.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {
                    public void onCheckedChanged(CompoundButton buttonView,
                            boolean isChecked) {
                        mAppOps.setMode(switchOp, entry.getPackageOps()
                                .getUid(), entry.getPackageOps()
                                .getPackageName(),
                                isChecked ? AppOpsManager.MODE_ALLOWED
                                        : AppOpsManager.MODE_IGNORED);
                    }
                });
                if (AppOpsManager.isStrictOp(switchOp)) {
                    sp.setVisibility(View.VISIBLE);
                } else {
                    sw.setVisibility(View.VISIBLE);
                }
            }
        }

        if (mPm.checkPermission(Manifest.permission.INTERNET, mPackageInfo.packageName)
                == PackageManager.PERMISSION_GRANTED) {
            TextView internetCategory = (TextView) mInflater.inflate(
                    R.layout.preference_category_material, null);
            internetCategory.setText(R.string.privacy_guard_internet_category);
            mOperationsSection.addView(internetCategory);

            addInternetSwitch(POLICY_REJECT_ON_WLAN);
            addInternetSwitch(POLICY_REJECT_ON_DATA);
        }

        return true;
    }

    private void addInternetSwitch(final int policy) {
        // Add internet category permissions
        final View view = mInflater.inflate(R.layout.app_ops_details_item,
                mOperationsSection, false);
        mOperationsSection.addView(view);

        ((TextView)view.findViewById(R.id.op_name)).setText(
                policy == POLICY_REJECT_ON_DATA ? R.string.restrict_app_cellular_title :
                        R.string.restrict_app_wlan_title);
        view.findViewById(R.id.op_counts).setVisibility(View.INVISIBLE);
        view.findViewById(R.id.op_time).setVisibility(View.INVISIBLE);
        view.findViewById(R.id.spinnerWidget).setVisibility(View.GONE);

        Switch sw = (Switch) view.findViewById(R.id.switchWidget);
        sw.setChecked((mPolicyManager.getUidPolicy(
                mPackageInfo.applicationInfo.uid) & policy) != 0);
        sw.setTag(policy);
        sw.setVisibility(View.VISIBLE);
        sw.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {
                if (isChecked) {
                    mPolicyManager.addUidPolicy(mPackageInfo.applicationInfo.uid,
                            policy);
                } else {
                    mPolicyManager.removeUidPolicy(mPackageInfo.applicationInfo.uid,
                            policy);
                }
            }
        });
    }

    private void setIntentAndFinish(boolean finish, boolean appChanged) {
        Intent intent = new Intent();
        intent.putExtra(ManageApplications.APP_CHG, appChanged);
        SettingsActivity sa = (SettingsActivity)getActivity();
        sa.finishPreferencePanel(this, Activity.RESULT_OK, intent);
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mState = new AppOpsState(getActivity());
        mPm = getActivity().getPackageManager();
        mInflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mAppOps = (AppOpsManager)getActivity().getSystemService(Context.APP_OPS_SERVICE);
        mPolicyManager = NetworkPolicyManager.from(getActivity());

        retrieveAppEntry();

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.app_ops_details, container, false);
        Utils.prepareCustomPreferencesList(container, view, view, false);

        mRootView = view;
        mOperationsSection = (LinearLayout)view.findViewById(R.id.operations_section);
        return view;
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.APP_OPS_DETAILS;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!refreshUi()) {
            setIntentAndFinish(true, true);
        }
    }
}
