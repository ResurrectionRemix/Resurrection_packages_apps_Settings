/*Copyright (C) 2015 The ResurrectionRemix Project
     Licensed under the Apache License, Version 2.0 (the "License");
     you may not use this file except in compliance with the License.
     You may obtain a copy of the License at

          http://www.apache.org/licenses/LICENSE-2.0

     Unless required by applicable law or agreed to in writing, software
     distributed under the License is distributed on an "AS IS" BASIS,
     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     See the License for the specific language governing permissions and
     limitations under the License.
*/
package com.android.settings.rr;

import android.app.Activity;
import android.content.Context;
import android.content.ContentResolver;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.support.v7.preference.ListPreference;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.PreferenceScreen;
import com.android.settings.accessibility.ToggleFontSizePreferenceFragment;
import android.provider.Settings;
import android.text.TextUtils;

import com.android.internal.logging.MetricsProto.MetricsEvent;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import com.android.settings.rr.SeekBarPreference;

public class UISettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "UI";
    private static final String KEY_FONT_SIZE = "font_size";
    private static final String SCREENSHOT_TYPE = "screenshot_type";
    private static final String SCREENSHOT_DELAY = "screenshot_delay";
    private static final String KEY_DOZE_FRAGMENT = "doze_fragment";

    private Preference mFontSizePref;
    private ListPreference mScreenshotType;
    private SeekBarPreference mScreenshotDelay;
    private PreferenceScreen mDozeFragement;

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.RESURRECTED;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Activity activity = getActivity(); 
		ContentResolver resolver = getActivity().getContentResolver();

        addPreferencesFromResource(R.xml.rr_ui_settings);
 		mFontSizePref = findPreference(KEY_FONT_SIZE);

        mScreenshotType = (ListPreference) findPreference(SCREENSHOT_TYPE);
        int mScreenshotTypeValue = Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.SCREENSHOT_TYPE, 0);
        mScreenshotType.setValue(String.valueOf(mScreenshotTypeValue));
        mScreenshotType.setSummary(mScreenshotType.getEntry());
        mScreenshotType.setOnPreferenceChangeListener(this);

        mScreenshotDelay = (SeekBarPreference) findPreference(SCREENSHOT_DELAY);
        int screenshotDelay = Settings.System.getInt(resolver,
                Settings.System.SCREENSHOT_DELAY, 1000);
        mScreenshotDelay.setValue(screenshotDelay / 1);
        mScreenshotDelay.setOnPreferenceChangeListener(this);

        mDozeFragement = (PreferenceScreen) findPreference(KEY_DOZE_FRAGMENT);
        if (!isDozeAvailable(activity)) {
            getPreferenceScreen().removePreference(mDozeFragement);
            mDozeFragement = null;
         }
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if  (preference == mScreenshotType) {
            int mScreenshotTypeValue = Integer.parseInt(((String) objValue).toString());
            mScreenshotType.setSummary(
                    mScreenshotType.getEntries()[mScreenshotTypeValue]);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.SCREENSHOT_TYPE, mScreenshotTypeValue);
            mScreenshotType.setValue(String.valueOf(mScreenshotTypeValue));
            return true;
        } else if (preference == mScreenshotDelay) {
            int screenshotDelay = (Integer) objValue;
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.SCREENSHOT_DELAY, screenshotDelay * 1);
            return true;
         }
        return false;
    }

    private void updateFontSizeSummary() {
        final Context context = mFontSizePref.getContext();
        final float currentScale = Settings.System.getFloat(context.getContentResolver(),
                Settings.System.FONT_SCALE, 1.0f);
        final Resources res = context.getResources();
        final String[] entries = res.getStringArray(R.array.entries_font_size);
        final String[] strEntryValues = res.getStringArray(R.array.entryvalues_font_size);
        final int index = ToggleFontSizePreferenceFragment.fontSizeValueToIndex(currentScale,
                strEntryValues);
        mFontSizePref.setSummary(entries[index]);
    }

    private static boolean isDozeAvailable(Context context) {
        String name = Build.IS_DEBUGGABLE ? SystemProperties.get("debug.doze.component") : null;
        if (TextUtils.isEmpty(name)) {
            name = context.getResources().getString(
                    com.android.internal.R.string.config_dozeComponent);
        }
        return !TextUtils.isEmpty(name);
     }

 @Override
    public void onResume() {
        super.onResume();
        updateFontSizeSummary();
	}
}
