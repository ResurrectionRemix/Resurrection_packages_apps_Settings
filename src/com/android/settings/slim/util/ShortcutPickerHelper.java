/*
 * Copyright (C) 2014 SlimRoms Project
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

package com.android.settings.slim.util;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;

import com.android.settings.R;

import com.android.internal.util.slim.AppHelper;

import java.util.ArrayList;

public class ShortcutPickerHelper {

    public static final int REQUEST_PICK_SHORTCUT = 100;
    public static final int REQUEST_PICK_APPLICATION = 101;
    public static final int REQUEST_CREATE_SHORTCUT = 102;

    private Activity mParent;
    private OnPickListener mListener;
    private PackageManager mPackageManager;
    private int lastFragmentId;

    public interface OnPickListener {
        void shortcutPicked(String uri, String friendlyName, Bitmap bmp, boolean isApplication);
    }

    public ShortcutPickerHelper(Activity parent, OnPickListener listener) {
        mParent = parent;
        mPackageManager = mParent.getPackageManager();
        mListener = listener;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_PICK_APPLICATION:
                    completeSetCustomApp(data);
                    break;
                case REQUEST_CREATE_SHORTCUT:
                    completeSetCustomShortcut(data);
                    break;
                case REQUEST_PICK_SHORTCUT:
                    processShortcut(data, REQUEST_PICK_APPLICATION, REQUEST_CREATE_SHORTCUT);
                    break;
            }
        }
    }

    public void pickShortcut(int fragmentId) {
        pickShortcut(fragmentId, false);
    }

    public void pickShortcut(int fragmentId, boolean fullAppsOnly) {
        lastFragmentId = fragmentId;

        if (fullAppsOnly) {
            Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

            Intent pickIntent = new Intent(Intent.ACTION_PICK_ACTIVITY);
            pickIntent.putExtra(Intent.EXTRA_INTENT, mainIntent);
            startFragmentOrActivity(pickIntent, REQUEST_PICK_APPLICATION);
        } else {
            Bundle bundle = new Bundle();

            ArrayList<String> shortcutNames = new ArrayList<String>();
            shortcutNames.add(mParent.getString(R.string.shortcuts_applications));
            bundle.putStringArrayList(Intent.EXTRA_SHORTCUT_NAME, shortcutNames);

            ArrayList<ShortcutIconResource> shortcutIcons = new ArrayList<ShortcutIconResource>();
            shortcutIcons.add(ShortcutIconResource.fromContext(mParent,
                    android.R.drawable.sym_def_app_icon));
            bundle.putParcelableArrayList(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, shortcutIcons);

            Intent pickIntent = new Intent(Intent.ACTION_PICK_ACTIVITY);
            pickIntent.putExtra(Intent.EXTRA_INTENT, new Intent(Intent.ACTION_CREATE_SHORTCUT));
            pickIntent.putExtra(Intent.EXTRA_TITLE, mParent.getText(
                    R.string.shortcuts_select_custom_app_title));
            pickIntent.putExtras(bundle);
            startFragmentOrActivity(pickIntent, REQUEST_PICK_SHORTCUT);
        }
    }

    private void startFragmentOrActivity(Intent pickIntent, int requestCode) {
        if (lastFragmentId == 0) {
            mParent.startActivityForResult(pickIntent, requestCode);
        } else {
            Fragment cFrag = mParent.getFragmentManager().findFragmentById(lastFragmentId);
            if (cFrag != null) {
                mParent.startActivityFromFragment(cFrag, pickIntent, requestCode);
            }
        }
    }

    private void processShortcut(Intent intent,
        int requestCodeApplication, int requestCodeShortcut) {
        // Handle case where user selected "Applications"
        String applicationName = mParent.getResources().getString(R.string.shortcuts_applications);
        String shortcutName = intent.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);

        if (applicationName != null && applicationName.equals(shortcutName)) {
            Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

            Intent pickIntent = new Intent(Intent.ACTION_PICK_ACTIVITY);
            pickIntent.putExtra(Intent.EXTRA_INTENT, mainIntent);
            startFragmentOrActivity(pickIntent, requestCodeApplication);
        } else {
            startFragmentOrActivity(intent, requestCodeShortcut);
        }
    }

    private void completeSetCustomApp(Intent data) {
        mListener.shortcutPicked(data.toUri(0),
            AppHelper.getFriendlyActivityName(mParent, mPackageManager, data, false), null, true);
    }

    private void completeSetCustomShortcut(Intent data) {
        Intent intent = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT);
        /* preserve shortcut name, we want to restore it later */
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, data.getStringExtra(
                Intent.EXTRA_SHORTCUT_NAME));
        String appUri = intent.toUri(0);
        appUri = appUri.replaceAll("com.android.contacts.action.QUICK_CONTACT",
                "android.intent.action.VIEW");

        // Check if icon is present
        Bitmap bmp = null;
        Parcelable extra = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON);
        if (extra != null && extra instanceof Bitmap) {
            bmp = (Bitmap) extra;
        }
        // No icon till now check if icon resource is present
        if (bmp == null) {
            extra = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE);
            if (extra != null && extra instanceof Intent.ShortcutIconResource) {
                try {
                    Intent.ShortcutIconResource iconResource = (ShortcutIconResource) extra;
                    Resources resources =
                            mPackageManager.getResourcesForApplication(iconResource.packageName);
                    final int id = resources.getIdentifier(iconResource.resourceName, null, null);
                    bmp = BitmapFactory.decodeResource(resources, id);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        mListener.shortcutPicked(appUri,
                AppHelper.getFriendlyShortcutName(mParent, mPackageManager, intent), bmp, false);
    }

}
