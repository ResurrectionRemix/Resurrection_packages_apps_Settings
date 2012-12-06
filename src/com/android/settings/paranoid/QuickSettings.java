/*
 * Copyright (C) 2011 The CyanogenMod Project
 * Copyright (C) 2011 The AOKP Project
 * This code has been modified.  Portions copyright (C) 2012, ParanoidAndroid Project.
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

package com.android.settings.paranoid;

import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.app.ListFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.android.settings.paranoid.TouchInterceptor;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.R;

import java.util.ArrayList;
import java.util.Arrays;

public class QuickSettings extends SettingsPreferenceFragment implements OnPreferenceChangeListener {

    private static final String TAG = "QuickSettings";

    private static final String PREF_ENABLED_TILES = "enabled_tiles";
    private static final String PREF_TILES_ORDER = "tiles_order";

    private static final String TILE_SEPARATOR = "|";
    private static final String WIFI = "WIFI";
    private static final String BLUETOOTH = "BLUETOOTH";
    private static final String LOCATION = "LOCATION";
    private static final String DATA = "DATA";
    private static final String NFC = "NFC";
    private static final String SCREEN_ROTATION = "SCREEN_ROTATION";
    private static final String AIRPLANE = "AIRPLANE";
    private static final String BATTERY = "BATTERY";
    private static final String SYNC = "SYNC";

    private static final String DEFAULT_TILES = WIFI + TILE_SEPARATOR
            + DATA + TILE_SEPARATOR + BATTERY + TILE_SEPARATOR
            + AIRPLANE + TILE_SEPARATOR + BLUETOOTH;

    // Arrays containing the entire set of tiles
    private static ArrayList<String> allEntries;
    private static ArrayList<String> allValues;

    // Filtered entries, removed unexistent hardware
    private static String[] mValues;
    private static String[] mEntries;

    private static PackageManager pm;

    private static Context mContext;

    Preference mEnabledTiles;
    Preference mTileOrder;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.quick_settings_tiles);

        mContext = getActivity();

        mEnabledTiles = findPreference(PREF_ENABLED_TILES);

        mTileOrder = findPreference(PREF_TILES_ORDER);

        pm = mContext.getPackageManager();

        getAvailableTileList(getResources()
            .getStringArray(R.array.quick_settings_entries), getResources()
            .getStringArray(R.array.quick_settings_values));
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mEnabledTiles) {
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);

            ArrayList<String> enabledTiles = getTilesStringArray();

            boolean checkedTiles[] = new boolean[mValues.length];

            for (int i = 0; i < checkedTiles.length; i++) {
                if (enabledTiles.contains(mValues[i])) {
                    checkedTiles[i] = true;
                }
            }

            builder.setTitle(R.string.tiles_display_dialog);
            builder.setCancelable(true);
            builder.setPositiveButton(R.string.tiles_display_close,
                    new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });

            builder.setMultiChoiceItems(mEntries, checkedTiles, new OnMultiChoiceClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                    String tileKey = (mValues[which]);

                    if (isChecked) {
                        addTile(tileKey);
                    } else {
                        removeTile(tileKey);
                    }
                }
            });

            AlertDialog d = builder.create();

            d.show();

            return true;
        } else if (preference == mTileOrder) {
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            TileDragList fragment = new TileDragList();
            ft.addToBackStack(PREF_TILES_ORDER);
            ft.replace(this.getId(), fragment);
            ft.commit();
            return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return true;
    }

    public static void addTile(String key) {
        ArrayList<String> enabledTiles = getTilesStringArray();
        enabledTiles.add(key);
        setTilesFromStringArray(enabledTiles);
    }

    public static void removeTile(String key) {
        ArrayList<String> enabledTiles = getTilesStringArray();
        enabledTiles.remove(key);
        setTilesFromStringArray(enabledTiles);
    }

    public static class TileDragList extends ListFragment {

        private ListView mButtonList;
        private ButtonAdapter mButtonAdapter;
        private Context mTilesContext;

        /** Called when the activity is first created. */
        @Override
        public void onCreate(Bundle icicle) {
            super.onCreate(icicle);

            mTilesContext = getActivity().getBaseContext();
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            // Inflate the layout for this fragment
            View v = inflater.inflate(R.layout.order_tiles_activity, container,
                    false);

            return v;
        }

        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            mButtonList = this.getListView();
            ((TouchInterceptor) mButtonList).setDropListener(mDropListener);
            mButtonAdapter = new ButtonAdapter(mTilesContext);
            setListAdapter(mButtonAdapter);
        };

        @Override
        public void onDestroy() {
            ((TouchInterceptor) mButtonList).setDropListener(null);
            setListAdapter(null);
            super.onDestroy();
        }

        @Override
        public void onResume() {
            super.onResume();
            // reload our buttons and invalidate the views for redraw
            mButtonAdapter.reloadButtons();
            mButtonList.invalidateViews();
        }

        private TouchInterceptor.DropListener mDropListener = new TouchInterceptor.DropListener() {
            public void drop(int from, int to) {
                // get the current button list
                ArrayList<String> tiles = getTilesStringArray();

                // move the button
                if (from < tiles.size()) {
                    String tile = tiles.remove(from);

                    if (to <= tiles.size()) {
                        tiles.add(to, tile);

                        // save our buttons
                        setTilesFromStringArray(tiles);

                        // tell our adapter/listview to reload
                        mButtonAdapter.reloadButtons();
                        mButtonList.invalidateViews();
                    }
                }
            }
        };

        private class ButtonAdapter extends BaseAdapter {
            private Context mButtonContext;
            private LayoutInflater mInflater;
            private ArrayList<Tile> mTiles;

            public ButtonAdapter(Context c) {
                mButtonContext = c;
                mInflater = LayoutInflater.from(mButtonContext);
                reloadButtons();
            }

            public void reloadButtons() {
                mTiles = new ArrayList<Tile>();
                ArrayList<String> tileArray = getTilesStringArray();

                for(String tile : tileArray) {
                    if(!tile.equals("")) {
                        mTiles.add(new Tile(tile));
                    }
                }
            }

            public int getCount() {
                return mTiles.size();
            }

            public Object getItem(int position) {
                return mTiles.get(position);
            }

            public long getItemId(int position) {
                return position;
            }

            public View getView(int position, View convertView, ViewGroup parent) {
                final View v;
                if (convertView == null) {
                    v = mInflater.inflate(R.layout.order_tiles_list_item, null);
                } else {
                    v = convertView;
                }

                Tile tile = mTiles.get(position);
                final TextView name = (TextView) v.findViewById(R.id.name);

                for(int i = 0; i < mValues.length; i++) {
                    if(tile.getId().equals(mValues[i])) {
                        name.setText(mEntries[i]);
                        break;
                    }
                }

                return v;
            }
        }

    }

    public static class Tile {
        private String mId;

        public Tile(String id) {
            mId = id;
        }

        public String getId() {
            return mId;
        }
    }


    public static void getAvailableTileList(String[] entries, String[] values) {
        allEntries = new ArrayList<String>(Arrays.asList(entries));
        allValues = new ArrayList<String>(Arrays.asList(values));

        // Check if device has gyroscope
        if(!pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_GYROSCOPE)) {
            removeEntry(SCREEN_ROTATION);
        }

        // Check if device has bluetooth
        if (!pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
            removeEntry(BLUETOOTH);
        }

        // Check if device has network capabilities
        boolean hasMobileData = pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY);
        if (!hasMobileData) {
            removeEntry(DATA);
        }

        // Check if device has GPS
        if(!pm.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS)) {
            removeEntry(LOCATION);
        }

        // Check if device has Wi-Fi
        if(!pm.hasSystemFeature(PackageManager.FEATURE_WIFI)) {
            removeEntry(WIFI);
        }

        /*
         * Waiting for functionality to be merged
         *
        // Check if device has tethering capabilities
        ConnectivityManager cm =
                (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

        String[] mUsbRegexs = cm.getTetherableUsbRegexs();
        String[] mWifiRegexs = cm.getTetherableWifiRegexs();

        final boolean usbAvailable = mUsbRegexs.length != 0;
        final boolean wifiAvailable = mWifiRegexs.length != 0;

        if (!wifiAvailable) {
            removeEntry(WIFI_AP);
        }

        if (!usbAvailable) {
            removeEntry(USB_TETHER);
        }

        // Check if device has LTE
        if(Phone.LTE_ON_CDMA_TRUE != TelephonyManager.getDefault().getLteOnCdmaMode() ||
                TelephonyManager.getDefault().getLteOnGsmMode() == 0) {
            removeEntry(LTE);
        }*/

        // Check if device has NFC
        if(!pm.hasSystemFeature(PackageManager.FEATURE_NFC)) {
            removeEntry(NFC);
        }

        mEntries = allEntries.toArray(new String[allEntries.size()]);
        mValues = allValues.toArray(new String[allValues.size()]);
    }

    public static void removeEntry(String entry) {
        for(int i = 0; i < allValues.size(); i++) {
            if(allValues.get(i).equals(entry)) {
                allValues.remove(i);
                allEntries.remove(i);
            }
        }
    }

    public static void setTilesFromStringArray(ArrayList<String> newTilesArray) {
        String newTiles = TextUtils.join("|", newTilesArray);

        Settings.System.putString(mContext.getContentResolver(),
                Settings.System.QUICK_SETTINGS_ENTRIES, newTiles);
    }

    public static ArrayList<String> getTilesStringArray() {
        String cluster = Settings.System.getString(mContext.getContentResolver(),
                Settings.System.QUICK_SETTINGS_ENTRIES);

        if (cluster == null) {
            cluster = DEFAULT_TILES;
        }

        String[] tilesStringArray = cluster.split("\\|");

        return new ArrayList<String>(Arrays.asList(tilesStringArray));
    }
}
