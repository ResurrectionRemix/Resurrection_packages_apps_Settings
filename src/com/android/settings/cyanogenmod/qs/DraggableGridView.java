/*
 * Copyright (c) 2011, Animoto Inc.
 * Copyright (C) 2012-2015 The CyanogenMod Project
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
package com.android.settings.cyanogenmod.qs;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Handler;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.*;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.settings.R;

import java.util.ArrayList;
import java.util.Collections;

public class DraggableGridView extends ViewGroup implements
        View.OnTouchListener, View.OnClickListener, View.OnLongClickListener {

    public static float childRatio = .95f;
    protected int colCount, childSize, padding, dpi, scroll = 0;
    protected float lastDelta = 0;
    protected Handler handler = new Handler();
    protected int dragged = -1, lastX = -1, lastY = -1, lastTarget = -1;
    protected boolean enabled = true, touching = false, isDelete = false;
    public static int animT = 150;
    protected ArrayList<Integer> newPositions = new ArrayList<Integer>();
    protected OnRearrangeListener onRearrangeListener;
    protected OnClickListener secondaryOnClickListener;
    private OnItemClickListener onItemClickListener;

    private boolean mUseMainTiles = false;

    public DraggableGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setListeners();
        handler.removeCallbacks(updateTask);
        handler.postAtTime(updateTask, SystemClock.uptimeMillis() + 500);
        setChildrenDrawingOrderEnabled(true);
        DisplayMetrics metrics = new DisplayMetrics();
        ((Activity) context).getWindowManager().getDefaultDisplay().getMetrics(metrics);
        dpi = metrics.densityDpi;
        mUseMainTiles = Settings.Secure.getIntForUser(
                getContext().getContentResolver(), Settings.Secure.QS_USE_MAIN_TILES,
                1, UserHandle.myUserId()) == 1;
    }

    protected void setListeners() {
        setOnTouchListener(this);
        super.setOnClickListener(this);
        setOnLongClickListener(this);
    }

    @Override
    public void setOnClickListener(OnClickListener l) {
        secondaryOnClickListener = l;
    }

    protected Runnable updateTask = new Runnable() {
        public void run() {
            if (dragged != -1) {
                if (lastY < padding * 3 && scroll > 0) {
                    scroll -= 20;
                } else if (lastY > getBottom() - getTop() - (padding * 3)
                        && scroll < getMaxScroll()) {
                    scroll += 20;
                }
            } else if (lastDelta != 0 && !touching) {
                scroll += lastDelta;
                lastDelta *= .9;
                if (Math.abs(lastDelta) < .25) {
                    lastDelta = 0;
                }
            }
            clampScroll();
            onLayout(true, getLeft(), getTop(), getRight(), getBottom());
            handler.postDelayed(this, 25);
        }
    };

    @Override
    public void addView(View child, int index) {
        super.addView(child, index);
        newPositions.add(-1);
        if (onRearrangeListener != null) {
            onRearrangeListener.onChange();
        }
    }

    @Override
    public void addView(View child) {
        super.addView(child);
        newPositions.add(-1);
        if (onRearrangeListener != null) {
            onRearrangeListener.onChange();
        }
    };

    @Override
    public void removeViewAt(int index) {
        super.removeViewAt(index);
        newPositions.remove(index);
        if (onRearrangeListener != null) {
            onRearrangeListener.onChange();
        }
    };

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        // compute width of view, in dp
        float w = (r - l) / (dpi / 160f);

        // determine number of columns, at least 2
        colCount = 3;

        // determine childSize and padding, in px
        childSize = (r - l) / colCount;
        childSize = Math.round(childSize * childRatio);
        padding = ((r - l) - (childSize * colCount)) / (colCount + 1);

        for (int i = 0; i < getChildCount(); i++) {
            if (i != dragged) {
                Point xy = getCoorFromIndex(i);
                // If using main tiles and index == 0 or 1, we need to offset the tiles
                if (mUseMainTiles && i < 2) {
                    getChildAt(i).layout(xy.x+childSize/2, xy.y, (int) (xy.x + childSize*1.5),
                            xy.y + childSize);
                } else {
                    getChildAt(i).layout(xy.x, xy.y, xy.x + childSize,
                            xy.y + childSize);
                }
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Calculate the cell width dynamically
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        int availableWidth = (int) (width - getPaddingLeft()
                - getPaddingRight() - (3 - 1) * 0);
        float cellWidth = (float) Math.ceil(((float) availableWidth) / 3);

        // Update each of the children's widths accordingly to the cell width
        int N = getChildCount();
        int cellHeight = 0;
        int cursor = 0;
        for (int i = 0; i < N; ++i) {
            // Update the child's width
            View v = (View) getChildAt(i);
            if (v.getVisibility() != View.GONE) {
                ViewGroup.LayoutParams lp = (ViewGroup.LayoutParams) v
                        .getLayoutParams();
                int colSpan = 1;
                lp.width = (int) ((colSpan * cellWidth) + (colSpan - 1) * 0);

                // Measure the child
                int newWidthSpec = MeasureSpec.makeMeasureSpec(lp.width,
                        MeasureSpec.EXACTLY);
                int newHeightSpec = MeasureSpec.makeMeasureSpec(lp.height,
                        MeasureSpec.EXACTLY);
                v.measure(newWidthSpec, newHeightSpec);

                // Save the cell height
                if (cellHeight <= 0) {
                    cellHeight = height;
                }
                cursor += colSpan;
            }
        }

        // Set the measured dimensions. We always fill the tray width, but wrap
        // to the height of
        // all the tiles.
        int numRows = (int) Math.ceil((float) cursor / 3);
        int newHeight = (int) ((numRows * cellHeight) + ((numRows - 1) * 0))
                + getPaddingTop() + getPaddingBottom();
        // setMeasuredDimension(width, newHeight);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected int getChildDrawingOrder(int childCount, int i) {
        if (dragged == -1) {
            return i;
        } else if (i == childCount - 1) {
            return dragged;
        } else if (i >= dragged) {
            return i + 1;
        }
        return i;
    }

    public int getIndexFromCoor(int x, int y) {
        int row = getRowFromCoor(y + scroll);
        int col = getColFromCoor(row, x);
        // touch is between columns or rows
        if (col == -1 || row == -1) {
            return -1;
        }
        int index = 0;

        index = row * colCount + col;

        if (mUseMainTiles) {
            // If we click on (0, 2) and are using main tiles, that
            // position is empty
            if (row == 0 && col == 2) {
                return -1;
            }

            // There is one tile less from row > 0
            if (row > 0) {
                index--;
            }
        }

        if (index > getChildCount()) {
            return -1;
        }
        return index;
    }

    protected int getColFromCoor(int row, int coor) {
        coor -= padding;
        // If we are using main tiles, we have offset the click position
        if (mUseMainTiles && row == 0) {
            coor -= childSize/2;
        }
        for (int i = 0; coor > 0; i++) {
            if (coor < childSize) {
                return i;
            }
            coor -= (childSize + padding);
        }
        return -1;
    }

    protected int getRowFromCoor(int coor) {
        coor -= padding;
        for (int i = 0; coor > 0; i++) {
            if (coor < childSize) {
                return i;
            }
            coor -= (childSize + padding);
        }
        return -1;
    }


    protected int getTargetFromCoor(int x, int y) {
        if (getRowFromCoor(y + scroll) == -1) // touch is between rows
            return -1;

        int leftPos = getIndexFromCoor(x - (childSize / 4), y);
        int rightPos = getIndexFromCoor(x + (childSize / 4), y);
        if (leftPos == -1 && rightPos == -1) // touch is in the middle of
            // nowhere
            return -1;
        if (leftPos == rightPos) // touch is in the middle of a visual
            return -1;

        int target = -1;
        if (rightPos > -1) {
            target = rightPos;
        } else if (leftPos > -1) {
            target = leftPos + 1;
        }
        if (dragged < target) {
            return target - 1;
        }

        return target;
    }

    protected Point getCoorFromIndex(int index) {
        int col = index % colCount;
        int row = index / colCount;

        if (mUseMainTiles) {
            // If on (0,2) and main tiles, (0,2) -> (1,0)
            if (row == 0 && col == 2) {
                col = 0;
                row = 1;
            }
            // If on row > 0, we skipped a column
            if (index > 2) {
                col++;
            }
        }

        if (col == 3) {
            col = 0;
            row++;
        }

        return new Point(padding / 2 + (childSize + padding / 2) * col, padding
                / 2 + (childSize + padding / 2) * row - scroll);
    }

    public int getIndexOf(View child) {
        for (int i = 0; i < getChildCount(); i++) {
            if (getChildAt(i) == child) {
                return i;
            }
        }
        return -1;
    }

    // EVENT HANDLERS
    public void onClick(View view) {
        if (enabled) {
            if (secondaryOnClickListener != null) {
                secondaryOnClickListener.onClick(view);
            }
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(null,
                        getChildAt(getLastIndex()), getLastIndex(),
                        getLastIndex() / colCount);
            }
        }
    }

    void toggleAddDelete(boolean delete) {
        int resid = R.drawable.ic_menu_add_dark;
        int stringid = R.string.profiles_add;
        if (delete) {
            resid = R.drawable.ic_menu_delete;
            stringid = R.string.dialog_delete_title;
        }
        TextView addDeleteTile = ((TextView) getChildAt(getChildCount() - 1).findViewById(
                android.R.id.title));
        ImageView icon = ((ImageView) getChildAt(getChildCount() - 1).findViewById(
                android.R.id.icon));
        icon.setImageResource(resid);
        addDeleteTile.setText(stringid);
    }

    public boolean onLongClick(View view) {
        if (!enabled) {
            return false;
        }
        int index = getLastIndex();
        if (index != -1 && index != getChildCount() - 1) {
            toggleAddDelete(true);
            dragged = index;
            animateDragged();
            return true;
        }
        return false;
    }

    public boolean onTouch(View view, MotionEvent event) {
        int action = event.getAction();
        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                enabled = true;
                lastX = (int) event.getX();
                lastY = (int) event.getY();
                touching = true;
                break;
            case MotionEvent.ACTION_MOVE:
                int delta = lastY - (int) event.getY();
                if (dragged != -1) {
                    // change draw location of dragged visual
                    int x = (int) event.getX(), y = (int) event.getY();
                    int l = x - (3 * childSize / 4), t = y - (3 * childSize / 4);
                    getChildAt(dragged).layout(l, t, l + (childSize * 3 / 2),
                            t + (childSize * 3 / 2));

                    // check for new target hover
                    int target = getTargetFromCoor(x, y);
                    //Check if hovering over delete target
                    if (getIndexFromCoor(x, y) == getChildCount() - 1) {
                        getChildAt(dragged).setBackgroundColor(Color.RED);
                        isDelete = true;
                        break;
                    } else {
                        isDelete = false;
                        getChildAt(dragged).setBackgroundResource(R.drawable.card_back);
                    }
                    if (lastTarget != target && target != getChildCount() - 1) {
                        if (target != -1) {
                            animateGap(target);
                            lastTarget = target;
                        }
                    }
                } else {
                    scroll += delta;
                    clampScroll();
                    if (Math.abs(delta) > 2) {
                        enabled = false;
                    }
                    onLayout(true, getLeft(), getTop(), getRight(), getBottom());
                }
                lastX = (int) event.getX();
                lastY = (int) event.getY();
                lastDelta = delta;
                break;
            case MotionEvent.ACTION_UP:
                if (dragged != -1) {
                    toggleAddDelete(false);
                    View v = getChildAt(dragged);
                    if (lastTarget != -1 && !isDelete) {
                        reorderChildren(true);
                    } else {
                        Point xy = getCoorFromIndex(dragged);
                        if (mUseMainTiles && dragged < 2) {
                            xy.x += childSize/2;
                        }
                        v.layout(xy.x, xy.y, xy.x + childSize, xy.y + childSize);
                    }
                    v.clearAnimation();
                    if (v instanceof ImageView) {
                        ((ImageView) v).setAlpha(255);
                    }
                    if (isDelete) {
                        lastTarget = dragged;
                        removeViewAt(dragged);
                        reorderChildren(false);
                        onRearrangeListener.onChange();
                    }
                    lastTarget = -1;
                    dragged = -1;
                }
                touching = false;
                isDelete = false;
                break;
        }
        if (dragged != -1) {
            return true;
        }
        return false;
    }

    // EVENT HELPERS
    protected void animateDragged() {
        View v = getChildAt(dragged);
        int x = getCoorFromIndex(dragged).x + childSize/2, y = getCoorFromIndex(dragged).y
                + childSize/2;
        int l = x - (3 * childSize / 4), t = y - (3 * childSize / 4);
        if (mUseMainTiles && dragged < 2) {
            l += childSize/2;
        }
        v.layout(l, t, l + (childSize * 3 / 2), t + (childSize * 3 / 2));
        AnimationSet animSet = new AnimationSet(true);
        ScaleAnimation scale = new ScaleAnimation(.667f, 1, .667f, 1,
                childSize * 3 / 4, childSize * 3 / 4);
        scale.setDuration(animT);
        AlphaAnimation alpha = new AlphaAnimation(1, .5f);
        alpha.setDuration(animT);

        animSet.addAnimation(scale);
        animSet.addAnimation(alpha);
        animSet.setFillEnabled(true);
        animSet.setFillAfter(true);

        v.clearAnimation();
        v.startAnimation(animSet);
    }

    protected void animateGap(int target) {
        for (int i = 0; i < getChildCount(); i++) {
            View v = getChildAt(i);
            if (i == dragged) {
                continue;
            }
            int newPos = i;
            if (dragged < target && i >= dragged + 1 && i <= target) {
                newPos--;
            } else if (target < dragged && i >= target && i < dragged) {
                newPos++;
            }

            // animate
            int oldPos = i;
            if (newPositions.get(i) != -1) {
                oldPos = newPositions.get(i);
            }
            if (oldPos == newPos) {
                continue;
            }

            Point oldXY = getCoorFromIndex(oldPos);
            Point newXY = getCoorFromIndex(newPos);

            int offsetOld = 0;
            if (mUseMainTiles && oldPos < 2) {
                offsetOld = childSize/2;
            }
            Point oldOffset = new Point(oldXY.x +offsetOld - v.getLeft(), oldXY.y
                    - v.getTop());

            int offsetNew = 0;
            if (mUseMainTiles && newPos < 2) {
                offsetNew = childSize/2;
            }
            Point newOffset = new Point(newXY.x +offsetNew - v.getLeft(), newXY.y
                    - v.getTop());

            TranslateAnimation translate = new TranslateAnimation(
                    Animation.ABSOLUTE, oldOffset.x, Animation.ABSOLUTE,
                    newOffset.x, Animation.ABSOLUTE, oldOffset.y,
                    Animation.ABSOLUTE, newOffset.y);
            translate.setDuration(animT);
            translate.setFillEnabled(true);
            translate.setFillAfter(true);
            v.clearAnimation();
            v.startAnimation(translate);

            newPositions.set(i, newPos);
        }
    }

    protected void reorderChildren(boolean notify) {
        ArrayList<View> children = new ArrayList<View>();
        for (int i = 0; i < getChildCount(); i++) {
            getChildAt(i).clearAnimation();
            children.add(getChildAt(i));
        }
        removeAllViews();
        while (dragged != lastTarget) {
            if (lastTarget == children.size()) // dragged and dropped to the
            // right of the last element
            {
                children.add(children.remove(dragged));
                dragged = lastTarget;
            } else if (dragged < lastTarget) // shift to the right
            {
                Collections.swap(children, dragged, dragged + 1);
                dragged++;
            } else if (dragged > lastTarget) // shift to the left
            {
                Collections.swap(children, dragged, dragged - 1);
                dragged--;
            }
        }
        for (int i = 0; i < children.size(); i++) {
            newPositions.set(i, -1);
            addView(children.get(i));
        }
        onLayout(true, getLeft(), getTop(), getRight(), getBottom());

        if (onRearrangeListener != null && notify) {
            onRearrangeListener.onChange();
        }
    }

    public void scrollToTop() {
        scroll = 0;
    }

    public void scrollToBottom() {
        scroll = Integer.MAX_VALUE;
        clampScroll();
    }

    protected void clampScroll() {
        int stretch = 3, overreach = getHeight() / 2;
        int max = getMaxScroll();
        max = Math.max(max, 0);

        if (scroll < -overreach) {
            scroll = -overreach;
            lastDelta = 0;
        } else if (scroll > max + overreach) {
            scroll = max + overreach;
            lastDelta = 0;
        } else if (scroll < 0) {
            if (scroll >= -stretch) {
                scroll = 0;
            } else if (!touching) {
                scroll -= scroll / stretch;
            }
        } else if (scroll > max) {
            if (scroll <= max + stretch) {
                scroll = max;
            } else if (!touching) {
                scroll += (max - scroll) / stretch;
            }
        }
    }

    protected int getMaxScroll() {
        int rowCount = (int) Math.ceil((double) getChildCount() / colCount), max = rowCount
                * childSize + (rowCount + 1) * padding - getHeight();
        return max;
    }

    public int getLastIndex() {
        return getIndexFromCoor(lastX, lastY);
    }

    public void setOnRearrangeListener(OnRearrangeListener l) {
        this.onRearrangeListener = l;
    }

    public void setOnItemClickListener(OnItemClickListener l) {
        this.onItemClickListener = l;
    }

    public interface OnRearrangeListener {
        public abstract void onChange();
    }
}
