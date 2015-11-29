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

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.StatFs;
import android.util.DisplayMetrics;
import android.view.Display;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import com.android.settings.R;

public class ExtraInfo {
    private static final String UNKNOWN = "unknown";
    private final Display mDisplay;
    private DisplayMetrics mDisplayMetrics;

    public String getBuildManufacturer() {         return gets(Build.MANUFACTURER); }
    public String getBuildVersionCodename() {      return gets(Build.VERSION.CODENAME); }
    public String getBuildCpuAbi() {               return gets(Build.CPU_ABI); }
    public String getBuildCpuAbi2() {              return gets(Build.CPU_ABI2); }
    public String getBuildBootloader() {           return gets(Build.BOOTLOADER); }
    public String getBuildDisplay() {              return gets(Build.DISPLAY); }

    public String getProp(String prop) {
        if (prop == null || prop.length() == 0) return UNKNOWN;
       String s = UNKNOWN;
        try { s = ExtraInfoLib.shellExec("getprop " + prop).get(0).trim(); }
        catch (IOException e) {}
        catch (SecurityException e) {}
        if (s == "[]") return UNKNOWN;
        return s;
    }

    public ExtraInfo(Context context) {
        mDisplay = ((Activity) context).getWindowManager().getDefaultDisplay();
        mDisplayMetrics = new DisplayMetrics();
        mDisplay.getMetrics(mDisplayMetrics);
    }

    private String gets(String s) {
        return (s == null || s.length() == 0) ? UNKNOWN : s;
        }

        @SuppressWarnings("deprecation")
        public String getSystemSize() {
        StatFs stat = new StatFs("/system");
        return String.valueOf((long)stat.getBlockSize() * (long)stat.getBlockCount());
    }

        @SuppressWarnings("deprecation")
        public String getSystemSize(String scale, int decimalPlaces) {
        StatFs stat = new StatFs("/system");
        return ExtraInfoLib.round(
                        ExtraInfoLib.scaleData(
                                (double)stat.getBlockSize() * (double)stat.getBlockCount(),
                                "B", scale), decimalPlaces) + " " + scale;
    }

        @SuppressWarnings("deprecation")
        public String getDataSize() {
        StatFs stat = new StatFs("/data");
        return String.valueOf((long)stat.getBlockSize() * (long)stat.getBlockCount());
    }

        @SuppressWarnings("deprecation")
        public String getDataSize(String scale, int decimalPlaces) {
        StatFs stat = new StatFs("/data");
        return ExtraInfoLib.round(
                ExtraInfoLib.scaleData(
                    (double)stat.getBlockSize() * (double)stat.getBlockCount(),
                    "B", scale), decimalPlaces) + " " + scale;
    }

    public String getPropName() {           return getProp("ro.product.name"); }
    public String getPropHardware() {       return getProp("ro.hardware"); }
    public String getPropBootloader() {     return getProp("ro.bootloader"); }
    public String getPropCpuAbi2() {     return getProp("ro.product.cpu.abi2"); }

    private List<String> getProc(String proc) {
        List<String> list = new ArrayList<String>();
        if (proc == null || proc.length() == 0) return list;
        try { list = ExtraInfoLib.shellExec("cat /proc/" + proc); }
        catch (IOException e) {}
        catch (SecurityException e) {}
        return list;
    }
    public String getProcCpuField(String field) {
        if (field == null || field.length() == 0) return UNKNOWN;
        List<String> list = getProc("cpuinfo");
        for (String s : list) {
            String[] parts = s.split(":", 2);
            if (parts[0].trim().equals(field)) { return parts[1].trim(); }
        }
        return UNKNOWN;
    }
    public String getProcMemField(String field) {
        if (field == null || field.length() == 0) return UNKNOWN;
        List<String> list = getProc("meminfo");
        for (String s : list) {
            String[] parts = s.split(":", 2);
            if (parts[0].trim().equals(field)) { return parts[1].trim(); }
        }
        return UNKNOWN;
    }

    public List<String> getProcCpuInfo() {      return getProc("cpuinfo"); }

    public String getProcCpuDescription() {     return getProcCpuField("Processor"); }
    public String getProcCpuBogoMips() {        return getProcCpuField("BogoMIPS"); }
    public String getProcCpuFeatures() {        return getProcCpuField("Features"); }
    public String getProcCpuImplementer() {     return getProcCpuField("CPU implementer"); }
    public String getProcCpuArchitecture() {    return getProcCpuField("CPU architecture"); }
    public String getProcCpuVariant() {         return getProcCpuField("CPU variant"); }
    public String getProcCpuPart() {            return getProcCpuField("CPU part"); }
    public String getProcCpuRevision() {        return getProcCpuField("CPU revision"); }

    public List<String> getProcMemInfo() { return getProc("meminfo"); }

    public String getProcMemTotal() { return getProcMemField("MemTotal"); }
    public String getProcMemTotal(String scale) {
        if (scale.length() == 0) return UNKNOWN;
        String[] parts = getProcMemTotal().split("\\s", 2);
        double value = 0.0;
        try { value = Double.valueOf(parts[0]); }
        catch (NumberFormatException e) {}
        return String.valueOf(ExtraInfoLib.scaleData(value, parts[0], scale));
    }

    public String getProcVersion() { return getProc("version").get(0); }
    public String getProcVersionKernel() {
        String[] parts = getProc("version").get(0).split("\\s");
        return parts[2];
    }

        public String getDisplayWidthInches() {
        NumberFormat numFormat = NumberFormat.getInstance();
        numFormat.setMaximumFractionDigits(2);
        return numFormat.format(mDisplayMetrics.xdpi);
    }

        public String getDisplayHeightInches() {
        NumberFormat numFormat = NumberFormat.getInstance();
        numFormat.setMaximumFractionDigits(2);
        return numFormat.format(mDisplayMetrics.ydpi);
    }

        public String getDisplayDiagonalInches() {
        NumberFormat numFormat = NumberFormat.getInstance();
        numFormat.setMaximumFractionDigits(2);

        return numFormat.format(
                        Math.sqrt(
                        Math.pow(mDisplayMetrics.xdpi, 2) +
                        Math.pow(mDisplayMetrics.ydpi, 2)));
    }

        @SuppressWarnings("deprecation")
        public String getDisplayWidth() { return String.valueOf(mDisplay.getWidth()); }
        @SuppressWarnings("deprecation")
        public String getDisplayHeight() { return String.valueOf(mDisplay.getHeight()); }
    public String getDisplayDpiX() { return String.valueOf(mDisplayMetrics.xdpi); }
    public String getDisplayDpiY() { return String.valueOf(mDisplayMetrics.ydpi); }
    public String getDisplayRefreshRate() { return String.valueOf(mDisplay.getRefreshRate()); }
    public String getDisplayLogicalDensity() { return String.valueOf(mDisplayMetrics.density); }

    public String getDisplayDpi() {
        return String.valueOf(mDisplayMetrics.densityDpi);
    }
    public String getDisplayDensity() {
            if ((int) (160 * mDisplayMetrics.density) <= 159) return "LDPI";
            if ((int) (160 * mDisplayMetrics.density) <= 239) return "MDPI";
            if ((int) (160 * mDisplayMetrics.density) <= 319) return "HDPI";
            if ((int) (160 * mDisplayMetrics.density) <= 479) return "XHDPI";
            if ((int) (160 * mDisplayMetrics.density) <= 639) return "XXHDPI";
            if ((int) (160 * mDisplayMetrics.density) <= 799) return "XXXHDPI";
        return UNKNOWN;
    }
}
