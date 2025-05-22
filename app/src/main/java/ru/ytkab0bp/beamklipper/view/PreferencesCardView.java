package ru.ytkab0bp.beamklipper.view;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Region;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.hoho.android.usbserial.driver.UsbSerialDriver;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import ru.ytkab0bp.beamklipper.BeamServerData;
import ru.ytkab0bp.beamklipper.CloudActivity;
import ru.ytkab0bp.beamklipper.KlipperApp;
import ru.ytkab0bp.beamklipper.KlipperInstance;
import ru.ytkab0bp.beamklipper.MainActivity;
import ru.ytkab0bp.beamklipper.R;
import ru.ytkab0bp.beamklipper.cloud.CloudController;
import ru.ytkab0bp.beamklipper.events.CloudLoginStateUpdatedEvent;
import ru.ytkab0bp.beamklipper.serial.KlipperProbeTable;
import ru.ytkab0bp.beamklipper.serial.UsbSerialManager;
import ru.ytkab0bp.beamklipper.utils.Prefs;
import ru.ytkab0bp.beamklipper.utils.ViewUtils;
import ru.ytkab0bp.beamklipper.view.preferences.PreferenceHeaderView;
import ru.ytkab0bp.beamklipper.view.preferences.PreferenceSwitchView;
import ru.ytkab0bp.beamklipper.view.preferences.PreferenceValueView;
import ru.ytkab0bp.beamklipper.view.preferences.PreferenceView;
import ru.ytkab0bp.eventbus.EventHandler;

public class PreferencesCardView extends FrameLayout {
    private final static int MIN_HEIGHT_DP = 64;
    private final static int VIEW_TYPE_HEADER = 0, VIEW_TYPE_SWITCH = 1, VIEW_TYPE_PREFERENCE = 2, VIEW_TYPE_PREF_VALUE = 3;
    private Paint outlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint dimmPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private LinearLayout header;
    private TextView title;
    private float progress;

    private RecyclerView listView;
    private RecyclerView.Adapter adapter;

    private int itemsCount = 0;
    private int accountHeaderRow;
    private int accountStatusRow;
    private int generalHeaderRow;
    private int systemSettingsRow;
    private int frontendRow;
    private int cameraHeaderRow;
    private int cameraEnabledRow;
    private int usbHeaderRow;
    private int usbNamingRow;
    private int listUsbRow;
    private int otherHeaderRow;
    private int getMCUFirmwareRow;

