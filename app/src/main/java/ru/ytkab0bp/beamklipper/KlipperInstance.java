package ru.ytkab0bp.beamklipper;

import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;

import ru.ytkab0bp.beamklipper.events.InstanceStateChangedEvent;
import ru.ytkab0bp.beamklipper.events.WebStateChangedEvent;
import ru.ytkab0bp.beamklipper.service.BaseKlippyService;
import ru.ytkab0bp.beamklipper.service.BaseMoonrakerService;
import ru.ytkab0bp.beamklipper.service.BasePythonService;
import ru.ytkab0bp.beamklipper.service.CameraService;
import ru.ytkab0bp.beamklipper.service.WebService;
import ru.ytkab0bp.beamklipper.utils.Prefs;
import ru.ytkab0bp.remotebeamlib.RemoteBeamConnection;

public class KlipperInstance {
    public final static int SLOTS_COUNT = 4;
    private final static String TAG = "beam_instance";

    public String name;
    public String id;
    public InstanceIcon icon = InstanceIcon.PRINTER;
    public boolean autostart;
    public String remoteId;
    public String remoteToken;
    private State state = State.IDLE;
    private RemoteBeamConnection remoteBeamConnection;

    private static Map<KlipperInstance, Integer> slots = new HashMap<>();
    private static ServiceConnection webServerConnection;
    private static ServiceConnection cameraServerConnection;
    private Intent klippyIntent;
    private ServiceConnection klippyConnection;
    private boolean klippyConnected;
    private Intent moonrakerIntent;
    private ServiceConnection moonrakerConnection;
    private boolean moonrakerConnected;
    private int slot;

    private static List<KlipperInstance> instances = Collections.emptyList();
    private static Map<String, KlipperInstance> instanceMap = new HashMap<String, KlipperInstance>() {
        @Nullable
        @Override
        public KlipperInstance get(@Nullable Object key) {
            KlipperInstance inst = super.get(key);
            if (inst == null) {
                for (KlipperInstance i : instances) {
                    if (Objects.equals(key, i.id)) {
                        put((String) key, inst = i);
                        break;
                    }
                }
            }
            return inst;
        }
    };

    public static void onInstancesLoadedFromDB(List<KlipperInstance> instances) {
        for (KlipperInstance inst : instances) {
            KlipperInstance was = getInstance(inst.id);
            if (was != null) {
                inst.state = was.state;
                inst.klippyConnection = was.klippyConnection;
                inst.klippyConnected = was.klippyConnected;
                inst.klippyIntent = was.klippyIntent;
                inst.moonrakerConnection = was.moonrakerConnection;
                inst.moonrakerConnected = was.moonrakerConnected;
                inst.moonrakerIntent = was.moonrakerIntent;
                inst.slot = was.slot;
                slots.remove(was);
                slots.put(inst, inst.slot);
            }
        }
        KlipperInstance.instances = instances;
        instanceMap.clear();

        for (KlipperInstance inst : instances) {
            if (inst.autostart && inst.getState() == State.IDLE) {
                inst.start();
            }
        }
    }

    public static KlipperInstance getInstance(String id) {
        return instanceMap.get(id);
    }

    public State getState() {
        return state;
    }

    public static List<KlipperInstance> getInstances() {
        return instances;
    }

    public File getDirectory() {
        return new File(KlipperApp.INSTANCE.getFilesDir(), "instance" + File.separator + id);
    }

    public File getPublicDirectory() {
        return new File(getDirectory(), "public");
    }

    public static boolean hasFreeSlots() {
        return slots.size() < SLOTS_COUNT;
    }

