/*
 * Copyright (C) 2012-2014 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.cyanogenmod;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.hardware.Camera;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.Display;
import android.widget.Toast;

import com.android.internal.util.cm.LockscreenBackgroundUtil;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.ChooseLockSettingsHelper;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

public class LockscreenInterface extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String LOCKSCREEN_GENERAL_CATEGORY = "lockscreen_general_category";
    private static final String LOCKSCREEN_WIDGETS_CATEGORY = "lockscreen_widgets_category";
    private static final String KEY_BATTERY_STATUS = "lockscreen_battery_status";
    private static final String KEY_LOCKSCREEN_BUTTONS = "lockscreen_buttons";
    private static final String KEY_ENABLE_WIDGETS = "keyguard_enable_widgets";
    private static final String KEY_ENABLE_CAMERA = "keyguard_enable_camera";
    private static final String LOCKSCREEN_QUICK_UNLOCK_CONTROL = "lockscreen_quick_unlock_control";
    private static final String PREF_LOCKSCREEN_TORCH = "lockscreen_torch";
    private static final String KEY_LOCKSCREEN_MODLOCK_ENABLED = "lockscreen_modlock_enabled";
    
    private static final String LOCKSCREEN_BACKGROUND_STYLE = "lockscreen_background_style";

    private static final String LOCKSCREEN_WALLPAPER_TEMP_NAME = ".lockwallpaper";

    private static final int REQUEST_PICK_WALLPAPER = 201;

    private CheckBoxPreference mEnableKeyguardWidgets;
    private CheckBoxPreference mEnableCameraWidget;
    private CheckBoxPreference mQuickUnlock;
    private CheckBoxPreference mGlowpadTorch;
    private CheckBoxPreference mEnableModLock;
    private ListPreference mLockBackground;
    private ListPreference mBatteryStatus;

    private ChooseLockSettingsHelper mChooseLockSettingsHelper;
    private LockPatternUtils mLockUtils;
    private DevicePolicyManager mDPM;

    private File mTempWallpaper, mWallpaper;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.lockscreen_interface_settings);

        PreferenceScreen prefs = getPreferenceScreen();

        mChooseLockSettingsHelper = new ChooseLockSettingsHelper(getActivity());
        mLockUtils = mChooseLockSettingsHelper.utils();
        mDPM = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);

        // Find categories
        PreferenceCategory generalCategory = (PreferenceCategory)
                findPreference(LOCKSCREEN_GENERAL_CATEGORY);
        PreferenceCategory widgetsCategory = (PreferenceCategory)
                findPreference(LOCKSCREEN_WIDGETS_CATEGORY);

        // Find preferences
        mEnableKeyguardWidgets = (CheckBoxPreference) findPreference(KEY_ENABLE_WIDGETS);
        mEnableCameraWidget = (CheckBoxPreference) findPreference(KEY_ENABLE_CAMERA);
        mEnableCameraWidget = (CheckBoxPreference) findPreference(KEY_ENABLE_CAMERA);
        
        mEnableModLock = (CheckBoxPreference) findPreference(KEY_LOCKSCREEN_MODLOCK_ENABLED);
         if (mEnableModLock != null) {
             mEnableModLock.setOnPreferenceChangeListener(this);
         }
         
        mGlowpadTorch = (CheckBoxPreference) findPreference(PREF_LOCKSCREEN_TORCH);
        mGlowpadTorch.setChecked(Settings.System.getInt(
                getActivity().getApplicationContext().getContentResolver(),
                Settings.System.LOCKSCREEN_GLOWPAD_TORCH, 0) == 1);
        mGlowpadTorch.setOnPreferenceChangeListener(this);

        // Remove glowpad torch if device doesn't have torch
        if (!hasTorch()) {
            prefs.removePreference(mGlowpadTorch);
        }

        mBatteryStatus = (ListPreference) findPreference(KEY_BATTERY_STATUS);
        if (mBatteryStatus != null) {
            mBatteryStatus.setOnPreferenceChangeListener(this);
        }

        // Remove lockscreen button actions if device doesn't have hardware keys
        if (!hasButtons()) {
            generalCategory.removePreference(findPreference(KEY_LOCKSCREEN_BUTTONS));
        }

        // Enable or disable lockscreen widgets based on policy
        checkDisabledByPolicy(mEnableKeyguardWidgets,
                DevicePolicyManager.KEYGUARD_DISABLE_WIDGETS_ALL);

        // Enable or disable camera widget based on device and policy
        if (Camera.getNumberOfCameras() == 0) {
            widgetsCategory.removePreference(mEnableCameraWidget);
            mEnableCameraWidget = null;
            mLockUtils.setCameraEnabled(false);
        } else if (mLockUtils.isSecure()) {
            checkDisabledByPolicy(mEnableCameraWidget,
                    DevicePolicyManager.KEYGUARD_DISABLE_SECURE_CAMERA);
        }
        
        boolean canEnableModLockscreen = false;
        final Bundle keyguard_metadata = Utils.getApplicationMetadata(
                getActivity(), "com.android.keyguard");
        if (keyguard_metadata != null) {
            canEnableModLockscreen = keyguard_metadata.getBoolean(
                    "com.cyanogenmod.keyguard", false);
        }

        if (mEnableModLock != null && !canEnableModLockscreen) {
            generalCategory.removePreference(mEnableModLock);
            mEnableModLock = null;
        }
        
        // Remove maximize widgets on tablets
        if (!Utils.isPhone(getActivity())) {
            widgetsCategory.removePreference(
                    findPreference(Settings.System.LOCKSCREEN_MAXIMIZE_WIDGETS));
        }

	    // Quick unlock
	    mQuickUnlock = (CheckBoxPreference) findPreference(LOCKSCREEN_QUICK_UNLOCK_CONTROL);
		if (mQuickUnlock != null) {
		mQuickUnlock.setChecked(Settings.System.getInt(getContentResolver(),
		Settings.System.LOCKSCREEN_QUICK_UNLOCK_CONTROL, 0) == 1);
        }

	    // Lockscreen Background
        mLockBackground = (ListPreference) findPreference(LOCKSCREEN_BACKGROUND_STYLE);
        mLockBackground.setOnPreferenceChangeListener(this);

        mTempWallpaper = getActivity().getFileStreamPath(LOCKSCREEN_WALLPAPER_TEMP_NAME);
        mWallpaper = LockscreenBackgroundUtil.getWallpaperFile(getActivity());
    }

    @Override
    public void onResume() {
        super.onResume();

        // Update custom widgets and camera
        if (mEnableKeyguardWidgets != null) {
            mEnableKeyguardWidgets.setChecked(mLockUtils.getWidgetsEnabled());
        }
        // Update battery status
        if (mBatteryStatus != null) {
            ContentResolver cr = getActivity().getContentResolver();
            int batteryStatus = Settings.System.getInt(cr,
                    Settings.System.LOCKSCREEN_BATTERY_VISIBILITY, 0);
            mBatteryStatus.setValueIndex(batteryStatus);
            mBatteryStatus.setSummary(mBatteryStatus.getEntries()[batteryStatus]);
        }

        // Update mod lockscreen status
        if (mEnableModLock != null) {
            ContentResolver cr = getActivity().getContentResolver();
            boolean checked = Settings.System.getInt(
                    cr, Settings.System.LOCKSCREEN_MODLOCK_ENABLED, 1) == 1;
            mEnableModLock.setChecked(checked);
        }
                    
        if (mEnableCameraWidget != null) {
            mEnableCameraWidget.setChecked(mLockUtils.getCameraEnabled());

        }
        
	    if (mQuickUnlock != null) {
            mQuickUnlock.setChecked(Settings.System.getInt(getActivity().getContentResolver(),
                  Settings.System.LOCKSCREEN_QUICK_UNLOCK_CONTROL, 0) == 1);
        }

        updateBackgroundPreference();
    }

    private void updateBackgroundPreference() {
        int lockVal = LockscreenBackgroundUtil.getLockscreenStyle(getActivity());
        mLockBackground.setValue(Integer.toString(lockVal));
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        final String key = preference.getKey();

        if (KEY_ENABLE_WIDGETS.equals(key)) {
            mLockUtils.setWidgetsEnabled(mEnableKeyguardWidgets.isChecked());
            return true;
        } else if (KEY_ENABLE_CAMERA.equals(key)) {
            mLockUtils.setCameraEnabled(mEnableCameraWidget.isChecked());
            return true;
        }
	if (preference == mQuickUnlock) {
            Settings.System.putInt(getActivity().getContentResolver(),
                Settings.System.LOCKSCREEN_QUICK_UNLOCK_CONTROL, mQuickUnlock.isChecked() ? 1 : 0);
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        ContentResolver cr = getActivity().getContentResolver();
        if (preference == mBatteryStatus) {
            int value = Integer.valueOf((String) objValue);
            int index = mBatteryStatus.findIndexOfValue((String) objValue);
            Settings.System.putInt(cr, Settings.System.LOCKSCREEN_BATTERY_VISIBILITY, value);
            mBatteryStatus.setSummary(mBatteryStatus.getEntries()[index]);
            return true;
        } else if (preference == mGlowpadTorch) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_GLOWPAD_TORCH,
                    (Boolean) objValue ? 1 : 0);
           return true;
        } else if (preference == mEnableModLock) {
            boolean value = (Boolean) objValue;
            Settings.System.putInt(cr, Settings.System.LOCKSCREEN_MODLOCK_ENABLED,
                    value ? 1 : 0);
            return true;
        } else if (preference == mLockBackground) {
            int index = mLockBackground.findIndexOfValue((String) objValue);
            handleBackgroundSelection(index);
        }
        return false;
    }

    /**
     * Checks if the device has hardware buttons.
     * @return has Buttons
     */
    public boolean hasButtons() {
        return (getResources().getInteger(
                com.android.internal.R.integer.config_deviceHardwareKeys) > 0);
    }

    /**
     * Checks if the device has torch.
     * @return has torch
     */
    public boolean hasTorch() {
        return getResources().getBoolean(com.android.internal.R.bool.config_enableTorch);
    }

    /**
     * Checks if a specific policy is disabled by a device administrator, and disables the
     * provided preference if so.
     * @param preference Preference
     * @param feature Feature
     */
    private void checkDisabledByPolicy(Preference preference, int feature) {
        boolean disabled = featureIsDisabled(feature);

        if (disabled) {
            preference.setSummary(R.string.security_enable_widgets_disabled_summary);
        }

        preference.setEnabled(!disabled);
    }

    /**
     * Checks if a specific policy is disabled by a device administrator.
     * @param feature Feature
     * @return Is disabled
     */
    private boolean featureIsDisabled(int feature) {
        return (mDPM.getKeyguardDisabledFeatures(null) & feature) != 0;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_PICK_WALLPAPER) {
            if (resultCode == Activity.RESULT_OK) {
                Uri uri = data != null ? data.getData() : null;
                if (uri == null) {
                    uri = Uri.fromFile(mTempWallpaper);
                }
                new SaveUserWallpaperTask(getActivity().getApplicationContext()).execute(uri);
            } else {
                toastLockscreenWallpaperStatus(getActivity(), false);
            }
        }
    }

    private Bitmap getBitmapFromUri(Uri uri) {
        ParcelFileDescriptor parcelFileDescriptor = null;
        try {
            parcelFileDescriptor = getContentResolver().openFileDescriptor(uri, "r");
            FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
            Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
            return image;
        } catch (IOException e) {
        } finally {
            if (parcelFileDescriptor != null) {
                try {
                    parcelFileDescriptor.close();
                } catch (IOException e) {
                }
            }
        }
        return null;
    }

    private void handleBackgroundSelection(int index) {
        if (index == LockscreenBackgroundUtil.LOCKSCREEN_STYLE_IMAGE) {
            // Launches intent for user to select an image/crop it to set as background
            final Intent intent = new Intent(Intent.ACTION_GET_CONTENT, null);
            intent.setType("image/*");
            intent.putExtra("crop", "true");
            intent.putExtra("scale", true);
            intent.putExtra("scaleUpIfNeeded", false);
            intent.putExtra("scaleType", 6);
            intent.putExtra("layout_width", -1);
            intent.putExtra("layout_height", -2);
            intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());

            final Display display = getActivity().getWindowManager().getDefaultDisplay();
            boolean isPortrait = getResources().getConfiguration().orientation ==
                    Configuration.ORIENTATION_PORTRAIT;

            Point screenDimension = new Point();
            display.getSize(screenDimension);
            int width = screenDimension.x;
            int height = screenDimension.y;

            intent.putExtra("aspectX", isPortrait ? width : height);
            intent.putExtra("aspectY", isPortrait ? height : width);

            try {
                mTempWallpaper.createNewFile();
                mTempWallpaper.setWritable(true, false);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mTempWallpaper));
                intent.putExtra("return-data", false);
                getActivity().startActivityFromFragment(this, intent, REQUEST_PICK_WALLPAPER);
            } catch (IOException e) {
                toastLockscreenWallpaperStatus(getActivity(), false);
            } catch (ActivityNotFoundException e) {
                toastLockscreenWallpaperStatus(getActivity(), false);
            }
        } else if (index == LockscreenBackgroundUtil.LOCKSCREEN_STYLE_DEFAULT) {
            // Sets background to default
            Settings.System.putInt(getContentResolver(),
                            Settings.System.LOCKSCREEN_BACKGROUND_STYLE, LockscreenBackgroundUtil.LOCKSCREEN_STYLE_DEFAULT);
            if (mWallpaper.exists()) {
                mWallpaper.delete();
            }
            updateKeyguardWallpaper(getActivity());
            updateBackgroundPreference();
        }
    }

    private static void toastLockscreenWallpaperStatus(Context context, boolean success) {
        Toast.makeText(context, context.getResources().getString(
                success ? R.string.background_result_successful
                        : R.string.background_result_not_successful),
                Toast.LENGTH_LONG).show();
    }

    private static void updateKeyguardWallpaper(Context context) {
        context.sendBroadcast(new Intent(Intent.ACTION_KEYGUARD_WALLPAPER_CHANGED));
    }

    private class SaveUserWallpaperTask extends AsyncTask<Uri, Void, Boolean> {

        private Toast mToast;
        Context mContext;

        public SaveUserWallpaperTask(Context ctx) {
            mContext = ctx;
        }

        @Override
        protected void onPreExecute() {
            mToast = Toast.makeText(getActivity(), R.string.setting_lockscreen_background,
                    Toast.LENGTH_LONG);
            mToast.show();
        }

        @Override
        protected Boolean doInBackground(Uri... params) {
            if (getActivity().isFinishing()) {
                return false;
            }
            FileOutputStream out = null;
            try {
                Bitmap wallpaper = getBitmapFromUri(params[0]);
                if (wallpaper == null) {
                    return false;
                }
                mWallpaper.createNewFile();
                mWallpaper.setReadable(true, false);
                out = new FileOutputStream(mWallpaper);
                wallpaper.compress(Bitmap.CompressFormat.JPEG, 85, out);

                if (mTempWallpaper.exists()) {
                    mTempWallpaper.delete();
                }
                return true;
            } catch (IOException e) {
            } finally {
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e) {
                    }
                }
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            mToast.cancel();
            toastLockscreenWallpaperStatus(mContext, result);
            if (result) {
                Settings.System.putInt(getContentResolver(),
                        Settings.System.LOCKSCREEN_BACKGROUND_STYLE,
                        LockscreenBackgroundUtil.LOCKSCREEN_STYLE_IMAGE);
                updateKeyguardWallpaper(mContext);
                if (!isDetached()) {
                    updateBackgroundPreference();
                }
            }
        }
    }
}
