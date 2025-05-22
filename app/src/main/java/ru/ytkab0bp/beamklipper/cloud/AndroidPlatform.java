package ru.ytkab0bp.beamklipper.cloud;

import android.util.Base64;
import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ru.ytkab0bp.beamklipper.utils.ViewUtils;
import ru.ytkab0bp.remotebeamlib.IPlatform;

public class AndroidPlatform implements IPlatform {
    private final static ExecutorService IO_POOL = Executors.newCachedThreadPool();

    @Override
    public void schedule(Runnable r, long delay) {
        ViewUtils.postOnMainThread(r, delay);
    }

    @Override
    public void scheduleNetwork(Runnable r) {
        IO_POOL.submit(r);
    }

    @Override
    public void logD(String tag, String message) {
        Log.d(tag, message);
    }

    @Override
    public String encodeBase64(byte[] data) {
        return Base64.encodeToString(data, Base64.NO_WRAP);
    }

    @Override
    public byte[] decodeBase64(String str) {
        return Base64.decode(str, 0);
    }
}
