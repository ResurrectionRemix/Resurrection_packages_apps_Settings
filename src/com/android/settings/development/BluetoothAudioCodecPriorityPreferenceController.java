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

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;

import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

public class BluetoothAudioCodecPriorityPreferenceController extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {

    private static final String A2DP_SOURCE_CODEC_PRIORITY = "a2dp_source_codec_priority";

    private final String[] mListValues;
    private final String[] mListSummaries;

    public BluetoothAudioCodecPriorityPreferenceController(Context context) {
        super(context);

        mListValues = context.getResources().getStringArray(R.array.a2dp_source_codec_priority_values);
        mListSummaries = context.getResources().getStringArray(R.array.a2dp_source_codec_priority_entries);
    }

    @Override
    public String getPreferenceKey() {
        return A2DP_SOURCE_CODEC_PRIORITY;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        writeBluetoothAudioCodecPriorityOptions(newValue);
        updateBluetoothAudioCodecPriorityOptions();
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        updateBluetoothAudioCodecPriorityOptions();
    }

    private void updateBluetoothAudioCodecPriorityOptions() {
        final int value = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.A2DP_SOURCE_CODEC_PRIORITY, 0);
        int index = 0; // default
        for (int i = 0; i < mListValues.length; i++) {
            int val = Integer.parseInt(mListValues[i]);
            if (val >= value) {
                index = i;
                break;
            }
        }
        final ListPreference listPreference = (ListPreference) mPreference;
        listPreference.setValue(mListValues[index]);
        listPreference.setSummary(mListSummaries[index]);
    }

    private void writeBluetoothAudioCodecPriorityOptions(Object newValue) {
        final int value = Integer.valueOf((String) newValue);
        Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.A2DP_SOURCE_CODEC_PRIORITY, value);
    }
}
