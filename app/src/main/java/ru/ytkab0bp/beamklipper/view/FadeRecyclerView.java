package ru.ytkab0bp.beamklipper.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.math.MathUtils;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import ru.ytkab0bp.beamklipper.utils.ViewUtils;

public class FadeRecyclerView extends RecyclerView {
    private final static int HEIGHT_DP = 32;

    private Paint topPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint bottomPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float topProgress, bottomProgress;
    private float overlayAlpha = 1f;

    private Bitmap bitmap;
    private Canvas bitmapCanvas;
    private boolean bitmapMode;

    public FadeRecyclerView(@NonNull Context context) {
        super(context);

        LinearLayoutManager llm = new LinearLayoutManager(context);
        setLayoutManager(llm);
        setWillNotDraw(false);
        addOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                topProgress = 1f;
                if (llm.findFirstVisibleItemPosition() == 0) {
                    View ch = llm.getChildAt(0);
                    int size = Math.min(ch.getHeight(), ViewUtils.dp(HEIGHT_DP) / 2);
                    topProgress = MathUtils.clamp(-ch.getTop() / (float) size, 0, 1);
                }
                bottomProgress = 1f;
                if (llm.findLastVisibleItemPosition() == recyclerView.getAdapter().getItemCount() - 1) {
                    View ch = llm.getChildAt(llm.getChildCount() - 1);
                    int size = Math.min(ch.getHeight(), ViewUtils.dp(HEIGHT_DP) / 2);
                    bottomProgress = MathUtils.clamp((ch.getBottom() - getHeight()) / (float) size, 0, 1);
                }
                invalidate();
            }
        });
        invalidateShaders();
    }

    /**
     * Very heavy, should be used only if transparent background is really needed
     */
    public void setBitmapMode() {
        this.bitmapMode = true;
        topPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        bottomPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        invalidateShaders();
    }

    public void setOverlayAlpha(float overlayAlpha) {
        this.overlayAlpha = overlayAlpha;
        invalidate();
    }

    @Override
    public void draw(Canvas c) {
        Canvas cv;
        if (bitmapMode) {
            if (bitmap == null || bitmap.getWidth() != getWidth() || bitmap.getHeight() != getHeight()) {
                if (bitmap != null) {
                    bitmap.recycle();
                }
                bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
                bitmapCanvas = new Canvas(bitmap);
            }
            bitmap.eraseColor(Color.TRANSPARENT);
            cv = bitmapCanvas;
            super.draw(cv);
        } else {
            super.draw(cv = c);
        }

        if (topProgress > 0) {
            cv.save();
            if (bitmapMode) {
                cv.translate(0, -ViewUtils.dp(HEIGHT_DP) * (1f - topProgress * overlayAlpha));
            } else {
                topPaint.setAlpha((int) (topProgress * overlayAlpha * 0xFF));
            }
            cv.drawRect(0, 0, getWidth(), ViewUtils.dp(HEIGHT_DP), topPaint);
            cv.restore();
        }
        if (bottomProgress > 0) {
            cv.save();
            if (bitmapMode) {
                cv.translate(0, ViewUtils.dp(HEIGHT_DP) * (1f - bottomProgress * overlayAlpha));
            } else {
                bottomPaint.setAlpha((int) (bottomProgress * overlayAlpha * 0xFF));
            }
            cv.drawRect(0, getHeight() - ViewUtils.dp(HEIGHT_DP), getWidth(), getHeight(), bottomPaint);
            cv.restore();
        }

        if (bitmapMode) {
            c.drawBitmap(bitmap, 0, 0, null);
            invalidate();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (bitmap != null) {
            bitmap.recycle();
            bitmap = null;
            bitmapCanvas = null;
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        invalidateShaders();
    }

    private void invalidateShaders() {
        if (getWidth() == 0 || getHeight() == 0) return;

        int clr = bitmapMode ? Color.BLACK : ViewUtils.resolveColor(getContext(), android.R.attr.windowBackground);
        topPaint.setShader(new LinearGradient(getWidth() / 2f, 0, getWidth() / 2f, ViewUtils.dp(HEIGHT_DP), bitmapMode ? 0 : clr, bitmapMode ? clr : 0, Shader.TileMode.CLAMP));
        bottomPaint.setShader(new LinearGradient(getWidth() / 2f, getHeight() - ViewUtils.dp(HEIGHT_DP), getWidth() / 2f, getHeight(), bitmapMode ? clr : 0, bitmapMode ? 0 : clr, Shader.TileMode.CLAMP));
        invalidate();
    }
}
