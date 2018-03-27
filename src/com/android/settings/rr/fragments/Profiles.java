/*
 *  Copyright (C) 2017 The OmniROM Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
*/
package com.android.settings.rr.fragments;

import android.content.SharedPreferences;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

public class Profiles {

    public static class Profile implements Comparable<Profile> {

        private String mName;
        private String mSettings;

        public Profile(String name, String settings){
            mName = name;
            mSettings = settings;
        }

        public String getName() {
            return mName;
        }

        public String getSettings() {
            return mSettings;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || ! (o instanceof Profile)) {
                return false;
            }
            return mName.equals(((Profile)o).getName());
        }

        @Override
        public int compareTo(Profile profile) {
            return mName.compareTo(profile.getName());
        }
    }

    private static final String PROFILE_PREFS = "profiles";

    private static List<Profile> mUserProfileList = new ArrayList<>();
    private static List<Profile> mSysteProfileList = new ArrayList<>();

    public static void initProfiles() {
        mSysteProfileList.clear();
	mSysteProfileList.add(new Profile("aggressive", "light_after_inactive_to=30000,light_pre_idle_to=30000,light_idle_to=30000,light_idle_factor=2.0,light_max_idle_to=60000,light_idle_maintenance_min_budget=30000,light_idle_maintenance_max_budget=60000,min_light_maintenance_time=5000,min_deep_maintenance_time=10000,inactive_to=60000,sensing_to=0,locating_to=10000,location_accuracy=20.0,motion_inactive_to=60000,idle_after_inactive_to=0,idle_pending_to=30000,max_idle_pending_to=60000,idle_pending_factor=2.0,idle_to=3600000,max_idle_to=21600000,idle_factor=2.0,min_time_to_alarm=3600000,max_temp_app_whitelist_duration=20000,mms_temp_app_whitelist_duration=20000,sms_temp_app_whitelist_duration=20000,notification_whitelist_duration=20000"));
	mSysteProfileList.add(new Profile("rrprofile", "light_after_inactive_to=60000, light_pre_idle_to=60000, light_idle_to=70000, light_idle_factor=2.0, light_max_idle_to=90000, light_idle_maintenance_min_budget=60000, light_idle_maintenance_max_budget=100000, min_light_maintenance_time=5000, min_deep_maintenance_time=30000, inactive_to=100000, sensing_to=0, locating_to=10000, location_accuracy=20.0, motion_inactive_to=30000, idle_after_inactive_to=0, idle_pending_to=60000, max_idle_pending_to=100000, idle_pending_factor=2.0, idle_to=180000, max_idle_to=360000, idle_factor=2.0, min_time_to_alarm=180000, max_temp_app_whitelist_duration=30000, mms_temp_app_whitelist_duration=30000, sms_temp_app_whitelist_duration=20000"));
    }

    public static List<Profile> getUserProfileList() {
        return mUserProfileList;
    }

    public static List<Profile> getSystemProfileList() {
        return mSysteProfileList;
    }

    public static void addProfile(Profile profile) {
        if (mUserProfileList.contains(profile)) {
            mUserProfileList.remove(profile);
        }
        mUserProfileList.add(profile);
    }

    public static void removeProfile(Profile profile) {
        mUserProfileList.remove(profile);
    }

    public static void loadUserProfiles(SharedPreferences sharedPref) {
        String userProfile = sharedPref.getString(PROFILE_PREFS , null);
        mUserProfileList.clear();
        if (!TextUtils.isEmpty(userProfile)) {
            String[] profiles = userProfile.split(";");
            for (String profile : profiles) {
                String[] profileParts = profile.split(":");
                Profile p = new Profile(profileParts[0], profileParts[1]);
                mUserProfileList.add(p);
            }
        }
    }

    public static void saveUserProfiles(SharedPreferences sharedPref){
        if (mUserProfileList.size() != 0) {
            StringBuffer sb = new StringBuffer();
            for (Profile p : mUserProfileList) {
                sb.append(p.getName() + ":" + p.getSettings() + ";");
            }
            sharedPref.edit().putString(PROFILE_PREFS, sb.toString()).commit();
        } else {
            sharedPref.edit().remove(PROFILE_PREFS).commit();
        }
    }
}


