/*
 * Copyright (C) 2013-2015 The CyanogenMod Project
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

import static cyanogenmod.hardware.CMHardwareManager.FEATURE_DISPLAY_COLOR_CALIBRATION;

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

import com.android.settings.IntervalSeekBar;
import com.android.settings.R;

import cyanogenmod.hardware.CMHardwareManager;

/**
 * Special preference type that allows configuration of Color settings
 */
public class DisplayColor extends DialogPreference {
    private static final String TAG = "ColorCalibration";

    private final Context mContext;

    private final int minRGB;
    private final int maxRGB;
    private final float defaultRGB;
    private final boolean useCMHW;

    // These arrays must all match in length and order
    private static final int[] SEEKBAR_ID = new int[] {
        R.id.color_red_seekbar,
        R.id.color_green_seekbar,
        R.id.color_blue_seekbar
    };

    private static final int[] SEEKBAR_VALUE_ID = new int[] {
        R.id.color_red_value,
        R.id.color_green_value,
        R.id.color_blue_value
    };

    private ColorSeekBar[] mSeekBars = new ColorSeekBar[SEEKBAR_ID.length];

    private final float[] mCurrentColors = new float[3];
    private final float[] mOriginalColors = new float[3];

    public DisplayColor(Context context, AttributeSet attrs) {
        super(context, attrs);

        mContext = context;

        final CMHardwareManager mHardware = CMHardwareManager.getInstance(context);
        useCMHW = mHardware.isSupported(FEATURE_DISPLAY_COLOR_CALIBRATION);
        if (useCMHW) {
            minRGB = mHardware.getDisplayColorCalibrationMin();
            maxRGB = mHardware.getDisplayColorCalibrationMax();
            defaultRGB = (float) mHardware.getDisplayColorCalibrationDefault() / maxRGB;
        } else {
            // Initialize these just to avoid compiler errors.
            minRGB = 20;
            maxRGB = 100;
            defaultRGB = 1.0f;
        }

        setDialogLayoutResource(R.layout.display_color_calibration);
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

        String colorAdjustmentTemp = Settings.System.getStringForUser(mContext.getContentResolver(),
                Settings.System.DISPLAY_COLOR_ADJUSTMENT,
                UserHandle.USER_CURRENT);
        String[] colorAdjustment = colorAdjustmentTemp == null ?
                null : colorAdjustmentTemp.split(" ");
        if (colorAdjustment == null || colorAdjustment.length != 3) {
            colorAdjustment = new String[] { Float.toString(defaultRGB),
                    Float.toString(defaultRGB), Float.toString(defaultRGB) };
        }
        try {
            mOriginalColors[0] = Float.parseFloat(colorAdjustment[0]);
            mOriginalColors[1] = Float.parseFloat(colorAdjustment[1]);
            mOriginalColors[2] = Float.parseFloat(colorAdjustment[2]);
        } catch (NumberFormatException e) {
            mOriginalColors[0] = defaultRGB;
            mOriginalColors[1] = defaultRGB;
            mOriginalColors[2] = defaultRGB;
        }

        System.arraycopy(mOriginalColors, 0, mCurrentColors, 0, 3);

        for (int i = 0; i < SEEKBAR_ID.length; i++) {
            IntervalSeekBar seekBar = (IntervalSeekBar) view.findViewById(SEEKBAR_ID[i]);
            TextView value = (TextView) view.findViewById(SEEKBAR_VALUE_ID[i]);
            mSeekBars[i] = new ColorSeekBar(seekBar, value, i);
            if (useCMHW) {
                mSeekBars[i].mSeekBar.setMinimum((float) minRGB / maxRGB);
                /* Maximum hasn't changed but it's relative to the minimum so it needs
                   to be reset */
                mSeekBars[i].mSeekBar.setMaximum(1.0f);
            }
            mSeekBars[i].mSeekBar.setProgressFloat(mCurrentColors[i]);
            int percent = Math.round(100F * mCurrentColors[i]);
            value.setText(String.format("%d%%", percent));
        }
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
                for (int i = 0; i < mSeekBars.length; i++) {
                    mSeekBars[i].mSeekBar.setProgressFloat(defaultRGB);
                    mCurrentColors[i] = defaultRGB;
                }
                updateColors(mCurrentColors);
            }
        });
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        updateColors(positiveResult ? mCurrentColors : mOriginalColors);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        if (getDialog() == null || !getDialog().isShowing()) {
            return superState;
        }

        // Save the dialog state
        final SavedState myState = new SavedState(superState);
        myState.currentColors = mCurrentColors;
        myState.originalColors = mOriginalColors;

        // Restore the old state when the activity or dialog is being paused
        updateColors(mOriginalColors);

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

        System.arraycopy(myState.originalColors, 0, mOriginalColors, 0, 3);
        System.arraycopy(myState.currentColors, 0, mCurrentColors, 0, 3);
        for (int i = 0; i < mSeekBars.length; i++) {
            mSeekBars[i].mSeekBar.setProgressFloat(mCurrentColors[i]);
        }
        updateColors(mCurrentColors);
    }

    private static class SavedState extends BaseSavedState {
        float[] originalColors;
        float[] currentColors;

        public SavedState(Parcelable superState) {
            super(superState);
        }

        public SavedState(Parcel source) {
            super(source);
            originalColors = source.createFloatArray();
            currentColors = source.createFloatArray();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeFloatArray(originalColors);
            dest.writeFloatArray(currentColors);
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

    private void updateColors(float[] colors) {
        Settings.System.putStringForUser(mContext.getContentResolver(),
                Settings.System.DISPLAY_COLOR_ADJUSTMENT,
                new StringBuilder().append(colors[0]).append(" ")
                                   .append(colors[1]).append(" ")
                                   .append(colors[2]).toString(),
                UserHandle.USER_CURRENT);
    }

    private class ColorSeekBar implements SeekBar.OnSeekBarChangeListener {
        private int mIndex;
        private final IntervalSeekBar mSeekBar;
        private TextView mValue;

        public ColorSeekBar(IntervalSeekBar seekBar, TextView value, int index) {
            mSeekBar = seekBar;
            mValue = value;
            mIndex = index;

            mSeekBar.setOnSeekBarChangeListener(this);
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            IntervalSeekBar isb = (IntervalSeekBar)seekBar;
            float fp = isb.getProgressFloat();
            if (fromUser) {
                mCurrentColors[mIndex] = fp;
                updateColors(mCurrentColors);
            }

            int percent = Math.round(100F * fp);
            mValue.setText(String.format("%d%%", percent));
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
