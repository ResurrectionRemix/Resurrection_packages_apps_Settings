/*
   Copyright (c) 2014, The Linux Foundation. All Rights Reserved.
Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
      copyright notice, this list of conditions and the following
      disclaimer in the documentation and/or other materials provided
      with the distribution.
    * Neither the name of The Linux Foundation nor the names of its
      contributors may be used to endorse or promote products derived
      from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.android.settings;

import java.io.FileNotFoundException;
import java.io.InputStream;

import android.app.ActionBar;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RadioButton;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.PopupWindow;

import com.android.display.IPPService;

public class ScreenColorSettings extends Activity {
    private final static String TAG = "ScreenColorSettings";
    private final static boolean DEBUG = false;

    private final static int MODE_STANDARD = 0;
    private final static int MODE_VIVID    = 1;
    private final static int MODE_CUSTOM   = 2;

    private static final String COLOR_MODE       = "mode";
    private static final String COLOR_HUE        = "hue";
    private static final String COLOR_SATURATION = "saturation";
    private static final String COLOR_INTENSITY  = "intensity";
    private static final String COLOR_CONTRAST   = "contrast";

    private static final String PREVIEW_STRING_NAME = "screencolor_preview_name";

    private int mMode = 0;
    private IPPService mPPService = null;
    private PPServiceConnection mPPServiceConn = null;
    private SharedPreferences mSharedPreference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initPPService();
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        setContentView(R.layout.screencolor_settings);

        ListView listView = (ListView)findViewById(R.id.mode_list);
        listView.setBackgroundColor(R.color.screencolor_background);
        mSharedPreference = getSharedPreferences(PREVIEW_STRING_NAME, Context.MODE_PRIVATE);
        mMode = mSharedPreference.getInt(COLOR_MODE, 0);
        setScreenColorMode(mMode);
        String[] list = getResources().getStringArray(R.array.screen_color_setting);
        ColorModeAdaper adapter = new ColorModeAdaper(this,
                R.layout.screen_color_item, list);
        listView.setAdapter(adapter);
    }

    private class ColorModeAdaper extends ArrayAdapter<String> {
        private int mResource;
        private RelativeLayout mItemView;

        public ColorModeAdaper(Context context, int resource, String[] list) {
            super(context, resource, list);
            this.mResource = resource;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            String name = getItem(position);
            if(convertView == null) {
                mItemView = new RelativeLayout(getContext());
                LayoutInflater vi = (LayoutInflater)getContext()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                vi.inflate(mResource, mItemView, true);
            } else {
                mItemView = (RelativeLayout) convertView;
            }
            ((TextView) mItemView.findViewById(android.R.id.title)).setText(name);

            RadioButton radioButton = (RadioButton) mItemView.findViewById(android.R.id.button1);
            ImageView setting = (ImageView) mItemView.findViewById(android.R.id.button2);
            if (mMode == position) {
                radioButton.setChecked(true);
                setting.setEnabled(true);
            } else {
                radioButton.setChecked(false);
                setting.setEnabled(false);
            }

            if (position != getCount()-1) {
                ((ImageView) mItemView.findViewById(R.id.divider)).setVisibility(View.INVISIBLE);
                setting.setVisibility(View.INVISIBLE);
            } else {
                setting.setOnClickListener(new OnClickListener () {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent();
                        intent.setClass(ScreenColorSettings.this, CustomScreenColor.class);
                        startActivity(intent);
                    }});
            }

            View v = mItemView.findViewById(R.id.widget_frame);
            v.setTag(position);
            v.setOnClickListener(new OnClickListener () {
                @Override
                public void onClick(View v) {
                    int mode = (Integer)(v.getTag());
                    setScreenColorMode(mode);
                    notifyDataSetChanged();
                }});
            return mItemView;
        }
    }

    private void initPPService() {
        mPPServiceConn = new PPServiceConnection();
        String packetname = "com.qualcomm.display";
        String service = "com.qualcomm.display.PPService";
        Intent i = new Intent(IPPService.class.getName());

        ComponentName component = new ComponentName(packetname, service);
        i.setComponent(component);

        bindService(i, mPPServiceConn, Context.BIND_AUTO_CREATE);
    }

    private class PPServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mPPService = IPPService.Stub.asInterface((IBinder) service);
            if (DEBUG) Log.d(TAG, "onServiceConnected, service=" + mPPService);
            try {
                if (null != mPPService) {
                    mPPService.startPP();
                    setScreenColorMode(mMode);
                }
            } catch (RemoteException e) {
                Log.e(TAG, "startPP exception");
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            if (null != mPPService) {
                try {
                    mPPService.stopPP();
                } catch (RemoteException e) {
                    Log.e(TAG, "stopPP exception");
                }
            }
            mPPService = null;
        }

    }

    private void setScreenColorMode(int mode) {
        if (DEBUG) Log.i(TAG, "setScreenColorMode mode = " + mode);
        int hue = 0;
        int saturation = 0;
        int intensity = 0;
        int contrast = 0;
        switch (mode) {
            case MODE_STANDARD:
                hue = 180;
                saturation = 180;
                intensity = 255;
                contrast = 180;
                break;
            case MODE_VIVID:
                hue = 180;
                saturation = 210;
                intensity = 255;
                contrast = 180;
                break;
            case MODE_CUSTOM:
                hue = mSharedPreference.getInt(COLOR_HUE, 180);
                saturation = mSharedPreference.getInt(COLOR_SATURATION, 180);
                intensity = mSharedPreference.getInt(COLOR_INTENSITY, 255);
                contrast = mSharedPreference.getInt(COLOR_CONTRAST, 180);
                break;
            default:
                break;
        }
        saveModeValue(mode);
        updateHSCIValue(hue, saturation, intensity, contrast);
    }

    private void updateHSCIValue(int hue, int saturation, int intensity, int contrast) {
        try {
            if (null != mPPService && mPPService.getPPStatus()) {
                mPPService.updateHSIC(hue, saturation, intensity, contrast);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "updateHSIC exception");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mPPServiceConn);
    }

    private void saveModeValue(int mode) {
        mMode = mode;
        Editor editor = mSharedPreference.edit();
        editor.putInt(COLOR_MODE, mode);
        editor.commit();
    }
}
