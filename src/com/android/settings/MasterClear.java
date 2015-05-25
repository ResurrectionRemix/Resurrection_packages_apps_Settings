/*
 * Copyright (C) 2008 The Android Open Source Project
 * Copyright (C) 2015 The CyanogenMod Project
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
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Environment;
import android.os.Process;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import com.android.internal.logging.MetricsLogger;

import java.util.List;

/**
 * Confirm and execute a reset of the device to a clean "just out of the box"
 * state.  Multiple confirmations are required: first, a general "are you sure
 * you want to do this?" prompt, followed by a keyguard pattern trace if the user
 * has defined one, followed by a final strongly-worded "THIS WILL ERASE EVERYTHING
 * ON THE PHONE" prompt.  If at any time the phone is allowed to go to sleep, is
 * locked, et cetera, then the confirmation sequence is abandoned.
 *
 * This is the initial screen.
 */
public class MasterClear extends InstrumentedFragment {
    private static final String TAG = "MasterClear";

    private static final int KEYGUARD_REQUEST = 55;

    //must match MasterClearReceiver.java extra
    public static final String EXTRA_WIPE_MEDIA = "wipe_media";

    private View mContentView;
    private Button mInitiateButton;
    private View mExternalStorageContainer;
    private View mInternalStorageContainer;
    private CheckBox mInternalStorage;
    private CheckBox mExternalStorage;

    /**
     * Keyguard validation is run using the standard {@link ConfirmLockPattern}
     * component as a subactivity
     * @param request the request code to be returned once confirmation finishes
     * @return true if confirmation launched
     */
    private boolean runKeyguardConfirmation(int request) {
        Resources res = getActivity().getResources();
        return new ChooseLockSettingsHelper(getActivity(), this).launchConfirmationActivity(
                request, res.getText(R.string.master_clear_title));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != KEYGUARD_REQUEST) {
            return;
        }

        // If the user entered a valid keyguard trace, present the final
        // confirmation prompt; otherwise, go back to the initial state.
        if (resultCode == Activity.RESULT_OK) {
            showFinalConfirmation();
        } else {
            establishInitialState();
        }
    }

    private void showFinalConfirmation() {
        MasterClearConfirm.createInstance(mInternalStorage.isChecked(),
                mExternalStorage.isChecked()) .show(getFragmentManager(),
                MasterClearConfirm.class.getSimpleName());
    }

    /**
     * If the user clicks to begin the reset sequence, we next require a
     * keyguard confirmation if the user has currently enabled one.  If there
     * is no keyguard available, we simply go to the final confirmation prompt.
     */
    private final Button.OnClickListener mInitiateListener = new Button.OnClickListener() {

        public void onClick(View v) {
            if (!runKeyguardConfirmation(KEYGUARD_REQUEST)) {
                showFinalConfirmation();
            }
        }
    };

    /**
     * In its initial state, the activity presents a button for the user to
     * click in order to initiate a confirmation sequence.  This method is
     * called from various other points in the code to reset the activity to
     * this base state.
     *
     * <p>Reinflating views from resources is expensive and prevents us from
     * caching widget pointers, so we use a single-inflate pattern:  we lazy-
     * inflate each view, caching all of the widget pointers we'll need at the
     * time, then simply reuse the inflated views directly whenever we need
     * to change contents.
     */
    private void establishInitialState() {
        mInitiateButton = (Button) mContentView.findViewById(R.id.initiate_master_clear);
        mInitiateButton.setOnClickListener(mInitiateListener);
        mInternalStorage = (CheckBox) mContentView.findViewById(R.id.erase_internal);
        mInternalStorageContainer = mContentView.findViewById(R.id.erase_internal_container);
        mExternalStorageContainer = mContentView.findViewById(R.id.erase_external_container);
        mExternalStorage = (CheckBox) mContentView.findViewById(R.id.erase_external);

        boolean hasExternalStorage = false;

        /**
         * Here we do some logic to ensure the proper states are initialized.
         * - hide internal memory section if device doesn't support it
         * - force internal memory to be erased if the device is encrypted
         * - show and hide the sd card section if the device supports this (and its inserted)
         * TODO: mutli SD card support: no devices we support have this, but that might change
         */

        if (Environment.isExternalStorageEmulated()) {
            // we may have to force wipe internal storage due to encryption.
            mInternalStorageContainer.setEnabled(!isExtStorageEncrypted()
                    && !Environment.isExternalStorageRemovable());
            mInternalStorageContainer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mInternalStorage.toggle();
                }
            });
            if (!mInternalStorageContainer.isEnabled()) {
                // force internal wipe
                mInternalStorage.setChecked(true);
                TextView internalSummaryText = (TextView) mContentView.findViewById(
                        R.id.erase_storage_checkbox_description);
                internalSummaryText.setText(
                        R.string.factory_reset_erase_stored_content_summary_forced);
            }
        } else {
            // there's no storage emulation; hide internal storage
            mInternalStorageContainer.setVisibility(View.GONE);

            // primary storage can be removed. but does it exist?
        }

        hasExternalStorage = Environment.isExternalStorageRemovable()
                && Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
        mExternalStorageContainer.setVisibility(hasExternalStorage ? View.VISIBLE : View.GONE);
        mExternalStorageContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mExternalStorage.toggle();
            }
        });

        StringBuffer contentDescription = new StringBuffer();
        getContentDescription(mInternalStorageContainer, contentDescription);
        mInternalStorageContainer.setContentDescription(contentDescription);
    }

    private void getContentDescription(View v, StringBuffer description) {
        if (v instanceof ViewGroup) {
            ViewGroup vGroup = (ViewGroup) v;
            for (int i = 0; i < vGroup.getChildCount(); i++) {
                View nextChild = vGroup.getChildAt(i);
                getContentDescription(nextChild, description);
            }
        } else if (v instanceof TextView) {
            TextView vText = (TextView) v;
            description.append(vText.getText());
            description.append(","); // Allow Talkback to pause between sections.
        }
    }

    private boolean isExtStorageEncrypted() {
        String state = SystemProperties.get("vold.decrypt");
        return !"".equals(state);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        if (!Process.myUserHandle().isOwner()
                || UserManager.get(getActivity()).hasUserRestriction(
                UserManager.DISALLOW_FACTORY_RESET)) {
            return inflater.inflate(R.layout.master_clear_disallowed_screen, null);
        }

        mContentView = inflater.inflate(R.layout.master_clear_cm, null);

        establishInitialState();
        return mContentView;
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.MASTER_CLEAR;
    }
}
