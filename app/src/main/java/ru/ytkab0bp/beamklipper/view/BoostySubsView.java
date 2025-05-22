package ru.ytkab0bp.beamklipper.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.View;

import androidx.core.math.MathUtils;

import java.util.ArrayList;
import java.util.List;

import ru.ytkab0bp.beamklipper.R;
import ru.ytkab0bp.beamklipper.utils.ViewUtils;

public class BoostySubsView extends View {
    private TextPaint paint = new TextPaint();

    private List<String> strings = new ArrayList<>();
    private SparseArray<CharSequence> ellipsizedStrings = new SparseArray<>();
    private int index;
    private float progress;
    private long lastUpdated;
    private int firstHeight;

    private Rect rect = new Rect();

    public BoostySubsView(Context context) {
        super(context);

        paint.setTextSize(ViewUtils.dp(20));
        paint.setTypeface(ViewUtils.getTypeface(ViewUtils.ROBOTO_MEDIUM));
        updateColors();
    }

    public void setStrings(List<String> strings) {
        this.strings = strings;
        ellipsizedStrings.clear();
        index = 0;
        progress = 0;
        if (!strings.isEmpty()) {
            String str = strings.get(index);
            paint.getTextBounds(str, 0, str.length(), rect);
            firstHeight = rect.height();
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        long dt = Math.min(16, System.currentTimeMillis() - lastUpdated);
        lastUpdated = System.currentTimeMillis();
        if (!strings.isEmpty()) {
            float tY = (ViewUtils.dp(24) + firstHeight) * progress;
            canvas.save();
            canvas.translate(0, -tY);
            float halfHeight = getHeight() / 2f;
            int y = 0;

            int i = index;
            while (y <= getHeight() + tY) {
                int j = i;
                while (j < 0) j += strings.size();
                while (j >= strings.size()) j -= strings.size();

                CharSequence str = ellipsizedStrings.get(j);
                if (str == null) {
                    ellipsizedStrings.set(j, str = TextUtils.ellipsize(strings.get(j), paint, getWidth() - getPaddingLeft() - getPaddingRight(), TextUtils.TruncateAt.END));
                }

                paint.getTextBounds(str.toString(), 0, str.length(), rect);
                float highlight = (1f - Math.abs((y - tY - firstHeight / 2f - halfHeight) / halfHeight));
                highlight = MathUtils.clamp(highlight, 0, 1);
                paint.setAlpha((int) (0xFF * highlight));

                float x = (getWidth() - rect.width()) / 2f;
                canvas.drawText(str, 0, str.length(), x, y, paint);

                y += rect.height() + ViewUtils.dp(24);
                i++;
            }

            canvas.restore();

            progress += dt / 2000f;
            if (progress > 1) {
                progress -= 1f;
                index++;
                index %= strings.size();

                String str = strings.get(index);
                paint.getTextBounds(str, 0, str.length(), rect);
                firstHeight = rect.height();
            }
            invalidate();
        }
    }

    public void updateColors() {
        paint.setColor(ViewUtils.resolveColor(getContext(), R.attr.textColorOnAccent));
        invalidate();
    }
}
