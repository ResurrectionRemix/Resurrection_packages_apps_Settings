package com.android.settings.rr.changelog;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;

import java.io.BufferedReader;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import com.android.settings.R;

public class ChangelogActivity extends BaseChangelogActivity {

    private static final String CHANGELOG_PATH = "/system/etc/RR/Changelog.txt";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.changelog_activity);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.changelog_name);
        setSupportActionBar(toolbar);

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.changelog);
        recyclerView.setHasFixedSize(true);
        ArrayList<ChangelogItem> changeLogArray = new ArrayList<>();

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy", Locale.ENGLISH);
            Date date;
            Date nowDate = new Date();
            BufferedReader reader = new BufferedReader(new FileReader(CHANGELOG_PATH));
            String line;
            String directory = "";
            String commits = "";
            boolean checknext = false;
            while ((line = reader.readLine()) != null) {
                if (!line.matches("={20}") && !Objects.equals(line.trim(), "")) {
                    if (line.matches("     (\\d\\d\\-\\d\\d\\-\\d{4})")) {//it's date
                        date = sdf.parse(line.trim());
                        long now = nowDate.getTime();
                        long time = date.getTime();
                        final long diff = now - time;
                        String timeString;
                        if (diff < 1000 * 60 * 60 * 24) {
                            timeString = getResources().getString(R.string.changelog_time_today);
                        } else if (diff < 1000 * 60 * 60 * 24 * 2) {
                            timeString = getResources().getString(R.string.changelog_time_yesterday);
                        } else if (diff < 1000 * 60 * 60 * 24 * 3) {
                            timeString = getResources().getString(R.string.changelog_time_2_days);
                        } else if (diff < 1000 * 60 * 60 * 24 * 4) {
                            timeString = getResources().getString(R.string.changelog_time_3_days);
                        } else if (diff < 1000 * 60 * 60 * 24 * 5) {
                            timeString = getResources().getString(R.string.changelog_time_4_days);
                        } else if (diff < 1000 * 60 * 60 * 24 * 6) {
                            timeString = getResources().getString(R.string.changelog_time_5_days);
                        } else if (diff < 1000 * 60 * 60 * 24 * 7) {
                            timeString = getResources().getString(R.string.changelog_time_6_days);
                        } else if (diff < 1000 * 60 * 60 * 24 * 14) {
                            timeString = getResources().getString(R.string.changelog_time_1_week);
                        } else if (diff < 1000 * 60 * 60 * 24 * 21) {
                            timeString = getResources().getString(R.string.changelog_time_2_weeks);
                        } else if (diff < 1000L * 60 * 60 * 24 * 28) {
                            timeString = getResources().getString(R.string.changelog_time_3_weeks);
                        } else {
                            timeString = line.trim().replaceAll("-", "/");
                        }
                        changeLogArray.add(new ChangelogItem(timeString));
                    } else if (line.matches("^\\s*(   \\* )\\S*")) {//it's directory
                        if (checknext) {
                            commits = commits.substring(0, commits.lastIndexOf("\n\n"));//remove lf on end
                            changeLogArray.add(new ChangelogItem(directory, commits));
                            commits = ""; //reset commits
                            checknext = false;
                        } else {
                            checknext = true;
                            commits = "";
                        }
                        directory = line.replaceAll("(   \\* )", "");
                    } else {
                        final String re = "^([a-f0-9]{1,12}) ";
                        line = line.replaceFirst(re, "");
                        commits += line + "\n\n";
                        checknext = true;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        ChangeLogAdapter adapter = new ChangeLogAdapter(this, changeLogArray);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

    }
}
