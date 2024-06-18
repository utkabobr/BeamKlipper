package ru.ytkab0bp.beamklipper.view;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.dynamicanimation.animation.FloatValueHolder;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.LinkedList;
import java.util.Objects;

import ru.ytkab0bp.beamklipper.KlipperApp;
import ru.ytkab0bp.beamklipper.KlipperInstance;
import ru.ytkab0bp.beamklipper.R;
import ru.ytkab0bp.beamklipper.events.InstanceStateChangedEvent;
import ru.ytkab0bp.beamklipper.events.WebStateChangedEvent;
import ru.ytkab0bp.beamklipper.service.WebService;
import ru.ytkab0bp.beamklipper.utils.Prefs;
import ru.ytkab0bp.beamklipper.utils.ViewUtils;
import ru.ytkab0bp.eventbus.EventHandler;

public class KlipperInstanceView extends LinearLayout {
    private String id;
    private CardView cardView;
    private ImageView icon;
    private LinearLayout titleSubtitleLayout;
    private TextView title;
    private TextView subtitle;
    private StartStopButton startStopButton;

    private SpringAnimation visibleAnimation;
    private LinkedList<Runnable> visibleAnimationQueue = new LinkedList<>();

    public KlipperInstanceView(Context context) {
        super(context);

        setPadding(ViewUtils.dp(16), ViewUtils.dp(12), ViewUtils.dp(16), ViewUtils.dp(12));
        setGravity(Gravity.CENTER_VERTICAL);
        setWillNotDraw(false);
        setBackground(ViewUtils.resolveDrawable(context, android.R.attr.selectableItemBackground));
        setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        cardView = new CardView(context);
        cardView.setCardElevation(0);
        cardView.setRadius(ViewUtils.dp(14));
        FrameLayout fl = new FrameLayout(context);
        fl.setPadding(ViewUtils.dp(8), ViewUtils.dp(8), ViewUtils.dp(8), ViewUtils.dp(8));
        icon = new ImageView(context);
        icon.setImageTintList(ColorStateList.valueOf(Color.WHITE));
        icon.setLayoutParams(new LinearLayout.LayoutParams(ViewUtils.dp(24), ViewUtils.dp(24)));
        fl.addView(icon);
        cardView.addView(fl);
        addView(cardView);

        titleSubtitleLayout = new LinearLayout(context);
        titleSubtitleLayout.setOrientation(VERTICAL);
        titleSubtitleLayout.setClipToPadding(false);
        titleSubtitleLayout.setClipChildren(false);
        title = new TextView(context);
        title.setTextColor(ViewUtils.resolveColor(context, android.R.attr.textColorPrimary));
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        titleSubtitleLayout.addView(title);

        subtitle = new TextView(context);
        subtitle.setTextColor(ViewUtils.resolveColor(context, android.R.attr.textColorSecondary));
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        subtitle.setVisibility(GONE);
        titleSubtitleLayout.addView(subtitle);

        addView(titleSubtitleLayout, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f) {{
            leftMargin = rightMargin = ViewUtils.dp(12);
        }});

        startStopButton = new StartStopButton(context);
        startStopButton.setPadding(ViewUtils.dp(8), ViewUtils.dp(8), ViewUtils.dp(8), ViewUtils.dp(8));
        startStopButton.setLayoutParams(new LinearLayout.LayoutParams(ViewUtils.dp(28 + 12), ViewUtils.dp(28 + 12)));
        addView(startStopButton);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        KlipperApp.EVENT_BUS.registerListener(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        KlipperApp.EVENT_BUS.unregisterListener(this);
    }

    public void setColorIndex(int i) {
        switch (i) {
            default:
            case 0:
                cardView.setCardBackgroundColor(ViewUtils.resolveColor(getContext(), R.attr.startStopButtonColor_0));
                break;
            case 1:
                cardView.setCardBackgroundColor(ViewUtils.resolveColor(getContext(), R.attr.startStopButtonColor_1));
                break;
            case 2:
                cardView.setCardBackgroundColor(ViewUtils.resolveColor(getContext(), R.attr.startStopButtonColor_2));
                break;
            case 3:
                cardView.setCardBackgroundColor(ViewUtils.resolveColor(getContext(), R.attr.startStopButtonColor_3));
                break;
            case 4:
                cardView.setCardBackgroundColor(ViewUtils.resolveColor(getContext(), R.attr.startStopButtonColor_4));
                break;
            case 5:
                cardView.setCardBackgroundColor(ViewUtils.resolveColor(getContext(), R.attr.startStopButtonColor_5));
                break;
            case 6:
                cardView.setCardBackgroundColor(ViewUtils.resolveColor(getContext(), R.attr.startStopButtonColor_6));
                break;
            case 7:
                cardView.setCardBackgroundColor(ViewUtils.resolveColor(getContext(), R.attr.startStopButtonColor_7));
                break;
            case 8:
                cardView.setCardBackgroundColor(ViewUtils.resolveColor(getContext(), R.attr.startStopButtonColor_8));
                break;
            case 9:
                cardView.setCardBackgroundColor(ViewUtils.resolveColor(getContext(), R.attr.startStopButtonColor_9));
                break;
        }
        invalidate();
    }

