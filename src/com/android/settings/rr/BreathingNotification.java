/*
 * Copyright (C) 2014-2015 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.settings.rr;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ContentResolver;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.SwitchPreference;
import android.preference.PreferenceFragment;
import android.preference.Preference.OnPreferenceChangeListener;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.preference.PreferenceScreen;
import android.text.format.DateFormat;
import android.os.UserHandle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.internal.util.rr.RRUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import cyanogenmod.providers.CMSettings;

import net.margaritov.preference.colorpicker.ColorPickerPreference;

public class BreathingNotification extends SettingsPreferenceFragment
        implements OnPreferenceChangeListener {

    private static final String MISSED_CALL_BREATH = "missed_call_breath";
    private static final String VOICEMAIL_BREATH = "voicemail_breath";
    private static final String SMS_BREATH = "sms_breath";

    private SwitchPreference mMissedCallBreath;
    private SwitchPreference mVoicemailBreath;
    private SwitchPreference mSmsBreath;

  @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

	addPreferencesFromResource(R.xml.breathing_notif);
	PreferenceScreen prefSet = getPreferenceScreen();
	ContentResolver resolver = getActivity().getContentResolver(); 
        Context context = getActivity();

 	 mMissedCallBreath = (SwitchPreference) findPreference(MISSED_CALL_BREATH);
         mVoicemailBreath = (SwitchPreference) findPreference(VOICEMAIL_BREATH);
         mSmsBreath = (SwitchPreference) findPreference(SMS_BREATH);

         ConnectivityManager cm = (ConnectivityManager)
                   context.getSystemService(Context.CONNECTIVITY_SERVICE);

         if (cm.isNetworkSupported(ConnectivityManager.TYPE_MOBILE)) {

             mMissedCallBreath.setChecked(Settings.System.getInt(resolver,
                     Settings.System.KEY_MISSED_CALL_BREATH, 0) == 1);
             mMissedCallBreath.setOnPreferenceChangeListener(this);

             mVoicemailBreath.setChecked(Settings.System.getInt(resolver,
                     Settings.System.KEY_VOICEMAIL_BREATH, 0) == 1);
             mVoicemailBreath.setOnPreferenceChangeListener(this);

	    mSmsBreath.setChecked(Settings.System.getInt(resolver,
                    Settings.System.KEY_SMS_BREATH, 0) == 1);
            mSmsBreath.setOnPreferenceChangeListener(this);

         } else {
             prefSet.removePreference(mMissedCallBreath);
             prefSet.removePreference(mVoicemailBreath);
             prefSet.removePreference(mSmsBreath);
          }
	}

    protected int getMetricsCategory() {
        return MetricsLogger.APPLICATION;
    } 

   @Override
    public void onResume() {
        super.onResume();
    }

	public boolean onPreferenceChange(Preference preference, Object newValue) {
		ContentResolver resolver = getActivity().getContentResolver();
	  if (preference == mMissedCallBreath) {
             boolean value = (Boolean) newValue;
             Settings.System.putInt(resolver, Settings.System.KEY_MISSED_CALL_BREATH, value ? 1 : 0);
             return true;
        } else if (preference == mVoicemailBreath) {
          boolean value = (Boolean) newValue;
          Settings.System.putInt(resolver, Settings.System.KEY_VOICEMAIL_BREATH, value ? 1 : 0);
          return true;
	} else if (preference == mSmsBreath) {
            boolean value = (Boolean) newValue;
            Settings.System.putInt(resolver, Settings.System.KEY_SMS_BREATH, value ? 1 : 0);
            return true;
	}
	return false;
    }
}



