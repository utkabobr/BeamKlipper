package ru.ytkab0bp.beamklipper.view;

import android.content.Context;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import ru.ytkab0bp.beamklipper.utils.ViewUtils;

public class EditTextRowView extends LinearLayout {
    private TextView value;
    private TextView title;

    public EditTextRowView(Context context) {
        super(context);

        value = new TextView(context);
        value.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        value.setTextColor(ViewUtils.resolveColor(context, android.R.attr.textColorPrimary));
        addView(value, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        title = new TextView(context);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        title.setTextColor(ViewUtils.resolveColor(context, android.R.attr.textColorSecondary));
        addView(title, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER);
        setBackground(ViewUtils.resolveDrawable(context, android.R.attr.selectableItemBackground));
        setPadding(ViewUtils.dp(21), ViewUtils.dp(12), ViewUtils.dp(21), ViewUtils.dp(12));
        setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setMinimumHeight(ViewUtils.dp(52));
    }

    public CharSequence getText() {
        return title.getVisibility() == GONE ? null : value.getText();
    }

    public void bind(int title, String value) {
        if (TextUtils.isEmpty(value)) {
            this.value.setText(title);
            this.title.setVisibility(GONE);
        } else {
            this.title.setText(title);
            this.value.setText(value);
            this.title.setVisibility(VISIBLE);
        }
    }
}
