/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.settings.display;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnResume;

import android.app.Fragment;
import android.content.Context;
import android.content.FontInfo;
import android.content.IFontService;
import android.content.pm.PackageManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.Preference.OnPreferenceClickListener;
import android.text.TextUtils;
import android.util.Log;
 public class FontPickerPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, LifecycleObserver, OnResume {
    private static final String TAG = "FontPickerPreferenceController";
    private static final String KEY_FONT_PICKER_FRAGMENT_PREF = "custom_font";
    private static final String SUBS_PACKAGE = "projekt.substratum";
     private FontDialogPreference mFontPreference;
    private IFontService mFontService;
     public FontPickerPreferenceController(Context context, Lifecycle lifecycle, Fragment parent) {
        super(context);
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
        mFontService = IFontService.Stub.asInterface(
                ServiceManager.getService("dufont"));
    }
     @Override
    public void onResume() {
        if (mFontPreference == null) {
            return;
        }
        if (!isPackageInstalled(SUBS_PACKAGE, mContext)) {
            mFontPreference.setSummary(getCurrentFontInfo().fontName.replace("_", " "));
        } else {
            mFontPreference.setSummary(mContext.getString(
                    com.android.settings.R.string.disable_fonts_installed_title));
        }
    }
     @Override
    public void displayPreference(PreferenceScreen screen) {
        mFontPreference = (FontDialogPreference) screen.findPreference(KEY_FONT_PICKER_FRAGMENT_PREF);
        if (!isPackageInstalled(SUBS_PACKAGE, mContext)) {
            mFontPreference.setEnabled(true);
        } else {
            mFontPreference.setEnabled(false);
        }
    }
     @Override
    public boolean isAvailable() {
        return true;
    }
     @Override
    public String getPreferenceKey() {
        return KEY_FONT_PICKER_FRAGMENT_PREF;
    }
     private FontInfo getCurrentFontInfo() {
        try {
            return mFontService.getFontInfo();
        } catch (RemoteException e) {
            return FontInfo.getDefaultFontInfo();
        }
    }
     private boolean isPackageInstalled(String package_name, Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            pm.getPackageInfo(package_name, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
