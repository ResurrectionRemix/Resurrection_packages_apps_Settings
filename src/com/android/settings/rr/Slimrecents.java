/*
* Copyright (C) 2015 ResurrectionRemix Rom
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.android.settings.rr;


  import android.app.ActivityManager;
  import android.app.AlertDialog;
  import android.app.Dialog;
  import android.content.ContentResolver;
  import android.content.DialogInterface;
  import android.content.res.Resources;
  import android.database.ContentObserver;
  import android.os.Bundle;
  import android.os.Handler;
  import android.os.UserHandle;
  import android.preference.ListPreference;
  import android.preference.Preference;
  import android.preference.Preference.OnPreferenceChangeListener;
  import android.preference.PreferenceCategory;
  import android.preference.PreferenceScreen;
  import android.preference.SlimSeekBarPreference;
  import android.preference.SwitchPreference;
  import android.provider.Settings;
  import android.provider.Settings.SettingNotFoundException;
  import android.util.Log;
  import android.view.Gravity;
  import android.view.Menu;
  import android.view.MenuItem;
  import android.view.MenuInflater;
  
  import com.android.internal.logging.MetricsLogger;
  import com.android.settings.DialogCreatable;
  import com.android.settings.R;
  import com.android.settings.SettingsPreferenceFragment;
  
  import com.android.internal.util.slim.DeviceUtils;
  
  import net.margaritov.preference.colorpicker.ColorPickerPreference;
public class Slimrecents extends SettingsPreferenceFragment
            implements DialogCreatable, Preference.OnPreferenceChangeListener  {

    private static final int MENU_RESET = Menu.FIRST;
    private static final int DEFAULT_BACKGROUND_COLOR = 0x00ffffff;
    
 // Preferences
    private static final String USE_SLIM_RECENTS = "use_slim_recents";
   private static final String ONLY_SHOW_RUNNING_TASKS = "only_show_running_tasks";
    private static final String RECENTS_MAX_APPS = "max_apps";
    private static final String RECENT_PANEL_SHOW_TOPMOST =
            "recent_panel_show_topmost";
    private static final String RECENT_PANEL_LEFTY_MODE =
            "recent_panel_lefty_mode";
    private static final String RECENT_PANEL_SCALE =
            "recent_panel_scale";
    private static final String RECENT_PANEL_EXPANDED_MODE =
            "recent_panel_expanded_mode";
    private static final String RECENT_PANEL_BG_COLOR =
            "recent_panel_bg_color";
   private static final String RECENT_CARD_BG_COLOR =
            "recent_card_bg_color";
    private static final String RECENT_CARD_TEXT_COLOR =            "recent_card_text_color";
  
     private SwitchPreference mUseSlimRecents;
    private SwitchPreference mShowRunningTasks;
    private SlimSeekBarPreference mMaxApps;
    private SwitchPreference mRecentsShowTopmost;
    private SwitchPreference mRecentPanelLeftyMode;
    private SlimSeekBarPreference mRecentPanelScale;
    private ListPreference mRecentPanelExpandedMode;
    private ColorPickerPreference mRecentPanelBgColor;
    private ColorPickerPreference mRecentCardBgColor;
    private ColorPickerPreference mRecentCardTextColor;
    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.DEVELOPMENT;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.slim_recents);
        ContentResolver resolver = getActivity().getContentResolver();
        PreferenceScreen prefSet = getPreferenceScreen();

     mUseSlimRecents = (SwitchPreference) prefSet.findPreference(USE_SLIM_RECENTS);
        mUseSlimRecents.setChecked(Settings.System.getInt(resolver,
                Settings.System.USE_SLIM_RECENTS, 0) == 1);
        mUseSlimRecents.setOnPreferenceChangeListener(this);

        updatePreference();
    }

  private void updatePreference() {
        boolean slimRecent = Settings.System.getInt(getActivity().getContentResolver(),
                   Settings.System.USE_SLIM_RECENTS, 0) == 1;
            initializeAllPreferences();
            updateRecentPanelPreferences();
      }

      @Override
     public void onResume() {
         super.onResume();
        updatePreference();
    }
	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
	ContentResolver resolver = getActivity().getContentResolver();
	 if (preference == mUseSlimRecents) {
            Settings.System.putInt(getContentResolver(), Settings.System.USE_SLIM_RECENTS,
                    ((Boolean) newValue) ? 1 : 0);
            updatePreference();
            return true;
       } else if (preference == mShowRunningTasks) {
            Settings.System.putInt(getContentResolver(), Settings.System.RECENT_SHOW_RUNNING_TASKS,
                    ((Boolean) newValue) ? 1 : 0);
            return true;
        } else if (preference == mRecentPanelScale) {
            int value = Integer.parseInt((String) newValue);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.RECENT_PANEL_SCALE_FACTOR, value);
            return true;
        } else if (preference == mRecentPanelExpandedMode) {
            int value = Integer.parseInt((String) newValue);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.RECENT_PANEL_EXPANDED_MODE, value);
            return true;
        } else if (preference == mRecentPanelBgColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            if (hex.equals("#00ffffff")) {
                preference.setSummary(R.string.default_string);
            } else {
                preference.setSummary(hex);
            }
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.RECENT_PANEL_BG_COLOR,
                    intHex);
            return true;
        } else if (preference == mRecentCardBgColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            if (hex.equals("#00ffffff")) {
                preference.setSummary(R.string.default_string);
            } else {
                preference.setSummary(hex);
            }
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.RECENT_CARD_BG_COLOR,
                    intHex);
           return true;
        } else if (preference == mRecentCardTextColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            if (hex.equals("#00ffffff")) {
                preference.setSummary(R.string.default_string);
            } else {
                preference.setSummary(hex);
            }
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.RECENT_CARD_TEXT_COLOR,
                    intHex);
            return true;
        } else if (preference == mRecentPanelLeftyMode) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.RECENT_PANEL_GRAVITY,
                    ((Boolean) newValue) ? Gravity.LEFT : Gravity.RIGHT);
            return true;
        } else if (preference == mRecentsShowTopmost) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.RECENT_PANEL_SHOW_TOPMOST,
                    ((Boolean) newValue) ? 1 : 0);
            return true;
        } else if (preference == mMaxApps) {
            int value = Integer.parseInt((String) newValue);
            Settings.System.putInt(getContentResolver(),
                Settings.System.RECENTS_MAX_APPS, value);
            return true;
        }
        return false;
    }
    
	    @Override
      public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
          menu.add(0, MENU_RESET, 0, R.string.reset)
                  .setIcon(R.drawable.ic_action_reset)
                  .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
      }
  
      @Override
      public boolean onOptionsItemSelected(MenuItem item) {
          switch (item.getItemId()) {
              case MENU_RESET:
                  resetToDefault();
                  return true;
              default:
                  return super.onContextItemSelected(item);
          }
      }
  
      private void resetToDefault() {
          AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
          alertDialog.setTitle(R.string.shortcut_action_reset);
          alertDialog.setMessage(R.string.reset_message);
          alertDialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int id) {
                  resetValues();
              }
          });
          alertDialog.setNegativeButton(R.string.cancel, null);
          alertDialog.create().show();
      }
  
      private void resetValues() {
          Settings.System.putInt(getContentResolver(),
                  Settings.System.RECENT_PANEL_BG_COLOR, DEFAULT_BACKGROUND_COLOR);
          mRecentPanelBgColor.setNewPreviewColor(DEFAULT_BACKGROUND_COLOR);
          mRecentPanelBgColor.setSummary(R.string.default_string);
          Settings.System.putInt(getContentResolver(),
                  Settings.System.RECENT_CARD_BG_COLOR, DEFAULT_BACKGROUND_COLOR);
          mRecentCardBgColor.setNewPreviewColor(DEFAULT_BACKGROUND_COLOR);
          mRecentCardBgColor.setSummary(R.string.default_string);
          Settings.System.putInt(getContentResolver(),
                  Settings.System.RECENT_CARD_TEXT_COLOR, DEFAULT_BACKGROUND_COLOR);
          mRecentCardTextColor.setNewPreviewColor(DEFAULT_BACKGROUND_COLOR);
          mRecentCardTextColor.setSummary(R.string.default_string);
      }
  
      private void updateRecentPanelPreferences() {
          final boolean recentLeftyMode = Settings.System.getInt(getContentResolver(),
                  Settings.System.RECENT_PANEL_GRAVITY, Gravity.RIGHT) == Gravity.LEFT;
          mRecentPanelLeftyMode.setChecked(recentLeftyMode);
  
          final int recentScale = Settings.System.getInt(getContentResolver(),
                  Settings.System.RECENT_PANEL_SCALE_FACTOR, 100);
          mRecentPanelScale.setInitValue(recentScale - 60);
  
          final int recentExpandedMode = Settings.System.getInt(getContentResolver(),
                  Settings.System.RECENT_PANEL_EXPANDED_MODE, 0);
          mRecentPanelExpandedMode.setValue(recentExpandedMode + "");
      }
  
      private void initializeAllPreferences() {
          mUseSlimRecents = (SwitchPreference) findPreference(USE_SLIM_RECENTS);
          mUseSlimRecents.setOnPreferenceChangeListener(this);
  
          mShowRunningTasks = (SwitchPreference) findPreference(ONLY_SHOW_RUNNING_TASKS);
          mShowRunningTasks.setOnPreferenceChangeListener(this);
  
          mMaxApps = (SlimSeekBarPreference) findPreference(RECENTS_MAX_APPS);
          mMaxApps.setOnPreferenceChangeListener(this);
          mMaxApps.minimumValue(5);
          mMaxApps.setInitValue(Settings.System.getIntForUser(getContentResolver(),
                  Settings.System.RECENTS_MAX_APPS, ActivityManager.getMaxRecentTasksStatic(),
                  UserHandle.USER_CURRENT) - 5);
          mMaxApps.disablePercentageValue(true);
  
          // Recent panel background color
          mRecentPanelBgColor =
                  (ColorPickerPreference) findPreference(RECENT_PANEL_BG_COLOR);
          mRecentPanelBgColor.setOnPreferenceChangeListener(this);
          final int intColor = Settings.System.getInt(getContentResolver(),
                  Settings.System.RECENT_PANEL_BG_COLOR, 0x00ffffff);
          String hexColor = String.format("#%08x", (0x00ffffff & intColor));
          if (hexColor.equals("#00ffffff")) {
              mRecentPanelBgColor.setSummary(R.string.default_string);
          } else {
              mRecentPanelBgColor.setSummary(hexColor);
          }
          mRecentPanelBgColor.setNewPreviewColor(intColor);
  
          // Recent card background color
          mRecentCardBgColor =
                  (ColorPickerPreference) findPreference(RECENT_CARD_BG_COLOR);
          mRecentCardBgColor.setOnPreferenceChangeListener(this);
          final int intColorCard = Settings.System.getInt(getContentResolver(),
                  Settings.System.RECENT_CARD_BG_COLOR, 0x00ffffff);
          String hexColorCard = String.format("#%08x", (0x00ffffff & intColorCard));
          if (hexColorCard.equals("#00ffffff")) {
              mRecentCardBgColor.setSummary(R.string.default_string);
          } else {
              mRecentCardBgColor.setSummary(hexColorCard);
          }
          mRecentCardBgColor.setNewPreviewColor(intColorCard);
  
          // Recent card text color
          mRecentCardTextColor =
                  (ColorPickerPreference) findPreference(RECENT_CARD_TEXT_COLOR);
          mRecentCardTextColor.setOnPreferenceChangeListener(this);
          final int intColorText = Settings.System.getInt(getContentResolver(),
                  Settings.System.RECENT_CARD_TEXT_COLOR, 0x00ffffff);
          String hexColorText = String.format("#%08x", (0x00ffffff & intColorText));
          if (hexColorText.equals("#00ffffff")) {
              mRecentCardTextColor.setSummary(R.string.default_string);
          } else {
              mRecentCardTextColor.setSummary(hexColorText);
          }
          mRecentCardTextColor.setNewPreviewColor(intColorText);
  
          // Enable options menu for color reset
          setHasOptionsMenu(true);
  
          boolean enableRecentsShowTopmost = Settings.System.getInt(getContentResolver(),
                                        Settings.System.RECENT_PANEL_SHOW_TOPMOST, 0) == 1;
          mRecentsShowTopmost = (SwitchPreference) findPreference(RECENT_PANEL_SHOW_TOPMOST);
          mRecentsShowTopmost.setChecked(enableRecentsShowTopmost);
          mRecentsShowTopmost.setOnPreferenceChangeListener(this);
  
          mRecentPanelLeftyMode =
                  (SwitchPreference) findPreference(RECENT_PANEL_LEFTY_MODE);
          mRecentPanelLeftyMode.setOnPreferenceChangeListener(this);
  
          mRecentPanelScale =
                  (SlimSeekBarPreference) findPreference(RECENT_PANEL_SCALE);
          mRecentPanelScale.setInterval(5);
          mRecentPanelScale.setDefault(100);
          mRecentPanelScale.minimumValue(60);
          mRecentPanelScale.setOnPreferenceChangeListener(this);
          mRecentPanelScale.setInitValue(Settings.System.getInt(getContentResolver(),
                  Settings.System.RECENT_PANEL_SCALE_FACTOR, 100) - 60);
  
          mRecentPanelExpandedMode =
                  (ListPreference) findPreference(RECENT_PANEL_EXPANDED_MODE);
          mRecentPanelExpandedMode.setOnPreferenceChangeListener(this);
      }
}



