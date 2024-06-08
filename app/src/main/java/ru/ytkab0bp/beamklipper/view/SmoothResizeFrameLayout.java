package ru.ytkab0bp.beamklipper.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.dynamicanimation.animation.FloatValueHolder;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

import java.util.ArrayList;
import java.util.List;

public class SmoothResizeFrameLayout extends FrameLayout {
    private final ArrayList<View> mMatchParentChildren = new ArrayList<>(1);

    private FloatValueHolder widthValue;
    private SpringAnimation mWidthSpring;
    private FloatValueHolder heightValue;
    private SpringAnimation mHeightSpring;
    private boolean ignoreNextLayout;

    private List<View> forceNotMeasure = new ArrayList<>();

    public SmoothResizeFrameLayout(@NonNull Context context) {
        super(context);
    }

    public SmoothResizeFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public SmoothResizeFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        mWidthSpring = new SpringAnimation(widthValue = new FloatValueHolder(getWidth()))
                .setSpring(new SpringForce(getWidth())
                        .setStiffness(1000f)
                        .setDampingRatio(SpringForce.DAMPING_RATIO_NO_BOUNCY))
                .addUpdateListener((animation, value, velocity) -> invalidateSize());
        mHeightSpring = new SpringAnimation(heightValue = new FloatValueHolder(getHeight()))
                .setSpring(new SpringForce(getHeight())
                        .setStiffness(1000f)
                        .setDampingRatio(SpringForce.DAMPING_RATIO_NO_BOUNCY))
                .addUpdateListener((animation, value, velocity) -> invalidateSize());
    }

    private void invalidateSize() {
        int width = (int) widthValue.getValue();
        int height = (int) heightValue.getValue();
        if (getMeasuredWidth() == width && getMeasuredHeight() == height) return;
        setMeasuredDimension(width, height);
        requestLayout();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mWidthSpring != null) {
            mWidthSpring.cancel();
            mWidthSpring = null;
        }
        if (mHeightSpring != null) {
            mHeightSpring.cancel();
            mHeightSpring = null;
        }
    }

    public void ignoreNextLayout() {
        this.ignoreNextLayout = true;
    }

    public void addForceNotMeasure(View v) {
        forceNotMeasure.add(v);
    }

    public void removeForceNotMeasure(View v) {
        forceNotMeasure.remove(v);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int count = getChildCount();

        final boolean measureMatchParentChildren =
                MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.EXACTLY ||
                        MeasureSpec.getMode(heightMeasureSpec) != MeasureSpec.EXACTLY;
        mMatchParentChildren.clear();

        int maxHeight = 0;
        int maxWidth = 0;
        int childState = 0;

        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() != GONE && !forceNotMeasure.contains(child)) {
                measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0);
                final LayoutParams lp = (LayoutParams) child.getLayoutParams();
                maxWidth = Math.max(maxWidth,
                        child.getMeasuredWidth() + lp.leftMargin + lp.rightMargin);
                maxHeight = Math.max(maxHeight,
                        child.getMeasuredHeight() + lp.topMargin + lp.bottomMargin);
                childState = combineMeasuredStates(childState, child.getMeasuredState());
                if (measureMatchParentChildren) {
                    if (lp.width == LayoutParams.MATCH_PARENT ||
                            lp.height == LayoutParams.MATCH_PARENT) {
                        mMatchParentChildren.add(child);
                    }
                }
            }
        }

        // Account for padding too
        maxWidth += getPaddingLeft() + getPaddingRight();
        maxHeight += getPaddingTop() + getPaddingBottom();

        // Check against our minimum height and width
        maxHeight = Math.max(maxHeight, getSuggestedMinimumHeight());
        maxWidth = Math.max(maxWidth, getSuggestedMinimumWidth());

        // Check against our foreground's minimum height and width
        final Drawable drawable = getForeground();
        if (drawable != null) {
            maxHeight = Math.max(maxHeight, drawable.getMinimumHeight());
            maxWidth = Math.max(maxWidth, drawable.getMinimumWidth());
        }

//        setMeasuredDimension(resolveSizeAndState(maxWidth, widthMeasureSpec, childState),
//                resolveSizeAndState(maxHeight, heightMeasureSpec,
//                        childState << MEASURED_HEIGHT_STATE_SHIFT));

        count = mMatchParentChildren.size();
        if (count > 1) {
            for (int i = 0; i < count; i++) {
                final View child = mMatchParentChildren.get(i);
                final MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();

                final int childWidthMeasureSpec;
                if (lp.width == LayoutParams.MATCH_PARENT) {
                    final int width = Math.max(0, getMeasuredWidth()
                            - getPaddingLeft() - getPaddingRight()
                            - lp.leftMargin - lp.rightMargin);
                    childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(
                            width, MeasureSpec.EXACTLY);
                } else {
                    childWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec,
                            getPaddingLeft() + getPaddingRight() +
                                    lp.leftMargin + lp.rightMargin,
                            lp.width);
                }

                final int childHeightMeasureSpec;
                if (lp.height == LayoutParams.MATCH_PARENT) {
                    final int height = Math.max(0, getMeasuredHeight()
                            - getPaddingTop() - getPaddingBottom()
                            - lp.topMargin - lp.bottomMargin);
                    childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(
                            height, MeasureSpec.EXACTLY);
                } else {
                    childHeightMeasureSpec = getChildMeasureSpec(heightMeasureSpec,
                            getPaddingTop() + getPaddingBottom() +
                                    lp.topMargin + lp.bottomMargin,
                            lp.height);
                }

                child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
            }
        }

        int measuredWidth = MeasureSpec.getSize(resolveSizeAndState(maxWidth, widthMeasureSpec, childState)),
            measuredHeight = MeasureSpec.getSize(resolveSizeAndState(maxHeight, heightMeasureSpec, childState << MEASURED_HEIGHT_STATE_SHIFT));

        if (ignoreNextLayout && isLaidOut()) {
            ignoreNextLayout = false;
            if (mWidthSpring != null && mHeightSpring != null) {
                mWidthSpring.getSpring().setFinalPosition(measuredWidth);
                mWidthSpring.start();
                mHeightSpring.getSpring().setFinalPosition(measuredHeight);
                mHeightSpring.start();
            }
            return;
        }
        if (mWidthSpring != null && mHeightSpring != null) {
            if (mWidthSpring.getSpring().getFinalPosition() != 0) {
                mWidthSpring.getSpring().setFinalPosition(measuredWidth);
                mWidthSpring.start();
            } else {
                mWidthSpring.cancel();
                mWidthSpring.getSpring().setFinalPosition(measuredWidth);
                widthValue.setValue(measuredWidth);
            }
            if (mHeightSpring.getSpring().getFinalPosition() != 0) {
                mHeightSpring.getSpring().setFinalPosition(measuredHeight);
                mHeightSpring.start();
            } else {
                mHeightSpring.cancel();
                mHeightSpring.getSpring().setFinalPosition(measuredHeight);
                heightValue.setValue(measuredHeight);
            }
            setMeasuredDimension((int) widthValue.getValue(), (int) heightValue.getValue());
        }
    }
}
