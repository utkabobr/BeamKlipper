package ru.ytkab0bp.beamklipper.serial;

class SerialNative {
    static {
        System.loadLibrary("serial");
    }

    static native long create(String file, SerialProxy proxy);
    static native void write(long pointer, byte[] data, int len);
    static native void release(long pointer);
}
