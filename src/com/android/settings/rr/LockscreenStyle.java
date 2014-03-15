/*
 * Copyright (C) 2013 SlimRoms Project
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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.Display;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.R;
import com.android.settings.Utils;

import com.android.internal.widget.LockPatternUtils;

import net.margaritov.preference.colorpicker.ColorPickerPreference;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class LockscreenStyle extends SettingsPreferenceFragment
        implements OnPreferenceChangeListener {

    private static final String TAG = "LockscreenStyle";

    private static final String WALLPAPER_NAME = "lockscreen_wallpaper";
    private static final String KEY_LOCKSCREEN_COLORIZE_ICON = "lockscreen_colorize_icon";
    private static final String KEY_LOCKSCREEN_LOCK_ICON = "lockscreen_lock_icon";
    private static final String KEY_LOCKSCREEN_FRAME_COLOR = "lockscreen_frame_color";
    private static final String KEY_LOCKSCREEN_LOCK_COLOR = "lockscreen_lock_color";
    private static final String KEY_LOCKSCREEN_DOTS_COLOR = "lockscreen_dots_color";
    private static final String LOCKSCREEN_BACKGROUND = "lockscreen_background";
    private static final String LOCKSCREEN_BACKGROUND_STYLE = "lockscreen_background_style";
    private static final String LOCKSCREEN_BACKGROUND_COLOR_FILL = "lockscreen_background_color_fill";

    private static final int REQUEST_PICK_WALLPAPER = 201;
    private static final int COLOR_FILL = 0;
    private static final int CUSTOM_IMAGE = 1;
    private static final int DEFAULT = 2;

    private String mDefault;

    private ColorPickerPreference mLockColorFill;
    private ListPreference mLockBackground;

    private PreferenceCategory mLockscreenBackground;
    private File wallpaperImage;
    private File wallpaperTemporary;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createCustomView();
    }
    private PreferenceScreen createCustomView() {
        PreferenceScreen prefSet = getPreferenceScreen();
        if (prefSet != null) {
            prefSet.removeAll();
        }
        addPreferencesFromResource(R.xml.lockscreen_style);
        prefSet = getPreferenceScreen();

        // Set to string so we don't have to create multiple objects of it
        mDefault = getResources().getString(R.string.default_string);


        mLockscreenBackground = (PreferenceCategory) findPreference(LOCKSCREEN_BACKGROUND);

        mLockBackground = (ListPreference) findPreference(LOCKSCREEN_BACKGROUND_STYLE);
        mLockBackground.setOnPreferenceChangeListener(this);
        mLockBackground.setValue(Integer.toString(Settings.System.getInt(getContentResolver(),
                Settings.System.LOCKSCREEN_BACKGROUND_STYLE, 2)));
        mLockBackground.setSummary(mLockBackground.getEntry());

        mLockColorFill = (ColorPickerPreference) findPreference(LOCKSCREEN_BACKGROUND_COLOR_FILL);
        mLockColorFill.setOnPreferenceChangeListener(this);
        mLockColorFill.setSummary(ColorPickerPreference.convertToARGB(
                Settings.System.getInt(getContentResolver(),
                Settings.System.LOCKSCREEN_BACKGROUND_COLOR, 0x00000000)));

        setHasOptionsMenu(true);
        
        return prefSet;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private Uri getLockscreenExternalUri() {
        File dir = getActivity().getExternalCacheDir();
        File wallpaper = new File(dir, WALLPAPER_NAME);
        return Uri.fromFile(wallpaper);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {

           if (requestCode == REQUEST_PICK_WALLPAPER) {
                FileOutputStream wallpaperStream = null;
                try {
                    wallpaperStream = getActivity().openFileOutput(WALLPAPER_NAME,
                            Context.MODE_WORLD_READABLE);

                } catch (FileNotFoundException e) {
                    return; // NOOOOO
                }
                Uri selectedImageUri = getLockscreenExternalUri();
                Bitmap bitmap;
                if (data != null) {
                    Uri mUri = data.getData();
                    try {
                        bitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(),
                                mUri);
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, wallpaperStream);

                        Toast.makeText(getActivity(), getResources().getString(R.string.
                                background_result_successful), Toast.LENGTH_LONG).show();
                        Settings.System.putInt(getContentResolver(),
                                Settings.System.LOCKSCREEN_BACKGROUND_STYLE, 1);
                        updateVisiblePreferences();

                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        bitmap = BitmapFactory.decodeFile(selectedImageUri.getPath());
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, wallpaperStream);
                    } catch (NullPointerException npe) {
                        Log.e(TAG, "SeletedImageUri was null.");
                        Toast.makeText(getActivity(), getResources().getString(R.string.
                                background_result_not_successful), Toast.LENGTH_LONG).show();
                        super.onActivityResult(requestCode, resultCode, data);
                        return;
                    }
                }
            } else {
        }
    }
    
      @Override
      public boolean onPreferenceChange(Preference preference, Object newValue) {
       if (preference == mLockBackground) {
            int index = mLockBackground.findIndexOfValue(String.valueOf(newValue));
            preference.setSummary(mLockBackground.getEntries()[index]);
            return handleBackgroundSelection(index);
        } else if (preference == mLockColorFill) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int value = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_BACKGROUND_COLOR, value);
          return true;
        }
        return false;
    }	
    
    private void updateVisiblePreferences() {
        int visible = Settings.System.getInt(getContentResolver(),
                Settings.System.LOCKSCREEN_BACKGROUND_STYLE, 2);
        if (visible == 0) {
            mLockscreenBackground.addPreference(mLockColorFill);
        } else {
            mLockscreenBackground.removePreference(mLockColorFill);
        }
    }


    private boolean handleBackgroundSelection(int index) {
        if (index == COLOR_FILL) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_BACKGROUND_STYLE, 0);
            updateVisiblePreferences();
            return true;
        } else if (index == CUSTOM_IMAGE) {
            // Used to reset the image when already set
            Settings.System.putInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_BACKGROUND_STYLE, 2);
            // Launches intent for user to select an image/crop it to set as background
            Display display = getActivity().getWindowManager().getDefaultDisplay();

            int width = getActivity().getWallpaperDesiredMinimumWidth();
            int height = getActivity().getWallpaperDesiredMinimumHeight();
            float spotlightX = (float)display.getWidth() / width;
            float spotlightY = (float)display.getHeight() / height;

            Intent intent = new Intent(Intent.ACTION_PICK,
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            intent.putExtra("crop", "true");
            intent.putExtra("scale", true);
            intent.putExtra("scaleUpIfNeeded", true);
            intent.putExtra("aspectX", width);
            intent.putExtra("aspectY", height);
            intent.putExtra("outputX", width);
            intent.putExtra("outputY", height);
            intent.putExtra("spotlightX", spotlightX);
            intent.putExtra("spotlightY", spotlightY);
            intent.putExtra("outputFormat", Bitmap.CompressFormat.PNG.toString());
            intent.putExtra(MediaStore.EXTRA_OUTPUT, getLockscreenExternalUri());

            startActivityForResult(intent, REQUEST_PICK_WALLPAPER);
        } else if (index == DEFAULT) {
            // Sets background to default
            Settings.System.putInt(getContentResolver(),
                            Settings.System.LOCKSCREEN_BACKGROUND_STYLE, 2);
            updateVisiblePreferences();
            return true;
        }
        return false;
    }
}