    public PreferencesCardView(@NonNull Context context) {
        super(context);

        dimmPaint.setColor(Color.BLACK);
        outlinePaint.setStyle(Paint.Style.FILL);
        outlinePaint.setColor(ViewUtils.resolveColor(getContext(), R.attr.cardOutlineColor));
        bgPaint.setColor(ViewUtils.resolveColor(getContext(), android.R.attr.windowBackground));

        LinearLayout ll = new LinearLayout(context);
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.setGravity(Gravity.CENTER_HORIZONTAL);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(ViewUtils.dp(4f));
        paint.setColor(ViewUtils.resolveColor(getContext(), R.attr.dividerColor));
        header = new LinearLayout(context) {
            @Override
            protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);

                float cx = getWidth() / 2f, cy = ViewUtils.dp(8) + paint.getStrokeWidth() - ViewUtils.dp(32) * progress, len = ViewUtils.dp(32);
                canvas.drawLine(cx - len / 2f, cy, cx + len / 2f, cy, paint);
            }
        };
        header.setPadding(ViewUtils.dp(21), ViewUtils.dp(8), ViewUtils.dp(21), 0);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setWillNotDraw(false);

        title = new TextView(context);
        title.setText(R.string.Settings);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        title.setTextColor(ViewUtils.resolveColor(context, android.R.attr.textColorSecondary));
        title.setTypeface(ViewUtils.getTypeface(ViewUtils.ROBOTO_MEDIUM));
        title.setGravity(Gravity.CENTER);
        title.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        header.addView(title);

        ll.addView(header, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewUtils.dp(64)));

        updateRows();
        listView = new RecyclerView(context);
        listView.setLayoutManager(new LinearLayoutManager(context));
        listView.setAdapter(adapter = new RecyclerView.Adapter() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View v;
                switch (viewType) {
                    default:
                    case VIEW_TYPE_HEADER:
                        v = new PreferenceHeaderView(context);
                        break;
                    case VIEW_TYPE_SWITCH:
                        v = new PreferenceSwitchView(context);
                        break;
                    case VIEW_TYPE_PREFERENCE:
                        v = new PreferenceView(context);
                        break;
                    case VIEW_TYPE_PREF_VALUE:
                        v = new PreferenceValueView(context);
                        break;
                }
                return new RecyclerView.ViewHolder(v) {};
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                switch (getItemViewType(position)) {
                    case VIEW_TYPE_HEADER:
                        PreferenceHeaderView header = (PreferenceHeaderView) holder.itemView;
                        if (position == cameraHeaderRow) {
                            header.setText(R.string.Camera);
                        } else if (position == usbHeaderRow) {
                            header.setText(R.string.USB);
                        } else if (position == generalHeaderRow) {
                            header.setText(R.string.General);
                        } else if (position == accountHeaderRow) {
                            header.setText(R.string.SettingsCloudManageTitle);
                        } else if (position == otherHeaderRow) {
                            header.setText(R.string.Other);
                        }
                        break;
                    case VIEW_TYPE_SWITCH:
                        PreferenceSwitchView switchView = (PreferenceSwitchView) holder.itemView;
                        if (position == cameraEnabledRow) {
                            switchView.bind(getContext().getString(R.string.EnableCamera), null, Prefs.isCameraEnabled());
                            switchView.setOnClickListener(v -> {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ContextCompat.checkSelfPermission(v.getContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                                    ActivityCompat.requestPermissions((Activity) v.getContext(), new String[]{Manifest.permission.CAMERA}, 0);
                                    return;
                                }
                                switchView.setChecked(!switchView.isChecked());
                                Prefs.setCameraEnabled(switchView.isChecked());
                                KlipperInstance.onCameraConfigChanged(switchView.isChecked());
                            });
                        }
                        break;
                    case VIEW_TYPE_PREFERENCE:
                        PreferenceView pref = (PreferenceView) holder.itemView;
                        if (position == listUsbRow) {
                            pref.bind(getContext().getString(R.string.ListUSB), null);
                            pref.setOnClickListener(v -> {
                                UsbManager manager = (UsbManager) KlipperApp.INSTANCE.getSystemService(Context.USB_SERVICE);
                                List<String> list = new ArrayList<>();
                                for (UsbDevice dev : manager.getDeviceList().values()) {
                                    Class<? extends UsbSerialDriver> drv = KlipperProbeTable.getInstance().findDriver(dev);
                                    list.add(Integer.toHexString(dev.getVendorId()) + "/" + Integer.toHexString(dev.getProductId()) + " - " + dev.getDeviceName() + (drv != null ? " - " + drv.getName() + "\n" + new File(KlipperApp.INSTANCE.getFilesDir(), "serial/" + UsbSerialManager.getUID(dev)).getAbsolutePath() : ""));
                                }
                                AlertDialog.Builder b = new MaterialAlertDialogBuilder(context).setTitle(R.string.ListUSBTitle);
                                if (list.isEmpty()) {
                                    b.setMessage(R.string.ListUSBNoDevices);
                                } else {
                                    b.setItems(list.toArray(new String[0]), null);
                                }
                                b.setPositiveButton(android.R.string.ok, null).show();
                            });
                        } else if (position == accountStatusRow) {
                            if (Prefs.getCloudAPIToken() == null) {
                                pref.bind(getContext().getString(R.string.SettingsCloudNotLoggedIn), getContext().getString(R.string.SettingsCloudTapToShowMore));
                            } else {
                                if (CloudController.getUserInfo() == null) {
                                    pref.bind(getContext().getString(R.string.SettingsCloudLoading), null);
                                } else {
                                    pref.bind(CloudController.getUserInfo().displayName, getContext().getString(R.string.SettingsCloudTapToManage));
                                }
                            }
                            pref.setOnClickListener(v -> v.getContext().startActivity(new Intent(v.getContext(), CloudActivity.class)));
                        } else if (position == systemSettingsRow) {
                            pref.bind(getContext().getString(R.string.SystemSettings), null);
                            pref.setOnClickListener(v -> getContext().startActivity(new Intent(Settings.ACTION_SETTINGS)));
                        } else if (position == getMCUFirmwareRow) {
                            pref.bind(getContext().getString(R.string.OtherGetFirmware), null);
                            pref.setOnClickListener(v -> new QRCodeAlertDialog(getContext(), "https://github.com/utkabobr/klipper/releases/tag/prebuilt-v0.12.0").show());
                        }
                        break;
                    case VIEW_TYPE_PREF_VALUE:
                        PreferenceValueView val = (PreferenceValueView) holder.itemView;
                        if (position == usbNamingRow) {
                            val.bind(KlipperApp.INSTANCE.getString(R.string.USBDeviceNaming), KlipperApp.INSTANCE.getString(Prefs.getUsbDeviceNaming() == Prefs.USB_DEVICE_NAMING_BY_PATH ? R.string.USBDeviceNamingByPath : R.string.USBDeviceNamingByVidPid));
                            val.setOnClickListener(v -> new MaterialAlertDialogBuilder(v.getContext())
                                    .setTitle(R.string.USBDeviceNaming)
                                    .setItems(new CharSequence[] {
                                            KlipperApp.INSTANCE.getString(R.string.USBDeviceNamingByPath),
                                            KlipperApp.INSTANCE.getString(R.string.USBDeviceNamingByVidPid)
                                    }, (dialog, which) -> {
                                        Prefs.setUsbDeviceNaming(which);
                                        notifyItemChanged(holder.getAdapterPosition());
                                    }).show());
                        } else if (position == frontendRow) {
                            val.bind(KlipperApp.INSTANCE.getString(R.string.WebFrontend), KlipperApp.INSTANCE.getString(Prefs.isMainsailEnabled() ? R.string.Mainsail : R.string.Fluidd));
                            val.setOnClickListener(v -> new MaterialAlertDialogBuilder(v.getContext())
                                    .setTitle(R.string.WebFrontend)
                                    .setItems(new CharSequence[] {
                                            KlipperApp.INSTANCE.getString(R.string.Fluidd),
                                            KlipperApp.INSTANCE.getString(R.string.Mainsail)
                                    }, (dialog, which) -> {
                                        Prefs.setMainsailEnabled(which == 1);
                                        notifyItemChanged(holder.getAdapterPosition());
                                    }).show());
                        }
                        break;
                }
            }

            @Override
            public int getItemCount() {
                return itemsCount;
            }

            @Override
            public int getItemViewType(int position) {
                if (position == cameraEnabledRow) {
                    return VIEW_TYPE_SWITCH;
                } else if (position == cameraHeaderRow || position == usbHeaderRow || position == generalHeaderRow || position == accountHeaderRow || position == otherHeaderRow) {
                    return VIEW_TYPE_HEADER;
                } else if (position == listUsbRow || position == accountStatusRow || position == systemSettingsRow || position == getMCUFirmwareRow) {
                    return VIEW_TYPE_PREFERENCE;
                } else if (position == usbNamingRow || position == frontendRow) {
                    return VIEW_TYPE_PREF_VALUE;
                }
                return 0;
            }
        });
        ll.addView(listView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        addView(ll, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        setWillNotDraw(false);
        setFitsSystemWindows(true);
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

    @EventHandler(runOnMainThread = true)
    public void onCloudAuthStateUpdated(CloudLoginStateUpdatedEvent e) {
        if (BeamServerData.isCloudAvailable()) {
            adapter.notifyItemChanged(accountStatusRow);
        }
    }

    public LinearLayout getHeader() {
        return header;
    }

    public RecyclerView getListView() {
        return listView;
    }

    private void updateRows() {
        itemsCount = 0;
        if (BeamServerData.isCloudAvailable()) {
            accountHeaderRow = itemsCount++;
            accountStatusRow = itemsCount++;
        } else {
            accountHeaderRow = -1;
            accountStatusRow = -1;
        }
        generalHeaderRow = itemsCount++;
        if (getContext() instanceof MainActivity && ((MainActivity) getContext()).isCurrentLauncher()) {
            systemSettingsRow = itemsCount++;
        } else {
            systemSettingsRow = -1;
        }
        frontendRow = itemsCount++;
        cameraHeaderRow = itemsCount++;
        cameraEnabledRow = itemsCount++;
        usbHeaderRow = itemsCount++;
        usbNamingRow = itemsCount++;
        listUsbRow = itemsCount++;
        otherHeaderRow = itemsCount++;
        getMCUFirmwareRow = itemsCount++;
    }

    private Path path = new Path();
    @Override
    public void draw(@NonNull Canvas canvas) {
        float radius = (1f - progress) * ViewUtils.dp(32);
        path.rewind();
        path.addRoundRect(0, ViewUtils.lerp(getHeight() - ViewUtils.dp(MIN_HEIGHT_DP) - getPaddingBottom(), 0, progress), getWidth(), getHeight() + radius, radius, radius, Path.Direction.CW);
        if (progress > 0) {
            canvas.save();
            canvas.clipPath(path, Region.Op.DIFFERENCE);
            dimmPaint.setAlpha((int) (0x33 * progress));
            canvas.drawPaint(dimmPaint);
            canvas.restore();
        }
        canvas.save();
        canvas.clipPath(path);
        int alpha = outlinePaint.getAlpha();
        outlinePaint.setAlpha((int) ((1f - progress) * alpha));
        float stroke = outlinePaint.getStrokeWidth() / 2f;
        canvas.drawPaint(bgPaint);
        canvas.drawRoundRect(stroke, ViewUtils.lerp(getHeight() - ViewUtils.dp(MIN_HEIGHT_DP) - getPaddingBottom() + stroke, 0, progress), getWidth() - stroke, getHeight() + radius, radius, radius, outlinePaint);
        outlinePaint.setAlpha(alpha);
        super.draw(canvas);
        canvas.restore();
    }

    private void invalidateProgress() {
        title.setScaleX(ViewUtils.lerp(1f, 0.5f, progress));
        title.setScaleY(ViewUtils.lerp(1f, 0.5f, progress));
        header.setAlpha(1f - progress);
        header.invalidate();
        listView.setAlpha(progress);
        for (int i = 0; i < getChildCount(); i++) {
            getChildAt(i).setTranslationY(ViewUtils.lerp(getHeight() - ViewUtils.dp(MIN_HEIGHT_DP) - getPaddingTop() - getPaddingBottom(), 0, progress));
        }

        if (getContext() instanceof MainActivity) {
            Window w = ((MainActivity) getContext()).getWindow();
            w.setNavigationBarColor(ColorUtils.blendARGB(ViewUtils.resolveColor(getContext(), R.attr.navbarColor), ViewUtils.resolveColor(getContext(), android.R.attr.windowBackground), progress));
        }
    }

    public void setProgress(float progress) {
        this.progress = progress;
        invalidateProgress();
        invalidate();
    }
}
