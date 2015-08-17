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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import com.android.settings.R;

import cyanogenmod.hardware.CMHardwareManager;

import java.util.Arrays;

/**
 * Special preference type that allows configuration of Gamma settings
 */
public class DisplayGamma extends DialogPreference {
    private static final String TAG = "GammaCalibration";

    private static final int[] BAR_COLORS = new int[] {
        R.string.color_red_title,
        R.string.color_green_title,
        R.string.color_blue_title
    };

    private GammaSeekBar[][] mSeekBars;

    private int[][] mCurrentColors;
    private int[][] mOriginalColors;
    private int mNumberOfControls;
    private CMHardwareManager mHardware;

    public DisplayGamma(Context context, AttributeSet attrs) {
        super(context, attrs);

        mHardware = CMHardwareManager.getInstance(context);
        if (!mHardware.isSupported(CMHardwareManager.FEATURE_DISPLAY_GAMMA_CALIBRATION)) {
            return;
        }

        mNumberOfControls = mHardware.getNumGammaControls();
        mSeekBars = new GammaSeekBar[mNumberOfControls][BAR_COLORS.length];

        mOriginalColors = new int[mNumberOfControls][];
        mCurrentColors = new int[mNumberOfControls][];

        setDialogLayoutResource(R.layout.display_gamma_calibration);
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

        final ViewGroup container = (ViewGroup) view.findViewById(R.id.gamma_container);
        final LayoutInflater inflater = LayoutInflater.from(getContext());
        final SharedPreferences prefs = getSharedPreferences();
        final Resources res = container.getResources();
        final String[] gammaDescriptors = res.getStringArray(R.array.gamma_descriptors);

        // Create multiple sets of seekbars, depending on the
        // number of controls the device has
        for (int index = 0; index < mNumberOfControls; index++) {
            mOriginalColors[index] = mHardware.getDisplayGammaCalibration(index);
            mCurrentColors[index] = Arrays.copyOf(mOriginalColors[index],
                    mOriginalColors[index].length);

            final String defaultKey = "display_gamma_default_" + index;
            if (!prefs.contains(defaultKey)) {
                prefs.edit()
                        .putString(defaultKey, buildPreferenceValue(mOriginalColors[index]))
                        .apply();
            }

            if (mNumberOfControls != 1) {
                TextView header = (TextView) inflater.inflate(
                        R.layout.display_gamma_calibration_header, container, false);

                if (index < gammaDescriptors.length) {
                    header.setText(gammaDescriptors[index]);
                } else {
                    header.setText(res.getString(
                            R.string.gamma_tuning_control_set_header, index + 1));
                }
                container.addView(header);
            }

            int min = mHardware.getDisplayGammaCalibrationMin();
            int max = mHardware.getDisplayGammaCalibrationMax();
            for (int color = 0; color < BAR_COLORS.length; color++) {
                ViewGroup item = (ViewGroup) inflater.inflate(
                        R.layout.display_gamma_calibration_item, container, false);

                mSeekBars[index][color] = new GammaSeekBar(index, color, item, min, max);
                mSeekBars[index][color].setGamma(mCurrentColors[index][color]);
                // make sure to add the seekbar group to the container _after_
                // creating GammaSeekBar, so that GammaSeekBar has a chance to
                // get the correct subviews without getting confused by duplicate IDs
                container.addView(item);
            }
        }
    }

    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);

        // can't use onPrepareDialogBuilder for this as we want the dialog
        // to be kept open on click
        AlertDialog d = (AlertDialog) getDialog();
        Button defaultsButton = d.getButton(DialogInterface.BUTTON_NEUTRAL);
        defaultsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (int index = 0; index < mSeekBars.length; index++) {
                    final SharedPreferences prefs = getSharedPreferences();
                    final String defaultKey = "display_gamma_default_" + index;
                    // this key is guaranteed to be present, as we have
                    // created it in onBindDialogView()
                    final String value = prefs.getString(defaultKey, null);
                    final String[] defaultColors = value.split(" ");

                    for (int color = 0; color < BAR_COLORS.length; color++) {
                        int val = Integer.valueOf(defaultColors[color]);
                        mSeekBars[index][color].setGamma(val);
                        mCurrentColors[index][color] = val;
                    }
                    mHardware.setDisplayGammaCalibration(index, mCurrentColors[index]);
                }
            }
       });
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            Editor editor = getEditor();
            for (int i = 0; i < mNumberOfControls; i++) {
                editor.putString("display_gamma_" + i,
                        buildPreferenceValue(mHardware.getDisplayGammaCalibration(i)));
            }
            editor.apply();
        } else if (mOriginalColors != null) {
            for (int i = 0; i < mNumberOfControls; i++) {
                mHardware.setDisplayGammaCalibration(i, mOriginalColors[i]);
            }
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
        myState.controlCount = mNumberOfControls;
        myState.currentColors = mCurrentColors;
        myState.originalColors = mOriginalColors;

        // Restore the old state when the activity or dialog is being paused
        for (int i = 0; i < mNumberOfControls; i++) {
            mHardware.setDisplayGammaCalibration(i, mOriginalColors[i]);
        }
        mOriginalColors = null;

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
        mNumberOfControls = myState.controlCount;
        mOriginalColors = myState.originalColors;
        mCurrentColors = myState.currentColors;

        for (int index = 0; index < mNumberOfControls; index++) {
            for (int color = 0; color < BAR_COLORS.length; color++) {
                mSeekBars[index][color].setGamma(mCurrentColors[index][color]);
            }
            mHardware.setDisplayGammaCalibration(index, mCurrentColors[index]);
        }
    }

    private String buildPreferenceValue(int[] colorValues) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < colorValues.length; i++) {
            if (i != 0) {
                builder.append(" ");
            }
            builder.append(colorValues[i]);
        }
        return builder.toString();
    }

    public static void restore(Context context) {
        final CMHardwareManager hardware = CMHardwareManager.getInstance(context);
        if (!hardware.isSupported(CMHardwareManager.FEATURE_DISPLAY_GAMMA_CALIBRATION)) {
            return;
        }

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int[] rgb = new int[3];
        for (int i = 0; i < hardware.getNumGammaControls(); i++) {
            final String value = prefs.getString("display_gamma_" + i, null);
            if (value != null) {
                final String[] values = value.split(" ");
                rgb[0] = Integer.valueOf(values[0]);
                rgb[1] = Integer.valueOf(values[1]);
                rgb[2] = Integer.valueOf(values[2]);
                hardware.setDisplayGammaCalibration(i, rgb);
            }
        }
    }

    private static class SavedState extends BaseSavedState {
        int controlCount;
        int[][] originalColors;
        int[][] currentColors;

        public SavedState(Parcelable superState) {
            super(superState);
        }

        public SavedState(Parcel source) {
            super(source);
            controlCount = source.readInt();
            originalColors = new int[controlCount][];
            currentColors = new int[controlCount][];
            for (int i = 0; i < controlCount; i++) {
                originalColors[i] = source.createIntArray();
            }
            for (int i = 0; i < controlCount; i++) {
                currentColors[i] = source.createIntArray();
            }
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(controlCount);
            for (int i = 0; i < controlCount; i++) {
                dest.writeIntArray(originalColors[i]);
            }
            for (int i = 0; i < controlCount; i++) {
                dest.writeIntArray(currentColors[i]);
            }
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

    private class GammaSeekBar implements SeekBar.OnSeekBarChangeListener {
        private int mControlIndex;
        private int mColorIndex;
        private int mOriginal;
        private int mMin;
        private int mMax;
        private SeekBar mSeekBar;
        private TextView mValue;

        public GammaSeekBar(int controlIndex, int colorIndex, ViewGroup container,
                int min, int max) {
            mControlIndex = controlIndex;
            mColorIndex = colorIndex;

            mMin = min;
            mMax = max;

            mValue = (TextView) container.findViewById(R.id.color_value);
            mSeekBar = (SeekBar) container.findViewById(R.id.color_seekbar);

            TextView label = (TextView) container.findViewById(R.id.color_text);
            label.setText(container.getContext().getString(BAR_COLORS[colorIndex]));

            mSeekBar.setMax(mMax - mMin);
            mSeekBar.setProgress(0);
            mValue.setText(String.valueOf(mSeekBar.getProgress() + mMin));

            // this must be done last, we don't want to apply our initial value to the hardware
            mSeekBar.setOnSeekBarChangeListener(this);
        }

        public void setGamma(int gamma) {
            mSeekBar.setProgress(gamma - mMin);
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser) {
                mCurrentColors[mControlIndex][mColorIndex] = progress + mMin;
                mHardware.setDisplayGammaCalibration(mControlIndex,
                        mCurrentColors[mControlIndex]);
            }
            mValue.setText(String.valueOf(progress + mMin));
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            // Do nothing
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            // Do nothing
        }
    }
}
