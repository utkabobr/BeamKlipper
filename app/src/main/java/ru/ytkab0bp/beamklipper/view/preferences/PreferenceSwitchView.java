package ru.ytkab0bp.beamklipper.view.preferences;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.materialswitch.MaterialSwitch;

import ru.ytkab0bp.beamklipper.utils.ViewUtils;

public class PreferenceSwitchView extends LinearLayout {
    private TextView title;
    private TextView subtitle;
    private MaterialSwitch mSwitch;

    public PreferenceSwitchView(Context context) {
        super(context);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setMinimumHeight(ViewUtils.dp(52));
        setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setPadding(ViewUtils.dp(21), ViewUtils.dp(6), ViewUtils.dp(16), ViewUtils.dp(6));
        setBackground(ViewUtils.resolveDrawable(context, android.R.attr.selectableItemBackground));

        LinearLayout ll = new LinearLayout(context);
        ll.setOrientation(VERTICAL);

        title = new TextView(context);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        title.setTextColor(ViewUtils.resolveColor(context, android.R.attr.textColorPrimary));
        ll.addView(title);

        subtitle = new TextView(context);
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        subtitle.setTextColor(ViewUtils.resolveColor(context, android.R.attr.textColorSecondary));
        ll.addView(subtitle);

        addView(ll, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        mSwitch = new MaterialSwitch(context) {
            @Override
            public boolean dispatchTouchEvent(MotionEvent event) {
                return false;
            }
        };
        addView(mSwitch, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    public void setChecked(boolean checked) {
        mSwitch.setChecked(checked);
    }

    public boolean isChecked() {
        return mSwitch.isChecked();
    }

    public void bind(String title, String subtitle, boolean checked) {
        this.title.setText(title);
        if (TextUtils.isEmpty(subtitle)) {
            this.subtitle.setVisibility(GONE);
        } else {
            this.subtitle.setText(subtitle);
            this.subtitle.setVisibility(VISIBLE);
        }
        mSwitch.setChecked(checked);
        mSwitch.setTrackTintList(new ColorStateList(new int[][] {
            {android.R.attr.state_checked},
            {-android.R.attr.state_checked}
        }, new int[] {
            ViewUtils.resolveColor(getContext(), android.R.attr.colorAccent),
            0xFF7F7F7F
        }));

        mSwitch.setThumbTintList(new ColorStateList(new int[][] {
                {android.R.attr.state_checked},
                {-android.R.attr.state_checked}
        }, new int[] {
                Color.WHITE,
                0x44FFFFFF
        }));
    }
}
