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
import android.content.res.Resources;
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

public class LockScreenSecurity extends SettingsPreferenceFragment  implements OnPreferenceChangeListener {
	
    private static final String LOCKSCREEN_MAX_NOTIF_CONFIG = "lockscreen_max_notif_cofig";
    private static final String KEYGUARD_TOGGLE_TORCH = "keyguard_toggle_torch";
    private static final String PREF_LS_BOUNCER = "lockscreen_bouncer";
    private static final String LOCKSCREEN_SECURITY_ALPHA = "lockscreen_security_alpha";	
	

    private SeekBarPreference mMaxKeyguardNotifConfig;
    private SwitchPreference mKeyguardTorch;	
    private ListPreference mLsBouncer;
    private SeekBarPreference mLsSecurityAlpha;		

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.rr_ls_security);
        ContentResolver resolver = getActivity().getContentResolver();

 	mMaxKeyguardNotifConfig = (SeekBarPreference) findPreference(LOCKSCREEN_MAX_NOTIF_CONFIG);
        int kgconf = Settings.System.getInt(resolver,
                Settings.System.LOCKSCREEN_MAX_NOTIF_CONFIG, 5);
        mMaxKeyguardNotifConfig.setValue(kgconf);
        mMaxKeyguardNotifConfig.setOnPreferenceChangeListener(this);

	 mLsBouncer = (ListPreference) findPreference(PREF_LS_BOUNCER);
        mLsBouncer.setOnPreferenceChangeListener(this);
        int lockbouncer = Settings.Secure.getInt(resolver,
                Settings.Secure.LOCKSCREEN_BOUNCER, 0);
        mLsBouncer.setValue(String.valueOf(lockbouncer));
        updateBouncerSummary(lockbouncer);
	
	  mLsSecurityAlpha = (SeekBarPreference) findPreference(LOCKSCREEN_SECURITY_ALPHA);
        float alpha2 = Settings.System.getFloat(resolver,
                Settings.System.LOCKSCREEN_SECURITY_ALPHA, 0.75f);
        mLsSecurityAlpha.setValue((int)(100 * alpha2));
        mLsSecurityAlpha.setOnPreferenceChangeListener(this);
   }

  private void updateBouncerSummary(int value) {
         Resources res = getResources();
  
         if (value == 0) {
             // stock bouncer
             mLsBouncer.setSummary(res.getString(R.string.ls_bouncer_on_summary));
         } else if (value == 1) {
             // bypass bouncer
             mLsBouncer.setSummary(res.getString(R.string.ls_bouncer_off_summary));
         } else {
             String type = null;
             switch (value) {
                 case 2:
                     type = res.getString(R.string.ls_bouncer_dismissable);
                     break;
                 case 3:
                     type = res.getString(R.string.ls_bouncer_persistent);
                     break;
                 case 4:
                     type = res.getString(R.string.ls_bouncer_all);
                     break;
             }
             // Remove title capitalized formatting
             type = type.toLowerCase();
             mLsBouncer.setSummary(res.getString(R.string.ls_bouncer_summary, type));
         }
     }
 

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.APPLICATION;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue)
	{
	ContentResolver resolver = getActivity().getContentResolver();
	if (preference == mMaxKeyguardNotifConfig) {
            int kgconf = (Integer) newValue;
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.LOCKSCREEN_MAX_NOTIF_CONFIG, kgconf);
            return true;
        } else if (preference == mLsSecurityAlpha) {
            int alpha2 = (Integer) newValue;
            Settings.System.putFloat(resolver,
                    Settings.System.LOCKSCREEN_SECURITY_ALPHA, alpha2 / 100.0f);
            return true;
         }
	return false;
	}
}	
