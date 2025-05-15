package ru.ytkab0bp.beamklipper;

import android.app.ActivityManager;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.content.ContextCompat;

public class PermissionsChecker {
    public final static boolean ENABLE_NOTIFICATIONS_CHANNEL_CHECK = false;
    private static boolean ignoreNotificationsChannel;

    public static void setIgnoreNotificationsChannel(boolean ignoreNotificationsChannel) {
        PermissionsChecker.ignoreNotificationsChannel = ignoreNotificationsChannel;
    }

    public static boolean ignoreNotificationsChannel() {
        return ignoreNotificationsChannel;
    }

    public static boolean hasNotificationPerm() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.O || ContextCompat.checkSelfPermission(KlipperApp.INSTANCE, "android.permission.POST_NOTIFICATIONS") == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean isNotificationsChannelHidden() {
        if (!ENABLE_NOTIFICATIONS_CHANNEL_CHECK) {
            return true;
        }
        NotificationManager notificationManager = (NotificationManager) KlipperApp.INSTANCE.getSystemService(Context.NOTIFICATION_SERVICE);
        return ignoreNotificationsChannel || Build.VERSION.SDK_INT < Build.VERSION_CODES.O || notificationManager.getNotificationChannel(KlipperApp.SERVICES_CHANNEL) != null && notificationManager.getNotificationChannel(KlipperApp.SERVICES_CHANNEL).getImportance() == NotificationManager.IMPORTANCE_NONE;
    }

    public static boolean hasBatteryPerm() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.P || !((ActivityManager) KlipperApp.INSTANCE.getSystemService(Context.ACTIVITY_SERVICE)).isBackgroundRestricted();
    }

    public static boolean isNotBrokenBySDCard() {
        PackageManager pm = KlipperApp.INSTANCE.getPackageManager();
        try {
            ApplicationInfo info = pm.getApplicationInfo(KlipperApp.INSTANCE.getPackageName(), 0);
            return (info.flags & ApplicationInfo.FLAG_EXTERNAL_STORAGE) == 0;
        } catch (PackageManager.NameNotFoundException ignored) {}

        return true;
    }

    public static boolean needBlockStart() {
        return !hasNotificationPerm() || !hasBatteryPerm() || !isNotBrokenBySDCard() || !isNotificationsChannelHidden();
    }
}