    public void bindWeb() {
        this.id = null;
        if (Prefs.isMainsailEnabled()) {
            icon.setImageResource(R.drawable.ic_sailing_24);
            title.setText(R.string.mainsail);
            setColorIndex(6);
        } else {
            icon.setImageResource(R.drawable.ic_square_stack_up_outline_28);
            title.setText(R.string.fluidd);
            setColorIndex(9);
        }

        boolean visible = KlipperInstance.isWebServerRunning();
        subtitle.setVisibility(visible ? VISIBLE : GONE);
        subtitle.setTag(visible ? true : null);
        if (visible) {
            bindWebSubtitle();
        }
        setOnClickListener(v -> v.getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://127.0.0.1:" + WebService.PORT + "/"))));
        setClickable(visible);

        startStopButton.setVisibility(GONE);
    }

    private void bindWebSubtitle() {
        WifiManager wm = (WifiManager) KlipperApp.INSTANCE.getSystemService(Context.WIFI_SERVICE);
        subtitle.setText(KlipperApp.INSTANCE.getString(R.string.ip_info, Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress()), WebService.PORT));
    }

    public void bind(KlipperInstance instance) {
        if (instance == null) return;
        this.id = instance.id;
        icon.setImageResource(instance.icon.drawable);
        title.setText(instance.name);

        if (instance.getState() == KlipperInstance.State.STARTING) {
            subtitle.setText(R.string.instance_starting);
        } else if (instance.getState() == KlipperInstance.State.STOPPING) {
            subtitle.setText(R.string.instance_stopping);
        }

        boolean wasVisible = subtitle.getTag() != null;
        boolean visible = instance.getState() == KlipperInstance.State.STARTING || instance.getState() == KlipperInstance.State.STOPPING;
        if (visible != wasVisible) {
            subtitle.setVisibility(visible ? VISIBLE : GONE);
            subtitle.setTag(visible ? true : null);
        }

        setColorIndex(id.hashCode() % 10);
        startStopButton.setColorIndex(id.hashCode() % 10);
        startStopButton.setStopped(instance.getState() != KlipperInstance.State.RUNNING && instance.getState() != KlipperInstance.State.STOPPING);
        startStopButton.setOnClickListener(v -> {
            KlipperInstance inst = KlipperInstance.getInstance(id);
            if (inst.getState() == KlipperInstance.State.STARTING || inst.getState() == KlipperInstance.State.STOPPING) {
                return;
            }

            if (inst.getState() == KlipperInstance.State.IDLE) {
                if (!KlipperInstance.hasFreeSlots()) {
                    new MaterialAlertDialogBuilder(getContext())
                            .setTitle(R.string.no_free_slots)
                            .setMessage(getContext().getString(R.string.no_free_slots_description, KlipperInstance.SLOTS_COUNT))
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                    return;
                }
                inst.start();
            } else {
                inst.stop();

                if (inst.autostart) {
                    inst.autostart = false;
                    KlipperApp.DATABASE.update(inst);
                }
            }
        });
        invalidate();
    }

    private void animateSubtitle(boolean visible) {
        subtitle.setTag(visible ? true : null);
        if (visibleAnimation != null) {
            visibleAnimationQueue.push(() -> animateSubtitle(visible));
            return;
        }

        float fY, tY;
        if (visible) {
            subtitle.setVisibility(VISIBLE);
            subtitle.setAlpha(0);

            fY = ViewUtils.dp(8);
            tY = 0;
        } else {
            fY = 0;
            tY = ViewUtils.dp(8);
        }
        visibleAnimation = new SpringAnimation(new FloatValueHolder(0))
                .setMinimumVisibleChange(1 / 256f)
                .setSpring(new SpringForce(1)
                        .setStiffness(1000f)
                        .setDampingRatio(SpringForce.DAMPING_RATIO_NO_BOUNCY))
                .addUpdateListener((animation, value, velocity) -> {
                    titleSubtitleLayout.setTranslationY(ViewUtils.lerp(fY, tY, value));
                    subtitle.setAlpha(visible ? value : 1f - value);
                })
                .addEndListener((animation, canceled, value, velocity) -> {
                    if (!visible) {
                        subtitle.setVisibility(GONE);
                        titleSubtitleLayout.setTranslationY(0);
                    }
                    visibleAnimation = null;
                    if (!visibleAnimationQueue.isEmpty()) {
                        visibleAnimationQueue.remove(0).run();
                    }
                });
        titleSubtitleLayout.setTranslationY(fY);
        visibleAnimation.start();
    }

    @EventHandler(runOnMainThread = true)
    public void onWebStateChanged(WebStateChangedEvent e) {
        if (id == null) {
            if (e.state == KlipperInstance.State.RUNNING) {
                bindWebSubtitle();
            }

            boolean wasVisible = subtitle.getTag() != null;
            boolean visible = e.state == KlipperInstance.State.RUNNING;
            if (visible != wasVisible) {
                animateSubtitle(visible);
                setClickable(visible);
            }
        }
    }

    @EventHandler(runOnMainThread = true)
    public void onStateChanged(InstanceStateChangedEvent e) {
        if (Objects.equals(id, e.id)) {
            if (e.state == KlipperInstance.State.STARTING) {
                subtitle.setText(R.string.instance_starting);
            } else if (e.state == KlipperInstance.State.STOPPING) {
                subtitle.setText(R.string.instance_stopping);
            }

            boolean wasVisible = subtitle.getTag() != null;
            boolean visible = e.state == KlipperInstance.State.STARTING || e.state == KlipperInstance.State.STOPPING;
            if (visible != wasVisible) {
                animateSubtitle(visible);
            }

            startStopButton.setStopped(e.state != KlipperInstance.State.RUNNING && e.state != KlipperInstance.State.STOPPING);
        }
    }
}
