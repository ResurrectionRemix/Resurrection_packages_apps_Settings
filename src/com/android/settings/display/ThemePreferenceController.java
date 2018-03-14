/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.android.settings.display;

import android.content.Context;
import android.content.om.IOverlayManager;
import android.content.om.OverlayInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.support.annotation.VisibleForTesting;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.text.TextUtils;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.core.instrumentation.MetricsFeatureProvider;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.AbstractPreferenceController;

import libcore.util.Objects;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

import static com.android.internal.logging.nano.MetricsProto.MetricsEvent.ACTION_THEME;

public class ThemePreferenceController extends AbstractPreferenceController implements
        PreferenceControllerMixin, Preference.OnPreferenceChangeListener {

    private static final String KEY_THEME = "theme";
    private static final String KEY_THEMES_DISABLED = "default";

    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private final OverlayManager mOverlayService;
    private final PackageManager mPackageManager;

    public ThemePreferenceController(Context context) {
        this(context, ServiceManager.getService(Context.OVERLAY_SERVICE) != null
                ? new OverlayManager() : null);
    }

    @VisibleForTesting
    ThemePreferenceController(Context context, OverlayManager overlayManager) {
        super(context);
        mOverlayService = overlayManager;
        mPackageManager = context.getPackageManager();
        mMetricsFeatureProvider = FeatureFactory.getFactory(context).getMetricsFeatureProvider();
    }

    @Override
    public String getPreferenceKey() {
        return KEY_THEME;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (KEY_THEME.equals(preference.getKey())) {
            mMetricsFeatureProvider.action(mContext, ACTION_THEME);
        }
        return false;
    }

    @Override
    public void updateState(Preference preference) {
        ListPreference pref = (ListPreference) preference;
        List<String> pkgs = new ArrayList<>(Arrays.asList(getAvailableThemes()));
        List<CharSequence> labels = new ArrayList<>(pkgs.size());
        for (String pkg : pkgs) {
            CharSequence label = null;
            try {
                label = mPackageManager.getApplicationInfo(pkg, 0).loadLabel(mPackageManager);
            } catch (NameNotFoundException e) {
                label = pkg;
            }
            labels.add(label);
        }
        labels.add(mContext.getString(R.string.default_theme));
        pkgs.add(KEY_THEMES_DISABLED);

        pref.setEntries(labels.toArray(new CharSequence[labels.size()]));
        pref.setEntryValues(pkgs.toArray(new String[pkgs.size()]));
        String theme = getCurrentTheme();
        CharSequence themeLabel = null;

        if (theme != null) {
            int i = 0;
            for (String pkg : pkgs) {
                if (TextUtils.equals(pkg, theme)) {
                    themeLabel = labels.get(i);
                    break;
                }
                i++;
            }
        }

        if (TextUtils.isEmpty(themeLabel)) {
            themeLabel = mContext.getString(R.string.default_theme);
            theme = KEY_THEMES_DISABLED;
        }

        pref.setSummary(themeLabel);
        pref.setValue(theme);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String current = getTheme();
        if (Objects.equal(newValue, current)) {
            return true;
        }
        try {
            if (newValue.equals(KEY_THEMES_DISABLED)) {
                disableTheme();
            } else {
                mOverlayService.setEnabledExclusive((String) newValue, true, UserHandle.myUserId());
            }
        } catch (RemoteException e) {
            return false;
        }
        return true;
    }

    private boolean isChangeableOverlay(String packageName) {
        try {
            PackageInfo pi = mPackageManager.getPackageInfo(packageName, 0);
            return pi != null && (pi.overlayFlags & PackageInfo.FLAG_OVERLAY_STATIC) == 0;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private String getTheme() {
        try {
            List<OverlayInfo> infos = mOverlayService.getOverlayInfosForTarget("android",
                    UserHandle.myUserId());
            for (int i = 0, size = infos.size(); i < size; i++) {
                if (infos.get(i).isEnabled() &&
                        isChangeableOverlay(infos.get(i).packageName)) {
                    return infos.get(i).packageName;
                }
            }
        } catch (RemoteException e) {
        }
        return null;
    }

    private void disableTheme() {
        try {
            List<OverlayInfo> infos = mOverlayService.getOverlayInfosForTarget("android",
                    UserHandle.myUserId());
            for (int i = 0, size = infos.size(); i < size; i++) {
                if (infos.get(i).isEnabled() &&
                        isChangeableOverlay(infos.get(i).packageName)) {
                    mOverlayService.setEnabled(infos.get(i).packageName, false, UserHandle.myUserId());
                }
            }
        } catch (RemoteException e) {
        }
    }

    @Override
    public boolean isAvailable() {
        if (mOverlayService == null) return false;
        String[] themes = getAvailableThemes();
        return themes != null && themes.length >= 1;
    }


    @VisibleForTesting
    String getCurrentTheme() {
        return getTheme();
    }

    @VisibleForTesting
    String[] getAvailableThemes() {
        try {
            List<OverlayInfo> infos = mOverlayService.getOverlayInfosForTarget("android",
                    UserHandle.myUserId());
            List<String> pkgs = new ArrayList(infos.size());
            for (int i = 0, size = infos.size(); i < size; i++) {
                if (isChangeableOverlay(infos.get(i).packageName)) {
                    pkgs.add(infos.get(i).packageName);
                }
            }
            return pkgs.toArray(new String[pkgs.size()]);
        } catch (RemoteException e) {
        }
        return new String[0];
    }

    public static class OverlayManager {
        private final IOverlayManager mService;

        public OverlayManager() {
            mService = IOverlayManager.Stub.asInterface(
                    ServiceManager.getService(Context.OVERLAY_SERVICE));
        }

        public void setEnabledExclusive(String pkg, boolean enabled, int userId)
                throws RemoteException {
            mService.setEnabledExclusive(pkg, enabled, userId);
        }

        public void setEnabled(String pkg, boolean enabled, int userId)
                throws RemoteException {
            mService.setEnabled(pkg, enabled, userId);
        }

        public List<OverlayInfo> getOverlayInfosForTarget(String target, int userId)
                throws RemoteException {
            return mService.getOverlayInfosForTarget(target, userId);
        }
    }
}
