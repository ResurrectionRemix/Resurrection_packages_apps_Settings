/*
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

package com.android.settings.livedisplay;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.UserHandle;
import android.preference.DialogPreference;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import com.android.settings.R;

/**
 * Preference for selection of color temperature range for LiveDisplay
 */
public class DisplayTemperature extends DialogPreference {
    private static final String TAG = "DisplayTemperature";

    private final Context mContext;

    private ColorTemperatureSeekBar mDayTemperature;
    private ColorTemperatureSeekBar mNightTemperature;

    private int mOriginalDayTemperature;
    private int mOriginalNightTemperature;

    private final int mDefaultDayTemperature;
    private final int mDefaultNightTemperature;

    public DisplayTemperature(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;

        mDefaultDayTemperature = mContext.getResources().getInteger(
                com.android.internal.R.integer.config_dayColorTemperature);
        mDefaultNightTemperature = mContext.getResources().getInteger(
                com.android.internal.R.integer.config_nightColorTemperature);

        setDialogLayoutResource(R.layout.display_temperature);
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        builder.setNeutralButton(R.string.settings_reset_button,
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        mOriginalDayTemperature = Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.DISPLAY_TEMPERATURE_DAY,
                mDefaultDayTemperature,
                UserHandle.USER_CURRENT);
        mOriginalNightTemperature = Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.DISPLAY_TEMPERATURE_NIGHT,
                mDefaultNightTemperature,
                UserHandle.USER_CURRENT);

        SeekBar day = (SeekBar) view.findViewById(R.id.day_temperature_seekbar);
        TextView dayText = (TextView) view.findViewById(R.id.day_temperature_value);
        mDayTemperature = new ColorTemperatureSeekBar(day, dayText);

        SeekBar night = (SeekBar) view.findViewById(R.id.night_temperature_seekbar);
        TextView nightText = (TextView) view.findViewById(R.id.night_temperature_value);
        mNightTemperature = new ColorTemperatureSeekBar(night, nightText);

        mDayTemperature.setProgress(mOriginalDayTemperature);
        mNightTemperature.setProgress(mOriginalNightTemperature);
    }

    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);

        // Can't use onPrepareDialogBuilder for this as we want the dialog
        // to be kept open on click
        AlertDialog d = (AlertDialog) getDialog();
        Button defaultsButton = d.getButton(DialogInterface.BUTTON_NEUTRAL);
        defaultsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDayTemperature.setProgress(mDefaultDayTemperature);
                mNightTemperature.setProgress(mDefaultNightTemperature);
                updateTemperature(true);
            }
        });
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        updateTemperature(positiveResult);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        if (getDialog() == null || !getDialog().isShowing()) {
            return superState;
        }

        // Save the dialog state
        final SavedState myState = new SavedState(superState);
        myState.originalDayTemperature = mOriginalDayTemperature;
        myState.originalNightTemperature = mOriginalNightTemperature;
        myState.currentDayTemperature = mDayTemperature.getProgress();
        myState.currentNightTemperature = mNightTemperature.getProgress();

        // Restore the old state when the activity or dialog is being paused
        updateTemperature(false);

        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null || !state.getClass().equals(SavedState.class)) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());

        mOriginalDayTemperature = myState.originalDayTemperature;
        mOriginalNightTemperature = myState.originalNightTemperature;
        mDayTemperature.setProgress(myState.currentDayTemperature);
        mNightTemperature.setProgress(myState.currentNightTemperature);;

        updateTemperature(true);
    }

    private static class SavedState extends BaseSavedState {
        int originalDayTemperature;
        int originalNightTemperature;
        int currentDayTemperature;
        int currentNightTemperature;

        public SavedState(Parcelable superState) {
            super(superState);
        }

        public SavedState(Parcel source) {
            super(source);
            originalDayTemperature = source.readInt();
            originalNightTemperature = source.readInt();
            currentDayTemperature = source.readInt();
            currentNightTemperature = source.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(originalDayTemperature);
            dest.writeInt(originalNightTemperature);
            dest.writeInt(currentDayTemperature);
            dest.writeInt(currentNightTemperature);
        }

        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {

            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    private void updateTemperature(boolean accept) {
        int day = accept ? mDayTemperature.getProgress() : mOriginalDayTemperature;
        int night = accept ? mNightTemperature.getProgress() : mOriginalNightTemperature;
        callChangeListener(new Integer[] { day, night });

        Settings.System.putIntForUser(mContext.getContentResolver(),
                Settings.System.DISPLAY_TEMPERATURE_DAY, day,
                UserHandle.USER_CURRENT);

        Settings.System.putIntForUser(mContext.getContentResolver(),
                Settings.System.DISPLAY_TEMPERATURE_NIGHT, night,
                UserHandle.USER_CURRENT);
    }

    private class ColorTemperatureSeekBar implements SeekBar.OnSeekBarChangeListener {
        private final SeekBar mSeekBar;
        private final TextView mValue;

        private static final int MIN = 1000;
        private static final int MAX = 10000;
        private static final int STEP = 100;

        public ColorTemperatureSeekBar(SeekBar seekBar, TextView value) {
            mSeekBar = seekBar;
            mValue = value;

            mSeekBar.setMax((MAX - MIN) / STEP);
            mSeekBar.setOnSeekBarChangeListener(this);
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser) {
                updateTemperature(true);
            }
            mValue.setText(mContext.getResources().getString(
                    R.string.live_display_color_temperature_label, progress * STEP + MIN));
        }

        public void setProgress(int progress) {
            int p = Math.max(progress, MIN) - MIN;
            mSeekBar.setProgress(Math.round((float) p / STEP));
        }

        public int getProgress() {
            return mSeekBar.getProgress() * STEP + MIN;
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            // Do nothing here
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            // Do nothing here
        }
    }
}
