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
import android.content.Context;
import android.content.ContentResolver;
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
import android.provider.SearchIndexableResource;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;

import java.util.List;
import java.util.ArrayList;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class LockscreenUI extends SettingsPreferenceFragment  implements OnPreferenceChangeListener {


    private static final String KEY_LOCKSCREEN_BLUR_RADIUS = "lockscreen_blur_radius";
    private static final String LOCK_CLOCK_FONTS = "lock_clock_fonts";	
	
    private SeekBarPreference mBlurRadius;
    private ListPreference mLockClockFonts;	

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.rr_ls_ui);
        ContentResolver resolver = getActivity().getContentResolver();


	mBlurRadius = (SeekBarPreference) findPreference(KEY_LOCKSCREEN_BLUR_RADIUS);
            mBlurRadius.setValue(Settings.System.getInt(resolver,
                    Settings.System.LOCKSCREEN_BLUR_RADIUS, 14));
            mBlurRadius.setOnPreferenceChangeListener(this);


            mLockClockFonts = (ListPreference) findPreference(LOCK_CLOCK_FONTS);
            mLockClockFonts.setValue(String.valueOf(Settings.System.getInt(
                    resolver, Settings.System.LOCK_CLOCK_FONTS, 4)));
            mLockClockFonts.setSummary(mLockClockFonts.getEntry());
            mLockClockFonts.setOnPreferenceChangeListener(this);

}

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.APPLICATION;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue)
	{
	ContentResolver resolver = getActivity().getContentResolver();
	 if (preference == mBlurRadius) {
                int width = ((Integer)newValue).intValue();
                Settings.System.putInt(resolver,
                        Settings.System.LOCKSCREEN_BLUR_RADIUS, width);
                return true;
	} else if (preference == mLockClockFonts) {
                Settings.System.putInt(resolver, Settings.System.LOCK_CLOCK_FONTS,
                        Integer.valueOf((String) newValue));
                mLockClockFonts.setValue(String.valueOf(newValue));
                mLockClockFonts.setSummary(mLockClockFonts.getEntry());
                return true;
	} 
	return false;
	}
}	
