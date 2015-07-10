/*
 * Copyright (C) 2015 The CyanogenMod Project
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
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import com.android.internal.widget.LockPatternUtils;
import com.android.setupwizard.navigationbar.SetupWizardNavBar;

/**
 * Setup Wizard's version of ManageFingerprints screen. It inherits the logic and basic structure
 * from ManageFingerprints class, and should remain similar to that behaviorally. This class should
 * only overload base methods for minor theme and behavior differences specific to Setup Wizard.
 * Other changes should be done to ManageFingerprints class instead and let this class inherit
 * those changes.
 */
public class SetupManageFingerprints extends ManageFingerprints
        implements SetupWizardNavBar.NavigationBarListener {

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return SetupManageFingerprintsFragment.class.getName().equals(fragmentName);
    }

    @Override
    /* package */ Class<? extends Fragment> getFragmentClass() {
        return SetupManageFingerprintsFragment.class;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onApplyThemeResource(Resources.Theme theme, int resid, boolean first) {
        resid = R.style.SetupWizardThemeFingerprint;
        super.onApplyThemeResource(theme, resid, first);
    }

    @Override
    public void onNavigationBarCreated(SetupWizardNavBar bar) {
        SetupWizardUtils.setImmersiveMode(this, bar);
    }

    @Override
    public void onNavigateBack() {
        onBackPressed();
    }

    @Override
    public void onNavigateNext() {
        setResult(RESULT_OK);
        finish();
    }

    @Override
    protected Intent createFallbackIntent() {
        Intent fallBackIntent = SetupEnrollFingerprint.createIntent(this);
        fallBackIntent.putExtra(LockPatternUtils.LOCKSCREEN_BIOMETRIC_WEAK_FALLBACK, true);
        fallBackIntent.putExtra(LockPatternUtils.LOCKSCREEN_FINGERPRINT_FALLBACK, true);
        fallBackIntent.putExtra(CONFIRM_CREDENTIALS, false);
        SetupWizardUtils.copySetupExtras(getIntent(), fallBackIntent);
        return fallBackIntent;
    }

    public static class SetupManageFingerprintsFragment extends FingerprintListFragment {
        @Override
        protected int getLayoutResource() {
            return R.layout.setup_finger_list;
        }

        @Override
        protected Intent getEnrollmentIntent() {
            Intent intent = SetupEnrollFingerprint.createIntent(getActivity());
            intent.putExtra(LockPatternUtils.LOCKSCREEN_FINGERPRINT_FALLBACK, false);
            SetupWizardUtils.copySetupExtras(getActivity().getIntent(), intent);
            return intent;
        }
    }
}
