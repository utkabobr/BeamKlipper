package ru.ytkab0bp.beamklipper.view.preferences;

import android.content.Context;
import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatTextView;

import ru.ytkab0bp.beamklipper.utils.ViewUtils;

public class PreferenceHeaderView extends AppCompatTextView {
    public PreferenceHeaderView(@NonNull Context context) {
        super(context);

        setPadding(ViewUtils.dp(21), ViewUtils.dp(6), ViewUtils.dp(21), 0);
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        setTypeface(ViewUtils.getTypeface(ViewUtils.ROBOTO_MEDIUM));
        setTextColor(ViewUtils.resolveColor(context, android.R.attr.colorAccent));
    }
}
