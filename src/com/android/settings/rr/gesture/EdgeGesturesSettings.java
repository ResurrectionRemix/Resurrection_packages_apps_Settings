package com.android.settings.rr.gesture;

import android.app.ActionBar;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;

import com.android.internal.logging.nano.MetricsProto;
import com.android.internal.utils.du.DUActionUtils;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.rr.Preferences.SecureSettingSeekBarPreference;
import com.android.settings.widget.SeekBarPreference;

/**
 * Created by arasthel on 17/02/18.
 */

public class EdgeGesturesSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    public static final String EDGE_GESTURES_ENABLED = "edge_gestures_enabled";
    public static final String EDGE_GESTURES_SCREEN_PERCENT = "edge_gestures_back_screen_percent";

    private String previousTitle;

    private SwitchPreference enabledPreference;
    private SecureSettingSeekBarPreference screenPercentPreference;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.rr_edge_gestures);

        enabledPreference = (SwitchPreference) findPreference(EDGE_GESTURES_ENABLED);
        /*enabledPreference.setChecked((Settings.System.getInt(getContentResolver(),
                Settings.Secure.EDGE_GESTURES_ENABLED, 0) == 1));*/
        enabledPreference.setOnPreferenceChangeListener(this);

        screenPercentPreference = (SecureSettingSeekBarPreference) findPreference(EDGE_GESTURES_SCREEN_PERCENT);
        int percent = Settings.Secure.getIntForUser(getContentResolver(), Settings.Secure.EDGE_GESTURES_BACK_SCREEN_PERCENT, 60, UserHandle.USER_CURRENT);
        screenPercentPreference.setValue(percent);
        screenPercentPreference.setIntervalValue(5);
    }

    @Override
    public void onStart() {
        super.onStart();

        ActionBar actionBar = getActivity().getActionBar();
        previousTitle = actionBar.getTitle().toString();
        actionBar.setTitle(R.string.edge_gestures_title);
    }

    @Override
    public void onStop() {
        super.onStop();

        ActionBar actionBar = getActivity().getActionBar();
        actionBar.setTitle(previousTitle);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.RESURRECTED;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == enabledPreference) {
            int enabled = ((boolean) newValue) ? 1 : 0;
            //Settings.Secure.putIntForUser(getContentResolver(), Settings.Secure.EDGE_GESTURES_ENABLED, enabled, UserHandle.USER_CURRENT);

            if (enabled == 1) {
                Settings.Secure.putInt(getContentResolver(),
                        Settings.Secure.NAVIGATION_BAR_VISIBLE,
                        0);
            } else {
                if (DUActionUtils.hasNavbarByDefault(getPrefContext())) {
                    Settings.Secure.putInt(getContentResolver(),
                            Settings.Secure.NAVIGATION_BAR_VISIBLE,
                            1);
                }
            }
            return true;
        } /*else if (preference == hapticFeedbackDurationPreference) {
            int hapticFeedbackValue = Integer.valueOf((String) newValue);
            Settings.Secure.putIntForUser(getContentResolver(), Settings.Secure.EDGE_GESTURES_FEEDBACK_DURATION, hapticFeedbackValue, UserHandle.USER_CURRENT);
            return true;
        } else if (preference == longPressDurationPreference) {
            int longPressValue = Integer.valueOf((String) newValue);
            Settings.Secure.putIntForUser(getContentResolver(), Settings.Secure.EDGE_GESTURES_LONG_PRESS_DURATION, longPressValue, UserHandle.USER_CURRENT);
            return true;
        }*/

        return false;
    }
}
