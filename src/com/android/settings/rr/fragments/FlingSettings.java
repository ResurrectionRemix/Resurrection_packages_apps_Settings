/*
 * Copyright (C) 2014 TeamEos
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

package com.android.settings.rr.fragments;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;

import net.margaritov.preference.colorpicker.ColorPickerPreference;

import com.android.settings.rr.Preferences.CustomSeekBarPreference;

import com.android.internal.logging.nano.MetricsProto;
import com.android.internal.utils.du.ActionConstants;
import com.android.internal.utils.du.ActionHandler;
import com.android.internal.utils.du.DUActionUtils;
import com.android.internal.utils.du.Config.ButtonConfig;
import com.android.settings.R;
import com.android.settings.rr.IconPickHelper;
import com.android.settings.rr.Preferences.ActionPreference;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserHandle;
import android.support.v7.preference.PreferenceScreen;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class FlingSettings extends ActionFragment implements
        Preference.OnPreferenceChangeListener, IconPickHelper.OnPickListener {
    private static final String TAG = FlingSettings.class.getSimpleName();
    public static final String FLING_LOGO_URI = "fling_custom_icon_config";

    private static final int MENU_RESET = Menu.FIRST;
    private static final int MENU_SAVE = Menu.FIRST + 1;
    private static final int MENU_RESTORE = Menu.FIRST + 2;

    private static final int DIALOG_RESET_CONFIRM = 1;
    private static final int DIALOG_RESTORE_PROFILE = 2;
    private static final int DIALOG_SAVE_PROFILE = 3;
    private static final String CONFIG_STORAGE = Environment.getExternalStorageDirectory()
            + File.separator
            + "fling_configs";
    private static final String FLING_CONFIGS_PREFIX = "fling_config_";
    private static final String KEY_FLING_BACKUP = "fling_profile_save";
    private static final String KEY_FLING_RESTORE = "fling_profile_restore";

    Context mContext;
    IconPickHelper mIconPickHelper;
    boolean mIsTablet;

    SwitchPreference mShowLogo;
    SwitchPreference mAnimateLogo;
    SwitchPreference mShowRipple;
    SwitchPreference mTrailsEnabled;
    SwitchPreference mKbCursors;

    CustomSeekBarPreference mTrailsWidth;
    CustomSeekBarPreference mLongPressTimeout;

    CustomSeekBarPreference mLogoOpacity;

    CustomSeekBarPreference mSwipePortRight;
    CustomSeekBarPreference mSwipePortLeft;
    CustomSeekBarPreference mSwipeLandRight;
    CustomSeekBarPreference mSwipeLandLeft;
    CustomSeekBarPreference mSwipeVertUp;
    CustomSeekBarPreference mSwipeVertDown;

    ColorPickerPreference mRippleColor;
    ColorPickerPreference mTrailsColor;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.fling_settings);

        mFooterPreferenceMixin.createFooterPreference().setTitle(R.string.fling_back_home_policy);

        mContext = (Context) getActivity();
        mIconPickHelper = new IconPickHelper(getActivity(), this);

        mShowLogo = (SwitchPreference) findPreference("eos_fling_show_logo");
        mShowLogo.setChecked(Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.FLING_LOGO_VISIBLE, 1) == 1);
        mShowLogo.setOnPreferenceChangeListener(this);

        mAnimateLogo = (SwitchPreference) findPreference("eos_fling_animate_logo");
        mAnimateLogo.setChecked(Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.FLING_LOGO_ANIMATES, 1) == 1);
        mAnimateLogo.setOnPreferenceChangeListener(this);

        mShowRipple = (SwitchPreference) findPreference("eos_fling_show_ripple");
        mShowRipple.setChecked(Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.FLING_RIPPLE_ENABLED, 1) == 1);
        mShowRipple.setOnPreferenceChangeListener(this);

        int rippleColor = Settings.Secure.getIntForUser(getContentResolver(),
                Settings.Secure.FLING_RIPPLE_COLOR, Color.WHITE, UserHandle.USER_CURRENT);
        mRippleColor = (ColorPickerPreference) findPreference("eos_fling_ripple_color");
        mRippleColor.setNewPreviewColor(rippleColor);
        mRippleColor.setOnPreferenceChangeListener(this);

        mTrailsEnabled = (SwitchPreference) findPreference("eos_fling_trails_enable");
        mTrailsEnabled.setChecked(Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.FLING_TRAILS_ENABLED, 1) == 1);
        mTrailsEnabled.setOnPreferenceChangeListener(this);

        int trailsColor = Settings.Secure.getIntForUser(getContentResolver(),
                Settings.Secure.FLING_TRAILS_COLOR, Color.WHITE, UserHandle.USER_CURRENT);
        mTrailsColor = (ColorPickerPreference) findPreference("eos_fling_trails_color");
        mTrailsColor.setNewPreviewColor(trailsColor);
        mTrailsColor.setOnPreferenceChangeListener(this);

        mTrailsWidth = (CustomSeekBarPreference) findPreference("du_fling_trails_width");
        int width = Settings.Secure.getIntForUser(getContentResolver(),
                Settings.Secure.FLING_TRAILS_WIDTH, 15, UserHandle.USER_CURRENT);
        mTrailsWidth.setValue(width);
        mTrailsWidth.setOnPreferenceChangeListener(this);

        // NOTE: we display to the user actual timeouts starting from touch event
        // but framework wants the value less tap timeout, which is 100ms
        // so we always write 100ms less but display 100ms more
        mLongPressTimeout = (CustomSeekBarPreference) findPreference("du_fling_longpress_pref");
        int val = Settings.Secure.getIntForUser(getContentResolver(),
                Settings.Secure.FLING_LONGPRESS_TIMEOUT, 250, UserHandle.USER_CURRENT);
        val += 100;
        mLongPressTimeout.setValue(val);
        mLongPressTimeout.setOnPreferenceChangeListener(this);

        mIsTablet = !DUActionUtils.navigationBarCanMove();

        mSwipePortRight = (CustomSeekBarPreference) findPreference("du_fling_longswipe_port_right");
        val = Settings.Secure.getIntForUser(
                getContentResolver(), Settings.Secure.FLING_LONGSWIPE_THRESHOLD_RIGHT_PORT,
                mIsTablet ? 30 : 40, UserHandle.USER_CURRENT);
        mSwipePortRight.setValue(val);
        mSwipePortRight.setOnPreferenceChangeListener(this);

        mSwipePortLeft = (CustomSeekBarPreference) findPreference("du_fling_longswipe_port_left");
        val = Settings.Secure.getIntForUser(
                getContentResolver(), Settings.Secure.FLING_LONGSWIPE_THRESHOLD_LEFT_PORT,
                mIsTablet ? 30 : 40, UserHandle.USER_CURRENT);
        mSwipePortLeft.setValue(val);
        mSwipePortLeft.setOnPreferenceChangeListener(this);

        mSwipeLandRight = (CustomSeekBarPreference) findPreference("du_fling_longswipe_land_right");
        mSwipeLandLeft = (CustomSeekBarPreference) findPreference("du_fling_longswipe_land_left");
        mSwipeVertUp = (CustomSeekBarPreference) findPreference("du_fling_longswipe_vert_up");
        mSwipeVertDown = (CustomSeekBarPreference) findPreference("du_fling_longswipe_vert_down");

        PreferenceCategory longSwipeCategory = (PreferenceCategory) getPreferenceScreen()
                .findPreference("eos_long_swipe_category");

        if (mIsTablet) {
            longSwipeCategory.removePreference(mSwipeVertUp);
            longSwipeCategory.removePreference(mSwipeVertDown);
            val = Settings.Secure.getIntForUser(
                    getContentResolver(), Settings.Secure.FLING_LONGSWIPE_THRESHOLD_RIGHT_LAND,
                    25, UserHandle.USER_CURRENT);
            mSwipeLandRight.setValue(val);
            mSwipeLandRight.setOnPreferenceChangeListener(this);

            val = Settings.Secure.getIntForUser(
                    getContentResolver(), Settings.Secure.FLING_LONGSWIPE_THRESHOLD_LEFT_LAND,
                    25, UserHandle.USER_CURRENT);
            mSwipeLandLeft.setValue(val);
            mSwipeLandLeft.setOnPreferenceChangeListener(this);
        } else {
            longSwipeCategory.removePreference(mSwipeLandRight);
            longSwipeCategory.removePreference(mSwipeLandLeft);
            val = Settings.Secure.getIntForUser(
                    getContentResolver(), Settings.Secure.FLING_LONGSWIPE_THRESHOLD_UP_LAND,
                    40, UserHandle.USER_CURRENT);
            mSwipeVertUp.setValue(val);
            mSwipeVertUp.setOnPreferenceChangeListener(this);

            val = Settings.Secure.getIntForUser(
                    getContentResolver(), Settings.Secure.FLING_LONGSWIPE_THRESHOLD_DOWN_LAND,
                    40, UserHandle.USER_CURRENT);
            mSwipeVertDown.setValue(val);
            mSwipeVertDown.setOnPreferenceChangeListener(this);
        }

        mKbCursors = (SwitchPreference) findPreference("fling_keyboard_cursors");
        mKbCursors.setChecked(Settings.Secure.getIntForUser(getContentResolver(),
                Settings.Secure.FLING_KEYBOARD_CURSORS, 1,
                UserHandle.USER_CURRENT) == 1);
        mKbCursors.setOnPreferenceChangeListener(this);

        mLogoOpacity = (CustomSeekBarPreference) findPreference("fling_logo_opacity");
        int alpha = Settings.Secure.getIntForUser(getContentResolver(),
                Settings.Secure.FLING_LOGO_OPACITY, 255,
                UserHandle.USER_CURRENT);
        mLogoOpacity.setValue(alpha);
        mLogoOpacity.setOnPreferenceChangeListener(this);

        onPreferenceScreenLoaded(ActionConstants.getDefaults(ActionConstants.FLING));

        setHasOptionsMenu(true);
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        switch (dialogId) {
            case DIALOG_RESET_CONFIRM: {
                Dialog dialog;
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
                alertDialog.setTitle(R.string.fling_factory_reset_title);
                alertDialog.setMessage(R.string.fling_factory_reset_confirm);
                alertDialog.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        resetFling();
                    }
                });
                alertDialog.setNegativeButton(R.string.write_settings_off, null);
                dialog = alertDialog.create();
                return dialog;
            }
            case DIALOG_RESTORE_PROFILE: {
                Dialog dialog;
                final ConfigAdapter configAdapter = new ConfigAdapter(getActivity(),
                        getConfigFiles(CONFIG_STORAGE));
                AlertDialog.Builder configDialog = new AlertDialog.Builder(getActivity());
                configDialog.setTitle(R.string.fling_config_dialog_title);
                configDialog.setNegativeButton(getString(android.R.string.cancel), null);
                configDialog.setAdapter(configAdapter, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        String resultMsg;
                        try {
                            File configFile = (File) configAdapter.getItem(item);
                            String config = getFlingConfigFromStorage(configFile);
                            restoreConfig(getActivity(), config);
                            loadAndSetConfigs();
                            onActionPolicyEnforced(mPrefHolder);
                            resultMsg = getString(R.string.fling_config_restore_success_toast);
                        } catch (Exception e) {
                            resultMsg = getString(R.string.fling_config_restore_error_toast);
                        }
                        Toast.makeText(getActivity(), resultMsg, Toast.LENGTH_SHORT).show();
                    }
                });
                dialog = configDialog.create();
                return dialog;
            }
            case DIALOG_SAVE_PROFILE: {
                Dialog dialog;
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                final EditText input = new EditText(getActivity());
                builder.setTitle(getString(R.string.fling_config_name_edit_dialog_title));
                builder.setMessage(R.string.fling_config_name_edit_dialog_message);
                builder.setView(input);
                builder.setNegativeButton(getString(android.R.string.cancel), null);
                builder.setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                String inputText = input.getText().toString();
                                if (TextUtils.isEmpty(inputText)) {
                                    inputText = String.valueOf(android.text.format.DateFormat
                                            .format("yyyy-MM-dd_hh:mm:ss", new java.util.Date()));
                                }
                                String resultMsg;
                                try {
                                    String currentConfig = getCurrentConfig(getActivity());
                                    backupFlingConfig(currentConfig, inputText);
                                    resultMsg = getString(R.string.fling_config_backup_success_toast);
                                } catch (Exception e) {
                                    resultMsg = getString(R.string.fling_config_backup_error_toast);
                                }
                                Toast.makeText(getActivity(), resultMsg, Toast.LENGTH_SHORT).show();
                            }
                        });
                dialog = builder.create();
                dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                return dialog;
            }
            default: {
                return super.onCreateDialog(dialogId);
            }
        }
    }

    @Override
    public int getDialogMetricsCategory(int dialogId) {
        switch (dialogId) {
            case DIALOG_RESET_CONFIRM:
            case DIALOG_RESTORE_PROFILE:
            case DIALOG_SAVE_PROFILE:
                return MetricsProto.MetricsEvent.RESURRECTED;
            default:
                return super.getDialogMetricsCategory(dialogId);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, MENU_RESET, 0, R.string.reset)
                .setIcon(com.android.internal.R.drawable.ic_menu_refresh)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.add(0, MENU_SAVE, 0, R.string.fling_backup_current_config_title)
                .setIcon(R.drawable.ic_fling_save_profile)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.add(0, MENU_RESTORE, 0, R.string.fling_restore_config_title)
                .setIcon(R.drawable.ic_fling_restore_profile)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_RESET:
                showDialog(DIALOG_RESET_CONFIRM);
                return true;
            case MENU_SAVE:
                showDialog(DIALOG_SAVE_PROFILE);
                return true;
            case MENU_RESTORE:
                showDialog(DIALOG_RESTORE_PROFILE);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    private void resetFling() {
        restoreConfig(getActivity(), getDefaultConfig(getActivity()));
        loadAndSetConfigs();
        onActionPolicyEnforced(mPrefHolder);

        ButtonConfig logoConfig = ButtonConfig.getButton(mContext, FLING_LOGO_URI, true);
        logoConfig.clearCustomIconIconUri();
        ButtonConfig.setButton(mContext, logoConfig, FLING_LOGO_URI, true);

        Settings.Secure.putInt(getContentResolver(),
                Settings.Secure.FLING_LOGO_VISIBLE, 1);
        mShowLogo.setChecked(true);
        mShowLogo.setOnPreferenceChangeListener(this);

        Settings.Secure.putInt(getContentResolver(),
                Settings.Secure.FLING_LOGO_ANIMATES, 1);
        mAnimateLogo.setChecked(true);
        mAnimateLogo.setOnPreferenceChangeListener(this);

        Settings.Secure.putInt(getContentResolver(),
                Settings.Secure.FLING_RIPPLE_ENABLED, 1);
        mShowRipple.setChecked(true);
        mShowRipple.setOnPreferenceChangeListener(this);

        Settings.Secure.putInt(getContentResolver(),
                Settings.Secure.FLING_RIPPLE_COLOR, Color.WHITE);
        mRippleColor.setNewPreviewColor(Color.WHITE);
        mRippleColor.setOnPreferenceChangeListener(this);

        Settings.Secure.putInt(getContentResolver(),
                Settings.Secure.FLING_LONGPRESS_TIMEOUT, 250);
        mLongPressTimeout.setValue(250+100);
        mLongPressTimeout.setOnPreferenceChangeListener(this);

        Settings.Secure.putInt(getContentResolver(),
                Settings.Secure.FLING_LONGSWIPE_THRESHOLD_RIGHT_PORT, mIsTablet ? 30 : 40);
        mSwipePortRight.setValue(mIsTablet ? 30 : 40);
        mSwipePortRight.setOnPreferenceChangeListener(this);

        Settings.Secure.putInt(getContentResolver(),
                Settings.Secure.FLING_LONGSWIPE_THRESHOLD_LEFT_PORT, mIsTablet ? 30 : 40);
        mSwipePortLeft.setValue(mIsTablet ? 30 : 40);
        mSwipePortLeft.setOnPreferenceChangeListener(this);

        Settings.Secure.putInt(getContentResolver(),
                Settings.Secure.FLING_LONGSWIPE_THRESHOLD_RIGHT_LAND, 25);
        mSwipeLandRight.setValue(25);
        mSwipeLandRight.setOnPreferenceChangeListener(this);

        Settings.Secure.putInt(getContentResolver(),
                Settings.Secure.FLING_LONGSWIPE_THRESHOLD_LEFT_LAND, 25);
        mSwipeLandLeft.setValue(25);
        mSwipeLandLeft.setOnPreferenceChangeListener(this);

        Settings.Secure.putInt(getContentResolver(),
                Settings.Secure.FLING_LONGSWIPE_THRESHOLD_UP_LAND, 40);
        mSwipeVertUp.setValue(40);
        mSwipeVertUp.setOnPreferenceChangeListener(this);

        Settings.Secure.putInt(getContentResolver(),
                Settings.Secure.FLING_LONGSWIPE_THRESHOLD_DOWN_LAND, 40);
        mSwipeVertDown.setValue(40);
        mSwipeVertDown.setOnPreferenceChangeListener(this);

        Settings.Secure.putInt(getContentResolver(),
                Settings.Secure.FLING_KEYBOARD_CURSORS, 1);
        mKbCursors.setChecked(true);
        mKbCursors.setOnPreferenceChangeListener(this);

        Settings.Secure.putInt(getContentResolver(),
                Settings.Secure.FLING_LOGO_OPACITY, 255);
        mLogoOpacity.setValue(255);
        mLogoOpacity.setOnPreferenceChangeListener(this);
    }

    static class ConfigAdapter extends ArrayAdapter<File> {
        private final ArrayList<File> mConfigFiles;
        private final Context mContext;

        public ConfigAdapter(Context context, ArrayList<File> files) {
            super(context, android.R.layout.select_dialog_item, files);
            mContext = context;
            mConfigFiles = files;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            View itemRow = convertView;
            File f = mConfigFiles.get(position);
            itemRow = ((LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                    .inflate(android.R.layout.select_dialog_item, null);
            String name = f.getName();
            if (name.startsWith(FLING_CONFIGS_PREFIX)) {
                name = f.getName().substring(FLING_CONFIGS_PREFIX.length(), f.getName().length());
            }
            ((TextView) itemRow.findViewById(android.R.id.text1)).setText(name);

            return itemRow;
        }
    }

    private static class StartsWithFilter implements FileFilter {
        private String[] mStartsWith;

        public StartsWithFilter(String[] startsWith) {
            mStartsWith = startsWith;
        }

        @Override
        public boolean accept(File file) {
            for (String extension : mStartsWith) {
                if (file.getName().toLowerCase().startsWith(extension)) {
                    return true;
                }
            }
            return false;
        }
    }

    static String getCurrentConfig(Context ctx) {
        String config = Settings.Secure.getStringForUser(
                ctx.getContentResolver(), ActionConstants.getDefaults(ActionConstants.FLING)
                        .getUri(),
                UserHandle.USER_CURRENT);
        if (TextUtils.isEmpty(config)) {
            config = getDefaultConfig(ctx);
        }
        return config;
    }

    static String getDefaultConfig(Context ctx) {
        return ActionConstants.getDefaults(ActionConstants.FLING).getDefaultConfig();
    }

    static void restoreConfig(Context context, String config) {
        Settings.Secure.putStringForUser(context.getContentResolver(),
                ActionConstants.getDefaults(ActionConstants.FLING)
                        .getUri(), config,
                UserHandle.USER_CURRENT);
    }

    static void backupFlingConfig(String config, String suffix) {
        File dir = new File(CONFIG_STORAGE);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File configFile = new File(dir, FLING_CONFIGS_PREFIX + suffix);
        FileOutputStream stream;
        try {
            stream = new FileOutputStream(configFile);
            stream.write(config.getBytes());
            stream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String getFlingConfigFromStorage(File file) {
        int length = (int) file.length();
        byte[] bytes = new byte[length];
        FileInputStream in;
        try {
            in = new FileInputStream(file);
            in.read(bytes);
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        String contents = new String(bytes);
        return contents;
    }

    public static ArrayList<File> getConfigFiles(String path) {
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        ArrayList<File> list = new ArrayList<File>();
        for (File tmp : dir.listFiles(new StartsWithFilter(new String[] {
                FLING_CONFIGS_PREFIX
        }))) {
            list.add(tmp);
        }
        return list;
    }

    @Override
    public void iconPicked(String iconType, String iconPackage, String iconName) {
        if (TextUtils.isEmpty(iconType)
                || TextUtils.isEmpty(iconPackage)
                || TextUtils.isEmpty(iconName)) {
            return;
        }
        ButtonConfig logoConfig = ButtonConfig.getButton(mContext, FLING_LOGO_URI, true);
        logoConfig.setCustomIconUri(iconType, iconPackage, iconName);
        ButtonConfig.setButton(mContext, logoConfig, FLING_LOGO_URI, true);
    }

    @Override
    public void imagePicked(Uri uri) {
        if (uri != null) {
            ButtonConfig logoConfig = ButtonConfig.getButton(mContext, FLING_LOGO_URI, true);
            logoConfig.setCustomImageUri(uri);
            ButtonConfig.setButton(mContext, logoConfig, FLING_LOGO_URI, true);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        mIconPickHelper.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference == findPreference("fling_custom_logo_pick")) {
            mIconPickHelper.pickIcon(getId(), IconPickHelper.REQUEST_PICK_ICON_PACK);
            return true;
        } else if (preference == findPreference("fling_custom_logo_reset")) {
            ButtonConfig logoConfig = ButtonConfig.getButton(mContext, FLING_LOGO_URI, true);
            logoConfig.clearCustomIconIconUri();
            ButtonConfig.setButton(mContext, logoConfig, FLING_LOGO_URI, true);
            return true;
        } else if (preference == findPreference("fling_custom_logo_gallery_pick")) {
            mIconPickHelper.pickIcon(getId(), IconPickHelper.REQUEST_PICK_ICON_GALLERY);
            return true;
        }

        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference.equals(mShowLogo)) {
            boolean enabled = ((Boolean) newValue).booleanValue();
            Settings.Secure.putInt(getContentResolver(),
                    Settings.Secure.FLING_LOGO_VISIBLE, enabled ? 1 : 0);
            return true;
        } else if (preference.equals(mAnimateLogo)) {
            boolean enabled = ((Boolean) newValue).booleanValue();
            Settings.Secure.putInt(getContentResolver(),
                    Settings.Secure.FLING_LOGO_ANIMATES, enabled ? 1 : 0);
            return true;
        } else if (preference.equals(mShowRipple)) {
            boolean enabled = ((Boolean) newValue).booleanValue();
            Settings.Secure.putInt(getContentResolver(),
                    Settings.Secure.FLING_RIPPLE_ENABLED, enabled ? 1 : 0);
            return true;
        } else if (preference.equals(mRippleColor)) {
            int color = ((Integer) newValue).intValue();
            Settings.Secure.putInt(getContentResolver(),
                    Settings.Secure.FLING_RIPPLE_COLOR, color);
            return true;
        } else if (preference.equals(mTrailsEnabled)) {
            boolean enabled = ((Boolean) newValue).booleanValue();
            Settings.Secure.putInt(getContentResolver(),
                    Settings.Secure.FLING_TRAILS_ENABLED, enabled ? 1 : 0);
            return true;
        } else if (preference.equals(mTrailsColor)) {
            int color = ((Integer) newValue).intValue();
            Settings.Secure.putInt(getContentResolver(),
                    Settings.Secure.FLING_TRAILS_COLOR, color);
            return true;
        } else if (preference == mTrailsWidth) {
            int val = (Integer) newValue;
            Settings.Secure.putIntForUser(getContentResolver(),
                    Settings.Secure.FLING_TRAILS_WIDTH, val, UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mLongPressTimeout) {
            int val = (Integer) newValue;
            val -= 100;
            Settings.Secure.putIntForUser(getContentResolver(),
                    Settings.Secure.FLING_LONGPRESS_TIMEOUT, val, UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mSwipePortRight) {
            int val = (Integer) newValue;
            Settings.Secure.putIntForUser(getContentResolver(),
                    Settings.Secure.FLING_LONGSWIPE_THRESHOLD_RIGHT_PORT, val,
                    UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mSwipePortLeft) {
            int val = (Integer) newValue;
            Settings.Secure.putIntForUser(getContentResolver(),
                    Settings.Secure.FLING_LONGSWIPE_THRESHOLD_LEFT_PORT, val,
                    UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mSwipeLandRight) {
            int val = (Integer) newValue;
            Settings.Secure.putIntForUser(getContentResolver(),
                    Settings.Secure.FLING_LONGSWIPE_THRESHOLD_RIGHT_LAND, val,
                    UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mSwipeLandLeft) {
            int val = (Integer) newValue;
            Settings.Secure.putIntForUser(getContentResolver(),
                    Settings.Secure.FLING_LONGSWIPE_THRESHOLD_LEFT_LAND, val,
                    UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mSwipeVertUp) {
            int val = (Integer) newValue;
            Settings.Secure
                    .putIntForUser(getContentResolver(),
                            Settings.Secure.FLING_LONGSWIPE_THRESHOLD_UP_LAND, val,
                            UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mSwipeVertDown) {
            int val = (Integer) newValue;
            Settings.Secure.putIntForUser(getContentResolver(),
                    Settings.Secure.FLING_LONGSWIPE_THRESHOLD_DOWN_LAND, val,
                    UserHandle.USER_CURRENT);
            return true;
        } else if (preference.equals(mKbCursors)) {
            boolean enabled = ((Boolean) newValue).booleanValue();
            Settings.Secure.putIntForUser(getContentResolver(),
                    Settings.Secure.FLING_KEYBOARD_CURSORS, enabled ? 1 : 0,
                    UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mLogoOpacity) {
            int val = (Integer) newValue;
            Settings.Secure.putIntForUser(getContentResolver(),
                    Settings.Secure.FLING_LOGO_OPACITY, val,
                    UserHandle.USER_CURRENT);
            return true;
        }
        return false;
    }

    protected boolean usesExtendedActionsList() {
        return true;
    }

    protected void onActionPolicyEnforced(ArrayList<ActionPreference> prefs) {
        enforceAction(prefs, ActionHandler.SYSTEMUI_TASK_BACK);
        enforceAction(prefs, ActionHandler.SYSTEMUI_TASK_HOME);
    }

    /*
     * Iterate the list: if only one instance, disable it otherwise, enable
     */
    private void enforceAction(ArrayList<ActionPreference> prefs, String action) {
        ArrayList<ActionPreference> actionPrefs = new ArrayList<ActionPreference>();
        for (ActionPreference pref : prefs) {
            if (pref.getActionConfig().getAction().equals(action)) {
                actionPrefs.add(pref);
            }
        }
        boolean moreThanOne = actionPrefs.size() > 1;
        for (ActionPreference pref : actionPrefs) {
            pref.setEnabled(moreThanOne);
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.RESURRECTED;
    }
}
