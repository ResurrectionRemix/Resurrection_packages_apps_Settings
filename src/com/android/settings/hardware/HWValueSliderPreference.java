/*
 * Copyright (C) 2013 The CyanogenMod Project
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

package com.android.settings.hardware;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Button;

import com.android.settings.R;

public abstract class HWValueSliderPreference extends DialogPreference implements
        SeekBar.OnSeekBarChangeListener {
    private SeekBar mSeekBar;
    private TextView mValue;
    private TextView mWarning;
    private int mOriginalValue;
    private int mMin;

    private HardwareInterface mHw;

    private Drawable mProgressDrawable;
    private Drawable mProgressThumb;
    private LightingColorFilter mRedFilter;

    protected interface HardwareInterface {
        int getMinValue();
        int getMaxValue();
        int getCurrentValue();
        int getDefaultValue();
        int getWarningThreshold();
        boolean setValue(int value);
        String getPreferenceName();
    }

    public HWValueSliderPreference(Context context, AttributeSet attrs, HardwareInterface hw) {
        super(context, attrs);

        if (hw != null) {
            mHw = hw;
            mMin = hw.getMinValue();
        }
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        builder.setNeutralButton(R.string.auto_brightness_reset_button,
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        mSeekBar = (SeekBar) view.findViewById(com.android.internal.R.id.seekbar);
        mValue = (TextView) view.findViewById(R.id.value);
        mWarning = (TextView) view.findViewById(R.id.warning_text);

        int warningThreshold = mHw.getWarningThreshold();
        if (warningThreshold > 0) {
            String message = getContext().getResources().getString(
                    R.string.vibrator_warning, hwValueToPercent(warningThreshold));
            mWarning.setText(message);
        } else if (mWarning != null) {
            mWarning.setVisibility(View.GONE);
        }

        Drawable progressDrawable = mSeekBar.getProgressDrawable();
        if (progressDrawable instanceof LayerDrawable) {
            LayerDrawable ld = (LayerDrawable) progressDrawable;
            mProgressDrawable = ld.findDrawableByLayerId(android.R.id.progress);
        }
        mProgressThumb = mSeekBar.getThumb();
        mRedFilter = new LightingColorFilter(Color.BLACK,
                getContext().getResources().getColor(android.R.color.holo_red_light));

        // Read the current value in case user wants to dismiss his changes
        mOriginalValue = mHw.getCurrentValue();

        // Restore percent value from SharedPreferences object
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());
        int value = settings.getInt(mHw.getPreferenceName(), mHw.getDefaultValue());

        mSeekBar.setOnSeekBarChangeListener(this);
        mSeekBar.setMax(mHw.getMaxValue() - mMin);
        mSeekBar.setProgress(value - mMin);
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
                mSeekBar.setProgress(mHw.getDefaultValue() - mMin);
            }
        });
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            // Store HW value in SharedPreferences object
            SharedPreferences.Editor editor =
                    PreferenceManager.getDefaultSharedPreferences(getContext()).edit();
            editor.putInt(mHw.getPreferenceName(), mSeekBar.getProgress() + mMin);
            editor.commit();
        } else if (mOriginalValue >= mMin) {
            mHw.setValue(mOriginalValue);
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        if (getDialog() == null || !getDialog().isShowing()) {
            return superState;
        }

        // Save the dialog state
        final SavedState myState = new SavedState(superState);
        myState.value = mSeekBar.getProgress();
        myState.originalValue = mOriginalValue;

        // Restore the old state when the activity or dialog is being paused
        mHw.setValue(mOriginalValue);
        mOriginalValue = Integer.MIN_VALUE;

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
        mOriginalValue = myState.originalValue;
        mSeekBar.setProgress(myState.value);
    }

    protected static void restore(Context context, HardwareInterface hw) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int value = prefs.getInt(hw.getPreferenceName(), hw.getDefaultValue());

        hw.setValue(value);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        int warningThreshold = mHw.getWarningThreshold();
        boolean shouldWarn = warningThreshold > 0 && progress >= (warningThreshold - mMin);

        if (mProgressDrawable != null) {
            mProgressDrawable.setColorFilter(shouldWarn ? mRedFilter : null);
        }
        if (mProgressThumb != null) {
            mProgressThumb.setColorFilter(shouldWarn ? mRedFilter : null);
        }

        mHw.setValue(progress + mMin);
        mValue.setText(String.format("%d%%", hwValueToPercent(progress + mMin)));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        // Do nothing here
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        // Do nothing here
    }

    private int hwValueToPercent(int value) {
        double maxValue = mHw.getMaxValue();
        double minValue = mHw.getMinValue();
        double percent = (value - minValue) * (100 / (maxValue - minValue));

        if (percent > 100) {
            percent = 100;
        } else if (percent < 0) {
            percent = 0;
        }

        return (int) percent;
    }

    private static class SavedState extends BaseSavedState {
        int value;
        int originalValue;

        public SavedState(Parcelable superState) {
            super(superState);
        }

        public SavedState(Parcel source) {
            super(source);
            value = source.readInt();
            originalValue = source.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(value);
            dest.writeInt(originalValue);
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
}
