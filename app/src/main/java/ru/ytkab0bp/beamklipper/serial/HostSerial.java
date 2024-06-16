package ru.ytkab0bp.beamklipper.serial;

import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import java.io.File;

import ru.ytkab0bp.beamklipper.KlipperApp;

public class HostSerial {
    private final static String TAG = "beam_host_mcu";
    private static HandlerThread thread;

    static {
        System.loadLibrary("beam_host_mcu");
    }

    private static native int runNative(String path);

    public static void startThread() {
        thread = new HandlerThread(TAG);
        thread.start();
        new Handler(thread.getLooper()).post(() -> {
            android.os.Process.setThreadPriority(-20);
            Log.d(TAG, "Starting host MCU...");
            int code = runNative(new File(KlipperApp.INSTANCE.getFilesDir(), "beam_host_mcu").getAbsolutePath());
            if (code < 0) {
                Log.e(TAG, "Failed to start host MCU");
            }
            KlipperApp.INSTANCE.sendBroadcast(new Intent(KlipperApp.ACTION_NEED_HOST_MCU_RESTART), KlipperApp.PERMISSION);

            // Kill process to cleanup all the stuff
            android.os.Process.killProcess(android.os.Process.myPid());
        });
    }
}
