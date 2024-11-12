package ru.ytkab0bp.beamklipper;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.app.UiModeManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.dynamicanimation.animation.FloatValueHolder;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import ru.ytkab0bp.beamklipper.events.InstanceCreatedEvent;
import ru.ytkab0bp.beamklipper.events.InstanceDestroyedEvent;
import ru.ytkab0bp.beamklipper.events.InstanceUpdatedEvent;
import ru.ytkab0bp.beamklipper.events.WebFrontendChangedEvent;
import ru.ytkab0bp.beamklipper.serial.KlipperProbeTable;
import ru.ytkab0bp.beamklipper.serial.UsbSerialManager;
import ru.ytkab0bp.beamklipper.utils.LogUploader;
import ru.ytkab0bp.beamklipper.utils.ViewUtils;
import ru.ytkab0bp.beamklipper.view.EditTextRowView;
import ru.ytkab0bp.beamklipper.view.HomeView;
import ru.ytkab0bp.beamklipper.view.KlipperInstanceView;
import ru.ytkab0bp.beamklipper.view.PermissionRowView;
import ru.ytkab0bp.beamklipper.view.PreferencesCardView;
import ru.ytkab0bp.beamklipper.view.RefBadgeView;
import ru.ytkab0bp.beamklipper.view.SmoothItemAnimator;
import ru.ytkab0bp.beamklipper.view.SmoothResizeFrameLayout;
import ru.ytkab0bp.beamklipper.view.preferences.PreferenceSwitchView;
import ru.ytkab0bp.eventbus.EventHandler;

public class MainActivity extends AppCompatActivity {
    private final static int REQUEST_NOTIFICATIONS = 100;
    private final static int VIEW_TYPE_HEADER = 0, VIEW_TYPE_INSTANCE = 1, VIEW_TYPE_NEW = 2, VIEW_TYPE_WEB = 3;
    private final static Object NOTIFY_LIVE = new Object();

    private HomeView homeView;
    private MaterialCardView listCardView;
    private SmoothResizeFrameLayout resizeFrame;
    private RecyclerView listView;
    private List<KlipperInstance> instances = new ArrayList<>();

    private SpringAnimation newOrEditAnimation;
    private LinearLayout newOrEditLayout;
    private TextView newOrEditTitle;
    private KlipperInstance editInstance;
    private EditTextRowView nameRow;
    private EditTextRowView configRow;
    private TextView editOpenDirectoryRow;
    private TextView editUploadLogsRow;
    private PreferenceSwitchView autostartRow;
    private TextView newOrEditContinue;

    private PreferencesCardView preferencesView;

    private MaterialCardView noPermsLayout;
    private PermissionRowView batteryRow;
    private PermissionRowView notificationsRow;
    private PermissionRowView hideServicesChannelRow;
    private PermissionRowView brokenBySDCardRow;

    private ImageView logoView;
    private TextView titleView;
    private FrameLayout badgesLayout;
    private RefBadgeView[] refBadges = new RefBadgeView[3];

    private boolean isTV;

    @SuppressLint({"BatteryLife", "InlinedApi"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);

        UiModeManager uiModeManager = (UiModeManager) (getSystemService(UI_MODE_SERVICE));
        if (uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION || getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEVISION) ||
                getPackageManager().hasSystemFeature(PackageManager.FEATURE_LEANBACK) || !getPackageManager().hasSystemFeature("android.hardware.touchscreen") ||
                !getPackageManager().hasSystemFeature("android.hardware.telephony")) {
            isTV = true;
            PermissionsChecker.setIgnoreNotificationsChannel(true);
        }
        if (Build.MANUFACTURER.toLowerCase().contains("meizu") || Build.BRAND.toLowerCase().contains("meizu")) {
            PermissionsChecker.setIgnoreNotificationsChannel(true);
        }

        FrameLayout fl = new FrameLayout(this);

        homeView = new HomeView(this);
        badgesLayout = new FrameLayout(this) {
            @Override
            protected void onSizeChanged(int w, int h, int oldw, int oldh) {
                super.onSizeChanged(w, h, oldw, oldh);
                invalidateHomeProgress(homeView.getProgress());
            }
        };
        badgesLayout.setClipChildren(false);
        badgesLayout.setClipToPadding(false);
        fl.setOnApplyWindowInsetsListener((v, insets) -> {
            badgesLayout.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            preferencesView.setPadding(insets.getSystemWindowInsetLeft(), 0, insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) listCardView.getLayoutParams();
            params.leftMargin = ViewUtils.dp(21) + insets.getSystemWindowInsetLeft();
            params.topMargin = ViewUtils.dp(64) + insets.getSystemWindowInsetTop();
            params.rightMargin = ViewUtils.dp(21) + insets.getSystemWindowInsetRight();
            params.bottomMargin = ViewUtils.dp(72) + insets.getSystemWindowInsetBottom();
            listCardView.requestLayout();
            return insets;
        });

