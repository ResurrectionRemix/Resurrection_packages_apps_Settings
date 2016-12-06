/**
 * Copyright (C) 2013-2016 The CyanogenMod Project
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
import android.os.Vibrator;
import android.support.v7.preference.DialogPreference;
import android.support.v7.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Button;

import com.android.settings.CustomDialogPreference;
import com.android.settings.R;

import cyanogenmod.hardware.CMHardwareManager;
import cyanogenmod.providers.CMSettings;

public class VibratorIntensity extends CustomDialogPreference
        implements SeekBar.OnSeekBarChangeListener {

    private int mOriginalValue;
    private int mMinValue;
    private int mMaxValue;
    private int mDefaultValue;
    private int mWarningValue;

    private SeekBar mSeekBar;
    private TextView mValueText;
    private TextView mWarningText;

    private Drawable mProgressDrawable;
    private Drawable mProgressThumb;
    private LightingColorFilter mRedFilter;

    public VibratorIntensity(final Context context, final AttributeSet attrs) {
        super(context, attrs);

        setDialogLayoutResource(R.layout.vibrator_intensity);
    }

    @Override
    protected void onPrepareDialogBuilder(final AlertDialog.Builder builder,
            final DialogInterface.OnClickListener listener) {
        builder.setNeutralButton(R.string.vibrator_intensity_dialog_reset, null);
    }

    @Override
    protected void onBindDialogView(final View view) {
        super.onBindDialogView(view);

        mSeekBar = (SeekBar) view.findViewById(com.android.internal.R.id.seekbar);
        mValueText = (TextView) view.findViewById(R.id.value);
        mWarningText = (TextView) view.findViewById(R.id.warning_text);

        // Read the current value in case user wants to dismiss his changes
        final CMHardwareManager hardware = CMHardwareManager.getInstance(getContext());
        mOriginalValue = hardware.getVibratorIntensity();
        mWarningValue = hardware.getVibratorWarningIntensity();
        mMinValue = hardware.getVibratorMinIntensity();
        mMaxValue = hardware.getVibratorMaxIntensity();
        mDefaultValue = hardware.getVibratorDefaultIntensity();

        final String message = getContext().getResources().getString(
                R.string.vibrator_intensity_dialog_warning,
                intensityToPercent(mMinValue, mMaxValue, mWarningValue));
        mWarningText.setText(message);
        if (mWarningValue <= 0) {
            mWarningText.setVisibility(View.GONE);
        }

        final Drawable progressDrawable = mSeekBar.getProgressDrawable();
        if (progressDrawable instanceof LayerDrawable) {
            LayerDrawable ld = (LayerDrawable) progressDrawable;
            mProgressDrawable = ld.findDrawableByLayerId(android.R.id.progress);
        }
        mProgressThumb = mSeekBar.getThumb();
        mRedFilter = new LightingColorFilter(Color.BLACK,
                getContext().getResources().getColor(android.R.color.holo_red_light));

        mSeekBar.setOnSeekBarChangeListener(this);
        mSeekBar.setMax(mMaxValue - mMinValue);
        mSeekBar.setProgress(mOriginalValue - mMinValue);
    }

    @Override
    protected boolean onDismissDialog(final DialogInterface dialog, final int which) {
        // Can't use onPrepareDialogBuilder for this as we want the dialog
        // to be kept open on click
        if (which == DialogInterface.BUTTON_NEUTRAL) {
            mSeekBar.setProgress(mDefaultValue - mMinValue);
            setVibratorIntensity(mDefaultValue);
            testVibration();
            return false;
        }
        return true;
    }

    @Override
    protected void onDialogClosed(final boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            // Store percent value in SharedPreferences object
            final SharedPreferences prefs = PreferenceManager
                    .getDefaultSharedPreferences(getContext());
            final int intensity = mSeekBar.getProgress() + mMinValue;
            final int percent = intensityToPercent(mMinValue, mMaxValue, intensity);
            prefs.edit().putInt(CMSettings.Secure.VIBRATOR_INTENSITY, percent).commit();
            CMSettings.Secure.putInt(getContext().getContentResolver(),
                    CMSettings.Secure.VIBRATOR_INTENSITY, intensity);
        } else {
            setVibratorIntensity(mOriginalValue);
            CMSettings.Secure.putInt(getContext().getContentResolver(),
                    CMSettings.Secure.VIBRATOR_INTENSITY, mOriginalValue);
        }
    }

    @Override
    public void onProgressChanged(
                final SeekBar seekBar, final int progress, final boolean fromUser) {
        final int intensity = progress + mMinValue;
        final boolean shouldWarn = mWarningValue > 0 && intensity >= mWarningValue;

        if (mProgressDrawable != null) {
            mProgressDrawable.setColorFilter(shouldWarn ? mRedFilter : null);
        }
        if (mProgressThumb != null) {
            mProgressThumb.setColorFilter(shouldWarn ? mRedFilter : null);
        }

        mValueText.setText(
                String.format("%d%%", intensityToPercent(mMinValue, mMaxValue, intensity)));
    }

    @Override
    public void onStartTrackingTouch(final SeekBar seekBar) {
        // Do nothing
    }

    @Override
    public void onStopTrackingTouch(final SeekBar seekBar) {
        setVibratorIntensity(seekBar.getProgress() + mMinValue);
        testVibration();
    }

    private void setVibratorIntensity(final int intensity) {
        final CMHardwareManager hardware = CMHardwareManager.getInstance(getContext());
        hardware.setVibratorIntensity(intensity);
    }

    private void testVibration() {
        final Vibrator vib = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
        vib.vibrate(200);
    }

    private static int intensityToPercent(final int min, final int max, final int value) {
        int percent = Math.round((value - min) * (100.f / (max - min)));

        if (percent > 100) {
            percent = 100;
        } else if (percent < 0) {
            percent = 0;
        }

        return percent;
    }

    private static int percentToIntensity(final int min, final int max, final int percent) {
        int value = Math.round((((max - min) * percent) / 100.f) + min);

        if (value > max) {
            value = max;
        } else if (value < min) {
            value = min;
        }

        return value;
    }
}