    public void start() {
        if (state != State.IDLE) return;
        notifyStateChanged(State.STARTING);

        if (!getDirectory().exists() && !getDirectory().mkdirs()) {
            Log.w(TAG, String.format("Failed to create instance directory (%s)", id));
            stop();
            return;
        }
        if (!getPublicDirectory().exists() && !getPublicDirectory().mkdirs()) {
            Log.w(TAG, String.format("Failed to create public instance directory (%s)", id));
            stop();
            return;
        }
        File config = new File(getPublicDirectory(), "printer_data");
        if (!config.exists() && !config.mkdirs()) {
            Log.w(TAG, String.format("Failed to create data directory (%s)", id));
            stop();
            return;
        }

        slot = -1;
        if (slots.isEmpty()) {
            slot = 0;
        } else if (slots.size() < SLOTS_COUNT) {
            Collection<Integer> cl = slots.values();
            for (int i = 0; i < SLOTS_COUNT; i++) {
                if (!cl.contains(i)) {
                    slot = i;
                    break;
                }
            }
        } else {
            throw new IllegalStateException("Can't start " + id + ": out of slots");
        }
        slots.put(this, slot);
        try {
            klippyIntent = new Intent(KlipperApp.INSTANCE, Class.forName("ru.ytkab0bp.beamklipper.service.KlippyService_" + slot));
            klippyIntent.putExtra(BasePythonService.KEY_INSTANCE, id);
            KlipperApp.INSTANCE.bindService(klippyIntent, klippyConnection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    klippyConnected = true;

                    if (moonrakerConnected) {
                        notifyStateChanged(State.RUNNING);
                    }
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {}
            }, Context.BIND_AUTO_CREATE);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        try {
            moonrakerIntent = new Intent(KlipperApp.INSTANCE, Class.forName("ru.ytkab0bp.beamklipper.service.MoonrakerService_" + slot));
            moonrakerIntent.putExtra(BasePythonService.KEY_INSTANCE, id);
            KlipperApp.INSTANCE.bindService(moonrakerIntent, moonrakerConnection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    moonrakerConnected = true;

                    if (klippyConnected) {
                        notifyStateChanged(State.RUNNING);
                    }
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {}
            }, Context.BIND_AUTO_CREATE);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        if (remoteId != null) {
            try {
                File f = new File(getPublicDirectory(), "config/moonraker.conf");
                String s = BaseMoonrakerService.readString(f);
                Matcher m = BaseMoonrakerService.MOONRAKER_PORT_PATTERN.matcher(s);
                if (m.find()) {
                    int port = Integer.parseInt(m.group(1));
                    remoteBeamConnection = new RemoteBeamConnection(remoteToken, "http://127.0.0.1:8888", "127.0.0.1:" + port, new RemoteBeamConnection.EventListener() {
                        @Override
                        public void onConnected(RemoteBeamConnection conn) {
                            Log.d(TAG, "Remote connected");
                        }

                        @Override
                        public void onError(RemoteBeamConnection conn, Exception e) {
                            Log.e(TAG, "Remote error", e);
                        }

                        @Override
                        public void onServerRejected(RemoteBeamConnection conn, String message) {
                            Log.d(TAG, "Server rejected: " + message);
                        }

                        @Override
                        public void onDisconnected(RemoteBeamConnection conn) {
                            Log.d(TAG, "Remote disconnected");
                        }
                    });
                    remoteBeamConnection.connect();
                } else {
                    throw new IOException("No match");
                }
            } catch (IOException e) {
                Log.e(TAG, "Failed to parse port", e);
            }
        }
    }

