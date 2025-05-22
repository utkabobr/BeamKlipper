package ru.ytkab0bp.beamklipper.utils;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.preference.PreferenceManager;

import ru.ytkab0bp.beamklipper.BuildConfig;
import ru.ytkab0bp.beamklipper.KlipperApp;
import ru.ytkab0bp.beamklipper.events.WebFrontendChangedEvent;
import ru.ytkab0bp.beamklipper.serial.UsbSerialManager;

public class Prefs {
    public final static int USB_DEVICE_NAMING_BY_PATH = 0, USB_DEVICE_NAMING_BY_VID_PID = 1;

    private static SharedPreferences mPrefs;

    public static void init(Context ctx) {
        mPrefs = PreferenceManager.getDefaultSharedPreferences(ctx);
    }

    public static String getLastCommit() {
        return mPrefs.getString("last_commit", null);
    }

    public static void setLastCommit() {
        mPrefs.edit().putString("last_commit", BuildConfig.COMMIT).apply();
    }

    public static String getCloudAPIToken() {
        return mPrefs.getString("cloud_api_token", null);
    }

    public static void setCloudAPIToken(String token) {
        SharedPreferences.Editor e = mPrefs.edit();
        if (token == null) {
            e.remove("cloud_api_token");
        } else {
            e.putString("cloud_api_token", token);
        }
        e.apply();
    }

    public static String getCloudCachedUserFeatures() {
        return mPrefs.getString("cloud_cached_user_features", null);
    }

    public static void setCloudCachedUserFeatures(String features) {
        SharedPreferences.Editor e = mPrefs.edit();
        if (features == null) {
            e.remove("cloud_cached_user_features");
        } else {
            e.putString("cloud_cached_user_features", features);
        }
        e.apply();
    }

    public static long getCloudLastFeaturesSync() {
        return mPrefs.getLong("cloud_last_features_sync", 0);
    }

    public static void setCloudLastFeaturesSync(long ls) {
        mPrefs.edit().putLong("cloud_last_features_sync", ls).apply();
    }

    public static long getCloudLastSync() {
        return mPrefs.getLong("cloud_last_sync", 0);
    }

    public static void setCloudLastSync(long ls) {
        mPrefs.edit().putLong("cloud_last_sync", ls).apply();
    }

    public static long getCloudLocalLastSentModified() {
        return mPrefs.getLong("cloud_local_last_sent_modified", 0);
    }

    public static void setCloudLocalLastSentModified(long lm) {
        mPrefs.edit().putLong("cloud_local_last_sent_modified", lm).apply();
    }

    public static long getCloudLocalLastModified() {
        return mPrefs.getLong("cloud_local_last_modified", 0);
    }

    public static void setCloudLocalLastModified(long lm) {
        mPrefs.edit().putLong("cloud_local_last_modified", lm).apply();
    }

    public static long getCloudRemoteLastModified() {
        return mPrefs.getLong("cloud_remote_last_modified", 0);
    }

    public static void setCloudRemoteLastModified(long lm) {
        mPrefs.edit().putLong("cloud_remote_last_modified", lm).apply();
    }

    public static String getCloudCachedUserInfo() {
        return mPrefs.getString("cloud_cached_user_info", null);
    }

    public static void setCloudCachedUserInfo(String info) {
        SharedPreferences.Editor e = mPrefs.edit();
        if (info == null) {
            e.remove("cloud_cached_user_info");
        } else {
            e.putString("cloud_cached_user_info", info);
        }
        e.apply();
    }

    public static long getLastCheckedInfo() {
        return mPrefs.getLong("last_checked_info", 0);
    }

    public static void setLastCheckedInfo() {
        mPrefs.edit().putLong("last_checked_info", System.currentTimeMillis()).apply();
    }

    // Only used for displaying Boosty info, nothing more
    public static boolean isRussianIP() {
        return mPrefs.getBoolean("russian_ip", false);
    }

    public static void setRussianIP(boolean v) {
        mPrefs.edit().putBoolean("russian_ip", v).apply();
    }

    public static void setBeamServerData(String data) {
        mPrefs.edit().putString("beam_server_data", data).apply();
    }

    public static String getBeamServerData() {
        return mPrefs.getString("beam_server_data", "{}");
    }

    public static void setMainsailEnabled(boolean val) {
        mPrefs.edit().putBoolean("mainsail", val).apply();
        KlipperApp.EVENT_BUS.fireEvent(new WebFrontendChangedEvent());
    }

    public static boolean isMainsailEnabled() {
        return mPrefs.getBoolean("mainsail", true);
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
