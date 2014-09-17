package com.android.settings.cyanogenmod;

import android.bluetooth.BluetoothAdapter;
import android.content.ContentResolver;
import android.content.Context;
import android.net.ConnectivityManager;
import android.nfc.NfcAdapter;
import android.provider.Settings;

/**
 * Created by roman on 11/24/14.
 */
public class DeviceUtils {
    public static boolean deviceSupportsMobileData(Context ctx) {
        ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.isNetworkSupported(ConnectivityManager.TYPE_MOBILE);
    }

    public static boolean deviceSupportsBluetooth() {
        return (BluetoothAdapter.getDefaultAdapter() != null);
    }

    public static boolean deviceSupportsNfc(Context ctx) {
        return NfcAdapter.getDefaultAdapter(ctx) != null;
    }
}