    public static void onCameraConfigChanged(boolean enable) {
        if (cameraServerConnection == null && !slots.isEmpty() && enable) {
            KlipperApp.INSTANCE.bindService(new Intent(KlipperApp.INSTANCE, CameraService.class), cameraServerConnection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {}

                @Override
                public void onServiceDisconnected(ComponentName name) {}
            }, Context.BIND_AUTO_CREATE);
        } else if (cameraServerConnection != null && !enable) {
            KlipperApp.INSTANCE.unbindService(cameraServerConnection);
            KlipperApp.INSTANCE.stopService(new Intent(KlipperApp.INSTANCE, CameraService.class));
            cameraServerConnection = null;
        }
    }

    private void onKlippyUnbound() {
        klippyConnection = null;
        klippyConnected = false;

        if (!moonrakerConnected) {
            notifyStateChanged(State.IDLE);
        }
    }

    private void onMoonrakerUnbound() {
        moonrakerConnection = null;
        moonrakerConnected = false;

        if (!klippyConnected) {
            notifyStateChanged(State.IDLE);
        }
    }

    public void stop() {
        if (state != State.RUNNING) return;
        notifyStateChanged(State.STOPPING);

        NotificationManager nm = (NotificationManager) KlipperApp.INSTANCE.getSystemService(Context.NOTIFICATION_SERVICE);
        if (klippyConnection != null) {
            KlipperApp.INSTANCE.unbindService(klippyConnection);
            KlipperApp.INSTANCE.stopService(klippyIntent);
            onKlippyUnbound();
            nm.cancel(BaseKlippyService.BASE_ID + slot);
        }
        if (moonrakerConnection != null) {
            KlipperApp.INSTANCE.unbindService(moonrakerConnection);
            KlipperApp.INSTANCE.stopService(moonrakerIntent);
            onMoonrakerUnbound();
            nm.cancel(BaseMoonrakerService.BASE_ID + slot);
        }
        if (remoteBeamConnection != null) {
            remoteBeamConnection.disconnect();
            remoteBeamConnection = null;
        }
    }

    private void notifyStateChanged(State state) {
        this.state = state;
        KlipperApp.EVENT_BUS.fireEvent(new InstanceStateChangedEvent(id, state));

        if (state == State.IDLE) {
            slots.remove(this);
            if (slots.isEmpty()) {
                if (webServerConnection != null) {
                    KlipperApp.EVENT_BUS.fireEvent(new WebStateChangedEvent(State.STOPPING));
                    KlipperApp.INSTANCE.unbindService(webServerConnection);
                    KlipperApp.INSTANCE.stopService(new Intent(KlipperApp.INSTANCE, WebService.class));
                    KlipperApp.EVENT_BUS.fireEvent(new WebStateChangedEvent(State.IDLE));
                    webServerConnection = null;
                }
                if (cameraServerConnection != null) {
                    KlipperApp.INSTANCE.unbindService(cameraServerConnection);
                    KlipperApp.INSTANCE.stopService(new Intent(KlipperApp.INSTANCE, CameraService.class));
                    cameraServerConnection = null;
                }
            }
        } else if (state == State.RUNNING) {
            if (webServerConnection == null) {
                KlipperApp.EVENT_BUS.fireEvent(new WebStateChangedEvent(State.STARTING));
                KlipperApp.INSTANCE.bindService(new Intent(KlipperApp.INSTANCE, WebService.class), webServerConnection = new ServiceConnection() {
                    @Override
                    public void onServiceConnected(ComponentName name, IBinder service) {
                        KlipperApp.EVENT_BUS.fireEvent(new WebStateChangedEvent(State.RUNNING));
                    }

                    @Override
                    public void onServiceDisconnected(ComponentName name) {
                    }
                }, Context.BIND_AUTO_CREATE);
            }

            if (Prefs.isCameraEnabled()) {
                if (cameraServerConnection == null) {
                    KlipperApp.INSTANCE.bindService(new Intent(KlipperApp.INSTANCE, CameraService.class), cameraServerConnection = new ServiceConnection() {
                        @Override
                        public void onServiceConnected(ComponentName name, IBinder service) {
                        }

                        @Override
                        public void onServiceDisconnected(ComponentName name) {
                        }
                    }, Context.BIND_AUTO_CREATE);
                }
            }
        }
    }

    public static boolean isWebServerRunning() {
        return webServerConnection != null;
    }

    public enum State {
        IDLE,
        STARTING,
        RUNNING,
        STOPPING
    }
}
