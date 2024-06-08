package ru.ytkab0bp.beamklipper.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.AttrRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import ru.ytkab0bp.beamklipper.R;
import ru.ytkab0bp.beamklipper.utils.ViewUtils;

public class RefBadgeView extends LinearLayout {
    private ImageView icon;
    private TextView title;
    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint outlinePaint;
    private float progress;

    public RefBadgeView(Context context) {
        super(context);

        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);

        icon = new ImageView(context);
        icon.setColorFilter(Color.WHITE);
        icon.setLayoutParams(new LinearLayout.LayoutParams(ViewUtils.dp(22 + 18), ViewUtils.dp(22 + 18)));
        icon.setPadding(ViewUtils.dp(9), ViewUtils.dp(9), ViewUtils.dp(9), ViewUtils.dp(9));
        addView(icon);

        title = new TextView(context);
        title.setMaxLines(1);
        title.setEllipsize(TextUtils.TruncateAt.END);
        title.setGravity(Gravity.CENTER);
        title.setTypeface(ViewUtils.getTypeface(ViewUtils.ROBOTO_MEDIUM));
        title.setTextColor(Color.WHITE);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f) {{
            setMarginStart(ViewUtils.dp(8));
            setMarginEnd(ViewUtils.dp(22 + 9));
        }});

        setBackground(ViewUtils.createRipple(ViewUtils.resolveColor(context, android.R.attr.colorControlHighlight), 0));
        setWillNotDraw(false);
        setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewUtils.dp(22 + 18)) {{
            leftMargin = rightMargin = ViewUtils.dp(9);
        }});
    }

    public ImageView getIcon() {
        return icon;
    }

    private Path path = new Path();
    @Override
    public void draw(@NonNull Canvas canvas) {
        path.rewind();
        path.addRoundRect(0, 0, ViewUtils.lerp(getHeight(), getWidth(), progress), getHeight(), getHeight() / 2f, getHeight() / 2f, Path.Direction.CW);
        canvas.save();
        canvas.clipPath(path);

        if (outlinePaint == null) {
            canvas.drawPaint(paint);
        }

        super.draw(canvas);
        canvas.restore();

        if (outlinePaint != null) {
            float stroke = outlinePaint.getStrokeWidth();
            canvas.drawRoundRect(stroke, stroke, ViewUtils.lerp(getHeight(), getWidth(), progress) - stroke, getHeight() - stroke, getHeight() / 2f, getHeight() / 2f, outlinePaint);
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if ((ev.getActionMasked() == MotionEvent.ACTION_DOWN || ev.getActionMasked() == MotionEvent.ACTION_MOVE) && ev.getX() > ViewUtils.lerp(getHeight(), getWidth(), progress)) {
            return false;
        }
        return super.dispatchTouchEvent(ev);
    }

    public void setColored() {
        icon.setColorFilter(null);
        outlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        outlinePaint.setStyle(Paint.Style.STROKE);
        outlinePaint.setStrokeWidth(ViewUtils.dp(1.5f));
        outlinePaint.setColor(ViewUtils.resolveColor(getContext(), R.attr.cardOutlineColor));
        title.setTextColor(ViewUtils.resolveColor(getContext(), android.R.attr.textColorPrimary));
    }

    public void setIcon(@DrawableRes int i, @AttrRes int bgColor, @StringRes int titleRes) {
        icon.setImageResource(i);
        if (bgColor != 0) {
            paint.setColor(ViewUtils.resolveColor(getContext(), bgColor));
        } else {
            setColored();
        }
        title.setText(titleRes);
    }

    public void setProgress(float v) {
        this.progress = v;
        title.setAlpha(v);
        invalidate();
    }
}
