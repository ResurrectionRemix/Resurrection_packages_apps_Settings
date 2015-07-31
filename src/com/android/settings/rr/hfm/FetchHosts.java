/*
 * Copyright (C) 2014 Android Ice Cold Project
 * Copyright (C) 2014 Dirty Unicorns
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.rr.hfm;

import java.io.IOException;

import com.android.settings.rr.hfm.HfmHelpers;

public class FetchHosts {

    public static String[] stringUrl = {
        "http://download.dirtyunicorns.com/files/misc/hosts/hosts1.txt",
        "http://download.dirtyunicorns.com/files/misc/hosts/hosts2.txt",
        "http://download.dirtyunicorns.com/files/misc/hosts/hosts3.txt",
        "http://download.dirtyunicorns.com/files/misc/hosts/hosts4.txt"
    };

    public static int successfulSources;

    public static void fetch() throws IOException {

        String cmd = "mount -o rw,remount /system && echo \'foo\' > /etc/started.cfg && sed -i -e '$G' /etc/hosts.alt";

        successfulSources = 0;

        int i = 0;
        while (i <= 3) {
            if (HfmHelpers.isAvailable(stringUrl[i])) {
                cmd = cmd + " && wget \"" + stringUrl[i] + "\" -O /etc/hosts" + i; //Get file
                cmd = cmd + " && sed -i -e '$G' /etc/hosts" + i; //Put newline
                successfulSources++;
            }
            i++;
        }

        cmd = cmd + " && cat /etc/hosts[0-9] /etc/hosts.alt > /etc/hosts.tmp" //Merge old & new hosts
                  + " && sort -u /etc/hosts.tmp -o /etc/hosts.tmp" //Remove duplicate lines
                  + " && sed -i '/^[@#]/ d' /etc/hosts.tmp" //Remove commented lines
                  + " && sed -i '/^$/d' /etc/hosts.tmp" //Remove blank lines
                  + " && sed -i '1i#LiquidSmooth\' /etc/hosts.tmp" // Add LiquidSmooth tag
                  + " && cp -f /etc/hosts.tmp /etc/hosts.alt"
                  + " && rm -f /etc/hosts[0-9] /etc/hosts.tmp /etc/started.cfg" //Clean up
                  + " && mount -o ro,remount /system";

       HfmHelpers.RunAsRoot(cmd);
    }
}
