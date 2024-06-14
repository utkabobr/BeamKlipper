package ru.ytkab0bp.beamklipper.view;

import android.content.Context;
import android.content.res.Configuration;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.math.MathUtils;
import androidx.core.util.Consumer;
import androidx.dynamicanimation.animation.FloatValueHolder;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

public class HomeView extends FrameLayout {
    private final static boolean SETTINGS_ENABLED = true;
    private Consumer<Float> progressListener;
    private GestureDetector gestureDetector;
    private int touchSlop;

    private float startOffset;
    private float startProgress;
    private boolean isTouchDisabled, processingSwipe;
    private SpringAnimation animation;
    private float progress;

    private View scrollView;

    public HomeView(@NonNull Context context) {
        super(context);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onScroll(@Nullable MotionEvent e1, @NonNull MotionEvent e2, float distanceX, float distanceY) {
                if (!processingSwipe && !isTouchDisabled) {
                    if (progress == 0 && scrollView != null && scrollView.canScrollVertically(e1.getY() - e2.getY() > 0 ? 1 : -1)) {
                        isTouchDisabled = true;
                    } else if (animation == null && Math.abs(e2.getY() - e1.getY()) >= touchSlop && Math.abs(distanceY) >= Math.abs(distanceX) * 1.5f) {
                        startOffset = e2.getY() - e1.getY();
                        startProgress = progress;
                        processingSwipe = true;

                        MotionEvent ev = MotionEvent.obtain(e2);
                        ev.setAction(MotionEvent.ACTION_CANCEL);
                        for (int i = 0; i < getChildCount(); i++) {
                            getChildAt(i).dispatchTouchEvent(ev);
                        }
                        ev.recycle();
                    } else {
                        isTouchDisabled = true;
                    }
                }
                if (processingSwipe) {
                    progress = MathUtils.clamp(startProgress + (e2.getY() - e1.getY() - startOffset) / getHeight(), SETTINGS_ENABLED ? -1f : 0, 1f);
                    invalidateProgress();
                }
                return processingSwipe;
            }

            @Override
            public boolean onFling(@Nullable MotionEvent e1, @NonNull MotionEvent e2, float velocityX, float velocityY) {
                if (processingSwipe && Math.abs(velocityY) >= 3500) {
                    if (velocityY > 0) {
                        animateTo(progress >= 0 ? 1 : 0);
                    } else {
                        if (SETTINGS_ENABLED) {
                            animateTo(progress > 0 ? 0 : -1);
                        } else {
                            animateTo(0);
                        }
                    }
                }
                return false;
            }
        });
    }

    public void setScrollView(View scrollView) {
        this.scrollView = scrollView;
    }

    public void animateTo(float to) {
        if (progress == to) return;
        animation = new SpringAnimation(new FloatValueHolder(progress))
                .setMinimumVisibleChange(1 / 256f)
                .setSpring(new SpringForce(to)
                        .setStiffness(800f)
                        .setDampingRatio(SpringForce.DAMPING_RATIO_NO_BOUNCY))
                .addUpdateListener((animation1, value, velocity) -> {
                    progress = value;
                    invalidateProgress();
                })
                .addEndListener((animation1, canceled, value, velocity) -> animation = null);
        animation.start();
    }

    private void invalidateProgress() {
        if (progressListener != null) {
            progressListener.accept(progress);
        }
    }

    public float getProgress() {
        return progress;
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        touchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
    }

    public void setProgressListener(Consumer<Float> progressListener) {
        this.progressListener = progressListener;
        if (progressListener != null) {
            progressListener.accept(progress);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        invalidateProgress();
    }

    private void clearFlags() {
        processingSwipe = false;
        isTouchDisabled = false;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        boolean det = gestureDetector.onTouchEvent(ev);
        if (ev.getActionMasked() == MotionEvent.ACTION_UP || ev.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            if (processingSwipe) {
                if (animation == null && progress != 0 && progress != 1 && progress != -1) {
                    if (progress > 0) {
                        if (progress > 0.5f) {
                            animateTo(1f);
                        } else {
                            animateTo(0);
                        }
                    } else if (progress < 0) {
                        if (progress < -0.5f) {
                            animateTo(-1f);
                        } else {
                            animateTo(0);
                        }
                    }
                }
            }
            clearFlags();
        }
        return det || super.dispatchTouchEvent(ev) || ev.getActionMasked() == MotionEvent.ACTION_DOWN;
    }
}
