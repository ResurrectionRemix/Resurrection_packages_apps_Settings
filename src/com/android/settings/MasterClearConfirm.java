/*
 * Copyright (C) 2010 The Android Open Source Project
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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.service.persistentdata.PersistentDataBlockManager;
import com.android.internal.os.storage.ExternalStorageFormatter;

import android.content.Intent;
import android.os.Bundle;
import android.os.UserManager;

/**
 * Confirm and execute a reset of the device to a clean "just out of the box"
 * state.  Multiple confirmations are required: first, a general "are you sure
 * you want to do this?" prompt, followed by a keyguard pattern trace if the user
 * has defined one, followed by a final strongly-worded "THIS WILL ERASE EVERYTHING
 * ON THE PHONE" prompt.  If at any time the phone is allowed to go to sleep, is
 * locked, et cetera, then the confirmation sequence is abandoned.
 *
 * This is the confirmation screen.
 */
public class MasterClearConfirm extends DialogFragment {

    private boolean mEraseSdCard;
    private boolean mEraseInternal;

    public static MasterClearConfirm createInstance(boolean wipeInternal, boolean wipeExternal) {
        Bundle b = new Bundle();
        b.putBoolean(MasterClear.EXTRA_WIPE_MEDIA, wipeInternal);
        b.putBoolean(MasterClear.EXTRA_WIPE_SDCARD, wipeExternal);
        MasterClearConfirm fragment = new MasterClearConfirm();
        fragment.setArguments(b);

        return fragment;
    }

    /**
     * The user has gone through the multiple confirmation, so now we go ahead
     * and invoke the Checkin Service to reset the device to its factory-default
     * state (rebooting in the process).
     */
    private void onResetConfirmed() {
        if (Utils.isMonkeyRunning()) {
            return;
        }

        final PersistentDataBlockManager pdbManager = (PersistentDataBlockManager)
                getActivity().getSystemService(Context.PERSISTENT_DATA_BLOCK_SERVICE);

        if (pdbManager != null && !pdbManager.getOemUnlockEnabled()) {
            // if OEM unlock is enabled, this will be wiped during FR process.

            final ProgressDialog progressDialog = new ProgressDialog(getActivity());
            progressDialog.setIndeterminate(true);
            progressDialog.setCancelable(false);
            progressDialog.setTitle(
                    getActivity().getString(R.string.master_clear_progress_title));
            progressDialog.setMessage(
                    getActivity().getString(R.string.master_clear_progress_text));
            progressDialog.show();

            // need to prevent orientation changes as we're about to go into
            // a long IO request, so we won't be able to access inflate resources on flash
            final int oldOrientation = getActivity().getRequestedOrientation();
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    pdbManager.wipe();
                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    progressDialog.hide();
                    getActivity().setRequestedOrientation(oldOrientation);
                    doMasterClear();
                }
            }.execute();
        } else {
            doMasterClear();
        }
    }

    private void doMasterClear() {
        if (mEraseSdCard) {
            Intent intent = new Intent(ExternalStorageFormatter.FORMAT_AND_FACTORY_RESET);
            intent.putExtra(Intent.EXTRA_REASON, "MasterClearConfirm");
            intent.putExtra(MasterClear.EXTRA_WIPE_MEDIA, mEraseInternal);
            intent.putExtra(MasterClear.EXTRA_WIPE_SDCARD, mEraseSdCard);
            intent.setComponent(ExternalStorageFormatter.COMPONENT_NAME);
            getActivity().startService(intent);
        } else {
            Intent intent = new Intent(Intent.ACTION_MASTER_CLEAR);
            intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
            intent.putExtra(MasterClear.EXTRA_WIPE_MEDIA, mEraseInternal);
            intent.putExtra(Intent.EXTRA_REASON, "MasterClearConfirm");
            getActivity().sendBroadcast(intent);
            // Intent handling is asynchronous -- assume it will happen soon.
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (UserManager.get(getActivity()).hasUserRestriction(
                UserManager.DISALLOW_FACTORY_RESET)) {
            return new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.master_clear_not_available)
                    .create();
        }

        final AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.device_reset_title)
                .setMessage(getString(R.string.factory_reset_warning_text_message))
                .setNegativeButton(R.string.cancel_all_caps, null)
                .setPositiveButton(R.string.factory_reset_warning_text_reset_now,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                onResetConfirmed();
                            }
                        })
                .create();
        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                AlertDialog d = (AlertDialog) dialog;
                d.getButton(DialogInterface.BUTTON_POSITIVE)
                        .setTextColor(getResources().getColor(R.color.factory_reset_color));
            }
        });

        return alertDialog;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        mEraseInternal = args != null && args.getBoolean(MasterClear.EXTRA_WIPE_MEDIA, false);
        mEraseSdCard = args != null && args.getBoolean(MasterClear.EXTRA_WIPE_SDCARD, false);

        setShowsDialog(true);
    }
}