        logoView = new ImageView(this);
        logoView.setImageResource(R.drawable.icon_logo);
        badgesLayout.addView(logoView, new FrameLayout.LayoutParams(ViewUtils.dp(28), ViewUtils.dp(28)) {{
            topMargin = ViewUtils.dp(6);
            leftMargin = ViewUtils.dp(9);
        }});

        titleView = new TextView(this);
        titleView.setText(R.string.app_name);
        titleView.setGravity(Gravity.CENTER_VERTICAL);
        titleView.setTextColor(ViewUtils.resolveColor(this, android.R.attr.colorAccent));
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        titleView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewUtils.dp(22 + 18)) {{
            leftMargin = ViewUtils.dp(9 + 28 + 12);
            rightMargin = ViewUtils.dp(9);
        }});
        badgesLayout.addView(titleView);

        refBadges[0] = new RefBadgeView(this);
        refBadges[0].setIcon(R.drawable.ic_boosty, R.attr.boostyColor, R.string.badge_boosty);
        refBadges[0].setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://boosty.to/ytkab0bp"))));
        refBadges[0].setId(R.id.badge_boosty);
        refBadges[0].setNextFocusDownId(R.id.badge_telegram);
        badgesLayout.addView(refBadges[0]);

        refBadges[1] = new RefBadgeView(this);
        refBadges[1].setIcon(R.drawable.ic_telegram, R.attr.telegramColor, R.string.badge_telegram);
        refBadges[1].getIcon().setTranslationX(-ViewUtils.dp(1));
        refBadges[1].setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/ytkab0bp_channel"))));
        refBadges[1].setId(R.id.badge_telegram);
        refBadges[1].setNextFocusUpId(R.id.badge_boosty);
        refBadges[1].setNextFocusDownId(R.id.badge_k3d);
        badgesLayout.addView(refBadges[1]);

        refBadges[2] = new RefBadgeView(this);
        refBadges[2].setIcon(R.drawable.k3d_logo_new_14, 0, R.string.badge_k3d);
        refBadges[2].getIcon().setPadding(ViewUtils.dp(8), ViewUtils.dp(8), ViewUtils.dp(8), ViewUtils.dp(8));
        refBadges[2].setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/K_3_D"))));
        refBadges[2].setId(R.id.badge_k3d);
        refBadges[2].setNextFocusUpId(R.id.badge_telegram);
        badgesLayout.addView(refBadges[2]);

        listCardView = new MaterialCardView(this);
        listCardView.setStrokeWidth(ViewUtils.dp(2f));
        listCardView.setStrokeColor(ViewUtils.resolveColor(this, R.attr.cardOutlineColor));
        listCardView.setRadius(ViewUtils.dp(32));

        preferencesView = new PreferencesCardView(this);

        homeView.setProgressListener(this::invalidateHomeProgress);

        resizeFrame = new SmoothResizeFrameLayout(this);

        Paint dividerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dividerPaint.setColor(ViewUtils.resolveColor(MainActivity.this, R.attr.cardOutlineColor));
        dividerPaint.setStyle(Paint.Style.STROKE);
        dividerPaint.setStrokeWidth(ViewUtils.dp(1.5f));

        listView = new RecyclerView(this);
        listView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        listView.setLayoutManager(new LinearLayoutManager(this));
        listView.setItemAnimator(new SmoothItemAnimator());
        listView.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                for (int i = 0; i < parent.getChildCount(); i++) {
                    View child = parent.getChildAt(i);
                    if (parent.getChildViewHolder(child).getAdapterPosition() != listView.getAdapter().getItemCount() - 1) {
                        c.drawLine(ViewUtils.dp(1.5f), child.getY() + child.getHeight() - ViewUtils.dp(1), child.getWidth() - ViewUtils.dp(1.5f), child.getY() + child.getHeight() - ViewUtils.dp(1), dividerPaint);
                    }
                }
            }
        });
        homeView.setScrollView(listView);
        listView.setAdapter(new RecyclerView.Adapter() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View v;
                switch (viewType) {
                    default:
                    case VIEW_TYPE_HEADER:
                        TextView tv = new TextView(MainActivity.this);
                        tv.setTextColor(ViewUtils.resolveColor(MainActivity.this, android.R.attr.textColorPrimary));
                        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
                        tv.setTypeface(ViewUtils.getTypeface(ViewUtils.ROBOTO_MEDIUM));
                        tv.setGravity(Gravity.CENTER);
                        tv.setText(R.string.instances);
                        tv.setPadding(ViewUtils.dp(12), 0, ViewUtils.dp(12), 0);
                        tv.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewUtils.dp(52)));
                        v = tv;
                        break;
                    case VIEW_TYPE_WEB:
                    case VIEW_TYPE_INSTANCE:
                        v = new KlipperInstanceView(MainActivity.this);
                        break;
                    case VIEW_TYPE_NEW:
                        LinearLayout ll = new LinearLayout(MainActivity.this);
                        ll.setOrientation(LinearLayout.HORIZONTAL);
                        ll.setGravity(Gravity.CENTER);
                        ll.setBackground(ViewUtils.resolveDrawable(MainActivity.this, android.R.attr.selectableItemBackground));
                        ll.setPadding(0, ViewUtils.dp(16), 0, ViewUtils.dp(16));
                        ll.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewUtils.dp(52)));

                        ImageView add = new ImageView(MainActivity.this);
                        add.setImageResource(R.drawable.ic_add_outline_28);
                        add.setColorFilter(ViewUtils.resolveColor(MainActivity.this, android.R.attr.textColorSecondary));
                        ll.addView(add, new LinearLayout.LayoutParams(ViewUtils.dp(22), ViewUtils.dp(22)) {{
                            setMarginEnd(ViewUtils.dp(8));
                        }});

                        TextView title = new TextView(MainActivity.this);
                        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                        title.setTextColor(ViewUtils.resolveColor(MainActivity.this, android.R.attr.textColorPrimary));
                        title.setText(R.string.new_instance);
                        title.setGravity(Gravity.CENTER);
                        ll.addView(title, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

                        v = ll;
                        break;
                }
                return new RecyclerView.ViewHolder(v) {};
            }

            /** @noinspection unchecked*/
            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, @NonNull List payloads) {
                if (payloads.contains(NOTIFY_LIVE)) {
                    KlipperInstanceView view = (KlipperInstanceView) holder.itemView;
                    view.bind(instances.get(position - 2));
                    return;
                }
                super.onBindViewHolder(holder, position, payloads);
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                switch (getItemViewType(position)) {
                    case VIEW_TYPE_INSTANCE: {
                        KlipperInstanceView view = (KlipperInstanceView) holder.itemView;
                        view.bind(instances.get(position - 2));
                        view.setOnClickListener(v -> {
                            KlipperInstance inst = instances.get(position - 2);
                            newOrEditTitle.setText(R.string.edit_instance);
                            editInstance = inst;
                            editOpenDirectoryRow.setVisibility(View.VISIBLE);
                            editUploadLogsRow.setVisibility(View.VISIBLE);
                            autostartRow.bind(getString(R.string.autostart), null, inst.autostart);
                            nameRow.bind(R.string.instance_name, inst.name);
                            configRow.setVisibility(View.GONE);
                            animateNewOrEditLayout(true);
                            newOrEditContinue.setText(R.string.instance_ok);
                        });
                        view.setOnLongClickListener(v -> {
                            KlipperInstance inst = instances.get(position - 2);
                            new MaterialAlertDialogBuilder(MainActivity.this)
                                    .setTitle(getString(R.string.instance_delete, inst.name))
                                    .setMessage(R.string.instance_delete_confirm)
                                    .setNegativeButton(android.R.string.cancel, null)
                                    .setPositiveButton(android.R.string.ok, (dialog, which) -> KlipperApp.DATABASE.delete(inst))
                                    .show();
                            return true;
                        });
                        break;
                    }
                    case VIEW_TYPE_WEB: {
                        KlipperInstanceView view = (KlipperInstanceView) holder.itemView;
                        view.bindWeb();
                        break;
                    }
                    case VIEW_TYPE_NEW:
                        holder.itemView.setOnClickListener(v -> {
                            newOrEditTitle.setText(R.string.new_instance);
                            editInstance = null;
                            editOpenDirectoryRow.setVisibility(View.GONE);
                            editUploadLogsRow.setVisibility(View.GONE);
                            autostartRow.bind(getString(R.string.autostart), null, false);
                            nameRow.bind(R.string.instance_name, null);
                            configRow.bind(R.string.instance_config, null);
                            configRow.setVisibility(View.VISIBLE);
                            newOrEditContinue.setText(R.string.instance_create);
                            animateNewOrEditLayout(true);
                        });
                        break;
                }
            }

            @Override
            public int getItemViewType(int position) {
                return position == 0 ? VIEW_TYPE_HEADER : position == 1 ? VIEW_TYPE_WEB : position == getItemCount() - 1 ? VIEW_TYPE_NEW : VIEW_TYPE_INSTANCE;
            }

            @Override
            public int getItemCount() {
                return instances.size() + 3;
            }
        });
        resizeFrame.addView(listView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        newOrEditLayout = new LinearLayout(MainActivity.this) {
            {
                setWillNotDraw(false);
            }

            @Override
            public void draw(@NonNull Canvas canvas) {
                super.draw(canvas);

                for (int i = 0; i < getChildCount() - 1; i++) {
                    View child = getChildAt(i);
                    if (child.getVisibility() == View.VISIBLE) {
                        canvas.drawLine(ViewUtils.dp(1.5f), child.getY() + child.getHeight() - ViewUtils.dp(1), child.getWidth() - ViewUtils.dp(1.5f), child.getY() + child.getHeight() - ViewUtils.dp(1), dividerPaint);
                    }
                }
            }
        };
        newOrEditLayout.setOrientation(LinearLayout.VERTICAL);
        newOrEditLayout.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        newOrEditTitle = new TextView(this);
        newOrEditTitle.setTextColor(ViewUtils.resolveColor(this, android.R.attr.textColorPrimary));
        newOrEditTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        newOrEditTitle.setTypeface(ViewUtils.getTypeface(ViewUtils.ROBOTO_MEDIUM));
        newOrEditTitle.setGravity(Gravity.CENTER);
        newOrEditTitle.setPadding(ViewUtils.dp(12), 0, ViewUtils.dp(12), 0);
        newOrEditTitle.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewUtils.dp(52)));
        newOrEditTitle.setOnClickListener(v -> animateNewOrEditLayout(false));
        newOrEditTitle.setFocusable(false);
        newOrEditLayout.addView(newOrEditTitle);

        nameRow = new EditTextRowView(this);
        nameRow.setOnClickListener(v -> {
            FrameLayout frame = new FrameLayout(v.getContext());
            frame.setPadding(ViewUtils.dp(21), 0, ViewUtils.dp(21), 0);
            EditText et = new EditText(v.getContext());
            et.setText(nameRow.getText());
            frame.addView(et);
            new MaterialAlertDialogBuilder(v.getContext())
                    .setTitle(R.string.instance_name)
                    .setView(frame)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> nameRow.bind(R.string.instance_name, et.getText().toString()))
                    .show();
        });
        newOrEditLayout.addView(nameRow);

        configRow = new EditTextRowView(this);
        configRow.setOnClickListener(v -> {
            File config = new File(KlipperApp.INSTANCE.getFilesDir(), "klipper/config");
            List<String> filesList = new ArrayList<>();
            for (File f : config.listFiles()) {
                filesList.add(f.getName());
            }
            Collections.sort(filesList);
            new MaterialAlertDialogBuilder(v.getContext())
                    .setTitle(R.string.instance_config)
                    .setItems(filesList.toArray(new String[0]), (dialog, which) -> configRow.bind(R.string.instance_config, filesList.get(which)))
                    .show();
        });
        newOrEditLayout.addView(configRow);

        editOpenDirectoryRow = new TextView(this);
        editOpenDirectoryRow.setText(R.string.edit_open_directory);
        editOpenDirectoryRow.setTextColor(ViewUtils.resolveColor(this, android.R.attr.textColorPrimary));
        editOpenDirectoryRow.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        editOpenDirectoryRow.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        editOpenDirectoryRow.setPadding(ViewUtils.dp(21), 0, ViewUtils.dp(21), 0);
        editOpenDirectoryRow.setBackground(ViewUtils.resolveDrawable(this, android.R.attr.selectableItemBackground));
        editOpenDirectoryRow.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewUtils.dp(52)));
        editOpenDirectoryRow.setOnClickListener(v -> {
            Uri uri = DocumentsContract.buildRootUri("ru.ytkab0bp.beamklipper", editInstance.id);
            try {
                try {
                    try {
                        startActivity(new Intent("android.intent.action.VIEW").setDataAndType(uri, DocumentsContract.Document.MIME_TYPE_DIR));
                    } catch (ActivityNotFoundException unused) {
                        startActivity(new Intent("android.provider.action.BROWSE").setDataAndType(uri, DocumentsContract.Document.MIME_TYPE_DIR));
                    }
                } catch (ActivityNotFoundException unused2) {
                    startActivity(new Intent("android.provider.action.BROWSE_DOCUMENT_ROOT").setDataAndType(uri, DocumentsContract.Document.MIME_TYPE_DIR));
                }
            } catch (ActivityNotFoundException ignored) {}
        });
        newOrEditLayout.addView(editOpenDirectoryRow);

        editUploadLogsRow = new TextView(this);
        editUploadLogsRow.setText(R.string.upload_logs);
        editUploadLogsRow.setTextColor(ViewUtils.resolveColor(this, android.R.attr.textColorPrimary));
        editUploadLogsRow.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        editUploadLogsRow.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        editUploadLogsRow.setPadding(ViewUtils.dp(21), 0, ViewUtils.dp(21), 0);
        editUploadLogsRow.setBackground(ViewUtils.resolveDrawable(this, android.R.attr.selectableItemBackground));
        editUploadLogsRow.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewUtils.dp(52)));
        editUploadLogsRow.setOnClickListener(v -> LogUploader.uploadLogs(editInstance));
        newOrEditLayout.addView(editUploadLogsRow);

        autostartRow = new PreferenceSwitchView(this);
        autostartRow.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewUtils.dp(52)));
        autostartRow.setOnClickListener(v -> autostartRow.setChecked(!autostartRow.isChecked()));
        newOrEditLayout.addView(autostartRow);

        newOrEditContinue = new TextView(this);
        newOrEditContinue.setTextColor(ViewUtils.resolveColor(this, android.R.attr.textColorPrimary));
        newOrEditContinue.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        newOrEditContinue.setTypeface(ViewUtils.getTypeface(ViewUtils.ROBOTO_MEDIUM));
        newOrEditContinue.setGravity(Gravity.CENTER);
        newOrEditContinue.setPadding(ViewUtils.dp(12), 0, ViewUtils.dp(12), 0);
        newOrEditContinue.setBackground(ViewUtils.resolveDrawable(this, android.R.attr.selectableItemBackground));
        newOrEditContinue.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewUtils.dp(52)));
        newOrEditContinue.setOnClickListener(v -> {
            if (TextUtils.isEmpty(nameRow.getText())) {
                new MaterialAlertDialogBuilder(MainActivity.this)
                        .setTitle(R.string.error)
                        .setMessage(R.string.error_name_empty)
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
                return;
            }

            if (editInstance != null) {
                editInstance.name = nameRow.getText().toString().trim();
                editInstance.autostart = autostartRow.isChecked();
                KlipperApp.DATABASE.update(editInstance);
                editInstance = null;
                animateNewOrEditLayout(false);
                return;
            }

            if (TextUtils.isEmpty(configRow.getText())) {
                new MaterialAlertDialogBuilder(MainActivity.this)
                        .setTitle(R.string.error)
                        .setMessage(R.string.error_config_empty)
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
                return;
            }

            KlipperInstance inst = new KlipperInstance();
            inst.id = UUID.randomUUID().toString();
            inst.name = nameRow.getText().toString().trim();
            inst.autostart = autostartRow.isChecked();
            File cfg = new File(inst.getPublicDirectory(), "config/printer.cfg");
            cfg.getParentFile().mkdirs();
            try {
                FileInputStream fis = new FileInputStream(new File(KlipperApp.INSTANCE.getFilesDir(), "klipper/config/" + configRow.getText().toString()));
                FileOutputStream fos = new FileOutputStream(cfg);
                byte[] buffer = new byte[10240]; int c;
                while ((c = fis.read(buffer)) != -1) {
                    fos.write(buffer, 0, c);
                }
                fis.close();
                fos.close();
            } catch (Exception ignored) {}
            KlipperApp.DATABASE.insert(inst);
            animateNewOrEditLayout(false);
        });
        newOrEditLayout.addView(newOrEditContinue);

        newOrEditLayout.setVisibility(View.GONE);
        resizeFrame.addView(newOrEditLayout);

        listCardView.addView(resizeFrame, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        homeView.addView(listCardView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER) {{
            leftMargin = rightMargin = ViewUtils.dp(21);
            topMargin = ViewUtils.dp(64);
            bottomMargin = ViewUtils.dp(72);
        }});
        homeView.addView(preferencesView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        homeView.addView(badgesLayout, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT) {{
            topMargin = ViewUtils.dp(12);
            leftMargin = rightMargin = ViewUtils.dp(12);
        }});

        fl.addView(homeView);

        noPermsLayout = new MaterialCardView(this);
        noPermsLayout.setStrokeWidth(ViewUtils.dp(2f));
        noPermsLayout.setStrokeColor(ViewUtils.resolveColor(this, R.attr.cardOutlineColor));
        noPermsLayout.setRadius(ViewUtils.dp(32));
        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.VERTICAL);

        batteryRow = new PermissionRowView(this);
        batteryRow.bind(R.string.battery_optimization_exclusion, PermissionsChecker.hasBatteryPerm(), true);
        batteryRow.setPadding(batteryRow.getPaddingLeft(), ViewUtils.dp(6), batteryRow.getPaddingRight(), batteryRow.getPaddingBottom());
        batteryRow.setOnClickListener(v -> {
            PermissionRowView r = (PermissionRowView) v;
            if (!r.isChecked()) {
                startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", getPackageName(), null)));
            }
        });
        ll.addView(batteryRow);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationsRow = new PermissionRowView(this);
            notificationsRow.bind(R.string.notifications, PermissionsChecker.hasNotificationPerm(), true);
            notificationsRow.setOnClickListener(v -> {
                PermissionRowView r = (PermissionRowView) v;
                if (!r.isChecked()) {
                    requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATIONS);
                }
            });
            ll.addView(notificationsRow);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !PermissionsChecker.ignoreNotificationsChannel()) {
            hideServicesChannelRow = new PermissionRowView(this);
            hideServicesChannelRow.bind(R.string.notifications_hide_channel, PermissionsChecker.isNotificationsChannelHidden(), true);
            hideServicesChannelRow.setOnClickListener(v -> {
                PermissionRowView r = (PermissionRowView) v;
                if (!r.isChecked()) {
                    Toast.makeText(this, getString(R.string.notifications_hide_channel_info, getString(R.string.channel_services)), Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName())
                            .putExtra(Settings.EXTRA_CHANNEL_ID, KlipperApp.SERVICES_CHANNEL));
                }
            });
            ll.addView(hideServicesChannelRow);
        }
        if (!PermissionsChecker.isNotBrokenBySDCard()) {
            brokenBySDCardRow = new PermissionRowView(this);
            brokenBySDCardRow.bind(R.string.not_on_sdcard, PermissionsChecker.isNotBrokenBySDCard(), true);
            brokenBySDCardRow.setOnClickListener(v -> {
                startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.parse("package:" + KlipperApp.INSTANCE.getPackageName())));
                Toast.makeText(this, R.string.not_on_sdcard_info, Toast.LENGTH_SHORT).show();
            });
            ll.addView(brokenBySDCardRow);
        }

        PermissionRowView row = new PermissionRowView(this);
        row.titleView.setGravity(Gravity.CENTER);
        row.titleView.setTypeface(ViewUtils.getTypeface(ViewUtils.ROBOTO_MEDIUM));
        row.titleView.setText(R.string.next);
        row.mSwitch.setVisibility(View.GONE);
        row.setPadding(row.getPaddingLeft(), ViewUtils.dp(14), row.getPaddingRight(), ViewUtils.dp(14));
        row.setOnClickListener(v -> {
            if (PermissionsChecker.needBlockStart()) return;
            animateHomeView();
        });
        ll.addView(row);

        noPermsLayout.addView(ll);
        fl.addView(noPermsLayout, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER) {{
            leftMargin = topMargin = rightMargin = bottomMargin = ViewUtils.dp(21);
        }});

        noPermsLayout.setVisibility(PermissionsChecker.needBlockStart() ? View.VISIBLE : View.GONE);
        homeView.setVisibility(PermissionsChecker.needBlockStart() ? View.GONE : View.VISIBLE);

        if (isTV) {
            preferencesView.setFocusable(false);
            preferencesView.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
            badgesLayout.setFocusable(false);
            badgesLayout.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        }

        fl.setBackgroundColor(ViewUtils.resolveColor(this, android.R.attr.windowBackground));
        setContentView(fl);

        processIntent(getIntent());
        instances = new ArrayList<>(KlipperInstance.getInstances());
        KlipperApp.EVENT_BUS.registerListener(this);
    }

    @Override
    public void onBackPressed() {
        if (newOrEditLayout.getVisibility() != View.GONE) {
            animateNewOrEditLayout(false);
            return;
        }
        if (homeView.getProgress() != 0) {
            homeView.animateTo(0);
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        KlipperApp.EVENT_BUS.unregisterListener(this);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (newOrEditLayout.findFocus() != null && keyCode != KeyEvent.KEYCODE_BACK) {
            return newOrEditLayout.onKeyUp(keyCode, event);
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (newOrEditLayout.findFocus() != null && keyCode != KeyEvent.KEYCODE_BACK) {
            return newOrEditLayout.onKeyDown(keyCode, event);
        }

        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            boolean focusInBadges = homeView.getTargetProgress() == 1;
            boolean focusInList = homeView.getTargetProgress() == 0;
            boolean focusInSettings = homeView.getTargetProgress() == -1;

            if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                if (focusInSettings) return super.onKeyDown(keyCode, event);

                boolean isLast;
                if (focusInList) {
                    View focus = listView.findFocus();
                    isLast = focus != null && listView.getChildViewHolder(focus).getAdapterPosition() == listView.getAdapter().getItemCount() - 1;
                } else {
                    isLast = badgesLayout.findFocus() == refBadges[refBadges.length - 1];
                }

                if (!isLast) return super.onKeyDown(keyCode, event);

                homeView.animateTo(focusInList ? -1 : 0, () -> {
                    if (focusInList) {
                        preferencesView.getListView().getChildAt(1).requestFocus();
                    } else {
                        listView.getChildAt(2 + (KlipperInstance.isWebServerRunning() ? 1 : 0)).requestFocus();
                    }
                });
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                if (focusInBadges) return super.onKeyDown(keyCode, event);

                boolean isFirst;
                if (focusInList) {
                    View focus = listView.findFocus();
                    isFirst = focus != null && listView.getChildViewHolder(focus).getAdapterPosition() == 2 + (KlipperInstance.isWebServerRunning() ? 1 : 0);
                } else {
                    View focus = preferencesView.getListView().findFocus();
                    isFirst = focus != null && preferencesView.getListView().getChildViewHolder(focus).getAdapterPosition() == 1;
                }

                if (!isFirst) return super.onKeyDown(keyCode, event);

                homeView.animateTo(focusInList ? 1 : 0, () -> {
                    if (focusInList) {
                        refBadges[refBadges.length - 1].requestFocus();
                    } else {
                        listView.getChildAt(2 + (KlipperInstance.isWebServerRunning() ? 1 : 0)).requestFocus();
                    }
                });
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    private void invalidateHomeProgress(float progress) {
        float beb = 0.3f;
        float posProgress = Math.max(0, progress);
        for (int i = 0; i < refBadges.length; i++) {
            int j = refBadges.length - 1 - i;
            float pr = (Math.max(posProgress, beb * j) - beb * j) / (1f - beb * j);

            RefBadgeView badge = refBadges[i];
            badge.setProgress(pr);

            float fX = -ViewUtils.dp(9) + badgesLayout.getWidth() - badgesLayout.getPaddingLeft() - badgesLayout.getPaddingRight() - ViewUtils.dp(22 + 18) * (i + 1) - ViewUtils.dp(8) * i;
            float tX = 0;

            float fY = 0;
            float tY = ViewUtils.dp(92) + ViewUtils.dp(22 + 18 + 10) * i;

            badge.setTranslationX(ViewUtils.lerp(fX, tX, pr));
            badge.setTranslationY(ViewUtils.lerp(fY, tY, pr));
        }
        titleView.setTranslationX(posProgress * ((badgesLayout.getWidth() - titleView.getWidth()) / 2f - ViewUtils.dp(28 + 12)));
        titleView.setTranslationY(posProgress * ViewUtils.dp(92 - 52));

        float scale = ViewUtils.lerp(ViewUtils.dp(28), ViewUtils.dp(52), posProgress) / ViewUtils.dp(28);
        logoView.setScaleX(scale);
        logoView.setScaleY(scale);
        logoView.setTranslationX(posProgress * (badgesLayout.getWidth() - logoView.getWidth()) / 2f);
        logoView.setTranslationY((posProgress < 0.5f ? posProgress * 2 : 1f - (posProgress - 0.5f) * 2) * -ViewUtils.dp(12));

        float negProgress = Math.min(0, progress);
        listCardView.setTranslationY(progress * ViewUtils.dp(92 + (22 + 18) * refBadges.length + (10) * (refBadges.length - 1)));
        listCardView.setAlpha(1f + negProgress);

        preferencesView.setProgress(-negProgress);

        if (isTV) {
            if (progress >= 0 && preferencesView.isFocusable()) {
                preferencesView.setFocusable(false);
                preferencesView.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
            } else if (progress < 0 && !preferencesView.isFocusable()) {
                preferencesView.setFocusable(true);
                preferencesView.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
            }

            if (progress <= 0 && badgesLayout.isFocusable()) {
                badgesLayout.setFocusable(false);
                badgesLayout.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
            } else if (progress > 0 && !badgesLayout.isFocusable()) {
                badgesLayout.setFocusable(true);
                badgesLayout.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
            }
        }
    }

    @EventHandler(runOnMainThread = true)
    public void onFrontendChanged(WebFrontendChangedEvent e) {
        listView.getAdapter().notifyItemChanged(1);
    }

    @EventHandler(runOnMainThread = true)
    public void onInstanceCreated(InstanceCreatedEvent e) {
        instances.add(KlipperInstance.getInstance(e.id));
        listView.getAdapter().notifyItemInserted(listView.getAdapter().getItemCount() - 2);
    }

    @EventHandler(runOnMainThread = true)
    public void onInstanceUpdated(InstanceUpdatedEvent e) {
        int i = -1;
        for (int j = 0; j < instances.size(); j++) {
            KlipperInstance inst = instances.get(j);
            if (Objects.equals(inst.id, e.id)) {
                i = j;
                instances.set(i, KlipperInstance.getInstance(inst.id));
                break;
            }
        }
        if (i != -1) {
            listView.getAdapter().notifyItemChanged(i + 2, NOTIFY_LIVE);
        }
    }

    @EventHandler(runOnMainThread = true)
    public void onInstanceDestroyed(InstanceDestroyedEvent e) {
        int i = -1;
        for (int j = 0; j < instances.size(); j++) {
            KlipperInstance inst = instances.get(j);
            if (Objects.equals(inst.id, e.id)) {
                i = j;
                break;
            }
        }
        if (i != -1) {
            instances.remove(i);
            listView.getAdapter().notifyItemRemoved(i + 2);
        }
    }

    private void animateNewOrEditLayout(boolean visible) {
        if (newOrEditAnimation != null) {
            return;
        }
        if (visible) {
            resizeFrame.addForceNotMeasure(listView);
            newOrEditLayout.setVisibility(View.VISIBLE);
            newOrEditLayout.setAlpha(0);
        } else {
            resizeFrame.addForceNotMeasure(newOrEditLayout);
            listView.setVisibility(View.VISIBLE);
            listView.setAlpha(0);
        }
        newOrEditAnimation = new SpringAnimation(new FloatValueHolder(visible ? 0 : 1))
                .setMinimumVisibleChange(1 / 256f)
                .setSpring(new SpringForce(visible ? 1 : 0)
                        .setStiffness(1000f)
                        .setDampingRatio(SpringForce.DAMPING_RATIO_NO_BOUNCY))
                .addUpdateListener((animation, value, velocity) -> {
                    listView.setAlpha(1f - value);
                    newOrEditLayout.setAlpha(value);
                })
                .addEndListener((animation, canceled, value, velocity) -> {
                    if (visible) {
                        listView.setVisibility(View.GONE);
                        resizeFrame.removeForceNotMeasure(listView);
                        nameRow.requestFocus();
                    } else {
                        newOrEditLayout.setVisibility(View.GONE);
                        resizeFrame.removeForceNotMeasure(newOrEditLayout);
                        listView.getChildAt(2 + (KlipperInstance.isWebServerRunning() ? 1 : 0)).requestFocus();
                    }
                    newOrEditAnimation = null;
                });
        newOrEditAnimation.start();
    }

    private void animateHomeView() {
        SpringAnimation anim = new SpringAnimation(new FloatValueHolder(0))
                .setMinimumVisibleChange(1 / 256f)
                .setSpring(new SpringForce(1)
                        .setStiffness(1000)
                        .setDampingRatio(SpringForce.DAMPING_RATIO_NO_BOUNCY))
                .addUpdateListener((animation, value, velocity) -> {
                    homeView.setPivotX(homeView.getWidth() / 2f);
                    homeView.setPivotY(homeView.getHeight() / 2f);

                    homeView.setPivotX(homeView.getWidth() / 2f);
                    homeView.setPivotY(homeView.getHeight() / 2f);

                    noPermsLayout.setScaleX(ViewUtils.lerp(1f, 0.6f, value));
                    noPermsLayout.setScaleY(ViewUtils.lerp(1f, 0.6f, value));
                    noPermsLayout.setAlpha(1f - value);

                    homeView.setScaleX(ViewUtils.lerp(0.6f, 1f, value));
                    homeView.setScaleY(ViewUtils.lerp(0.6f, 1f, value));
                    homeView.setAlpha(value);
                })
                .addEndListener((animation, canceled, value, velocity) -> noPermsLayout.setVisibility(View.GONE));
        homeView.setVisibility(View.VISIBLE);
        anim.start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_NOTIFICATIONS && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            notificationsRow.setChecked(true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        batteryRow.setChecked(PermissionsChecker.hasBatteryPerm());
        if (hideServicesChannelRow != null) {
            hideServicesChannelRow.setChecked(PermissionsChecker.isNotificationsChannelHidden());
        }
        if (brokenBySDCardRow != null) {
            brokenBySDCardRow.setChecked(PermissionsChecker.isNotBrokenBySDCard());
        }
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        processIntent(intent);
    }

    private void processIntent(Intent intent) {
        if (intent != null && intent.getAction() != null && intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
            UsbSerialProber prober = new UsbSerialProber(KlipperProbeTable.getInstance());
            UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
            for (UsbSerialDriver drv : prober.findAllDrivers(manager)) {
                if (!manager.hasPermission(drv.getDevice())) {
                    manager.requestPermission(drv.getDevice(), PendingIntent.getBroadcast(this, 0, new Intent(UsbSerialManager.ACTION_ON_DEVICE_CONNECTED).setPackage(getPackageName()), PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_NO_CREATE));
                } else {
                    sendBroadcast(new Intent(UsbSerialManager.ACTION_ON_DEVICE_CONNECTED).putExtra(UsbManager.EXTRA_DEVICE, drv.getDevice()).putExtra(UsbManager.EXTRA_PERMISSION_GRANTED, true).setPackage(getPackageName()));
                }
            }
        }
    }
}
