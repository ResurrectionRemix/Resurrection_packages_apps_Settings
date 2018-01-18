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

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;


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

        private ListPreference mVolumeDialogStroke;
        private Preference mVolumeDialogStrokeColor;
        private Preference mVolumeDialogStrokeThickness;
        private Preference mVolumeDialogDashWidth;
        private Preference mVolumeDialogDashGap;

        static final int DEFAULT_VOLUME_DIALOG_STROKE_COLOR = 0xFF80CBC4;

    	@Override
    	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.rr_volume_panel);

        mVolumeDialogStroke =
                (ListPreference) findPreference(Settings.System.VOLUME_DIALOG_STROKE);
        mVolumeDialogStroke.setOnPreferenceChangeListener(this);
        mVolumeDialogStrokeColor = findPreference(Settings.System.VOLUME_DIALOG_STROKE_COLOR);
        mVolumeDialogStrokeThickness =
                findPreference(Settings.System.VOLUME_DIALOG_STROKE_THICKNESS);
        mVolumeDialogDashWidth = findPreference(Settings.System.VOLUME_DIALOG_STROKE_DASH_WIDTH);
        mVolumeDialogDashGap = findPreference(Settings.System.VOLUME_DIALOG_STROKE_DASH_GAP);
        updateVolumeDialogDependencies(mVolumeDialogStroke.getValue());
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mVolumeDialogStroke) {
            updateVolumeDialogDependencies((String) newValue);
            return true;
        } else {
            return false;
        }
    }

    private void updateVolumeDialogDependencies(String volumeDialogStroke) {
        if (volumeDialogStroke.equals("0")) {
            mVolumeDialogStrokeColor.setEnabled(false);
            mVolumeDialogStrokeThickness.setEnabled(false);
            mVolumeDialogDashWidth.setEnabled(false);
            mVolumeDialogDashGap.setEnabled(false);
        } else if (volumeDialogStroke.equals("1")) {
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
