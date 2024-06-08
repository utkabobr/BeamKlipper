package ru.ytkab0bp.beamklipper.serial;

public interface SerialProxy {
    void onDataReceived(byte[] data);
}
