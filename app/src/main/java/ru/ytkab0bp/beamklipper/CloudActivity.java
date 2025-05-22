package ru.ytkab0bp.beamklipper;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

import ru.ytkab0bp.beamklipper.cloud.CloudAPI;
import ru.ytkab0bp.beamklipper.cloud.CloudController;
import ru.ytkab0bp.beamklipper.events.CloudLoginStateUpdatedEvent;
import ru.ytkab0bp.beamklipper.events.CloudNeedQREvent;
import ru.ytkab0bp.beamklipper.utils.Prefs;
import ru.ytkab0bp.beamklipper.utils.ViewUtils;
import ru.ytkab0bp.beamklipper.view.FadeRecyclerView;
import ru.ytkab0bp.beamklipper.view.GLNoiseView;
import ru.ytkab0bp.beamklipper.view.QRCodeAlertDialog;
import ru.ytkab0bp.beamklipper.view.SimpleRecyclerAdapter;
import ru.ytkab0bp.beamklipper.view.SimpleRecyclerItem;
import ru.ytkab0bp.beamklipper.view.TextColorImageSpan;
import ru.ytkab0bp.beamklipper.view.recycler.CloudPreferenceItem;
import ru.ytkab0bp.eventbus.EventHandler;

public class CloudActivity extends AppCompatActivity {
    private FrameLayout buttonView;
    private TextView buttonText;
    private ProgressBar buttonProgress;
    private FadeRecyclerView recyclerView;
    private SimpleRecyclerAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.setPadding(0, ViewUtils.dp(42), 0, 0);

        TextView title = new TextView(this);
        title.setTextColor(Color.WHITE);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setText(R.string.SettingsCloudManageTitle);
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        title.setGravity(Gravity.CENTER);
        title.setPadding(ViewUtils.dp(12), 0, ViewUtils.dp(12), 0);
        ll.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setTextColor(Color.WHITE);
        subtitle.setText(R.string.SettingsCloudManageDescription);
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(ViewUtils.dp(12), ViewUtils.dp(3), ViewUtils.dp(12), ViewUtils.dp(6));
        ll.addView(subtitle);

        FrameLayout fl = new FrameLayout(this);
        recyclerView = new FadeRecyclerView(this);
        recyclerView.setBitmapMode();
        recyclerView.setAdapter(adapter = new SimpleRecyclerAdapter());
        recyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        fl.addView(recyclerView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        bindFeatures();

        ll.addView(fl, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        TextView tosButton = new TextView(this);
        SpannableStringBuilder sb = SpannableStringBuilder.valueOf(getString(R.string.SettingsCloudManageTermsOfService)).append(" ");
        Drawable dr = ContextCompat.getDrawable(this, R.drawable.ic_external_link_outline_24);
        int size = ViewUtils.dp(16);
        dr.setBounds(0, 0, size, size);
        sb.append("d", new TextColorImageSpan(dr, ViewUtils.dp(2f)), SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE);
        tosButton.setText(sb);
        tosButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        tosButton.setTextColor(Color.WHITE);
        tosButton.setTypeface(ViewUtils.getTypeface(ViewUtils.ROBOTO_MEDIUM));
        tosButton.setGravity(Gravity.CENTER);
        tosButton.setPadding(ViewUtils.dp(12), ViewUtils.dp(8), ViewUtils.dp(12), ViewUtils.dp(8));
        tosButton.setBackground(ViewUtils.createRipple(ViewUtils.resolveColor(this, android.R.attr.colorControlHighlight), 16));
        tosButton.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://beam3d.ru/slicebeam_cloud_tos.html"))));
        ll.addView(tosButton, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewUtils.dp(52)) {{
            leftMargin = rightMargin = ViewUtils.dp(16);
            bottomMargin = ViewUtils.dp(8);
        }});

        buttonView = new FrameLayout(this);
        buttonView.setBackground(ViewUtils.createRipple(ViewUtils.resolveColor(this, android.R.attr.colorControlHighlight), ViewUtils.resolveColor(this, android.R.attr.colorAccent), 16));

        buttonText = new TextView(this);
        buttonText.setTextColor(Color.WHITE);
        buttonText.setTypeface(ViewUtils.getTypeface(ViewUtils.ROBOTO_MEDIUM));
        buttonText.setGravity(Gravity.CENTER);
        buttonText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        buttonView.addView(buttonText, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));

