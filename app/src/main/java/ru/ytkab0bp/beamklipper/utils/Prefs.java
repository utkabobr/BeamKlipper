package ru.ytkab0bp.beamklipper.utils;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.preference.PreferenceManager;

import ru.ytkab0bp.beamklipper.KlipperApp;
import ru.ytkab0bp.beamklipper.events.WebFrontendChangedEvent;
import ru.ytkab0bp.beamklipper.serial.UsbSerialManager;

public class Prefs {
    public final static int USB_DEVICE_NAMING_BY_PATH = 0, USB_DEVICE_NAMING_BY_VID_PID = 1;

    private static SharedPreferences mPrefs;

    public static void init(Context ctx) {
        mPrefs = PreferenceManager.getDefaultSharedPreferences(ctx);
    }

    public static void setMainsailEnabled(boolean val) {
        mPrefs.edit().putBoolean("mainsail", val).apply();
        KlipperApp.EVENT_BUS.fireEvent(new WebFrontendChangedEvent());
    }

    public static boolean isMainsailEnabled() {
        return mPrefs.getBoolean("mainsail", false);
    }

    public static int getCameraWidth() {
        return mPrefs.getInt("camera_width", 1280);
    }

    public static int getCameraHeight() {
        return mPrefs.getInt("camera_height", 720);
    }

    public static String getCameraId() {
        return mPrefs.getString("camera_id", null);
    }

    public static boolean isCameraEnabled() {
        return (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || KlipperApp.INSTANCE.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) &&
                mPrefs.getBoolean("camera_enabled", false);
    }

    public static void setCameraEnabled(boolean en) {
        mPrefs.edit().putBoolean("camera_enabled", en).apply();
    }

    public static int getUsbDeviceNaming() {
        return mPrefs.getInt("usb_device_naming", USB_DEVICE_NAMING_BY_PATH);
    }

    public static void setUsbDeviceNaming(int naming) {
        UsbSerialManager.disconnectAll();
        mPrefs.edit().putInt("usb_device_naming", naming).apply();
        UsbSerialManager.connectAll();
    }

    public static boolean isFlashlightEnabled() {
        return mPrefs.getBoolean("flashlight", false);
    }

    public static void setFlashlightEnabled(boolean f) {
        mPrefs.edit().putBoolean("flashlight", f).apply();
    }

    public static boolean isAutofocusEnabled() {
        return mPrefs.getBoolean("autofocus", false);
    }

    public static void setAutofocusEnabled(boolean f) {
        mPrefs.edit().putBoolean("autofocus", f).apply();
    }

    public static float getFocusDistance() {
        return mPrefs.getFloat("focus", 0);
    }

    public static void setFocusDistance(float f) {
        mPrefs.edit().putFloat("focus", f).apply();
    }
}
