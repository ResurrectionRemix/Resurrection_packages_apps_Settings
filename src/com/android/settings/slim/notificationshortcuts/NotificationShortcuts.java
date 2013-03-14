/*
 * Copyright (C) 2013 SlimRoms (blk_jack)
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

package com.android.settings.slim.notificationshortcuts;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.ContentResolver;
import android.content.res.Resources;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.preference.SwitchPreference;
import android.preference.ListPreference;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.slim.notificationshortcuts.IconPicker;
import com.android.settings.slim.notificationshortcuts.ShortcutPickHelper;

import net.margaritov.preference.colorpicker.ColorPickerDialog;

public class NotificationShortcuts extends SettingsPreferenceFragment implements ShortcutPickHelper.OnPickListener,
    IconPicker.OnIconPickListener {

    private final static String TAG = NotificationShortcuts.class.getSimpleName();

    private static final String NOTIFICATION_SHORTCUTS_TOGGLE = "pref_notification_shortcuts_toggle";
    private static final String NOTIFICATION_SHORTCUTS_CUSTOM = "pref_notification_shortcuts_custom";
    private static final String NOTIFICATION_SHORTCUTS_LIST = "pref_notification_shortcuts_list";
    private static final String NOTIFICATION_SHORTCUTS_RESET = "pref_notification_shortcuts_reset";
    private static final String NOTIFICATION_SHORTCUTS_QUANTITY = "pref_notification_shortcuts_quantity";
    private static final String NOTIFICATION_SHORTCUTS_COLOR = "pref_notification_shortcuts_color";
    private static final String NOTIFICATION_SHORTCUTS_COLORIZE_TOGGLE = "pref_notification_shortcuts_colorize_toggle";
    private static final String NOTIFICATION_SHORTCUTS_HIDE_CARRIER = "pref_notification_shortcuts_hide_carrier";

    public final static String ICON_RESOURCE = "icon_resource";
    public final static String ICON_PACKAGE = "icon_package";
    public final static String ICON_FILE = "icon_file";
    public final static String EMPTY_TARGET = "empty";
    public final static String DEFAULT_TARGETS = "empty|empty|empty|empty|empty|empty|empty|empty|empty|empty|empty|empty|empty|empty|empty|empty";

    private ContentResolver mCr;
    private PreferenceScreen mPrefSet;
    private PreferenceGroup mPrefList;

    private SwitchPreference mNotificationShortcutsToggle;
    private ListPreference mNotificationShortcutsQuantity;
    private Preference mNotificationShortcutsReset;
    private Preference mNotificationShortcutsColor;
    private CheckBoxPreference mNotificationShortcutsColorizeToggle;
    private CheckBoxPreference mNotificationShortcutsHideCarrier;

    private ImageButton mDialogIcon;
    private Button mDialogLabel;
    private ShortcutPickHelper mPicker;
    private IconPicker mIconPicker;
    private ArrayList<TargetInfo> mTargetStore = new ArrayList<TargetInfo>();
    private int maxTargets;
    private int mPrefListOrder;

    private ViewGroup mContainer;
    private Activity mActivity;
    private Resources mResources;
    private File mImageTmp;
    private static String EMPTY_LABEL;

    class TargetInfo {
        String uri, pkgName;
        Drawable defaultIcon;
        String iconType;
        String iconSource;
        TargetInfo(Drawable dI) {
            defaultIcon = dI;
        }
        TargetInfo(String in, String iType, String iSource, Drawable dI) {
            uri = in;
            defaultIcon = dI;
            iconType = iType;
            iconSource = iSource;
        }
    }

    public void createShortcutPrefs() {
        initializeView(Settings.System.getStringForUser(mActivity.getContentResolver(), Settings.System.NOTIFICATION_SHORTCUTS_TARGETS, UserHandle.USER_CURRENT_OR_SELF));

        if (Settings.System.getInt(mCr,
                NOTIFICATION_SHORTCUTS_TOGGLE, 0) == 1) {
            if (mPrefListOrder > 0) {
                try {
                    mPrefSet.addPreference(mPrefList);
                } catch (Exception e) {
                    mPrefList = new PreferenceCategory(this.mActivity);
                    mPrefList.setKey(NOTIFICATION_SHORTCUTS_LIST);
                    mPrefList.setTitle(R.string.notification_shortcuts_title);
                    mPrefSet.addPreference(mPrefList);
                }
                mPrefList.setOrder(mPrefListOrder);
                mPrefListOrder = -1;
            }

            mPrefList.removeAll();
            mPrefList.setOrderingAsAdded(true);

            for (int i=0; i < maxTargets; i++) {
                Preference pref = new Preference(this.mActivity);
                pref.setOnPreferenceClickListener(mCustomShortcut);

                if (mTargetStore.get(i).uri.equals(EMPTY_TARGET)) {
                    pref.setTitle("<" + this.mActivity.getResources().getString(R.string.wifi_security_none) + ">");
                } else {
                    pref.setTitle(mPicker.getFriendlyNameForUri(mTargetStore.get(i).uri));
                }
                mPrefList.addPreference(pref);

                if (Settings.System.getInt(mCr,
                        NOTIFICATION_SHORTCUTS_TOGGLE, 0) == 1) {
                    pref.setEnabled(true);
                } else {
                    pref.setEnabled(false);
                }

                pref.setOrder(i);
            }
        } else {
            try {
                mPrefList = (PreferenceGroup) mPrefSet.findPreference(NOTIFICATION_SHORTCUTS_LIST);
                mPrefListOrder = mPrefList.getOrder();
                mPrefSet.removePreference(mPrefList);
            } catch (Exception e) {
            }
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        createShortcutPrefs();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.notification_shortcuts);

        mPrefSet = getPreferenceScreen();
        mPrefList = (PreferenceGroup) mPrefSet.findPreference(NOTIFICATION_SHORTCUTS_LIST);
        mCr = getContentResolver();

        /* Custom Notification Area Shortcuts pref */
        mNotificationShortcutsToggle = (SwitchPreference) mPrefSet.findPreference(
                NOTIFICATION_SHORTCUTS_TOGGLE);
        mNotificationShortcutsToggle.setChecked(Settings.System.getInt(mCr,
                NOTIFICATION_SHORTCUTS_TOGGLE, 0) == 1);
        mNotificationShortcutsToggle.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference,
                        Object newValue) {
                Settings.System.putInt(mCr, Settings.System.NOTIFICATION_SHORTCUTS_TOGGLE, (Boolean) newValue ? 1 : 0);
                mPrefSet.removePreference(mNotificationShortcutsColor);
                Settings.System.putInt(mCr, Settings.System.NOTIFICATION_SHORTCUTS_COLOR, 0xFFDFE0E0);
                mPrefSet.addPreference(mNotificationShortcutsColor);

                return true;
            }
        });

        mNotificationShortcutsQuantity = (ListPreference) mPrefSet.findPreference(
                NOTIFICATION_SHORTCUTS_QUANTITY);
        mNotificationShortcutsQuantity.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference,
                        Object newValue) {
                maxTargets = Integer.valueOf((String) newValue);
                Settings.System.putInt(mCr, Settings.System.NOTIFICATION_SHORTCUTS_QUANTITY, maxTargets);

                return true;
            }
        });

        mNotificationShortcutsColor = (Preference) mPrefSet.findPreference(
                NOTIFICATION_SHORTCUTS_COLOR);
        mNotificationShortcutsColor.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference,
                        Object newValue) {
                Settings.System.putInt(mCr, Settings.System.NOTIFICATION_SHORTCUTS_COLOR, (Integer) newValue);
                return true;
            }
        });

        mNotificationShortcutsReset = (Preference) mPrefSet.findPreference(
            NOTIFICATION_SHORTCUTS_RESET);
        mNotificationShortcutsReset.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Settings.System.putString(mCr, Settings.System.NOTIFICATION_SHORTCUTS_TARGETS, DEFAULT_TARGETS);
                initializeView(Settings.System.getStringForUser(mActivity.getContentResolver(), Settings.System.NOTIFICATION_SHORTCUTS_TARGETS, UserHandle.USER_CURRENT_OR_SELF));
                return true;
            }
        });

        mNotificationShortcutsColorizeToggle = (CheckBoxPreference) mPrefSet.findPreference(
                NOTIFICATION_SHORTCUTS_COLORIZE_TOGGLE);
        mNotificationShortcutsColorizeToggle.setChecked(Settings.System.getIntForUser(getContentResolver(),
                Settings.System.NOTIFICATION_SHORTCUTS_COLORIZE_TOGGLE, 1, UserHandle.USER_CURRENT_OR_SELF) == 1);
        mNotificationShortcutsColorizeToggle.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference,
                        Object newValue) {
                Settings.System.putInt(mCr, Settings.System.NOTIFICATION_SHORTCUTS_COLORIZE_TOGGLE, (Boolean) newValue ? 1 : 0);
                return true;
            }
        });

        mNotificationShortcutsHideCarrier = (CheckBoxPreference) mPrefSet.findPreference(
                NOTIFICATION_SHORTCUTS_HIDE_CARRIER);
        mNotificationShortcutsHideCarrier.setChecked(Settings.System.getIntForUser(getContentResolver(),
                Settings.System.NOTIFICATION_SHORTCUTS_HIDE_CARRIER, 0, UserHandle.USER_CURRENT_OR_SELF) == 1);
        mNotificationShortcutsHideCarrier.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference,
                        Object newValue) {
                Settings.System.putInt(mCr, Settings.System.NOTIFICATION_SHORTCUTS_HIDE_CARRIER, (Boolean) newValue ? 1 : 0);
                return true;
            }
        });

        // Found all the preferences, let's observe..
        mActivity = getActivity();
        mResources = getResources();

        mPicker = new ShortcutPickHelper(mActivity, this);
        mIconPicker = new IconPicker(mActivity, this);
        EMPTY_LABEL = mActivity.getResources().getString(R.string.notification_shortcuts_empty_title);
        mImageTmp = new File(mActivity.getCacheDir() + "/target.tmp");

        mActivity.getContentResolver().registerContentObserver(
        Settings.System.getUriFor(Settings.System.NOTIFICATION_SHORTCUTS_TOGGLE), true,
        mNotificationObserver);

        mActivity.getContentResolver().registerContentObserver(
        Settings.System.getUriFor(Settings.System.NOTIFICATION_SHORTCUTS_TARGETS), true,
        mNotificationObserver);

        mActivity.getContentResolver().registerContentObserver(
        Settings.System.getUriFor(Settings.System.NOTIFICATION_SHORTCUTS_QUANTITY), true,
        mNotificationObserver);

        maxTargets = Settings.System.getIntForUser(mActivity.getContentResolver(), Settings.System.NOTIFICATION_SHORTCUTS_QUANTITY, 6, UserHandle.USER_CURRENT_OR_SELF);
    }

    @Override
    public void onResume() {
        super.onResume();
        createShortcutPrefs();
    }

    @Override
    public void onStop(){
        super.onStop();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    private ContentObserver mNotificationObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            createShortcutPrefs();
        }
    };

    private boolean moveShortcut(boolean up, int target) {
        int nutarget;
        if (up) {
            nutarget = target - 1;
        } else {
            nutarget = target + 1;
        }

        Collections.swap(mTargetStore,target,nutarget);
        buildTarget(mTargetStore.get(nutarget).uri);
        return true;
    }

    public OnPreferenceClickListener mCustomShortcut = new OnPreferenceClickListener() {
        AlertDialog dialog;
        @Override
        public boolean onPreferenceClick(Preference preference) {
            String key = preference.getKey();
            final int target = preference.getOrder();

            AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
            builder.setTitle(R.string.notification_shortcuts_edit_title);
            builder.setMessage(R.string.notification_shortcuts_edit_msg);
            View view = View.inflate(mActivity, R.layout.notification_shortcuts_dialog, null);
            if (target == 0) {
                view.findViewById(R.id.move_up).setVisibility(View.INVISIBLE);
            } else if (target == (maxTargets - 1)) {
                view.findViewById(R.id.move_down).setVisibility(View.INVISIBLE);
            }
            view.findViewById(R.id.icon).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!mDialogLabel.getText().equals(EMPTY_LABEL)) {
                        try {
                            mImageTmp.createNewFile();
                            mImageTmp.setWritable(true, false);
                            mIconPicker.pickIcon(getId(), mImageTmp);
                        } catch (IOException e) {
                        }
                    }
                }
            });
            view.findViewById(R.id.label).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mPicker.pickShortcut(new String[] {EMPTY_LABEL}, new ShortcutIconResource[] {
                            ShortcutIconResource.fromContext(mActivity, android.R.drawable.ic_delete) }, getId());
                }
            });
            view.findViewById(R.id.move_up).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!mTargetStore.get(target).uri.startsWith("empty")) {
                        Toast.makeText(mActivity, getString(R.string.notification_shortcuts_move_up, mPicker.getFriendlyNameForUri(mTargetStore.get(target).uri)), Toast.LENGTH_LONG).show();
                    }
                    boolean moved = moveShortcut(true, target);

                    createShortcutPrefs();
                    if (moved)
                        dialog.dismiss();
                }
            });
            view.findViewById(R.id.move_down).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!mTargetStore.get(target).uri.startsWith("empty")) {
                        Toast.makeText(mActivity, getString(R.string.notification_shortcuts_move_down, mPicker.getFriendlyNameForUri(mTargetStore.get(target).uri)), Toast.LENGTH_LONG).show();
                    }
                    boolean moved = moveShortcut(false, target);

                    createShortcutPrefs();
                    if (moved)
                        dialog.dismiss();
                }
            });
            mDialogIcon = ((ImageButton) view.findViewById(R.id.icon));
            mDialogLabel = ((Button) view.findViewById(R.id.label));
            TargetInfo item = mTargetStore.get(target);
            mDialogIcon.setImageDrawable(mTargetStore.get(target).defaultIcon.mutate());
            TargetInfo tmpIcon = new TargetInfo(null);
            tmpIcon.iconType = item.iconType;
            tmpIcon.iconSource = item.iconSource;
            tmpIcon.pkgName = item.pkgName;
            mDialogIcon.setTag(tmpIcon);
            if (mTargetStore.get(target).uri.equals(EMPTY_TARGET)) {
                mDialogLabel.setText(EMPTY_LABEL);
            } else {
                mDialogLabel.setText(mPicker.getFriendlyNameForUri(mTargetStore.get(target).uri));
            }
            mDialogLabel.setTag(mTargetStore.get(target).uri);
            builder.setView(view);
            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    TargetInfo vObject = (TargetInfo) mDialogIcon.getTag();
                    String type = null, source = null, pkgName = null;
                    if (vObject != null) {
                        type = vObject.iconType;
                        source = vObject.iconSource;
                        pkgName = vObject.pkgName;
                    }
                    InsetDrawable pD = new InsetDrawable(mDialogIcon.getDrawable(), 0, 0, 0, 0);
                    setTarget(target, mDialogLabel.getTag().toString(), pD, type, source, pkgName);
                }
            });

            builder.setNegativeButton(R.string.cancel, null);
            builder.setCancelable(false);
            dialog = builder.create();
            dialog.show();
            ((TextView)dialog.findViewById(android.R.id.message)).setTextAppearance(mActivity,
                    android.R.style.TextAppearance_DeviceDefault_Small);

            return false;
        }
    };
    private void initializeView(String input) {
        if (input == null) {
            input = DEFAULT_TARGETS;
        }
        mTargetStore.clear();
        final PackageManager packMan = mActivity.getPackageManager();
        final String[] targetStore = input.split("\\|");

        for (int cc = 0; cc < maxTargets; cc++) {
            String uri = EMPTY_TARGET;
            Drawable front = null;
            String iconType = null;
            String iconSource = null;
            if (cc < targetStore.length) {
                uri = targetStore[cc];
                if (!uri.equals(EMPTY_TARGET)) {
                    try {
                        Intent in = Intent.parseUri(uri, 0);
                        if (in.hasExtra(ICON_FILE)) {
                            String rSource = in.getStringExtra(ICON_FILE);
                            File fPath = new File(rSource);
                            if (fPath != null) {
                                if (fPath.exists()) {
                                    front = new BitmapDrawable(getResources(), BitmapFactory.decodeFile(rSource));
                                }
                            }
                        } else if (in.hasExtra(ICON_RESOURCE)) {
                            String rSource = in.getStringExtra(ICON_RESOURCE);
                            String rPackage = in.getStringExtra(ICON_PACKAGE);
                            if (rSource != null) {
                                if (rPackage != null) {
                                    try {
                                        Context rContext = mActivity.createPackageContext(rPackage, 0);
                                        int id = rContext.getResources().getIdentifier(rSource, "drawable", rPackage);
                                        front = rContext.getResources().getDrawable(id);
                                    } catch (NameNotFoundException e) {
                                        e.printStackTrace();
                                    } catch (NotFoundException e) {
                                        e.printStackTrace();
                                    }
                                } else {
                                    front = mResources.getDrawable(mResources.getIdentifier(rSource, "drawable", "android"));
                                }
                            }
                        }
                        if (front == null) {
                            ActivityInfo aInfo = in.resolveActivityInfo(packMan, PackageManager.GET_ACTIVITIES);
                            if (aInfo != null) {
                                front = aInfo.loadIcon(packMan);
                            } else {
                                front = mResources.getDrawable(android.R.drawable.sym_def_app_icon).mutate();
                            }
                        }
                    } catch (Exception e) {
                    }
                }
            }
            if (front == null) {
                Drawable emptyIcon = mResources.getDrawable(R.drawable.ic_empty).mutate();
                front = emptyIcon;
            }
            mTargetStore.add(new TargetInfo(uri, iconType, iconSource,
                    front.getConstantState().newDrawable().mutate()));
        }
    }

    private void setTarget(int position, String uri, Drawable draw, String iconType, String iconSource, String pkgName) {
        TargetInfo item = mTargetStore.get(position);

        item.defaultIcon = mDialogIcon.getDrawable().getConstantState().newDrawable().mutate();
        item.uri = uri;
        item.iconType = iconType;
        item.iconSource = iconSource;
        item.pkgName = pkgName;

        buildTarget(uri);
    }

    private void buildTarget(String uri) {
        // Set target info and save all
        StringBuilder targetLayout = new StringBuilder();
        for (int i = 0; i < maxTargets; i++) {
            String mUri = mTargetStore.get(i).uri;
            String mType = mTargetStore.get(i).iconType;
            String mSource = mTargetStore.get(i).iconSource;
            if (!uri.equals(EMPTY_TARGET) && mType != null) {
                try {
                    Intent in = Intent.parseUri(mUri, 0);
                    in.putExtra(mType, mSource);
                    String mPkgName = mTargetStore.get(i).pkgName;
                    if (mPkgName != null) {
                        in.putExtra(ICON_PACKAGE, mTargetStore.get(i).pkgName);
                    } else {
                        in.removeExtra(ICON_PACKAGE);
                    }
                    mUri = in.toUri(0);
                } catch (URISyntaxException e) {
                }
            }
            targetLayout.append(mUri);
            targetLayout.append("|");
        }
        targetLayout.deleteCharAt(targetLayout.length() - 1);
        Settings.System.putString(mActivity.getContentResolver(), Settings.System.NOTIFICATION_SHORTCUTS_TARGETS, targetLayout.toString());
    }

    @Override
    public void shortcutPicked(String uri, String friendlyName, boolean isApplication) {
        try {
            Intent i = Intent.parseUri(uri, 0);
            PackageManager pm = mActivity.getPackageManager();
            ActivityInfo aInfo = i.resolveActivityInfo(pm, PackageManager.GET_ACTIVITIES);
            Drawable icon = null;
            if (aInfo != null) {
                icon = aInfo.loadIcon(pm).mutate();
            } else {
                icon = mResources.getDrawable(android.R.drawable.sym_def_app_icon);
            }
            mDialogLabel.setText(friendlyName);
            mDialogLabel.setTag(uri);
            mDialogIcon.setImageDrawable(resizeForDialog(icon));
            mDialogIcon.setTag(null);
        } catch (Exception e) {
        }
    }

    private Drawable resizeForDialog(Drawable image) {
        int size = (int) mResources.getDimensionPixelSize(com.android.internal.R.dimen.system_bar_icon_size);
        Bitmap d = ((BitmapDrawable)image).getBitmap();
        Bitmap bitmapOrig = Bitmap.createScaledBitmap(d, size, size, false);
        return new BitmapDrawable(mResources, bitmapOrig);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        String shortcut_name = null;
        if (data != null) {
            shortcut_name = data.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);
        }
        if (shortcut_name != null && shortcut_name.equals(EMPTY_LABEL)) {
            mDialogLabel.setText(EMPTY_LABEL);
            mDialogLabel.setTag(EMPTY_TARGET);
            mDialogIcon.setImageResource(R.drawable.ic_empty);
        } else if (requestCode == IconPicker.REQUEST_PICK_SYSTEM || requestCode == IconPicker.REQUEST_PICK_GALLERY
                || requestCode == IconPicker.REQUEST_PICK_ICON_PACK) {
            mIconPicker.onActivityResult(requestCode, resultCode, data);
        } else if (requestCode != Activity.RESULT_CANCELED && resultCode != Activity.RESULT_CANCELED) {
            mPicker.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void iconPicked(int requestCode, int resultCode, Intent in) {
        Drawable ic = null;
        String iconType = null;
        String pkgName = null;
        String iconSource = null;
        if (requestCode == IconPicker.REQUEST_PICK_GALLERY) {
            if (resultCode == Activity.RESULT_OK) {
                File mImage = new File(mActivity.getFilesDir() + "/shortcuts_" + System.currentTimeMillis() + ".png");
                if (mImageTmp.exists()) {
                    mImageTmp.renameTo(mImage);
                }
                mImage.setReadable(true, false);
                iconType = ICON_FILE;
                iconSource = mImage.toString();
                ic = new BitmapDrawable(getResources(), BitmapFactory.decodeFile(mImage.toString()));
            } else {
                if (mImageTmp.exists()) {
                    mImageTmp.delete();
                }
                return;
            }
        } else if (requestCode == IconPicker.REQUEST_PICK_SYSTEM) {
            String resourceName = in.getStringExtra(IconPicker.RESOURCE_NAME);
            ic = mResources.getDrawable(mResources.getIdentifier(resourceName, "drawable", "android")).mutate();
            iconType = ICON_RESOURCE;
            iconSource = resourceName;
        } else if (requestCode == IconPicker.REQUEST_PICK_ICON_PACK && resultCode == Activity.RESULT_OK) {
            String resourceName = in.getStringExtra(IconPicker.RESOURCE_NAME);
            pkgName = in.getStringExtra(IconPicker.PACKAGE_NAME);
            try {
                Context rContext = mActivity.createPackageContext(pkgName, 0);
                int id = rContext.getResources().getIdentifier(resourceName, "drawable", pkgName);
                ic = rContext.getResources().getDrawable(id);
            } catch (NameNotFoundException e) {
                e.printStackTrace();
            }
            iconType = ICON_RESOURCE;
            iconSource = resourceName;
        } else {
            return;
        }
        TargetInfo tmpIcon = new TargetInfo(null);
        tmpIcon.iconType = iconType;
        tmpIcon.iconSource = iconSource;
        tmpIcon.pkgName = pkgName;
        mDialogIcon.setTag(tmpIcon);
        mDialogIcon.setImageDrawable(ic);
    }
}
