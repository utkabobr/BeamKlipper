package ru.ytkab0bp.beamklipper.utils;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import org.kamranzafar.jtar.TarEntry;
import org.kamranzafar.jtar.TarHeader;
import org.kamranzafar.jtar.TarOutputStream;
import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.XZOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import ru.ytkab0bp.beamklipper.KlipperApp;
import ru.ytkab0bp.beamklipper.KlipperInstance;
import ru.ytkab0bp.beamklipper.R;

public class LogUploader {
    private final static String TAG = "logs_uploader";
    private static OkHttpClient httpClient = new OkHttpClient.Builder().followRedirects(false).build();

    /** @noinspection OctalInteger*/
    public static void uploadLogs(KlipperInstance instance) {
        File dir = instance.getPublicDirectory();
        File klippyLog = new File(dir, "logs/klippy.log");
        File moonrakerLog = new File(dir, "logs/moonraker.log");

        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            XZOutputStream xz = new XZOutputStream(bos, new LZMA2Options());
            TarOutputStream tar = new TarOutputStream(xz);
            byte[] buffer = new byte[10240];
            int c;
            if (klippyLog.exists()) {
                tar.putNextEntry(new TarEntry(new TarHeader() {{
                    mode = 0100644;
                    name.append("klippy.log");
                    size = klippyLog.length();
                }}));
                FileInputStream fis = new FileInputStream(klippyLog);
                while ((c = fis.read(buffer)) != -1) {
                    tar.write(buffer, 0, c);
                }
                fis.close();
            }
            if (moonrakerLog.exists()) {
                tar.putNextEntry(new TarEntry(new TarHeader() {{
                    mode = 0100644;
                    name.append("moonraker.log");
                    size = moonrakerLog.length();
                }}));
                FileInputStream fis = new FileInputStream(moonrakerLog);
                while ((c = fis.read(buffer)) != -1) {
                    tar.write(buffer, 0, c);
                }
                fis.close();
            }

            tar.close();
            xz.close();
            bos.close();

            httpClient.newCall(new Request.Builder()
                            .url("https://coderus.openrepos.net/klipper_logs/upload")
                            .post(new MultipartBody.Builder()
                                    .setType(MultipartBody.MIXED)
                                    .addFormDataPart("tarfile", "logs.tar.xz", RequestBody.create(MediaType.get("application/x-gtar"), bos.toByteArray()))
                                    .build())
                            .build())
                    .enqueue(new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            Log.e(TAG, "Failed to upload logs", e);
                            ViewUtils.postOnMainThread(() -> Toast.makeText(KlipperApp.INSTANCE, R.string.upload_failed, Toast.LENGTH_SHORT).show());
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            if (response.headers().get("Location") != null) {
                                ViewUtils.postOnMainThread(() -> {
                                    Toast.makeText(KlipperApp.INSTANCE, R.string.upload_success, Toast.LENGTH_SHORT).show();
                                    String loc = response.headers().get("Location");
                                    if (!loc.startsWith("https://")) {
                                        loc = "https://coderus.openrepos.net" + loc;
                                    }
                                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(loc));
                                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    KlipperApp.INSTANCE.startActivity(i);

                                    ClipboardManager clipboard = (ClipboardManager) KlipperApp.INSTANCE.getSystemService(Context.CLIPBOARD_SERVICE);
                                    ClipData clip = ClipData.newPlainText("beam", loc);
                                    clipboard.setPrimaryClip(clip);
                                });
                                response.close();
                            } else {
                                response.close();
                                onFailure(call, new IOException("Not a redirect"));
                            }
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "Failed to pack logs", e);
        }
    }
}
