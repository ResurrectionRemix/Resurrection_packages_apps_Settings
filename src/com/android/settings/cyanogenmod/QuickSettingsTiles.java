/*
 * Copyright (C) 2012 The CyanogenMod Project
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

package com.android.settings.cyanogenmod;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.cyanogenmod.QuickSettingsUtil.TileInfo;

import java.util.ArrayList;
public class QuickSettingsTiles extends Fragment {

    private static final int MENU_RESET = Menu.FIRST;

    DraggableGridView mDragView;
    private ViewGroup mContainer;
    LayoutInflater mInflater;
    Resources mSystemUiResources;
    TileAdapter mTileAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mDragView = new DraggableGridView(getActivity(), null);
        mContainer = container;
        mInflater = inflater;
        PackageManager pm = getActivity().getPackageManager();
        if (pm != null) {
            try {
                mSystemUiResources = pm.getResourcesForApplication("com.android.systemui");
            } catch (Exception e) {
                mSystemUiResources = null;
            }
        }
        mTileAdapter = new TileAdapter(getActivity(), 0);
        return mDragView;
    }

    void genTiles() {
        mDragView.removeAllViews();
        ArrayList<String> tiles = QuickSettingsUtil.getTileListFromString(QuickSettingsUtil.getCurrentTiles(getActivity()));
        for (String tileindex : tiles) {
            QuickSettingsUtil.TileInfo tile = QuickSettingsUtil.TILES.get(tileindex);
            if (tile != null) {
                addTile(tile.getTitleResId(), tile.getIcon(), 0, false);
            }
        }
        addTile(R.string.tiles_add_title, null, R.drawable.ic_menu_add, false);
    }

    /**
     * Adds a tile to the dragview
     * @param titleId - string id for tile text in systemui
     * @param iconSysId - resource id for icon in systemui
     * @param iconRegId - resource id for icon in local package
     * @param newTile - whether a new tile is being added by user
     */
    void addTile(int titleId, String iconSysId, int iconRegId, boolean newTile) {
        View v = (View) mInflater.inflate(R.layout.qs_tile, null, false);
        final TextView name = (TextView) v.findViewById(R.id.qs_text);
        name.setText(titleId);
        if (mSystemUiResources != null && iconSysId != null) {
            int resId = mSystemUiResources.getIdentifier(iconSysId, null, null);
            if (resId > 0) {
                try {
                    Drawable d = mSystemUiResources.getDrawable(resId);
                    name.setCompoundDrawablesRelativeWithIntrinsicBounds(null, d, null, null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            name.setCompoundDrawablesRelativeWithIntrinsicBounds(0, iconRegId, 0, 0);
        }
        mDragView.addView(v, newTile ? mDragView.getChildCount() - 1 : mDragView.getChildCount());
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        genTiles();
        mDragView.setOnRearrangeListener(new OnRearrangeListener() {
            public void onRearrange(int oldIndex, int newIndex) {
                ArrayList<String> tiles = QuickSettingsUtil.getTileListFromString(QuickSettingsUtil.getCurrentTiles(getActivity()));
                String oldTile = tiles.get(oldIndex);
                tiles.remove(oldIndex);
                tiles.add(newIndex, oldTile);
                QuickSettingsUtil.saveCurrentTiles(getActivity(), QuickSettingsUtil.getTileStringFromList(tiles));
            }
            @Override
            public void onDelete(int index) {
                ArrayList<String> tiles = QuickSettingsUtil.getTileListFromString(QuickSettingsUtil.getCurrentTiles(getActivity()));
                tiles.remove(index);
                QuickSettingsUtil.saveCurrentTiles(getActivity(), QuickSettingsUtil.getTileStringFromList(tiles));
            }
        });
        mDragView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                if (arg2 != mDragView.getChildCount() - 1) return;
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.tile_choose_title)
                .setAdapter(mTileAdapter, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, final int position) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                ArrayList<String> curr = QuickSettingsUtil.getTileListFromString(QuickSettingsUtil.getCurrentTiles(getActivity()));
                                curr.add(mTileAdapter.getTileId(position));
                                QuickSettingsUtil.saveCurrentTiles(getActivity(), QuickSettingsUtil.getTileStringFromList(curr));
                            }
                        }).start();
                        TileInfo info = QuickSettingsUtil.TILES.get(mTileAdapter.getTileId(position));
                        addTile(info.getTitleResId(), info.getIcon(), 0, true);
                    }
                });
                builder.create().show();
            }
        });

        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!Utils.isTablet()) {
            mContainer.setPadding(20, 0, 0, 0);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        menu.add(0, MENU_RESET, 0, R.string.tiles_reset_title)
                .setIcon(R.drawable.ic_settings_backup) // use the backup icon
                .setAlphabeticShortcut('r')
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM |
                MenuItem.SHOW_AS_ACTION_WITH_TEXT);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_RESET:
                resetTiles();
                return true;
            default:
                return false;
        }
    }

    private void resetTiles() {
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
        alert.setTitle(R.string.tiles_reset_title);
        alert.setMessage(R.string.tiles_reset_message);
        alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                QuickSettingsUtil.resetTiles(getActivity());
                genTiles();
            }
        });
        alert.setNegativeButton(R.string.cancel, null);
        alert.create().show();
    }

    @SuppressWarnings("rawtypes")
    static class TileAdapter extends ArrayAdapter {

        String[] mTileKeys;
        Resources mResources;

        public TileAdapter(Context context, int textViewResourceId) {
            super(context, android.R.layout.simple_list_item_1);
            mTileKeys = new String[getCount()];
            QuickSettingsUtil.TILES.keySet().toArray(mTileKeys);
            mResources = context.getResources();
        }

        @Override
        public int getCount() {
            return QuickSettingsUtil.TILES.size();
        }

        @Override
        public Object getItem(int position) {
            int resid = QuickSettingsUtil.TILES.get(mTileKeys[position])
                    .getTitleResId();
            return mResources.getString(resid);
        }

        public String getTileId(int position) {
            return QuickSettingsUtil.TILES.get(mTileKeys[position])
                    .getId();
        }

    }

    public interface OnRearrangeListener {
        public abstract void onRearrange(int oldIndex, int newIndex);
        public abstract void onDelete(int index);
    }
}
