/*
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
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.SeekBarPreference;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.Display;
import android.view.Window;
import android.widget.Toast;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.R;

import java.io.File;
import java.io.IOException;

public class LockscreenGeneral extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String TAG = "LockscreenGeneral";

    private static final String KEY_SEE_TRHOUGH = "see_through";
    private static final String KEY_BLUR_RADIUS = "blur_radius";

    private CheckBoxPreference mSeeThrough;
    private SeekBarPreference mBlurRadius;

    private Activity mActivity;
    private ContentResolver mResolver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getActivity();
        mActivity = getActivity();
        mResolver = mActivity.getContentResolver();

        addPreferencesFromResource(R.xml.lockscreen_general);

        PreferenceScreen prefs = getPreferenceScreen();
        ContentResolver resolver = getContentResolver();

        // lockscreen see through
        mSeeThrough = (CheckBoxPreference) prefs.findPreference(KEY_SEE_TRHOUGH);
        mBlurRadius = (SeekBarPreference) prefs.findPreference(KEY_BLUR_RADIUS);
        mBlurRadius.setProgress(Settings.System.getInt(resolver, 
            Settings.System.LOCKSCREEN_BLUR_RADIUS, 12));
        mBlurRadius.setOnPreferenceChangeListener(this);

    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        ContentResolver cr = getContentResolver();

        if (preference == mSeeThrough) {
              Settings.System.putInt(cr, Settings.System.LOCKSCREEN_SEE_THROUGH,
                      mSeeThrough.isChecked() ? 1 : 0);
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        ContentResolver cr = getActivity().getContentResolver();

        if (preference == mBlurRadius) {
          Settings.System.putInt(cr, Settings.System.LOCKSCREEN_BLUR_RADIUS, (Integer)value);
          return true;
        }
        return false;
    }
}
