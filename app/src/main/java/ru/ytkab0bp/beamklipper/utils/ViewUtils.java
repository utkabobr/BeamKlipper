package ru.ytkab0bp.beamklipper.utils;

import android.animation.TimeInterpolator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.animation.PathInterpolator;

import java.util.HashMap;
import java.util.Map;

import ru.ytkab0bp.beamklipper.KlipperApp;

public class ViewUtils {
    public final static TimeInterpolator CUBIC_INTERPOLATOR = new PathInterpolator(0.25f, 0, 0.25f, 1f);
    public final static String ROBOTO_MEDIUM = "Roboto-Medium";

    private static Map<String, Typeface> typefaceCache = new HashMap<>();

    private final static Handler uiHandler = new Handler(Looper.getMainLooper());

    public static void removeCallbacks(Runnable r) {
        uiHandler.removeCallbacks(r);
    }

    public static void postOnMainThread(Runnable r) {
        uiHandler.post(r);
    }

    public static void postOnMainThread(Runnable r, long delay) {
        uiHandler.postDelayed(r, delay);
    }

    public static Handler getUiHandler() {
        return uiHandler;
    }

    public static Typeface getTypeface(String key) {
        Typeface typeface = typefaceCache.get(key);
        if (typeface == null) {
            typefaceCache.put(key, typeface = Typeface.createFromAsset(KlipperApp.INSTANCE.getAssets(), key + ".ttf"));
        }
        return typeface;
    }

    public static float lerp(float from, float to, float val) {
        return from + (to - from) * val;
    }

    public static int dp(float dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, KlipperApp.INSTANCE.getResources().getDisplayMetrics());
    }

    public static Drawable resolveDrawable(Context ctx, int attr) {
        TypedArray arr = ctx.obtainStyledAttributes(new int[] {attr});
        Drawable d = arr.getDrawable(0);
        arr.recycle();
        return d;
    }

    public static int resolveColor(Context ctx, int color) {
        TypedArray arr = ctx.obtainStyledAttributes(new int[] {color});
        int i = arr.getColor(0, 0);
        arr.recycle();
        return i;
    }

    public static RippleDrawable createRipple(int color, float radiusDp) {
        return createRipple(color, 0, radiusDp);
    }

    public static RippleDrawable createRipple(int color, int fillColor, float radiusDp) {
        if (radiusDp == -1) {
            return new RippleDrawable(ColorStateList.valueOf(color), null, null);
        }
        GradientDrawable mask = new GradientDrawable();
        mask.setColor(Color.BLACK);
        mask.setCornerRadius(dp(radiusDp));
        return new RippleDrawable(ColorStateList.valueOf(color), fillColor != 0 ? new GradientDrawable() {{
            setColor(fillColor);
            setCornerRadius(dp(radiusDp));
        }} : null, mask);
    }
}
