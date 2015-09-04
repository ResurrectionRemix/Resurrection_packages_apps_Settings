/*
 * Copyright (C) 2013-2015 Slimroms
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


package com.android.settings.rr;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.res.Resources;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.os.UserHandle;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.SwitchPreference;
import android.preference.SeekBarPreference;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.MediaStore;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.util.AbstractAsyncSuCMDProcessor;
import com.android.settings.util.CMDProcessor;
import android.view.Display;
import android.view.Window;
import android.widget.Toast;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import java.util.List;
import com.android.settings.Utils;
import com.android.settings.cyanogenmod.SystemSettingSwitchPreference;

import java.io.File;
import java.io.IOException;
import java.io.DataOutputStream;

public class AnimationSettings extends SettingsPreferenceFragment implements
	Preference.OnPreferenceChangeListener {

    private ListPreference mToastAnimation;

    private static final String KEY_TOAST_ANIMATION = "toast_animation";
private final Configuration mCurConfig = new Configuration();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.rr_animation_settings);


		// Toast animation
        	mToastAnimation = (ListPreference)findPreference(KEY_TOAST_ANIMATION);
        	mToastAnimation.setSummary(mToastAnimation.getEntry());
        	int CurrentToastAnimation = Settings.System.getInt(getContentResolver(), Settings.System.TOAST_ANIMATION, 1);
        	mToastAnimation.setValueIndex(CurrentToastAnimation);
        	mToastAnimation.setSummary(mToastAnimation.getEntries()[CurrentToastAnimation]);
        	mToastAnimation.setOnPreferenceChangeListener(this);
    
	}
    public boolean onPreferenceChange(Preference preference, Object newValue) {
       ContentResolver resolver = getActivity().getContentResolver();
        if (preference == mToastAnimation) {
            int index = mToastAnimation.findIndexOfValue((String) newValue);
            Settings.System.putString(getContentResolver(), Settings.System.TOAST_ANIMATION, (String) newValue);
            mToastAnimation.setSummary(mToastAnimation.getEntries()[index]);
            Toast.makeText(mContext, "Toast Test", Toast.LENGTH_SHORT).show();
            return true;
	    }	
	return true;
     }
}
