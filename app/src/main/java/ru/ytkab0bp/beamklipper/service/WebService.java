package ru.ytkab0bp.beamklipper.service;

import static org.nanohttpd.protocols.http.response.Response.newChunkedResponse;
import static org.nanohttpd.protocols.http.response.Response.newFixedLengthResponse;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;

import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.NanoHTTPD;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.response.Status;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.ytkab0bp.beamklipper.KlipperApp;
import ru.ytkab0bp.beamklipper.R;

public class WebService extends Service {
    public final static int PORT = 8888;
    private final static int ID = 300000;

    private final static Pattern API_PATTERN = Pattern.compile("^/(printer|api|access|machine|server)/");
    private static SharedPreferences mPrefs;
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ROOT);
    private HttpServer httpServer = new HttpServer();

    private NotificationManager notificationManager;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        Notification.Builder not = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? new Notification.Builder(this, KlipperApp.SERVICES_CHANNEL) : new Notification.Builder(this));
        not.setContentTitle(getString(R.string.web_title))
                .setContentText(getString(R.string.web_description))
                .setSmallIcon(android.R.drawable.sym_def_app_icon)
                .setOngoing(true);
        notificationManager.notify(ID, not.build());
        startForeground(ID, not.build());
        return new Binder();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mPrefs = KlipperApp.INSTANCE.getSharedPreferences("web", 0);
        try {
            httpServer.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        httpServer.stop();

        stopForeground(true);
        notificationManager.cancel(ID);
    }

    private static class HttpServer extends NanoHTTPD {

        public HttpServer() {
            super(PORT);
        }

        private Response serveStatic(String path) {
            Context ctx = KlipperApp.INSTANCE;
            if (path.equals("/")) path = "/index.html";

            try {
                String mimeType = "text/plain";
                if (path.endsWith(".js")) {
                    mimeType = "text/javascript";
                } else if (path.endsWith(".html")) {
                    mimeType = "text/html";
                } else if (path.endsWith(".css")) {
                    mimeType = "text/css";
                }
                InputStream in = ctx.getAssets().open("fluidd" + path);
                Response response = newChunkedResponse(Status.OK, mimeType, in);
                response.addHeader("Date", dateFormat.format(new Date()));
                response.addHeader("Last-Modified", getLastModifiedString());
                response.addHeader("Cache-Control", "max-age=604800");
                return response;
            } catch (IOException e) {
                return newFixedLengthResponse(Status.NOT_FOUND, "text/plain", "Not Found");
            }
        }

        private String getLastModifiedString() {
            long lastModified = mPrefs.getLong("last_modified", System.currentTimeMillis());
            return dateFormat.format(new Date(lastModified));
        }

        @Override
        public Response serve(IHTTPSession session) {
            Matcher m = API_PATTERN.matcher(session.getUri());
            if (m.find()) {
                try {
                    HttpURLConnection con = (HttpURLConnection) new URL("http://127.0.0.1:7125/" + session.getUri().substring(1)).openConnection();
                    return newChunkedResponse(Status.OK, con.getContentType(), con.getInputStream());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            if (session.getUri().startsWith("/index.html") || session.getUri().equals("/")) {
                return serveStatic("/");
            } else {
                return serveStatic(session.getUri());
            }
        }
    }
}
