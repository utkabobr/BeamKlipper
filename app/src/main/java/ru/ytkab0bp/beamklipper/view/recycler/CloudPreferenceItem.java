package ru.ytkab0bp.beamklipper.view.recycler;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import ru.ytkab0bp.beamklipper.KlipperApp;
import ru.ytkab0bp.beamklipper.utils.ViewUtils;
import ru.ytkab0bp.beamklipper.view.SimpleRecyclerItem;

public class CloudPreferenceItem extends SimpleRecyclerItem<CloudPreferenceItem.PreferenceHolderView> {
    private Drawable mIcon;
    private CharSequence mTitle;
    private ValueProvider mSubtitle;
    private View.OnClickListener onClickListener;
    private View.OnLongClickListener onLongClickListener;
    private int textColorRes;
    private boolean noTint;
    private ValueProvider valueProvider;
    private float roundRadius;
    private int mPaddings = ViewUtils.dp(12);
    private boolean mForceDark;

    public CloudPreferenceItem setTitle(CharSequence title) {
        mTitle = title;
        return this;
    }

    public CloudPreferenceItem setSubtitle(CharSequence subtitle) {
        mSubtitle = ()->subtitle;
        return this;
    }

    public CloudPreferenceItem setPaddings(int paddings) {
        this.mPaddings = paddings;
        return this;
    }

    public CloudPreferenceItem setForceDark(boolean mForceDark) {
        this.mForceDark = mForceDark;
        return this;
    }

    public CloudPreferenceItem setSubtitleProvider(ValueProvider mSubtitle) {
        this.mSubtitle = mSubtitle;
        return this;
    }

    public CloudPreferenceItem setValueProvider(ValueProvider valueProvider) {
        this.valueProvider = valueProvider;
        return this;
    }

    public CloudPreferenceItem setValue(String text) {
        this.valueProvider = () -> text;
        return this;
    }

    public CloudPreferenceItem setIcon(int iconRes) {
        mIcon = ContextCompat.getDrawable(KlipperApp.INSTANCE, iconRes);
        return this;
    }

    public CloudPreferenceItem setIcon(Drawable drawable) {
        mIcon = drawable;
        return this;
    }

    public CloudPreferenceItem setNoTint(boolean noTint) {
        this.noTint = noTint;
        return this;
    }

    public CloudPreferenceItem setRoundRadius(float roundRadius) {
        this.roundRadius = roundRadius;
        return this;
    }

    public CloudPreferenceItem setTextColorRes(int textColorRes) {
        this.textColorRes = textColorRes;
        return this;
    }

    public CloudPreferenceItem setOnClickListener(View.OnClickListener onClickListener) {
        this.onClickListener = onClickListener;
        return this;
    }

    public CloudPreferenceItem setOnLongClickListener(View.OnLongClickListener onLongClickListener) {
        this.onLongClickListener = onLongClickListener;
        return this;
    }

    @Override
    public PreferenceHolderView onCreateView(Context ctx) {
        return new PreferenceHolderView(ctx);
    }

    @Override
    public void onBindView(PreferenceHolderView view) {
        view.bind(this);
    }

    public final static class PreferenceHolderView extends LinearLayout {
        private TextView title, subtitle;
        private ImageView icon;
        private TextView value;
        private float radius;

        private CloudPreferenceItem item;

        public PreferenceHolderView(Context context) {
            super(context);

            setOrientation(HORIZONTAL);
            setGravity(Gravity.CENTER_VERTICAL);

            icon = new AppCompatImageView(context) {
                private Path path = new Path();

                @Override
                public void draw(@NonNull Canvas canvas) {
                    if (radius != 0) {
                        canvas.save();
                        path.rewind();
                        path.addRoundRect(0, 0, getWidth(), getHeight(), radius, radius, Path.Direction.CW);
                        canvas.clipPath(path);
                    }
                    super.draw(canvas);
                    if (radius != 0) {
                        canvas.restore();
                    }
                }
            };
            icon.setLayoutParams(new LayoutParams(ViewUtils.dp(28), ViewUtils.dp(28)) {{
                setMarginStart(ViewUtils.dp(4));
                setMarginEnd(ViewUtils.dp(8));
            }});
            addView(icon);

            LinearLayout innerLayout = new LinearLayout(context);
            innerLayout.setOrientation(VERTICAL);
            innerLayout.setGravity(Gravity.CENTER_VERTICAL);

            title = new TextView(context);
            title.setEllipsize(TextUtils.TruncateAt.END);
            title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            innerLayout.addView(title);

            subtitle = new TextView(context);
            subtitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
            innerLayout.addView(subtitle);

            addView(innerLayout, new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f) {{
                setMarginStart(ViewUtils.dp(8));
                setMarginEnd(ViewUtils.dp(8));
            }});

