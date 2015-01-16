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
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.settings.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class QSTiles extends Fragment implements
        DraggableGridView.OnRearrangeListener, AdapterView.OnItemClickListener {

    private static final String[] AVAILABLE_TILES = {
        "wifi" ,"bt", "cell", "airplane", "rotation", "flashlight",
        "location", "cast", "inversion", "hotspot"
    };

    private static final String QS_DEFAULT_ORDER =
            "wifi,bt,cell,airplane,rotation,flashlight,location,cast";

    private DraggableGridView mDraggableGridView;

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

        ContentResolver resolver = getActivity().getContentResolver();
        String order = Settings.System.getString(resolver, Settings.System.QS_TILES);
        if (TextUtils.isEmpty(order)) {
            order = QS_DEFAULT_ORDER;
            Settings.System.putString(resolver, Settings.System.QS_TILES, order);
        }

        for (String tileType: order.split(",")) {
            mDraggableGridView.addView(buildQSTile(tileType));
        }
        // Add a dummy tile for the "Add / Delete" tile
        mDraggableGridView.addView(buildQSTile(""));

        mDraggableGridView.setOnRearrangeListener(this);
        mDraggableGridView.setOnItemClickListener(this);
        mDraggableGridView.setMaxItemCount(AVAILABLE_TILES.length);
    }

    @Override
    public void onChange() {
        updateSettings();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // Add / delete button clicked
        if (position == mDraggableGridView.getChildCount() - 1) {
            addTile();
        }
    }

    private void addTile() {
        ContentResolver resolver = getActivity().getContentResolver();

        // We load the added tiles and compare it to the list of available tiles.
        // We only show the tiles that aren't already on the grid.
        String order = Settings.System.getString(resolver, Settings.System.QS_TILES);

        List<String> savedTiles = Arrays.asList(order.split(","));

        List<QSTileHolder> tilesList = new ArrayList<QSTileHolder>();
        for (String tile : AVAILABLE_TILES) {
            // Don't count the already added tiles
            if (savedTiles.contains(tile)) continue;
            // Don't count the dummy tile
            if (tile.equals("")) continue;
            tilesList.add(QSTileHolder.from(getActivity(), tile));
        }

        if (tilesList.isEmpty()) {
            return;
        }

        ListView listView = new ListView(getActivity());
        listView.setAdapter(new QSListAdapter(getActivity(), tilesList));

        final AlertDialog addTileDialog = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.add_qs)
                .setView(listView)
                .setNegativeButton(R.string.cancel, null)
                .show();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Close dialog and add the new tile to the last available position
                // before "Add / Delete" tile
                int newPosition = mDraggableGridView.getChildCount() - 1;
                if (newPosition < 0) newPosition = 0;
                addTileDialog.dismiss();

                QSTileHolder holder = (QSTileHolder) parent.getItemAtPosition(position);
                mDraggableGridView.addView(buildQSTile(holder.value), newPosition);
                mDraggableGridView.updateAddDeleteState();
            }
        });
    }

    public void updateSettings() {
        ContentResolver resolver = getActivity().getContentResolver();
        String order = "";

        // Add every tile except the last one (Add / Delete) to the list
        for (int i = 0; i < mDraggableGridView.getChildCount()-1; i++) {
            if (i > 0) {
                order += ",";
            }
            order += mDraggableGridView.getChildAt(i).getTag();
        }

        Settings.System.putString(resolver, Settings.System.QS_TILES, order);
    }

    private View buildQSTile(String tileType) {
        QSTileHolder item = QSTileHolder.from(getActivity(), tileType);
        View qsTile = getLayoutInflater(null).inflate(R.layout.qs_item, null);

        if (item.name != null) {
            ImageView icon = (ImageView) qsTile.findViewById(android.R.id.icon);
            icon.setImageResource(item.drawableId);
            TextView title = (TextView) qsTile.findViewById(android.R.id.title);
            title.setText(item.name);
        }
        qsTile.setTag(tileType);

        return qsTile;
    }

    public static int determineTileCount(Context context) {
        String order = Settings.System.getString(context.getContentResolver(),
                Settings.System.QS_TILES);
        if (TextUtils.isEmpty(order)) {
            order = QS_DEFAULT_ORDER;
        }
        return order.split(",").length;
    }
}
