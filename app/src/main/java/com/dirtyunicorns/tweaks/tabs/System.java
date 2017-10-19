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

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.dirtyunicorns.tweaks.R;
import com.dirtyunicorns.tweaks.fragments.Miscellaneous;
import com.dirtyunicorns.tweaks.fragments.PowerMenu;
import com.dirtyunicorns.tweaks.helpers.Utils;

public class System extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.system, null);

        final boolean miscellaneousAvailable  = getResources().getBoolean(R.bool.miscellaneousAvailable);
        final boolean powermenuAvailable  = getResources().getBoolean(R.bool.powermenuAvailable);


        LinearLayout layout = root.findViewById(R.id.device_extras_layout);
        ImageView device_extras = root.findViewById(R.id.device_extras_imageview);

        boolean deviceExtras = Utils.isPackageInstalled("com.dirtyunicorns.settings.device", getContext().getPackageManager());

        if (!deviceExtras) layout.setVisibility(View.GONE);

        if (deviceExtras) device_extras.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.setComponent(new ComponentName("com.dirtyunicorns.settings.device", "com.dirtyunicorns.settings.device.TouchscreenGestureSettings"));
                startActivity(intent);
            }
        });

        ImageView miscellaneous = root.findViewById(R.id.miscellaneous_imageview);
        miscellaneous.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (miscellaneousAvailable) {
                    Intent intent = new Intent(getActivity(), Miscellaneous.class);
                    startActivity(intent);
                } else {
                    snackBar();
                }
            }
        });

        ImageView powermenu = root.findViewById(R.id.powermenu_imageview);
        powermenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (powermenuAvailable) {
                    Intent intent = new Intent(getActivity(), PowerMenu.class);
                    startActivity(intent);
                } else {
                    snackBar();
                }
            }
        });

        return root;
    }

    public void snackBar() {
        Snackbar snackbar = Snackbar.make(getActivity().findViewById(R.id.viewSnack), getString(R.string.features_not_available), Snackbar.LENGTH_LONG);
        View sbView = snackbar.getView();
        sbView.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.NavigationBarColor));
        snackbar.show();
    }
}