        buttonProgress = new ProgressBar(this);
        buttonProgress.setIndeterminateTintList(ColorStateList.valueOf(Color.WHITE));
        buttonView.addView(buttonProgress, new FrameLayout.LayoutParams(ViewUtils.dp(28), ViewUtils.dp(28), Gravity.CENTER));

        bindLoginButton(false);

        ll.addView(buttonView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewUtils.dp(52)) {{
            leftMargin = rightMargin = ViewUtils.dp(16);
            bottomMargin = ViewUtils.dp(16);
        }});

        ll.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        FrameLayout frame = new FrameLayout(this);
        frame.setOnApplyWindowInsetsListener((v, insets) -> {
            ll.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets;
        });
        frame.addView(new GLNoiseView(this));
        frame.addView(ll, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(frame);

        KlipperApp.EVENT_BUS.registerListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        KlipperApp.EVENT_BUS.unregisterListener(this);
    }

    @EventHandler(runOnMainThread = true)
    public void onCloudAuthStateUpdated(CloudLoginStateUpdatedEvent e) {
        bindLoginButton(true);
        bindFeatures();
    }

    @EventHandler(runOnMainThread = true)
    public void onNeedQR(CloudNeedQREvent e) {
        new QRCodeAlertDialog(this, e.link).show();
    }

    private void bindFeatures() {
        List<SimpleRecyclerItem> items = new ArrayList<>();
        if (CloudController.getUserFeatures() != null) {
            for (CloudAPI.SubscriptionLevel lvl : CloudController.getUserFeatures().levels) {
                items.add(new CloudSubscriptionLevel(lvl));
            }
        }
        adapter.setItems(items);
    }

    private void bindLoginButton(boolean animate) {
        boolean loggedIn = Prefs.getCloudAPIToken() != null;
        boolean loading = !loggedIn && CloudController.isLoggingIn();
        boolean wasLoading = buttonProgress.getTag() != null;
        if (animate) {
            if (wasLoading != loading) {
                buttonProgress.setTag(loading ? 1 : null);

                buttonProgress.animate().cancel();
                buttonProgress.animate().scaleX(loading ? 1f : 0.4f).scaleY(loading ? 1f : 0.4f).alpha(loading ? 1f : 0f).setDuration(150).setInterpolator(ViewUtils.CUBIC_INTERPOLATOR).setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        if (loading) {
                            buttonProgress.setVisibility(View.VISIBLE);
                            buttonProgress.setAlpha(0f);
                            buttonProgress.setScaleX(0.4f);
                            buttonProgress.setScaleY(0.4f);
                        }
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (!loading) {
                            buttonProgress.setVisibility(View.GONE);
                        }
                    }
                }).start();

                buttonText.animate().cancel();
                buttonText.animate().scaleX(!loading ? 1f : 0.4f).scaleY(!loading ? 1f : 0.4f).alpha(!loading ? 1f : 0f).setDuration(150).setInterpolator(ViewUtils.CUBIC_INTERPOLATOR).setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        if (!loading) {
                            buttonText.setVisibility(View.VISIBLE);
                            buttonText.setAlpha(0f);
                            buttonText.setScaleX(0.4f);
                            buttonText.setScaleY(0.4f);
                        }
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (loading) {
                            buttonText.setVisibility(View.GONE);
                        }
                    }
                }).start();
            }
        } else {
            buttonProgress.setTag(loading ? 1 : null);
            buttonProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
            buttonText.setVisibility(loading ? View.GONE : View.VISIBLE);
        }
        buttonText.setText(loggedIn ? R.string.SettingsCloudManageButtonLogOut : R.string.SettingsCloudManageButtonLogIn);
        buttonView.setOnClickListener(v-> {
            if (loading) {
                new MaterialAlertDialogBuilder(v.getContext())
                        .setTitle(R.string.SettingsCloudManageButtonLogInCancelTitle)
                        .setMessage(R.string.SettingsCloudManageButtonLogInCancel)
                        .setNegativeButton(R.string.No, null)
                        .setPositiveButton(R.string.Yes, (dialog, which) -> CloudController.cancelLogin())
                        .show();
            } else if (Prefs.getCloudAPIToken() != null) {
                CloudController.logout();
            } else {
                CloudController.beginLogin();
            }
        });
    }

    private final static class CloudSubscriptionLevel extends SimpleRecyclerItem<CloudSubscriptionLevel.LevelHolderView> {
        private CloudAPI.SubscriptionLevel level;

        private CloudSubscriptionLevel(CloudAPI.SubscriptionLevel level) {
            this.level = level;
        }

        @Override
        public LevelHolderView onCreateView(Context ctx) {
            return new LevelHolderView(ctx);
        }

        @Override
        public void onBindView(LevelHolderView view) {
            view.bind(this);
        }

        public final static class LevelHolderView extends LinearLayout {
            private ImageView icon;
            private TextView title;
            private TextView price;

            private RecyclerView featuresLayout;
            private SimpleRecyclerAdapter featuresAdapter;

            public LevelHolderView(@NonNull Context context) {
                super(context);

                setOrientation(VERTICAL);
                setPadding(0, ViewUtils.dp(16), 0, ViewUtils.dp(8));

                LinearLayout inner = new LinearLayout(context);
                inner.setOrientation(HORIZONTAL);
                inner.setGravity(Gravity.CENTER_VERTICAL);
                inner.setPadding(ViewUtils.dp(28), 0, ViewUtils.dp(28), 0);
                addView(inner, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT) {{
                    bottomMargin = ViewUtils.dp(8);
                }});

                icon = new ImageView(context);
                inner.addView(icon, new LayoutParams(ViewUtils.dp(26), ViewUtils.dp(26)));

                title = new TextView(context);
                title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
                title.setTypeface(ViewUtils.getTypeface(ViewUtils.ROBOTO_MEDIUM));
                inner.addView(title, new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f) {{
                    leftMargin = ViewUtils.dp(12);
                }});

                price = new TextView(context);
                price.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
                price.setTypeface(ViewUtils.getTypeface(ViewUtils.ROBOTO_MEDIUM));
                inner.addView(price);

                featuresLayout = new RecyclerView(context) {
                    @Override
                    public boolean dispatchTouchEvent(MotionEvent ev) {
                        return false;
                    }

                    @Override
                    protected boolean dispatchHoverEvent(MotionEvent event) {
                        return false;
                    }
                };
                featuresLayout.setLayoutManager(new LinearLayoutManager(context));
                featuresLayout.setAdapter(featuresAdapter = new SimpleRecyclerAdapter());
                addView(featuresLayout, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT) {{
                    topMargin = ViewUtils.dp(3);
                    leftMargin = rightMargin = ViewUtils.dp(16);
                    bottomMargin = ViewUtils.dp(8);
                }});

                setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT) {{
                    leftMargin = rightMargin = ViewUtils.dp(12);
                    topMargin = ViewUtils.dp(12);
                }});
                onApplyTheme();
            }

            public void bind(CloudSubscriptionLevel item) {
                CloudAPI.SubscriptionLevel lvl = item.level;
                title.setText(lvl.title);
                price.setText(lvl.price);
                if (lvl.level <= 0) {
                    icon.setImageResource(R.drawable.ic_zero_ruble_outline_28);
                    price.setText(R.string.SettingsCloudManageFree);
                } else if (lvl.level == 1) {
                    icon.setImageResource(R.drawable.ic_stars_outline_28);
                } else {
                    icon.setImageResource(R.drawable.ic_cloud_plus_outline_28);
                }

                List<SimpleRecyclerItem> items = new ArrayList<>();
                CloudAPI.UserFeatures features = CloudController.getUserFeatures();
                CloudAPI.UserInfo info = CloudController.getUserInfo();
                Context ctx = getContext();
                if (!BuildConfig.IS_GOOGLE_PLAY && features.earlyAccessLevel != -1 && lvl.level >= features.earlyAccessLevel) {
                    items.add(new CloudPreferenceItem()
                            .setForceDark(true)
                            .setPaddings(ViewUtils.dp(8))
                            .setIcon(R.drawable.ic_clock_circle_dashed_outline_24)
                            .setTitle(ctx.getString(R.string.SettingsCloudManageFeatureEarlyAccess))
                            .setSubtitle(ctx.getString(R.string.SettingsCloudManageFeatureEarlyAccessDescription)));
                }
                if (features.remoteAccessLevel != -1 && lvl.level >= features.remoteAccessLevel) {
                    items.add(new CloudPreferenceItem()
                            .setForceDark(true)
                            .setPaddings(ViewUtils.dp(8))
                            .setIcon(R.drawable.ic_globe_outline_28)
                            .setTitle(ctx.getString(R.string.SettingsCloudManageFeatureRemoteAccess))
                            .setSubtitle(ctx.getString(R.string.SettingsCloudManageFeatureRemoteAccessDescription, features.remoteAccessPrintersLimit)));
                }
                if (features.syncRequiredLevel != -1 && lvl.level >= features.syncRequiredLevel) {
                    items.add(new CloudPreferenceItem()
                            .setForceDark(true)
                            .setPaddings(ViewUtils.dp(8))
                            .setIcon(R.drawable.ic_sync_outline_28)
                            .setTitle(ctx.getString(R.string.SettingsCloudManageFeatureCloudSync))
                            .setSubtitle(ctx.getString(R.string.SettingsCloudManageFeatureCloudSyncDescription)));
                }
                if (features.aiGeneratorRequiredLevel != -1 && lvl.level >= features.aiGeneratorRequiredLevel) {
                    items.add(new CloudPreferenceItem()
                            .setForceDark(true)
                            .setPaddings(ViewUtils.dp(8))
                            .setIcon(R.drawable.ic_brain_outline_28)
                            .setTitle(ctx.getString(R.string.SettingsCloudManageFeatureAIGenerator))
                            .setSubtitle(ctx.getString(R.string.SettingsCloudManageFeatureAIGeneratorDescription, features.aiGeneratorModelsPerMonth)));
                }
                if (lvl.level > 0) {
                    items.add(new CloudPreferenceItem()
                            .setForceDark(true)
                            .setPaddings(ViewUtils.dp(8))
                            .setIcon(R.drawable.ic_box_heart_outline_28)
                            .setTitle(ctx.getString(R.string.SettingsCloudManageFeatureFreeForAll))
                            .setSubtitle(ctx.getString(R.string.SettingsCloudManageFeatureFreeForAllDescription)));
                }
                featuresAdapter.setItems(items);
                featuresLayout.setVisibility(items.isEmpty() ? View.GONE : View.VISIBLE);

                boolean subscribed = lvl.level > 0 && info != null && lvl.level == info.currentLevel;
                boolean allowSubscribe = lvl.level > 0 && (info == null || lvl.level > info.currentLevel);
                if (subscribed) {
                    price.setText(R.string.SettingsCloudManageSubscribed);
                }
                price.setVisibility(allowSubscribe || subscribed ? View.VISIBLE : View.GONE);
                setOnClickListener(v -> {
                    if (subscribed) {
                        v.getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(lvl.manageUrl)));
                    } else {
                        new MaterialAlertDialogBuilder(getContext())
                                .setTitle(lvl.title)
                                .setMessage(R.string.SettingsCloudManageLevelRedirectMessage)
                                .setPositiveButton(android.R.string.ok, (dialog, which) -> v.getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(lvl.subscribeOrUpgradeUrl))))
                                .setNegativeButton(R.string.SettingsCloudManageLevelRedirectAlreadySubscribed, (dialog, which) -> v.getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(features.alreadySubscribedInfoUrl))))
                                .show();
                    }
                });
                setClickable(allowSubscribe || subscribed);
                onApplyTheme();
            }

            private void onApplyTheme() {
                int accent = ViewUtils.resolveColor(getContext(), android.R.attr.colorAccent);
                if (ColorUtils.calculateLuminance(accent) >= 0.6f) {
                    accent = ColorUtils.blendARGB(accent, Color.BLACK, 0.075f);
                }
                boolean tooLight = ColorUtils.calculateLuminance(accent) >= 0.6f;
                title.setTextColor(0xffffffff);
                price.setTextColor(0xffffffff);
                icon.setImageTintList(ColorStateList.valueOf(0xffffffff));
                featuresLayout.setBackground(ViewUtils.createRipple(0, tooLight ? 0x33ffffff : 0x21ffffff, 24));
                setBackground(ViewUtils.createRipple(0x21000000, ColorUtils.blendARGB(0xffffffff, accent, tooLight ? 0.9f : 0.75f), 32));
            }
        }
    }
}
