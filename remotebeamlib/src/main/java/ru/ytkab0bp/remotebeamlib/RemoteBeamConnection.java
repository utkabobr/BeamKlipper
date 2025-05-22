package ru.ytkab0bp.remotebeamlib;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.handshake.ServerHandshake;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public class RemoteBeamConnection {
    private final static String TAG = "remote_beam";
    private final static String REMOTE_ENDPOINT = "wss://api.beam3d.ru/ws/server/%1$s";
    private final static Pattern API_PATTERN = Pattern.compile("^/(printer|api|access|machine|server)/");

    private final String token;
    private final String frontendUrl;
    private final String moonrakerBaseUrl;
    private boolean connectRequested;
    private WebSocketClient webSocket;
    private EventListener eventListener;
    private int failedCounter;

    private Map<String, WebSocket> clientMap = new HashMap<>();

    public RemoteBeamConnection(String token, String frontendUrl, String moonrakerBaseUrl, EventListener eventListener) {
        this.token = token;
        this.frontendUrl = frontendUrl;
        this.moonrakerBaseUrl = moonrakerBaseUrl;
        this.eventListener = eventListener;
    }

    public void connect() {
        if (connectRequested) return;
        connectRequested = true;

        try {
            webSocket = new WebSocketClient(new URI(String.format(REMOTE_ENDPOINT, token))) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    RemoteBeam.getPlatform().logD(TAG, token + ": connected");
                    eventListener.onConnected(RemoteBeamConnection.this);
                    failedCounter = 0;
                }

                @Override
                public void onMessage(String message) {
                    JsonObject obj = JsonParser.parseString(message).getAsJsonObject();
                    if (obj.has("beam_action")) {
                        switch (obj.get("beam_action").getAsString()) {
                            case "web_request": {
                                String path = obj.get("path").getAsString();
                                byte[] data = RemoteBeam.getPlatform().decodeBase64(obj.get("payload").getAsString());
                                String method = obj.get("method").getAsString();
                                String uuid = obj.get("uuid").getAsString();
                                String requestMime = obj.has("mime_type") ? obj.get("mime_type").getAsString() : null;
                                RemoteBeam.getPlatform().scheduleNetwork(() -> {
                                    try {
                                        String url;
                                        if (API_PATTERN.matcher(path).find()) {
                                            url = "http://" + moonrakerBaseUrl + path;
                                        } else {
                                            url = frontendUrl + path;
                                        }

                                        URL u = new URL(url);
                                        HttpURLConnection con = (HttpURLConnection) u.openConnection();
                                        con.setRequestMethod(method);
                                        if (requestMime != null) {
                                            con.setRequestProperty("Content-Type", requestMime);
                                        }
                                        if (Objects.equals(method, "POST")) {
                                            con.setDoOutput(true);

                                            OutputStream out = con.getOutputStream();
                                            out.write(data);
                                            out.flush();
                                            out.close();
                                        }

                                        String mime = con.getHeaderField("Content-Type");
                                        if (mime == null) mime = "text/plain";

                                        InputStream in = con.getResponseCode() >= 200 && con.getResponseCode() < 300 ? con.getInputStream() : con.getErrorStream();
                                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                                        byte[] buffer = new byte[10240];
                                        int c;
                                        while ((c = in.read(buffer)) != -1) {
                                            out.write(buffer, 0, c);
                                        }
                                        in.close();
                                        out.close();

                                        JsonObject r = new JsonObject();
                                        r.addProperty("beam_action", "web_response");
                                        r.addProperty("uuid", uuid);
                                        r.addProperty("status", "OK");
                                        r.addProperty("mime_type", mime);
                                        r.addProperty("content", RemoteBeam.getPlatform().encodeBase64(out.toByteArray()));
                                        send(r.toString());
                                    } catch (Exception e) {
                                        JsonObject r = new JsonObject();
                                        r.addProperty("beam_action", "web_response");
                                        r.addProperty("uuid", uuid);
                                        r.addProperty("status", "REQUEST_TIMEOUT");
                                        r.addProperty("mime_type", "text/plain");
                                        r.addProperty("content", "Failed to contact moonraker.");
                                        send(r.toString());
                                    }
                                });
                                return;
                            }
                            case "connect":
                                try {
                                    String uuid = obj.get("beam_uuid").getAsString();
                                    WebSocketClient newWs = new WebSocketClient(new URI("ws://" + moonrakerBaseUrl + "/websocket")) {
                                        @Override
                                        public void onOpen(ServerHandshake handshakedata) {}

                                        @Override
                                        public void onMessage(String message) {
                                            if (webSocket == null) {
                                                close();
                                                return;
                                            }
                                            JsonObject obj = JsonParser.parseString(message).getAsJsonObject();
                                            obj.addProperty("beam_uuid", uuid);
                                            webSocket.send(obj.toString());
                                        }

                                        @Override
                                        public void onClose(int code, String reason, boolean remote) {
                                            JsonObject obj = new JsonObject();
                                            obj.addProperty("beam_action", "close");
                                            obj.addProperty("uuid", uuid);
                                            webSocket.send(obj.toString());
                                        }

                                        @Override
                                        public void onError(Exception ex) {
                                            ex.printStackTrace();
                                        }
                                    };
                                    newWs.connectBlocking();
                                    clientMap.put(uuid, newWs);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                return;
                            case "disconnect": {
                                String uuid = obj.get("beam_uuid").getAsString();
                                if (clientMap.containsKey(uuid)) {
                                    WebSocket client = clientMap.remove(uuid);
                                    client.close();
                                }
                                return;
                            }
                        }
                    }

                    if (obj.has("beam_uuid")) {
                        String uuid = obj.get("beam_uuid").getAsString();
                        if (clientMap.containsKey(uuid)) {
                            WebSocket client = clientMap.get(uuid);

                            obj.remove("beam_uuid");
                            client.send(obj.toString());
                        }
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    if (code == CloseFrame.POLICY_VALIDATION) {
                        RemoteBeam.getPlatform().logD(TAG, token + ": Abort because of server rejection");
                        eventListener.onServerRejected(RemoteBeamConnection.this, reason);
                    } else {
                        RemoteBeam.getPlatform().logD(TAG, token + ": Connection seems to be failed, trying to reconnect");
                        failedCounter++;
                        RemoteBeam.getPlatform().schedule(RemoteBeamConnection.this::connect, getBackoffDelay(failedCounter));
                    }
                    eventListener.onDisconnected(RemoteBeamConnection.this);
                    webSocket = null;
                }

                @Override
                public void onError(Exception ex) {
                    eventListener.onError(RemoteBeamConnection.this, ex);
                }
            };
            webSocket.connect();
        } catch (Exception ignored) {}
    }

    public void disconnect() {
        if (!connectRequested) return;
        connectRequested = false;

        if (webSocket != null) {
            webSocket.close();
        }
    }

    private static long getBackoffDelay(int i) {
        if (i == 1) {
            return 10000;
        } else if (i > 1 && i <= 5) {
            return 30000;
        } else {
            return 60000 + i * 5000L;
        }
    }

    public interface EventListener {
        void onConnected(RemoteBeamConnection conn);
        void onError(RemoteBeamConnection conn, Exception e);
        void onServerRejected(RemoteBeamConnection conn, String message);
        void onDisconnected(RemoteBeamConnection conn);
    }
}