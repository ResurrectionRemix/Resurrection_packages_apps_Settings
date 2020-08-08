package com.android.settings.rr.fragments.navbar;

import android.content.pm.ActivityInfo;
import android.provider.Settings;
import android.view.View;
import android.widget.ListView;

import com.android.internal.util.rr.AppPicker;

public class BackLongPress extends AppPicker {

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        if (!mIsActivitiesList) {
            // we are in the Apps list
            String packageName = applist.get(position).packageName;
            String friendlyAppString = (String) applist.get(position).loadLabel(packageManager);
            setPackage(packageName, friendlyAppString);
            setPackageActivity(null);
        } else if (mIsActivitiesList) {
            // we are in the Activities list
            setPackageActivity(mActivitiesList.get(position));
        }

        mIsActivitiesList = false;
        finish();
    }

    @Override
    protected void onLongClick(int position) {
        if (mIsActivitiesList) return;
        String packageName = applist.get(position).packageName;
        String friendlyAppString = (String) applist.get(position).loadLabel(packageManager);
        // always set CUSTOM_APP so we can fallback if something goes wrong with
        // packageManager.getPackageInfo
        setPackage(packageName, friendlyAppString);
        setPackageActivity(null);
        showActivitiesDialog(packageName);
    }

    protected void setPackage(String packageName, String friendlyAppString) {
        Settings.System.putString(
                getContentResolver(), Settings.System.KEY_BACK_LONG_PRESS_CUSTOM_APP, packageName);
        Settings.System.putString(
                getContentResolver(), Settings.System.KEY_BACK_LONG_PRESS_CUSTOM_APP_FR_NAME,
                friendlyAppString);
    }

    protected void setPackageActivity(ActivityInfo ai) {
        Settings.System.putString(
                getContentResolver(), Settings.System.KEY_BACK_LONG_PRESS_CUSTOM_ACTIVITY,
                ai != null ? ai.name : "NONE");
    }
}
