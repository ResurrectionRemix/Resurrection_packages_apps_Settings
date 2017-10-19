/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.settings.inputmethod;
public class UserDictionaryLocalePicker extends com.android.internal.app.LocalePicker {
    public UserDictionaryLocalePicker(final UserDictionaryAddWordFragment parent) {
        super();
        setLocaleSelectionListener(parent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mStockRecentsCategory = (PreferenceCategory) findPreference(PREF_STOCK_RECENTS_CATEGORY);
        mAlternativeRecentsCategory =
                (PreferenceCategory) findPreference(PREF_ALTERNATIVE_RECENTS_CATEGORY);

        // Alternative recents en-/disabling
        Preference.OnPreferenceChangeListener alternativeRecentsChangeListener =
                new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                updateDependencies((Boolean) newValue ? preference : null);
                return true;
            }
        };
        for (int i = 0; i < mAlternativeRecentsCategory.getPreferenceCount(); i++) {
            Preference preference = mAlternativeRecentsCategory.getPreference(i);
            if (preference instanceof MasterSwitchPreference) {
                preference.setOnPreferenceChangeListener(alternativeRecentsChangeListener);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        for (int i = 0; i < mAlternativeRecentsCategory.getPreferenceCount(); i++) {
            Preference preference = mAlternativeRecentsCategory.getPreference(i);
            if (preference instanceof MasterSwitchPreference) {
                ((MasterSwitchPreference) preference).reloadValue();
            }
        }
        updateDependencies(null);
    }

    private void updateDependencies(Preference enabledAlternativeRecentsPreference) {
        boolean alternativeRecentsEnabled = false;
        for (int i = 0; i < mAlternativeRecentsCategory.getPreferenceCount(); i++) {
            Preference preference = mAlternativeRecentsCategory.getPreference(i);
            if (enabledAlternativeRecentsPreference != null
                    && enabledAlternativeRecentsPreference != preference
                    && preference instanceof MasterSwitchPreference
                    && ((MasterSwitchPreference) preference).isChecked()) {
                // Only one alternative recents at the time!
                ((MasterSwitchPreference) preference).setCheckedPersisting(false);
            } else if (preference instanceof MasterSwitchPreference
                    && ((MasterSwitchPreference) preference).isChecked()) {
                alternativeRecentsEnabled = true;
            }
        }
        mStockRecentsCategory.setEnabled(!alternativeRecentsEnabled);
    }
}
