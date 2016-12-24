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
import android.provider.Settings;
import android.os.Bundle;
import android.support.v7.preference.ListPreference;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.PreferenceScreen;
import android.widget.Toast;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import net.margaritov.preference.colorpicker.ColorPickerPreference;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

public class ToastAnimations extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "ToastAnimations";
    private static final String KEY_TOAST_ANIMATION = "toast_animation";
    private static final String TOAST_ICON_COLOR = "toast_icon_color";
    private static final String TOAST_TEXT_COLOR = "toast_text_color";

    private ListPreference mToastAnimation;
    private ColorPickerPreference mIconColor;
    private ColorPickerPreference mTextColor;
    protected Context mContext;
    protected ContentResolver mContentRes;

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.RESURRECTED;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.rr_toast);
	mContext = getActivity().getApplicationContext();
	mContentRes = getActivity().getContentResolver();

        int intColor = 0xffffffff;
        String hexColor = String.format("#%08x", (0xffffffff & 0xffffffff));

        mIconColor =
                (ColorPickerPreference) findPreference(TOAST_ICON_COLOR);
        intColor = Settings.System.getInt(getContentResolver(),
                Settings.System.TOAST_ICON_COLOR, 0xffffffff);
        hexColor = String.format("#%08x", (0xffffffff & intColor));
        mIconColor.setNewPreviewColor(intColor);
        mIconColor.setSummary(hexColor);
        mIconColor.setOnPreferenceChangeListener(this);

        mTextColor =
                (ColorPickerPreference) findPreference(TOAST_TEXT_COLOR);
        intColor = Settings.System.getInt(getContentResolver(),
                Settings.System.TOAST_TEXT_COLOR, 0xffffffff);
        hexColor = String.format("#%08x", (0xffffffff & intColor));
        mTextColor.setNewPreviewColor(intColor);
        mTextColor.setSummary(hexColor);
        mTextColor.setOnPreferenceChangeListener(this);

 	mToastAnimation = (ListPreference) findPreference(KEY_TOAST_ANIMATION);
        mToastAnimation.setSummary(mToastAnimation.getEntry());
        int CurrentToastAnimation = Settings.System.getInt(getContentResolver(), Settings.System.TOAST_ANIMATION, 1);
        mToastAnimation.setValueIndex(CurrentToastAnimation); //set to index of default value
        mToastAnimation.setSummary(mToastAnimation.getEntries()[CurrentToastAnimation]);
        mToastAnimation.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue){
	   boolean result = false;
           if (preference == mToastAnimation) {
              int index = mToastAnimation.findIndexOfValue((String) newValue);
              Settings.System.putString(getContentResolver(), Settings.System.TOAST_ANIMATION, (String) newValue);
              mToastAnimation.setSummary(mToastAnimation.getEntries()[index]);
              Toast.makeText(mContext, "Toast Test", Toast.LENGTH_SHORT).show();
              return true;
          }  else if (preference == mIconColor) {
            String hex = ColorPickerPreference.convertToARGB(Integer
                    .valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.TOAST_ICON_COLOR, intHex);
             return true;
        } else if (preference == mTextColor) {
            String hex = ColorPickerPreference.convertToARGB(Integer
                    .valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.TOAST_TEXT_COLOR, intHex);
             return true;
       }
        return false;
    }
}
