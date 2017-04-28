package com.android.settings.rr;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.MenuItem;

public class SubActivity extends Activity {

    public static final String EXTRA_TITLE =
            "com.android.settings.rr.SubActivity.title";

    public static final String EXTRA_FRAGMENT_CLASS =
            "com.android.settings.rr.SubActivity.fragment_class";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        String fragmentExtra = getIntent().getStringExtra(EXTRA_FRAGMENT_CLASS);
        if (fragmentExtra != null  && !fragmentExtra.isEmpty()) {
            try {
                Class<?> fragmentClass = Class.forName(fragmentExtra);
                Fragment fragment = (Fragment) fragmentClass.newInstance();
                getFragmentManager().beginTransaction()
                        .replace(android.R.id.content, fragment).commit();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        String title = getIntent().getStringExtra(EXTRA_TITLE);
        if (title != null  && !title.isEmpty()) {
            setTitle(title);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
