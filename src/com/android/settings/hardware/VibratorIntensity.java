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

import android.content.Context;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.widget.SeekBar;

import com.android.settings.R;
import org.cyanogenmod.hardware.VibratorHW;

public class VibratorIntensity extends HWValueSliderPreference {
    private static final HardwareInterface HW_INTERFACE = new HardwareInterface() {
        @Override
        public int getMinValue() {
            return VibratorHW.getMinIntensity();
        }
        @Override
        public int getMaxValue() {
            return VibratorHW.getMaxIntensity();
        }
        @Override
        public int getCurrentValue() {
            return VibratorHW.getCurIntensity();
        }
        @Override
        public int getDefaultValue() {
            return VibratorHW.getDefaultIntensity();
        }
        @Override
        public int getWarningThreshold() {
            return VibratorHW.getWarningThreshold();
        }
        @Override
        public boolean setValue(int value) {
            return VibratorHW.setIntensity(value);
        }
        @Override
        public String getPreferenceName() {
            return "vibration_intensity";
        }
    };

    public VibratorIntensity(Context context, AttributeSet attrs) {
        super(context, attrs, isSupported() ? HW_INTERFACE : null);

        setDialogLayoutResource(R.layout.vibrator_intensity);
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        Vibrator vib = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
        vib.vibrate(200);
    }

    public static boolean isSupported() {
        try {
            return VibratorHW.isSupported();
        } catch (NoClassDefFoundError e) {
            // Hardware abstraction framework isn't installed
            return false;
        }
    }

    public static void restore(Context context) {
        if (!isSupported()) {
            return;
        }
        HWValueSliderPreference.restore(context, HW_INTERFACE);
    }
}
