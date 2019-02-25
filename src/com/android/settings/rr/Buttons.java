/*Copyright (C) 2015 The ResurrectionRemix Project
     Licensed under the Apache License, Version 2.0 (the "License");
     you may not use this file except in compliance with the License.
     You may obtain a copy of the License at

          http://www.apache.org/licenses/LICENSE-2.0

     Unless required by applicable law or agreed to in writing, software
     distributed under the License is distributed on an "AS IS" BASIS,
     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     See the License for the specific language governing permissions and
     limitations under the License.
*/
package com.android.settings.rr;

import android.content.ContentResolver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.os.RemoteException;
import android.provider.Settings;
import android.view.Display;
import android.view.DisplayInfo;
import android.provider.SearchIndexableResource;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.ListPreference;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.rr.utils.DeviceUtils;
import com.android.settings.rr.utils.TelephonyUtils;
import org.lineageos.internal.util.ScreenType;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable.SearchIndexProvider;

import static android.provider.Settings.Secure.CAMERA_DOUBLE_TAP_POWER_GESTURE_DISABLED;
import static org.lineageos.internal.util.DeviceKeysConstants.*;

import lineageos.hardware.LineageHardwareManager;
import lineageos.providers.LineageSettings;
import com.android.internal.utils.ActionUtils;

import java.util.ArrayList;
import java.util.List;