            value = new TextView(context);
            value.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
            value.setPadding(ViewUtils.dp(8), ViewUtils.dp(6), ViewUtils.dp(8), ViewUtils.dp(6));
            value.setVisibility(GONE);
            addView(value, new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            setMinimumHeight(ViewUtils.dp(56));
            setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            onApplyTheme();
        }

        void bind(CloudPreferenceItem item) {
            this.item = item;
            setPadding(item.mPaddings, item.mPaddings, item.mPaddings, item.mPaddings);
            title.setText(item.mTitle);
            title.setVisibility(TextUtils.isEmpty(item.mTitle) ? GONE : VISIBLE);

            CharSequence sub = item.mSubtitle != null ? item.mSubtitle.provide() : null;
            subtitle.setText(sub);
            subtitle.setVisibility(TextUtils.isEmpty(sub) ? GONE : VISIBLE);

            CharSequence v = item.valueProvider != null ? item.valueProvider.provide() : null;
            value.setText(v);
            value.setVisibility(TextUtils.isEmpty(v) ? GONE : VISIBLE);

            if (item.mIcon != null) {
                icon.setVisibility(VISIBLE);
                icon.setImageDrawable(item.mIcon);
            } else {
                icon.setVisibility(GONE);
            }
            if (item.onClickListener != null) {
                setOnClickListener(item.onClickListener);
            } else {
                setClickable(false);
            }
            setOnLongClickListener(item.onLongClickListener);

            if (item.textColorRes != 0) {
                title.setTextColor(ViewUtils.resolveColor(KlipperApp.INSTANCE, item.textColorRes));
            }

            if (item.textColorRes != 0 || item.mIcon != null) {
                title.setTypeface(ViewUtils.getTypeface(ViewUtils.ROBOTO_MEDIUM));
            } else {
                title.setTypeface(Typeface.DEFAULT);
            }

            if (item.noTint) {
                icon.setImageTintList(null);
            } else {
                icon.setImageTintList(ColorStateList.valueOf(ViewUtils.resolveColor(getContext(), item.textColorRes != 0 ? item.textColorRes : android.R.attr.textColorSecondary)));
            }
            radius = item.roundRadius;
            icon.invalidate();

            ViewGroup.LayoutParams params = icon.getLayoutParams();
            params.width = params.height = radius != 0 ? ViewUtils.dp(42) : ViewUtils.dp(28);
            if (item.mForceDark) {
                onApplyTheme();
            }
        }

        public void onApplyTheme() {
            boolean dark = item != null && item.mForceDark;
            title.setTextColor(dark ? Color.WHITE : ViewUtils.resolveColor(getContext(), android.R.attr.textColorPrimary));
            subtitle.setTextColor(dark ? 0x99FFFFFF : ViewUtils.resolveColor(getContext(), android.R.attr.textColorSecondary));
            value.setTextColor(dark ? 0x99FFFFFF : ViewUtils.resolveColor(getContext(), android.R.attr.textColorSecondary));
            icon.setImageTintList(ColorStateList.valueOf(dark ? 0x99FFFFFF : ViewUtils.resolveColor(getContext(), android.R.attr.textColorSecondary)));
            setBackground(ViewUtils.createRipple(dark ? 0x21FFFFFF : ViewUtils.resolveColor(getContext(), android.R.attr.colorControlHighlight), 16));
        }
    }

    public interface ValueProvider {
        CharSequence provide();
    }
}
