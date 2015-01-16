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

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Handler;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.settings.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DraggableGridView extends ViewGroup implements
        View.OnTouchListener, View.OnClickListener, View.OnLongClickListener {

    private static final float CHILD_RATIO = .95f;
    private static final int ANIM_DURATION = 150;
    private static final int COL_COUNT = 3;

    protected int mChildSize, mPadding, mLeftOffset, mScroll = 0;
    protected float mLastDelta = 0;
    protected Handler mHandler = new Handler();
    protected int mDragged = -1, mLastX = -1, mLastY = -1, mLastTarget = -1;
    protected boolean mEnabled = true, mTouching = false, mIsDelete = false;
    protected final ArrayList<Integer> mNewPositions = new ArrayList<Integer>();
    protected OnRearrangeListener mOnRearrangeListener;
    protected OnClickListener mSecondaryOnClickListener;
    private AdapterView.OnItemClickListener mOnItemClickListener;

    private boolean mUseMainTiles = false;
    private int mMaxItemCount = -1;

    protected Runnable mUpdateTask = new Runnable() {
        public void run() {
            if (mDragged != -1) {
                if (mLastY < mPadding * 3 && mScroll > 0) {
                    mScroll -= 20;
                } else if (mLastY > getBottom() - getTop() - (mPadding * 3)
                        && mScroll < getMaxScroll()) {
                    mScroll += 20;
                }
            } else if (mLastDelta != 0 && !mTouching) {
                mScroll += mLastDelta;
                mLastDelta *= .9;
                if (Math.abs(mLastDelta) < .25) {
                    mLastDelta = 0;
                }
            }
            clampScroll();
            onLayout(true, getLeft(), getTop(), getRight(), getBottom());
            mHandler.postDelayed(this, 25);
        }
    };

    public DraggableGridView(Context context, AttributeSet attrs) {
        super(context, attrs);

        setListeners();
        setChildrenDrawingOrderEnabled(true);

        mUseMainTiles = Settings.System.getInt(getContext().getContentResolver(),
                Settings.System.QS_USE_MAIN_TILES, 1) == 1;
    }

    public void setMaxItemCount(int count) {
        mMaxItemCount = count;
        updateAddDeleteState();
    }

    protected void setListeners() {
        setOnTouchListener(this);
        super.setOnClickListener(this);
        setOnLongClickListener(this);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mHandler.postDelayed(mUpdateTask, 500);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mHandler.removeCallbacks(mUpdateTask);
    }

    @Override
    public void setOnClickListener(OnClickListener l) {
        mSecondaryOnClickListener = l;
    }

    @Override
    public void addView(View child, int index) {
        super.addView(child, index);
        mNewPositions.add(-1);
        if (mOnRearrangeListener != null) {
            mOnRearrangeListener.onChange();
        }
    }

    @Override
    public void addView(View child) {
        super.addView(child);
        mNewPositions.add(-1);
        if (mOnRearrangeListener != null) {
            mOnRearrangeListener.onChange();
        }
    };

    @Override
    public void removeViewAt(int index) {
        super.removeViewAt(index);
        mNewPositions.remove(index);
        if (mOnRearrangeListener != null) {
            mOnRearrangeListener.onChange();
        }
    };

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int width = r - l;
        mPadding = Math.min((width - (mChildSize * COL_COUNT)) / (COL_COUNT + 1),
                getResources().getDimensionPixelSize(R.dimen.qs_tile_max_padding));
        mLeftOffset = (width - mChildSize * COL_COUNT - mPadding * (COL_COUNT - 1)) / 2;

        for (int i = 0; i < getChildCount(); i++) {
            if (i != mDragged) {
                Point xy = getCoordinateFromIndex(i);
                int left = xy.x;
                // If using main tiles and index == 0 or 1, we need to offset the tiles
                if (mUseMainTiles && i < (COL_COUNT - 1)) {
                    left += mChildSize / 2;
                }
                getChildAt(i).layout(left, xy.y, left + mChildSize, xy.y + mChildSize);
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int availableWidth = getMeasuredWidth() - getPaddingLeft() - getPaddingRight();
        mChildSize = Math.min(Math.round((float) availableWidth * CHILD_RATIO / COL_COUNT),
                getResources().getDimensionPixelSize(R.dimen.qs_tile_max_size));

        // Update each of the children's widths accordingly to the cell width
        int N = getChildCount();
        int childSpec = MeasureSpec.makeMeasureSpec(mChildSize, MeasureSpec.EXACTLY);
        int visibleChildren = 0;

        for (int i = 0; i < N; ++i) {
            // Update the child's width
            View v = (View) getChildAt(i);
            if (v.getVisibility() != View.GONE) {
                ViewGroup.LayoutParams lp =
                        (ViewGroup.LayoutParams) v.getLayoutParams();

                // Measure the child
                v.measure(childSpec, childSpec);
                visibleChildren++;
            }
        }
    }

    @Override
    protected int getChildDrawingOrder(int childCount, int i) {
        if (mDragged == -1) {
            return i;
        } else if (i == childCount - 1) {
            return mDragged;
        } else if (i >= mDragged) {
            return i + 1;
        }
        return i;
    }

    public int getIndexFromCoordinate(int x, int y) {
        int row = getColOrRowFromCoordinate(y + mScroll);
        int col = getColFromCoordinate(row, x);
        // touch is between columns or rows
        if (col == -1 || row == -1) {
            return -1;
        }
        int index = 0;

        index = row * COL_COUNT + col;

        if (mUseMainTiles) {
            // If we click on (0, 2) and are using main tiles, that
            // position is empty
            if (row == 0 && col == COL_COUNT - 1) {
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

    protected int getColFromCoordinate(int row, int coordinate) {
        // If we are using main tiles, we have offset the click position
        if (mUseMainTiles && row == 0) {
            coordinate -= mChildSize / 2;
        }
        return getColOrRowFromCoordinate(coordinate);
    }

    protected int getColOrRowFromCoordinate(int coordinate) {
        coordinate -= mPadding;
        for (int i = 0; coordinate > 0; i++) {
            if (coordinate < mChildSize) {
                return i;
            }
            coordinate -= mChildSize + mPadding;
        }
        return -1;
    }


    protected int getTargetFromCoordinate(int x, int y) {
        if (getColOrRowFromCoordinate(y + mScroll) == -1) {
            // touch is between rows
            return -1;
        }

        int leftPos = getIndexFromCoordinate(x - mChildSize / 4, y);
        int rightPos = getIndexFromCoordinate(x + mChildSize / 4, y);
        if (leftPos == -1 && rightPos == -1) {
            // touch is in the middle of nowhere
            return -1;
        }
        if (leftPos == rightPos) {
            // touch is in the middle of a visual
            return -1;
        }

        int target = -1;
        if (rightPos > -1) {
            target = rightPos;
        } else if (leftPos > -1) {
            target = leftPos + 1;
        }
        if (mDragged < target) {
            return target - 1;
        }

        return target;
    }

    protected Point getCoordinateFromIndex(int index) {
        int col = index % COL_COUNT;
        int row = index / COL_COUNT;

        if (mUseMainTiles) {
            // If on (0,2) and main tiles, (0,2) -> (1,0)
            if (row == 0 && col == COL_COUNT - 1) {
                col = 0;
                row = 1;
            }
            // If on row > 0, we skipped a column
            if (index >= COL_COUNT) {
                col++;
            }
        }

        if (col == COL_COUNT) {
            col = 0;
            row++;
        }

        return new Point(mLeftOffset + (mPadding / 2) * (col + 1) + mChildSize * col,
                (mPadding / 2) * (row + 1) + mChildSize * row - mScroll);
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
    @Override
    public void onClick(View view) {
        if (mEnabled) {
            if (mSecondaryOnClickListener != null) {
                mSecondaryOnClickListener.onClick(view);
            }
            if (mOnItemClickListener != null) {
                mOnItemClickListener.onItemClick(null,
                        getChildAt(getLastIndex()), getLastIndex(),
                        getLastIndex() / COL_COUNT);
            }
        }
    }

    void updateAddDeleteState() {
        boolean dragging = mDragged != -1;
        int activeTiles = getChildCount() - (dragging ? 2 : 1);
        boolean limitReached = mMaxItemCount > 0 && activeTiles >= mMaxItemCount;
        int iconResId = dragging ? R.drawable.ic_menu_delete : R.drawable.ic_menu_add_dark;
        int titleResId = dragging ? R.string.qs_action_delete :
                limitReached ? R.string.qs_action_no_more_tiles : R.string.qs_action_add;

        View tile = getChildAt(getChildCount() - 1);
        TextView title = (TextView) tile.findViewById(android.R.id.title);
        ImageView icon = (ImageView) tile.findViewById(android.R.id.icon);

        title.setText(titleResId);
        title.setEnabled(!limitReached);

        icon.setImageResource(iconResId);
        icon.setEnabled(!limitReached);
    }

    @Override
    public boolean onLongClick(View view) {
        if (!mEnabled) {
            return false;
        }
        int index = getLastIndex();
        if (index != -1 && index != getChildCount() - 1) {
            mDragged = index;
            updateAddDeleteState();
            startAnimation(animateDragging(true));
            return true;
        }
        return false;
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        int action = event.getAction();
        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                mEnabled = true;
                mLastX = (int) event.getX();
                mLastY = (int) event.getY();
                mTouching = true;
                break;
            case MotionEvent.ACTION_MOVE:
                int delta = mLastY - (int) event.getY();
                if (mDragged != -1) {
                    // change draw location of dragged visual
                    ColoringCardView draggedView = (ColoringCardView) getChildAt(mDragged);
                    int x = (int) event.getX(), y = (int) event.getY();

                    draggedView.setTranslationX(draggedView.getTranslationX() + x - mLastX);
                    draggedView.setTranslationY(draggedView.getTranslationY() + y - mLastY);

                    //Check if hovering over delete target
                    mIsDelete = getIndexFromCoordinate(x, y) == getChildCount() - 1;
                    draggedView.setColor(mIsDelete ? Color.RED : Color.TRANSPARENT);

                    // check for new target hover
                    int target = getTargetFromCoordinate(x, y);
                    if (target != -1 && mLastTarget != target && target != getChildCount() - 1) {
                        animateGap(target);
                        mLastTarget = target;
                    }
                } else {
                    mScroll += delta;
                    clampScroll();
                    if (Math.abs(delta) > 2) {
                        mEnabled = false;
                    }
                    onLayout(true, getLeft(), getTop(), getRight(), getBottom());
                }
                mLastX = (int) event.getX();
                mLastY = (int) event.getY();
                mLastDelta = delta;
                break;
            case MotionEvent.ACTION_UP:
                if (mDragged != -1) {
                    int dragged = mDragged;
                    List<Animator> animators = animateDragging(false);

                    mDragged = -1;
                    updateAddDeleteState();

                    if (mLastTarget != -1 && !mIsDelete) {
                        reorderChildren(dragged, animators);
                    } else if (mIsDelete) {
                        mLastTarget = dragged;
                        removeViewAt(dragged);
                        reorderChildren(dragged, animators);
                    } else {
                        View v = getChildAt(dragged);
                        animators.add(ObjectAnimator.ofFloat(v, "translationX",
                                    v.getTranslationX(), 0));
                        animators.add(ObjectAnimator.ofFloat(v, "translationY",
                                    v.getTranslationY(), 0));
                    }
                    updateAddDeleteState();
                    startAnimation(animators);
                    mLastTarget = -1;
                }
                mTouching = false;
                mIsDelete = false;
                break;
        }
        if (mDragged != -1) {
            return true;
        }
        return false;
    }

    // EVENT HELPERS
    protected List<Animator> animateDragging(boolean start) {
        List<Animator> animators = new ArrayList<Animator>();
        View v = getChildAt(mDragged);

        v.setPivotX(mChildSize / 2);
        v.setPivotY(mChildSize / 2);

        animators.add(ObjectAnimator.ofFloat(v, "alpha", v.getAlpha(), start ? 0.7f : 1.0f));
        animators.add(ObjectAnimator.ofFloat(v, "scaleX", v.getScaleX(), start ? 1.1f : 1.0f));
        animators.add(ObjectAnimator.ofFloat(v, "scaleY", v.getScaleY(), start ? 1.1f : 1.0f));

        float z = start ? getResources().getDimension(R.dimen.qs_tile_dragged_z) : 0f;
        animators.add(ObjectAnimator.ofFloat(v, "translationZ", v.getTranslationZ(), z));

        return animators;
    }

    protected void startAnimation(List<Animator> animators) {
        if (animators.isEmpty()) {
            return;
        }
        AnimatorSet animation = new AnimatorSet();
        animation.playTogether(animators);
        animation.setDuration(ANIM_DURATION);
        animation.start();
    }

    protected void animateGap(int target) {
        for (int i = 0; i < getChildCount(); i++) {
            if (i == mDragged) {
                continue;
            }

            View v = getChildAt(i);
            int newPos = i;
            if (mDragged < target && i >= mDragged + 1 && i <= target) {
                newPos--;
            } else if (target < mDragged && i >= target && i < mDragged) {
                newPos++;
            }

            // animate
            int oldPos = i;
            if (mNewPositions.get(i) != -1) {
                oldPos = mNewPositions.get(i);
            }
            if (oldPos == newPos) {
                continue;
            }

            Point oldXY = getCoordinateFromIndex(oldPos);
            Point newXY = getCoordinateFromIndex(newPos);

            int offsetOld = 0;
            if (mUseMainTiles && oldPos < 2) {
                offsetOld = mChildSize / 2;
            }
            Point oldOffset = new Point(oldXY.x + offsetOld - v.getLeft(),
                    oldXY.y - v.getTop());

            int offsetNew = 0;
            if (mUseMainTiles && newPos < 2) {
                offsetNew = mChildSize / 2;
            }
            Point newOffset = new Point(newXY.x + offsetNew - v.getLeft(),
                    newXY.y - v.getTop());

            Animator x = ObjectAnimator.ofFloat(v, "translationX", oldOffset.x, newOffset.x);
            Animator y = ObjectAnimator.ofFloat(v, "translationY", oldOffset.y, newOffset.y);
            startAnimation(Arrays.asList(x, y));

            mNewPositions.set(i, newPos);
        }
    }

    private static class ChildReorderInfo {
        View view;
        float lastX;
        float lastY;
    }

    protected void reorderChildren(int dragged, List<Animator> animators) {
        final ArrayList<ChildReorderInfo> children = new ArrayList<ChildReorderInfo>();

        for (int i = 0; i < getChildCount(); i++) {
            View v = getChildAt(i);
            ChildReorderInfo info = new ChildReorderInfo();
            info.view = getChildAt(i);
            info.lastX = info.view.getX();
            info.lastY = info.view.getY();
            info.view.setTranslationX(0);
            info.view.setTranslationY(0);
            children.add(info);
        }

        removeAllViews();
        while (dragged != mLastTarget) {
            if (mLastTarget == children.size()) {
                // dragged and dropped to the right of the last element
                children.add(children.remove(dragged));
                dragged = mLastTarget;
            } else if (dragged < mLastTarget) {
                // shift to the right
                Collections.swap(children, dragged, dragged + 1);
                dragged++;
            } else if (dragged > mLastTarget) {
                // shift to the left
                Collections.swap(children, dragged, dragged - 1);
                dragged--;
            }
        }
        for (int i = 0; i < children.size(); i++) {
            mNewPositions.set(i, -1);
            addView(children.get(i).view);
        }
        onLayout(true, getLeft(), getTop(), getRight(), getBottom());

        for (int i = 0; i < children.size(); i++) {
            ChildReorderInfo info = children.get(i);
            if (info.view.getLeft() != info.lastX) {
                animators.add(ObjectAnimator.ofFloat(info.view,
                            "translationX", info.lastX - info.view.getLeft(), 0));
            }
            if (info.view.getTop() != info.lastY) {
                animators.add(ObjectAnimator.ofFloat(info.view,
                            "translationY", info.lastY - info.view.getTop(), 0));
            }
        }

        if (mOnRearrangeListener != null) {
            mOnRearrangeListener.onChange();
        }
    }

    public void scrollToTop() {
        mScroll = 0;
    }

    public void scrollToBottom() {
        mScroll = Integer.MAX_VALUE;
        clampScroll();
    }

    protected void clampScroll() {
        int stretch = 3, overreach = getHeight() / 10;
        int max = getMaxScroll();
        max = Math.max(max, 0);

        if (mScroll < -overreach) {
            mScroll = -overreach;
            mLastDelta = 0;
        } else if (mScroll > max + overreach) {
            mScroll = max + overreach;
            mLastDelta = 0;
        } else if (mScroll < 0) {
            if (mScroll >= -stretch) {
                mScroll = 0;
            } else if (!mTouching) {
                mScroll -= mScroll / stretch;
            }
        } else if (mScroll > max) {
            if (mScroll <= max + stretch) {
                mScroll = max;
            } else if (!mTouching) {
                mScroll += (max - mScroll) / stretch;
            }
        }
    }

    protected int getMaxScroll() {
        int childCount = getChildCount();
        if (childCount >= COL_COUNT && mUseMainTiles) {
            childCount++;
        }
        int rowCount = (childCount + COL_COUNT - 1 /* round up */) / COL_COUNT;
        return rowCount * mChildSize + (rowCount + 1) * mPadding - getHeight();
    }

    public int getLastIndex() {
        return getIndexFromCoordinate(mLastX, mLastY);
    }

    public void setOnRearrangeListener(OnRearrangeListener l) {
        mOnRearrangeListener = l;
    }

    public void setOnItemClickListener(AdapterView.OnItemClickListener l) {
        mOnItemClickListener = l;
    }

    public interface OnRearrangeListener {
        public abstract void onChange();
    }
}
