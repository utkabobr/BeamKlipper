package ru.ytkab0bp.beamklipper;

import android.annotation.SuppressLint;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

import ru.ytkab0bp.beamklipper.cloud.CloudController;
import ru.ytkab0bp.beamklipper.db.BeamDB;
import ru.ytkab0bp.beamklipper.cloud.AndroidPlatform;
import ru.ytkab0bp.beamklipper.serial.UsbSerialManager;
import ru.ytkab0bp.beamklipper.utils.Prefs;
import ru.ytkab0bp.beamklipper.utils.ViewUtils;
import ru.ytkab0bp.eventbus.EventBus;
import ru.ytkab0bp.remotebeamlib.RemoteBeam;

public class KlipperApp extends Application {
    public final static String PERMISSION = BuildConfig.APPLICATION_ID + ".permission.INTERNAL_BROADCASTS";
    public final static String SERVICES_CHANNEL = "services";

    public static KlipperApp INSTANCE;
    public static BeamDB DATABASE;
    public static EventBus EVENT_BUS = EventBus.newBus("main");
    public static boolean hasUpdateInfo;

    @Override
    public void onCreate() {
        super.onCreate();
        INSTANCE = this;
        Prefs.init(this);
        DATABASE = new BeamDB(this);
        KlipperInstance.onInstancesLoadedFromDB(DATABASE.getInstances());
        EventBus.registerImpl(this);
        BundleInstaller.init(this);
        RemoteBeam.init(new AndroidPlatform());
        CloudController.initCached();
        CloudController.init();

        try {
            getAssets().open("update.json").close();
            hasUpdateInfo = true;
        } catch (IOException e) {
            hasUpdateInfo = false;
        }
        try {
            BeamServerData.SERVER_DATA = new BeamServerData(new JSONObject(Prefs.getBeamServerData()));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        if (System.currentTimeMillis() - Prefs.getLastCheckedInfo() >= 86400000L) {
            ViewUtils.postOnMainThread(BeamServerData::load);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            nm.createNotificationChannel(new NotificationChannel(SERVICES_CHANNEL, KlipperApp.INSTANCE.getString(R.string.ServicesChannel), NotificationManager.IMPORTANCE_LOW));
        }

        if (Objects.equals(getProcessNameCompat(), getPackageName())) {
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
