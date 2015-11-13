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
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.settings.R;

public class MainActivity extends Activity {
    private LinearLayout mLayout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.extrainfo_layout);
        mLayout = (LinearLayout) findViewById(R.id.mainLinearLayout);
        loadInfo();
    }

    private void addItem(String name, String value) { addItem(name, value, false); }
    private void addItem(String name, String value, boolean ucWords) {
        if (value != null && value.length() > 0) {
            mLayout.addView(new KeyTextView(this, name));
            if (ucWords) value = ExtraInfoLib.ucWords(value);
            mLayout.addView(new ValueTextView(this, value));
        }
    }

    private static class SeparatorTextView extends TextView {
        public SeparatorTextView(Context context) {
            super(context);
            setBackgroundColor(0xFFFFFFFF);
        }
    }

   private static class CategoryTextView extends TextView {
        public CategoryTextView(Context context, String text) {
            super(context);
            int indent = ExtraInfoLib.dpToPx(context, 5);
            int pad = ExtraInfoLib.dpToPx(context, 2);
            setTextAppearance(context, android.R.style.TextAppearance_Medium);
            setBackgroundColor(0xFF1D1D1D);
            setTextColor(0xFFFFFFFF);
            setGravity(Gravity.CENTER_VERTICAL);
            setPadding(indent, pad, pad, pad);
            setText(text);
        }
    }

    private static class KeyTextView extends TextView {
        public KeyTextView(Context context, String text) {
            super(context);
            int indent = ExtraInfoLib.dpToPx(context, 5);
            int pad = ExtraInfoLib.dpToPx(context, 2);
            setTextAppearance(context, android.R.style.TextAppearance_Small);
            setPadding(indent, pad, pad, pad);
            setText(text);
        }
    }

    private static class ValueTextView extends TextView {
        public ValueTextView(Context context, String text) {
            super(context);
            int indent = ExtraInfoLib.dpToPx(context, 20);
            int pad = ExtraInfoLib.dpToPx(context, 2);
            setTextColor(Color.GREEN);
            setPadding(indent, pad, pad, pad);
            setText(text);
        }
    }

    private void loadInfo() {
        ExtraInfo di = new ExtraInfo(this);

        addCategory("BASIC INFO");

        addItem("Manufacturer", di.getBuildManufacturer(), true);
        addItem("Hardware type", di.getPropHardware());
        addItem("System partition", di.getSystemSize() + " Bytes");
        addItem("Data partition", di.getDataSize() + " Bytes");
        addItem("RAM", di.getProcMemTotal());
        addItem("Bootloader", di.getPropBootloader());

        addCategory("CPU INFO");

        addItem("Chipset / Processor", di.getProcCpuDescription());
        addItem("Features", di.getProcCpuFeatures());
        addItem("BogoMIPS", di.getProcCpuBogoMips());
        addItem("ABI", di.getBuildCpuAbi());
        addItem("ABI2", di.getPropCpuAbi2());
        addItem("Architecture", di.getProcCpuArchitecture());
        addItem("Implementer", di.getProcCpuImplementer());
        addItem("Variant", di.getProcCpuVariant());
        addItem("Part", di.getProcCpuPart());
        addItem("Revision", di.getProcCpuRevision());

        addCategory("DISPLAY INFO");

        addItem("Diagonal", di.getDisplayDiagonalInches() + " Inches");
        addItem("Width", di.getDisplayWidthInches() + " Inches");
        addItem("Height", di.getDisplayHeightInches() + " Inches");
        addItem("Width", di.getDisplayWidth() + " Pixels");
        addItem("Height", di.getDisplayHeight() + " Pixels");
        addItem("Density", di.getDisplayDensity());
        addItem("DOTS PER INCH", di.getDisplayDpi());
        addItem("Actual DPI X", di.getDisplayDpiX());
        addItem("Actual DPI Y", di.getDisplayDpiY());
        addItem("Refresh Rate", di.getDisplayRefreshRate());

        }

    @SuppressWarnings("deprecation")
        private void addCategory(String name) {
        mLayout.addView(new CategoryTextView(this, name),
                        new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
        mLayout.addView(new SeparatorTextView(this),
                        new LayoutParams(LayoutParams.FILL_PARENT, ExtraInfoLib.dpToPx(this, 1)));
    }
}
