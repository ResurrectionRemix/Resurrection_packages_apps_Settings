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
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.dirtyunicorns.tweaks.R;
import com.dirtyunicorns.tweaks.fragments.Buttons;
import com.dirtyunicorns.tweaks.fragments.NavigationBar;

public class Navigation extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.navigation, null);

        final boolean buttonsAvailable  = getResources().getBoolean(R.bool.buttonsAvailable);
        final boolean duiAvailable  = getResources().getBoolean(R.bool.duiAvailable);

        ImageView buttons = root.findViewById(R.id.buttons_imageview);
        buttons.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (buttonsAvailable) {
                    Intent intent = new Intent(getActivity(), Buttons.class);
                    startActivity(intent);
                } else {
                   snackBar();
                }
            }
        });

        ImageView navigationbar = root.findViewById(R.id.navigationbar_imageview);
        navigationbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (duiAvailable) {
                    Intent intent = new Intent(getActivity(), NavigationBar.class);
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
