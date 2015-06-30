/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.settings;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import com.android.internal.widget.LockPatternUtils;
import com.android.setupwizard.navigationbar.SetupWizardNavBar;

/**
 * Setup Wizard's version of ChooseLockPattern screen. It inherits the logic and basic structure
 * from ChooseLockPattern class, and should remain similar to that behaviorally. This class should
 * only overload base methods for minor theme and behavior differences specific to Setup Wizard.
 * Other changes should be done to ChooseLockPattern class instead and let this class inherit
 * those changes.
 */
public class SetupChooseLockPatternSize extends ChooseLockPatternSize
        implements SetupWizardNavBar.NavigationBarListener {

    @Override
    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent());
        modIntent.putExtra(EXTRA_SHOW_FRAGMENT, SetupChooseLockPatternSizeFragment.class.getName());
        return modIntent;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return SetupChooseLockPatternSizeFragment.class.getName().equals(fragmentName);
    }

    @Override
    protected void onApplyThemeResource(Resources.Theme theme, int resid, boolean first) {
        resid = SetupWizardUtils.getTheme(getIntent(), resid);
        super.onApplyThemeResource(theme, resid, first);
    }

    @Override
    public void onNavigationBarCreated(SetupWizardNavBar bar) {
        SetupWizardUtils.setImmersiveMode(this, bar);
        bar.getNextButton().setVisibility(View.INVISIBLE);
    }

    @Override
    public void onNavigateBack() {
        onBackPressed();
    }

    @Override
    public void onNavigateNext() {
    }

    public static class SetupChooseLockPatternSizeFragment extends ChooseLockPatternSizeFragment {

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            final View view = inflater.inflate(R.layout.setup_preference, container, false);
            ListView list = (ListView) view.findViewById(android.R.id.list);
            View title = view.findViewById(R.id.title);
            if (title == null) {
                final View header = inflater.inflate(R.layout.setup_wizard_header_lock, list, false);
                list.addHeaderView(header, null, false);
                final TextView details = (TextView) header.findViewById(R.id.details);
                if (details != null) {
                    details.setVisibility(View.VISIBLE);
                    details.setText(R.string.setup_lock_pattern_size_details);
                }
            }
            return view;
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            SetupWizardUtils.setIllustration(getActivity(),
                    R.drawable.setup_illustration_lock_screen_generic);
            SetupWizardUtils.setHeaderText(getActivity(), R.string.setup_lock_pattern_size_title);
        }

        @Override
        public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
                Preference preference) {
            final String key = preference.getKey();

            byte patternSize;
            if (LOCK_PATTERN_SIZE_4.equals(key)) {
                patternSize = 4;
            } else if (LOCK_PATTERN_SIZE_5.equals(key)) {
                patternSize = 5;
            } else if (LOCK_PATTERN_SIZE_6.equals(key)) {
                patternSize = 6;
            } else {
                patternSize = 3;
            }

            final boolean isFallback = getActivity().getIntent()
                    .getBooleanExtra(LockPatternUtils.LOCKSCREEN_BIOMETRIC_WEAK_FALLBACK, false);
            boolean isRequiredForDecrypt = getActivity().getIntent()
                    .getBooleanExtra(EncryptionInterstitial.EXTRA_REQUIRE_PASSWORD, true);

            Intent intent = new Intent(getActivity(), SetupChooseLockPattern.class);
            intent.putExtra("pattern_size", patternSize);
            intent.putExtra("key_lock_method", "pattern");
            intent.putExtra("confirm_credentials", false);
            intent.putExtra(LockPatternUtils.LOCKSCREEN_BIOMETRIC_WEAK_FALLBACK,
                    isFallback);
            intent.putExtra(LockPatternUtils.LOCKSCREEN_FINGERPRINT_FALLBACK, isFallback);
            intent.putExtra(EncryptionInterstitial.EXTRA_REQUIRE_PASSWORD, isRequiredForDecrypt);
            intent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
            SetupWizardUtils.copySetupExtras(getActivity().getIntent(), intent);
            startActivity(intent);

            finish();
            return true;
        }
    }
}
