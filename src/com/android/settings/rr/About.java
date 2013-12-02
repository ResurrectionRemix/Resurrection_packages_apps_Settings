package com.android.settings.rr;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
    
public class About extends SettingsPreferenceFragment {

public static final String TAG = "About";
    
private static final String RR_ROM_SHARE = "share";

    Preference mSiteUrl;
    Preference mForumUrl;
    Preference mSourceUrl;
    Preference mFacebookUrl;
    Preference mGoogleUrl;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.about_rom);
        mSiteUrl = findPreference("rr_website");
        mForumUrl = findPreference("rr_irc");
        mSourceUrl = findPreference("rr_source");
        mFacebookUrl = findPreference("rr_facebook");
        mGoogleUrl = findPreference("rr_google_plus");
        
        PreferenceGroup devsGroup = (PreferenceGroup) findPreference("devs");
        ArrayList<Preference> devs = new ArrayList<Preference>();
        for (int i = 0; i < devsGroup.getPreferenceCount(); i++) {
            devs.add(devsGroup.getPreference(i));
        }
        devsGroup.removeAll();
        devsGroup.setOrderingAsAdded(false);
        Collections.shuffle(devs);
        for(int i = 0; i < devs.size(); i++) {
            Preference p = devs.get(i);
            p.setOrder(i);

            devsGroup.addPreference(p);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mSiteUrl) {
            launchUrl("http://www.resurrectionremix.com/index.php");
        } else if (preference == mForumUrl) {
            launchUrl("http://www.resurrectionremix.com/index.php");
        } else if (preference == mSourceUrl) {
            launchUrl("https://github.com/ResurrectionRemix");
        } else if (preference == mFacebookUrl) {
            launchUrl("https://www.facebook.com/resurrectionremixrom");
        } else if (preference == mGoogleUrl) {
            launchUrl("https://plus.google.com/u/0/communities/109352646351468373340");
          } else if (preference.getKey().equals(RR_ROM_SHARE)) {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, String.format(
                    getActivity().getString(R.string.share_message)));
            startActivity(Intent.createChooser(intent, getActivity().getString(R.string.share_chooser_title)));
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void launchUrl(String url) {
        Uri uriUrl = Uri.parse(url);
        Intent donate = new Intent(Intent.ACTION_VIEW, uriUrl);
        getActivity().startActivity(donate);
    }
}
