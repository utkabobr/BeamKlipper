package ru.ytkab0bp.beamklipper.service;

import android.app.Notification;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

import ru.ytkab0bp.beamklipper.KlipperApp;
import ru.ytkab0bp.beamklipper.BundleInstaller;
import ru.ytkab0bp.beamklipper.KlipperInstance;
import ru.ytkab0bp.beamklipper.R;

public class BaseMoonrakerService extends BasePythonService {
    private final static int BASE_ID = 200000;

    private int index;

    public BaseMoonrakerService(int num) {
        index = num;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        IBinder b = super.onBind(intent);
        Notification.Builder not = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? new Notification.Builder(this, KlipperApp.SERVICES_CHANNEL) : new Notification.Builder(this));
        not.setContentTitle(getString(R.string.moonraker_title, getInstance().name))
                .setContentText(getString(R.string.moonraker_description))
                .setSmallIcon(android.R.drawable.sym_def_app_icon)
                .setOngoing(true);
        notificationManager.notify(BASE_ID + index, not.build());
        startForeground(BASE_ID + index, not.build());
        return b;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        stopForeground(true);
        notificationManager.cancel(BASE_ID + index);
    }

    @Override
    protected void onStartPython() {
        try {
            KlipperInstance inst = getInstance();
            File logs = new File(inst.getPublicDirectory(), "logs/moonraker.log");
            logs.getParentFile().mkdirs();
            File config = new File(inst.getPublicDirectory(), "config");
            File socket = new File(inst.getDirectory(), "klippy_uds");
            File moonSocket = new File(inst.getDirectory(), "moonraker_uds");

            File moonrakerCfg = new File(config, "moonraker.conf");
            if (!moonrakerCfg.exists()) {
                moonrakerCfg.getParentFile().mkdirs();
                FileOutputStream fos = new FileOutputStream(moonrakerCfg);
                fos.write(BundleInstaller.readString(KlipperApp.INSTANCE.getAssets(), "moonraker/default.conf")
                        .replace("${KLIPPY_UDS}", socket.getAbsolutePath())
                        .getBytes(StandardCharsets.UTF_8));
                fos.close();
            }
            runPython(new File(KlipperApp.INSTANCE.getFilesDir(), "moonraker"), "bootstrap", "moonraker.py", "-u", moonSocket.getAbsolutePath(), "-l", logs.getAbsolutePath(), "-d", inst.getPublicDirectory().getAbsolutePath(), "-c", moonrakerCfg.getAbsolutePath());
        } catch (Exception e) {
            Log.e("moonraker_" + index, "Failed to start moonraker", e);
        }
    }
}
