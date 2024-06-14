package ru.ytkab0bp.beamklipper.view.preferences;

import android.content.Context;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import ru.ytkab0bp.beamklipper.utils.ViewUtils;

public class PreferenceValueView extends LinearLayout {
    private TextView title;
    private TextView value;

    public PreferenceValueView(Context context) {
        super(context);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setBackground(ViewUtils.resolveDrawable(context, android.R.attr.selectableItemBackground));
        setPadding(ViewUtils.dp(21), ViewUtils.dp(6), ViewUtils.dp(16), ViewUtils.dp(6));
        setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewUtils.dp(64)));

        title = new TextView(context);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        title.setTextColor(ViewUtils.resolveColor(context, android.R.attr.textColorPrimary));
        addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        value = new TextView(context);
        value.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        value.setTextColor(ViewUtils.resolveColor(context, android.R.attr.colorAccent));
        value.setTypeface(ViewUtils.getTypeface(ViewUtils.ROBOTO_MEDIUM));
        addView(value, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT) {{
            setMarginStart(ViewUtils.dp(8));
        }});
    }

    public void bind(String title, String value) {
        this.title.setText(title);
        this.value.setText(value);
    }
}
