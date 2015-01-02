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
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;
import com.android.settings.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class QSTiles extends Fragment {

    private static final String[] AVAILABLE_TILES = {
        "wifi" ,"bt", "cell", "airplane", "rotation", "flashlight", "location", "cast"
    };

    private static final String QS_DEFAULT_ORDER =
            "wifi,bt,inversion,cell,airplane,rotation,flashlight,location,cast,hotspot";

    DraggableGridView draggableGridView;

    private static final String TILES_SETTING = "sysui_qs_tiles";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.settings_qs_tiles, container, false);
        draggableGridView = (DraggableGridView) v.findViewById(R.id.qs_gridview);
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ContentResolver resolver = getActivity().getContentResolver();
        String order = Settings.Secure.getStringForUser(resolver, TILES_SETTING,
                UserHandle.myUserId());
        if (TextUtils.isEmpty(order)) {
            order = QS_DEFAULT_ORDER;
            Settings.Secure.putStringForUser(resolver, TILES_SETTING, order, UserHandle.myUserId());
        }

        String[] tiles = order.split(",");
        for (String tileType: tiles) {
            draggableGridView.addView(QSUtils.parseQSTile(getActivity(), tileType));
        }
        // Add a false tile for the "Add / Delete" tile
        draggableGridView.addView(QSUtils.parseQSTile(getActivity(), ""));

        draggableGridView.setOnRearrangeListener(new DraggableGridView.OnRearrangeListener() {

            @Override
            public void onChange() {
                updateSettings();
            }
        });

        draggableGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                // Add / delete button clicked
                if (i == draggableGridView.getChildCount()-1) {
                    addTile();
                }
            }
        });

        draggableGridView.toggleAddDelete(false);
    }

    public void addTile() {
        ContentResolver resolver = getActivity().getContentResolver();

        // We load the added tiles and compare it to the list of available tiles.
        // We only show the tiles that aren't already on the grid.
        String order = Settings.Secure.getStringForUser(resolver, TILES_SETTING,
                UserHandle.myUserId());

        List<String> savedTiles = Arrays.asList(order.split(","));

        List<QSTileHolder> tilesList = new ArrayList<QSTileHolder>();
        for (String tile : AVAILABLE_TILES) {
            // Don't count the already added tiles
            if (savedTiles.contains(tile)) continue;
            // Don't count the false tile
            if (tile.equals("")) continue;
            tilesList.add(QSUtils.getAdapterItem(getActivity(), tile));
        }

        if (!tilesList.isEmpty()) {
            ListView listView = new ListView(getActivity());
            listView.setAdapter(new QSListAdapter(getActivity(), -1, tilesList));

            final AlertDialog addTileDialog = new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.add_qs)
                    .setView(listView)
                    .setNegativeButton(R.string.cancel, null)
                    .show();

            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    // Close dialog and add the new tile to the last available position
                    // before "Add / Delete" tile
                    int newPosition = draggableGridView.getChildCount()-1;
                    if (newPosition < 0) newPosition = 0;
                    addTileDialog.dismiss();
                    draggableGridView.addView(QSUtils.parseQSTile(getActivity(),
                            ((QSTileHolder) adapterView.getItemAtPosition(i)).getValue()),
                            newPosition);
                }
            });
        } else {
            // If there aren't any tiles to add, just show an error toast
            Toast.makeText(getActivity(), getString(R.string.qs_already_added_all),
                    Toast.LENGTH_SHORT).show();
        }
    }

    public void updateSettings() {
        ContentResolver resolver = getActivity().getContentResolver();
        String order = "";

        // Add every tile except the last one (Add / Delete) to the list
        for (int i = 0; i < draggableGridView.getChildCount()-1; i++) {
            if (i > 0) {
                order += ",";
            }
            order += draggableGridView.getChildAt(i).getTag();
        }

        Settings.Secure.putStringForUser(resolver, TILES_SETTING, order, UserHandle.myUserId());
    }
}
