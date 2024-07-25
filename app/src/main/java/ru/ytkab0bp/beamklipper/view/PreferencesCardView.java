package ru.ytkab0bp.beamklipper.view;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Region;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.hoho.android.usbserial.driver.UsbSerialDriver;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import ru.ytkab0bp.beamklipper.KlipperApp;
import ru.ytkab0bp.beamklipper.KlipperInstance;
import ru.ytkab0bp.beamklipper.R;
import ru.ytkab0bp.beamklipper.serial.KlipperProbeTable;
import ru.ytkab0bp.beamklipper.serial.UsbSerialManager;
import ru.ytkab0bp.beamklipper.service.BaseRemoteControlService;
import ru.ytkab0bp.beamklipper.utils.Prefs;
import ru.ytkab0bp.beamklipper.utils.ViewUtils;
import ru.ytkab0bp.beamklipper.view.preferences.PreferenceHeaderView;
import ru.ytkab0bp.beamklipper.view.preferences.PreferenceSwitchView;
import ru.ytkab0bp.beamklipper.view.preferences.PreferenceValueView;
import ru.ytkab0bp.beamklipper.view.preferences.PreferenceView;

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

    private int itemsCount = 0;
    private int generalHeaderRow;
    private int frontendRow;
    private int remoteServiceRow;
    private int cameraHeaderRow;
    private int cameraEnabledRow;
    private int usbHeaderRow;
    private int usbNamingRow;
    private int listUsbRow;

    public PreferencesCardView(@NonNull Context context) {
        super(context);

        dimmPaint.setColor(Color.BLACK);
        outlinePaint.setStyle(Paint.Style.STROKE);
        outlinePaint.setStrokeWidth(ViewUtils.dp(2f));
        outlinePaint.setColor(ViewUtils.resolveColor(getContext(), R.attr.cardOutlineColor));
        bgPaint.setColor(ViewUtils.resolveColor(getContext(), android.R.attr.windowBackground));

        LinearLayout ll = new LinearLayout(context);
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.setGravity(Gravity.CENTER_HORIZONTAL);

        header = new LinearLayout(context);
        header.setPadding(ViewUtils.dp(21), 0, ViewUtils.dp(21), 0);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        ImageView iv = new ImageView(context);
        iv.setImageResource(R.drawable.ic_chevron_up_outline_28);
        iv.setLayoutParams(new LinearLayout.LayoutParams(ViewUtils.dp(24), ViewUtils.dp(24)));
        iv.setColorFilter(ViewUtils.resolveColor(context, android.R.attr.textColorSecondary));
        header.addView(iv);

        title = new TextView(context);
        title.setText(R.string.settings);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        title.setTextColor(ViewUtils.resolveColor(context, android.R.attr.textColorSecondary));
        title.setTypeface(ViewUtils.getTypeface(ViewUtils.ROBOTO_MEDIUM));
        title.setGravity(Gravity.CENTER);
        title.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        header.addView(title);

        iv = new ImageView(context);
        iv.setImageResource(R.drawable.ic_chevron_up_outline_28);
        iv.setLayoutParams(new LinearLayout.LayoutParams(ViewUtils.dp(24), ViewUtils.dp(24)));
        iv.setColorFilter(ViewUtils.resolveColor(context, android.R.attr.textColorSecondary));
        header.addView(iv);

        ll.addView(header, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewUtils.dp(64)));

        updateRows();
        listView = new RecyclerView(context);
        listView.setLayoutManager(new LinearLayoutManager(context));
        listView.setAdapter(new RecyclerView.Adapter() {
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
                            header.setText(R.string.camera);
                        } else if (position == usbHeaderRow) {
                            header.setText(R.string.usb);
                        } else if (position == generalHeaderRow) {
                            header.setText(R.string.general);
                        }
                        break;
                    case VIEW_TYPE_SWITCH:
                        PreferenceSwitchView switchView = (PreferenceSwitchView) holder.itemView;
                        if (position == cameraEnabledRow) {
                            switchView.bind(getContext().getString(R.string.enable_camera), null, Prefs.isCameraEnabled());
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
                            pref.bind(getContext().getString(R.string.usb_list), null);
                            pref.setOnClickListener(v -> {
                                UsbManager manager = (UsbManager) KlipperApp.INSTANCE.getSystemService(Context.USB_SERVICE);
                                List<String> list = new ArrayList<>();
                                for (UsbDevice dev : manager.getDeviceList().values()) {
                                    Class<? extends UsbSerialDriver> drv = KlipperProbeTable.getInstance().findDriver(dev);
                                    list.add(Integer.toHexString(dev.getVendorId()) + "/" + Integer.toHexString(dev.getProductId()) + " - " + dev.getDeviceName() + (drv != null ? " - " + drv.getName() + "\n" + new File(KlipperApp.INSTANCE.getFilesDir(), "serial/" + UsbSerialManager.getUID(dev)).getAbsolutePath() : ""));
                                }
                                AlertDialog.Builder b = new MaterialAlertDialogBuilder(context).setTitle(R.string.usb_list_title);
                                if (list.isEmpty()) {
                                    b.setMessage(R.string.usb_list_no_devices);
                                } else {
                                    b.setItems(list.toArray(new String[0]), null);
                                }
                                b.setPositiveButton(android.R.string.ok, null).show();
                            });
                        }
                        break;
                    case VIEW_TYPE_PREF_VALUE:
                        PreferenceValueView val = (PreferenceValueView) holder.itemView;
                        if (position == usbNamingRow) {
                            val.bind(KlipperApp.INSTANCE.getString(R.string.usb_device_naming), KlipperApp.INSTANCE.getString(Prefs.getUsbDeviceNaming() == Prefs.USB_DEVICE_NAMING_BY_PATH ? R.string.usb_device_naming_dev_path : R.string.usb_device_naming_vid_pid));
                            val.setOnClickListener(v -> new MaterialAlertDialogBuilder(v.getContext())
                                    .setTitle(R.string.usb_device_naming)
                                    .setItems(new CharSequence[] {
                                            KlipperApp.INSTANCE.getString(R.string.usb_device_naming_dev_path),
                                            KlipperApp.INSTANCE.getString(R.string.usb_device_naming_vid_pid)
                                    }, (dialog, which) -> {
                                        Prefs.setUsbDeviceNaming(which);
                                        notifyItemChanged(holder.getAdapterPosition());
                                    }).show());
                        } else if (position == frontendRow) {
                            val.bind(KlipperApp.INSTANCE.getString(R.string.web_frontend), KlipperApp.INSTANCE.getString(Prefs.isMainsailEnabled() ? R.string.mainsail : R.string.fluidd));
                            val.setOnClickListener(v -> new MaterialAlertDialogBuilder(v.getContext())
                                    .setTitle(R.string.web_frontend)
                                    .setItems(new CharSequence[] {
                                            KlipperApp.INSTANCE.getString(R.string.fluidd),
                                            KlipperApp.INSTANCE.getString(R.string.mainsail)
                                    }, (dialog, which) -> {
                                        Prefs.setMainsailEnabled(which == 1);
                                        notifyItemChanged(holder.getAdapterPosition());
                                    }).show());
                        } else if (position == remoteServiceRow) {
                            String svcName;
                            switch (Prefs.getRemoteControl()) {
                                default:
                                    svcName = KlipperApp.INSTANCE.getString(R.string.remote_service_none);
                                    break;
                                case TELEGRAM_BOT:
                                    svcName = KlipperApp.INSTANCE.getString(R.string.remote_service_telegram_bot);
                                    break;
                            }

                            val.bind(KlipperApp.INSTANCE.getString(R.string.remote_service), svcName);
                            val.setOnClickListener(v -> new MaterialAlertDialogBuilder(v.getContext())
                                    .setTitle(R.string.remote_service)
                                    .setItems(new CharSequence[] {
                                            KlipperApp.INSTANCE.getString(R.string.remote_service_none),
                                            KlipperApp.INSTANCE.getString(R.string.remote_service_telegram_bot)
                                    }, (dialog, which) -> {
                                        Prefs.setRemoteControl(BaseRemoteControlService.RemoteControlService.values()[which]);
                                        new MaterialAlertDialogBuilder(getContext())
                                                .setTitle(R.string.remote_service)
                                                .setMessage(R.string.remote_config_notice)
                                                .setPositiveButton(android.R.string.ok, null)
                                                .show();
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
                } else if (position == cameraHeaderRow || position == usbHeaderRow || position == generalHeaderRow) {
                    return VIEW_TYPE_HEADER;
                } else if (position == listUsbRow) {
                    return VIEW_TYPE_PREFERENCE;
                } else if (position == usbNamingRow || position == frontendRow || position == remoteServiceRow) {
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

    public RecyclerView getListView() {
        return listView;
    }

    private void updateRows() {
        itemsCount = 0;
        generalHeaderRow = itemsCount++;
        frontendRow = itemsCount++;
        remoteServiceRow = itemsCount++;
        cameraHeaderRow = itemsCount++;
        cameraEnabledRow = itemsCount++;
        usbHeaderRow = itemsCount++;
        usbNamingRow = itemsCount++;
        listUsbRow = itemsCount++;
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
        listView.setAlpha(progress);
        for (int i = 0; i < getChildCount(); i++) {
            getChildAt(i).setTranslationY(ViewUtils.lerp(getHeight() - ViewUtils.dp(MIN_HEIGHT_DP) - getPaddingTop() - getPaddingBottom(), 0, progress));
        }
    }

    public void setProgress(float progress) {
        this.progress = progress;
        invalidateProgress();
        invalidate();
    }
}
