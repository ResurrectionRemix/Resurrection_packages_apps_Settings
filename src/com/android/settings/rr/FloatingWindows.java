package com.android.settings.rr;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;

import com.android.settings.SettingsPreferenceFragment;

import com.android.internal.logging.MetricsLogger;

import com.android.settings.R;

public class FloatingWindows extends SettingsPreferenceFragment
            implements OnPreferenceChangeListener {

        private static final String TAG = "FloatingWindows";

        private static final String FLOATING_WINDOW_MODE = "floating_window_mode";
        private static final String GESTURE_ANYWHERE_FLOATING = "gesture_anywhere_floating";
        private static final String SLIM_ACTION_FLOATS = "slim_action_floats";

        SwitchPreference mFloatingWindowMode;
        SwitchPreference mGestureAnywhereFloatingWindow;
        SwitchPreference mSlimActionFloatingWindow;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            ContentResolver resolver = getActivity().getContentResolver();
            Resources res = getResources();

            addPreferencesFromResource(R.xml.floating_windows);

            PreferenceScreen prefSet = getPreferenceScreen();

            mFloatingWindowMode = (SwitchPreference)
                    prefSet.findPreference(FLOATING_WINDOW_MODE);
            mFloatingWindowMode.setChecked(Settings.System.getInt(resolver,
                    Settings.System.FLOATING_WINDOW_MODE, 0) == 1);
            mFloatingWindowMode.setOnPreferenceChangeListener(this);

            mGestureAnywhereFloatingWindow = (SwitchPreference)
                    prefSet.findPreference(GESTURE_ANYWHERE_FLOATING);
            mGestureAnywhereFloatingWindow.setChecked(Settings.System.getInt(resolver,
                    Settings.System.GESTURE_ANYWHERE_FLOATING, 0) == 1);
            mGestureAnywhereFloatingWindow.setOnPreferenceChangeListener(this);

            mSlimActionFloatingWindow = (SwitchPreference)
                    prefSet.findPreference(SLIM_ACTION_FLOATS);
            mSlimActionFloatingWindow.setChecked(Settings.System.getInt(resolver,
                    Settings.System.SLIM_ACTION_FLOATS, 0) == 1);
            mSlimActionFloatingWindow.setOnPreferenceChangeListener(this);
        }

        @Override
        public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }
        
    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.DONT_TRACK_ME_BRO;
    }

        @Override
        public boolean onPreferenceChange(Preference preference, Object objValue) {
            ContentResolver resolver = getActivity().getContentResolver();
            final String key = preference.getKey();
            if (preference == mFloatingWindowMode) {
                Settings.System.putInt(resolver,
                        Settings.System.FLOATING_WINDOW_MODE,
                (Boolean) objValue ? 1 : 0);
                return true;
            } else if (preference == mGestureAnywhereFloatingWindow) {
                Settings.System.putInt(resolver,
                        Settings.System.GESTURE_ANYWHERE_FLOATING,
                (Boolean) objValue ? 1 : 0);
                return true;
            } else if (preference == mSlimActionFloatingWindow) {
                Settings.System.putInt(resolver,
                        Settings.System.SLIM_ACTION_FLOATS,
                (Boolean) objValue ? 1 : 0);
                return true;
            }

            return true;
        }

        @Override
        public void onPause() {
            super.onPause();
        }

        @Override
        public void onResume() {
            super.onResume();
        }
}
