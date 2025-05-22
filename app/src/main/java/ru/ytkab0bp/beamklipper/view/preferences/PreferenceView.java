package ru.ytkab0bp.beamklipper.view.preferences;

import android.content.Context;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import ru.ytkab0bp.beamklipper.utils.ViewUtils;

public class PreferenceView extends LinearLayout {
    private TextView title;
    private TextView subtitle;

    public PreferenceView(Context context) {
        super(context);
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setBackground(ViewUtils.resolveDrawable(context, android.R.attr.selectableItemBackground));
        setPadding(ViewUtils.dp(21), ViewUtils.dp(16), ViewUtils.dp(16), ViewUtils.dp(16));
        setMinimumHeight(ViewUtils.dp(52));
        setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        title = new TextView(context);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        title.setTextColor(ViewUtils.resolveColor(context, android.R.attr.textColorPrimary));
        addView(title);

        subtitle = new TextView(context);
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        subtitle.setTextColor(ViewUtils.resolveColor(context, android.R.attr.textColorSecondary));
        addView(subtitle);
    }

    public void bind(String title, String subtitle) {
        this.title.setText(title);
        if (TextUtils.isEmpty(subtitle)) {
            this.subtitle.setVisibility(GONE);
        } else {
            this.subtitle.setText(subtitle);
            this.subtitle.setVisibility(VISIBLE);
        }
    }
}
