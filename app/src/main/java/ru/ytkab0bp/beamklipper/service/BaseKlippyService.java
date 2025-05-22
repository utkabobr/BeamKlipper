package ru.ytkab0bp.beamklipper.service;

import android.app.Notification;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.ytkab0bp.beamklipper.BundleInstaller;
import ru.ytkab0bp.beamklipper.KlipperApp;
import ru.ytkab0bp.beamklipper.KlipperInstance;
import ru.ytkab0bp.beamklipper.R;

public class BaseKlippyService extends BasePythonService {
    public final static int BASE_ID = 100000;

    /** @noinspection FieldCanBeLocal*/
    private int index;

    public BaseKlippyService(int num) {
        index = num;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        IBinder b = super.onBind(intent);
        Notification.Builder not = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? new Notification.Builder(this, KlipperApp.SERVICES_CHANNEL) : new Notification.Builder(this));
        not.setContentTitle(getString(R.string.KlippyTitle, getInstance().name))
                .setContentText(getString(R.string.KlippyDescription))
                .setSmallIcon(R.drawable.icon_adaptive_foreground)
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
        KlipperInstance inst = getInstance();
        try {
            File logs = new File(inst.getPublicDirectory(), "logs/klippy.log");
            File config = new File(inst.getPublicDirectory(), "config");
            File socket = new File(inst.getDirectory(), "klippy_uds");
            File virtualInput = new File(inst.getDirectory(), "vinput");
            virtualInput.createNewFile();

            logs.getParentFile().mkdirs();
            File printerCfg = new File(config, "printer.cfg");
            try {
                FileInputStream fis = new FileInputStream(printerCfg);
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                byte[] buffer = new byte[10240]; int c;
                while ((c = fis.read(buffer)) != -1) {
                    bos.write(buffer, 0, c);
                }

                bos.close();
                fis.close();

                String str = bos.toString();
                boolean changed = false;

                Pattern pattern = Pattern.compile("\\[virtual_sdcard][\\r\\n ]+path: ([^\\r\\n]+)", Pattern.DOTALL);
                Matcher m = pattern.matcher(str);
                if (m.find()) {
                    String path = m.group(1);
                    if (!path.startsWith(inst.getPublicDirectory().getAbsolutePath())) {
                        str = str.substring(0, m.start()) + str.substring(m.end() + 1);
                    }
                }
                if (!str.contains("[virtual_sdcard]")) {
                    str += "\n[virtual_sdcard]\npath: " + new File(inst.getPublicDirectory(), "gcodes").getAbsolutePath() + "\n";
                    changed = true;
                }
                if (changed) {
                    FileOutputStream fos = new FileOutputStream(printerCfg);
                    fos.write(str.getBytes(StandardCharsets.UTF_8));
                    fos.close();
                }

                File beeperCfg = new File(config, "beam_beeper.cfg");
                if (!beeperCfg.exists()) {
                    FileOutputStream fos = new FileOutputStream(beeperCfg);
                    fos.write(BundleInstaller.readString(KlipperApp.INSTANCE.getAssets(), "klipper/beam_beeper.cfg").getBytes(StandardCharsets.UTF_8));
                    fos.close();
                }
            } catch (Exception ignored) {}
            runPython(new File(KlipperApp.INSTANCE.getFilesDir(), "klipper/klippy"), "klippy", "klippy.py", "-B", virtualInput.getAbsolutePath(), "-l", logs.getAbsolutePath(), "-a", socket.getAbsolutePath(), printerCfg.getAbsolutePath());
        } catch (Exception e) {
            Log.e("klippy_" + index, "Failed to start klippy", e);
        }
    }
}
