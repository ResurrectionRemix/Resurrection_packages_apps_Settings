/*
 * Copyright (C) 2020 crDroid Android Project
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
 * limitations under the License
 */

package com.android.settings.rr;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

/**
 * Dialog to set the Vibration Intensities of ringer.
 */
public class VibrationIntensityDialog extends InstrumentedDialogFragment {
    private static final String TAG = "VibrationIntensityDialog";
    private static final String RING_VIBRATION_INTENSITY = "ring_vibration_intensity";
    private static final String NOTIFICATION_VIBRATION_INTENSITY = "notification_vibration_intensity";

    private Context mContext;
    private String mPreferenceKey;
    private int mProgress;
    private Preference mPreference;
    private boolean mIsRinger;

    public void setParameters(Context context, String preferenceKey, Preference preference) {
        mContext = context;
        mPreferenceKey = preferenceKey;
        mPreference = preference;
    }

    @Override
    public int getMetricsCategory() {
        return (mIsRinger) ? SettingsEnums.ACCESSIBILITY_VIBRATION_RING : SettingsEnums.ACCESSIBILITY_VIBRATION_NOTIFICATION;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        int dialogTitle;

        mIsRinger = mPreferenceKey.equals(RING_VIBRATION_INTENSITY);
        final ContentResolver contentResolver = mContext.getContentResolver();

        final View view = getActivity().getLayoutInflater().inflate(
                R.layout.dialog_vibration_intensity, null);
        final TextView txtView = view.findViewById(R.id.vibration_intensity_text);
        final SeekBar sb = view.findViewById(R.id.vibration_intensity_seekbar);

        if (mIsRinger) {
            sb.setMin(1);
            sb.setMax(3);
            mProgress = Settings.System.getIntForUser(contentResolver, RING_VIBRATION_INTENSITY, 2, UserHandle.USER_CURRENT);
            sb.setProgress(mProgress);
            setText(txtView, mPreference, mProgress);
            dialogTitle = R.string.vibration_intensity_ringer;
        } else {
            sb.setMin(1);
            sb.setMax(3);
            mProgress = Settings.System.getIntForUser(contentResolver, NOTIFICATION_VIBRATION_INTENSITY, 2, UserHandle.USER_CURRENT);
            sb.setProgress(mProgress);
            setText(txtView, mPreference, mProgress);
            dialogTitle = R.string.vibration_intensity_notification;
        }

        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged (SeekBar seekBar, int progress, boolean fromUser) {
                mProgress = progress;
                setText(txtView, mPreference, progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Settings.System.putIntForUser(contentResolver, (mIsRinger) ? RING_VIBRATION_INTENSITY : NOTIFICATION_VIBRATION_INTENSITY,
                    mProgress, UserHandle.USER_CURRENT);
            }
        });

        return new AlertDialog.Builder(getContext())
                .setTitle(dialogTitle)
                .setView(view)
                .setPositiveButton(R.string.okay, null)
                .create();
    }

    private void setText(TextView txtView, Preference preference, int status) {
        switch(status) {
            case 0:
                txtView.setText(R.string.vibration_intensity_disabled);
                preference.setSummary(R.string.vibration_intensity_disabled);
                break;
            case 1:
                txtView.setText(R.string.vibration_intensity_light);
                preference.setSummary(R.string.vibration_intensity_light);
                break;
            case 2:
                txtView.setText(R.string.vibration_intensity_medium);
                preference.setSummary(R.string.vibration_intensity_medium);
                break;
            case 3:
                txtView.setText(R.string.vibration_intensity_strong);
                preference.setSummary(R.string.vibration_intensity_strong);
                break;
            case 4:
                txtView.setText(R.string.vibration_intensity_custom);
                preference.setSummary(R.string.vibration_intensity_custom);
                break;
        }
    }
}
