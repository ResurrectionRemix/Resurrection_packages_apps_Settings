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
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import com.android.settings.R;

import org.cyanogenmod.internal.util.MathUtils;

import cyanogenmod.hardware.LiveDisplayConfig;
import cyanogenmod.hardware.LiveDisplayManager;

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

    private final LiveDisplayManager mLiveDisplay;
    private final LiveDisplayConfig mConfig;

    private static final int STEP = 100;

    public DisplayTemperature(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mLiveDisplay = LiveDisplayManager.getInstance(mContext);
        mConfig = mLiveDisplay.getConfig();

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

        mOriginalDayTemperature = mLiveDisplay.getDayColorTemperature();
        mOriginalNightTemperature = mLiveDisplay.getNightColorTemperature();

        SeekBar day = (SeekBar) view.findViewById(R.id.day_temperature_seekbar);
        TextView dayText = (TextView) view.findViewById(R.id.day_temperature_value);
        mDayTemperature = new ColorTemperatureSeekBar(day, dayText);

        SeekBar night = (SeekBar) view.findViewById(R.id.night_temperature_seekbar);
        TextView nightText = (TextView) view.findViewById(R.id.night_temperature_value);
        mNightTemperature = new ColorTemperatureSeekBar(night, nightText);

        mDayTemperature.setTemperature(mOriginalDayTemperature);
        mNightTemperature.setTemperature(mOriginalNightTemperature);
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
                mDayTemperature.setTemperature(mConfig.getDefaultDayTemperature());
                mNightTemperature.setTemperature(mConfig.getDefaultNightTemperature());
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
        myState.currentDayTemperature = mDayTemperature.getTemperature();
        myState.currentNightTemperature = mNightTemperature.getTemperature();

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
        mDayTemperature.setTemperature(myState.currentDayTemperature);
        mNightTemperature.setTemperature(myState.currentNightTemperature);;

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
        int day = accept ? mDayTemperature.getTemperature() : mOriginalDayTemperature;
        int night = accept ? mNightTemperature.getTemperature() : mOriginalNightTemperature;
        callChangeListener(new Integer[] { day, night });

        mLiveDisplay.setDayColorTemperature(day);
        mLiveDisplay.setNightColorTemperature(night);
    }

    int roundUp(int value) {
        return ((value + STEP / 2) / STEP) * STEP;
    }

    private class ColorTemperatureSeekBar implements SeekBar.OnSeekBarChangeListener {
        private final SeekBar mSeekBar;
        private final TextView mValue;

        private final int mMin;
        private final int mMax;

        private final int mBalanceMin;
        private final int mBalanceMax;

        private final int mBarMax;

        private final boolean mUseBalance;
        private final double[] mBalanceCurve;

        public ColorTemperatureSeekBar(SeekBar seekBar, TextView value) {
            mSeekBar = seekBar;
            mValue = value;
            mMin = mConfig.getColorTemperatureRange().getLower();
            mMax = mConfig.getColorTemperatureRange().getUpper();
            mBalanceMin = mConfig.getColorBalanceRange().getLower();
            mBalanceMax = mConfig.getColorBalanceRange().getUpper();
            mUseBalance = mConfig.hasFeature(LiveDisplayManager.FEATURE_COLOR_BALANCE) &&
                    ((mBalanceMin != 0) || (mBalanceMax != 0));

            if (mUseBalance) {
                mBalanceCurve = MathUtils.powerCurve(mMin, mConfig.getDefaultDayTemperature(), mMax);
                mBarMax = mBalanceMax - mBalanceMin;
            } else {
                mBalanceCurve = null;
                mBarMax = (mMax - mMin) / STEP;
            }
            mSeekBar.setMax(mBarMax);
            mSeekBar.setOnSeekBarChangeListener(this);
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser) {
                updateTemperature(true);
            }

            int displayValue;
            if (mUseBalance) {
                displayValue = roundUp(Math.round((float)MathUtils.linearToPowerCurve(
                        mBalanceCurve, (double)progress / (double)mBarMax)));
            } else {
                displayValue = progress * STEP + mMin;
            }
            Log.d(TAG, "onProgressChanged: progress=" + progress + " displayValue=" + displayValue);

            mValue.setText(mContext.getResources().getString(
                    R.string.live_display_color_temperature_label, displayValue));
        }

        public void setTemperature(int temperature) {
            if (mUseBalance) {
                double z = MathUtils.powerCurveToLinear(mBalanceCurve, (double)temperature);
                mSeekBar.setProgress(Math.round((float)(z * (double)mBarMax)));
                return;
            }
            int p = Math.max(temperature, mMin) - mMin;
            mSeekBar.setProgress(Math.round((float) p / STEP));
        }

        public int getTemperature() {
            if (mUseBalance) {
                return Math.round((float)MathUtils.linearToPowerCurve(
                        mBalanceCurve, (double)mSeekBar.getProgress() / (double)mBarMax));
            }
            return mSeekBar.getProgress() * STEP + mMin;
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
