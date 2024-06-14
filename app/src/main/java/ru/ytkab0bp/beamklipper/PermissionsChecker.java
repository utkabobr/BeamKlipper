package ru.ytkab0bp.beamklipper;

import android.app.ActivityManager;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.content.ContextCompat;

public class PermissionsChecker {
    public static boolean hasNotificationPerm() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.O || ContextCompat.checkSelfPermission(KlipperApp.INSTANCE, "android.permission.POST_NOTIFICATIONS") == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean isNotificationsChannelHidden() {
        NotificationManager notificationManager = (NotificationManager) KlipperApp.INSTANCE.getSystemService(Context.NOTIFICATION_SERVICE);
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.O || notificationManager.getNotificationChannel(KlipperApp.SERVICES_CHANNEL) != null && notificationManager.getNotificationChannel(KlipperApp.SERVICES_CHANNEL).getImportance() == NotificationManager.IMPORTANCE_NONE;
    }

    public static boolean hasBatteryPerm() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.P || !((ActivityManager) KlipperApp.INSTANCE.getSystemService(Context.ACTIVITY_SERVICE)).isBackgroundRestricted();
    }

    public static boolean needBlockStart() {
        return !hasNotificationPerm() || !hasBatteryPerm() || !isNotificationsChannelHidden();
    }
}
