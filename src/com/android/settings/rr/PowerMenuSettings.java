package com.android.settings.rr;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.support.v7.preference.ListPreference;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.PreferenceScreen;
import android.provider.Settings;

import com.android.internal.logging.MetricsProto.MetricsEvent;


import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import net.margaritov.preference.colorpicker.ColorPickerPreference;
import com.android.settings.SeekBarPreference;

public class PowerMenuSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
;
        private static final String PREF_TRANSPARENT_POWER_MENU = "transparent_power_menu";
        private static final String PREF_TRANSPARENT_POWER_DIALOG_DIM = "transparent_power_dialog_dim";

        private SeekBarPreference mPowerMenuAlpha;
        private SeekBarPreference mPowerDialogDim;

    	@Override
    	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.rr_power_menu);

            PreferenceScreen prefSet = getPreferenceScreen();
            final ContentResolver resolver = getActivity().getContentResolver();

            // Power menu alpha
            mPowerMenuAlpha =
                    (SeekBarPreference) prefSet.findPreference(PREF_TRANSPARENT_POWER_MENU);
            int powerMenuAlpha = Settings.System.getInt(resolver,
                    Settings.System.TRANSPARENT_POWER_MENU, 100);
            mPowerMenuAlpha.setProgress(powerMenuAlpha / 1);
            mPowerMenuAlpha.setOnPreferenceChangeListener(this);

            // Power/reboot dialog dim
            mPowerDialogDim =
                    (SeekBarPreference) prefSet.findPreference(PREF_TRANSPARENT_POWER_DIALOG_DIM);
            int powerDialogDim = Settings.System.getInt(resolver,
                    Settings.System.TRANSPARENT_POWER_DIALOG_DIM, 50);
            mPowerDialogDim.setProgress(powerDialogDim / 1);
            mPowerDialogDim.setOnPreferenceChangeListener(this);
        }

   	 	@Override
    	protected int getMetricsCategory() {
        return MetricsEvent.RESURRECTED;
		}

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            ContentResolver resolver = getActivity().getContentResolver();
            if (preference == mPowerMenuAlpha) {
                int alpha = (Integer) newValue;
                Settings.System.putInt(resolver,
                        Settings.System.TRANSPARENT_POWER_MENU, alpha * 1);
                return true;
            } else if (preference == mPowerDialogDim) {
                int alpha = (Integer) newValue;
                Settings.System.putInt(resolver,
                        Settings.System.TRANSPARENT_POWER_DIALOG_DIM, alpha * 1);
                return true;
            }
            return false;
        }

        @Override
        public void onResume() {
            super.onResume();
        }
}
