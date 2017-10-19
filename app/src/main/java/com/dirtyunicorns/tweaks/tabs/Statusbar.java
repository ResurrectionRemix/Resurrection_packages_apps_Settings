/*
 * Copyright (C) 2017 The Dirty Unicorns Project
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

package com.dirtyunicorns.tweaks.tabs;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.dirtyunicorns.tweaks.R;
import com.dirtyunicorns.tweaks.fragments.BatteryOptions;
import com.dirtyunicorns.tweaks.fragments.CarrierLabel;
import com.dirtyunicorns.tweaks.fragments.ClockOptions;
import com.dirtyunicorns.tweaks.fragments.Notifications;
import com.dirtyunicorns.tweaks.fragments.QuickSettings;
import com.dirtyunicorns.tweaks.fragments.StatusbarItems;
import com.dirtyunicorns.tweaks.fragments.Ticker;
import com.dirtyunicorns.tweaks.fragments.Traffic;

public class Statusbar extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.statusbar, null);

        ImageView battery_options = root.findViewById(R.id.battery_options_imageview);
        battery_options.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), BatteryOptions.class);
                startActivity(intent);
            }
        });

        ImageView carrier_label = root.findViewById(R.id.carrier_label_imageview);
        carrier_label.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), CarrierLabel.class);
                startActivity(intent);
            }
        });

        ImageView clock_options = root.findViewById(R.id.clock_options_imageview);
        clock_options.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), ClockOptions.class);
                startActivity(intent);
            }
        });

        ImageView notifications = root.findViewById(R.id.notifications_imageview);
        notifications.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), Notifications.class);
                startActivity(intent);
            }
        });

        ImageView quicksettings = root.findViewById(R.id.quicksettings_imageview);
        quicksettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), QuickSettings.class);
                startActivity(intent);
            }
        });

        ImageView statusbar_items = root.findViewById(R.id.statusbar_items_imageview);
        statusbar_items.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), StatusbarItems.class);
                startActivity(intent);
            }
        });

        ImageView ticker = root.findViewById(R.id.ticker_imageview);
        ticker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), Ticker.class);
                startActivity(intent);
            }
        });

        ImageView traffic = root.findViewById(R.id.traffic_imageview);
        traffic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), Traffic.class);
                startActivity(intent);
            }
        });

        return root;
    }
}
