package ru.ytkab0bp.beamklipper.serial;

public interface KlipperMessageBlockConstants {
    int MESSAGE_MIN = 5;
    int MESSAGE_MAX = 64;
    int MESSAGE_HEADER_SIZE = 2;
    int MESSAGE_TRAILER_SIZE = 3;
    int MESSAGE_POS_LEN = 0;
    int MESSAGE_POS_SEQ = 1;
    int MESSAGE_TRAILER_CRC = 3;
    int MESSAGE_TRAILER_SYNC = 1;
    int MESSAGE_PAYLOAD_MAX = MESSAGE_MAX - MESSAGE_MIN;
    int MESSAGE_SEQ_MASK = 0x0f;
    int MESSAGE_DEST = 0x10;
    int MESSAGE_SYNC = 0x7E;

    int CF_NEED_SYNC = 1;
    int CF_NEED_VALID = 1<<1;
}
