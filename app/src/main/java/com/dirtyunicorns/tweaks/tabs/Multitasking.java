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
import com.dirtyunicorns.tweaks.fragments.Headsup;
import com.dirtyunicorns.tweaks.fragments.Recents;
import com.dirtyunicorns.tweaks.helpers.Utils;

public class Multitasking extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final ViewGroup root = (ViewGroup) inflater.inflate(R.layout.multitasking, null);

        final boolean headsupAvailable  = getResources().getBoolean(R.bool.headsupAvailable);
        final boolean recentsAvailable  = getResources().getBoolean(R.bool.recentsAvailable);

        ImageView headsup = root.findViewById(R.id.headsup_imageview);
        headsup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (headsupAvailable) {
                    Intent intent = new Intent(getActivity(), Headsup.class);
                    startActivity(intent);
                } else {
                    snackBar();
                }
            }
        });

        LinearLayout layout = root.findViewById(R.id.omniswitch_layout);
        ImageView omniswitch = root.findViewById(R.id.omniswitch_imageview);

        boolean OmniSwitch = Utils.isPackageInstalled("org.omnirom.omniswitch", getContext().getPackageManager());

        if (!OmniSwitch) layout.setVisibility(View.GONE);

        omniswitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.setComponent(new ComponentName("org.omnirom.omniswitch", "org.omnirom.omniswitch.SettingsActivity"));
                startActivity(intent);
            }
        });

        ImageView recents = root.findViewById(R.id.recents_imageview);
        recents.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (recentsAvailable) {
                    Intent intent = new Intent(getActivity(), Recents.class);
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
