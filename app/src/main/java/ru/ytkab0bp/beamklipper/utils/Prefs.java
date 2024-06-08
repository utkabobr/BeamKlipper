package ru.ytkab0bp.beamklipper.utils;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.preference.PreferenceManager;

import ru.ytkab0bp.beamklipper.KlipperApp;

public class Prefs {
    private static SharedPreferences mPrefs;

    public static void init(Context ctx) {
        mPrefs = PreferenceManager.getDefaultSharedPreferences(ctx);
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
}
