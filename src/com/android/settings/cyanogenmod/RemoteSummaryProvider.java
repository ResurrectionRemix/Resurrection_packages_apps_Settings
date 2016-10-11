package com.android.settings.cyanogenmod;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import android.util.Log;

import com.android.settings.dashboard.SummaryLoader;
import com.android.settingslib.drawer.Tile;

public class RemoteSummaryProvider implements SummaryLoader.SummaryProvider {

    private final Activity mActivity;
    private final SummaryLoader mLoader;
    private final ComponentName mReceiverComponent;
    private final String mKey;

    /* Manifest meta-data key with the receiver class name */
    private static final String META_SUMMARY_RECEIVER =
            "org.cyanogenmod.settings.summary.receiver";

    /* Key which should be sent in the broadcast to identify the component */
    private static final String META_SUMMARY_KEY =
            "org.cyanogenmod.settings.summary.key";

    /* Receiver will be sent an ordered broadcast, and should fill the result extras */
    private static final String ACTION_REFRESH_SUMMARY =
            "org.cyanogenmod.settings.REFRESH_SUMMARY";

    /* Key sent in the broadcast */
    private static final String EXTRA_KEY = "key";

    /* Summary returned in the result */
    private static final String EXTRA_SUMMARY = "summary";

    public static SummaryLoader.SummaryProvider createSummaryProvider(
            Activity activity, SummaryLoader summaryLoader, Tile tile) {

        Bundle meta = tile.metaData;
        if (meta == null) {
            return null;
        }

        String receiverClass = meta.getString(META_SUMMARY_RECEIVER);
        if (receiverClass == null) {
            return null;
        }

        return new RemoteSummaryProvider(activity, summaryLoader, new ComponentName(
                tile.intent.getComponent().getPackageName(), receiverClass),
                meta.getString(META_SUMMARY_KEY));
    }

    private RemoteSummaryProvider(Activity activity, SummaryLoader loader,
                                  ComponentName receiverComponent, String key) {
        mActivity = activity;
        mLoader = loader;
        mReceiverComponent = receiverComponent;
        mKey = key;
    }

    @Override
    public void setListening(boolean listening) {
        if (listening) {
            Intent i = new Intent(ACTION_REFRESH_SUMMARY);
            i.setComponent(mReceiverComponent);
            i.putExtra(EXTRA_KEY, mKey);

            mActivity.sendOrderedBroadcastAsUser(i, UserHandle.CURRENT,
                    Manifest.permission.WRITE_SETTINGS, new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent intent) {
                            final Bundle result = getResultExtras(true);
                            final String summary = result.getString(EXTRA_SUMMARY);
                            if (summary != null) {
                                mLoader.setSummary(RemoteSummaryProvider.this, summary);
                            }
                        }
                    }, null, Activity.RESULT_OK, null, null);
        }
    }
}
