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
package com.android.settings.rr.input;

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
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.ListPreference;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;

import com.android.internal.logging.MetricsProto.MetricsEvent;

import com.android.settings.rr.SeekBarPreference;
import com.android.settings.rr.navbar.ActionFragment;

import android.util.Log;
import android.view.IWindowManager;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.WindowManagerGlobal;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.R;
import com.android.settings.rr.Preferences.SystemSettingSwitchPreference;
import com.android.settings.rr.utils.DeviceUtils;
import com.android.settings.rr.utils.TelephonyUtils;
import org.cyanogenmod.internal.util.ScreenType;
import java.util.List;

import cyanogenmod.hardware.CMHardwareManager;
import cyanogenmod.providers.CMSettings;

import com.android.internal.utils.du.ActionConstants;
import com.android.internal.utils.du.DUActionUtils;

import static android.provider.Settings.Secure.CAMERA_DOUBLE_TAP_POWER_GESTURE_DISABLED;



public class HWSettings extends ActionFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "ButtonSettings";
;
	private static final String LONG_PRESS_KILL_DELAY = "long_press_kill_delay";
  
    private static final String KEY_BUTTON_BACKLIGHT = "button_backlight";
    private static final String CATEGORY_POWER = "power_key";
    private static final String HWKEY_DISABLE = "hardware_keys_disable";
    private static final String KEY_POWER_END_CALL = "power_end_call";
    private static final String KEY_HOME_ANSWER_CALL = "home_answer_call";
    private static final String KEY_VOLUME_MUSIC_CONTROLS = "volbtn_music_controls";
    private static final String KEY_VOLUME_CONTROL_RING_STREAM = "volume_keys_control_ring_stream";
    private static final String KEY_VOLUME_KEY_CURSOR_CONTROL = "volume_key_cursor_control";
    private static final String KEY_SWAP_VOLUME_BUTTONS = "swap_volume_buttons";
    private static final String KEY_BUTTON_LIGHT_SCREEN = "button_light_screen";
    private static final String KEY_CAMERA_DOUBLE_TAP_POWER_GESTURE
            = "camera_double_tap_power_gesture";

    private static final String CATEGORY_HOME = "home_key";
    private static final String CATEGORY_BACK = "back_key";
    private static final String CATEGORY_MENU = "menu_key";
    private static final String CATEGORY_ASSIST = "assist_key";
    private static final String CATEGORY_APPSWITCH = "app_switch_key";
    private static final String CATEGORY_CAMERA = "camera_key";
    private static final String CATEGORY_VOLUME = "volume_keys";
    private static final String CATEGORY_BACKLIGHT = "key_backlight";
    private static final String DT2L_CAMERA_VIBRATE_CONFIG = "dt2l_camera_vibrate_config";
    private static final String CATEGORY_HWKEY = "hardware_keys";

    // Available custom actions to perform on a key press.
    // Must match values for KEY_HOME_LONG_PRESS_ACTION in:
    // frameworks/base/core/java/android/provider/Settings.java
    private static final int ACTION_NOTHING = 0;
    private static final int ACTION_MENU = 1;
    private static final int ACTION_APP_SWITCH = 2;
    private static final int ACTION_SEARCH = 3;
    private static final int ACTION_VOICE_SEARCH = 4;
    private static final int ACTION_IN_APP_SEARCH = 5;
    private static final int ACTION_LAUNCH_CAMERA = 6;
    private static final int ACTION_SLEEP = 7;
    private static final int ACTION_LAST_APP = 8;


    // Masks for checking presence of hardware keys.
    // Must match values in frameworks/base/core/res/res/values/config.xml
    public static final int KEY_MASK_HOME = 0x01;
    public static final int KEY_MASK_BACK = 0x02;
    public static final int KEY_MASK_MENU = 0x04;
    public static final int KEY_MASK_ASSIST = 0x08;
    public static final int KEY_MASK_APP_SWITCH = 0x10;
    public static final int KEY_MASK_CAMERA = 0x20;
    public static final int KEY_MASK_VOLUME = 0x40;

    private ListPreference mHomeLongPressAction;
    private ListPreference mHomeDoubleTapAction;
    private ListPreference mMenuPressAction;
    private ListPreference mMenuLongPressAction;
    private ListPreference mAssistPressAction;
    private ListPreference mAssistLongPressAction;
    private ListPreference mAppSwitchPressAction;
    private ListPreference mAppSwitchLongPressAction;
    private SwitchPreference mCameraWakeScreen;
    private SwitchPreference mCameraSleepOnRelease;
    private SwitchPreference mCameraLaunch;
    private ListPreference mVolumeKeyCursorControl;
    private SwitchPreference mVolumeWakeScreen;
    private SwitchPreference mVolumeMusicControls;
    private SwitchPreference mSwapVolumeButtons;
    private SwitchPreference mDisableNavigationKeys;
    private SwitchPreference mPowerEndCall;
    private SwitchPreference mHomeAnswerCall;
    private SwitchPreference mCameraDoubleTapPowerGesture;
    private SeekBarPreference mDt2lCameraVibrateConfig;
    private SwitchPreference mEnableHwKeys;
    private SwitchPreference mHwKeyDisable;
    private SystemSettingSwitchPreference mHwKeyLight;

    private PreferenceScreen mNavigationPreferencesCat;

    private Handler mHandler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.rr_hw_keys);

        final ContentResolver resolver = getContentResolver();
        final PreferenceScreen prefScreen = getPreferenceScreen();
        final Resources res = getResources();
        final int deviceWakeKeys = getResources().getInteger(com.android.internal.R.integer.config_deviceHardwareWakeKeys);

        // bits for hardware keys present on device
        final int deviceKeys = getResources().getInteger(
                com.android.internal.R.integer.config_deviceHardwareKeys);

        // read bits for present hardware keys
        final boolean hasHomeKey = (deviceKeys & KEY_MASK_HOME) != 0;
        final boolean hasBackKey = (deviceKeys & KEY_MASK_BACK) != 0;
        final boolean hasMenuKey = (deviceKeys & KEY_MASK_MENU) != 0;
        final boolean hasAssistKey = (deviceKeys & KEY_MASK_ASSIST) != 0;
        final boolean hasAppSwitchKey = (deviceKeys & KEY_MASK_APP_SWITCH) != 0;
        final boolean hasCameraKey = (deviceKeys & KEY_MASK_CAMERA) != 0;


        final boolean showHomeWake = (deviceWakeKeys & KEY_MASK_HOME) != 0;
        final boolean showBackWake = (deviceWakeKeys & KEY_MASK_BACK) != 0;
        final boolean showMenuWake = (deviceWakeKeys & KEY_MASK_MENU) != 0;
        final boolean showAssistWake = (deviceWakeKeys & KEY_MASK_ASSIST) != 0;
        final boolean showAppSwitchWake = (deviceWakeKeys & KEY_MASK_APP_SWITCH) != 0;

       final boolean needsNavbar = DUActionUtils.hasNavbarByDefault(getActivity());
        final PreferenceCategory hwkeyCat = (PreferenceCategory) prefScreen
                .findPreference(CATEGORY_HWKEY);
        int keysDisabled = 0;
        if (!needsNavbar) {
            mHwKeyDisable = (SwitchPreference) findPreference(HWKEY_DISABLE);
            keysDisabled = Settings.Secure.getIntForUser(getContentResolver(),
                    Settings.Secure.HARDWARE_KEYS_DISABLE, 0,
                    UserHandle.USER_CURRENT);
            mHwKeyDisable.setChecked(keysDisabled != 0);
            mHwKeyDisable.setOnPreferenceChangeListener(this);
        } else {
            prefScreen.removePreference(hwkeyCat);
        }

        // load categories and init/remove preferences based on device
        // configuration

        final boolean hasPowerKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_POWER);
        final PreferenceCategory backCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_BACK);
        final PreferenceCategory homeCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_HOME);
        final PreferenceCategory menuCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_MENU);
        final PreferenceCategory assistCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_ASSIST);
        final PreferenceCategory appSwitchCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_APPSWITCH);

        // Power button ends calls.
        mPowerEndCall = (SwitchPreference) findPreference(KEY_POWER_END_CALL);

        // Home button answers calls.
        mHomeAnswerCall = (SwitchPreference) findPreference(KEY_HOME_ANSWER_CALL);

        if (hasBackKey) {
            if (!showBackWake) {
                backCategory.removePreference(findPreference(CMSettings.System.BACK_WAKE_SCREEN));
            }
        } else {
            prefScreen.removePreference(backCategory);
        }

        if (hasHomeKey) {
            if (!showHomeWake) {
                homeCategory.removePreference(findPreference(CMSettings.System.HOME_WAKE_SCREEN));
            }

            if (!TelephonyUtils.isVoiceCapable(getActivity())) {
                homeCategory.removePreference(mHomeAnswerCall);
                mHomeAnswerCall = null;
            }
        } else {
            prefScreen.removePreference(homeCategory);
        }

        // App switch key (recents)
        if (!hasAppSwitchKey) {
            prefScreen.removePreference(appSwitchCategory);
        }

        if (hasMenuKey) {
            if (!showMenuWake) {
                menuCategory.removePreference(findPreference(CMSettings.System.MENU_WAKE_SCREEN));
            }
        } else {
            prefScreen.removePreference(menuCategory);
        }


        if (hasAssistKey) {
            if (!showAssistWake) {
                assistCategory.removePreference(findPreference(CMSettings.System.ASSIST_WAKE_SCREEN));
            }
        } else {
            prefScreen.removePreference(assistCategory);
        }


        final ButtonBacklightBrightness backlight =
                (ButtonBacklightBrightness) findPreference(KEY_BUTTON_BACKLIGHT);
        mHwKeyLight = (SystemSettingSwitchPreference) findPreference(KEY_BUTTON_LIGHT_SCREEN);
        if (backlight != null) {
            if (!backlight.isButtonSupported() && !backlight.isKeyboardSupported() ||needsNavbar) {
                    prefScreen.removePreference(backlight);
                    prefScreen.removePreference(mHwKeyLight);
            }
        }

        // let super know we can load ActionPreferences
        onPreferenceScreenLoaded(ActionConstants.getDefaults(ActionConstants.HWKEYS));

        // load preferences first
       setActionPreferencesEnabled(keysDisabled == 0);
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

        // Home button answers calls.
        if (mHomeAnswerCall != null) {
            final int incallHomeBehavior = CMSettings.Secure.getInt(getContentResolver(),
                    CMSettings.Secure.RING_HOME_BUTTON_BEHAVIOR,
                    CMSettings.Secure.RING_HOME_BUTTON_BEHAVIOR_DEFAULT);
            final boolean homeButtonAnswersCall =
                (incallHomeBehavior == CMSettings.Secure.RING_HOME_BUTTON_BEHAVIOR_ANSWER);
            mHomeAnswerCall.setChecked(homeButtonAnswersCall);
        }
    }



    private void updateDisableNavkeysCategories() {
        final PreferenceScreen prefScreen = getPreferenceScreen();
	    boolean enabled = Settings.Secure.getInt(getActivity().getContentResolver(),
                Settings.Secure.HARDWARE_KEYS_DISABLE, 0) == 1;
        final ButtonBacklightBrightness backlight =
                (ButtonBacklightBrightness) prefScreen.findPreference(KEY_BUTTON_BACKLIGHT);
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        final int defaultBrightness = getContext().getResources().getInteger(com.android.internal.R.integer.config_buttonBrightnessSettingDefault);
        int keysDisabled = Settings.Secure.getIntForUser(getContentResolver(),
                    Settings.Secure.HARDWARE_KEYS_DISABLE, 0,
                    UserHandle.USER_CURRENT);
 	    /* Save/restore button timeouts to disable them in softkey mode */
        if (keysDisabled == 1) {
            CMSettings.Secure.putInt(getContext().getContentResolver(),
                    CMSettings.Secure.BUTTON_BRIGHTNESS, 0);
        } else {
            int oldBright = prefs.getInt(ButtonBacklightBrightness.KEY_BUTTON_BACKLIGHT,
                    defaultBrightness);
            CMSettings.Secure.putInt(getContext().getContentResolver(),
                    CMSettings.Secure.BUTTON_BRIGHTNESS, oldBright);
        }

        /* Toggle backlight control depending on navbar state, force it to
           off if enabling */
        if (backlight != null) {
            backlight.setEnabled(!enabled);
            backlight.updateSummary();
        }

        if (mHomeAnswerCall != null) {
            mHomeAnswerCall.setEnabled(!enabled);
        }
    }


    @Override
    protected boolean usesExtendedActionsList() {
        return true;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
         if (preference == mHwKeyDisable) {
            boolean value = (Boolean) newValue;
            Settings.Secure.putInt(getContentResolver(), Settings.Secure.HARDWARE_KEYS_DISABLE,
                    value ? 1 : 0);
            setActionPreferencesEnabled(!value);
            updateDisableNavkeysCategories();
            return true;
         }
        return false;
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference == mPowerEndCall) {
            handleTogglePowerButtonEndsCallPreferenceClick();
            return true;
        } else if (preference == mHomeAnswerCall) {
            handleToggleHomeButtonAnswersCallPreferenceClick();
            return true;
        }

        return super.onPreferenceTreeClick(preference);
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.RESURRECTED;
	}

    private void handleTogglePowerButtonEndsCallPreferenceClick() {
        Settings.Secure.putInt(getContentResolver(),
                Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR, (mPowerEndCall.isChecked()
                        ? Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_HANGUP
                        : Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_SCREEN_OFF));
    }

    private void handleToggleHomeButtonAnswersCallPreferenceClick() {
        CMSettings.Secure.putInt(getContentResolver(),
                CMSettings.Secure.RING_HOME_BUTTON_BEHAVIOR, (mHomeAnswerCall.isChecked()
                        ? CMSettings.Secure.RING_HOME_BUTTON_BEHAVIOR_ANSWER
                        : CMSettings.Secure.RING_HOME_BUTTON_BEHAVIOR_DO_NOTHING));
    }
}
