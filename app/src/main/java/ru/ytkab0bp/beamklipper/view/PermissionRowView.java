package ru.ytkab0bp.beamklipper.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatTextView;

import com.google.android.material.materialswitch.MaterialSwitch;

import ru.ytkab0bp.beamklipper.R;
import ru.ytkab0bp.beamklipper.utils.ViewUtils;

public class PermissionRowView extends LinearLayout {
    public TextView titleView;
    public MaterialSwitch mSwitch;

    private Paint dividerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private boolean divider;

    public PermissionRowView(Context context) {
        super(context);

        titleView = new AppCompatTextView(context);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        titleView.setTextColor(ViewUtils.resolveColor(context, android.R.attr.textColorPrimary));
        titleView.setGravity(Gravity.CENTER_VERTICAL);
        addView(titleView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f) {{
            setMarginEnd(ViewUtils.dp(12));
        }});

        mSwitch = new MaterialSwitch(context) {
            @Override
            public boolean dispatchTouchEvent(MotionEvent event) {
                return false;
            }
        };
        addView(mSwitch, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setPadding(ViewUtils.dp(21), ViewUtils.dp(6), ViewUtils.dp(16), ViewUtils.dp(6));
        setOrientation(HORIZONTAL);
        setWillNotDraw(false);
        setBackground(ViewUtils.resolveDrawable(context, android.R.attr.selectableItemBackground));

        dividerPaint.setColor(ViewUtils.resolveColor(context, R.attr.cardOutlineColor));
        dividerPaint.setStyle(Paint.Style.STROKE);
        dividerPaint.setStrokeWidth(ViewUtils.dp(1.5f));
    }

    public void bind(int text, boolean checked, boolean divider) {
        titleView.setText(text);
        setChecked(checked);
        this.divider = divider;
        invalidate();
    }

    public void setChecked(boolean c) {
        mSwitch.setChecked(c);
        setEnabled(!c);
    }

    public boolean isChecked() {
        return mSwitch.isChecked();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (divider) {
            canvas.drawLine(ViewUtils.dp(1.5f), getHeight() - ViewUtils.dp(1), getWidth() - ViewUtils.dp(1.5f), getHeight() - ViewUtils.dp(1), dividerPaint);
        }
    }
}
