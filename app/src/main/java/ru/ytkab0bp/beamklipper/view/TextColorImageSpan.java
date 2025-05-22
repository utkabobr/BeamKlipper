package ru.ytkab0bp.beamklipper.view;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.style.ImageSpan;

import androidx.annotation.NonNull;

public class TextColorImageSpan extends ImageSpan {
    private float offsetY;

    public TextColorImageSpan(@NonNull Drawable drawable, float offset) {
        super(drawable, ALIGN_BASELINE);
        this.offsetY = offset;
    }

    @Override
    public void draw(@NonNull Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, @NonNull Paint paint) {
        getDrawable().setTint(paint.getColor());
        canvas.save();
        canvas.translate(0, offsetY);
        super.draw(canvas, text, start, end, x, top, y, bottom, paint);
        canvas.restore();
    }
}
