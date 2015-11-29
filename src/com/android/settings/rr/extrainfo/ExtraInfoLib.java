/*
 * Copyright (C) 2014 The Dirty Unicorns project
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

package com.android.settings.rr.extrainfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import com.android.settings.R;

public final class ExtraInfoLib {
        private ExtraInfoLib() {
                throw new AssertionError();
        }

        public static int dpToPx(Context context, int dp) {
            float scale = context.getResources().getDisplayMetrics().density;
            return (int) (dp * scale + 0.5f);
        }

    public static String ucFirst(String str) {
        if (str == null) throw new NullPointerException("null");
        if (str.length() == 0) return str;
        char[] chars = str.toCharArray();
        chars[0] = Character.toUpperCase(chars[0]);
        return new String(chars);
    }

    public static String ucWords(String str) {
        if (str == null) throw new NullPointerException("null");
        if (str.length() == 0) return str;
        final StringBuilder result = new StringBuilder(str.length());
        String[] words = str.split("\\s");
        for(int i = 0; i < words.length; ++i) {
            if(i>0) result.append(" ");
            result.append(ucFirst(words[i]));
        }
        return result.toString();
    }

    public static List<String> shellExec(String command)
                throws IOException, SecurityException {
        List<String> list = new ArrayList<String>();
        BufferedReader stdInput = null;
        try {
            Process p = Runtime.getRuntime().exec(command);
            stdInput = new BufferedReader(new
                InputStreamReader(p.getInputStream()));
            String s;
            while ((s = stdInput.readLine()) != null) list.add(s);
        } catch (IOException e) {
                stdInput.close();
                throw e;
        }
        catch (SecurityException e) {
                stdInput.close();
                throw e;
        }
        stdInput.close();

        return list;
    }

    public static String round(double number, int decimalPlaces) {
            return round(number, decimalPlaces, false);
    }
    public static String round(double number, int decimalPlaces, boolean groupDigits) {
        NumberFormat numFormat = NumberFormat.getInstance();
        numFormat.setMaximumFractionDigits(decimalPlaces);
        numFormat.setGroupingUsed(groupDigits);
        return numFormat.format(number);
    }

    public static double scaleData(double value, String oldScale, String newScale) {
        final double FAILED = -1;
        if (oldScale.length() > 2 || newScale.length() > 2) return FAILED;
        if (oldScale.equals(newScale)) return value;

        int oldMultiplier = 0;
        int oldBase = 0;
        int newMultiplier = 0;
        int newBase = 0;

        if (oldScale.length() == 2) {
            if (oldScale.startsWith("k")) oldMultiplier = 1024;
            else if (oldScale.startsWith("K")) oldMultiplier = 1000;
            else if (oldScale.startsWith("m")) oldMultiplier = 1024 * 1024;
            else if (oldScale.startsWith("M")) oldMultiplier = 1000 * 1000;
            else if (oldScale.startsWith("g")) oldMultiplier = 1024 * 1024 * 1024;
            else if (oldScale.startsWith("G")) oldMultiplier = 1000 * 1000 * 1000;
            else return FAILED;

            if (oldScale.endsWith("b")) oldBase = 1;
            else if (oldScale.endsWith("B")) oldBase = 8;
            else return FAILED;
        }
        else if (oldScale.length() == 1) {
            if (oldScale.startsWith("k")) { oldMultiplier = 1024; oldBase = 1; }
            else if (oldScale.startsWith("K")) { oldMultiplier = 1000; oldBase = 1; }
            else if (oldScale.startsWith("m")) { oldMultiplier = 1024 * 1024; oldBase = 1; }
            else if (oldScale.startsWith("M")) { oldMultiplier = 1000 * 1000; oldBase = 1; }
            else if (oldScale.startsWith("g")) { oldMultiplier = 1024 * 1024 * 1024; oldBase = 1; }
            else if (oldScale.startsWith("G")) { oldMultiplier = 1000 * 1000 * 1000; oldBase = 1; }
            else if (oldScale.startsWith("b")) { oldMultiplier = 1; oldBase = 1; }
            else if (oldScale.startsWith("B")) { oldMultiplier = 1; oldBase = 8; }
            else return FAILED;
        }
        else { oldMultiplier = 1; oldBase = 1; }

        if (newScale.length() == 2) {
            if (newScale.startsWith("k")) newMultiplier = 1024;
            else if (newScale.startsWith("K")) newMultiplier = 1000;
            else if (newScale.startsWith("m")) newMultiplier = 1024 * 1024;
            else if (newScale.startsWith("M")) newMultiplier = 1000 * 1000;
            else if (newScale.startsWith("g")) newMultiplier = 1024 * 1024 * 1024;
            else if (newScale.startsWith("G")) newMultiplier = 1000 * 1000 * 1000;
            else return FAILED;

            if (newScale.endsWith("b")) newBase = 1;
            else if (newScale.endsWith("B")) newBase = 8;
            else return FAILED;
        }
        else if (newScale.length() == 1) {
            if (newScale.startsWith("k")) { newMultiplier = 1024; newBase = 1; }
            else if (newScale.startsWith("K")) { newMultiplier = 1000; newBase = 1; }
            else if (newScale.startsWith("m")) { newMultiplier = 1024 * 1024; newBase = 1; }
            else if (newScale.startsWith("M")) { newMultiplier = 1000 * 1000; newBase = 1; }
            else if (newScale.startsWith("g")) { newMultiplier = 1024 * 1024 * 1024; newBase = 1; }
            else if (newScale.startsWith("G")) { newMultiplier = 1000 * 1000 * 1000; newBase = 1; }
            else if (newScale.startsWith("b")) { newMultiplier = 1; newBase = 1; }
            else if (newScale.startsWith("B")) { newMultiplier = 1; newBase = 8; }
            else return FAILED;
        }
        else { newMultiplier = 1; newBase = 1; }

        return value * oldBase * oldMultiplier / newBase / newMultiplier;
    }
}
