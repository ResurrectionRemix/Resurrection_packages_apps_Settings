package com.android.settings.rr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.android.settings.rr.HAFRAppChooserAdapter.AppItem;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import com.android.settings.R;

public class HAFRAppListActivity extends Activity {

    /* others */
    static final int ID_ADD_APP = 1;

    /* main stuff */
    SharedPreferences mPref;
    HAFRAppListAdapter mPkgAdapter;
    ListView mListView;

    /* app dialog stuff */
    HAFRAppChooserDialog dDialog;
    ArrayList<String> excludeFromRecentsList;
    public static final String KEY_PREFERENCE_APPS = "hide_recents_apps_pref";

    @Override
    @SuppressLint("WorldReadableFiles")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPref = getSharedPreferences(KEY_PREFERENCE_APPS, MODE_PRIVATE);
        loadList();
        initAppList();
    }

    @Override
    public void onPause() {
        super.onPause();
        // Avoid WindowLeaked Exception
        // http://publicstaticdroidmain.com/2012/01/avoiding-android-memory-leaks-part-1/
        if (dDialog != null && dDialog.isShowing()) {
            dDialog.dismiss();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem add = menu.add(Menu.NONE, ID_ADD_APP, 0, R.string.hide_from_recents_add_app);
        add.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case ID_ADD_APP:
                dDialog.show(ID_ADD_APP);
                break;
        }
        return false;
    }

    private void initAppList() {
        dDialog = new HAFRAppChooserDialog(this) {
            @Override
            public void onListViewItemClick(AppItem info, int id) {
                addApp(info.packageName);
            }
        };
    }

    private void loadList() {
        final Map<String, Integer> list = getSetStrings();
        mPkgAdapter = new HAFRAppListAdapter(this, list) {
            @Override
            public void onRemoveButtonPress(PackageItem app_info) {
                removeApp(app_info.packageName);
            }
        };
        mListView = new ListView(this);
        mListView.setAdapter(mPkgAdapter);
        setContentView(mListView);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Integer> getSetStrings() {
        return (Map<String, Integer>) mPref.getAll();
    }

    public void removeApp(String pkg) {

        Editor editor = mPref.edit().clear();
        excludeFromRecentsList = new ArrayList<String>();
        List<ArrayList<String>> tempItemList2 = new ArrayList<ArrayList<String>>();
        List<String> tempItemList3 = new ArrayList<String>();

        for (HAFRAppListAdapter.PackageItem item : mPkgAdapter.getList()) {
            if (!item.packageName.equals(pkg)) {
                excludeFromRecentsList.add(item.packageName);
            }
        }

        PackageManager packageManager = getApplicationContext().getPackageManager();

        for (int i = 0; i < excludeFromRecentsList.size(); i++) {
            try {
                tempItemList2.add(new ArrayList<String>(Arrays.asList((String) packageManager.getApplicationLabel(packageManager.getApplicationInfo(excludeFromRecentsList.get(i), PackageManager.GET_META_DATA)), excludeFromRecentsList.get(i))));
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }
        Collections.sort(tempItemList2, new Comparator<ArrayList<String>>() {
            @Override
            public int compare(ArrayList<String> o1, ArrayList<String> o2) {
                return o1.get(0).toLowerCase().compareTo(o2.get(0).toLowerCase());
            }
        });

        for (int i = 0; i < excludeFromRecentsList.size(); i++) {
            tempItemList3.add(tempItemList2.get(i).get(1));
        }


        for (HAFRAppListAdapter.PackageItem item : mPkgAdapter.getList()) {
            editor.putInt(item.packageName, tempItemList3.indexOf(item.packageName));
        }
        editor.commit();
        saveExcludeFromRecentsString();
        updateList();
    }

    public void addApp(String pkg) {
        Editor editor = mPref.edit().clear();
        excludeFromRecentsList = new ArrayList<String>();
        List<ArrayList<String>> tempItemList2 = new ArrayList<ArrayList<String>>();
        List<String> tempItemList3 = new ArrayList<String>();

        for (HAFRAppListAdapter.PackageItem item : mPkgAdapter.getList()) {
            excludeFromRecentsList.add(item.packageName);
        }

        excludeFromRecentsList.add(pkg);
        PackageManager packageManager = getApplicationContext().getPackageManager();

        for (int i = 0; i < excludeFromRecentsList.size(); i++) {
            try {
                tempItemList2.add(new ArrayList<String>(Arrays.asList((String) packageManager.getApplicationLabel(packageManager.getApplicationInfo(excludeFromRecentsList.get(i), PackageManager.GET_META_DATA)), excludeFromRecentsList.get(i))));
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }
        Collections.sort(tempItemList2, new Comparator<ArrayList<String>>() {
            @Override
            public int compare(ArrayList<String> o1, ArrayList<String> o2) {
                return o1.get(0).toLowerCase().compareTo(o2.get(0).toLowerCase());
            }
        });

        for (int i = 0; i < excludeFromRecentsList.size(); i++) {
            tempItemList3.add(tempItemList2.get(i).get(1));
        }


        for (HAFRAppListAdapter.PackageItem item : mPkgAdapter.getList()) {
            editor.putInt(item.packageName, tempItemList3.indexOf(item.packageName));
        }

        editor.putInt(pkg, tempItemList3.indexOf(pkg));
        editor.commit();
        saveExcludeFromRecentsString();
        updateList();
    }

    private void saveExcludeFromRecentsString() {
        StringBuilder sb = new StringBuilder();
        for (String name : excludeFromRecentsList) {
            sb.append(name + "|");
        }

        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }

        Log.d("Myself5", sb.toString());
        Settings.System.putString(getContentResolver(),
        		Settings.System.HIDE_FROM_RECENTS_LIST, sb.toString());
    }

    public void updateList() {
        mPkgAdapter.update(getSetStrings());
    }
}
