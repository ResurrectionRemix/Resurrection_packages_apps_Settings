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
import com.android.settings.util.CMDProcessor;
import com.android.settings.util.Helpers;
import com.android.settings.Utils;
import com.android.settings.SettingsPreferenceFragment;
import net.margaritov.preference.colorpicker.ColorPickerPreference;
import com.android.settings.rr.SeekBarPreference;

public class TransparencySettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

        private static final String PREF_QS_TRANSPARENT_HEADER = "qs_transparent_header";
        private static final String PREF_QS_STROKE = "qs_stroke";
        private static final String PREF_QS_STROKE_COLOR = "qs_stroke_color";
        private static final String PREF_QS_STROKE_THICKNESS = "qs_stroke_thickness";
        private static final String PREF_QS_CORNER_RADIUS = "qs_corner_radius";
        private static final String PREF_QS_STROKE_DASH_WIDTH = "qs_dash_width";
        private static final String PREF_QS_STROKE_DASH_GAP = "qs_dash_gap";
        //private static final String PREF_NOTIFICATION_ALPHA = "notification_alpha";

        private SeekBarPreference mQSShadeAlpha;
        //private SeekBarPreference mQSHeaderAlpha;
        private ListPreference mQSStroke;
        private ColorPickerPreference mQSStrokeColor;
        private SeekBarPreference mQSStrokeThickness;
        private SeekBarPreference mQSCornerRadius;
        private SeekBarPreference mQSDashWidth;
        private SeekBarPreference mQSDashGap;
        //private SeekBarPreference mNotificationsAlpha;
        static final int DEFAULT_QS_STROKE_COLOR = 0x4285F4;

    	@Override
    	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.rr_transparency);

            PreferenceScreen prefSet = getPreferenceScreen();
            final ContentResolver resolver = getActivity().getContentResolver();

            // QS header alpha
            /*mQSHeaderAlpha =
                    (SeekBarPreference) prefSet.findPreference(PREF_QS_TRANSPARENT_HEADER);
            int qSHeaderAlpha = Settings.System.getInt(resolver,
                    Settings.System.QS_TRANSPARENT_HEADER, 255);
            mQSHeaderAlpha.setValue(qSHeaderAlpha / 1);
            mQSHeaderAlpha.setOnPreferenceChangeListener(this);*/

            // QS stroke
            mQSStroke =
                    (ListPreference) findPreference(PREF_QS_STROKE);
            int qSStroke = Settings.System.getIntForUser(resolver,
                            Settings.System.QS_STROKE, 0,
                            UserHandle.USER_CURRENT);
            mQSStroke.setValue(String.valueOf(qSStroke));
            mQSStroke.setSummary(mQSStroke.getEntry());
            mQSStroke.setOnPreferenceChangeListener(this);

           // QS stroke color
            mQSStrokeColor =
                    (ColorPickerPreference) findPreference(PREF_QS_STROKE_COLOR);
            mQSStrokeColor.setOnPreferenceChangeListener(this);
            int qSIntColor = Settings.System.getInt(resolver,
                    Settings.System.QS_STROKE_COLOR, DEFAULT_QS_STROKE_COLOR);
            String qSHexColor = String.format("#%08x", (0x4285F4 & qSIntColor));
            mQSStrokeColor.setSummary(qSHexColor);
            mQSStrokeColor.setNewPreviewColor(qSIntColor);

            // QS stroke thickness
            mQSStrokeThickness =
                    (SeekBarPreference) findPreference(PREF_QS_STROKE_THICKNESS);
            int qSStrokeThickness = Settings.System.getInt(resolver,
                    Settings.System.QS_STROKE_THICKNESS, 4);
            mQSStrokeThickness.setValue(qSStrokeThickness / 1);
            mQSStrokeThickness.setOnPreferenceChangeListener(this);

            // QS corner radius
            mQSCornerRadius =
                    (SeekBarPreference) findPreference(PREF_QS_CORNER_RADIUS);
            int qSCornerRadius = Settings.System.getInt(resolver,
                    Settings.System.QS_CORNER_RADIUS, 5);
            mQSCornerRadius.setValue(qSCornerRadius / 1);
            mQSCornerRadius.setOnPreferenceChangeListener(this);

            // QS dash width
            mQSDashWidth =
                    (SeekBarPreference) findPreference(PREF_QS_STROKE_DASH_WIDTH);
            int qSDialogDashWidth = Settings.System.getInt(resolver,
                    Settings.System.QS_STROKE_DASH_WIDTH, 0);
            if (qSDialogDashWidth != 0) {
                mQSDashWidth.setValue(qSDialogDashWidth / 1);
            } else {
                mQSDashWidth.setValue(0);
            }
            mQSDashWidth.setOnPreferenceChangeListener(this);

            // QS dash gap
            mQSDashGap =
                    (SeekBarPreference) findPreference(PREF_QS_STROKE_DASH_GAP);
            int qSDialogDashGap = Settings.System.getInt(resolver,
                    Settings.System.QS_STROKE_DASH_GAP, 10);
            mQSDashGap.setValue(qSDialogDashGap / 1);
            mQSDashGap.setOnPreferenceChangeListener(this);

            // Notifications alpha
            /*mNotificationsAlpha =
                    (SeekBarPreference) prefSet.findPreference(PREF_NOTIFICATION_ALPHA);
            int notificationsAlpha = Settings.System.getInt(resolver,
                    Settings.System.NOTIFICATION_ALPHA, 255);
            mNotificationsAlpha.setValue(notificationsAlpha / 1);
            mNotificationsAlpha.setOnPreferenceChangeListener(this);*/

            QSSettingsDisabler(qSStroke);

        }

   	@Override
    	protected int getMetricsCategory() {
        return MetricsEvent.RESURRECTED;
	}

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            ContentResolver resolver = getActivity().getContentResolver();
            if (preference == mQSStroke) {
                int qSStroke = Integer.parseInt((String) newValue);
                int index = mQSStroke.findIndexOfValue((String) newValue);
                Settings.System.putIntForUser(resolver, Settings.System.
                        QS_STROKE, qSStroke, UserHandle.USER_CURRENT);
                mQSStroke.setSummary(mQSStroke.getEntries()[index]);
                QSSettingsDisabler(qSStroke);
                if (index == 0) {
                    Helpers.showSystemUIrestartDialog(getActivity());
                }
                return true;
            } else if (preference == mQSStrokeColor) {
                String hex = ColorPickerPreference.convertToARGB(
                        Integer.valueOf(String.valueOf(newValue)));
                preference.setSummary(hex);
                int intHex = ColorPickerPreference.convertToColorInt(hex);
                Settings.System.putInt(resolver,
                        Settings.System.QS_STROKE_COLOR, intHex);
                return true;
            } else if (preference == mQSStrokeThickness) {
                int val = (Integer) newValue;
                Settings.System.putInt(resolver,
                        Settings.System.QS_STROKE_THICKNESS, val * 1);
                return true;
            } else if (preference == mQSCornerRadius) {
                int val = (Integer) newValue;
                Settings.System.putInt(resolver,
                        Settings.System.QS_CORNER_RADIUS, val * 1);
                return true;
            } else if (preference == mQSDashWidth) {
                int val = (Integer) newValue;
                Settings.System.putInt(resolver,
                        Settings.System.QS_STROKE_DASH_WIDTH, val * 1);
                return true;
            } else if (preference == mQSDashGap) {
                int val = (Integer) newValue;
                Settings.System.putInt(resolver,
                        Settings.System.QS_STROKE_DASH_GAP, val * 1);
                return true;
            /*} else if (preference == mNotificationsAlpha) {
                int alpha = (Integer) newValue;
                Settings.System.putInt(resolver,
                        Settings.System.NOTIFICATION_ALPHA, alpha * 1);
                return true;*/
            }
            return false;
        }

        private void QSSettingsDisabler(int qSStroke) {
            if (qSStroke == 0) {
                mQSStrokeThickness.setEnabled(false);
                mQSCornerRadius.setEnabled(false);
                mQSDashWidth.setEnabled(false);
                mQSDashGap.setEnabled(false);
            } else if (qSStroke == 1) {
                mQSStrokeColor.setEnabled(false);
                mQSStrokeThickness.setEnabled(true);
                mQSCornerRadius.setEnabled(true);
                mQSDashWidth.setEnabled(true);
                mQSDashGap.setEnabled(true);
            } else {
                mQSStrokeColor.setEnabled(true);
                mQSStrokeThickness.setEnabled(true);
                mQSCornerRadius.setEnabled(true);
                mQSDashWidth.setEnabled(true);
                mQSDashGap.setEnabled(true);
            }
        }

        @Override
        public void onResume() {
            super.onResume();
        }
}
