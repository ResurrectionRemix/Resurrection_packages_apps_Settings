package com.android.settings.cyanogenmod;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;

import com.android.settings.dashboard.SummaryLoader;
import com.android.settingslib.drawer.Tile;

import cyanogenmod.preference.RemotePreferenceManager;

import static cyanogenmod.preference.RemotePreference.ACTION_UPDATE_PREFERENCE;
import static cyanogenmod.preference.RemotePreference.EXTRA_KEY;
import static cyanogenmod.preference.RemotePreference.EXTRA_SUMMARY;
import static cyanogenmod.preference.RemotePreference.META_REMOTE_KEY;
import static cyanogenmod.preference.RemotePreference.META_REMOTE_RECEIVER;

public class RemoteSummaryProvider implements SummaryLoader.SummaryProvider,
        RemotePreferenceManager.OnRemoteUpdateListener {

    private final Activity mActivity;
    private final SummaryLoader mLoader;
    private final Tile mTile;
    private final String mKey;

    public static SummaryLoader.SummaryProvider createSummaryProvider(
            Activity activity, SummaryLoader summaryLoader, Tile tile) {
        if (tile.metaData != null && tile.metaData.containsKey(META_REMOTE_RECEIVER)) {
            return new RemoteSummaryProvider(activity, summaryLoader, tile);
        }
        return null;
    }

    private RemoteSummaryProvider(Activity activity, SummaryLoader loader, Tile tile) {
        mActivity = activity;
        mLoader = loader;
        mTile = tile;
        mKey = mTile.metaData.getString(META_REMOTE_KEY);
    }

    @Override
    public void setListening(boolean listening) {
        if (listening) {
            RemotePreferenceManager.get(mActivity).attach(mKey, this);
        } else {
            RemotePreferenceManager.get(mActivity).detach(mKey, this);
        }
    }

    @Override
    public Intent getReceiverIntent() {
        Intent i = new Intent(ACTION_UPDATE_PREFERENCE);
        i.setComponent(new ComponentName(mTile.intent.getComponent().getPackageName(),
                mTile.metaData.getString(META_REMOTE_RECEIVER)));
        i.putExtra(EXTRA_KEY, mKey);
        return i;
    }

    @Override
    public void onRemoteUpdated(Bundle bundle) {
        mLoader.setSummary(this, bundle.getString(EXTRA_SUMMARY));
    }
}
