package ru.ytkab0bp.beamklipper.view;

import android.content.Context;
import android.util.TypedValue;
import android.view.Gravity;

import androidx.appcompat.widget.AppCompatTextView;

import ru.ytkab0bp.beamklipper.R;
import ru.ytkab0bp.beamklipper.utils.ViewUtils;

public class BeamButton extends AppCompatTextView {
    private int colorRes = android.R.attr.colorAccent;
    private int color;

    public BeamButton(Context context) {
        super(context);
        setGravity(Gravity.CENTER);
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        setTypeface(ViewUtils.getTypeface(ViewUtils.ROBOTO_MEDIUM));
        setPadding(ViewUtils.dp(21), 0, ViewUtils.dp(21), 0);
        onApplyTheme();
    }

    public void setColor(int color) {
        this.color = color;
        this.colorRes = 0;
        onApplyTheme();
    }

    public void setColorRes(int colorRes) {
        this.colorRes = colorRes;
        onApplyTheme();
    }

    public void onApplyTheme() {
        setBackground(ViewUtils.createRipple(ViewUtils.resolveColor(getContext(), android.R.attr.colorControlHighlight), colorRes != 0 ? ViewUtils.resolveColor(getContext(), colorRes) : color, 16));
        setTextColor(ViewUtils.resolveColor(getContext(), R.attr.textColorOnAccent));
    }
}
