/* 
 * Copyright (C) 2014 RR
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

package com.android.settings.rr;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import com.android.settings.rr.SeekBarPreference;
import com.android.settings.rr.SeekBarPreferenceCham;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.internal.logging.MetricsLogger;

import net.margaritov.preference.colorpicker.ColorPickerPreference;

public class NotificationColorSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String PREF_CAT_COLORS =
            "notification_cat_colors";
    private static final String PREF_MEDIA_BG_MODE =
            "notification_media_bg_mode";
    private static final String PREF_APP_ICON_BG_MODE =
            "notification_app_icon_bg_mode";
    private static final String PREF_APP_ICON_COLOR_MODE =
            "notification_app_icon_color_mode";
    private static final String PREF_BG_COLOR =
            "notification_bg_color";
    private static final String PREF_BG_GUTS_COLOR =
            "notification_bg_guts_color";
    private static final String PREF_APP_ICON_BG_COLOR =
            "notification_app_icon_bg_color";
    private static final String PREF_ICON_COLOR =
            "notification_icon_color";
    private static final String PREF_CLEAR_ALL_ICON_COLOR =
            "notification_drawer_clear_all_icon_color";
    private static final String PREF_QS_TRANSPARENT_SHADE = "qs_transparent_shade";
    private static final String PREF_QS_TRANSPARENT_HEADER = "qs_transparent_header";
    private static final String PREF_TRANSPARENT_VOLUME_DIALOG = "transparent_volume_dialog";
    private static final String PREF_TRANSPARENT_POWER_MENU = "transparent_power_menu";
    private static final String PREF_TRANSPARENT_POWER_DIALOG_DIM = "transparent_power_dialog_dim";

    private static final int RR_BLUE_GREY = 0xff1b1f23;
    private static final int SYSTEMUI_SECONDARY = 0xff384248;
    private static final int WHITE = 0xffffffff;
    private static final int BLACK = 0xff000000;
    private static final int HOLO_BLUE_LIGHT = 0xff33b5e5;
    private static final int TRANSLUCENT_HOLO_BLUE_LIGHT = 0x4d33b5e5;
    private static final int TRANSLUCENT_WHITE = 0x4dffffff;

    private static final int MENU_RESET = Menu.FIRST;
    private static final int DLG_RESET = 0;

    private ListPreference mMediaBgMode;
    private ListPreference mAppIconBgMode;
    private ListPreference mAppIconColorMode;
    private ColorPickerPreference mBgColor;
    private ColorPickerPreference mBgGutsColor;
    private ColorPickerPreference mAppIconBgColor;
    private ColorPickerPreference mIconColor;
    private ColorPickerPreference mClearAllIconColor;
    private SeekBarPreference mQSShadeAlpha;
    private SeekBarPreferenceCham mQSHeaderAlpha;	
    private SeekBarPreferenceCham mVolumeDialogAlpha;	
    private SeekBarPreferenceCham mPowerMenuAlpha;
    private SeekBarPreferenceCham mPowerDialogDim;	
  
    private ContentResolver mResolver;

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.APPLICATION;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        refreshSettings();
    }

    public void refreshSettings() {
        PreferenceScreen prefs = getPreferenceScreen();
        if (prefs != null) {
            prefs.removeAll();
        }

        addPreferencesFromResource(R.xml.notification_colors);
        mResolver = getActivity().getContentResolver();

        int intColor;
        String hexColor;

        mMediaBgMode = (ListPreference) findPreference(PREF_MEDIA_BG_MODE);
        int mediaBgMode = Settings.System.getInt(mResolver,
                Settings.System.NOTIFICATION_MEDIA_BG_MODE, 0);
        mMediaBgMode.setValue(String.valueOf(mediaBgMode));
        mMediaBgMode.setSummary(mMediaBgMode.getEntry());
        mMediaBgMode.setOnPreferenceChangeListener(this);

        mBgColor =
                (ColorPickerPreference) findPreference(PREF_BG_COLOR);
        intColor = Settings.System.getInt(mResolver,
                Settings.System.NOTIFICATION_BG_COLOR, WHITE); 
        mBgColor.setNewPreviewColor(intColor);
        hexColor = String.format("#%08x", (0xffffffff & intColor));
        mBgColor.setSummary(hexColor);
        mBgColor.setDefaultColors(RR_BLUE_GREY, RR_BLUE_GREY);
        mBgColor.setOnPreferenceChangeListener(this);

        mBgGutsColor =
                (ColorPickerPreference) findPreference(PREF_BG_GUTS_COLOR);
        intColor = Settings.System.getInt(mResolver,
                Settings.System.NOTIFICATION_GUTS_BG_COLOR, SYSTEMUI_SECONDARY); 
        mBgGutsColor.setNewPreviewColor(intColor);
        hexColor = String.format("#%08x", (0xffffffff & intColor));
        mBgGutsColor.setSummary(hexColor);
        mBgGutsColor.setDefaultColors(SYSTEMUI_SECONDARY, SYSTEMUI_SECONDARY);
        mBgGutsColor.setOnPreferenceChangeListener(this);

        PreferenceCategory colorCat =
                (PreferenceCategory) findPreference(PREF_CAT_COLORS);

        mClearAllIconColor =
                (ColorPickerPreference) findPreference(PREF_CLEAR_ALL_ICON_COLOR);
        intColor = Settings.System.getInt(mResolver,
                Settings.System.NOTIFICATION_DRAWER_CLEAR_ALL_ICON_COLOR, WHITE); 
        mClearAllIconColor.setNewPreviewColor(intColor);
        hexColor = String.format("#%08x", (0xffffffff & intColor));
        mClearAllIconColor.setSummary(hexColor);
        mClearAllIconColor.setDefaultColors(WHITE, HOLO_BLUE_LIGHT);
        mClearAllIconColor.setOnPreferenceChangeListener(this);

	// QS shade alpha
        mQSShadeAlpha =
                (SeekBarPreference) findPreference(PREF_QS_TRANSPARENT_SHADE);
        int qSShadeAlpha = Settings.System.getInt(mResolver,
                Settings.System.QS_TRANSPARENT_SHADE, 255);
        mQSShadeAlpha.setValue(qSShadeAlpha / 1);
        mQSShadeAlpha.setOnPreferenceChangeListener(this);

        setHasOptionsMenu(true);

 	// QS header alpha
            mQSHeaderAlpha =
                    (SeekBarPreferenceCham) findPreference(PREF_QS_TRANSPARENT_HEADER);
            int qSHeaderAlpha = Settings.System.getInt(mResolver,
                    Settings.System.QS_TRANSPARENT_HEADER, 255);
            mQSHeaderAlpha.setValue(qSHeaderAlpha / 1);
            mQSHeaderAlpha.setOnPreferenceChangeListener(this);

 // Volume dialog alpha
            mVolumeDialogAlpha =
                    (SeekBarPreferenceCham) findPreference(PREF_TRANSPARENT_VOLUME_DIALOG);
            int volumeDialogAlpha = Settings.System.getInt(mResolver,
                    Settings.System.TRANSPARENT_VOLUME_DIALOG, 255);
            mVolumeDialogAlpha.setValue(volumeDialogAlpha / 1);
            mVolumeDialogAlpha.setOnPreferenceChangeListener(this);

            // Power menu alpha
            mPowerMenuAlpha =
                    (SeekBarPreferenceCham) findPreference(PREF_TRANSPARENT_POWER_MENU);
            int powerMenuAlpha = Settings.System.getInt(mResolver,
                    Settings.System.TRANSPARENT_POWER_MENU, 100);
            mPowerMenuAlpha.setValue(powerMenuAlpha / 1);
            mPowerMenuAlpha.setOnPreferenceChangeListener(this);

       // Power/reboot dialog dim
            mPowerDialogDim =
                    (SeekBarPreferenceCham) findPreference(PREF_TRANSPARENT_POWER_DIALOG_DIM);
            int powerDialogDim = Settings.System.getInt(mResolver,
                    Settings.System.TRANSPARENT_POWER_DIALOG_DIM, 50);
            mPowerDialogDim.setValue(powerDialogDim / 1);
            mPowerDialogDim.setOnPreferenceChangeListener(this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, MENU_RESET, 0, R.string.reset)
                .setIcon(R.drawable.ic_action_reset) // use the KitKat backup icon
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_RESET:
                showDialogInner(DLG_RESET);
                return true;
             default:
                return super.onContextItemSelected(item);
        }
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean value;
        String hex;
        int intHex;

        if (preference == mMediaBgMode) {
            int mediaBgMode = Integer.valueOf((String) newValue);
            int index = mMediaBgMode.findIndexOfValue((String) newValue);
            Settings.System.putInt(mResolver,
                Settings.System.NOTIFICATION_MEDIA_BG_MODE, mediaBgMode);
            preference.setSummary(mMediaBgMode.getEntries()[index]);
            return true;
        } else if (preference == mAppIconBgMode) {
            int appIconBgMode = Integer.valueOf((String) newValue);
            int index = mAppIconBgMode.findIndexOfValue((String) newValue);
            Settings.System.putInt(mResolver,
                Settings.System.NOTIFICATION_APP_ICON_BG_MODE, appIconBgMode);
            preference.setSummary(mAppIconBgMode.getEntries()[index]);
            refreshSettings();
            return true;
        } else if (preference == mAppIconColorMode) {
            int appIconColorMode = Integer.valueOf((String) newValue);
            int index = mAppIconColorMode.findIndexOfValue((String) newValue);
            Settings.System.putInt(mResolver,
                Settings.System.NOTIFICATION_APP_ICON_COLOR_MODE, appIconColorMode);
            preference.setSummary(mAppIconColorMode.getEntries()[index]);
            return true;
        } else if (preference == mBgColor) {
            hex = ColorPickerPreference.convertToARGB(
                Integer.valueOf(String.valueOf(newValue)));
            intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(mResolver,
                Settings.System.NOTIFICATION_BG_COLOR, intHex);
            preference.setSummary(hex);
            return true;
        } else if (preference == mBgGutsColor) {
            hex = ColorPickerPreference.convertToARGB(
                Integer.valueOf(String.valueOf(newValue)));
            intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(mResolver,
                Settings.System.NOTIFICATION_GUTS_BG_COLOR, intHex);
            preference.setSummary(hex);
            return true;
        } else if (preference == mAppIconBgColor) {
            hex = ColorPickerPreference.convertToARGB(
                Integer.valueOf(String.valueOf(newValue)));
            intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(mResolver,
                Settings.System.NOTIFICATION_APP_ICON_BG_COLOR, intHex);
            preference.setSummary(hex);
            return true;
        } else if (preference == mIconColor) {
            hex = ColorPickerPreference.convertToARGB(
                Integer.valueOf(String.valueOf(newValue)));
            intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(mResolver,
                Settings.System.NOTIFICATION_ICON_COLOR, intHex);
            preference.setSummary(hex);
            return true;
        } else if (preference == mClearAllIconColor) {
            hex = ColorPickerPreference.convertToARGB(
                Integer.valueOf(String.valueOf(newValue)));
            intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(mResolver,
                Settings.System.NOTIFICATION_DRAWER_CLEAR_ALL_ICON_COLOR, intHex);
            preference.setSummary(hex);
            return true;
        } else if (preference == mQSShadeAlpha) {
            int alpha = (Integer) newValue;
            Settings.System.putInt(mResolver,
                    Settings.System.QS_TRANSPARENT_SHADE, alpha * 1);
            return true;
	}  else if (preference == mQSHeaderAlpha) {
                int alpha = (Integer) newValue;
                Settings.System.putInt(mResolver,
                        Settings.System.QS_TRANSPARENT_HEADER, alpha * 1);
                return true;
	}  else if (preference == mVolumeDialogAlpha) {
                int alpha = (Integer) newValue;
                Settings.System.putInt(mResolver,
                        Settings.System.TRANSPARENT_VOLUME_DIALOG, alpha * 1);
                return true;
	} else if (preference == mPowerMenuAlpha) {
		int alpha = (Integer) newValue;
                Settings.System.putInt(mResolver,
                        Settings.System.TRANSPARENT_POWER_MENU, alpha * 1);
                return true;
	}else if (preference == mPowerDialogDim) {
		int alpha = (Integer) newValue;
                Settings.System.putInt(mResolver,
                        Settings.System.TRANSPARENT_POWER_DIALOG_DIM, alpha * 1);
                return true;
	}
        return false;
    }

    private void showDialogInner(int id) {
        DialogFragment newFragment = MyAlertDialogFragment.newInstance(id);
        newFragment.setTargetFragment(this, 0);
        newFragment.show(getFragmentManager(), "dialog " + id);
    }

    public static class MyAlertDialogFragment extends DialogFragment {

        public static MyAlertDialogFragment newInstance(int id) {
            MyAlertDialogFragment frag = new MyAlertDialogFragment();
            Bundle args = new Bundle();
            args.putInt("id", id);
            frag.setArguments(args);
            return frag;
        }

        NotificationColorSettings getOwner() {
            return (NotificationColorSettings) getTargetFragment();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int id = getArguments().getInt("id");
            switch (id) {
                case DLG_RESET:
                    return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.reset)
                    .setMessage(R.string.dlg_reset_values_message)
                    .setNegativeButton(R.string.cancel, null)
                    .setNeutralButton(R.string.dlg_reset_android,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.NOTIFICATION_MEDIA_BG_MODE, 0);
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.NOTIFICATION_APP_ICON_BG_MODE, 0);
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.NOTIFICATION_APP_ICON_COLOR_MODE, 0);
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.NOTIFICATION_BG_COLOR,
                                    WHITE);
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.NOTIFICATION_GUTS_BG_COLOR,
                                    SYSTEMUI_SECONDARY);
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.NOTIFICATION_APP_ICON_BG_COLOR,
                                    TRANSLUCENT_WHITE);
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.NOTIFICATION_TEXT_COLOR, BLACK);
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.NOTIFICATION_ICON_COLOR, BLACK);
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.NOTIFICATION_DRAWER_CLEAR_ALL_ICON_COLOR,
                                    WHITE);
                            getOwner().refreshSettings();
                        }
                    })
                    .setPositiveButton(R.string.dlg_reset_rr,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.NOTIFICATION_MEDIA_BG_MODE, 1);
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.NOTIFICATION_APP_ICON_BG_MODE, 1);
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.NOTIFICATION_APP_ICON_COLOR_MODE, 1);
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.NOTIFICATION_BG_COLOR,
                                    WHITE);
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.NOTIFICATION_GUTS_BG_COLOR,
                                    SYSTEMUI_SECONDARY);
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.NOTIFICATION_APP_ICON_BG_COLOR,
                                    TRANSLUCENT_HOLO_BLUE_LIGHT);
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.NOTIFICATION_TEXT_COLOR,
                                    HOLO_BLUE_LIGHT);
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.NOTIFICATION_ICON_COLOR,
                                    HOLO_BLUE_LIGHT);
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.NOTIFICATION_DRAWER_CLEAR_ALL_ICON_COLOR,
                                    HOLO_BLUE_LIGHT);
                            getOwner().refreshSettings();
                        }
                    })
                    .create();
            }
            throw new IllegalArgumentException("unknown id " + id);
        }

        @Override
        public void onCancel(DialogInterface dialog) {

        }
    }
}

