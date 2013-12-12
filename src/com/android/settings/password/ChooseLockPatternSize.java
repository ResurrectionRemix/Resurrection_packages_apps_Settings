/*
 * Copyright (C) 2012-2013 The CyanogenMod Project
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

package com.android.settings.password;

import android.content.Intent;
import android.content.res.Resources.Theme;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.EncryptionInterstitial;
import com.android.settings.R;
import com.android.settings.SetupWizardUtils;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.utils.SettingsDividerItemDecoration;
import com.android.setupwizardlib.GlifPreferenceLayout;

import org.lineageos.internal.logging.LineageMetricsLogger;

public class ChooseLockPatternSize extends PreferenceActivity {

    @Override
    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent());
        modIntent.putExtra(EXTRA_SHOW_FRAGMENT, ChooseLockPatternSizeFragment.class.getName());
        modIntent.putExtra(EXTRA_NO_HEADERS, true);
        return modIntent;
    }

    @Override
    protected void onApplyThemeResource(Theme theme, int resid, boolean first) {
        resid = SetupWizardUtils.getTheme(getIntent());
        super.onApplyThemeResource(theme, resid, first);
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        if (ChooseLockPatternSizeFragment.class.getName().equals(fragmentName)) return true;
        return false;
    }

    public static class ChooseLockPatternSizeFragment extends SettingsPreferenceFragment {
        private ChooseLockSettingsHelper mChooseLockSettingsHelper;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mChooseLockSettingsHelper = new ChooseLockSettingsHelper(this.getActivity());
            if (!(getActivity() instanceof ChooseLockPatternSize)) {
                throw new SecurityException("Fragment contained in wrong activity");
            }
            addPreferencesFromResource(R.xml.security_settings_pattern_size);
            setHeaderView(R.layout.choose_lock_pattern_size_header);
        }

        @Override
        public boolean onPreferenceTreeClick(Preference preference) {
            final String key = preference.getKey();

            byte patternSize;
            if ("lock_pattern_size_4".equals(key)) {
                patternSize = 4;
            } else if ("lock_pattern_size_5".equals(key)) {
                patternSize = 5;
            } else if ("lock_pattern_size_6".equals(key)) {
                patternSize = 6;
            } else {
                patternSize = 3;
            }

            final boolean isFallback = getActivity().getIntent()
                .getBooleanExtra(LockPatternUtils.LOCKSCREEN_BIOMETRIC_WEAK_FALLBACK, false);

            Intent intent = new Intent(getActivity(), ChooseLockPattern.class);
            intent.putExtra("pattern_size", patternSize);
            intent.putExtra("key_lock_method", "pattern");
            intent.putExtra("confirm_credentials", false);
            intent.putExtra(LockPatternUtils.LOCKSCREEN_BIOMETRIC_WEAK_FALLBACK,
                    isFallback);

            Intent originatingIntent = getActivity().getIntent();
            // Forward the challenge extras if available in originating intent.
            if (originatingIntent.hasExtra(ChooseLockSettingsHelper.EXTRA_KEY_HAS_CHALLENGE)) {
                intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_HAS_CHALLENGE,
                        originatingIntent.getBooleanExtra(
                                ChooseLockSettingsHelper.EXTRA_KEY_HAS_CHALLENGE, false));

                intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE,
                        originatingIntent.getLongExtra(
                                ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE, 0));
            }
            // Forward the Encryption interstitial required password selection
            if (originatingIntent.hasExtra(EncryptionInterstitial.EXTRA_REQUIRE_PASSWORD)) {
                intent.putExtra(EncryptionInterstitial.EXTRA_REQUIRE_PASSWORD, originatingIntent
                        .getBooleanExtra(EncryptionInterstitial.EXTRA_REQUIRE_PASSWORD, true));
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
            startActivity(intent);

            finish();
            return true;
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            GlifPreferenceLayout layout = (GlifPreferenceLayout) view;
            layout.setDividerItemDecoration(new SettingsDividerItemDecoration(getContext()));

            layout.setIcon(getContext().getDrawable(R.drawable.ic_lock));

            // Use the dividers in SetupWizardRecyclerLayout. Suppress the dividers in
            // PreferenceFragment.
            setDivider(null);
        }

        @Override
        public RecyclerView onCreateRecyclerView(LayoutInflater inflater, ViewGroup parent,
                Bundle savedInstanceState) {
            GlifPreferenceLayout layout = (GlifPreferenceLayout) parent;
            return layout.onCreateRecyclerView(inflater, parent, savedInstanceState);
        }

        @Override
        public int getMetricsCategory() {
            return LineageMetricsLogger.CHOOSE_LOCK_PATTERN_SIZE;
        }
    }
}
