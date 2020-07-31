/*Copyright (C) 2015 The ResurrectionRemix Project
     Licensed under the Apache License, Version 2.0 (the "License");
     you may not use this file except in compliance with the License.
     You may obtain a copy of the License at

          http://www.apache.org/licenses/LICENSE-2.0

     Unless required by applicable law or agreed to in writing, software
     distributed under the License is distributed on an "AS IS" BASIS,
     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     See the License for the specific language governing permissions and
     limitations under the License.
*/
package com.android.settings.rr;

import static android.os.UserHandle.USER_SYSTEM;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ActionBar;
import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.app.Fragment;
import android.app.UiModeManager;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Context;
import android.content.om.IOverlayManager;
import android.content.om.OverlayInfo;
import android.graphics.Color;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceCategory;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.SwitchPreference;
import android.provider.SearchIndexableResource;
import android.os.UserHandle;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.ServiceManager;
import android.os.Process;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.rr.Preferences.*;
import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settingslib.search.SearchIndexable;
import lineageos.hardware.LineageHardwareManager;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import lineageos.preference.LineageSystemSettingSwitchPreference;
import android.provider.Settings;
import java.util.Objects;
import com.android.settingslib.utils.ThreadUtils;
import com.android.internal.statusbar.ThemeAccentUtils;
import com.android.internal.util.rr.RRUtils;

@SearchIndexable
public class ThemeSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, Indexable {
    private static final String TAG = "ThemeSettings";
    private static final String PREF_THEME_SWITCH = "theme_switch";
    private static final String RESET = "reset";
    private IOverlayManager mOverlayService;
    private UiModeManager mUiModeManager;
    private LineageSystemSettingSwitchPreference mWakeProx;
    private LineageSystemSettingSwitchPreference mHighTouch;
    private ListPreference mThemeSwitch;
    private Preference mReset;
    protected Context mContext;

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.RESURRECTED;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.rr_theme_settings);
        mContext = getActivity();
        mReset = (Preference) findPreference(RESET);
        mThemeSwitch = (ListPreference) findPreference(PREF_THEME_SWITCH);
        mThemeSwitch.setOnPreferenceChangeListener(this);
        int systemTheme = Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.SYSTEM_THEME, 1, UserHandle.USER_CURRENT);
        int valueIndex = mThemeSwitch.findIndexOfValue(String.valueOf(systemTheme));
        mThemeSwitch.setValueIndex(valueIndex);
        mThemeSwitch.setSummary(mThemeSwitch.getEntry());
        mThemeSwitch.setOnPreferenceChangeListener(this);
        mFooterPreferenceMixin.createFooterPreference().setTitle(R.string.rr_themes_tutorial);
    }

    @Override
    public boolean onPreferenceTreeClick(final Preference preference) {
        return true;
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mThemeSwitch) {
            int systemThemeValue = Integer.valueOf((String) newValue);
            Settings.System.putIntForUser(mContext.getContentResolver(),
                    Settings.System.SYSTEM_THEME, systemThemeValue, UserHandle.USER_CURRENT);
             mThemeSwitch.setSummary(mThemeSwitch.getEntries()[systemThemeValue]);
             if (systemThemeValue == 7) {
                 Process.killProcess(Process.myPid());
             }

            return true;
        }
        return false;
    }

     /**
     * For Search.
     */
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        new BaseSearchIndexProvider() {
            @Override
            public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean enabled) {
                ArrayList<SearchIndexableResource> result =
                    new ArrayList<SearchIndexableResource>();
                    SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.rr_theme_settings;
                    result.add(sir);
                    return result;
            }

            @Override
            public List<String> getNonIndexableKeys(Context context) {
                List<String> keys = super.getNonIndexableKeys(context);
                return keys;
            }
        };
}
