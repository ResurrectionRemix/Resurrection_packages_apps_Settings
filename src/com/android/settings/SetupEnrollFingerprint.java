/*
 * Copyright (C) 2015 The Android Open Source Project
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

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.cmstats.FingerprintStats;
import com.android.setupwizard.navigationbar.SetupWizardNavBar;

/**
 * Setup Wizard's version of EnrollFingerprint screen. It inherits the logic and basic structure
 * from EnrollFingerprint class, and should remain similar to that behaviorally. This class should
 * only overload base methods for minor theme and behavior differences specific to Setup Wizard.
 * Other changes should be done to Enrollfingerprint class instead and let this class inherit
 * those changes.
 */
public class SetupEnrollFingerprint extends EnrollFingerprint
        implements SetupWizardNavBar.NavigationBarListener {

    private static final String PASSWORD_CONFIRMED = "password_confirmed";
    private static final String WAITING_FOR_CONFIRMATION = "waiting_for_confirmation";
    private static final int SET_FALLBACK = 99;
    private static final int CONFIRM_EXISTING_REQUEST = 100;

    private LockPatternUtils mLockPatternUtils;
    private boolean mPasswordConfirmed = false;
    private boolean mWaitingForConfirmation = false;

    public static Intent createIntent(Context context) {
        Intent intent = new Intent().setClass(context, SetupEnrollFingerprint.class);
        return intent;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return SetupEnrollFingerprintFragment.class.getName().equals(fragmentName);
    }

    @Override
    /* package */ Class<? extends Fragment> getFragmentClass() {
        return SetupEnrollFingerprintFragment.class;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final boolean confirmCredentials =
                getIntent().getBooleanExtra(ManageFingerprints.CONFIRM_CREDENTIALS, true);
        mPasswordConfirmed = !confirmCredentials;

        if (savedInstanceState != null) {
            mPasswordConfirmed = savedInstanceState.getBoolean(PASSWORD_CONFIRMED);
            mWaitingForConfirmation = savedInstanceState.getBoolean(WAITING_FOR_CONFIRMATION);
        }

        if (!mPasswordConfirmed && !mWaitingForConfirmation) {
            ChooseLockSettingsHelper helper =
                    new ChooseLockSettingsHelper(this, null);
            if (!helper.launchConfirmationActivity(CONFIRM_EXISTING_REQUEST, null, null)) {
                mPasswordConfirmed = true; // no password set, so no need to confirm
            } else {
                mWaitingForConfirmation = true;
            }
        }

        boolean needsFallback =
                getIntent().getBooleanExtra(LockPatternUtils.LOCKSCREEN_FINGERPRINT_FALLBACK, true);

        if (needsFallback) {
            Intent fallBackIntent = new Intent().setClass(this, SetupChooseLockGeneric.class);
            fallBackIntent.putExtra(LockPatternUtils.LOCKSCREEN_BIOMETRIC_WEAK_FALLBACK, true);
            fallBackIntent.putExtra(LockPatternUtils.LOCKSCREEN_FINGERPRINT_FALLBACK, true);
            fallBackIntent.putExtra(ManageFingerprints.CONFIRM_CREDENTIALS, false);
            fallBackIntent.putExtra(EXTRA_SHOW_FRAGMENT_TITLE,
                    R.string.backup_lock_settings_picker_title);
            SetupWizardUtils.copySetupExtras(getIntent(), fallBackIntent);
            startActivityForResult(fallBackIntent, SET_FALLBACK);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Saved so we don't force user to re-enter their password if configuration changes
        outState.putBoolean(PASSWORD_CONFIRMED, mPasswordConfirmed);
        outState.putBoolean(WAITING_FOR_CONFIRMATION, mWaitingForConfirmation);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CONFIRM_EXISTING_REQUEST && resultCode == Activity.RESULT_OK) {
            mWaitingForConfirmation = false;
            mPasswordConfirmed = true;
        } else if (requestCode == SET_FALLBACK &&
                (resultCode == Activity.RESULT_OK || resultCode == Activity.RESULT_FIRST_USER)) {
            Log.d("TAG", "fallback password set");
            mLockPatternUtils = new LockPatternUtils(this);
            mLockPatternUtils.setUseFingerprint();
        } else {
            finish();
            Log.d("TAG", "fallback password NOT set");
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onApplyThemeResource(Resources.Theme theme, int resid, boolean first) {
        resid = R.style.SetupWizardThemeFingerprint;
        super.onApplyThemeResource(theme, resid, first);
    }

    @Override
    public void onNavigationBarCreated(SetupWizardNavBar bar) {
        super.onNavigationBarCreated(bar);
        SetupWizardUtils.setImmersiveMode(this, bar);
    }

    public static class SetupEnrollFingerprintFragment extends EnrollFingerprintFragment {
        @Override
        protected void updateStage(Stage stage) {
            super.updateStage(stage);
            final SetupWizardNavBar setupBar = getEnrollmentActivity().getSetupBar();
            if (stage != Stage.EnrollmentFinished) {
                setupBar.getBackButton().setVisibility(View.VISIBLE);
            } else {
                // setup needs the label to say "NEXT" instead of "DONE"
                setupBar.getNextButton().setText(R.string.next_label);
                setupBar.getBackButton().setVisibility(View.INVISIBLE);
            }
        }

        @Override
        public void onNavigateBack() {
            switch (mUiStage) {
                case EnrollmentStep:
                case EnrollmentError:
                case EnrollmentFinished:
                case Introduction:
                case WrongSensor:
                    getActivity().onBackPressed();
                    break;
                default:
                    super.onNavigateBack();
            }
        }

        @Override
        public void onNavigateNext() {
            switch (mUiStage) {
                case EnrollmentFinished:
                    getActivity().setResult(Activity.RESULT_OK);
                    getActivity().finish();
                    break;
                default:
                    super.onNavigateNext();
            }
        }

        @Override
        protected String getStatsCategory() {
            return FingerprintStats.Categories.FINGERPRINT_ENROLLMENT_OOBE;
        }
    }
}
