/*
 * Copyright (C) 2014 Slimroms
 * Copyright (C) 2015-2017 Android Ice Cold Project
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

package com.android.settings.rr.dslv;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.ListFragment;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.Snackbar;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ListView;

import com.android.internal.util.rr.ActionConfig;
import com.android.internal.util.rr.ActionConstants;
import com.android.internal.util.rr.ActionHelper;
import com.android.internal.util.rr.ImageHelper;
import com.android.internal.util.rr.DeviceUtils;
import com.android.internal.util.rr.DeviceUtils.FilteredDeviceFeaturesArray;

import com.android.settings.R;
import com.android.settings.rr.dslv.DragSortListView;
import com.android.settings.rr.dslv.DragSortController;
import com.android.settings.rr.utils.SlimShortcutPickerHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

public class ActionListViewSettings extends ListFragment implements
            SlimShortcutPickerHelper.OnPickListener {

    private static final int DLG_SHOW_ACTION_DIALOG   = 0;
    private static final int DLG_SHOW_ICON_PICKER     = 1;
    private static final int DLG_DELETION_NOT_ALLOWED = 2;
    private static final int DLG_SHOW_HELP_SCREEN     = 3;
    private static final int DLG_RESET_TO_DEFAULT     = 4;

    private static final int MENU_HELP = Menu.FIRST;
    private static final int MENU_ADD = MENU_HELP + 1;
    private static final int MENU_RESET = MENU_ADD + 1;

    private static final int NAV_BAR               = 0;
    private static final int PIE                   = 1;
    private static final int PIE_SECOND            = 2;
    private static final int NAV_RING              = 3;
    private static final int LOCKSCREEN_SHORTCUT   = 4;
    private static final int POWER_MENU_SHORTCUT   = 5;
    private static final int SHAKE_EVENTS_DISABLED = 6;
    private static final int RECENT_APP_SIDEBAR    = 7;

    private static final int DEFAULT_MAX_ACTION_NUMBER = 5;
    private static final int DEFAULT_NUMBER_OF_ACTIONS = 3;

    public static final int REQUEST_PICK_CUSTOM_ICON = 1000;

    private int mActionMode;
    private int mMaxAllowedActions;
    private int mDefaultNumberOfActions;
    private boolean mUseAppPickerOnly;
    private boolean mUseFullAppsOnly;
    private boolean mDisableLongpress;
    private boolean mDisableIconPicker;
    private boolean mDisableDeleteLastEntry;

    private TextView mDisableMessage;

    private ActionConfigsAdapter mActionConfigsAdapter;

    private ArrayList<ActionConfig> mActionConfigs;
    private ActionConfig mActionConfig;

    private boolean mAdditionalFragmentAttached;
    private String mAdditionalFragment;
    private View mDivider;
    private View mFab;

    private int mPendingIndex = -1;
    private boolean mPendingLongpress;
    private boolean mPendingNewAction;

    private String[] mActionDialogValues;
    private String[] mActionDialogEntries;
    private String mActionValuesKey;
    private String mActionEntriesKey;

    private Activity mActivity;
    private SlimShortcutPickerHelper mPicker;

    private File mImageTmp;

    private DragSortListView.DropListener onDrop =
        new DragSortListView.DropListener() {
            @Override
            public void drop(int from, int to) {
                ActionConfig item = mActionConfigsAdapter.getItem(from);

                mActionConfigsAdapter.remove(item);
                mActionConfigsAdapter.insert(item, to);

                setConfig(mActionConfigs, false);
            }
        };

    private DragSortListView.RemoveListener onRemove =
        new DragSortListView.RemoveListener() {
            @Override
            public void remove(int which) {
                ActionConfig item = mActionConfigsAdapter.getItem(which);
                mActionConfigsAdapter.remove(item);
                if (mDisableDeleteLastEntry && mActionConfigs.size() == 0) {
                    mActionConfigsAdapter.add(item);
                    showDialogInner(DLG_DELETION_NOT_ALLOWED, 0, false, false);
                } else {
                    deleteIconFileIfPresent(item, true);
                    setConfig(mActionConfigs, false);
                    if (mActionConfigs.size() == 0) {
                        showDisableMessage(true);
                    }
                }
            }
        };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        return inflater.inflate(R.layout.action_list_view_main_recents, container, false);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = activity;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Resources res = getResources();

        mActionMode = getArguments().getInt("actionMode", NAV_BAR);
        mMaxAllowedActions = getArguments().getInt("maxAllowedActions", DEFAULT_MAX_ACTION_NUMBER);
        mDefaultNumberOfActions = getArguments().getInt("defaultNumberOfActions", DEFAULT_NUMBER_OF_ACTIONS);
        mAdditionalFragment = getArguments().getString("fragment", null);
        mActionValuesKey = getArguments().getString("actionValues", "shortcut_action_values");
        mActionEntriesKey = getArguments().getString("actionEntries", "shortcut_action_entries");
        mDisableLongpress = getArguments().getBoolean("disableLongpress", false);
        mUseAppPickerOnly = getArguments().getBoolean("useAppPickerOnly", false);
        mUseFullAppsOnly = getArguments().getBoolean("useOnlyFullAppPicker", false);
        mDisableIconPicker = getArguments().getBoolean("disableIconPicker", false);
        mDisableDeleteLastEntry = getArguments().getBoolean("disableDeleteLastEntry", false);

        mDisableMessage = (TextView) view.findViewById(R.id.disable_message);

        FilteredDeviceFeaturesArray finalActionDialogArray = new FilteredDeviceFeaturesArray();
        finalActionDialogArray = DeviceUtils.filterUnsupportedDeviceFeatures(mActivity,
            res.getStringArray(res.getIdentifier(
                    mActionValuesKey, "array", "com.android.settings")),
            res.getStringArray(res.getIdentifier(
                    mActionEntriesKey, "array", "com.android.settings")));
        mActionDialogValues = finalActionDialogArray.values;
        mActionDialogEntries = finalActionDialogArray.entries;

        mPicker = new SlimShortcutPickerHelper(mActivity, this);

        File folder = new File(Environment.getExternalStorageDirectory() + File.separator +
                ".rr" + File.separator + "icons");

        if (!folder.exists()) {
            folder.mkdirs();
        }

        mImageTmp = new File(folder.toString()
                + File.separator + "shortcut.tmp");

        DragSortListView listView = (DragSortListView) getListView();

        listView.setDropListener(onDrop);
        listView.setRemoveListener(onRemove);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                    long arg3) {
                if (mUseFullAppsOnly) {
                    if (mPicker != null) {
                        mPendingIndex = arg2;
                        mPendingLongpress = false;
                        mPendingNewAction = false;
                        mPicker.pickShortcut(getId(), true);
                    }
                } else if (!mUseAppPickerOnly) {
                    showDialogInner(DLG_SHOW_ACTION_DIALOG, arg2, false, false);
                } else {
                    if (mPicker != null) {
                        mPendingIndex = arg2;
                        mPendingLongpress = false;
                        mPendingNewAction = false;
                        mPicker.pickShortcut(getId());
                    }
                }
            }
        });

        if (!mDisableLongpress) {
            listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2,
                        long arg3) {
                    if (mUseFullAppsOnly) {
                        if (mPicker != null) {
                            mPendingIndex = arg2;
                            mPendingLongpress = true;
                            mPendingNewAction = false;
                            mPicker.pickShortcut(getId(), true);
                        }
                    } else if (!mUseAppPickerOnly) {
                        showDialogInner(DLG_SHOW_ACTION_DIALOG, arg2, true, false);
                    } else {
                        if (mPicker != null) {
                            mPendingIndex = arg2;
                            mPendingLongpress = true;
                            mPendingNewAction = false;
                            mPicker.pickShortcut(getId());
                        }
                    }
                    return true;
                }
            });
        }

        mActionConfigs = getConfig();

        if (mActionConfigs != null) {
            mActionConfigsAdapter = new ActionConfigsAdapter(mActivity, mActionConfigs);
            setListAdapter(mActionConfigsAdapter);
            showDisableMessage(mActionConfigs.size() == 0);
        }

        mDivider = (View) view.findViewById(R.id.divider);
        loadAdditionalFragment();

        mFab = view.findViewById(R.id.floating_action_button);
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mUseFullAppsOnly) {
                    if (mPicker != null) {
                        mPendingIndex = 0;
                        mPendingLongpress = false;
                        mPendingNewAction = true;
                        mPicker.pickShortcut(getId(), true);
                    }
                } else if (!mUseAppPickerOnly) {
                    showDialogInner(DLG_SHOW_ACTION_DIALOG, 0, false, true);
                } else {
                    if (mPicker != null) {
                        mPendingIndex = 0;
                        mPendingLongpress = false;
                        mPendingNewAction = true;
                        mPicker.pickShortcut(getId());
                    }
                }
            }
        });
        updateFabVisibility(mActionConfigs.size());

        // get shared preference
        SharedPreferences preferences =
                mActivity.getSharedPreferences("dslv_settings", Activity.MODE_PRIVATE);
        if (!preferences.getBoolean("first_help_shown_mode_" + mActionMode, false)) {
            preferences.edit()
                    .putBoolean("first_help_shown_mode_" + mActionMode, true).commit();
            showDialogInner(DLG_SHOW_HELP_SCREEN, 0, false, false);
        }

        setHasOptionsMenu(true);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mAdditionalFragmentAttached) {
            FragmentManager fragmentManager = getFragmentManager();
            Fragment fragment = fragmentManager.findFragmentById(R.id.fragment_container);
            if (fragment != null && !fragmentManager.isDestroyed()) {
                fragmentManager.beginTransaction().remove(fragment).commit();
            }
        }
    }

    private void loadAdditionalFragment() {
        if (mAdditionalFragment != null && !mAdditionalFragment.isEmpty()) {
            try {
                Class<?> classAdditionalFragment = Class.forName(mAdditionalFragment);
                Fragment fragment = (Fragment) classAdditionalFragment.newInstance();
                getFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment).commit();
                if (mDivider != null) {
                    mDivider.setVisibility(View.VISIBLE);
                }
                mAdditionalFragmentAttached = true;
            } catch (Exception e) {
                mAdditionalFragmentAttached = false;
                e.printStackTrace();
            }
        }
    }

    @Override
    public void shortcutPicked(String action,
                String description, Bitmap bmp, boolean isApplication) {
        if (mPendingIndex == -1) {
            return;
        }
        if (bmp != null && !mPendingLongpress) {
            // Icon is present, save it for future use and add the file path to the action.
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                File folder = new File(Environment.getExternalStorageDirectory() + File.separator +
                        ".rr" + File.separator + "icons");
                folder.mkdirs();
                String fileName = folder.toString()
                        + File.separator + "shortcut_" + System.currentTimeMillis() + ".png";
                try {
                    FileOutputStream out = new FileOutputStream(fileName);
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
                    out.close();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    action = action + "?hasExtraIcon=" + fileName;
                    File image = new File(fileName);
                    image.setReadable(true, false);
                }
            }
        }
        if (mPendingNewAction) {
            addNewAction(action, description);
        } else {
            updateAction(action, description, null, mPendingIndex, mPendingLongpress);
        }
        mPendingLongpress = false;
        mPendingNewAction = false;
        mPendingIndex = -1;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == SlimShortcutPickerHelper.REQUEST_PICK_SHORTCUT
                    || requestCode == SlimShortcutPickerHelper.REQUEST_PICK_APPLICATION
                    || requestCode == SlimShortcutPickerHelper.REQUEST_CREATE_SHORTCUT) {
                mPicker.onActivityResult(requestCode, resultCode, data);

            } else if (requestCode == REQUEST_PICK_CUSTOM_ICON && mPendingIndex != -1) {
                if (mImageTmp.length() == 0 || !mImageTmp.exists()) {
                    mPendingIndex = -1;
                    Snackbar.make(getView(), getString(R.string.shortcut_image_not_valid),
                            Snackbar.LENGTH_LONG).show();
                    return;
                }
                if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                    File folder = new File(Environment.getExternalStorageDirectory() +
                            File.separator + ".rr" + File.separator + "icons");
                    folder.mkdirs();
                    File image = new File(folder.toString() + File.separator
                            + "shortcut_" + System.currentTimeMillis() + ".png");
                    String path = image.getAbsolutePath();
                    mImageTmp.renameTo(image);
                    image.setReadable(true, false);
                    updateAction(null, null, path, mPendingIndex, false);
                    mPendingIndex = -1;
                }
            }
        } else {
            if (mImageTmp.exists()) {
                mImageTmp.delete();
            }
            mPendingLongpress = false;
            mPendingNewAction = false;
            mPendingIndex = -1;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void updateAction(String action, String description, String icon,
                int which, boolean longpress) {

        if (!longpress && checkForDuplicateMainNavActions(action)) {
            return;
        }

        ActionConfig actionConfig = mActionConfigsAdapter.getItem(which);
        mActionConfigsAdapter.remove(actionConfig);

        if (!longpress) {
            deleteIconFileIfPresent(actionConfig, false);
        }

        if (icon != null) {
            actionConfig.setIcon(icon);
        } else {
            if (longpress) {
                actionConfig.setLongpressAction(action);
                actionConfig.setLongpressActionDescription(description);
            } else {
                deleteIconFileIfPresent(actionConfig, true);
                actionConfig.setClickAction(action);
                actionConfig.setClickActionDescription(description);
                actionConfig.setIcon(ActionConstants.ICON_EMPTY);
            }
        }

        mActionConfigsAdapter.insert(actionConfig, which);
        showDisableMessage(false);
        setConfig(mActionConfigs, false);
    }

    private boolean checkForDuplicateMainNavActions(String action) {
        ActionConfig actionConfig;
        for (int i = 0; i < mActionConfigs.size(); i++) {
            actionConfig = mActionConfigsAdapter.getItem(i);
            if (actionConfig.getClickAction().equals(action)) {
                Snackbar.make(getView(), getString(R.string.shortcut_duplicate_entry),
                        Snackbar.LENGTH_LONG).show();
                return true;
            }
        }
        return false;
    }

    private void deleteIconFileIfPresent(ActionConfig action, boolean deleteShortCutIcon) {
        File oldImage = new File(action.getIcon());
        if (oldImage.exists()) {
            oldImage.delete();
        }
        oldImage = new File(action.getClickAction().replaceAll(".*?hasExtraIcon=", ""));
        if (oldImage.exists() && deleteShortCutIcon) {
            oldImage.delete();
        }
    }

    private void showDisableMessage(boolean show) {
        if (mDisableMessage == null || mDisableDeleteLastEntry) {
            return;
        }
        if (show) {
            mDisableMessage.setVisibility(View.VISIBLE);
        } else {
            mDisableMessage.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_RESET:
                    showDialogInner(DLG_RESET_TO_DEFAULT, 0, false, true);
                break;
            case MENU_HELP:
                    showDialogInner(DLG_SHOW_HELP_SCREEN, 0, false, true);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, MENU_HELP, 0, R.string.help)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        menu.add(0, MENU_RESET, 0, R.string.shortcut_action_reset)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
    }

    private void addNewAction(String action, String description) {
        if (checkForDuplicateMainNavActions(action)) {
            return;
        }
        ActionConfig actionConfig = new ActionConfig(
            action, description,
            ActionConstants.ACTION_NULL, getResources().getString(R.string.shortcut_action_none),
            ActionConstants.ICON_EMPTY);

            mActionConfigsAdapter.add(actionConfig);
            showDisableMessage(false);
            setConfig(mActionConfigs, false);
    }

    private ArrayList<ActionConfig> getConfig() {
        switch (mActionMode) {
            case LOCKSCREEN_SHORTCUT:
                return ActionHelper.getLockscreenShortcutConfig(mActivity);
/* Disabled for now till all features are back. Enable it step per step!!!!!!
            case NAV_BAR:
                return ActionHelper.getNavBarConfigWithDescription(
                    mActivity, mActionValuesKey, mActionEntriesKey);
            case NAV_RING:
                return ActionHelper.getNavRingConfigWithDescription(
                    mActivity, mActionValuesKey, mActionEntriesKey);
            case PIE:
                return ActionHelper.getPieConfigWithDescription(
                    mActivity, mActionValuesKey, mActionEntriesKey);
            case PIE_SECOND:
                return ActionHelper.getPieSecondLayerConfigWithDescription(
                    mActivity, mActionValuesKey, mActionEntriesKey);
            case POWER_MENU_SHORTCUT:
                return PolicyHelper.getPowerMenuConfigWithDescription(
                    mActivity, mActionValuesKey, mActionEntriesKey);
            case SHAKE_EVENTS_DISABLED:
                return ActionHelper.getDisabledShakeApps(mActivity);
*/
            case RECENT_APP_SIDEBAR:
                return ActionHelper.getRecentAppSidebarConfigWithDescription(
                        mActivity, mActionValuesKey, mActionEntriesKey);
        }
        return null;
    }

    private void setConfig(ArrayList<ActionConfig> actionConfigs, boolean reset) {
        switch (mActionMode) {
            case LOCKSCREEN_SHORTCUT:
                ActionHelper.setLockscreenShortcutConfig(mActivity, actionConfigs, reset);
                updateFabVisibility(reset ? mDefaultNumberOfActions : actionConfigs.size());
                break;
/* Disabled for now till all features are back. Enable it step per step!!!!!!
            case NAV_BAR:
                ActionHelper.setNavBarConfig(mActivity, actionConfigs, reset);
                updateFabVisibility(reset ? mDefaultNumberOfActions : actionConfigs.size());
                break;
            case NAV_RING:
                ActionHelper.setNavRingConfig(mActivity, actionConfigs, reset);
                break;
            case PIE:
                ActionHelper.setPieConfig(mActivity, actionConfigs, reset);
                updateFabVisibility(reset ? mDefaultNumberOfActions : actionConfigs.size());
                break;
            case PIE_SECOND:
                ActionHelper.setPieSecondLayerConfig(mActivity, actionConfigs, reset);
                updateFabVisibility(reset ? mDefaultNumberOfActions : actionConfigs.size());
                break;
            case POWER_MENU_SHORTCUT:
                PolicyHelper.setPowerMenuConfig(mActivity, actionConfigs, reset);
                break;
            case SHAKE_EVENTS_DISABLED:
                ActionHelper.setDisabledShakeApps(mActivity, actionConfigs, reset);
                break;
*/
            case RECENT_APP_SIDEBAR:
                ActionHelper.setRecentAppSidebarConfig(mActivity, actionConfigs, reset);
				updateFabVisibility(reset ? mDefaultNumberOfActions : actionConfigs.size());
                break;
        }
    }

    private void updateFabVisibility(int numberOfActions) {
        if (numberOfActions == mMaxAllowedActions) {
            mFab.setVisibility(View.GONE);
        } else {
            mFab.setVisibility(View.VISIBLE);
        }
    }

    private class ViewHolder {
        public TextView longpressActionDescriptionView;
        public ImageView iconView;
    }

    private class ActionConfigsAdapter extends ArrayAdapter<ActionConfig> {

        public ActionConfigsAdapter(Context context, List<ActionConfig> clickActionDescriptions) {
            super(context, R.layout.action_list_view_item_recents,
                    R.id.click_action_description, clickActionDescriptions);
        }

        public View getView(final int position, View convertView, ViewGroup parent) {
            View v = super.getView(position, convertView, parent);

            if (v != convertView && v != null) {
                ViewHolder holder = new ViewHolder();

                TextView longpressActionDecription =
                    (TextView) v.findViewById(R.id.longpress_action_description);
                ImageView icon = (ImageView) v.findViewById(R.id.icon);

                if (mDisableLongpress) {
                    longpressActionDecription.setVisibility(View.GONE);
                } else {
                    holder.longpressActionDescriptionView = longpressActionDecription;
                }

                holder.iconView = icon;

                v.setTag(holder);
            }

            ViewHolder holder = (ViewHolder) v.getTag();

            if (!mDisableLongpress) {
                holder.longpressActionDescriptionView.setText(
                    getResources().getString(R.string.shortcut_action_longpress)
                    + " " + getItem(position).getLongpressActionDescription());
            }

            Drawable d = null;
            String iconUri = getItem(position).getIcon();
            if (mActionMode == POWER_MENU_SHORTCUT) {
/* Disabled for now till slims power menu is back!!!!!!!!!!!!!!
                d = ImageHelper.resize(
                        mActivity, PolicyHelper.getPowerMenuIconImage(mActivity,
                        getItem(position).getClickAction(),
                        iconUri, false), 36); */
            } else {
                d = ImageHelper.resize(
                        mActivity, ActionHelper.getActionIconImage(mActivity,
                        getItem(position).getClickAction(),
                        iconUri), 48);
            }

            if (iconUri != null && iconUri.startsWith(ActionConstants.SYSTEM_ICON_IDENTIFIER)) {
                d.setTint(getResources().getColor(R.color.dslv_icon_dark));
            }
            holder.iconView.setImageDrawable(d);

            if (!mDisableIconPicker && holder.iconView.getDrawable() != null) {
                holder.iconView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mPendingIndex = position;
                        showDialogInner(DLG_SHOW_ICON_PICKER, 0, false, false);
                    }
                });
            }

            return v;
        }
    }

    private void showDialogInner(int id, int which, boolean longpress, boolean newAction) {
        DialogFragment newFragment =
            MyAlertDialogFragment.newInstance(id, which, longpress, newAction);
        newFragment.setTargetFragment(this, 0);
        newFragment.show(getFragmentManager(), "dialog " + id);
    }

    public static class MyAlertDialogFragment extends DialogFragment {

        public static MyAlertDialogFragment newInstance(int id,
                int which, boolean longpress, boolean newAction) {
            MyAlertDialogFragment frag = new MyAlertDialogFragment();
            Bundle args = new Bundle();
            args.putInt("id", id);
            args.putInt("which", which);
            args.putBoolean("longpress", longpress);
            args.putBoolean("newAction", newAction);
            frag.setArguments(args);
            return frag;
        }

        ActionListViewSettings getOwner() {
            return (ActionListViewSettings) getTargetFragment();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int id = getArguments().getInt("id");
            final int which = getArguments().getInt("which");
            final boolean longpress = getArguments().getBoolean("longpress");
            final boolean newAction = getArguments().getBoolean("newAction");
            switch (id) {
                case DLG_RESET_TO_DEFAULT:
                    return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.shortcut_action_reset)
                    .setMessage(R.string.reset_message)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.ok,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // first delete custom icons in case they exist
                            ArrayList<ActionConfig> actionConfigs = getOwner().getConfig();
                            for (int i = 0; i < actionConfigs.size(); i++) {
                                getOwner().deleteIconFileIfPresent(actionConfigs.get(i), true);
                            }

                            // reset provider values and action adapter to default
                            getOwner().setConfig(null, true);
                            getOwner().mActionConfigsAdapter.clear();

                            // Add the new default objects fetched from @getConfig()
                            actionConfigs = getOwner().getConfig();
                            final int newConfigsSize = actionConfigs.size();
                            for (int i = 0; i < newConfigsSize; i++) {
                                getOwner().mActionConfigsAdapter.add(actionConfigs.get(i));
                            }

                            // dirty helper if actionConfigs list has no entries
                            // to proper update the content. .notifyDatSetChanged()
                            // does not work in this case.
                            if (newConfigsSize == 0) {
                                ActionConfig emptyAction =
                                    new ActionConfig(null, null, null, null, null);
                                getOwner().mActionConfigsAdapter.add(emptyAction);
                                getOwner().mActionConfigsAdapter.remove(emptyAction);
                            }
                            getOwner().showDisableMessage(newConfigsSize == 0);
                        }
                    })
                    .create();
                case DLG_SHOW_HELP_SCREEN:
                    Resources res = getResources();
                    String finalHelpMessage;
                    String actionMode;
                    String icon = "";
                    switch (getOwner().mActionMode) {
                        case LOCKSCREEN_SHORTCUT:
                        case POWER_MENU_SHORTCUT:
                            actionMode = res.getString(R.string.shortcut_action_help_shortcut);
                            break;
                        case SHAKE_EVENTS_DISABLED:
                            actionMode = res.getString(R.string.shortcut_action_help_app);
                            break;
                        case NAV_BAR:
                        case NAV_RING:
                        case PIE:
                        case PIE_SECOND:
                        case RECENT_APP_SIDEBAR:
                        default:
                            actionMode = res.getString(R.string.shortcut_action_help_button);
                            break;
                    }
                    if (!getOwner().mDisableIconPicker) {
                        icon = res.getString(R.string.shortcut_action_help_icon);
                    }
                    finalHelpMessage = res.getString(
                        R.string.shortcut_action_help_main, actionMode, icon);
                    if (!getOwner().mDisableDeleteLastEntry) {
                        finalHelpMessage += " " + res.getString(
                                getOwner().mActionMode == PIE_SECOND
                                ? R.string.shortcut_action_help_pie_second_layer_delete_last_entry
                                : R.string.shortcut_action_help_delete_last_entry, actionMode);
                    }
                    return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.help)
                    .setMessage(finalHelpMessage)
                    .setNegativeButton(R.string.ok,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    })
                    .create();
                case DLG_DELETION_NOT_ALLOWED:
                    return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.shortcut_action_warning)
                    .setMessage(R.string.shortcut_action_warning_message)
                    .setNegativeButton(R.string.ok,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    })
                    .create();
                case DLG_SHOW_ACTION_DIALOG:
                    int title;
                    if (longpress) {
                        title = R.string.shortcut_action_select_action_longpress;
                    } else if (newAction) {
                        title = R.string.shortcut_action_select_action_newaction;
                    } else {
                        title = R.string.shortcut_action_select_action;
                    }

                    // for normal press action we filter out null value
                    // due it does not make sense to set a null action
                    // on normal press action
                    String[] values = null;
                    String[] entries = null;
                    if (!longpress) {
                        List<String> finalEntriesList = new ArrayList<String>();
                        List<String> finalValuesList = new ArrayList<String>();

                        for (int i = 0; i < getOwner().mActionDialogValues.length; i++) {
                            if (!getOwner().mActionDialogValues[i]
                                    .equals(ActionConstants.ACTION_NULL)) {
                                finalEntriesList.add(getOwner().mActionDialogEntries[i]);
                                finalValuesList.add(getOwner().mActionDialogValues[i]);
                            }
                        }

                        entries = finalEntriesList.toArray(new String[finalEntriesList.size()]);
                        values = finalValuesList.toArray(new String[finalValuesList.size()]);
                    }

                    final String[] finalDialogValues =
                        longpress ? getOwner().mActionDialogValues : values;
                    final String[] finalDialogEntries =
                        longpress ? getOwner().mActionDialogEntries : entries;

                    return new AlertDialog.Builder(getActivity())
                    .setTitle(title)
                    .setNegativeButton(R.string.cancel, null)
                    .setItems(finalDialogEntries,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int item) {
                            if (finalDialogValues[item].equals(ActionConstants.ACTION_APP)) {
                                if (getOwner().mPicker != null) {
                                    getOwner().mPendingIndex = which;
                                    getOwner().mPendingLongpress = longpress;
                                    getOwner().mPendingNewAction = newAction;
                                    getOwner().mPicker.pickShortcut(getOwner().getId());
                                }
                            } else {
                                if (newAction) {
                                    getOwner().addNewAction(finalDialogValues[item],
                                            finalDialogEntries[item]);
                                } else {
                                    getOwner().updateAction(finalDialogValues[item],
                                            finalDialogEntries[item],
                                            null, which, longpress);
                                }
                            }
                        }
                    })
                    .create();
                case DLG_SHOW_ICON_PICKER:
                    return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.shortcuts_icon_picker_type)
                    .setNegativeButton(R.string.cancel, null)
                    .setItems(R.array.icon_types,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            switch(which) {
                                case 0: // Default
                                    getOwner().updateAction(null, null,
                                        ActionConstants.ICON_EMPTY,
                                        getOwner().mPendingIndex, false);
                                    getOwner().mPendingIndex = -1;
                                    break;
                                case 1://  System defaults
                                
                                    ListView list = new ListView(getActivity());
                                    list.setAdapter(new IconAdapter());
                                    final Dialog holoDialog = new Dialog(getActivity());
                                    holoDialog.setTitle(
                                            R.string.shortcuts_icon_picker_choose_icon_title);
                                    holoDialog.setContentView(list);
                                    list.setOnItemClickListener(new OnItemClickListener() {
                                        @Override
                                        public void onItemClick(AdapterView<?> parent, View view,
                                                int position, long id) {
                                            IconAdapter adapter = (IconAdapter) parent.getAdapter();
                                            getOwner().updateAction(null, null,
                                                adapter.getItemReference(position),
                                                getOwner().mPendingIndex, false);
                                            getOwner().mPendingIndex = -1;
                                            holoDialog.cancel();
                                        }
                                    });
                                    holoDialog.show();
                                    break;
                                case 2:  //Custom user icon
                                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT, null);
                                    intent.setType("image/*");
                                    intent.putExtra("crop", "true");
                                    intent.putExtra("scale", true);
                                    intent.putExtra("outputFormat",
                                        Bitmap.CompressFormat.PNG.toString());
                                    intent.putExtra("aspectX", 100);
                                    intent.putExtra("aspectY", 100);
                                    intent.putExtra("outputX", 100);
                                    intent.putExtra("outputY", 100);
                                    try {
                                        getOwner().mImageTmp.createNewFile();
                                        getOwner().mImageTmp.setWritable(true, false);
                                        intent.putExtra(MediaStore.EXTRA_OUTPUT,
                                            Uri.fromFile(getOwner().mImageTmp));
                                        intent.putExtra("return-data", false);
                                        getOwner().startActivityForResult(
                                            intent, REQUEST_PICK_CUSTOM_ICON);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    } catch (ActivityNotFoundException e) {
                                        e.printStackTrace();
                                    }
                                    break;
                            }
                        }
                    })
                    .create();
            }
            throw new IllegalArgumentException("unknown id " + id);
        }

        @Override
        public void onCancel(DialogInterface dialog) {

        }

        public class IconAdapter extends BaseAdapter {

            TypedArray icons;
            String[] labels;
            int color;

            public IconAdapter() {
                labels = getResources().getStringArray(R.array.shortcut_icon_picker_labels);
                icons = getResources().obtainTypedArray(R.array.shortcut_icon_picker_icons);
                color = getResources().getColor(R.color.dslv_icon_dark);
            }

            @Override
            public Object getItem(int position) {
                return icons.getDrawable(position);
            }

            @Override
            public long getItemId(int position) {
                return 0;
            }

            @Override
            public int getCount() {
                return labels.length;
            }

            public String getItemReference(int position) {
                String name = icons.getString(position);
                int separatorIndex = name.lastIndexOf(File.separator);
                int periodIndex = name.lastIndexOf('.');
                return ActionConstants.SYSTEM_ICON_IDENTIFIER
                    + name.substring(separatorIndex + 1, periodIndex);
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View iView = convertView;
                if (convertView == null) {
                    iView = View.inflate(getActivity(), android.R.layout.simple_list_item_1, null);
                }
                TextView tt = (TextView) iView.findViewById(android.R.id.text1);
                tt.setText(labels[position]);
                Drawable ic = ((Drawable) getItem(position)).mutate();
                //ic.setTint(color);
                tt.setCompoundDrawablePadding(15);
                tt.setCompoundDrawablesWithIntrinsicBounds(ic, null, null, null);
                return iView;
            }
        }

    }

}
