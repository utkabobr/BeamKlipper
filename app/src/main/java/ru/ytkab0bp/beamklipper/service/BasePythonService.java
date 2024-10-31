package ru.ytkab0bp.beamklipper.service;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

import java.io.File;
import java.util.List;

import ru.ytkab0bp.beamklipper.KlipperInstance;

public class BasePythonService extends Service {
    public final static String KEY_INSTANCE = "instance";
    private final static String TAG = "python_service";
    private Python py;

    private HandlerThread pythonThread;
    private Handler pythonHandler;

    private KlipperInstance instance;

    protected NotificationManager notificationManager;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        instance = KlipperInstance.getInstance(intent.getStringExtra(KEY_INSTANCE));
        pythonHandler.post(this::onStartPython);
        return new Binder();
    }

    public KlipperInstance getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        android.os.Process.setThreadPriority(-20);
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        pythonThread = new HandlerThread(getClass().getName());
        pythonThread.start();
        pythonHandler = new Handler(pythonThread.getLooper());

        pythonHandler.post(()->{
            android.os.Process.setThreadPriority(-20);
            AndroidPlatform platform = new AndroidPlatform(this);
            do {
                // Hackfix old devices that fail to create paths
                // Idk why it happens, lol
                try {
                    platform.getPath();
                    Python.start(platform);
                    py = Python.getInstance();
                    break;
                } catch (Exception ignored) {}
            } while (true);
        });
    }

    protected Handler getPythonHandler() {
        return pythonHandler;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        pythonThread.quitSafely();
        pythonThread = null;
        pythonHandler = null;
        // We need to kill process as python thread will NOT quit by itself even by request, it's running blocking command
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    protected void onStartPython() {}

    protected void runPython(File dir, String module, String... args) {
        py.getModule("sys").get("path").callAttr("append", dir.getAbsolutePath());

        PyObject pyModule = Python.getInstance().getModule(module);
        List<PyObject> argv = pyModule.get("sys").get("argv").asList();
        argv.clear();
        for (String arg : args) {
            argv.add(PyObject.fromJava(arg));
        }

        try {
            pyModule.callAttr("main");
        } catch (Exception e) {
            Log.e(TAG, "Failed to start " + module, e);
        }
    }
}
