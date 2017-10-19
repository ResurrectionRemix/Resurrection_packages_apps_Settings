package com.dirtyunicorns.tweaks.fragments;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import com.dirtyunicorns.tweaks.R;
import com.dirtyunicorns.tweaks.activities.UnicornActivity;

public class PowerMenu extends UnicornActivity {

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(getResources().getString(R.string.powermenu_title));

        getFragmentManager().beginTransaction().replace(android.R.id.content,
                new PowerMenuFragment()).commit();
    }

    public static class PowerMenuFragment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.powermenu);
        }
    }
}