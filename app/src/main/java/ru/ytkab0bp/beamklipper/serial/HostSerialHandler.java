package ru.ytkab0bp.beamklipper.serial;

import android.util.Log;

import java.io.File;
import java.util.Arrays;

import ru.ytkab0bp.beamklipper.KlipperApp;

/**
 * Work in progress - this feature is required to support pseudo-GPIO controls
 */
public class HostSerialHandler implements KlipperMessageBlockConstants {
    private final static String TAG = "beam_host_mcu";

    private static NativeSerialPort hostPort;
    private static byte[] acknak = new byte[MESSAGE_MIN];
    private static byte nextSequence = MESSAGE_DEST;

    public HostSerialHandler() {
        hostPort = new NativeSerialPort(new File(KlipperApp.INSTANCE.getFilesDir(), "beam_host_mcu"));
        hostPort.setProxy(data -> {
            Log.d(TAG, Arrays.toString(data) + " | " + new String(data));
            new Runnable() {
                @Override
                public void run() {
                    if (findCommand(data, 0)) {
                        if (popCount < data.length) {
                            run();
                        }
                    }
                }
            }.run();
        });
    }

    private static int getCRC(byte[] buf, int offset, int len) {
        int crc = 0xffff;

        for (int i = offset; i < len; i++) {
            int data = buf[i];
            data ^= (crc & 0xff);
            data ^= (data & 0x0f) << 4;
            crc = ((data << 8) | (crc >> 8)) ^ (data >> 4) ^ (data << 3);
        }

        return crc;
    }

    public void release() {
        hostPort.release();
    }

    private static int syncState;
    private static int popCount;
    private static boolean findCommand(byte[] buf, int offset) {
        if ((syncState & CF_NEED_SYNC) != 0) {
            onSync(buf, offset);
            return true;
        }
        if (buf.length < MESSAGE_MIN) {
            popCount = 0;
            return false;
        }
        byte msglen = buf[offset + MESSAGE_POS_LEN];
        if (msglen < MESSAGE_MIN || msglen > MESSAGE_MAX) {
            onError(buf, offset);
            return true;
        }
        byte msgseq = buf[offset + MESSAGE_POS_SEQ];
        if ((msgseq & ~MESSAGE_SEQ_MASK) != MESSAGE_DEST) {
            onError(buf, offset);
            return true;
        }
        if (buf.length < msglen) {
            popCount = 0;
            return false;
        }
        int a = (buf[offset + msglen - MESSAGE_TRAILER_CRC + 1]), b = buf[offset + msglen - MESSAGE_TRAILER_CRC];
        if (a < 0) a += 256;
        if (b < 0) b += 256;
        b = b << 8;
        int msgcrc = a | b;
        int crc = getCRC(buf, offset, offset + msglen - MESSAGE_TRAILER_SIZE);
        if (crc != msgcrc) {
            Log.d(TAG, "CRC mismatch " + Integer.toHexString(msgcrc) + " != " + Integer.toHexString(crc));
            onError(buf, offset);
            return true;
        }
        syncState &= ~CF_NEED_VALID;
        popCount = offset + msglen;
        if (msgseq != nextSequence) {
            // Lost message
            sendAck();
            return true;
        }
        nextSequence = (byte) (((msgseq + 1) & MESSAGE_SEQ_MASK) | MESSAGE_DEST);
        return true;
    }

    private static void onSync(byte[] buf, int offset) {
        Log.d(TAG, "On sync");
        int j = -1;
        for (int i = offset; i < buf.length; i++) {
            if (buf[i] == MESSAGE_SYNC) {
                j = i;
                break;
            }
        }
        if (j > 0) {
            syncState &= ~CF_NEED_SYNC;
            popCount = j + 1;
        } else {
            popCount = buf.length;
        }

        if ((syncState & CF_NEED_VALID) != 0) {
            return;
        }
        syncState |= CF_NEED_VALID;
        sendAck();
    }

    private static void onError(byte[] buf, int offset) {
        if (buf[offset] == MESSAGE_SYNC) {
            popCount = 1;
            findCommand(buf, offset + 1);
            return;
        }
        Log.d(TAG, "On error " + Arrays.toString(buf));
        syncState |= CF_NEED_SYNC;
        onSync(buf, offset);
    }

    private static void sendAck() {
        acknak[MESSAGE_POS_LEN] = MESSAGE_MIN;
        acknak[MESSAGE_POS_SEQ] = nextSequence;
        int crc = getCRC(acknak, 0, acknak.length - MESSAGE_TRAILER_SIZE);
        acknak[acknak.length - MESSAGE_TRAILER_CRC] = (byte) (crc >> 8);
        acknak[acknak.length - MESSAGE_TRAILER_CRC + 1] = (byte) (crc & 0xFF);
        acknak[acknak.length - MESSAGE_TRAILER_SYNC] = MESSAGE_SYNC;
        Log.d(TAG, "Send ack " + Arrays.toString(acknak));
        hostPort.write(acknak, acknak.length);
    }

    private static int parseInt(byte[] data, int offset) {
        offset++;
        byte c = data[offset];
        int v = c & 0x7f;
        if ((c & 0x60) == 0x60) {
            v |= -0x20;
        }
        while ((c & 0x80) != 0) {
            offset++;
            c = data[offset];
            v = (v << 7) | (c & 0x7f);
        }
        return v;
    }
}
