/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.homepage;

import static com.android.settings.search.actionbar.SearchMenuController.NEED_SEARCH_ICON_IN_ACTION_BAR;
import static com.android.settingslib.search.SearchIndexable.MOBILE;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.provider.SearchIndexableResource;
import android.provider.Settings;

import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceFragmentCompat;

import com.android.settings.R;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.support.SupportPreferenceController;
import com.android.settingslib.core.instrumentation.Instrumentable;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.widget.AdaptiveIcon;

import java.util.Arrays;
import java.util.List;

@SearchIndexable(forTarget = MOBILE)
public class TopLevelSettings extends DashboardFragment implements
        PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    private static final String TAG = "TopLevelSettings";

    private int mIconStyle;
    private int mNormalColor;
    private int mAccentColor;

    public TopLevelSettings() {
        final Bundle args = new Bundle();
        // Disable the search icon because this page uses a full search view in actionbar.
        args.putBoolean(NEED_SEARCH_ICON_IN_ACTION_BAR, false);
        setArguments(args);
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.top_level_settings;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DASHBOARD_SUMMARY;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        use(SupportPreferenceController.class).setActivity(getActivity());
    }

    @Override
    public int getHelpResource() {
        // Disable the help icon because this page uses a full search view in actionbar.
        return 0;
    }

    @Override
    public Fragment getCallbackFragment() {
        return this;
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref) {
        new SubSettingLauncher(getActivity())
                .setDestination(pref.getFragment())
                .setArguments(pref.getExtras())
                .setSourceMetricsCategory(caller instanceof Instrumentable
                        ? ((Instrumentable) caller).getMetricsCategory()
                        : Instrumentable.METRICS_CATEGORY_UNKNOWN)
                .setTitleRes(-1)
                .launch();
        return true;
    }

    @Override
    protected boolean shouldForceRoundedIcon() {
        return getContext().getResources()
                .getBoolean(R.bool.config_force_rounded_icon_TopLevelSettings);
    }

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(
                        Context context, boolean enabled) {
                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.top_level_settings;
                    return Arrays.asList(sir);
                }

                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    // Never searchable, all entries in this page are already indexed elsewhere.
                    return false;
                }
            };

    @Override
    public void onResume() {
        super.onResume();
        updateTheme();
    }

    private void updateTheme() {
        int[] attrs = new int[] {
            android.R.attr.colorControlNormal,
            android.R.attr.colorAccent,
        };
        TypedArray ta = getContext().getTheme().obtainStyledAttributes(attrs);
        mNormalColor = ta.getColor(0, 0xff808080);
        mAccentColor = ta.getColor(1, 0xff808080);
        ta.recycle();

        mIconStyle = Settings.System.getInt(getContext().getContentResolver(),
                Settings.System.THEMING_SETTINGS_DASHBOARD_ICONS, 0);
        themePreferences(getPreferenceScreen());
    }

    private void themePreferences(PreferenceGroup prefGroup) {
        themePreference(prefGroup);
        for (int i = 0; i < prefGroup.getPreferenceCount(); i++) {
            Preference pref = prefGroup.getPreference(i);
            if (pref instanceof PreferenceGroup) {
                themePreferences(prefGroup);
            } else {
                themePreference(pref);
            }
        }
    }

    private void themePreference(Preference pref) {
        Drawable icon = pref.getIcon();
        if (icon != null) {
            if (icon instanceof AdaptiveIcon) {
                AdaptiveIcon aIcon = (AdaptiveIcon) icon;
                // Clear colors from previous calls
                aIcon.resetColorsAicp();
                switch (mIconStyle) {
                    case 1:
                        aIcon.setBackgroundColorAicp(mAccentColor);
                        break;
                    case 2:
                        aIcon.setForegroundColorAicp(mNormalColor);
                        aIcon.setBackgroundColorAicp(0);
                        break;
                    case 3:
                        aIcon.setForegroundColorAicp(mAccentColor);
                        aIcon.setBackgroundColorAicp(0);
                        break;
                }
            } else if (icon instanceof LayerDrawable) {
                LayerDrawable lIcon = (LayerDrawable) icon;
                if (lIcon.getNumberOfLayers() == 2) {
                    Drawable fg = lIcon.getDrawable(1);
                    Drawable bg = lIcon.getDrawable(0);
                    // Clear tints from previous calls
                    bg.setTintList(null);
                    fg.setTintList(null);
                    switch (mIconStyle) {
                        case 1:
                            bg.setTint(mAccentColor);
                            break;
                        case 2:
                            fg.setTint(mNormalColor);
                            bg.setTint(0);
                            break;
                        case 3:
                            fg.setTint(mAccentColor);
                            bg.setTint(0);
                            break;
                    }
                }
            }
        }
    }
}
