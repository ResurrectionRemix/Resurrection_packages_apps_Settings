/*
 * Copyright (C) 2016 The CyanogenMod Project
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
import android.util.Range;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import com.android.settings.IntervalSeekBar;
import com.android.settings.R;

import cyanogenmod.hardware.HSIC;
import cyanogenmod.hardware.LiveDisplayManager;

import java.util.List;

/**
 * Special preference type that allows configuration of Color settings
 */
public class PictureAdjustment extends DialogPreference {
    private static final String TAG = "PictureAdjustment";

    private final Context mContext;
    private final LiveDisplayManager mLiveDisplay;
    private final List<Range<Float>> mRanges;

    // These arrays must all match in length and order
    private static final int[] SEEKBAR_ID = new int[] {
        R.id.adj_hue_seekbar,
        R.id.adj_saturation_seekbar,
        R.id.adj_intensity_seekbar,
        R.id.adj_contrast_seekbar
    };

    private static final int[] SEEKBAR_VALUE_ID = new int[] {
        R.id.adj_hue_value,
        R.id.adj_saturation_value,
        R.id.adj_intensity_value,
        R.id.adj_contrast_value
    };

    private ColorSeekBar[] mSeekBars = new ColorSeekBar[SEEKBAR_ID.length];

    private final float[] mCurrentAdj = new float[5];
    private final float[] mOriginalAdj = new float[5];

    public PictureAdjustment(Context context, AttributeSet attrs) {
        super(context, attrs);

        mContext = context;
        mLiveDisplay = LiveDisplayManager.getInstance(mContext);
        mRanges = mLiveDisplay.getConfig().getPictureAdjustmentRanges();

        setDialogLayoutResource(R.layout.display_picture_adjustment);
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

    private void updateBars() {
        for (int i = 0; i < SEEKBAR_ID.length; i++) {
            mSeekBars[i].setValue(mCurrentAdj[i]);
        }
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        System.arraycopy(mLiveDisplay.getPictureAdjustment().toFloatArray(), 0, mOriginalAdj, 0, 5);
        System.arraycopy(mOriginalAdj, 0, mCurrentAdj, 0, 5);

        for (int i = 0; i < SEEKBAR_ID.length; i++) {
            IntervalSeekBar seekBar = (IntervalSeekBar) view.findViewById(SEEKBAR_ID[i]);
            TextView value = (TextView) view.findViewById(SEEKBAR_VALUE_ID[i]);
            final Range<Float> range = mRanges.get(i);
            mSeekBars[i] = new ColorSeekBar(seekBar, range, value, i);
        }
        updateBars();
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
                System.arraycopy(mLiveDisplay.getDefaultPictureAdjustment().toFloatArray(),
                        0, mCurrentAdj, 0, 5);
                updateBars();
                updateAdjustment(mCurrentAdj);
            }
        });
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        updateAdjustment(positiveResult ? mCurrentAdj : mOriginalAdj);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        if (getDialog() == null || !getDialog().isShowing()) {
            return superState;
        }

        // Save the dialog state
        final SavedState myState = new SavedState(superState);
        myState.currentAdj = mCurrentAdj;
        myState.originalAdj = mOriginalAdj;

        // Restore the old state when the activity or dialog is being paused
        updateAdjustment(mOriginalAdj);

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

        System.arraycopy(myState.originalAdj, 0, mOriginalAdj, 0, 5);
        System.arraycopy(myState.currentAdj, 0, mCurrentAdj, 0, 5);

        updateBars();
        updateAdjustment(mCurrentAdj);
    }

    private static class SavedState extends BaseSavedState {
        float[] originalAdj;
        float[] currentAdj;

        public SavedState(Parcelable superState) {
            super(superState);
        }

        public SavedState(Parcel source) {
            super(source);
            originalAdj = source.createFloatArray();
            currentAdj = source.createFloatArray();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeFloatArray(originalAdj);
            dest.writeFloatArray(currentAdj);
        }

        public static final Creator<SavedState> CREATOR =
                new Creator<PictureAdjustment.SavedState>() {

            public PictureAdjustment.SavedState createFromParcel(Parcel in) {
                return new PictureAdjustment.SavedState(in);
            }

            public PictureAdjustment.SavedState[] newArray(int size) {
                return new PictureAdjustment.SavedState[size];
            }
        };
    }

    private void updateAdjustment(final float[] adjustment) {
        mLiveDisplay.setPictureAdjustment(HSIC.fromFloatArray(adjustment));
    }

    private class ColorSeekBar implements SeekBar.OnSeekBarChangeListener {
        private int mIndex;
        private final IntervalSeekBar mSeekBar;
        private TextView mValue;
        private Range<Float> mRange;

        public ColorSeekBar(IntervalSeekBar seekBar, Range<Float> range, TextView value, int index) {
            mSeekBar = seekBar;
            mValue = value;
            mIndex = index;
            mRange = range;
            mSeekBar.setMinimum(range.getLower());
            mSeekBar.setMaximum(range.getUpper());

            mSeekBar.setOnSeekBarChangeListener(this);
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            IntervalSeekBar isb = (IntervalSeekBar)seekBar;
            float fp = isb.getProgressFloat();
            if (fromUser) {
                mCurrentAdj[mIndex] = mRanges.get(mIndex).clamp(fp);
                updateAdjustment(mCurrentAdj);
            }
            mValue.setText(getLabel(mCurrentAdj[mIndex]));
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            // Do nothing here
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            // Do nothing here
        }

        private String getLabel(float value) {
            if (mRange.getUpper() == 1.0f) {
                return String.format("%d%%", Math.round(100F * value));
            }
            return String.format("%d", Math.round(value));
        }

        public void setValue(float value) {
            mSeekBar.setProgressFloat(value);
            mValue.setText(getLabel(value));
        }
    }
}
