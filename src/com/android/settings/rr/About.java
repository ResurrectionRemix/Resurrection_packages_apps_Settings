/*
 * About.java
 * 
 * Copyright 2014 westcrip <westcrip@westcrip-altankrk>
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 * 
 * 
 */
package com.android.settings.rr;

import android.app.Activity;
import android.app.ActivityManagerNative;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.DialogInterface;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.ListPreference;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.PreferenceGroup;
import android.provider.MediaStore;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import android.view.IWindowManager;
import android.view.Display;
import android.view.Window;
import android.widget.Toast;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.R;
import com.android.settings.Utils;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;  
public class About extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
			
public static final String TAG = "About";
    
private static final String RR_ROM_SHARE = "share";
    
    Preference mSiteUrl;
    Preference mForumUrl;
    Preference mSourceUrl;
    Preference mFacebookUrl;
    Preference mGoogleUrl;
    Preference mDonateUrl;
    Preference mPitchBlackUrl;
    Preference mTwitterUrl;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.about_rom);
        PreferenceScreen prefSet = getPreferenceScreen();
        ContentResolver resolver = getContentResolver();
        mPitchBlackUrl = findPreference("rr_pb");
        mTwitterUrl = findPreference("rr_twitter");
        mSiteUrl = findPreference("rr_website");
        mForumUrl = findPreference("rr_forum");
        mSourceUrl = findPreference("rr_source");
        mFacebookUrl = findPreference("rr_facebook");
        mGoogleUrl = findPreference("rr_google_plus");
        mDonateUrl = findPreference("rr_donate");
        PreferenceGroup devsGroup = (PreferenceGroup) findPreference("devs");
        ArrayList<Preference> devs = new ArrayList<Preference>();
        for (int i = 0; i < devsGroup.getPreferenceCount(); i++) {
            devs.add(devsGroup.getPreference(i));
        }
        devsGroup.removeAll();
        devsGroup.setOrderingAsAdded(false);
        Collections.shuffle(devs);
        for(int i = 0; i < devs.size(); i++) {
            Preference p = devs.get(i);
            p.setOrder(i);

            devsGroup.addPreference(p);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        return false;
    }

    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference == mSiteUrl) {
            launchUrl("http://resurrectionremix.com/");
        } else if (preference == mPitchBlackUrl) {
            launchUrl("https://play.google.com/store/apps/details?id=pitchblack.origins.westcrip");
        } else if (preference == mTwitterUrl) {
            launchUrl("https://twitter.com/rrosofficial");
        } else if (preference == mForumUrl) {
            launchUrl("http://forum.resurrectionremix.com/");
        } else if (preference == mSourceUrl) {
            launchUrl("https://github.com/ResurrectionRemix");
        } else if (preference == mFacebookUrl) {
            launchUrl("https://www.facebook.com/resurrectionremixrom");
        } else if (preference == mGoogleUrl) {
            launchUrl("https://plus.google.com/u/0/communities/109352646351468373340");
        } else if (preference == mDonateUrl) {
            launchUrl("http://forum.xda-developers.com/donatetome.php?u=4144763");
        } else if (preference.getKey().equals(RR_ROM_SHARE)) {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, String.format(
                    getActivity().getString(R.string.share_message)));
            startActivity(Intent.createChooser(intent, getActivity().getString(R.string.share_chooser_title)));
            }  else {
                // If not handled, let preferences handle it.
                return super.onPreferenceTreeClick(preference);
   			}
         return true; 
    }
    private void launchUrl(String url) {
        try {
            Uri uriUrl = Uri.parse(url);
            Intent donate = new Intent(Intent.ACTION_VIEW, uriUrl);
            getActivity().startActivity(donate);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getActivity(), getActivity().getString(R.string.browser_not_found_toast), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.RESURRECTED;
     }
}
