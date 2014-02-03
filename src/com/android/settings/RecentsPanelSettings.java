package com.android.settings;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.MediaStore;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;

import com.android.settings.R;
import java.util.List;
import com.android.internal.util.omni.OmniSwitchConstants;

public class RecentsPanelSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "RecentsPanelSettings";

    private static final String RECENT_MENU_CLEAR_ALL = "recent_menu_clear_all";
    private static final String RECENT_MENU_CLEAR_ALL_LOCATION = "recent_menu_clear_all_location";
    
    private static final String RECENTS_USE_OMNISWITCH = "recents_use_omniswitch";
    private static final String OMNISWITCH_START_SETTINGS = "omniswitch_start_settings";

    // Package name of the omnniswitch app
    public static final String OMNISWITCH_PACKAGE_NAME = "org.omnirom.omniswitch";

    // Intent for launching the omniswitch settings actvity
    public static Intent INTENT_OMNISWITCH_SETTINGS = new Intent(Intent.ACTION_MAIN)
         .setClassName(OMNISWITCH_PACKAGE_NAME, OMNISWITCH_PACKAGE_NAME + ".SettingsActivity");
         
    private CheckBoxPreference mRecentClearAll;
    private ListPreference mRecentClearAllPosition;
    private CheckBoxPreference mRecentsUseOmniSwitch;
    private Preference mOmniSwitchSettings;
    private boolean mOmniSwitchStarted;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.recents_panel_settings);

        PreferenceScreen prefSet = getPreferenceScreen();
        ContentResolver resolver = getActivity().getContentResolver();

        boolean useOmniSwitch = false;
        try {
            useOmniSwitch = Settings.System.getInt(getContentResolver(), Settings.System.RECENTS_USE_OMNISWITCH) == 1
                                && isOmniSwitchServiceRunning();
        } catch(SettingNotFoundException e) {
        }

        // OmniSwitch
        mRecentsUseOmniSwitch = (CheckBoxPreference) prefSet.findPreference(RECENTS_USE_OMNISWITCH);
        mRecentsUseOmniSwitch.setChecked(useOmniSwitch);
        mRecentsUseOmniSwitch.setOnPreferenceChangeListener(this);

        mOmniSwitchSettings = (Preference) prefSet.findPreference(OMNISWITCH_START_SETTINGS);
        mOmniSwitchSettings.setEnabled(useOmniSwitch);

        // Default recents
        mRecentClearAll = (CheckBoxPreference) prefSet.findPreference(RECENT_MENU_CLEAR_ALL);
        mRecentClearAll.setChecked(Settings.System.getInt(resolver,
            Settings.System.SHOW_CLEAR_RECENTS_BUTTON, 1) == 1);
        mRecentClearAll.setOnPreferenceChangeListener(this);
        mRecentClearAll.setEnabled(!useOmniSwitch);
        mRecentClearAllPosition = (ListPreference) prefSet.findPreference(RECENT_MENU_CLEAR_ALL_LOCATION);
        String recentClearAllPosition = Settings.System.getString(resolver, Settings.System.CLEAR_RECENTS_BUTTON_LOCATION);
        if (recentClearAllPosition != null) {
             mRecentClearAllPosition.setValue(recentClearAllPosition);
        }
        mRecentClearAllPosition.setOnPreferenceChangeListener(this);
        mRecentClearAllPosition.setEnabled(!useOmniSwitch);
    }


    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mOmniSwitchSettings) {
            startActivity(INTENT_OMNISWITCH_SETTINGS);
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
     }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        ContentResolver resolver = getActivity().getContentResolver();
        if (preference == mRecentClearAll) {
            boolean value = (Boolean) objValue;
            Settings.System.putInt(resolver, Settings.System.SHOW_CLEAR_RECENTS_BUTTON, value ? 1 : 0);
        } else if (preference == mRecentClearAllPosition) {
            String value = (String) objValue;
            Settings.System.putString(resolver, Settings.System.CLEAR_RECENTS_BUTTON_LOCATION, value);
          return true;
        } else if (preference == mRecentsUseOmniSwitch) {
            boolean omniSwitchEnabled = (Boolean) objValue;

            // Give user information that OmniSwitch service is not running
            if (omniSwitchEnabled && !isOmniSwitchServiceRunning()) {
                openOmniSwitchFirstTimeWarning();
            }

            Settings.System.putInt(getContentResolver(), Settings.System.RECENTS_USE_OMNISWITCH, omniSwitchEnabled ? 1 : 0);

            // Update OmniSwitch UI components
            mRecentsUseOmniSwitch.setChecked(omniSwitchEnabled);
            mOmniSwitchSettings.setEnabled(omniSwitchEnabled);

            // Update default recents UI components
            mRecentClearAll.setEnabled(!omniSwitchEnabled);
            mRecentClearAllPosition.setEnabled(!omniSwitchEnabled);
        }

        return false;
    }

    private boolean isOmniSwitchServiceRunning() {
        String serviceName = "org.omnirom.omniswitch.SwitchService";
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceName.equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void openOmniSwitchFirstTimeWarning() {
        new AlertDialog.Builder(getActivity())
            .setTitle(getResources().getString(R.string.omniswitch_first_time_title))
            .setMessage(getResources().getString(R.string.omniswitch_first_time_message))
            .setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                }
            }).show();
    }
}
