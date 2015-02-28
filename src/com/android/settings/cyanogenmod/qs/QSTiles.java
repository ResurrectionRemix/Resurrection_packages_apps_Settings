/*
 * Copyright (C) 2015 The CyanogenMod Project
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
package com.android.settings.cyanogenmod.qs;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.internal.util.cm.QSUtils;
import com.android.settings.R;
import com.android.settings.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class QSTiles extends Fragment implements
        DraggableGridView.OnRearrangeListener, AdapterView.OnItemClickListener {
    private DraggableGridView mDraggableGridView;
    private View mAddDeleteTile;
    private boolean mDraggingActive;
    private Context mSystemUiContext;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.settings_qs_tiles, container, false);
        mDraggableGridView = (DraggableGridView) v.findViewById(R.id.qs_gridview);
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mSystemUiContext = Utils.createPackageContext(getActivity(), "com.android.systemui");

        ContentResolver resolver = getActivity().getContentResolver();
        String order = Settings.Secure.getString(resolver, Settings.Secure.QS_TILES);
        if (TextUtils.isEmpty(order)) {
            order = QSUtils.getDefaultTilesAsString(getActivity());;
            Settings.Secure.putString(resolver, Settings.Secure.QS_TILES, order);
        }

        for (String tileType: order.split(",")) {
            View tile = buildQSTile(tileType);
            if (tile != null) {
                mDraggableGridView.addView(tile);
            }
        }
        // Add a dummy tile for the "Add / Delete" tile
        mAddDeleteTile = buildQSTile(QSTileHolder.TILE_ADD_DELETE);
        mDraggableGridView.addView(mAddDeleteTile);
        updateAddDeleteState();

        mDraggableGridView.setOnRearrangeListener(this);
        mDraggableGridView.setOnItemClickListener(this);
        mDraggableGridView.setUseLargeFirstRow(Settings.Secure.getInt(resolver,
                Settings.Secure.QS_USE_MAIN_TILES, 1) == 1);
    }

    @Override
    public boolean onStartDrag(int position) {
        // add/delete tile shouldn't be dragged
        if (mDraggableGridView.getChildAt(position) == mAddDeleteTile) {
            return false;
        }
        mDraggingActive = true;
        updateAddDeleteState();
        return true;
    }

    @Override
    public void onEndDrag() {
        mDraggingActive = false;
        updateAddDeleteState();
        updateSettings();
    }

    @Override
    public boolean isDeleteTarget(int position) {
        return mDraggingActive && position == mDraggableGridView.getChildCount() - 1;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // Add / delete button clicked
        if (view == mAddDeleteTile) {
            addTile();
        }
    }

    private void updateAddDeleteState() {
        int activeTiles = mDraggableGridView.getChildCount() - (mDraggingActive ? 2 : 1);
        boolean limitReached = activeTiles >= QSUtils.getAvailableTiles(getActivity()).size();
        int iconResId = mDraggingActive ? R.drawable.ic_menu_delete : R.drawable.ic_menu_add_dark;
        int titleResId = mDraggingActive ? R.string.qs_action_delete :
                limitReached ? R.string.qs_action_no_more_tiles : R.string.qs_action_add;

        TextView title = (TextView) mAddDeleteTile.findViewById(android.R.id.title);
        ImageView icon = (ImageView) mAddDeleteTile.findViewById(android.R.id.icon);

        title.setText(titleResId);
        title.setEnabled(!limitReached);

        icon.setImageResource(iconResId);
        icon.setEnabled(!limitReached);
    }

    private void addTile() {
        ContentResolver resolver = getActivity().getContentResolver();

        // We load the added tiles and compare it to the list of available tiles.
        // We only show the tiles that aren't already on the grid.
        String order = Settings.Secure.getString(resolver, Settings.Secure.QS_TILES);

        List<String> savedTiles = Arrays.asList(order.split(","));

        final List<QSTileHolder> tilesList = new ArrayList<QSTileHolder>();
        for (String tile : QSUtils.getAvailableTiles(getActivity())) {
            // Don't count the already added tiles
            if (!savedTiles.contains(tile)) {
                QSTileHolder holder = QSTileHolder.from(getActivity(), tile);
                if (holder != null) {
                    tilesList.add(holder);
                }
            }
        }

        if (tilesList.isEmpty()) {
            return;
        }

        final DialogInterface.OnClickListener selectionListener =
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Add the new tile to the last available position before "Add / Delete" tile
                int newPosition = mDraggableGridView.getChildCount() - 1;
                if (newPosition < 0) newPosition = 0;

                QSTileHolder holder = tilesList.get(which);
                mDraggableGridView.addView(buildQSTile(holder.value), newPosition);
                updateAddDeleteState();
                updateSettings();
                dialog.dismiss();
            }
        };

        Collections.sort(tilesList, new Comparator<QSTileHolder>() {
            @Override
            public int compare(QSTileHolder lhs, QSTileHolder rhs) {
                return lhs.name.compareTo(rhs.name);
            }
        });

        final QSListAdapter adapter = new QSListAdapter(getActivity(),
                mSystemUiContext, tilesList);
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.add_qs)
                .setSingleChoiceItems(adapter, -1, selectionListener)
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void updateSettings() {
        ContentResolver resolver = getActivity().getContentResolver();
        StringBuilder tiles = new StringBuilder();

        // Add every tile except the last one (Add / Delete) to the list
        for (int i = 0; i < mDraggableGridView.getChildCount(); i++) {
            String type = (String) mDraggableGridView.getChildAt(i).getTag();
            if (!TextUtils.isEmpty(type)) {
                if (tiles.length() > 0) {
                    tiles.append(",");
                }
                tiles.append(type);
            }
        }

        Settings.Secure.putString(resolver, Settings.Secure.QS_TILES, tiles.toString());
    }

    private View buildQSTile(String tileType) {
        QSTileHolder item = QSTileHolder.from(getActivity(), tileType);
        if (item == null) {
            return null;
        }
        ColoringCardView qsTile = (ColoringCardView) getLayoutInflater(null)
                .inflate(R.layout.qs_item, null);
        int defaultColor = getResources().getColor(R.color.qs_tile_default_background_color);
        qsTile.setColor(defaultColor);
        if (item.name != null) {
            ImageView icon = (ImageView) qsTile.findViewById(android.R.id.icon);
            Drawable d = Utils.getNamedDrawable(mSystemUiContext, item.resourceName);
            d.setColorFilter(getResources().getColor(R.color.qs_tile_tint_color),
                    PorterDuff.Mode.SRC_ATOP);
            icon.setImageDrawable(d);
            TextView title = (TextView) qsTile.findViewById(android.R.id.title);
            title.setText(item.name);
        }
        qsTile.setTag(tileType);

        return qsTile;
    }

    public static int determineTileCount(Context context) {
        String order = Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.QS_TILES);
        if (TextUtils.isEmpty(order)) {
            order = QSUtils.getDefaultTilesAsString(context);
        }
        return order.split(",").length;
    }
}