public class Buttons extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "Buttons";
    private static final String CATEGORY_HWKEY = "hwkey_settings";
    private static final String CATEGORY_POWER = "power_key";
    private static final String CATEGORY_VOLUME = "volume_keys";
    private static final String KEY_POWER_END_CALL = "power_end_call";
    private static final String KEY_HOME_ANSWER_CALL = "home_answer_call";
    private static final String KEY_VOLUME_ANSWER_CALL = "volume_answer_call";
    private static final String KEY_VOLUME_MUSIC_CONTROLS = "volbtn_music_controls";
    private static final String KEY_VOLUME_CONTROL_RING_STREAM = "volume_keys_control_ring_stream";
    private static final String KEY_VOLUME_KEY_CURSOR_CONTROL = "volume_key_cursor_control";
    private static final String KEY_SWAP_VOLUME_BUTTONS = "swap_volume_buttons";
    private static final String KEY_TORCH_LONG_PRESS_POWER_GESTURE =
            "torch_long_press_power_gesture";
    private static final String KEY_TORCH_LONG_PRESS_POWER_TIMEOUT =
            "torch_long_press_power_timeout";

    private SwitchPreference mPowerEndCall;
    private SwitchPreference mHomeAnswerCall;
    private SwitchPreference mTorchLongPressPowerGesture;
    private ListPreference mTorchLongPressPowerTimeout;
    private ListPreference mVolumeKeyCursorControl;
    private SwitchPreference mVolumeWakeScreen;
    private SwitchPreference mVolumeMusicControls;
    private SwitchPreference mSwapVolumeButtons;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.rr_buttons);
        final Resources res = getResources();
        final ContentResolver resolver = getActivity().getContentResolver();
        final PreferenceScreen prefScreen = getPreferenceScreen();

       final boolean needsNavbar = ActionUtils.hasNavbarByDefault(getActivity());
       final PreferenceScreen hwkeyCat = (PreferenceScreen) prefScreen
                .findPreference(CATEGORY_HWKEY);
        final PreferenceCategory powerCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_POWER);
        final PreferenceCategory volumeCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_VOLUME);

        if (needsNavbar) {
            prefScreen.removePreference(hwkeyCat);
        }
        // Power button ends calls.
        mPowerEndCall = (SwitchPreference) findPreference(KEY_POWER_END_CALL);

        // Long press power while display is off to activate torchlight
        mTorchLongPressPowerGesture =
                (SwitchPreference) findPreference(KEY_TORCH_LONG_PRESS_POWER_GESTURE);
        final int torchLongPressPowerTimeout = LineageSettings.System.getInt(resolver,
                LineageSettings.System.TORCH_LONG_PRESS_POWER_TIMEOUT, 0);
        mTorchLongPressPowerTimeout = initList(KEY_TORCH_LONG_PRESS_POWER_TIMEOUT,
                torchLongPressPowerTimeout);

        int cursorControlAction = Settings.System.getInt(resolver,
                    Settings.System.VOLUME_KEY_CURSOR_CONTROL, 0);
        mVolumeKeyCursorControl = initList(KEY_VOLUME_KEY_CURSOR_CONTROL,
                    cursorControlAction);

        int swapVolumeKeys = LineageSettings.System.getInt(getContentResolver(),
                    LineageSettings.System.SWAP_VOLUME_KEYS_ON_ROTATION, 0);
        mSwapVolumeButtons = (SwitchPreference)
                    prefScreen.findPreference(KEY_SWAP_VOLUME_BUTTONS);
        if (mSwapVolumeButtons != null) {
                mSwapVolumeButtons.setChecked(swapVolumeKeys > 0);
        }


        if (!DeviceUtils.deviceSupportsFlashLight(getActivity())) {
            powerCategory.removePreference(mTorchLongPressPowerGesture);
            powerCategory.removePreference(mTorchLongPressPowerTimeout);
        }

        if (!TelephonyUtils.isVoiceCapable(getActivity())) {
             powerCategory.removePreference(mPowerEndCall);
             mPowerEndCall = null;

            volumeCategory.removePreference(
                findPreference(LineageSettings.System.VOLUME_ANSWER_CALL));
        }

        mVolumeWakeScreen = (SwitchPreference) findPreference(LineageSettings.System.VOLUME_WAKE_SCREEN);
        mVolumeMusicControls = (SwitchPreference) findPreference(KEY_VOLUME_MUSIC_CONTROLS);

        if (mVolumeWakeScreen != null) {
            if (mVolumeMusicControls != null) {
                mVolumeMusicControls.setDependency(LineageSettings.System.VOLUME_WAKE_SCREEN);
                mVolumeWakeScreen.setDisableDependentsState(true);
            }
        }

    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mVolumeKeyCursorControl) {
            handleSystemListChange(mVolumeKeyCursorControl, newValue,
                    Settings.System.VOLUME_KEY_CURSOR_CONTROL);
            return true;
        } else if (preference == mTorchLongPressPowerTimeout) {
            handleListChange(mTorchLongPressPowerTimeout, newValue,
                    LineageSettings.System.TORCH_LONG_PRESS_POWER_TIMEOUT);
            return true;
        }
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Power button ends calls.
        if (mPowerEndCall != null) {
            final int incallPowerBehavior = Settings.Secure.getInt(getContentResolver(),
                    Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR,
                    Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_DEFAULT);
            final boolean powerButtonEndsCall =
                    (incallPowerBehavior == Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_HANGUP);
            mPowerEndCall.setChecked(powerButtonEndsCall);
         }
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference == mSwapVolumeButtons) {
            int value = mSwapVolumeButtons.isChecked()
                    ? (ScreenType.isTablet(getActivity()) ? 2 : 1) : 0;
            if (value == 2) {
                Display defaultDisplay = getActivity().getWindowManager().getDefaultDisplay();

                DisplayInfo displayInfo = new DisplayInfo();
                defaultDisplay.getDisplayInfo(displayInfo);

                // Not all tablets are landscape
                if (displayInfo.getNaturalWidth() < displayInfo.getNaturalHeight()) {
                    value = 1;
                }
            }
            LineageSettings.System.putInt(getActivity().getContentResolver(),
                    LineageSettings.System.SWAP_VOLUME_KEYS_ON_ROTATION, value);
        } else if (preference == mPowerEndCall) {
            handleTogglePowerButtonEndsCallPreferenceClick();
            return true;
        }

        return super.onPreferenceTreeClick(preference);
    }


    @Override
    public int getMetricsCategory() {
        return MetricsEvent.RESURRECTED;
	}

    private ListPreference initList(String key, Action value) {
        return initList(key, value.ordinal());
    }

    private ListPreference initList(String key, int value) {
        ListPreference list = (ListPreference) getPreferenceScreen().findPreference(key);
        if (list == null) return null;
        list.setValue(Integer.toString(value));
        list.setSummary(list.getEntry());
        list.setOnPreferenceChangeListener(this);
        return list;
    }

    private void handleListChange(ListPreference pref, Object newValue, String setting) {
        String value = (String) newValue;
        int index = pref.findIndexOfValue(value);
        pref.setSummary(pref.getEntries()[index]);
        LineageSettings.System.putInt(getContentResolver(), setting, Integer.valueOf(value));
    }

    private void handleSystemListChange(ListPreference pref, Object newValue, String setting) {
        String value = (String) newValue;
        int index = pref.findIndexOfValue(value);
        pref.setSummary(pref.getEntries()[index]);
        Settings.System.putInt(getContentResolver(), setting, Integer.valueOf(value));
    }

    private void handleTogglePowerButtonEndsCallPreferenceClick() {
        Settings.Secure.putInt(getContentResolver(),
                Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR, (mPowerEndCall.isChecked()
                        ? Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_HANGUP
                        : Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_SCREEN_OFF));
    }

    private void handleToggleHomeButtonAnswersCallPreferenceClick() {
        LineageSettings.Secure.putInt(getContentResolver(),
                LineageSettings.Secure.RING_HOME_BUTTON_BEHAVIOR, (mHomeAnswerCall.isChecked()
                        ? LineageSettings.Secure.RING_HOME_BUTTON_BEHAVIOR_ANSWER
                        : LineageSettings.Secure.RING_HOME_BUTTON_BEHAVIOR_DO_NOTHING));
    }

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        new BaseSearchIndexProvider() {
            @Override
            public List < SearchIndexableResource > getXmlResourcesToIndex(Context context,
                boolean enabled) {
                ArrayList < SearchIndexableResource > resources =
                    new ArrayList < SearchIndexableResource > ();
                SearchIndexableResource res = new SearchIndexableResource(context);
                res.xmlResId = R.xml.rr_buttons;
                resources.add(res);
                return resources;
            }

            @Override
            public List < String > getNonIndexableKeys(Context context) {
                List < String > keys = super.getNonIndexableKeys(context);

                if (ActionUtils.hasNavbarByDefault(context))
                    keys.add(CATEGORY_HWKEY);

                if (!DeviceUtils.deviceSupportsFlashLight(context)) {
                    keys.add(KEY_TORCH_LONG_PRESS_POWER_GESTURE);
                    keys.add(KEY_TORCH_LONG_PRESS_POWER_TIMEOUT);
                }

                if (!TelephonyUtils.isVoiceCapable(context)) {
                    keys.add(KEY_POWER_END_CALL);
                    keys.add(KEY_VOLUME_ANSWER_CALL);
                }

                return keys;
            }
        };
}
