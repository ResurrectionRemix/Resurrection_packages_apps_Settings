/*
 * Copyright (C) 2016 The DirtyUnicorns Project
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

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Context;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.os.Environment;
import android.os.UserHandle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
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

import android.provider.SearchIndexableResource;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.utils.du.ActionConstants;
import com.android.internal.utils.du.Config;
import com.android.internal.utils.du.Config.ButtonConfig;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.rr.SeekBarPreferenceCham;

import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.util.Log;

import net.margaritov.preference.colorpicker.ColorPickerPreference;

import java.util.ArrayList;
import java.util.List;

public class SmartbarSettings extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener , Indexable {
    private ListPreference mSmartBarContext;
    private ListPreference mImeActions;
    private ListPreference mButtonAnim;
    private SeekBarPreferenceCham mButtonsAlpha;
	
    private ColorPickerPreference mNavbuttoncolor;
  
    static final int DEFAULT = 0xffffffff;

    private static final int MENU_RESET = Menu.FIRST;
    private static final int MENU_SAVE = Menu.FIRST + 1;
    private static final int MENU_RESTORE = Menu.FIRST + 2;

    private static final int DIALOG_RESET_CONFIRM = 1;
    private static final int DIALOG_RESTORE_PROFILE = 2;
    private static final int DIALOG_SAVE_PROFILE = 3;
    private static final String CONFIG_STORAGE = Environment.getExternalStorageDirectory()
            + File.separator
            + "smartbar_configs";
    private static final String SMARTBAR_CONFIGS_PREFIX = "smartbar_config_";
    private static final String KEY_SMARTBAR_BACKUP = "smartbar_profile_save";
    private static final String KEY_SMARTBAR_RESTORE = "smartbar_profile_restore";
    private static final String NAVBAR_COLOR = "navbar_button_color";
    private static final String PREF_NAVBAR_BUTTONS_ALPHA = "navbar_buttons_alpha";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.smartbar_settings);
        int intColor;
        String hexColor;

        int contextVal = Settings.Secure.getIntForUser(mContext.getContentResolver(),
                "smartbar_context_menu_mode", 0, UserHandle.USER_CURRENT);
        mSmartBarContext = (ListPreference) findPreference("smartbar_context_menu_position");
        mSmartBarContext.setValue(String.valueOf(contextVal));
        mSmartBarContext.setOnPreferenceChangeListener(this);

        int imeVal = Settings.Secure.getIntForUser(mContext.getContentResolver(),
                "smartbar_ime_hint_mode", 1, UserHandle.USER_CURRENT);
        mImeActions = (ListPreference) findPreference("smartbar_ime_action");
        mImeActions.setValue(String.valueOf(imeVal));
        mImeActions.setOnPreferenceChangeListener(this);

        int buttonAnimVal = Settings.Secure.getIntForUser(mContext.getContentResolver(),
                "smartbar_button_animation_style", 0, UserHandle.USER_CURRENT);
        mButtonAnim = (ListPreference) findPreference("smartbar_button_animation");
        mButtonAnim.setValue(String.valueOf(buttonAnimVal));
        mButtonAnim.setOnPreferenceChangeListener(this);

        
        
        mNavbuttoncolor = (ColorPickerPreference) findPreference(NAVBAR_COLOR);
        mNavbuttoncolor.setOnPreferenceChangeListener(this);
        intColor = Settings.System.getInt(getContentResolver(),
                    Settings.System.NAVBAR_BUTTON_COLOR, DEFAULT);
        hexColor = String.format("#%08x", (0xffffffff & intColor));
        mNavbuttoncolor.setSummary(hexColor);
        mNavbuttoncolor.setNewPreviewColor(intColor);

	mButtonsAlpha =
                (SeekBarPreferenceCham) findPreference(PREF_NAVBAR_BUTTONS_ALPHA);
        int bAlpha = Settings.Secure.getIntForUser(getContentResolver(),
                Settings.Secure.NAVBAR_BUTTONS_ALPHA, 255, UserHandle.USER_CURRENT);
        mButtonsAlpha.setValue(bAlpha / 1);
        mButtonsAlpha.setOnPreferenceChangeListener(this);

        setHasOptionsMenu(true);
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        Dialog dialog = null;
        switch (dialogId) {
            case DIALOG_RESET_CONFIRM:
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
                alertDialog.setTitle(R.string.smartbar_factory_reset_title);
                alertDialog.setMessage(R.string.smartbar_factory_reset_confirm);
                alertDialog.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        resetSmartbar();
                    }
                });
                alertDialog.setNegativeButton(R.string.write_settings_off, null);
                dialog = alertDialog.create();
                break;
            case DIALOG_RESTORE_PROFILE:
                final ConfigAdapter configAdapter = new ConfigAdapter(getActivity(),
                        getConfigFiles(CONFIG_STORAGE));
                AlertDialog.Builder configDialog = new AlertDialog.Builder(getActivity());
                configDialog.setTitle(R.string.smartbar_config_dialog_title);
                configDialog.setNegativeButton(getString(android.R.string.cancel), null);
                configDialog.setAdapter(configAdapter, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        String resultMsg;
                        try {
                            File configFile = (File) configAdapter.getItem(item);
                            String config = getSmartbarConfigFromStorage(configFile);
                            restoreConfig(getActivity(), config);
                            resultMsg = getString(R.string.smartbar_config_restore_success_toast);
                        } catch (Exception e) {
                            resultMsg = getString(R.string.smartbar_config_restore_error_toast);
                        }
                        Toast.makeText(getActivity(), resultMsg, Toast.LENGTH_SHORT).show();
                    }
                });
                dialog = configDialog.create();
                break;
            case DIALOG_SAVE_PROFILE:
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                final EditText input = new EditText(getActivity());
                builder.setTitle(getString(R.string.smartbar_config_name_edit_dialog_title));
                builder.setMessage(R.string.smartbar_config_name_edit_dialog_message);
                builder.setView(input);
                builder.setNegativeButton(getString(android.R.string.cancel), null);
                builder.setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                String inputText = input.getText().toString();
                                if (TextUtils.isEmpty(inputText)) {
                                    inputText = String.valueOf(android.text.format.DateFormat
                                            .format(
                                                    "yyyy-MM-dd_hh:mm:ss", new java.util.Date()));
                                }
                                String resultMsg;
                                try {
                                    String currentConfig = getCurrentConfig(getActivity());
                                    backupSmartbarConfig(currentConfig, inputText);
                                    resultMsg = getString(R.string.smartbar_config_backup_success_toast);
                                } catch (Exception e) {
                                    resultMsg = getString(R.string.smartbar_config_backup_error_toast);
                                }
                                Toast.makeText(getActivity(), resultMsg, Toast.LENGTH_SHORT).show();
                            }
                        });
                dialog = builder.create();
                dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                break;
        }
        return dialog;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == findPreference("smartbar_editor_mode")) {
            getActivity().sendBroadcastAsUser(new Intent("intent_navbar_edit"), UserHandle.CURRENT);
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, MENU_RESET, 0, R.string.reset)
                .setIcon(com.android.internal.R.drawable.ic_menu_refresh)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.add(0, MENU_SAVE, 0, R.string.smartbar_backup_current_config_title)
                .setIcon(R.drawable.ic_smartbar_save_profile)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.add(0, MENU_RESTORE, 0, R.string.smartbar_restore_config_title)
                .setIcon(R.drawable.ic_smartbar_restore_profile)
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

    private void resetSmartbar() {
        ArrayList<ButtonConfig> buttonConfigs = Config.getDefaultConfig(
               mContext,
        ActionConstants.getDefaults(ActionConstants.SMARTBAR));
               Config.setConfig(mContext,
        ActionConstants.getDefaults(ActionConstants.SMARTBAR),
               buttonConfigs);
        Intent intent = new Intent("intent_navbar_edit");
        intent.putExtra("extra_navbar_edit_reset_layout", "resetMePlox");
        getActivity().sendBroadcastAsUser(intent, UserHandle.CURRENT);

        Settings.Secure.putInt(mContext.getContentResolver(),
                "smartbar_context_menu_mode", 0);
        mSmartBarContext.setValue(String.valueOf(0));
        mSmartBarContext.setOnPreferenceChangeListener(this);

        Settings.Secure.putInt(mContext.getContentResolver(),
                "smartbar_ime_hint_mode", 1);
        mImeActions.setValue(String.valueOf(1));
        mImeActions.setOnPreferenceChangeListener(this);

        Settings.Secure.putInt(mContext.getContentResolver(),
                "smartbar_button_animation_style", 0);
        mButtonAnim.setValue(String.valueOf(0));
        mButtonAnim.setOnPreferenceChangeListener(this);

	Settings.Secure.putInt(mContext.getContentResolver(),
                "navbar_buttons_alpha", 255);
        mButtonsAlpha.setValue(255);
        mButtonsAlpha.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference.equals(mSmartBarContext)) {
            int position = Integer.parseInt(((String) newValue).toString());
            Settings.Secure.putInt(getContentResolver(), "smartbar_context_menu_mode",
                    position);
            return true;
        } else if (preference.equals(mButtonAnim)) {
            int val = Integer.parseInt(((String) newValue).toString());
            Settings.Secure.putInt(getContentResolver(), "smartbar_button_animation_style",
                    val);
            return true;
        } else if (preference.equals(mImeActions)) {
            int val = Integer.parseInt(((String) newValue).toString());
            Settings.Secure.putInt(getContentResolver(), "smartbar_ime_hint_mode",
                    val);
            return true;
        } else if (preference == mNavbuttoncolor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.NAVBAR_BUTTON_COLOR, intHex);
            return true;
        } else if (preference == mButtonsAlpha) {
            int val = (Integer) newValue;
            Settings.Secure.putIntForUser(getContentResolver(),
                    Settings.Secure.NAVBAR_BUTTONS_ALPHA, val * 1, UserHandle.USER_CURRENT);
            return true;
	}
        return false;
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
            if (name.startsWith(SMARTBAR_CONFIGS_PREFIX)) {
                name = f.getName().substring(SMARTBAR_CONFIGS_PREFIX.length(), f.getName().length());
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
                ctx.getContentResolver(), ActionConstants.getDefaults(ActionConstants.SMARTBAR)
                        .getUri(),
                UserHandle.USER_CURRENT);
        if (TextUtils.isEmpty(config)) {
            config = ActionConstants.getDefaults(ActionConstants.SMARTBAR).getDefaultConfig();
        }
        return config;
    }

    static void restoreConfig(Context context, String config) {
        Settings.Secure.putStringForUser(context.getContentResolver(),
                ActionConstants.getDefaults(ActionConstants.SMARTBAR)
                        .getUri(), config,
                UserHandle.USER_CURRENT);
        Intent intent = new Intent("intent_navbar_edit");
        intent.putExtra("extra_navbar_edit_reset_layout", "resetMePlox");
        context.sendBroadcastAsUser(intent, UserHandle.CURRENT);
    }

    static void backupSmartbarConfig(String config, String suffix) {
        File dir = new File(CONFIG_STORAGE);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File configFile = new File(dir, SMARTBAR_CONFIGS_PREFIX + suffix);
        FileOutputStream stream;
        try {
            stream = new FileOutputStream(configFile);
            stream.write(config.getBytes());
            stream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String getSmartbarConfigFromStorage(File file) {
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
                SMARTBAR_CONFIGS_PREFIX
        }))) {
            list.add(tmp);
        }
        return list;
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.DONT_TRACK_ME_BRO;
    }

    private void resetToDefault() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
        alertDialog.setTitle(R.string.header_colors_reset_title);
        alertDialog.setMessage(R.string.header_colors_reset_message);
        alertDialog.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                resetValues();
            }
        });
        alertDialog.setNegativeButton(R.string.cancel, null);
        alertDialog.create().show();
    }

    private void resetValues() {
        Settings.System.putInt(getContentResolver(),
                Settings.System.NAVBAR_BUTTON_COLOR, DEFAULT);
        mNavbuttoncolor.setNewPreviewColor(DEFAULT);
        mNavbuttoncolor.setSummary(R.string.default_string);

    }
    
    
     public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                                                                             boolean enabled) {
                     ArrayList<SearchIndexableResource> result =
                             new ArrayList<SearchIndexableResource>();
 
                     SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.smartbar_settings;
                     result.add(sir);
 
                     return result;
                 }
 
                 @Override
                 public List<String> getNonIndexableKeys(Context context) {
                     final List<String> keys = new ArrayList<String>();
                     return keys;
                 }
         };
}

