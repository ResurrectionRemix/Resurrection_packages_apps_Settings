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

package com.android.settings.biometrics.face;

import android.content.Context;
import android.hardware.face.FaceManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.Settings;
import com.android.settings.Utils;
import com.android.settings.biometrics.BiometricStatusPreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.security.SecurityFeatureProvider;
import com.android.internal.widget.LockPatternUtils;

import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnResume;

import com.android.internal.util.custom.faceunlock.FaceUnlockUtils;

public class FaceStatusPreferenceController extends BiometricStatusPreferenceController
        implements LifecycleObserver, OnResume {

    public static final String KEY_FACE_SETTINGS = "face_settings";
    private static final String PREF_KEY_SECURITY_CATEGORY = "security_category";

    protected final FaceManager mFaceManager;
    protected final LockPatternUtils mLockPatternUtils;
    private Preference mPreference;

    public FaceStatusPreferenceController(Context context, Lifecycle lifecycle) {
        this(context, KEY_FACE_SETTINGS, lifecycle);
    }

    public FaceStatusPreferenceController(Context context, String key,
            Lifecycle lifecycle) {
        super(context, key);
        mFaceManager = Utils.getFaceManagerOrNull(context);
        final SecurityFeatureProvider provider = FeatureFactory.getFactory(context)
                .getSecurityFeatureProvider();
        mLockPatternUtils = provider.getLockPatternUtils(context);
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        PreferenceCategory category = screen.findPreference(PREF_KEY_SECURITY_CATEGORY);
        mPreference = category.findPreference(KEY_FACE_SETTINGS);
    }

    @Override
    protected boolean isDeviceSupported() {
        return mFaceManager != null && mFaceManager.isHardwareDetected();
    }

    @Override
    protected boolean hasEnrolledBiometrics() {
        return mFaceManager.hasEnrolledTemplates(getUserId());
    }

    @Override
    protected String getSummaryTextEnrolled() {
        return mContext.getResources()
                .getString(R.string.security_settings_face_preference_summary);
    }

    @Override
    protected String getSummaryTextNoneEnrolled() {
        return mContext.getResources()
                .getString(R.string.security_settings_face_preference_summary_none);
    }

    @Override
    protected String getSettingsClassName() {
        return Settings.FaceSettingsActivity.class.getName();
    }

    @Override
    protected String getEnrollClassName() {
        return FaceEnrollIntroduction.class.getName();
    }

    @Override
    public void onResume() {
        if (mPreference != null){
            updateEnabledState(mPreference);
        }
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        updateEnabledState(preference);
    }

    private void updateEnabledState(Preference preference) {
        if (FaceUnlockUtils.hasMotoFaceUnlock()) {
            if (Utils.isFaceDisabledByAdmin(mContext)){
                preference.setEnabled(false);
                preference.setSummary(R.string.disabled_by_administrator_summary);
            }else if (!mLockPatternUtils.isSecure(getUserId())){
                preference.setEnabled(false);
                preference.setSummary(R.string.disabled_because_no_backup_security);
            }else{
                preference.setEnabled(true);
            }
        }
    }

}
