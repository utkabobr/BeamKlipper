package ru.ytkab0bp.beamklipper.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;
import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.FloatValueHolder;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

import ru.ytkab0bp.beamklipper.R;
import ru.ytkab0bp.beamklipper.utils.ViewUtils;

public class StartStopButton extends AppCompatImageView {
    private final static int DEFAULT_RADIUS = 30, MIN_RADIUS = 14;

    private Paint backgroundPaint = new Paint();
    private Path path = new Path();
    private float progress;

    private boolean wasStopped = true;
    private SpringAnimation spring;
    private Drawable mDrawable;
    private ColorFilter mFilter;

    public StartStopButton(@NonNull Context context) {
        this(context, null);
    }

    public StartStopButton(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        setImageResource(R.drawable.ic_play_28);
        setWillNotDraw(false);

        setBackground(ViewUtils.createRipple(ViewUtils.resolveColor(context, android.R.attr.colorControlHighlight), MIN_RADIUS));
        setColorFilter(ViewUtils.resolveColor(context, R.attr.startStopButtonForegroundColor));
        invalidate();
    }

    public void setColorIndex(int i) {
        i = Math.abs(i);
        switch (i) {
            default:
            case 0:
                backgroundPaint.setColor(ViewUtils.resolveColor(getContext(), R.attr.startStopButtonColor_0));
                break;
            case 1:
                backgroundPaint.setColor(ViewUtils.resolveColor(getContext(), R.attr.startStopButtonColor_1));
                break;
            case 2:
                backgroundPaint.setColor(ViewUtils.resolveColor(getContext(), R.attr.startStopButtonColor_2));
                break;
            case 3:
                backgroundPaint.setColor(ViewUtils.resolveColor(getContext(), R.attr.startStopButtonColor_3));
                break;
            case 4:
                backgroundPaint.setColor(ViewUtils.resolveColor(getContext(), R.attr.startStopButtonColor_4));
                break;
            case 5:
                backgroundPaint.setColor(ViewUtils.resolveColor(getContext(), R.attr.startStopButtonColor_5));
                break;
            case 6:
                backgroundPaint.setColor(ViewUtils.resolveColor(getContext(), R.attr.startStopButtonColor_6));
                break;
            case 7:
                backgroundPaint.setColor(ViewUtils.resolveColor(getContext(), R.attr.startStopButtonColor_7));
                break;
            case 8:
                backgroundPaint.setColor(ViewUtils.resolveColor(getContext(), R.attr.startStopButtonColor_8));
                break;
            case 9:
                backgroundPaint.setColor(ViewUtils.resolveColor(getContext(), R.attr.startStopButtonColor_9));
                break;
        }
        invalidate();
    }

    @Override
    public void draw(Canvas canvas) {
        path.rewind();
        float rad = ViewUtils.dp(ViewUtils.lerp(DEFAULT_RADIUS, MIN_RADIUS, progress));
        path.addRoundRect(0, 0, getWidth(), getHeight(), rad, rad, Path.Direction.CW);
        canvas.save();
        canvas.clipPath(path);
        canvas.drawPaint(backgroundPaint);

        canvas.save();
        float sc = progress < 0.5f ? 1f - progress : progress;
        canvas.scale(sc, sc, getWidth() / 2f, getHeight() / 2f);
        mDrawable.setBounds(getPaddingLeft(), getPaddingTop(), getWidth() - getPaddingRight(), getHeight() - getPaddingBottom());
        mDrawable.draw(canvas);
        canvas.restore();

        super.draw(canvas);
        canvas.restore();
    }

    @Override
    public void setImageResource(int resId) {
        mDrawable = ContextCompat.getDrawable(getContext(), resId);
        mDrawable.setColorFilter(mFilter);
        invalidate();
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        mFilter = cf;
        if (mDrawable != null) {
            mDrawable.setColorFilter(cf);
        }
    }

    public void setStopped(boolean stopped) {
        if (wasStopped == stopped) {
            return;
        }
        float current = wasStopped ? 0 : 1;
        wasStopped = stopped;
        if (spring != null) {
            spring.cancel();
        }

        spring = new SpringAnimation(new FloatValueHolder(current))
                .setMinimumVisibleChange(1 / 256f)
                .setSpring(new SpringForce(stopped ? 0 : 1)
                        .setStiffness(900)
                        .setDampingRatio(SpringForce.DAMPING_RATIO_NO_BOUNCY))
                .addUpdateListener(new DynamicAnimation.OnAnimationUpdateListener() {
                    boolean check = false;

                    @Override
                    public void onAnimationUpdate(DynamicAnimation animation, float value, float velocity) {
                        if (!check && (stopped && value < 0.5f || !stopped && value > 0.5f)) {
                            check = true;
                            setImageResource(stopped ? R.drawable.ic_play_28 : R.drawable.ic_stop_24);
                        }
                        setProgress(value);
                    }
                })
                .addEndListener((animation, canceled, value, velocity) -> StartStopButton.this.spring = null);
        spring.start();
    }

    public void setProgress(float progress) {
        this.progress = progress;
        invalidate();
    }
}
