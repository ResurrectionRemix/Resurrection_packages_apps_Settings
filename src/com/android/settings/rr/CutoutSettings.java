/*
 * Copyright (C) 2016-2020 crDroid Android Project
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

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.om.IOverlayManager;
import android.content.om.OverlayInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.SwitchPreference;
import androidx.preference.ListPreference;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.rr.Preferences.*;
import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import android.view.DisplayCutout;
import java.util.ArrayList;
import java.util.List;

public class CutoutSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String DISPLAY_CUTOUT = "sysui_display_cutout";
    private static final String DISPLAY_CUTOUT_MODE = "display_cutout_mode";
    private static final String STOCK_STATUSBAR = "stock_statusbar_in_hide";
    private static final String CUTOUT_STYLES = "overlay_display_devices";
    private static final String CUTOUT_BLACK = "sysui_display_cutout";
    private static final String CUTOUT_FULL = "display_cutout_force_fullscreen_settings";
    private static final String CUTOUT_CAT = "cutout_cat";
    private Context mContext;

    // Dark Variants
    private static final String[] CUTOUT_STYLES_ARRAY = {
        "com.android.internal.display.cutout.emulation.corner",//0
        "com.android.internal.display.cutout.emulation.double", //1
        "com.android.internal.display.cutout.emulation.narrow", //2
        "com.android.internal.display.cutout.emulation.tall", //3
        "com.android.internal.display.cutout.emulation.wide",//4
    };


    private Preference mDisplayCutout;
    private Preference mStockStatusbar;
    private ListPreference mImmersiveMode;
    private SecureSettingSwitchPreference mBlackbar;
    private Preference mFullscreen;
    private PreferenceCategory mCutoutCat;
    private IOverlayManager mOverlayManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.cutout);

        mContext = getActivity().getApplicationContext();

        final PreferenceScreen prefScreen = getPreferenceScreen();
        final Resources res = getResources();

        mImmersiveMode = (ListPreference) prefScreen.findPreference(DISPLAY_CUTOUT_MODE);
        mImmersiveMode.setOnPreferenceChangeListener(this);
        mBlackbar = (SecureSettingSwitchPreference) prefScreen.findPreference(CUTOUT_BLACK);
        mFullscreen = (Preference) prefScreen.findPreference(CUTOUT_FULL);
        mCutoutCat = (PreferenceCategory) prefScreen.findPreference(CUTOUT_CAT);

        int immersiveMode = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.DISPLAY_CUTOUT_MODE, 0);
        mDisplayCutout = (Preference) prefScreen.findPreference(DISPLAY_CUTOUT);
        mDisplayCutout.setEnabled(immersiveMode == 0);

        mStockStatusbar = (Preference) prefScreen.findPreference(STOCK_STATUSBAR);
        mStockStatusbar.setEnabled(immersiveMode == 2);
        mOverlayManager = IOverlayManager.Stub.asInterface(
                ServiceManager.getService(Context.OVERLAY_SERVICE));

        updatestyles();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mImmersiveMode) {
            int value = Integer.valueOf((String) newValue);
            mDisplayCutout.setEnabled(value == 0);
            mStockStatusbar.setEnabled(value == 2);
            return true;
        }
        return false;
    }

    public void updatestyles() {
        boolean hasNotch = mContext.getResources().getBoolean(
                org.lineageos.platform.internal.R.bool.config_haveNotch);
        if (hasNotch)  return;
        if (isEnabled()) {
            mCutoutCat.setEnabled(true);
        } else {
            mCutoutCat.setEnabled(false);
            mFooterPreferenceMixin.createFooterPreference().setTitle(R.string.cutout_disabled_summary);
        }
    }


    public boolean isEnabled() {
       return isTallsEnabled() || isWideEnabled() || isCornerEnabled() 
              || isDoubleEnabled() || isNarrowEnabled();
   }

    public boolean isTallsEnabled() {
        OverlayInfo info = null;
        try {
           String styles = CUTOUT_STYLES_ARRAY[3];
           info = mOverlayManager.getOverlayInfo(styles, UserHandle.USER_SYSTEM);
         } catch (RemoteException e) { /* Do nothing */ }
           if (info != null && info.isEnabled()) {
              return true;
        }
        return false;
    }

    public boolean isWideEnabled() {
        OverlayInfo info = null;
        try {
           String styles = CUTOUT_STYLES_ARRAY[4];
           info = mOverlayManager.getOverlayInfo(styles, UserHandle.USER_SYSTEM);
         } catch (RemoteException e) { /* Do nothing */ }
           if (info != null && info.isEnabled()) {
              return true;
        }
        return false;
    }

    public boolean isCornerEnabled() {
        OverlayInfo info = null;
        try {
           String styles = CUTOUT_STYLES_ARRAY[0];
           info = mOverlayManager.getOverlayInfo(styles, UserHandle.USER_SYSTEM);
         } catch (RemoteException e) { /* Do nothing */ }
           if (info != null && info.isEnabled()) {
              return true;
        }
        return false;
    }

    public boolean isDoubleEnabled() {
        OverlayInfo info = null;
        try {
           String styles = CUTOUT_STYLES_ARRAY[1];
           info = mOverlayManager.getOverlayInfo(styles, UserHandle.USER_SYSTEM);
         } catch (RemoteException e) { /* Do nothing */ }
           if (info != null && info.isEnabled()) {
              return true;
        }
        return false;
    }

    public boolean isNarrowEnabled() {
        OverlayInfo info = null;
        try {
           String styles = CUTOUT_STYLES_ARRAY[2];
           info = mOverlayManager.getOverlayInfo(styles, UserHandle.USER_SYSTEM);
         } catch (RemoteException e) { /* Do nothing */ }
           if (info != null && info.isEnabled()) {
              return true;
        }
        return false;
    }

    public static void reset(Context mContext) {
        ContentResolver resolver = mContext.getContentResolver();
        Settings.Secure.putIntForUser(resolver,
                Settings.Secure.SYSUI_DISPLAY_CUTOUT, 1, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.DISPLAY_CUTOUT_MODE, 0, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.STOCK_STATUSBAR_IN_HIDE, 1, UserHandle.USER_CURRENT);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.RESURRECTED;
    }
}
