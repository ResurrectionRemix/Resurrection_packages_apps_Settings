package com.mrapocalypse.screwdshop.frags;

import android.app.Activity;
import android.content.Context;
import android.content.ContentResolver;
import android.os.Bundle;
import android.app.Fragment;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.ListPreference;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.provider.Settings;


import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import com.android.internal.logging.MetricsProto.MetricsEvent;

public class AnimationSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.rr_animation_settings);

    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {

        return false;
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.APPLICATION;
    }

}
