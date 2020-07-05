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
 * limitations under the License.
 */

package com.android.settings.development;

import android.bluetooth.BluetoothCodecConfig;
import android.content.Context;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settingslib.core.lifecycle.Lifecycle;

public class BluetoothAudioCodecPriorityPreferenceController extends
        AbstractBluetoothA2dpPreferenceController {

    private static final int DEFAULT_INDEX = 0;
    private static final String A2DP_SOURCE_CODEC_PRIORITY = "a2dp_source_codec_priority";

    public BluetoothAudioCodecPriorityPreferenceController(Context context, Lifecycle lifecycle,
            BluetoothA2dpConfigStore store) {
        super(context, lifecycle, store);
    }

    @Override
    public String getPreferenceKey() {
        return A2DP_SOURCE_CODEC_PRIORITY;
    }

    @Override
    protected String[] getListValues() {
        return mContext.getResources().getStringArray(
                R.array.a2dp_source_codec_priority_values);
    }

    @Override
    protected String[] getListSummaries() {
        return mContext.getResources().getStringArray(
                R.array.a2dp_source_codec_priority_entries);
    }

    @Override
    protected int getDefaultIndex() {
        return DEFAULT_INDEX;
    }

    @Override
    protected void writeConfigurationValues(Object newValue) {
        final int value = Integer.valueOf((String) newValue);
        Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.A2DP_SOURCE_CODEC_PRIORITY, value);

        int codecTypeValue = BluetoothCodecConfig.SAMPLE_RATE_NONE; // default
        int codecPriorityValue = BluetoothCodecConfig.CODEC_PRIORITY_DEFAULT;
        switch (value) {
            case 0:
            default:
                // do nothing
                break;
            case 1:
                codecTypeValue = BluetoothCodecConfig.SOURCE_CODEC_TYPE_SBC;
                codecPriorityValue = BluetoothCodecConfig.CODEC_PRIORITY_HIGHEST;
                break;
            case 2:
                codecTypeValue = BluetoothCodecConfig.SOURCE_CODEC_TYPE_AAC;
                codecPriorityValue = BluetoothCodecConfig.CODEC_PRIORITY_HIGHEST;
                break;
            case 3:
                codecTypeValue = BluetoothCodecConfig.SOURCE_CODEC_TYPE_APTX;
                codecPriorityValue = BluetoothCodecConfig.CODEC_PRIORITY_HIGHEST;
                break;
            case 4:
                codecTypeValue = BluetoothCodecConfig.SOURCE_CODEC_TYPE_APTX_HD;
                codecPriorityValue = BluetoothCodecConfig.CODEC_PRIORITY_HIGHEST;
                break;
            case 5:
                codecTypeValue = BluetoothCodecConfig.SOURCE_CODEC_TYPE_LDAC;
                codecPriorityValue = BluetoothCodecConfig.CODEC_PRIORITY_HIGHEST;
                break;
            case 6:
                codecTypeValue = BluetoothCodecConfig.SOURCE_CODEC_TYPE_APTX_ADAPTIVE;
                codecPriorityValue = BluetoothCodecConfig.CODEC_PRIORITY_HIGHEST;
                break;
            case 7:
                codecTypeValue = BluetoothCodecConfig.SOURCE_CODEC_TYPE_APTX_TWSP;
                codecPriorityValue = BluetoothCodecConfig.CODEC_PRIORITY_HIGHEST;
                break;
        }
        mBluetoothA2dpConfigStore.setCodecType(codecTypeValue);
        mBluetoothA2dpConfigStore.setCodecPriority(codecPriorityValue);
    }

    @Override
    protected int getCurrentA2dpSettingIndex(BluetoothCodecConfig config) {
        return Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.A2DP_SOURCE_CODEC_PRIORITY, 0);
    }
}
