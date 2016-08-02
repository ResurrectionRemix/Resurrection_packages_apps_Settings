/*
 * Copyright (C) 2015 The Dirty Unicorns Project
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

import java.util.ArrayList;

import net.margaritov.preference.colorpicker.ColorPickerPreference;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.graphics.Color;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.preference.Preference;
import android.preference.ListPreference;
import android.provider.Settings;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import android.provider.SearchIndexableResource;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;

import com.android.settings.rr.SeekBarPreference;

import java.util.ArrayList;
import java.util.List;

public class PulseSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener , Indexable {
    private static final String TAG = PulseSettings.class.getSimpleName();
    private static final String CUSTOM_DIMEN = "pulse_custom_dimen";
    private static final String CUSTOM_DIV = "pulse_custom_div";
    private static final String PULSE_BLOCK = "pulse_filled_block_size";
    private static final String EMPTY_BLOCK = "pulse_empty_block_size";
    private static final String FUDGE_FACOR = "pulse_custom_fudge_factor";
    private static final int RENDER_STYLE_FADING_BARS = 0;
    private static final int RENDER_STYLE_SOLID_LINES = 1;
    private static final String SOLID_FUDGE = "pulse_solid_fudge_factor";
    private static final String SOLID_LAVAMP_SPEED = "lavamp_solid_speed";
    private static final String FADING_LAVAMP_SPEED = "fling_pulse_lavalamp_speed";

    private static final int MENU_RESET = Menu.FIRST;
    private static final int DLG_RESET = 0;

    SwitchPreference mShowPulse;
    ListPreference mRenderMode;
    SwitchPreference mLavaLampEnabled;
    ColorPickerPreference mPulseColor;
    SeekBarPreference mCustomDimen;
    SeekBarPreference mCustomDiv;
    SeekBarPreference mFilled;
    SeekBarPreference mEmpty;
    SeekBarPreference mFudge;
    SeekBarPreference mSolidFudge;
    SeekBarPreference mSolidSpeed;
    SeekBarPreference mFadingSpeed;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
	PreferenceScreen prefs = getPreferenceScreen();

        addPreferencesFromResource(R.xml.pulse_settings);
        ActionBar bar = getActivity().getActionBar();
        if (bar != null) {
            bar.setTitle(R.string.pulse_settings);
        }

        mShowPulse = (SwitchPreference) findPreference("eos_fling_show_pulse");
        mShowPulse.setChecked(Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.FLING_PULSE_ENABLED, 0) == 1);
        mShowPulse.setOnPreferenceChangeListener(this);

        int renderMode = Settings.Secure.getIntForUser(getContentResolver(),
                Settings.Secure.PULSE_RENDER_STYLE_URI, RENDER_STYLE_SOLID_LINES, UserHandle.USER_CURRENT);
        mRenderMode = (ListPreference) findPreference("pulse_render_mode");
        mRenderMode.setValue(String.valueOf(renderMode));
        mRenderMode.setOnPreferenceChangeListener(this);

        PreferenceCategory fadingBarsCat = (PreferenceCategory)findPreference("pulse_1");
        fadingBarsCat.setEnabled(renderMode == RENDER_STYLE_FADING_BARS);

        int pulseColor = Settings.Secure.getIntForUser(getContentResolver(),
                Settings.Secure.FLING_PULSE_COLOR, Color.WHITE, UserHandle.USER_CURRENT);
        mPulseColor = (ColorPickerPreference) findPreference("eos_fling_pulse_color");
        mPulseColor.setNewPreviewColor(pulseColor);
        mPulseColor.setOnPreferenceChangeListener(this);

        mLavaLampEnabled = (SwitchPreference) findPreference("eos_fling_lavalamp");
        mLavaLampEnabled.setChecked(Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.FLING_PULSE_LAVALAMP_ENABLED, 1) == 1);
        mLavaLampEnabled.setOnPreferenceChangeListener(this);

        mCustomDimen =
                    (SeekBarPreference) findPreference(CUSTOM_DIMEN);
        int customdimen = Settings.Secure.getIntForUser(getContentResolver(),
                    Settings.Secure.PULSE_CUSTOM_DIMEN, 14,
                    UserHandle.USER_CURRENT);
        mCustomDimen.setValue(customdimen / 1);
        mCustomDimen.setOnPreferenceChangeListener(this);

        mCustomDiv = (SeekBarPreference) findPreference(CUSTOM_DIV);
        int customdiv = Settings.Secure.getIntForUser(getContentResolver(),
                    Settings.Secure.PULSE_CUSTOM_DIV, 8,
                    UserHandle.USER_CURRENT);
        mCustomDiv.setValue(customdiv / 1);
        mCustomDiv.setOnPreferenceChangeListener(this);
        
        mFilled = (SeekBarPreference)  findPreference(PULSE_BLOCK);
        int filled = Settings.Secure.getIntForUser(getContentResolver(),
                    Settings.Secure.PULSE_FILLED_BLOCK_SIZE, 2,
                    UserHandle.USER_CURRENT);
        mFilled.setValue(filled / 1);
        mFilled.setOnPreferenceChangeListener(this);
        
        mEmpty = (SeekBarPreference) findPreference(EMPTY_BLOCK);
        int empty = Settings.Secure.getIntForUser(getContentResolver(),
                    Settings.Secure.PULSE_EMPTY_BLOCK_SIZE, 1,
                    UserHandle.USER_CURRENT);
        mEmpty.setValue(empty / 1);
        mEmpty.setOnPreferenceChangeListener(this);
        
        mFudge = (SeekBarPreference) findPreference(FUDGE_FACOR);
        int fudge = Settings.Secure.getIntForUser(getContentResolver(),
                    Settings.Secure.PULSE_CUSTOM_FUDGE_FACTOR, 2,
                    UserHandle.USER_CURRENT);
        mFudge.setValue(fudge / 1);
        mFudge.setOnPreferenceChangeListener(this);

        PreferenceCategory solidBarsCat = (PreferenceCategory)findPreference("pulse_2");
        solidBarsCat.setEnabled(renderMode == RENDER_STYLE_SOLID_LINES);

        mSolidFudge = (SeekBarPreference) findPreference(SOLID_FUDGE);
        int solidfudge = Settings.Secure.getIntForUser(getContentResolver(),
                    Settings.Secure.PULSE_SOLID_FUDGE_FACTOR, 2,
                    UserHandle.USER_CURRENT);
        mSolidFudge.setValue(solidfudge/ 1);
        mSolidFudge.setOnPreferenceChangeListener(this);

        mSolidSpeed =
                    (SeekBarPreference) findPreference(SOLID_LAVAMP_SPEED);
        int speed = Settings.Secure.getInt(getContentResolver(),
                    Settings.Secure.LAVAMP_SOLID_SPEED, 10000);
        mSolidSpeed.setValue(speed / 1);
        mSolidSpeed.setOnPreferenceChangeListener(this);

        mFadingSpeed =
                    (SeekBarPreference) findPreference(FADING_LAVAMP_SPEED);
        int fspeed = Settings.Secure.getInt(getContentResolver(),
                    Settings.Secure.FLING_PULSE_LAVALAMP_SPEED, 10000);
        mFadingSpeed.setValue(fspeed / 1);
        mFadingSpeed.setOnPreferenceChangeListener(this);

	setHasOptionsMenu(true);

    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
	ContentResolver mResolver = getActivity().getContentResolver();
        if (preference.equals(mRenderMode)) {
            int mode = Integer.valueOf((String) newValue);
            Settings.Secure.putIntForUser(getContentResolver(),
                    Settings.Secure.PULSE_RENDER_STYLE_URI, mode, UserHandle.USER_CURRENT);
            PreferenceCategory fadingBarsCat = (PreferenceCategory)findPreference("pulse_1");
            fadingBarsCat.setEnabled(mode == RENDER_STYLE_FADING_BARS);
            PreferenceCategory solidBarsCat = (PreferenceCategory)findPreference("pulse_2");
            solidBarsCat.setEnabled(mode == RENDER_STYLE_SOLID_LINES);
            return true;
        } else if (preference.equals(mShowPulse)) {
            boolean enabled = ((Boolean) newValue).booleanValue();
            Settings.Secure.putIntForUser(getContentResolver(),
                    Settings.Secure.FLING_PULSE_ENABLED, enabled ? 1 : 0, UserHandle.USER_CURRENT);
            return true;
        } else if (preference.equals(mPulseColor)) {
            int color = ((Integer) newValue).intValue();
            Settings.Secure.putIntForUser(getContentResolver(),
                    Settings.Secure.FLING_PULSE_COLOR, color, UserHandle.USER_CURRENT);
            return true;
        } else if (preference.equals(mLavaLampEnabled)) {
            boolean enabled = ((Boolean) newValue).booleanValue();
            Settings.Secure.putIntForUser(getContentResolver(),
                    Settings.Secure.FLING_PULSE_LAVALAMP_ENABLED, enabled ? 1 : 0,
                    UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mCustomDimen) {
                int val = (Integer) newValue;
                Settings.Secure.putInt(mResolver,
                        Settings.Secure.PULSE_CUSTOM_DIMEN, val * 1);
                return true;
	}  else if (preference == mCustomDiv) {
                int val = (Integer) newValue;
                Settings.Secure.putInt(mResolver,
                        Settings.Secure.PULSE_CUSTOM_DIV, val * 1);
                return true;
	} else if (preference == mFilled) {
                int val = (Integer) newValue;
                Settings.Secure.putInt(mResolver,
                        Settings.Secure.PULSE_FILLED_BLOCK_SIZE, val * 1);
                return true;
	}  else if (preference == mEmpty) {
                int val = (Integer) newValue;
                Settings.Secure.putInt(mResolver,
                        Settings.Secure.PULSE_EMPTY_BLOCK_SIZE, val * 1);
                return true;
	} else if (preference == mFudge) {
                   int val = (Integer) newValue;
                Settings.Secure.putInt(mResolver,
                        Settings.Secure.PULSE_CUSTOM_FUDGE_FACTOR, val * 1);
                return true;
	}  else if (preference == mSolidFudge) {
                   int val = (Integer) newValue;
                Settings.Secure.putInt(mResolver,
                        Settings.Secure.PULSE_SOLID_FUDGE_FACTOR, val * 1);
                return true;
	} else if (preference == mSolidSpeed) {
                int val = (Integer) newValue;
                Settings.Secure.putInt(mResolver,
                        Settings.Secure.LAVAMP_SOLID_SPEED, val * 1);
                return true;
        } else if (preference == mFadingSpeed) {
                int val = (Integer) newValue;
                Settings.Secure.putInt(mResolver,
                        Settings.Secure.FLING_PULSE_LAVALAMP_SPEED, val * 1);
                return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        return true;
    }

    @Override
     public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
         menu.add(0, MENU_RESET, 0, R.string.reset)
                 .setIcon(R.drawable.ic_pulse_reset)
                 .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
     }
 
     @Override
     public boolean onOptionsItemSelected(MenuItem item) {
         switch (item.getItemId()) {
             case MENU_RESET:
                 resetToDefault();
                 return true;
             default:
                 return super.onContextItemSelected(item);
         }
     }
 
     private void resetToDefault() {
         AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
         alertDialog.setTitle(R.string.reset);
         alertDialog.setMessage(R.string.dlg_reset_values_message);
         alertDialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
             public void onClick(DialogInterface dialog, int id) {
                 resetAllValues();
                 resetAllSettings();
             }
         });
         alertDialog.setNegativeButton(R.string.cancel, null);
         alertDialog.create().show();
     }
 
     private void resetAllValues() {
          mCustomDimen.setValue(14);
	  mCustomDiv.setValue(8);
	  mFilled.setValue(2);
	  mEmpty.setValue(1);
	  mFudge.setValue(2);
	  mSolidFudge.setValue(2);
	  mSolidSpeed.setValue(10*1000);
	  mFadingSpeed.setValue(10*1000);
     }
 
     private void resetAllSettings() {
         setProperVal(mCustomDimen, 14);
         setProperVal(mCustomDiv, 8);
         setProperVal(mFilled, 2);
         setProperVal(mEmpty, 1);
         setProperVal(mFudge, 2);
         setProperVal(mSolidFudge, 2);
         setProperVal(mSolidSpeed, 10*1000);
         setProperVal(mFadingSpeed, 10*1000);
	 mLavaLampEnabled.setChecked(true);
     }


   private void setProperVal(Preference preference, int val) {
            String mString = "";
            if (preference == mCustomDimen) {
                mString = Settings.Secure.PULSE_CUSTOM_DIMEN;
            } else if (preference == mCustomDiv) {
                mString = Settings.Secure.PULSE_CUSTOM_DIV;
            } else if (preference == mFilled) {
                mString = Settings.Secure.PULSE_FILLED_BLOCK_SIZE;
            } else if (preference == mEmpty) {
                mString = Settings.Secure.PULSE_EMPTY_BLOCK_SIZE;
            } else if (preference == mFudge) {
                mString = Settings.Secure.PULSE_CUSTOM_FUDGE_FACTOR;
            } else if (preference == mSolidFudge) {
                mString = Settings.Secure.PULSE_SOLID_FUDGE_FACTOR;
            } else if (preference == mSolidSpeed) {
                mString = Settings.Secure.LAVAMP_SOLID_SPEED;
            } else if (preference == mFadingSpeed) {
                mString = Settings.Secure.FLING_PULSE_LAVALAMP_SPEED;
            }
    
            Settings.Secure.putInt(getActivity().getContentResolver(), mString, val);
        }
    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.DONT_TRACK_ME_BRO;
    }
    
    	    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                                                                             boolean enabled) {
                     ArrayList<SearchIndexableResource> result =
                             new ArrayList<SearchIndexableResource>();
 
                     SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.pulse_settings;
                     result.add(sir);
 
                     return result;
                 }
 
                 @Override
                 public List<String> getNonIndexableKeys(Context context) {
                     final List<String> keys = new ArrayList<String>();
                     return keys;
                 }
         };
}
