package ru.ytkab0bp.beamklipper.serial;

import android.annotation.SuppressLint;
import android.app.Application;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.util.Log;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import ru.ytkab0bp.beamklipper.BuildConfig;
import ru.ytkab0bp.beamklipper.KlipperApp;
import ru.ytkab0bp.beamklipper.utils.Prefs;
import ru.ytkab0bp.beamklipper.utils.ViewUtils;

public class UsbSerialManager {
    public final static String ACTION_ON_DEVICE_CONNECTED = BuildConfig.APPLICATION_ID + ".action.DEVICE_CONNECTED";
    public final static int FLAG_RESET_ARDUINO = 1;
    private final static boolean DEBUG = false;
    private final static String TAG = "beam_usb_serial";

    private static Map<String, UsbSerialPort> portMap = new HashMap<>();
    private static Map<String, NativeSerialPort> nativePortMap = new HashMap<>();
    private static Map<String, ReadThread> readThreadMap = new HashMap<>();
    private static UsbManager mUsbManager;
    private static BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Objects.equals(intent.getAction(), UsbManager.ACTION_USB_DEVICE_ATTACHED) || Objects.equals(intent.getAction(), ACTION_ON_DEVICE_CONNECTED)) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null) {
                    if (mUsbManager.hasPermission(device)) {
                        UsbSerialProber prober = new UsbSerialProber(KlipperProbeTable.getInstance());
                        UsbSerialDriver drv = prober.probeDevice(device);
                        if (drv != null && !drv.getPorts().isEmpty()) {
                            connect(drv);
                        }
                    } else {
                        if (Objects.equals(intent.getAction(), ACTION_ON_DEVICE_CONNECTED) && !intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                            if (DEBUG) {
                                Log.d(TAG, "Failed to acquire usb permission");
                            }
                            return;
                        }

                        if (DEBUG) {
                            Log.d(TAG, "Failed to connect, no permission: " + getUID(device));
                        }
                    }
                }
            } else if (Objects.equals(intent.getAction(), UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null) {
                    close(getUID(device));
                }
            }
        }
    };

    /** @noinspection ResultOfMethodCallIgnored*/
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    public static void init(Application ctx) {
        File f = new File(KlipperApp.INSTANCE.getFilesDir(), "serial");
        if (f.exists()) {
            for (File c : f.listFiles()) {
                if (c.delete()) {
                    if (DEBUG) {
                        Log.d(TAG, "Deleted old " + c.getAbsolutePath());
                    }
                }
            }
        } else f.mkdirs();

        mUsbManager = (UsbManager) ctx.getSystemService(Context.USB_SERVICE);
        IntentFilter filter = new IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        filter.addAction(ACTION_ON_DEVICE_CONNECTED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ctx.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            ctx.registerReceiver(receiver, filter);
        }

        connectAll();
    }

    public static void connectAll() {
        UsbSerialProber prober = new UsbSerialProber(KlipperProbeTable.getInstance());
        UsbManager manager = (UsbManager) KlipperApp.INSTANCE.getSystemService(Context.USB_SERVICE);
        for (UsbSerialDriver drv : prober.findAllDrivers(manager)) {
            if (!drv.getPorts().isEmpty()) {
                connect(drv);
            }
        }
    }

    public static void disconnectAll() {
        for (String uid : portMap.keySet()) {
            close(uid);
        }
    }

    public static String getUID(UsbDevice device) {
        switch (Prefs.getUsbDeviceNaming()) {
            default:
            case Prefs.USB_DEVICE_NAMING_BY_PATH:
                return device.getDeviceName().replace("/", "_");
            case Prefs.USB_DEVICE_NAMING_BY_VID_PID:
                return Integer.toHexString(device.getVendorId()) + "_" + Integer.toHexString(device.getProductId());
        }
    }

    public static void connect(UsbSerialDriver drv) {
        connect(drv, 0);
    }

    public static void connect(UsbSerialDriver drv, int flags) {
        if (!mUsbManager.hasPermission(drv.getDevice())) {
            mUsbManager.requestPermission(drv.getDevice(), PendingIntent.getBroadcast(KlipperApp.INSTANCE, 0, new Intent(UsbSerialManager.ACTION_ON_DEVICE_CONNECTED).setPackage(KlipperApp.INSTANCE.getPackageName()), PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_NO_CREATE));
            return;
        }

        boolean resetArduino = (flags & FLAG_RESET_ARDUINO) != 0;
        AtomicInteger currentBaudRate = new AtomicInteger(250000);
        UsbSerialPort port = drv.getPorts().get(0);
        try {
            port.open(mUsbManager.openDevice(drv.getDevice()));
            port.setRTS(true);
            if (resetArduino) {
                port.setDTR(true);
            }
            port.setParameters(currentBaudRate.get(), UsbSerialPort.DATABITS_8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
        } catch (Exception e) {
            Log.e(TAG, "Failed to open device " + drv.getDevice(), e);
            return;
        }

        String uid = getUID(drv.getDevice());
        NativeSerialPort nativePort = new NativeSerialPort(uid);
        nativePort.setProxy((data) -> {
            try {
                port.write(data, 0);
                port.setDTR(false);
                port.setRTS(false);
                if (DEBUG) {
                    Log.d(TAG, "Write " + Arrays.toString(data));
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to write to USB serial", e);
            }
        });
        ReadThread thread = new ReadThread(uid, port, nativePort);
        ViewUtils.postOnMainThread(thread::start);

        nativePortMap.put(uid, nativePort);
        portMap.put(uid, port);
        readThreadMap.put(uid, thread);
        if (DEBUG) {
            Log.d(TAG, "Connected " + nativePort.getFile());
        }
    }

    public static UsbDevice getDevice(String uid) {
        UsbSerialPort port = portMap.get(uid);
        if (port == null) return null;
        return port.getDevice();
    }

    public static void close(String uid) {
        NativeSerialPort nativePort = nativePortMap.remove(uid);
        UsbSerialPort port = portMap.remove(uid);
        ReadThread thread = readThreadMap.remove(uid);
        if (nativePort != null) {
            nativePort.release();
        }
        if (port != null) {
            try {
                port.close();
            } catch (IOException e) {
                Log.e(TAG, "Failed to close USB serial port", e);
            }
        }
        if (thread != null) {
            thread.interrupt();
        }
    }

    private static class ReadThread extends Thread {
        final String uid;
        final UsbSerialPort port;
        final NativeSerialPort nativePort;
        byte[] buffer = new byte[4096];

        private ReadThread(String uid, UsbSerialPort port, NativeSerialPort nativePort) {
            this.uid = uid;
            this.port = port;
            this.nativePort = nativePort;
            android.os.Process.setThreadPriority(-20);
        }

        @Override
        public void run() {
            while (!isInterrupted()) {
                try {
                    int c = port.read(buffer, 0);
                    if (c > 0) {
                        nativePort.write(buffer, c);
                        if (DEBUG) {
                            Log.d(TAG, "Read " + Arrays.toString(buffer));
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error reading serial", e);
                    if (e instanceof IOException) {
                        close(uid);
                    }
                }
            }
        }
    }
}
