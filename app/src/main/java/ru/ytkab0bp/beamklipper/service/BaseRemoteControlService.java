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

import ru.ytkab0bp.beamklipper.BundleInstaller;
import ru.ytkab0bp.beamklipper.KlipperApp;
import ru.ytkab0bp.beamklipper.KlipperInstance;
import ru.ytkab0bp.beamklipper.R;
import ru.ytkab0bp.beamklipper.utils.Prefs;

public class BaseRemoteControlService extends BasePythonService {
    private final static int BASE_ID = 500000;

    private int index;

    public BaseRemoteControlService(int num) {
        index = num;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        IBinder b = super.onBind(intent);
        Notification.Builder not = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? new Notification.Builder(this, KlipperApp.SERVICES_CHANNEL) : new Notification.Builder(this));
        not.setContentTitle(getString(R.string.remote_service_title, getInstance().name))
                .setContentText(getString(R.string.remote_service_description))
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
            String tag = Prefs.getRemoteControl().name().toLowerCase();
            File logs = new File(inst.getPublicDirectory(), "logs/" + tag + ".log");
            logs.getParentFile().mkdirs();
            File config = new File(inst.getPublicDirectory(), "config");
            File configFile = new File(config, "remote-" + tag + ".cfg");

            switch (Prefs.getRemoteControl()) {
                case NONE:
                    throw new IllegalArgumentException("Why am I even running?");
                case TELEGRAM_BOT:
                    if (!configFile.exists()) {
                        FileOutputStream fos = new FileOutputStream(configFile);
                        fos.write(BundleInstaller.readString(KlipperApp.INSTANCE.getAssets(), "moonraker_telegram_bot/default.conf").getBytes(StandardCharsets.UTF_8));
                        fos.close();
                    }

                    runPython(new File(KlipperApp.INSTANCE.getFilesDir(), "moonraker_telegram_bot"), "bootstrap", "main.py", "-c", configFile.getAbsolutePath(), "-l", logs.getAbsolutePath());
                    break;
            }
        } catch (Exception e) {
            Log.e("remote_" + index, "Failed to start remote service", e);
        }
    }

    public enum RemoteControlService {
        NONE, TELEGRAM_BOT
    }
}
