package com.android.settings.notification;


import com.android.settings.AudioEnhancerUtils;
import com.android.settings.AudioEnhancerService;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.R;
import android.content.Context;
import android.os.Bundle;
import android.content.res.Resources;
import android.app.Activity;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.TwoStatePreference;
import android.widget.Toast;

public class DiracSettings extends SettingsPreferenceFragment {

    private static final String KEY_MI_SOUND_ENHANCER = "mi_sound_enhancer";
    private static final String KEY_HEADSET_TYPE = "dirac_headsets";
    private static final String KEY_MUSIC_MODE = "dirac_mode";
    private static final String KEY_DIRAC_PRESET = "dirac_preset";
    private TwoStatePreference mEnableDisableDirac;
    private ListPreference mHeadsetType, mMusicMode, mDiracPreset;
    private Context mContext;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.dirac_settings);
        mContext = getActivity();
        mEnableDisableDirac = (TwoStatePreference) findPreference(KEY_MI_SOUND_ENHANCER);
        if (AudioEnhancerService.du.hasInitialized()) {
          mEnableDisableDirac.setChecked(AudioEnhancerService.du.isEnabled(mContext) ? true:false);
        }
        mEnableDisableDirac.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
          public boolean onPreferenceChange(Preference preference, Object newValue) {
            if(((TwoStatePreference) preference).isChecked() != (Boolean) newValue) {
              AudioEnhancerService.du.setEnabled(mContext, (Boolean) newValue ? true:false);
            }
            return true;
          }
        });
        mHeadsetType = (ListPreference) findPreference(KEY_HEADSET_TYPE);
        mMusicMode = (ListPreference) findPreference(KEY_MUSIC_MODE);
        mDiracPreset = (ListPreference) findPreference(KEY_DIRAC_PRESET);

        if (AudioEnhancerService.du.hasInitialized()) {
          mHeadsetType.setValue(Integer.toString(AudioEnhancerService.du.getHeadsetType(mContext)));
          mDiracPreset.setValue(AudioEnhancerService.du.getLevel(mContext));
        }

        mHeadsetType.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
          public boolean onPreferenceChange(Preference preference, Object newValue) {
            Toast.makeText(mContext,"Applied HeadSetType : " + String.valueOf(newValue),Toast.LENGTH_SHORT).show();
            int val = Integer.parseInt(newValue.toString());
            AudioEnhancerService.du.setHeadsetType(mContext, val);
            return true;
          }
        });

        mMusicMode.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
          public boolean onPreferenceChange(Preference preference, Object newValue) {
            Toast.makeText(mContext,"Applied Music Mode : " + String.valueOf(newValue),Toast.LENGTH_SHORT).show();
            int val = Integer.parseInt(newValue.toString());
            AudioEnhancerService.du.setMode(mContext, val);
            return true;
          }
        });

        mDiracPreset.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
          public boolean onPreferenceChange(Preference preference, Object newValue) {
            Toast.makeText(mContext,"Applied Level : " + String.valueOf(newValue),Toast.LENGTH_SHORT).show();
            AudioEnhancerService.du.setLevel(mContext, String.valueOf(newValue));
            return true;
          }
        });
        
        removePreference(KEY_MUSIC_MODE);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.NOTIFICATION_OTHER_SOUND;
    }
}