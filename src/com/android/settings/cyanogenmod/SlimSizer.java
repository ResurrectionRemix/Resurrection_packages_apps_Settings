/*=========================================================================
 *
 *  PROJECT:  SlimRoms
 *            Team Slimroms (http://www.slimroms.net)
 *
 *  COPYRIGHT Copyright (C) 2013 Slimroms http://www.slimroms.net
 *            All rights reserved
 *
 *  LICENSE   http://www.gnu.org/licenses/gpl-2.0.html GNU/GPL
 *
 *  AUTHORS:     fronti90
 *  DESCRIPTION: SlimSizer: manage your apps
 *
 *=========================================================================
 */
package com.android.settings.cyanogenmod;

import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Scanner;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.R;

public class SlimSizer extends SettingsPreferenceFragment {
    private final int STARTUP_DIALOG = 1;
    private final int DELETE_DIALOG = 2;
    private final int DELETE_MULTIPLE_DIALOG = 3;
    private final int REBOOT_DIALOG = 4;
    protected ArrayAdapter<String> adapter;
    private ArrayList<String> mSysApp;
    private boolean startup = true;
    public final String systemPath = "/system/app/";
    protected Process superUser;
    protected DataOutputStream dos;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.slim_sizer, container, false);
        return view;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        showSuperuserRequest();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final ImageView delButton = (ImageView) getView().findViewById(R.id.btn_delete);
        final ImageView profileButton = (ImageView) getView().findViewById(R.id.btn_profile);

        if (delButton == null) { return; }

        // create arraylist of apps not to be removed
        final ArrayList<String> safetyList = new ArrayList<String>();
        safetyList.add("CertInstaller.apk");
        safetyList.add("PackageInstaller.apk");
        safetyList.add("TelephonyProvider.apk");

        // create arraylist from /system/app content
        File system = new File(systemPath);
        String[] sysappArray = system.list();
        mSysApp = new ArrayList<String>(
                Arrays.asList(sysappArray));

        // remove "apps not to be removed" from list and sort list
        mSysApp.removeAll(safetyList);
        Collections.sort(mSysApp);

        // populate listview via arrayadapter
        adapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_list_item_multiple_choice, mSysApp);

        final ListView lv = (ListView) getView().findViewById(R.string.listsystem);
        lv.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        lv.setAdapter(adapter);

        // longclick an entry
        lv.setOnItemLongClickListener(new OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
                    final int arg2, long arg3) {
                // create deletion dialog
                String item = lv.getAdapter().getItem(arg2).toString();
                showDialog(DELETE_DIALOG, item, adapter, 0);
                return false;
            }
        });
        // click button delete
        delButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // check which items are selected
                String item = null;
                int itemCounter = 0;
                SparseBooleanArray checked = lv.getCheckedItemPositions();
                for (int i = lv.getCount() - 1; i > 0; i--) {
                    if (checked.get(i)) {
                        item = mSysApp.get(i);
                        itemCounter++;
                    }
                }
                if (item == null) {
                    toast(getResources().getString(
                            R.string.sizer_message_noselect));
                    return;
                } else {
                    showDialog(DELETE_MULTIPLE_DIALOG, item, adapter, itemCounter);
                }
            }
        });
        // click button profile
        profileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // call select dialog
                selectDialog(mSysApp, adapter);
            }
        });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        showSuperuserRequest();
    }

    private void showSuperuserRequest() {
        if (this.getUserVisibleHint() && adapter != null && startup) {
            showDialog(STARTUP_DIALOG, null, adapter, 0);
            startup = false;
        }
    }

    private void showDialog(int id, final String item,
            final ArrayAdapter<String> adapter, int itemCounter) {
        // startup dialog
        final AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());

        if (id == STARTUP_DIALOG) {
            // create warning dialog
            alert.setMessage(R.string.sizer_message_startup)
                    .setTitle(R.string.caution)
                    .setCancelable(true)
                    .setPositiveButton(R.string.ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                        int id) {
                                    // action for ok
                                    dialog.cancel();
                                }
                            });
        // delete dialog
        } else if (id == DELETE_DIALOG) {
            alert.setMessage(R.string.sizer_message_delete)
                    .setPositiveButton(R.string.ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                        int id) {
                                    // action for ok
                                    // call delete
                                    new SlimSizer.SlimDeleter().execute(item);
                                    // remove list entry
                                    adapter.remove(item);
                                    adapter.notifyDataSetChanged();
                                }
                            })
                    .setNegativeButton(R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                        int id) {
                                    // action for cancel
                                    dialog.cancel();
                                }
                            });
        } else if (id == DELETE_MULTIPLE_DIALOG) {
            String message;
            if (itemCounter == 1) {
                message = getResources().getString(R.string.sizer_message_delete_multi_one);
            } else {
                message = getResources().getString(R.string.sizer_message_delete_multi);
            }
            alert.setMessage(message)
                    .setPositiveButton(R.string.ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                        int id) {
                                    final ListView lv = (ListView) getView().findViewById(R.string.listsystem);
                                    ArrayList<String> itemsList = new ArrayList<String>();
                                    SparseBooleanArray checked = lv.getCheckedItemPositions();
                                    for (int i = lv.getCount() - 1; i > 0; i--) {
                                        if (checked.get(i)) {
                                            String appName = mSysApp.get(i);
                                            itemsList.add(appName);
                                            // remove list entry
                                            lv.setItemChecked(i, false);
                                            adapter.remove(appName);
                                        }
                                    }
                                    adapter.notifyDataSetChanged();
                                    new SlimSizer.SlimDeleter().execute(itemsList.toArray(new String[itemsList.size()]));
                                }
                            })
                    .setNegativeButton(R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                        int id) {
                                    // action for cancel
                                    dialog.cancel();
                                }
                            });
        } else if (id == REBOOT_DIALOG) {
        // create warning dialog
        alert.setMessage(R.string.reboot)
                .setTitle(R.string.caution)
                .setCancelable(true)
                .setPositiveButton(R.string.reboot_ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                    int id) {
                                // action for ok
                                try {
                                    dos.writeBytes("reboot");
                                    dos.flush();
                                    dos.close();
                                } catch (IOException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                            }
                })
                .setNegativeButton(R.string.reboot_cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                    int id) {
                                // action for cancel
                                dialog.cancel();
                            }
                        });
        }
        // show warning dialog
        alert.show();
    }

    // profile select dialog
    private void selectDialog(final ArrayList<String> sysAppProfile,
            final ArrayAdapter<String> adapter) {
        AlertDialog.Builder select = new AlertDialog.Builder(getActivity());
        select.setItems(R.array.slimsizer_profile_array,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // The 'which' argument contains the index position
                        // of the selected item
                        short state = sdAvailable();
                        File path = new File(Environment
                                .getExternalStorageDirectory() + "/Slim");
                        File savefile = new File(path + "/slimsizer.stf");
                        if (which == 0) {
                            // load profile action
                            if (state >= 1) {
                                String profile;
                                try {
                                    // read savefile and create arraylist
                                    profile = new Scanner(savefile, "UTF-8")
                                            .useDelimiter("\\A").next();
                                    ArrayList<String> profileState = new ArrayList<String>(
                                            Arrays.asList(profile.split(", ")));
                                    // create arraylist of unique entries in
                                    // sysAppProfile (currently installed apps)
                                    ArrayList<String> deleteList = new ArrayList<String>();
                                    for (String item : sysAppProfile) {
                                        if (!profileState.contains(item)) {
                                            deleteList.add(item);
                                        }
                                    }
                                    // delete all entries in deleteList
                                    ArrayList<String> itemsList = new ArrayList<String>();
                                    for (int i = deleteList.size() - 1; i > 0; i--) {
                                        String item = deleteList.get(i);
                                        itemsList.add(item);
                                        // remove list entry
                                        adapter.remove(item);
                                    }
                                    adapter.notifyDataSetChanged();
                                    new SlimSizer.SlimDeleter().execute(itemsList.toArray(new String[itemsList.size()]));
                                } catch (FileNotFoundException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                toast(getResources().getString(
                                        R.string.sizer_message_sdnoread));
                            }
                        } else if (which == 1) {
                            // save profile action
                            if (state == 2) {
                                try {
                                    // create directory if it doesnt exist
                                    if (!path.exists()) {
                                        path.mkdirs();
                                    }
                                    // create string from arraylists
                                    String lists = sysAppProfile.toString();
                                    lists = lists.replace("][", ",");
                                    lists = lists.replace("[", "");
                                    lists = lists.replace("]", "");
                                    // delete savefile if it exists (overwrite)
                                    if (savefile.exists()) {
                                        savefile.delete();
                                    }
                                    // create savefile and output lists to it
                                    FileWriter outstream = new FileWriter(
                                            savefile);
                                    BufferedWriter save = new BufferedWriter(
                                            outstream);
                                    save.write(lists);
                                    save.close();
                                    // check for success
                                    if (savefile.exists()) {
                                        toast(getResources()
                                                .getString(
                                                        R.string.sizer_message_filesuccess));
                                    } else {
                                        toast(getResources()
                                                .getString(
                                                        R.string.sizer_message_filefail));
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                toast(getResources().getString(
                                        R.string.sizer_message_sdnowrite));
                            }
                        }
                    }
                });
        select.show();
    }

    public void toast(String text) {
        // easy toasts for all!
        Toast toast = Toast.makeText(getView().getContext(), text,
                Toast.LENGTH_SHORT);
        toast.show();
    }

    public short sdAvailable() {
        // check if sdcard is available
        // taken from developer.android.com
        short mExternalStorageAvailable = 0;
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            // We can read and write the media
            mExternalStorageAvailable = 2;
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            // We can only read the media
            mExternalStorageAvailable = 1;
        } else {
            // Something else is wrong. It may be one of many other states, but
            // all we need
            // to know is we can neither read nor write
            mExternalStorageAvailable = 0;
        }
        return mExternalStorageAvailable;
    }

    // mount /system as ro on close
    protected void onStop(Bundle savedInstanceState) throws IOException {
        try {
            dos.writeBytes("\n" + "mount -o remount,ro /system" + "\n");
            dos.writeBytes("\n" + "exit" + "\n");
            dos.flush();
            dos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public class SlimDeleter extends AsyncTask<String, String, Void> {

        private ProgressDialog progress;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (dos == null) {
                try {
                    superUser = new ProcessBuilder("su", "-c", "/system/xbin/ash").start();
                    dos = new DataOutputStream(superUser.getOutputStream());
                    dos.writeBytes("\n" + "mount -o remount,rw /system" + "\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            progress = new ProgressDialog(getView().getContext());
            progress.setTitle(getString(R.string.delete_progress_title));
            progress.setMessage(getString(R.string.delete_progress));
            progress.show();
        }

        protected Void doInBackground(String... params) {
            for (String appName : params) {
                try {
                    dos.writeBytes("\n" + "rm -rf '" + systemPath + appName + "'\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void param) {
            super.onPreExecute();
            try {
                dos.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            progress.dismiss();
            showDialog(REBOOT_DIALOG, null, adapter, 0);
        }
    }
}
