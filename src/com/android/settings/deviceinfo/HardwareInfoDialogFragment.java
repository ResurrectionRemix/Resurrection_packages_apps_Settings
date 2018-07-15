/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.deviceinfo;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.SystemProperties;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

import java.text.DecimalFormat;

public class HardwareInfoDialogFragment extends InstrumentedDialogFragment {

    public static final String TAG = "HardwareInfo";

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.DIALOG_SETTINGS_HARDWARE_INFO;
    }

    public static HardwareInfoDialogFragment newInstance() {
        final HardwareInfoDialogFragment fragment = new HardwareInfoDialogFragment();
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.hardware_info)
                .setPositiveButton(android.R.string.ok, null);
        final View content = LayoutInflater.from(builder.getContext())
                .inflate(R.layout.dialog_hardware_info, null /* parent */);
        // Model
        setText(content, R.id.model_label, R.id.model_value,
                DeviceModelPreferenceController.getDeviceModel());
        // Hardware rev
        setText(content, R.id.hardware_rev_label, R.id.hardware_rev_value,
                SystemProperties.get("ro.boot.hardware.revision"));

        // Platform
        setText(content, R.id.hardware_plat_label, R.id.hardware_plat_value,
                SystemProperties.get("ro.board.platform"));

        // RAM
        ActivityManager actManager = (ActivityManager) builder.getContext().getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        actManager.getMemoryInfo(memInfo);
        DecimalFormat ramDecimalForm = new DecimalFormat("#.#");
        long totRam = memInfo.totalMem;
        double kb = (double)totRam / 1024.0;
        double mb = (double)totRam / 1048576.0;
        double gb = (double)totRam / 1073741824.0;
        String ramString = "";
        if (gb > 1) {
            ramString = ramDecimalForm.format(gb).concat(" GB");
        } else if (mb > 1) {
            ramString = ramDecimalForm.format(mb).concat(" MB");
        } else {
            ramString = ramDecimalForm.format(kb).concat(" KB");
        }
        setText(content, R.id.hardware_ram_label, R.id.hardware_ram_value, ramString);

        return builder.setView(content).create();
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    void setText(View content, int labelViewId, int valueViewId, String value) {
        if (content == null) {
            return;
        }
        final View labelView = content.findViewById(labelViewId);
        final TextView valueView = content.findViewById(valueViewId);
        if (!TextUtils.isEmpty(value)) {
            labelView.setVisibility(View.VISIBLE);
            valueView.setVisibility(View.VISIBLE);
            valueView.setText(value);
        } else {
            labelView.setVisibility(View.GONE);
            valueView.setVisibility(View.GONE);
        }
    }
}
