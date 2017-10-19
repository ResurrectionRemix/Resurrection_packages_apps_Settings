/*
 * Copyright (C) 2017 The Dirty Unicorns Project
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

package com.dirtyunicorns.tweaks.fragments;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import com.dirtyunicorns.tweaks.R;
import com.dirtyunicorns.tweaks.activities.UnicornActivity;

public class QuickSettings extends UnicornActivity {

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(getResources().getString(R.string.quicksettings_title));

        getFragmentManager().beginTransaction().replace(android.R.id.content,
                new QuickSettingsFragment()).commit();
    }

    public static class QuickSettingsFragment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.quicksettings);
        }
    }
}
