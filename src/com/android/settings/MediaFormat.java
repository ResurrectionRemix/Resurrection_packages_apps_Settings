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

package com.android.settings;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.storage.StorageVolume;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.android.internal.os.storage.ExternalStorageFormatter;

/**
 * Confirm and execute a format of the sdcard.
 * Multiple confirmations are required: first, a general "are you sure
 * you want to do this?" prompt, followed by a keyguard pattern trace if the user
 * has defined one, followed by a final strongly-worded "THIS WILL ERASE EVERYTHING
 * ON THE SD CARD" prompt.  If at any time the phone is allowed to go to sleep, is
 * locked, et cetera, then the confirmation sequence is abandoned.
 */
public class MediaFormat extends Activity {

    private static final int KEYGUARD_REQUEST = 55;

    private LayoutInflater mInflater;
    private StorageVolume mStorageVolume;

    private View mInitialView;
    private TextView mInitiateTextView;
    private Button mInitiateButton;

    private View mFinalView;
    private TextView mFinalTextView;
    private Button mFinalButton;

    public static boolean isUsbStorage(StorageVolume volumeObj, Context context) {
        if (volumeObj == null || context == null) {
            return false;
        }

        // check if the supplied storage volume is of USB storage
        Resources resourcesObj = context.getResources();
        if (resourcesObj == null) { return false; }

        boolean isUsb = volumeObj.getDescriptionId() == com.android.internal.R.string.storage_usb;

        return isUsb;
    }

    public static boolean isUiccStorage(StorageVolume volumeObj, Context context) {
        if (volumeObj == null || context == null) {
            return false;
        }

        // check if the supplied storage volume is of UICC (Mega SIM) storage
        Resources resourcesObj = context.getResources();
        if (resourcesObj == null) { return false; }

        boolean isUicc = volumeObj.getDescriptionId() == com.android.internal.R.string.storage_uicc;

        return isUicc;
    }

    /**
     * The user has gone through the multiple confirmation, so now we go ahead
     * and invoke the Mount Service to format the SD card.
     */
    private Button.OnClickListener mFinalClickListener = new Button.OnClickListener() {
            public void onClick(View v) {

                if (Utils.isMonkeyRunning()) {
                    return;
                }
                Intent intent = new Intent(ExternalStorageFormatter.FORMAT_ONLY);
                intent.setComponent(ExternalStorageFormatter.COMPONENT_NAME);
                // Transfer the storage volume to the new intent
                final StorageVolume storageVolume = getIntent().getParcelableExtra(
                        StorageVolume.EXTRA_STORAGE_VOLUME);
                intent.putExtra(StorageVolume.EXTRA_STORAGE_VOLUME, storageVolume);
                startService(intent);
                finish();
            }
        };

    /**
     *  Keyguard validation is run using the standard {@link ConfirmLockPattern}
     * component as a subactivity
     */
    private boolean runKeyguardConfirmation(int request) {
        // Initialize default resource id, then set the right string based on volume
        int explanationResId = R.string.sdcard_media_format_gesture_explanation;

        if (isUsbStorage(mStorageVolume, this)) {
            explanationResId = R.string.usb_media_format_gesture_explanation;

        } else if (isUiccStorage(mStorageVolume, this)) {
            explanationResId = R.string.uicc_media_format_gesture_explanation;
        }

        return new ChooseLockSettingsHelper(this)
                .launchConfirmationActivity(request,
                        getText(R.string.media_format_gesture_prompt),
                        getText(explanationResId));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != KEYGUARD_REQUEST) {
            return;
        }

        // If the user entered a valid keyguard trace, present the final
        // confirmation prompt; otherwise, go back to the initial state.
        if (resultCode == Activity.RESULT_OK) {
            establishFinalConfirmationState();
        } else if (resultCode == Activity.RESULT_CANCELED) {
            finish();
        } else {
            establishInitialState();
        }
    }

    /**
     * If the user clicks to begin the reset sequence, we next require a
     * keyguard confirmation if the user has currently enabled one.  If there
     * is no keyguard available, we simply go to the final confirmation prompt.
     */
    private Button.OnClickListener mInitiateListener = new Button.OnClickListener() {
            public void onClick(View v) {
                if (!runKeyguardConfirmation(KEYGUARD_REQUEST)) {
                    establishFinalConfirmationState();
                }
            }
        };

    /**
     * Configure the UI for the final confirmation interaction
     */
    private void establishFinalConfirmationState() {
        if (mFinalView == null) {
            mFinalView = mInflater.inflate(R.layout.media_format_final, null);
            mFinalButton =
                    (Button) mFinalView.findViewById(R.id.execute_media_format);
            mFinalTextView = (TextView) mFinalView
                    .findViewById(R.id.execute_media_format_label);
            mFinalButton.setOnClickListener(mFinalClickListener);
        }

        setContentView(mFinalView);

        // set the title based on volume
        if (isUsbStorage(mStorageVolume, this)) {
            setTitle(R.string.usb_media_format_button_text);
            mFinalTextView.setText(R.string.usb_media_format_final_desc);

        } else if (isUiccStorage(mStorageVolume, this)) {
            setTitle(R.string.uicc_media_format_button_text);
            mFinalTextView.setText(R.string.uicc_media_format_final_desc);
        }
    }

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
        if (mInitialView == null) {
            mInitialView = mInflater.inflate(R.layout.media_format_primary, null);
            mInitiateTextView = (TextView) mInitialView
                    .findViewById(R.id.initiate_media_format_label);
            mInitiateButton =
                    (Button) mInitialView.findViewById(R.id.initiate_media_format);
            mInitiateButton.setOnClickListener(mInitiateListener);
        }

        setContentView(mInitialView);

        // set the title based on volume
        if (isUsbStorage(mStorageVolume, this)) {
            setTitle(R.string.usb_media_format_button_text);
            mInitiateTextView.setText(R.string.usb_media_format_desc);
            mInitiateButton.setText(R.string.usb_media_format_button_text);

        } else if (isUiccStorage(mStorageVolume, this)) {
            setTitle(R.string.uicc_media_format_button_text);
            mInitiateTextView.setText(R.string.uicc_media_format_desc);
            mInitiateButton.setText(R.string.uicc_media_format_button_text);
        }
    }

    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        mInitialView = null;
        mFinalView = null;
        mInflater = LayoutInflater.from(this);

        mStorageVolume = getIntent().getParcelableExtra(
                StorageVolume.EXTRA_STORAGE_VOLUME);

        establishInitialState();
    }

    /** Abandon all progress through the confirmation sequence by returning
     * to the initial view any time the activity is interrupted (e.g. by
     * idle timeout).
     */
    @Override
    public void onPause() {
        super.onPause();

        if (!isFinishing()) {
            establishInitialState();
        }
    }
}
