package ru.ytkab0bp.beamklipper.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;

import ru.ytkab0bp.beamklipper.KlipperApp;
import ru.ytkab0bp.beamklipper.R;
import ru.ytkab0bp.beamklipper.serial.HostSerial;

public class HostSerialService extends Service {
    private final static int ID = 500000;

    private NotificationManager notificationManager;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        Notification.Builder not = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? new Notification.Builder(this, KlipperApp.SERVICES_CHANNEL) : new Notification.Builder(this));
        not.setContentTitle(getString(R.string.host_mcu_title))
                .setContentText(getString(R.string.host_mcu_description))
                .setSmallIcon(android.R.drawable.sym_def_app_icon)
                .setOngoing(true);
        notificationManager.notify(ID, not.build());
        startForeground(ID, not.build());
        return new Binder();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        HostSerial.startThread();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        android.os.Process.killProcess(android.os.Process.myPid());
    }
}
