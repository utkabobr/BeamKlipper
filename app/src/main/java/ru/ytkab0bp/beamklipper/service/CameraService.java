package ru.ytkab0bp.beamklipper.service;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;
import android.util.Range;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.ytkab0bp.beamklipper.KlipperApp;
import ru.ytkab0bp.beamklipper.R;
import ru.ytkab0bp.beamklipper.utils.Prefs;

@SuppressLint("MissingPermission")
public class CameraService extends Service {
    private final static String TAG = "beam_camera";
    private final static Pattern PATH_PATTERN = Pattern.compile("GET ([^\\r\\n]+) HTTP/1\\.[0-1]");

    private final static int PORT = 8889;
    private final static int ID = 400000;

    private NotificationManager notificationManager;

    private static List<CameraHandlerThread> handlerThreads = new CopyOnWriteArrayList<>();

    private ServerThread serverThread;
    private CameraManager cameraManager;
    private HandlerThread cameraThread;
    private Handler cameraHandler;
    private CameraCaptureSession captureSession;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        Notification.Builder not = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? new Notification.Builder(this, KlipperApp.SERVICES_CHANNEL) : new Notification.Builder(this));
        not.setContentTitle(getString(R.string.camera_title))
                .setContentText(getString(R.string.camera_description))
                .setSmallIcon(android.R.drawable.sym_def_app_icon)
                .setOngoing(true);
        notificationManager.notify(ID, not.build());
        startForeground(ID, not.build());
        return new Binder();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        cameraThread = new HandlerThread("camera");
        cameraThread.start();
        cameraHandler = new Handler(cameraThread.getLooper());
        cameraHandler.post(()-> android.os.Process.setThreadPriority(-10));

        serverThread = new ServerThread();
        serverThread.start();

        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            String id = cameraManager.getCameraIdList()[0];
            cameraManager.openCamera(Prefs.getCameraId() != null ? Prefs.getCameraId() : id, new CameraDevice.StateCallback() {
                byte[] buffer;

                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    try {
                        List<Surface> targets = new ArrayList<>();
                        ImageReader reader = ImageReader.newInstance(Prefs.getCameraWidth(), Prefs.getCameraHeight(), ImageFormat.JPEG, 3);
                        reader.setOnImageAvailableListener(r -> {
                            Image img = r.acquireLatestImage();
                            if (img == null) return;
                            ByteBuffer buf = img.getPlanes()[0].getBuffer();
                            if (buffer == null || buffer.length < buf.limit()) {
                                buffer = new byte[buf.limit()];
                            }
                            buf.position(0);
                            buf.get(buffer, 0, buf.limit());

                            deliverFrame(buffer, buf.limit());

                            img.close();
                        }, cameraHandler);
                        targets.add(reader.getSurface());
                        camera.createCaptureSession(targets, new CameraCaptureSession.StateCallback() {
                            @Override
                            public void onConfigured(@NonNull CameraCaptureSession session) {
                                Log.d(TAG, "Configured");

                                captureSession = session;

                                try {
                                    CaptureRequest.Builder builder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

                                    CameraCharacteristics chars = cameraManager.getCameraCharacteristics(camera.getId());
                                    Range<Integer>[] ranges = chars.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
                                    Range<Integer> selectedRange = null;
                                    for (Range<Integer> r : ranges) {
                                        if (r.getUpper() < 25) {
                                            selectedRange = r;
                                            break;
                                        }
                                    }

                                    builder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, selectedRange);
                                    builder.addTarget(reader.getSurface());
                                    session.setRepeatingRequest(builder.build(), null, null);
                                } catch (CameraAccessException e) {
                                    throw new RuntimeException(e);
                                }
                            }

                            @Override
                            public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                                Log.d(TAG, "Configure failed");
                            }
                        }, cameraHandler);
                    } catch (CameraAccessException e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    Log.d(TAG, "Disconnected");
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    Log.d(TAG, "Error " + error);
                }
            }, cameraHandler);
        } catch (Exception e) {
            Log.e(TAG, "Failed to open camera");
        }
    }

    private static void deliverFrame(byte[] data, int size) {
        for (CameraHandlerThread t : handlerThreads) {
            t.handler.post(()->{
                try {
                    OutputStream out = t.out;
                    out.write("--camera-frame\r\n".getBytes(StandardCharsets.UTF_8));
                    out.write(("Content-Type: image/jpeg\r\n" +
                               "Content-Length: " + size + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
                    out.write(data, 0, size);
                    out.write("\r\n\r\n".getBytes(StandardCharsets.UTF_8));
                    out.flush();

                    if (t.oneShot) {
                        t.quit();
                    }
                } catch (Exception e) {
                    if (t.socket.isClosed()) {
                        Log.e(TAG, "Failed to deliver frame", e);
                        t.quit();
                    }
                }
            });
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (captureSession != null) {
            captureSession.close();
            captureSession = null;
        }
        for (CameraHandlerThread h : handlerThreads) {
            h.quit();
        }
        handlerThreads.clear();
        serverThread.interrupt();
        cameraThread.quit();
        cameraHandler = null;
        stopForeground(true);
        notificationManager.cancel(ID);

        android.os.Process.killProcess(android.os.Process.myPid());
    }

    private static class ServerThread extends Thread {

        ServerThread() {
            setName("beam_camera_server");
            setDaemon(true);
        }

        @Override
        public void run() {
            try {
                ServerSocket socket = new ServerSocket(PORT);

                while (!isInterrupted()) {
                    Socket sock = socket.accept();
                    new CameraHandlerThread(sock);
                }

                socket.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static class CameraHandlerThread extends HandlerThread {
        private final static String HEADERS = "HTTP/1.0 200 OK\r\n" +
                "Connection: close\r\n" +
                "Max-Age: 0\r\n" +
                "Expires: 0\r\n" +
                "Cache-Control: no-cache, private\r\n" +
                "Pragma: no-cache\r\n" +
                "Content-Type: multipart/x-mixed-replace; boundary=camera-frame\r\n\r\n";

        private Socket socket;
        private OutputStream out;
        private boolean oneShot;
        private Handler handler;

        CameraHandlerThread(Socket sock) throws IOException {
            super("beam_camera_handler");

            socket = sock;
            InputStream in = sock.getInputStream();
            BufferedReader r = new BufferedReader(new InputStreamReader(in));
            String line = r.readLine();
            Matcher m = PATH_PATTERN.matcher(line);
            if (!m.find()) {
                socket.close();
                return;
            }
            String path = m.group(1);
            if (path.startsWith("/snapshot")) {
                oneShot = true;
            }

            out = socket.getOutputStream();

            start();
            handler = new Handler(getLooper());
            handler.post(()->{
                try {
                    out.write(HEADERS.getBytes(StandardCharsets.UTF_8));
                    out.flush();

                    handlerThreads.add(this);
                } catch (Exception e) {
                    Log.e(getName(), "Failed to write headers", e);
                    quit();
                }
            });
        }

        @Override
        public boolean quit() {
            try {
                socket.close();
            } catch (IOException ignored) {}
            handlerThreads.remove(this);

            return super.quit();
        }
    }
}
