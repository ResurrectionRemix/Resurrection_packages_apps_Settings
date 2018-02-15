/*
 * Copyright (C) 2017 AICP
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


package com.android.settings.rr.animation;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.support.v7.preference.Preference;
import android.support.v14.preference.SwitchPreference;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.support.v7.preference.ListPreference;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.rr.Preferences.CustomSeekBarPreference;
import com.android.settings.R;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;


import com.android.settings.rr.Preferences.MasterSwitchPreference;
import com.android.internal.logging.nano.MetricsProto;

public class ListViewAnimation extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    private static final String KEY_LISTVIEW_ANIMATION = "listview_animation";
    private static final String KEY_LISTVIEW_INTERPOLATOR = "listview_interpolator";

    private ListPreference mListViewAnimation;
    private ListPreference mListViewInterpolator;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.rr_list_animation);
        PreferenceScreen prefSet = getPreferenceScreen();

        mListViewAnimation = (ListPreference) prefSet.findPreference(KEY_LISTVIEW_ANIMATION);
              int listviewanimation = Settings.System.getInt(getContentResolver(),
                      Settings.System.LISTVIEW_ANIMATION, 0);
              mListViewAnimation.setValue(String.valueOf(listviewanimation));
              mListViewAnimation.setSummary(mListViewAnimation.getEntry());
              mListViewAnimation.setOnPreferenceChangeListener(this);
        
              mListViewInterpolator = (ListPreference) prefSet.findPreference(KEY_LISTVIEW_INTERPOLATOR);
              int listviewinterpolator = Settings.System.getInt(getContentResolver(),
                      Settings.System.LISTVIEW_INTERPOLATOR, 0);
              mListViewInterpolator.setValue(String.valueOf(listviewinterpolator));
              mListViewInterpolator.setSummary(mListViewInterpolator.getEntry());
              mListViewInterpolator.setOnPreferenceChangeListener(this);
              mListViewInterpolator.setEnabled(listviewanimation > 0);

    }
       
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference == mListViewAnimation) {
             int value = Integer.parseInt((String) objValue);
             int index = mListViewAnimation.findIndexOfValue((String) objValue);
             Settings.System.putInt(getContentResolver(),
                     Settings.System.LISTVIEW_ANIMATION, value);
             mListViewAnimation.setSummary(mListViewAnimation.getEntries()[index]);
             mListViewInterpolator.setEnabled(value > 0);
             return true;
         } else if (preference == mListViewInterpolator) {
             int value = Integer.parseInt((String) objValue);
             int index = mListViewInterpolator.findIndexOfValue((String) objValue);
             Settings.System.putInt(getContentResolver(),
                     Settings.System.LISTVIEW_INTERPOLATOR, value);
             mListViewInterpolator.setSummary(mListViewInterpolator.getEntries()[index]);
             return true;
      }
        return false;
    }

    
    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.RESURRECTED;
    }

}
