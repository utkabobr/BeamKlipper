package ru.ytkab0bp.beamklipper.serial;

import android.annotation.SuppressLint;
import android.util.Log;

import java.io.File;

import ru.ytkab0bp.beamklipper.KlipperApp;

public class NativeSerialPort implements SerialProxy {
    private final static String TAG = "beam_native_serial";
    private long pointer;
    private SerialProxy proxy;
    private File mFile;

    public NativeSerialPort(String name) {
        this(new File(KlipperApp.INSTANCE.getFilesDir(), "serial/" + name));
    }

    /** @noinspection ResultOfMethodCallIgnored*/
    @SuppressLint({"SetWorldReadable", "SetWorldWritable"})
    public NativeSerialPort(File file) {
        mFile = file;
        mFile.getParentFile().mkdirs();
        if (mFile.exists()) mFile.delete();
        pointer = SerialNative.create(mFile.getAbsolutePath(), this);
        if (pointer == 0) {
            Log.w(TAG, "Failed to open native port at " + mFile.getAbsolutePath());
            return;
        }
        mFile.setReadable(true, false);
        mFile.setWritable(true, false);
    }

    public void write(byte[] data, int len) {
        SerialNative.write(pointer, data, len);
    }

    public File getFile() {
        return mFile;
    }

    public NativeSerialPort setProxy(SerialProxy proxy) {
        this.proxy = proxy;
        return this;
    }

    @Override
    public void onDataReceived(byte[] data) {
        if (proxy != null) {
            proxy.onDataReceived(data);
        }
    }

    public void release() {
        SerialNative.release(pointer);
        mFile.delete();
    }
}
