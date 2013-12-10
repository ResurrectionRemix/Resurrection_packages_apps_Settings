/*
 * Copyright (C) 2012 The CyanogenMod Project
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

package com.android.settings.cyanogenmod;

<<<<<<< HEAD
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import com.android.settings.R;

=======
import com.android.settings.R;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Config;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
>>>>>>> 67871288ef10dafa45797239039ec3026e4c4020
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

<<<<<<< HEAD
public class ChangeLog extends Fragment {
    private static final String CHANGELOG_PATH = "/system/etc/CHANGELOG-CM.txt";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
        InputStreamReader inputReader = null;
        String text = null;

        try {
            StringBuilder data = new StringBuilder();
            char tmp[] = new char[2048];
            int numRead;

=======
import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;

public class ChangeLog extends AlertActivity {

    private static final String CHANGELOG_PATH = "/system/etc/CHANGELOG-CM.txt";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        InputStreamReader inputReader = null;
        StringBuilder data = null;
        try {
            data = new StringBuilder(2048);
            char tmp[] = new char[2048];
            int numRead;
>>>>>>> 67871288ef10dafa45797239039ec3026e4c4020
            inputReader = new FileReader(CHANGELOG_PATH);
            while ((numRead = inputReader.read(tmp)) >= 0) {
                data.append(tmp, 0, numRead);
            }
<<<<<<< HEAD
            text = data.toString();
        } catch (IOException e) {
            text = getString(R.string.changelog_error);
=======
        } catch (IOException e) {
            showErrorAndFinish();
            return;
>>>>>>> 67871288ef10dafa45797239039ec3026e4c4020
        } finally {
            try {
                if (inputReader != null) {
                    inputReader.close();
                }
            } catch (IOException e) {
            }
        }

<<<<<<< HEAD
        final TextView textView = new TextView(getActivity());
        textView.setText(text);

        final ScrollView scrollView = new ScrollView(getActivity());
        scrollView.addView(textView);

        return scrollView;
    }
=======
        if (TextUtils.isEmpty(data)) {
            showErrorAndFinish();
            return;
        }

        WebView webView = new WebView(this);

        // Begin the loading.  This will be done in a separate thread in WebView.
        webView.loadDataWithBaseURL(null, data.toString(), "text/plain", "utf-8", null);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                // Change from 'Loading...' to the real title
                mAlert.setTitle(getString(R.string.changelog_dialog));
            }
        });

        final AlertController.AlertParams p = mAlertParams;
        p.mTitle = getString(R.string.changelog_loading);
        p.mView = webView;
        p.mForceInverseBackground = true;
        setupAlert();
    }

    private void showErrorAndFinish() {
        Toast.makeText(this, R.string.changelog_error, Toast.LENGTH_LONG)
                .show();
        finish();
    }

>>>>>>> 67871288ef10dafa45797239039ec3026e4c4020
}
