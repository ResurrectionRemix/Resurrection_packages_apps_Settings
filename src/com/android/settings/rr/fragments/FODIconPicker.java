/*
 * Copyright (C) 2020 crDroid Android Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.rr.fragments;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.core.content.res.TypedArrayUtils;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settingslib.Utils;
import com.android.settingslib.widget.LayoutPreference;


public class FODIconPicker extends LayoutPreference {

    private Context mContext;

    private static ImageButton Button0;
    private static ImageButton Button1;
    private static ImageButton Button2;
    private static ImageButton Button3;
    private static ImageButton Button4;
    private static ImageButton Button5;
    private static ImageButton Button6;
    private static ImageButton Button7;
    private static ImageButton Button8;
    private static ImageButton Button9;
    private static ImageButton Button10;
    private static ImageButton Button11;
    private static ImageButton Button12;
    private static ImageButton Button13;
    private static ImageButton Button14;
    private static ImageButton Button15;
    private static ImageButton Button16;
    private static ImageButton Button17;
    private static ImageButton Button18;
    private static ImageButton Button19;
    private static ImageButton Button20;
    private static ImageButton Button21;
    private static ImageButton Button22;
    private static ImageButton Button23;
    private static ImageButton Button24;
    private static ImageButton Button25;
    private static ImageButton Button26;
    private static ImageButton Button27;
    private static ImageButton Button28;
    private static ImageButton Button29;
    private static ImageButton Button30;
    private static ImageButton Button31;
    private static ImageButton Button32;
    private static ImageButton Button33;
    private static ImageButton Button34;
    private static ImageButton Button35;
    private static ImageButton Button36;
    private static ImageButton Button37;
    private static ImageButton Button38;
    private static ImageButton Button39;
    private static ImageButton Button40;
    private static ImageButton Button41;
    private static ImageButton Button42;
    private static ImageButton Button43;
    private static ImageButton Button44;
    private static ImageButton Button45;
    private static ImageButton Button46;
    private static ImageButton Button47;
    private static ImageButton Button48;
    private static ImageButton Button49;
    private static ImageButton Button50;
    private static ImageButton Button51;
    private static ImageButton Button52;
    private static ImageButton Button53;

    private static final String TAG = "FODIconPicker";

    public FODIconPicker(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        setView();
    }

    public FODIconPicker(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private void setView() {
        Button0 = findViewById(R.id.fodicon0_button);
        Button1 = findViewById(R.id.fodicon1_button);
        Button2 = findViewById(R.id.fodicon2_button);
        Button3 = findViewById(R.id.fodicon3_button);
        Button4 = findViewById(R.id.fodicon4_button);
        Button5 = findViewById(R.id.fodicon5_button);
        Button6 = findViewById(R.id.fodicon6_button);
        Button7 = findViewById(R.id.fodicon7_button);
        Button8 = findViewById(R.id.fodicon8_button);
        Button9 = findViewById(R.id.fodicon9_button);
        Button10 = findViewById(R.id.fodicon10_button);
        Button11 = findViewById(R.id.fodicon11_button);
        Button12 = findViewById(R.id.fodicon12_button);
        Button13 = findViewById(R.id.fodicon13_button);
        Button14 = findViewById(R.id.fodicon14_button);
        Button15 = findViewById(R.id.fodicon15_button);
        Button16 = findViewById(R.id.fodicon16_button);
        Button17 = findViewById(R.id.fodicon17_button);
        Button18 = findViewById(R.id.fodicon18_button);
        Button19 = findViewById(R.id.fodicon19_button);
        Button20 = findViewById(R.id.fodicon20_button);
        Button21 = findViewById(R.id.fodicon21_button);
        Button22 = findViewById(R.id.fodicon22_button);
        Button23 = findViewById(R.id.fodicon23_button);
        Button24 = findViewById(R.id.fodiconunfunny_button);
        Button25 = findViewById(R.id.fodiconspiral_button);
        Button26 = findViewById(R.id.fodicon26_button);
        Button27 = findViewById(R.id.fodicon27_button);
        Button28 = findViewById(R.id.fodicon28_button);
        Button29 = findViewById(R.id.fodicon29_button);
        Button30 = findViewById(R.id.fodicon30_button);
        Button31 = findViewById(R.id.fodicon31_button);
        Button32 = findViewById(R.id.fodicon32_button);
        Button33 = findViewById(R.id.fodicon33_button);
        Button34 = findViewById(R.id.fodicon34_button);
        Button35 = findViewById(R.id.fodicon35_button);
        Button36 = findViewById(R.id.fodicon36_button);
        Button37 = findViewById(R.id.fodicon37_button);
        Button38 = findViewById(R.id.fodicon38_button);
        Button39 = findViewById(R.id.fodicon39_button);
        Button40 = findViewById(R.id.fodicon40_button);
        Button41 = findViewById(R.id.fodicon41_button);
        Button42 = findViewById(R.id.fodicon42_button);
        Button43 = findViewById(R.id.fodicon43_button);
        Button44 = findViewById(R.id.fodicon44_button);
        Button45 = findViewById(R.id.fodicon45_button);
        Button46 = findViewById(R.id.fodicon46_button);
        Button47 = findViewById(R.id.fodicon47_button);
        Button48 = findViewById(R.id.fodicon48_button);
        Button49 = findViewById(R.id.fodicon49_button);
        Button50 = findViewById(R.id.fodicon50_button);
        Button51 = findViewById(R.id.fodicon51_button);
        Button52 = findViewById(R.id.fodicon52_button);
        Button53 = findViewById(R.id.fodicon53_button);

        int defaultfodicon = Settings.System.getInt(
                mContext.getContentResolver(), Settings.System.FOD_ICON, 3);
        if (defaultfodicon==0) {
            updateHighlightedItem(Button0);
        } else if (defaultfodicon == 1) {
            updateHighlightedItem(Button1);
        } else if (defaultfodicon == 2) {
            updateHighlightedItem(Button2);
        } else if (defaultfodicon == 3) {
            updateHighlightedItem(Button3);
        } else if (defaultfodicon == 4) {
            updateHighlightedItem(Button4);
        } else if (defaultfodicon == 5) {
            updateHighlightedItem(Button5);
        } else if (defaultfodicon == 6) {
            updateHighlightedItem(Button6);
        } else if (defaultfodicon == 7) {
            updateHighlightedItem(Button7);
        } else if (defaultfodicon == 8) {
            updateHighlightedItem(Button8);
        } else if (defaultfodicon == 9) {
            updateHighlightedItem(Button9);
        } else if (defaultfodicon == 10) {
            updateHighlightedItem(Button10);
        } else if (defaultfodicon == 11) {
            updateHighlightedItem(Button11);
        } else if (defaultfodicon == 12) {
            updateHighlightedItem(Button12);
        } else if (defaultfodicon == 13) {
            updateHighlightedItem(Button13);
        } else if (defaultfodicon == 14) {
            updateHighlightedItem(Button14);
        } else if (defaultfodicon == 15) {
            updateHighlightedItem(Button15);
        } else if (defaultfodicon == 16) {
            updateHighlightedItem(Button16);
        } else if (defaultfodicon == 17) {
            updateHighlightedItem(Button17);
        } else if (defaultfodicon == 18) {
            updateHighlightedItem(Button18);
        } else if (defaultfodicon == 19) {
            updateHighlightedItem(Button19);
        } else if (defaultfodicon == 20) {
            updateHighlightedItem(Button20);
        } else if (defaultfodicon == 21) {
            updateHighlightedItem(Button21);
        } else if (defaultfodicon == 22) {
            updateHighlightedItem(Button22);
        } else if (defaultfodicon == 23) {
            updateHighlightedItem(Button23);
        } else if (defaultfodicon == 24) {
            updateHighlightedItem(Button24);
        } else if (defaultfodicon == 25) {
            updateHighlightedItem(Button25);
        } else if (defaultfodicon == 26) {
            updateHighlightedItem(Button26);
        } else if (defaultfodicon == 27) {
            updateHighlightedItem(Button27);
        } else if (defaultfodicon == 28) {
            updateHighlightedItem(Button28);
        } else if (defaultfodicon == 29) {
            updateHighlightedItem(Button29);
        } else if (defaultfodicon == 30) {
            updateHighlightedItem(Button30);
        } else if (defaultfodicon == 31) {
            updateHighlightedItem(Button31);
        } else if (defaultfodicon == 32) {
            updateHighlightedItem(Button32);
        } else if (defaultfodicon == 33) {
            updateHighlightedItem(Button33);
        } else if (defaultfodicon == 34) {
            updateHighlightedItem(Button34);
        } else if (defaultfodicon == 35) {
            updateHighlightedItem(Button35);
        } else if (defaultfodicon == 36) {
            updateHighlightedItem(Button36);
        } else if (defaultfodicon == 37) {
            updateHighlightedItem(Button37);
        } else if (defaultfodicon == 38) {
            updateHighlightedItem(Button38);
        } else if (defaultfodicon == 39) {
            updateHighlightedItem(Button39);
        } else if (defaultfodicon == 40) {
            updateHighlightedItem(Button40);
        } else if (defaultfodicon == 41) {
            updateHighlightedItem(Button41);
        } else if (defaultfodicon == 42) {
            updateHighlightedItem(Button42);
        } else if (defaultfodicon == 43) {
            updateHighlightedItem(Button43);
        } else if (defaultfodicon == 44) {
            updateHighlightedItem(Button44);
        } else if (defaultfodicon == 45) {
            updateHighlightedItem(Button45);
        } else if (defaultfodicon == 46) {
            updateHighlightedItem(Button46);
        } else if (defaultfodicon == 47) {
            updateHighlightedItem(Button47);
        } else if (defaultfodicon == 48) {
            updateHighlightedItem(Button48);
        } else if (defaultfodicon == 49) {
            updateHighlightedItem(Button49);
        } else if (defaultfodicon == 50) {
            updateHighlightedItem(Button50);
        } else if (defaultfodicon == 51) {
            updateHighlightedItem(Button51);
        } else if (defaultfodicon == 52) {
            updateHighlightedItem(Button52);
        } else if (defaultfodicon == 53) {
            updateHighlightedItem(Button53);
        }

        Button0.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateSettings(0);
                updateHighlightedItem(Button0);
            }
        });
        Button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateSettings(1);
                updateHighlightedItem(Button1);
            }
        });
        Button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateSettings(2);
                updateHighlightedItem(Button2);
            }
        });
        Button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateSettings(3);
                updateHighlightedItem(Button3);
            }
        });
        Button4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateSettings(4);
                updateHighlightedItem(Button4);
            }
        });
        Button5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateSettings(5);
                updateHighlightedItem(Button5);
            }
        });
        Button6.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateSettings(6);
                updateHighlightedItem(Button6);
            }
        });
        Button7.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateSettings(7);
                updateHighlightedItem(Button7);
            }
        });
        Button8.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateSettings(8);
                updateHighlightedItem(Button8);
            }
        });
        Button9.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateSettings(9);
                updateHighlightedItem(Button9);
            }
        });
        Button10.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateSettings(10);
                updateHighlightedItem(Button10);
            }
        });
        Button11.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateSettings(11);
                updateHighlightedItem(Button11);
            }
        });
        Button12.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateSettings(12);
                updateHighlightedItem(Button12);
            }
        });
        Button13.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateSettings(13);
                updateHighlightedItem(Button13);
            }
        });
        Button14.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateSettings(14);
                updateHighlightedItem(Button14);
            }
        });
        Button15.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateSettings(15);
                updateHighlightedItem(Button15);
            }
        });
        Button16.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateSettings(16);
                updateHighlightedItem(Button16);
            }
        });
        Button17.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateSettings(17);
                updateHighlightedItem(Button17);
            }
        });
        Button18.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateSettings(18);
                updateHighlightedItem(Button18);
            }
        });
        Button19.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateSettings(19);
                updateHighlightedItem(Button19);
            }
        });
        Button20.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateSettings(20);
                updateHighlightedItem(Button20);
            }
        });
        Button21.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateSettings(21);
                updateHighlightedItem(Button21);
            }
        });
        Button22.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateSettings(22);
                updateHighlightedItem(Button22);
            }
        });
        Button23.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateSettings(23);
                updateHighlightedItem(Button23);
            }
        });
        Button24.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateSettings(24);
                updateHighlightedItem(Button24);
            }
        });
        Button25.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateSettings(25);
                updateHighlightedItem(Button25);
            }
        });
        Button26.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateSettings(26);
                updateHighlightedItem(Button26);
            }
        });
        Button27.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateSettings(27);
                updateHighlightedItem(Button27);
            }
        });
        Button28.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateSettings(28);
                updateHighlightedItem(Button28);
            }
        });
        Button29.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateSettings(29);
                updateHighlightedItem(Button29);
            }
        });
        Button30.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateSettings(30);
                updateHighlightedItem(Button30);
            }
        });
        Button31.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateSettings(31);
                updateHighlightedItem(Button31);
            }
        });
        Button32.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateSettings(32);
                updateHighlightedItem(Button32);
            }
        });
        Button33.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateSettings(33);
                updateHighlightedItem(Button33);
            }
        });
        Button34.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateSettings(34);
                updateHighlightedItem(Button34);
            }
        });
        Button35.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateSettings(35);
                updateHighlightedItem(Button35);
            }
        });
        Button36.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateSettings(36);
                updateHighlightedItem(Button36);
            }
        });
        Button37.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateSettings(37);
                updateHighlightedItem(Button37);
            }
        });
        Button38.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateSettings(38);
                updateHighlightedItem(Button38);
            }
        });
        Button39.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateSettings(39);
                updateHighlightedItem(Button39);
            }
        });
        Button40.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateSettings(40);
                updateHighlightedItem(Button40);
            }
        });
        Button41.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateSettings(41);
                updateHighlightedItem(Button41);
            }
        });
        Button42.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateSettings(42);
                updateHighlightedItem(Button42);
            }
        });
        Button43.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateSettings(43);
                updateHighlightedItem(Button43);
            }
        });
        Button44.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateSettings(44);
                updateHighlightedItem(Button44);
            }
        });
        Button45.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateSettings(45);
                updateHighlightedItem(Button45);
            }
        });
        Button46.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateSettings(46);
                updateHighlightedItem(Button46);
            }
        });
        Button47.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateSettings(47);
                updateHighlightedItem(Button47);
            }
        });
        Button48.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateSettings(48);
                updateHighlightedItem(Button48);
            }
        });
        Button49.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateSettings(49);
                updateHighlightedItem(Button49);
            }
        });
        Button50.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateSettings(50);
                updateHighlightedItem(Button50);
            }
        });
        Button51.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateSettings(51);
                updateHighlightedItem(Button51);
            }
        });
        Button52.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateSettings(52);
                updateHighlightedItem(Button52);
            }
        });
        Button53.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateSettings(53);
                updateHighlightedItem(Button53);
            }
        });


    }

    private void updateSettings(int fodicon) {
        Settings.System.putInt(mContext.getContentResolver(), Settings.System.FOD_ICON, fodicon);
    }

    private void updateHighlightedItem(ImageButton activebutton) {
        int defaultcolor = mContext.getResources().getColor(R.color.fod_item_background_stroke_color);
        ColorStateList defaulttint = ColorStateList.valueOf(defaultcolor);
        Button0.setBackgroundTintList(defaulttint);
        Button1.setBackgroundTintList(defaulttint);
        Button2.setBackgroundTintList(defaulttint);
        Button3.setBackgroundTintList(defaulttint);
        Button4.setBackgroundTintList(defaulttint);
        Button5.setBackgroundTintList(defaulttint);
        Button6.setBackgroundTintList(defaulttint);
        Button7.setBackgroundTintList(defaulttint);
        Button8.setBackgroundTintList(defaulttint);
        Button9.setBackgroundTintList(defaulttint);
        Button10.setBackgroundTintList(defaulttint);
        Button11.setBackgroundTintList(defaulttint);
        Button12.setBackgroundTintList(defaulttint);
        Button13.setBackgroundTintList(defaulttint);
        Button14.setBackgroundTintList(defaulttint);
        Button15.setBackgroundTintList(defaulttint);
        Button16.setBackgroundTintList(defaulttint);
        Button17.setBackgroundTintList(defaulttint);
        Button18.setBackgroundTintList(defaulttint);
        Button19.setBackgroundTintList(defaulttint);
        Button20.setBackgroundTintList(defaulttint);
        Button21.setBackgroundTintList(defaulttint);
        Button22.setBackgroundTintList(defaulttint);
        Button23.setBackgroundTintList(defaulttint);
        Button24.setBackgroundTintList(defaulttint);
        Button25.setBackgroundTintList(defaulttint);
        Button26.setBackgroundTintList(defaulttint);
        Button27.setBackgroundTintList(defaulttint);
        Button28.setBackgroundTintList(defaulttint);
        Button29.setBackgroundTintList(defaulttint);
        Button30.setBackgroundTintList(defaulttint);
        Button31.setBackgroundTintList(defaulttint);
        Button32.setBackgroundTintList(defaulttint);
        Button33.setBackgroundTintList(defaulttint);
        Button34.setBackgroundTintList(defaulttint);
        Button35.setBackgroundTintList(defaulttint);
        Button36.setBackgroundTintList(defaulttint);
        Button37.setBackgroundTintList(defaulttint);
        Button38.setBackgroundTintList(defaulttint);
        Button39.setBackgroundTintList(defaulttint);
        Button40.setBackgroundTintList(defaulttint);
        Button41.setBackgroundTintList(defaulttint);
        Button42.setBackgroundTintList(defaulttint);
        Button43.setBackgroundTintList(defaulttint);
        Button44.setBackgroundTintList(defaulttint);
        Button45.setBackgroundTintList(defaulttint);
        Button46.setBackgroundTintList(defaulttint);
        Button47.setBackgroundTintList(defaulttint);
        Button48.setBackgroundTintList(defaulttint);
        Button49.setBackgroundTintList(defaulttint);
        Button50.setBackgroundTintList(defaulttint);
        Button51.setBackgroundTintList(defaulttint);
        Button52.setBackgroundTintList(defaulttint);
        Button53.setBackgroundTintList(defaulttint);
        activebutton.setBackgroundTintList(Utils.getColorAttr(getContext(), android.R.attr.colorAccent));
    }
}

