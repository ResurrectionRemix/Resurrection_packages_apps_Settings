package com.dirtyunicorns.tweaks.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class LauncherActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent launch = new Intent(this, com.dirtyunicorns.tweaks.activities.DirtyTweaks.class);
        startActivity(launch);
        finish();
    }
}
