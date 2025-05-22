package ru.ytkab0bp.remotebeamlib;

public interface IPlatform {
    void schedule(Runnable r, long delay);
    void scheduleNetwork(Runnable r);
    void logD(String tag, String message);
    String encodeBase64(byte[] data);
    byte[] decodeBase64(String str);
}
