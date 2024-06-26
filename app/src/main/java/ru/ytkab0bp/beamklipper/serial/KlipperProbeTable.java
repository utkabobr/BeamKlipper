package ru.ytkab0bp.beamklipper.serial;

import com.hoho.android.usbserial.driver.CdcAcmSerialDriver;
import com.hoho.android.usbserial.driver.Ch34xSerialDriver;
import com.hoho.android.usbserial.driver.ProbeTable;
import com.hoho.android.usbserial.driver.UsbSerialProber;

public class KlipperProbeTable extends ProbeTable {
    private static ProbeTable mInstance;

    private KlipperProbeTable() {}

    public static ProbeTable getInstance() {
        if (mInstance == null) {
            mInstance = UsbSerialProber.getDefaultProbeTable();
            mInstance.addProduct(0x1D50, 0x614E, CdcAcmSerialDriver.class);
            mInstance.addProduct(0xDD8, 0x3701, Ch34xSerialDriver.class);
        }
        return mInstance;
    }
}
