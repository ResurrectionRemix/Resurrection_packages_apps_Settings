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
import com.android.settings.rr.SeekBarPreference;

public class VolumePanelSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

        private static final String PREF_TRANSPARENT_VOLUME_DIALOG = "transparent_volume_dialog";
        private static final String PREF_VOLUME_DIALOG_STROKE = "volume_dialog_stroke";
        private static final String PREF_VOLUME_DIALOG_STROKE_COLOR = "volume_dialog_stroke_color";
        private static final String PREF_VOLUME_DIALOG_STROKE_THICKNESS = "volume_dialog_stroke_thickness";
        private static final String PREF_VOLUME_DIALOG_CORNER_RADIUS = "volume_dialog_corner_radius";
        private static final String PREF_VOLUME_DIALOG_STROKE_DASH_WIDTH = "volume_dialog_dash_width";
        private static final String PREF_VOLUME_DIALOG_STROKE_DASH_GAP = "volume_dialog_dash_gap";
        private static final String KEY_VOLUME_DIALOG_TIMEOUT = "volume_dialog_timeout";

        private SeekBarPreference mVolumeDialogAlpha;
        private ListPreference mVolumeDialogStroke;
        private ColorPickerPreference mVolumeDialogStrokeColor;
        private SeekBarPreference mVolumeDialogStrokeThickness;
        private SeekBarPreference mVolumeDialogCornerRadius;
        private SeekBarPreference mVolumeDialogDashWidth;
        private SeekBarPreference mVolumeDialogDashGap;
        private SeekBarPreference mVolumeDialogTimeout;

        static final int DEFAULT_VOLUME_DIALOG_STROKE_COLOR = 0xFF80CBC4;

    	@Override
    	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.rr_volume_panel);

            PreferenceScreen prefSet = getPreferenceScreen();
            final ContentResolver resolver = getActivity().getContentResolver();

            // Volume dialog alpha
            mVolumeDialogAlpha =
                    (SeekBarPreference) prefSet.findPreference(PREF_TRANSPARENT_VOLUME_DIALOG);
            int volumeDialogAlpha = Settings.System.getInt(resolver,
                    Settings.System.TRANSPARENT_VOLUME_DIALOG, 255);
            mVolumeDialogAlpha.setValue(volumeDialogAlpha / 1);
            mVolumeDialogAlpha.setOnPreferenceChangeListener(this);

            // Volume dialog stroke
            mVolumeDialogStroke =
                    (ListPreference) findPreference(PREF_VOLUME_DIALOG_STROKE);
            int volumeDialogStroke = Settings.System.getIntForUser(resolver,
                            Settings.System.VOLUME_DIALOG_STROKE, 0,
                            UserHandle.USER_CURRENT);
            mVolumeDialogStroke.setValue(String.valueOf(volumeDialogStroke));
            mVolumeDialogStroke.setSummary(mVolumeDialogStroke.getEntry());
            mVolumeDialogStroke.setOnPreferenceChangeListener(this);

            // Volume dialog stroke color
            mVolumeDialogStrokeColor =
                    (ColorPickerPreference) findPreference(PREF_VOLUME_DIALOG_STROKE_COLOR);
            mVolumeDialogStrokeColor.setOnPreferenceChangeListener(this);
            int intColor = Settings.System.getInt(resolver,
                    Settings.System.VOLUME_DIALOG_STROKE_COLOR, DEFAULT_VOLUME_DIALOG_STROKE_COLOR);
            String hexColor = String.format("#%08x", (0xFF80CBC4 & intColor));
            mVolumeDialogStrokeColor.setSummary(hexColor);
            mVolumeDialogStrokeColor.setNewPreviewColor(intColor);

            // Volume dialog stroke thickness
            mVolumeDialogStrokeThickness =
                    (SeekBarPreference) findPreference(PREF_VOLUME_DIALOG_STROKE_THICKNESS);
            int volumeDialogStrokeThickness = Settings.System.getInt(resolver,
                    Settings.System.VOLUME_DIALOG_STROKE_THICKNESS, 4);
            mVolumeDialogStrokeThickness.setValue(volumeDialogStrokeThickness / 1);
            mVolumeDialogStrokeThickness.setOnPreferenceChangeListener(this);

            // Volume dialog corner radius
            mVolumeDialogCornerRadius =
                    (SeekBarPreference) findPreference(PREF_VOLUME_DIALOG_CORNER_RADIUS);
            int volumeDialogCornerRadius = Settings.System.getInt(resolver,
                    Settings.System.VOLUME_DIALOG_CORNER_RADIUS, 10);
            mVolumeDialogCornerRadius.setValue(volumeDialogCornerRadius / 1);
            mVolumeDialogCornerRadius.setOnPreferenceChangeListener(this);

            // Volume dialog dash width
            mVolumeDialogDashWidth =
                    (SeekBarPreference) findPreference(PREF_VOLUME_DIALOG_STROKE_DASH_WIDTH);
            int volumeDialogDashWidth = Settings.System.getInt(resolver,
                    Settings.System.VOLUME_DIALOG_STROKE_DASH_WIDTH, 0);
            if (volumeDialogDashWidth != 0) {
                mVolumeDialogDashWidth.setValue(volumeDialogDashWidth / 1);
            } else {
                mVolumeDialogDashWidth.setValue(0);
            }
            mVolumeDialogDashWidth.setOnPreferenceChangeListener(this);

            // Volume dialog dash gap
            mVolumeDialogDashGap =
                    (SeekBarPreference) findPreference(PREF_VOLUME_DIALOG_STROKE_DASH_GAP);
            int volumeDialogDashGap = Settings.System.getInt(resolver,
                    Settings.System.VOLUME_DIALOG_STROKE_DASH_GAP, 10);
            mVolumeDialogDashGap.setValue(volumeDialogDashGap / 1);
            mVolumeDialogDashGap.setOnPreferenceChangeListener(this);

            mVolumeDialogTimeout =
                    (SeekBarPreference) findPreference(KEY_VOLUME_DIALOG_TIMEOUT);
            int timeout = Settings.System.getInt(resolver,
                    Settings.System.VOLUME_DIALOG_TIMEOUT, 3000);
            mVolumeDialogTimeout.setValue(timeout / 1);
            mVolumeDialogTimeout.setOnPreferenceChangeListener(this);

            VolumeDialogSettingsDisabler(volumeDialogStroke);

        }

   	 	@Override
    	protected int getMetricsCategory() {
        return MetricsEvent.RESURRECTED;
		}

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            ContentResolver resolver = getActivity().getContentResolver();
            if (preference == mVolumeDialogAlpha) {
                int alpha = (Integer) newValue;
                Settings.System.putInt(resolver,
                        Settings.System.TRANSPARENT_VOLUME_DIALOG, alpha * 1);
                return true;
            } else if (preference == mVolumeDialogStroke) {
                int volumeDialogStroke = Integer.parseInt((String) newValue);
                int index = mVolumeDialogStroke.findIndexOfValue((String) newValue);
                Settings.System.putIntForUser(resolver, Settings.System.
                        VOLUME_DIALOG_STROKE, volumeDialogStroke, UserHandle.USER_CURRENT);
                mVolumeDialogStroke.setSummary(mVolumeDialogStroke.getEntries()[index]);
                VolumeDialogSettingsDisabler(volumeDialogStroke);
                return true;
            } else if (preference == mVolumeDialogStrokeColor) {
                String hex = ColorPickerPreference.convertToARGB(
                        Integer.valueOf(String.valueOf(newValue)));
                preference.setSummary(hex);
                int intHex = ColorPickerPreference.convertToColorInt(hex);
                Settings.System.putInt(resolver,
                        Settings.System.VOLUME_DIALOG_STROKE_COLOR, intHex);
                return true;
            } else if (preference == mVolumeDialogStrokeThickness) {
                int val = (Integer) newValue;
                Settings.System.putInt(resolver,
                        Settings.System.VOLUME_DIALOG_STROKE_THICKNESS, val * 1);
                return true;
            } else if (preference == mVolumeDialogCornerRadius) {
                int val = (Integer) newValue;
                Settings.System.putInt(resolver,
                        Settings.System.VOLUME_DIALOG_CORNER_RADIUS, val * 1);
                return true;
            } else if (preference == mVolumeDialogDashWidth) {
                int val = (Integer) newValue;
                Settings.System.putInt(resolver,
                        Settings.System.VOLUME_DIALOG_STROKE_DASH_WIDTH, val * 1);
                return true;
            } else if (preference == mVolumeDialogDashGap) {
                int val = (Integer) newValue;
                Settings.System.putInt(resolver,
                        Settings.System.VOLUME_DIALOG_STROKE_DASH_GAP, val * 1);
                return true;
            }else if (preference == mVolumeDialogTimeout) {
                int val = (Integer) newValue;
                Settings.System.putInt(resolver,
                        Settings.System.VOLUME_DIALOG_TIMEOUT, val * 1);
                return true;
            }
            return false;
        }

        private void VolumeDialogSettingsDisabler(int volumeDialogStroke) {
            if (volumeDialogStroke == 0) {
				mVolumeDialogStrokeColor.setEnabled(false);
                mVolumeDialogStrokeThickness.setEnabled(false);
                mVolumeDialogDashWidth.setEnabled(false);
                mVolumeDialogDashGap.setEnabled(false);
            } else if (volumeDialogStroke == 1) {
				mVolumeDialogStrokeColor.setEnabled(false);
                mVolumeDialogStrokeThickness.setEnabled(true);
                mVolumeDialogDashWidth.setEnabled(true);
                mVolumeDialogDashGap.setEnabled(true);
            } else {
				mVolumeDialogStrokeColor.setEnabled(true);
                mVolumeDialogStrokeThickness.setEnabled(true);
                mVolumeDialogDashWidth.setEnabled(true);
                mVolumeDialogDashGap.setEnabled(true);
            }
        }

        @Override
        public void onResume() {
            super.onResume();
        }
}
