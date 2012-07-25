
package com.android.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IPowerManager;
import android.os.ServiceManager;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.android.settings.R;

public class LightLevelsActivity extends Activity implements OnClickListener {

    // IDs used by dynamically created widgets
    // Levels textviews 1000-1999
    // Levels buttons 2000-2999
    // Lcd buttons 3000-3999
    // Buttons buttons 4000-4999
    // Keyboard buttons 5000-5999

    private static final int UPDATE_RATE = 400;

    private boolean mHasKeyboard;
    private boolean mHasChanges;
    private Button mSave;
    private Button mDefaults;
    private Button mReload;
    private Button mNumLevels;
    private TextView mSensor;
    private TextView mScreen;
    private TextView mButtons;
    private TextView mKeyboard;
    private int[] mLevels;
    private int[] mLcdValues;
    private int[] mBtnValues;
    private int[] mKbValues;
    private AlertDialog mDialog;
    private EditText mEditor;
    private int mSensorRange;
    private int mEditedId;

    private Handler mHandler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.ll_title);
        setContentView(R.layout.lightlevels);

        mSensorRange = (int) ((SensorManager) getSystemService(SENSOR_SERVICE)).getDefaultSensor(
                Sensor.TYPE_LIGHT).getMaximumRange();
        mHandler = new Handler();
        mSave = (Button) findViewById(R.id.btn_save);
        mSave.setOnClickListener(this);
        mDefaults = (Button) findViewById(R.id.btn_default);
        mDefaults.setOnClickListener(this);
        mReload = (Button) findViewById(R.id.btn_reload);
        mReload.setOnClickListener(this);
        mSensor = (TextView) findViewById(R.id.ll_tw_lux_value);
        mScreen = (TextView) findViewById(R.id.ll_tw_lcd_value);
        mButtons = (TextView) findViewById(R.id.ll_tw_btn_value);
        mKeyboard = (TextView) findViewById(R.id.ll_tw_kb_value);
        mNumLevels = (Button) findViewById(R.id.btn_num_levels);
        mNumLevels.setOnClickListener(this);

        if (getResources().getConfiguration().keyboard == Configuration.KEYBOARD_NOKEYS) {
            ((TableLayout) findViewById(R.id.ll_table_info))
                    .removeView(findViewById(R.id.table_row_kb));
            ((TableRow) findViewById(R.id.ll_table_row_headers))
                    .removeView(findViewById(R.id.ll_tw_header_kb));
            mHasKeyboard = false;
        } else {
            mHasKeyboard = true;
        }

        mEditor = new EditText(this);
        mEditor.setInputType(InputType.TYPE_CLASS_NUMBER);
        mEditor.setImeOptions(EditorInfo.IME_ACTION_NONE);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setPositiveButton(getString(android.R.string.ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialogOk();
                    }
                }).setNegativeButton(android.R.string.cancel,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                }).setView(mEditor);
        mDialog = builder.create();
        mDialog.setOwnerActivity(this);
        mDialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
                        | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        loadData(false);
        mHasChanges = false;
        updateButtons();
    }

    @Override
    public void onResume() {
        super.onResume();
        mUpdateTask.run();
    }

    @Override
    public void onPause() {
        super.onPause();
        mHandler.removeCallbacks(mUpdateTask);
    }

    @Override
    public void onClick(View v) {
        if (v == mSave) {
            save();
        } else if (v == mDefaults) {
            loadData(true);
            mHasChanges = true;
            updateButtons();
        } else if (v == mReload) {
            loadData(false);
            mHasChanges = false;
            updateButtons();
        } else if (v == mNumLevels) {
            mDialog.setMessage(2 + " - " + mSensorRange);
            mEditor.setText(String.valueOf(mLevels.length + 1));
            mEditor.selectAll();
            mEditedId = -1337;
            mDialog.show();
        } else {
            int id = v.getId();
            int value = -1;
            int min = 0;
            int max = 0;
            if (id >= 2000 && id < 3000) {
                value = mLevels[id - 2000];
                min = 0;
                max = mSensorRange;
            } else if (id >= 3000 && id < 4000) {
                value = mLcdValues[id - 3000];
                min = Settings.System.getInt(getContentResolver(),
                        Settings.System.LIGHT_SCREEN_DIM,
                        android.os.PowerManager.BRIGHTNESS_DIM);
                max = android.os.PowerManager.BRIGHTNESS_ON;
            } else if (id >= 4000 && id < 5000) {
                value = mBtnValues[id - 4000];
                min = android.os.PowerManager.BRIGHTNESS_OFF;
                max = android.os.PowerManager.BRIGHTNESS_ON;
            } else if (id >= 5000 && id < 6000) {
                value = mKbValues[id - 5000];
                min = android.os.PowerManager.BRIGHTNESS_OFF;
                max = android.os.PowerManager.BRIGHTNESS_ON;
            } else {
                value = -1;
            }

            if (value >= 0) {
                mDialog.setMessage(min + " - " + max);
                mEditor.setText(String.valueOf(value));
                mEditor.selectAll();
                mEditedId = id;
                mDialog.show();
            }
        }
    }

    private void updateButtons() {
        mSave.setEnabled(mHasChanges);
    }

    private void dialogOk() {
        boolean changed = false;
        try {
            int value = Integer.valueOf(mEditor.getText().toString());
            int valLimitHi = android.os.PowerManager.BRIGHTNESS_ON;
            if (mEditedId == -1337) {
                if (value > 1 && value != (mLevels.length + 1)) {
                    int[] tmp = new int[value - 1];
                    System.arraycopy(mLevels, 0, tmp, 0, Math.min(tmp.length, mLevels.length));
                    mLevels = tmp;

                    tmp = new int[value];
                    System.arraycopy(mLcdValues, 0, tmp, 0, Math.min(tmp.length, mLcdValues.length));
                    mLcdValues = tmp;

                    tmp = new int[value];
                    System.arraycopy(mBtnValues, 0, tmp, 0, Math.min(tmp.length, mBtnValues.length));
                    mBtnValues = tmp;

                    tmp = new int[value];
                    System.arraycopy(mKbValues, 0, tmp, 0, Math.min(tmp.length, mKbValues.length));
                    mKbValues = tmp;

                    createEditor();
                    mHasChanges = true;
                    updateButtons();
                }
            } else if (mEditedId >= 2000 && mEditedId < 3000) {
                if (value >= 0 && value <= mSensorRange) {
                    mLevels[mEditedId - 2000] = value;
                    ((Button) findViewById(mEditedId)).setText(String.valueOf(value));
                    ((TextView) findViewById(mEditedId - 1000)).setText(String.valueOf(value - 1));
                    changed = true;
                }
            } else if (mEditedId >= 3000 && mEditedId < 4000) {
                if (value >= Settings.System.getInt(getContentResolver(),
                        Settings.System.LIGHT_SCREEN_DIM,
                        android.os.PowerManager.BRIGHTNESS_DIM)
                        && value <= valLimitHi) {
                    mLcdValues[mEditedId - 3000] = value;
                    ((Button) findViewById(mEditedId)).setText(String.valueOf(value));
                    changed = true;
                }
            } else if (mEditedId >= 4000 && mEditedId < 5000) {
                if (value >= android.os.PowerManager.BRIGHTNESS_OFF
                        && value <= valLimitHi) {
                    mBtnValues[mEditedId - 4000] = value;
                    ((Button) findViewById(mEditedId)).setText(String.valueOf(value));
                    changed = true;
                }
            } else if (mEditedId >= 5000 && mEditedId < 6000) {
                if (value >= android.os.PowerManager.BRIGHTNESS_OFF
                        && value <= valLimitHi) {
                    mKbValues[mEditedId - 5000] = value;
                    ((Button) findViewById(mEditedId)).setText(String.valueOf(value));
                    changed = true;
                }
            }
        } catch (NumberFormatException e) {
            // Ignore
        }
        if (changed) {
            mHasChanges = true;
            mSave.setEnabled(mHasChanges);
        }
    }

    private void loadData(boolean defaults) {
        if (!defaults) {
            try {
                ContentResolver cr = getContentResolver();
                mLevels = parseIntArray(Settings.System.getString(cr,
                        Settings.System.LIGHT_SENSOR_LEVELS));

                mLcdValues = parseIntArray(Settings.System.getString(cr,
                        Settings.System.LIGHT_SENSOR_LCD_VALUES));

                mBtnValues = parseIntArray(Settings.System.getString(cr,
                        Settings.System.LIGHT_SENSOR_BUTTON_VALUES));

                mKbValues = parseIntArray(Settings.System.getString(cr,
                        Settings.System.LIGHT_SENSOR_KEYBOARD_VALUES));

                // Sanity check
                int N = mLevels.length;
                if (N < 1 || mLcdValues.length != (N + 1) || mBtnValues.length != (N + 1)
                        || mKbValues.length != (N + 1)) {
                    throw new Exception("sanity check failed");
                }
            } catch (Exception e) {
                // Use defaults since we can't trust custom values
                defaults = true;
            }
        }

        if (defaults) {
            mLevels = getResources().getIntArray(
                    com.android.internal.R.array.config_autoBrightnessLevels);
            mLcdValues = getResources().getIntArray(
                    com.android.internal.R.array.config_autoBrightnessLcdBacklightValues);
            mBtnValues = getResources().getIntArray(
                    com.android.internal.R.array.config_autoBrightnessButtonBacklightValues);
            mKbValues = getResources().getIntArray(
                    com.android.internal.R.array.config_autoBrightnessKeyboardBacklightValues);
        }
        createEditor();
    }

    private int[] parseIntArray(String intArray) {
        int[] result;
        if (intArray == null || intArray.length() == 0) {
            result = new int[0];
        } else {
            String[] split = intArray.split(",");
            result = new int[split.length];
            for (int i = 0; i < split.length; i++) {
                result[i] = Integer.parseInt(split[i]);
            }
        }
        return result;
    }

    private void save() {
        // Sanity check
        boolean doSave = true;
        for (int i = 1; i < mLevels.length; i++) {
            if (mLevels[i] <= mLevels[i - 1]) {
                Toast.makeText(this, getString(R.string.ll_bad_levels), Toast.LENGTH_SHORT).show();
                doSave = false;
                break;
            }
        }
        if (doSave) {
            Settings.System.putString(getContentResolver(), Settings.System.LIGHT_SENSOR_LEVELS,
                    intArrayToString(mLevels));
            Settings.System.putString(getContentResolver(),
                    Settings.System.LIGHT_SENSOR_LCD_VALUES, intArrayToString(mLcdValues));
            Settings.System.putString(getContentResolver(),
                    Settings.System.LIGHT_SENSOR_BUTTON_VALUES, intArrayToString(mBtnValues));
            Settings.System.putString(getContentResolver(),
                    Settings.System.LIGHT_SENSOR_KEYBOARD_VALUES, intArrayToString(mKbValues));
            long tag = Settings.System.getLong(getContentResolver(),
                    Settings.System.LIGHTS_CHANGED, 0) + 1;
            Settings.System.putLong(getContentResolver(), Settings.System.LIGHTS_CHANGED, tag);

            mHasChanges = false;
            mSave.setEnabled(mHasChanges);
        }
    }

    private String intArrayToString(int[] array) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < array.length - 1; i++) {
            sb.append(array[i]);
            sb.append(",");
        }
        sb.append(array[array.length - 1]);
        return sb.toString();
    }

    private Runnable mUpdateTask = new Runnable() {
        public void run() {
            boolean autoLcd = Settings.System.getInt(getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS_MODE, 1337) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
            boolean filterEnabled = Settings.System.getInt(getContentResolver(),
                    Settings.System.LIGHT_FILTER, 0) != 0;

            try {
                IPowerManager power = IPowerManager.Stub.asInterface(ServiceManager
                        .getService("power"));
                if (filterEnabled && autoLcd) {
                    mSensor.setText(String.valueOf(power.getLightSensorValue()) + " / "
                            + String.valueOf(power.getRawLightSensorValue()));
                } else {
                    String value = String.valueOf(power.getLightSensorValue());
                    mSensor.setText(value + " / " + value);
                }
                if (autoLcd) {
                    mScreen.setText(String.valueOf(power.getLightSensorScreenBrightness()));
                } else {
                    mScreen.setText(getString(R.string.ll_disabled));
                }
                mButtons.setText(String.valueOf(power.getLightSensorButtonBrightness()));
                if (mHasKeyboard) {
                    mKeyboard.setText(String.valueOf(power.getLightSensorKeyboardBrightness()));
                }
            } catch (Exception e) {
                // Display "-" on any error

                if (autoLcd) {
                    mScreen.setText("-");
                } else {
                    mScreen.setText(getString(R.string.ll_disabled));
                }

                mSensor.setText("- / -");
                mButtons.setText("-");
                mKeyboard.setText("-");
            }

            mHandler.postDelayed(mUpdateTask, UPDATE_RATE);
        }
    };

    private void createEditor() {
        // Assume at least one defined level (two values)
        TableLayout table = (TableLayout) findViewById(R.id.ll_table_config);

        // Clear all
        while (table.getChildCount() > 1) {
            table.removeViewAt(table.getChildCount() - 1);
        }

        TableRow row;

        // First row
        row = createRow();

        // Lower
        row.addView(createTextView(0, "0"));

        // Upper
        row.addView(createTextView(1000, String.valueOf(mLevels[0] - 1)));

        // Screen
        row.addView(createButton(3000, String.valueOf(mLcdValues[0])));

        // Buttons
        row.addView(createButton(4000, String.valueOf(mBtnValues[0])));

        // Keyboard
        if (mHasKeyboard) {
            row.addView(createButton(5000, String.valueOf(mKbValues[0])));
        }

        table.addView(row, table.getChildCount());

        for (int i = 0; i < mLevels.length - 1; i++) {
            row = createRow();

            // Lower
            row.addView(createButton(2000 + i, String.valueOf(mLevels[i])));

            // Upper
            row.addView(createTextView(1000 + i + 1,
                    String.valueOf(Math.max(0, mLevels[i + 1] - 1))));

            // Screen
            row.addView(createButton(3000 + i + 1, String.valueOf(mLcdValues[i + 1])));

            // Buttons
            row.addView(createButton(4000 + i + 1, String.valueOf(mBtnValues[i + 1])));

            // Keyboard
            if (mHasKeyboard) {
                row.addView(createButton(5000 + i + 1, String.valueOf(mKbValues[i + 1])));
            }

            table.addView(row, table.getChildCount());
        }

        row = createRow();

        // Lower
        row.addView(createButton(2000 + mLevels.length - 1,
                String.valueOf(mLevels[mLevels.length - 1])));

        // Upper
        row.addView(createTextView((int) 1e10, String.valueOf((char) '\u221e')));

        // Screen
        row.addView(createButton(3000 + mLevels.length, String.valueOf(mLcdValues[mLevels.length])));

        // Buttons
        row.addView(createButton(4000 + mLevels.length, String.valueOf(mBtnValues[mLevels.length])));

        // Keyboard
        if (mHasKeyboard) {
            row.addView(createButton(5000 + mLevels.length,
                    String.valueOf(mKbValues[mLevels.length])));
        }

        table.addView(row, table.getChildCount());

        table.setColumnStretchable(0, true);
        table.setColumnStretchable(2, true);
        table.setColumnStretchable(3, true);
        if (mHasKeyboard) {
            table.setColumnStretchable(4, true);
        }
    }

    private int dp2px(int dp) {
        return (int) getResources().getDisplayMetrics().density * dp;
    }

    private Button createButton(int id, String text) {
        Button btn = new Button(this);
        btn.setId(id);
        btn.setText(text);
        btn.setMinWidth(dp2px(50));
        btn.setMaxWidth(dp2px(120));
        btn.setOnClickListener(this);
        return btn;
    }

    private TextView createTextView(int id, String text) {
        TextView tv = new TextView(this);
        tv.setGravity(Gravity.CENTER);
        tv.setText(text);
        tv.setWidth(dp2px(60));
        tv.setId(id);
        return tv;
    }

    private TableRow createRow() {
        TableRow row = new TableRow(this);
        row.setGravity(Gravity.CENTER);
        return row;
    }
}
