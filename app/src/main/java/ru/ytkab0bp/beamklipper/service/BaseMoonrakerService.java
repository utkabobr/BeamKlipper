package ru.ytkab0bp.beamklipper.service;

import android.app.Notification;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.system.Os;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.ytkab0bp.beamklipper.BundleInstaller;
import ru.ytkab0bp.beamklipper.KlipperApp;
import ru.ytkab0bp.beamklipper.KlipperInstance;
import ru.ytkab0bp.beamklipper.R;

public class BaseMoonrakerService extends BasePythonService {
    private final static int BASE_ID = 200000;
    private final static Pattern MOONRAKER_PORT_PATTERN = Pattern.compile("port: (\\d+)");

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
        try {
            KlipperInstance inst = getInstance();
            File logs = new File(inst.getPublicDirectory(), "logs/moonraker.log");
            logs.getParentFile().mkdirs();
            File config = new File(inst.getPublicDirectory(), "config");
            File timelapseOutputDir = new File(inst.getPublicDirectory(), "timelapses");
            File socket = new File(inst.getDirectory(), "klippy_uds");
            File tempFramesDir = new File(inst.getDirectory(), "timelapse_frames");
            File moonSocket = new File(inst.getDirectory(), "moonraker_uds");

            File resonancesLink = new File(config, "beam_resonances");
            File fromResonances = new File(KlipperApp.INSTANCE.getCacheDir(), "resonances");
            if (!resonancesLink.exists()) {
                fromResonances.mkdirs();
                Os.symlink(fromResonances.getAbsolutePath(), resonancesLink.getAbsolutePath());
            }

            File moonrakerCfg = new File(config, "moonraker.conf");
            if (!moonrakerCfg.exists()) {
                moonrakerCfg.getParentFile().mkdirs();
                int freePort = 7125;
                for (KlipperInstance otherInst : KlipperInstance.getInstances()) {
                    File f = new File(otherInst.getPublicDirectory(), "config/moonraker.conf");
                    if (f.exists()) {
                        String str = readString(f);
                        Matcher m = MOONRAKER_PORT_PATTERN.matcher(str);
                        if (m.find()) {
                            int otherPort = Integer.parseInt(m.group(1));
                            if (otherPort == freePort) {
                                freePort++;
                            }
                        }
                    }
                }

                FileOutputStream fos = new FileOutputStream(moonrakerCfg);
                fos.write(BundleInstaller.readString(KlipperApp.INSTANCE.getAssets(), "moonraker/default.conf")
                        .replace("${KLIPPY_UDS}", socket.getAbsolutePath())
                        .replace("${MOONRAKER_PORT}", String.valueOf(freePort))
                        .replace("${TIMELAPSE_FRAME_PATH}", tempFramesDir.getAbsolutePath())
                        .replace("${TIMELAPSE_OUTPUT}", timelapseOutputDir.getAbsolutePath())
                        .getBytes(StandardCharsets.UTF_8));
                fos.close();
            }
            File timelapseCfg = new File(config, "timelapse.cfg");
            if (!timelapseCfg.exists()) {
                FileOutputStream fos = new FileOutputStream(timelapseCfg);
                fos.write(BundleInstaller.readString(KlipperApp.INSTANCE.getAssets(), "moonraker/timelapse.cfg").getBytes(StandardCharsets.UTF_8));
                fos.close();
            }

            runPython(new File(KlipperApp.INSTANCE.getFilesDir(), "moonraker"), "bootstrap", "moonraker.py", "-u", moonSocket.getAbsolutePath(), "-l", logs.getAbsolutePath(), "-d", inst.getPublicDirectory().getAbsolutePath(), "-c", moonrakerCfg.getAbsolutePath());
        } catch (Exception e) {
            Log.e("moonraker_" + index, "Failed to start moonraker", e);
        }
    }

    private static String readString(File file) throws IOException {
        InputStream in = new FileInputStream(file);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buffer = new byte[10240]; int c;
        while ((c = in.read(buffer)) != -1) {
            bos.write(buffer, 0, c);
        }
        in.close();
        bos.close();

        return bos.toString();
    }
}
