/*
 *  Copyright (C) 2015 The OmniROM Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.android.settings.rr;

import com.android.internal.logging.MetricsLogger;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.Intent;
import android.preference.ListPreference;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.os.UserHandle;
import android.os.UserManager;
import com.android.settings.rr.SeekBarPreference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.provider.Settings;
import android.preference.SwitchPreference;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class LockScreenSettings extends SettingsPreferenceFragment  implements OnPreferenceChangeListener {
    public static final int IMAGE_PICK = 1;

    private static final String KEY_WALLPAPER_SET = "lockscreen_wallpaper_set";
    private static final String KEY_WALLPAPER_CLEAR = "lockscreen_wallpaper_clear";
    private static final String KEY_BLUR_RADIUS = "lockscreen_blur_radius";	

    private Preference mSetWallpaper;
    private Preference mClearWallpaper;
    private SeekBarPreference mBlurRadius;	

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.rr_lockscreen);

        mSetWallpaper = (Preference) findPreference(KEY_WALLPAPER_SET);
        mClearWallpaper = (Preference) findPreference(KEY_WALLPAPER_CLEAR);
     
	// Blur
        mBlurRadius =
                (SeekBarPreference) findPreference(KEY_BLUR_RADIUS);
        if (mBlurRadius != null) {
            int blurRadius = Settings.System.getInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_BLUR_RADIUS, 14);
            mBlurRadius.setValue(blurRadius);
            mBlurRadius.setOnPreferenceChangeListener(this);
        }
    }


   

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.APPLICATION;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mSetWallpaper) {
            setKeyguardWallpaper();
            return true;
        } else if (preference == mClearWallpaper) {
            clearKeyguardWallpaper();
            return true;
        }  
	return super.onPreferenceTreeClick(preferenceScreen, preference);
}
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == IMAGE_PICK && resultCode == Activity.RESULT_OK) {
            if (data != null && data.getData() != null) {
                Uri uri = data.getData();
                Intent intent = new Intent();
                intent.setClassName("com.android.wallpapercropper", "com.android.wallpapercropper.WallpaperCropActivity");
                intent.putExtra("keyguardMode", "1");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.setData(uri);
                startActivity(intent);
            }
        }
    }

    private void setKeyguardWallpaper() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, IMAGE_PICK);
    }

    private void clearKeyguardWallpaper() {
        WallpaperManager wallpaperManager = null;
        wallpaperManager = WallpaperManager.getInstance(getActivity());
        wallpaperManager.clearKeyguardWallpaper();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value)
	{
 boolean result = true;
        final String key = preference.getKey();
	if (KEY_BLUR_RADIUS.equals(key)) {
            int bluRadius = (Integer) value;
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.LOCKSCREEN_BLUR_RADIUS, bluRadius);
        }
        return result;
	}
}
