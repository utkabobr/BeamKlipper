package ru.ytkab0bp.beamklipper;

import android.annotation.SuppressLint;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

import ru.ytkab0bp.beamklipper.db.BeamDB;
import ru.ytkab0bp.beamklipper.serial.HostSerialHandler;
import ru.ytkab0bp.beamklipper.serial.UsbSerialManager;
import ru.ytkab0bp.beamklipper.utils.Prefs;
import ru.ytkab0bp.eventbus.EventBus;

public class KlipperApp extends Application {
    public final static String PERMISSION = BuildConfig.APPLICATION_ID + ".permission.INTERNAL_BROADCASTS";
    public final static String SERVICES_CHANNEL = "services";

    public static KlipperApp INSTANCE;
    public static BeamDB DATABASE;
    public static EventBus EVENT_BUS = EventBus.newBus("main");

    private static HostSerialHandler hostSerialHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        INSTANCE = this;
        Prefs.init(this);
        DATABASE = new BeamDB(this);
        KlipperInstance.onInstancesLoadedFromDB(DATABASE.getInstances());
        EventBus.registerImpl(this);
        BundleInstaller.init(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            nm.createNotificationChannel(new NotificationChannel(SERVICES_CHANNEL, KlipperApp.INSTANCE.getString(R.string.channel_services), NotificationManager.IMPORTANCE_LOW));
        }

        if (Objects.equals(getProcessNameCompat(), getPackageName())) {
//            hostSerialHandler = new HostSerialHandler();
            UsbSerialManager.init(this);
        }
    }

    @SuppressLint("DiscouragedPrivateApi")
    private static String getProcessNameCompat() {
        if (Build.VERSION.SDK_INT >= 28)
            return Application.getProcessName();

        // Using the same technique as Application.getProcessName() for older devices
        // Using reflection since ActivityThread is an internal API

        try {
            @SuppressLint("PrivateApi")
            Class<?> activityThread = Class.forName("android.app.ActivityThread");

            String methodName = "currentProcessName";

            Method getProcessName = activityThread.getDeclaredMethod(methodName);
            return (String) getProcessName.invoke(null);
        } catch (ClassNotFoundException | InvocationTargetException | IllegalAccessException |
                 NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
