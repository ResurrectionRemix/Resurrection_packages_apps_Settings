/*
 *     Copyright (C) 2016 The CyanogenMod Project
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */

package com.android.settings;

import android.app.Activity;
import android.os.Bundle;
import android.os.SystemProperties;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class CmRadioInfo extends Activity {
    private final String TAG = "CmRadioInfo";

    private Button mbnAutoLoadButton;
    private Button volteAvailOvrButton;
    private Button vtAvailOvrButton;
    private Button wfcAvailOvrButton;
    private Button adbRadioLog;
    private Button diagLog;

    static final String PROPERTY_SW_MBN_UPDATE = "persist.radio.sw_mbn_update";
    static final String PROPERTY_SW_MBN_VOLTE = "persist.radio.sw_mbn_volte";
    static final String PROPERTY_VOLTE_AVAIL_OVR = "persist.dbg.volte_avail_ovr";
    static final String PROPERTY_VT_AVAIL_OVR = "persist.dbg.vt_avail_ovr";
    static final String PROPERTY_DATA_IWLAN_ENABLE = "persist.data.iwlan.enable";
    static final String PROPERTY_WFC_AVAIL_OVR = "persist.dbg.wfc_avail_ovr";
    static final String PROPERTY_ADB_LOG_ON = "persist.radio.adb_log_on";
    static final String PROPERTY_DIAG_LOG_ON = "persist.radio.diag_log_on";

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setContentView(R.layout.cm_radio_info);

        mbnAutoLoadButton = (Button) findViewById(R.id.mbn_auto_load);
        mbnAutoLoadButton.setOnClickListener(mMbnAutoLoadHandler);

        volteAvailOvrButton = (Button) findViewById(R.id.volte_avail_ovr);
        volteAvailOvrButton.setOnClickListener(mVolteAvailOvrHandler);

        vtAvailOvrButton = (Button) findViewById(R.id.vt_avail_ovr);
        vtAvailOvrButton.setOnClickListener(mVtAvailOvrHandler);

        wfcAvailOvrButton = (Button) findViewById(R.id.wfc_avail_ovr);
        wfcAvailOvrButton.setOnClickListener(mWfcAvailOvrHandler);

        adbRadioLog = (Button) findViewById(R.id.adb_radio_log);
        adbRadioLog.setOnClickListener(mAdbRadioLogHandler);

        diagLog = (Button) findViewById(R.id.diag_log);
        diagLog.setOnClickListener(mDiagLogHandler);
    }

    @Override
    protected void onResume() {
        super.onResume();

        updateMbnAutoLoadState();
        updateVolteAvailOvrState();
        updateVtAvailOvrState();
        updateWfcAvailOvrState();
        updateAdbRadioLogState();
        updateDiagLogState();

        log("onResume: update cm radio info");
    }

    @Override
    public void onPause() {
        super.onPause();

        log("onPause: cm radio info");
    }

    OnClickListener mMbnAutoLoadHandler = new OnClickListener() {
        @Override
        public void onClick(View v) {
            SystemProperties.set(PROPERTY_SW_MBN_UPDATE, (isMbnAutoLoad() ? "0" : "1"));
            updateMbnAutoLoadState();
        }
    };

    private boolean isMbnAutoLoad() {
        return SystemProperties.getBoolean(PROPERTY_SW_MBN_UPDATE, false);
    }

    private void updateMbnAutoLoadState() {
        log("updateMbnAutoLoadState isMbnAutoLoad()=" + isMbnAutoLoad());
        String buttonText = isMbnAutoLoad() ?
                            getString(R.string.cm_radio_info_mbn_auto_load_on_label) :
                            getString(R.string.cm_radio_info_mbn_auto_load_off_label);
        mbnAutoLoadButton.setText(buttonText);
    }

    OnClickListener mVolteAvailOvrHandler = new OnClickListener() {
        @Override
        public void onClick(View v) {
            SystemProperties.set(PROPERTY_SW_MBN_VOLTE, (isVolteAvailOvr() ? "0" : "1"));
            SystemProperties.set(PROPERTY_VOLTE_AVAIL_OVR, (isVolteAvailOvr() ? "0" : "1"));
            updateVolteAvailOvrState();
        }
    };

    private boolean isVolteAvailOvr() {
        return SystemProperties.getBoolean(PROPERTY_VOLTE_AVAIL_OVR, false);
    }

    private void updateVolteAvailOvrState() {
        log("updateVolteAvailOvrState isVolteAvailOvr()=" + isVolteAvailOvr());
        String buttonText = isVolteAvailOvr() ?
                            getString(R.string.cm_radio_info_volte_avail_ovr_on_label) :
                            getString(R.string.cm_radio_info_volte_avail_ovr_off_label);
        volteAvailOvrButton.setText(buttonText);
    }

    OnClickListener mVtAvailOvrHandler = new OnClickListener() {
        @Override
        public void onClick(View v) {
            SystemProperties.set(PROPERTY_VT_AVAIL_OVR, (isVtAvailOvr() ? "0" : "1"));
            updateVtAvailOvrState();
        }
    };

    private boolean isVtAvailOvr() {
        return SystemProperties.getBoolean(PROPERTY_VT_AVAIL_OVR, false);
    }

    private void updateVtAvailOvrState() {
        log("updateVtAvailOvrState isVtAvailOvr()=" + isVtAvailOvr());
        String buttonText = isVtAvailOvr() ?
                            getString(R.string.cm_radio_info_vt_avail_ovr_on_label) :
                            getString(R.string.cm_radio_info_vt_avail_ovr_off_label);
        vtAvailOvrButton.setText(buttonText);
    }

    OnClickListener mWfcAvailOvrHandler = new OnClickListener() {
        @Override
        public void onClick(View v) {
            SystemProperties.set(PROPERTY_DATA_IWLAN_ENABLE, (isWfcAvailOvr() ? "false" : "true"));
            SystemProperties.set(PROPERTY_WFC_AVAIL_OVR, (isWfcAvailOvr() ? "0" : "1"));
            updateWfcAvailOvrState();
        }
    };

    private boolean isWfcAvailOvr() {
        return SystemProperties.getBoolean(PROPERTY_WFC_AVAIL_OVR, false);
    }

    private void updateWfcAvailOvrState() {
        log("updateWfcAvailOvrState isWfcAvailOvr()=" + isWfcAvailOvr());
        String buttonText = isWfcAvailOvr() ?
                            getString(R.string.cm_radio_info_wfc_avail_ovr_on_label) :
                            getString(R.string.cm_radio_info_wfc_avail_ovr_off_label);
        wfcAvailOvrButton.setText(buttonText);
    }

    OnClickListener mAdbRadioLogHandler = new OnClickListener() {
        @Override
        public void onClick(View v) {
            SystemProperties.set(PROPERTY_ADB_LOG_ON, (isAdbRadioLog() ? "0" : "1"));
            updateAdbRadioLogState();
        }
    };

    private boolean isAdbRadioLog() {
        return SystemProperties.getBoolean(PROPERTY_ADB_LOG_ON, false);
    }

    private void updateAdbRadioLogState() {
        log("updateAdbRadioLogState isAdbRadioLog()=" + isAdbRadioLog());
        String buttonText = isAdbRadioLog() ?
                getString(R.string.cm_radio_info_adb_log_on_label) :
                getString(R.string.cm_radio_info_adb_log_off_label);
        adbRadioLog.setText(buttonText);
    }

    OnClickListener mDiagLogHandler = new OnClickListener() {
        @Override
        public void onClick(View v) {
            SystemProperties.set(PROPERTY_DIAG_LOG_ON, (isDiagLog() ? "0" : "1"));
            updateDiagLogState();
        }
    };

    private boolean isDiagLog() {
        return SystemProperties.getBoolean(PROPERTY_DIAG_LOG_ON, false);
    }

    private void updateDiagLogState() {
        log("updateDiagLogState isDiagLog()=" + isDiagLog());
        String buttonText = isDiagLog() ?
                getString(R.string.cm_radio_info_diag_log_on_label) :
                getString(R.string.cm_radio_info_diag_log_off_label);
        diagLog.setText(buttonText);
    }

    private void log(String s) {
        Log.d(TAG, "[Phone] " + s);
    }
}
