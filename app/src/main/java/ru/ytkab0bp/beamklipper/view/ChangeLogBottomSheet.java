package ru.ytkab0bp.beamklipper.view;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Scroller;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import ru.ytkab0bp.beamklipper.BeamServerData;
import ru.ytkab0bp.beamklipper.KlipperApp;
import ru.ytkab0bp.beamklipper.R;
import ru.ytkab0bp.beamklipper.events.BeamServerDataUpdatedEvent;
import ru.ytkab0bp.beamklipper.utils.ViewUtils;
import ru.ytkab0bp.eventbus.EventHandler;

public class ChangeLogBottomSheet extends BottomSheetDialog {
    private BoostySubsView subsView;
    private ScrollView scrollView;
    private ViewPager pager;

    public ChangeLogBottomSheet(@NonNull Context context) {
        super(context);

        LinearLayout ll = new LinearLayout(context);
        ll.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable gd = new GradientDrawable();
        gd.setCornerRadii(new float[] {
                ViewUtils.dp(28), ViewUtils.dp(28),
                ViewUtils.dp(28), ViewUtils.dp(28),
                0, 0,
                0, 0
        });
        gd.setColor(ViewUtils.resolveColor(context, android.R.attr.windowBackground));
        ll.setBackground(gd);
        ll.setPadding(0, ViewUtils.dp(12), 0, ViewUtils.dp(12));

        FrameLayout fl = new FrameLayout(context);
        TextView titleA = new TextView(context);
        titleA.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        titleA.setTypeface(ViewUtils.getTypeface(ViewUtils.ROBOTO_MEDIUM));
        titleA.setText(R.string.Changelog);
        titleA.setTextColor(ViewUtils.resolveColor(context, android.R.attr.textColorPrimary));
        titleA.setGravity(Gravity.CENTER);
        titleA.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT) {{
            leftMargin = rightMargin = ViewUtils.dp(21);
        }});
        fl.addView(titleA);

        TextView titleB = new TextView(context);
        titleB.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        titleB.setTypeface(ViewUtils.getTypeface(ViewUtils.ROBOTO_MEDIUM));
        titleB.setText(R.string.ChangelogBoosty);
        titleB.setTextColor(ViewUtils.resolveColor(context, R.attr.textColorOnAccent));
        titleB.setGravity(Gravity.CENTER);
        titleB.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT) {{
            leftMargin = rightMargin = ViewUtils.dp(21);
        }});
        titleB.setAlpha(0f);
        fl.addView(titleB);

        ll.addView(fl);

        scrollView = new ScrollView(context);
        TextView text = new TextView(context);
        text.setTextColor(ViewUtils.resolveColor(context, android.R.attr.textColorPrimary));
        text.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        text.setPadding(ViewUtils.dp(16), ViewUtils.dp(12), ViewUtils.dp(16), ViewUtils.dp(12));

        try {
            InputStream in = getContext().getAssets().open("update.json");
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buffer = new byte[10240]; int c;
            while ((c = in.read(buffer)) != -1) {
                bos.write(buffer, 0, c);
            }
            bos.close();
            in.close();

            JSONObject obj = new JSONObject(bos.toString());
            String code = Locale.getDefault().getLanguage();
            if (obj.has(code)) {
                text.setText(obj.getString(code));
            } else {
                text.setText(obj.getString("en"));
            }
        } catch (Exception e) {
            Log.e("Changelog", "Failed to open update file", e);
        }
        scrollView.addView(text);

        DisplayMetrics dm = context.getResources().getDisplayMetrics();

        pager = new ViewPager(context) {{
            try {
                Field scroller = ViewPager.class.getDeclaredField("mScroller");
                scroller.setAccessible(true);

                Scroller mScroller = new Scroller(getContext(), ViewUtils.CUBIC_INTERPOLATOR::getInterpolation);
                scroller.set(this, mScroller);
            } catch (Exception ignored) {}
        }};
        pager.setAdapter(new PagerAdapter() {
            @Override
            public int getCount() {
                return BeamServerData.isBoostyAvailable() ? 2 : 1;
            }

            @NonNull
            @Override
            public Object instantiateItem(@NonNull ViewGroup container, int position) {
                View v;
                if (position == 0) {
                    v = scrollView;
                } else {
                    LinearLayout ll = new LinearLayout(context);
                    ll.setOrientation(LinearLayout.VERTICAL);

                    TextView subtitle = new TextView(context);
                    subtitle.setTextColor(ViewUtils.resolveColor(context, R.attr.textColorOnAccent));
                    subtitle.setText(R.string.ChangelogBoostyDescription);
                    subtitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
                    subtitle.setGravity(Gravity.CENTER);
                    subtitle.setTypeface(ViewUtils.getTypeface(ViewUtils.ROBOTO_MEDIUM));
                    subtitle.setPadding(ViewUtils.dp(12), 0, ViewUtils.dp(12), 0);
                    ll.addView(subtitle);

                    subsView = new BoostySubsView(context);
                    if (BeamServerData.SERVER_DATA != null) {
                        List<String> list = new ArrayList<>(BeamServerData.SERVER_DATA.boostySubscribers);
                        Collections.shuffle(list);
                        subsView.setStrings(list);
                    }
                    ll.addView(subsView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

                    TextView subscribeButton = new TextView(context);
                    subscribeButton.setText(R.string.ChangelogBoostySubscribe);
                    subscribeButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
                    subscribeButton.setTextColor(ViewUtils.resolveColor(context, R.attr.boostyColorTop));
                    subscribeButton.setTypeface(ViewUtils.getTypeface(ViewUtils.ROBOTO_MEDIUM));
                    subscribeButton.setGravity(Gravity.CENTER);
                    subscribeButton.setPadding(ViewUtils.dp(12), ViewUtils.dp(8), ViewUtils.dp(12), ViewUtils.dp(8));
                    subscribeButton.setOnClickListener(v2 -> context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://boosty.to/ytkab0bp"))));
                    ll.addView(subscribeButton, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

                    v = ll;
                }

                container.addView(v);
                return v;
            }

            @Override
            public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
                container.removeView((View) object);
            }

            @Override
            public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
                return view == object;
            }
        });
        BeamButton btn = new BeamButton(context);
        pager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                if (position == pager.getAdapter().getCount() - 1) {
                    btn.setText(R.string.ChangelogOK);
                } else {
                    btn.setText(R.string.ChangelogNext);
                }
            }

            private int[] colors = new int[2];
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                float pr = position == 0 ? positionOffset : 1f;
                colors[0] = ColorUtils.blendARGB(ViewUtils.resolveColor(context, R.attr.dialogBackground), ViewUtils.resolveColor(context, R.attr.boostyColorTop), pr);
                colors[1] = ColorUtils.blendARGB(ViewUtils.resolveColor(context, R.attr.dialogBackground), ViewUtils.resolveColor(context, R.attr.boostyColorBottom), pr);
                gd.setColors(colors);
                titleA.setAlpha(1f - pr);
                titleA.setTranslationX(-titleA.getWidth() * 0.25f * pr);
                titleB.setAlpha(pr);
                titleB.setTranslationX(titleB.getWidth() * 0.25f * (1f - pr));
                btn.setColor(ColorUtils.blendARGB(ViewUtils.resolveColor(context, android.R.attr.colorAccent), ViewUtils.resolveColor(context, R.attr.boostyColorTop), pr));
            }
        });
        ll.addView(pager, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (dm.heightPixels * 0.45f)));

        btn.setText(R.string.ChangelogNext);
        btn.setOnClickListener(v -> {
            if (pager.getCurrentItem() != pager.getAdapter().getCount() - 1) {
                pager.setCurrentItem(pager.getCurrentItem() + 1);
            } else {
                dismiss();
            }
        });
        ll.addView(btn, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewUtils.dp(48)) {{
            leftMargin = topMargin = rightMargin = bottomMargin = ViewUtils.dp(12);
        }});

        ll.setFitsSystemWindows(true);
        setContentView(ll);

        KlipperApp.EVENT_BUS.registerListener(this);
        setOnDismissListener(dialog -> KlipperApp.EVENT_BUS.unregisterListener(this));
    }

    @EventHandler(runOnMainThread = true)
    public void onDataUpdated(BeamServerDataUpdatedEvent e) {
        if (BeamServerData.SERVER_DATA != null) {
            List<String> list = new ArrayList<>(BeamServerData.SERVER_DATA.boostySubscribers);
            Collections.shuffle(list);
            subsView.setStrings(list);
        }
        pager.getAdapter().notifyDataSetChanged();
    }

    @Override
    public void show() {
        super.show();
        getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
    }
}
