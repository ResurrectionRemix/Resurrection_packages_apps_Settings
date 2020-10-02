/*
 * Copyright (C) 2017 AICP
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


package com.android.settings.rr.fragments;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.settings.rr.utils.RRContextConstants;
import android.provider.SearchIndexableResource;
import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.rr.Preferences.*;
import com.android.settings.rr.utils.RRUtils;
import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settingslib.search.SearchIndexable;

import com.android.internal.widget.LockPatternUtils;
import android.provider.Settings;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
@SearchIndexable
public class FingerprintSettings extends SettingsPreferenceFragment implements Indexable {

    private static final String FOD_ICON_PICKER_CATEGORY = "fod_icon_picker";
    private static final String FP_KEYSTORE = "fp_unlock_keystore";
    private static final String EXP = "fod_exp";
    private Preference mExp;
    private SwitchPreference mFingerprintVib;
    private SystemSettingSwitchPreference mFingerprintUnlock;
    private static FingerprintManager mFingerprintManager;
    private static boolean supportsFod;
    private boolean mSupported;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.rr_fp);
        PreferenceScreen prefSet = getPreferenceScreen();
        ContentResolver resolver = getActivity().getContentResolver();

        mSupported =  getResources().getBoolean(
                R.bool.config_supportScreenOffFod);
        mFingerprintUnlock = (SystemSettingSwitchPreference) findPreference(FP_KEYSTORE);
        mExp = (Preference) findPreference(EXP);
        // FOD category
        PreferenceCategory fodIconPickerCategory = (PreferenceCategory) findPreference(FOD_ICON_PICKER_CATEGORY);
        PackageManager packageManager = getContext().getPackageManager();
        supportsFod = packageManager.hasSystemFeature(RRContextConstants.Features.FOD);
        if (fodIconPickerCategory != null && !supportsFod) {
            fodIconPickerCategory.getParent().removePreference(fodIconPickerCategory);
        }
        mFingerprintManager = (FingerprintManager) 
                getActivity().getSystemService(Context.FINGERPRINT_SERVICE);

        if (mFingerprintUnlock != null) {
           if (LockPatternUtils.isDeviceEncryptionEnabled()) {
               mFingerprintUnlock.setEnabled(false);
               mFingerprintUnlock.setSummary(R.string.fp_encrypt_warning);
            } else {
               mFingerprintUnlock.setEnabled(true);
               mFingerprintUnlock.setSummary(R.string.fp_unlock_keystore_summary);
            }
        }

        int anim = Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.RR_CONFIG_ANIM, 0);
        try {
            if (anim == 0) {
                removePreference("animation");
            } else if (anim == 1) {
                removePreference("preview");
            } else if (anim == 2) {
                removePreference("animation");
                removePreference("preview");
            }
        } catch (Exception e) {}
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.RESURRECTED;
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
                    sir.xmlResId = R.xml.rr_fp;
                    result.add(sir);
                    return result;
            }

            @Override
            public List<String> getNonIndexableKeys(Context context) {
                List<String> keys = super.getNonIndexableKeys(context);
                    if (!supportsFod) {
                        try {
                           keys.add("fod_icon_picker");
                           keys.add("fod_icon_picker_category");
                           keys.add("fod_pressed_state");
                           keys.add("fod_recognizing_animation");
                           keys.add("fod_anim");
                           keys.add("fod_icon_wallpaper_color");
                        } catch (Exception e) {}
                    }
                    if (mFingerprintManager == null || !mFingerprintManager.isHardwareDetected()) {
                        try {
                           keys.add("fp_general");
                           keys.add("fp_success_vibrate");
                           keys.add("fp_error_vibrate");
                           keys.add("fp_unlock_keystore");
                        } catch (Exception e) {}
                    }
                return keys;
            }
        };
}
