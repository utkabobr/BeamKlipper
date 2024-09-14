package ru.ytkab0bp.beamklipper;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class BundleInstaller {
    public static void init(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences("installation", 0);
        AssetManager assets = ctx.getAssets();
        try {
            PackageManager pm = ctx.getPackageManager();
            PackageInfo info = pm.getPackageInfo(ctx.getPackageName(), 0);
            String ver = readString(assets, "bundle_version") + "_beam-" + info.versionName;

            File root = ctx.getFilesDir();
            if (!prefs.getString("version", "").equals(ver)) {
                JSONObject index = new JSONObject(readString(assets, "index.json"));
                unpack(assets, index, root, "klipper");
                unpack(assets, index, root, "moonraker");

                prefs.edit().putString("version", ver).apply();
            }

            File nativeDir = new File(info.applicationInfo.nativeLibraryDir);
            File lib = new File(nativeDir, "libklippy_chelper.so");

            String str = readString(assets, "klipper/klippy/chelper/__init__.py");
            str = str.replace("${DEST_LIB}", lib.getAbsolutePath());
            if (!prefs.getString("native_lib", "").equals(lib.getAbsolutePath())) {
                FileOutputStream fos = new FileOutputStream(new File(root, "klipper/klippy/chelper/__init__.py"));
                fos.write(str.getBytes(StandardCharsets.UTF_8));
                fos.close();

                prefs.edit().putString("native_lib", lib.getAbsolutePath()).apply();
            }

            str = readString(assets, "moonraker/moonraker/utils/sysfs_devs.py");
            str = str.replace("TTY_PATH = \"/sys/class/tty\"", "TTY_PATH = \"" + new File(KlipperApp.INSTANCE.getFilesDir(), "serial").getAbsolutePath() + "\"");
            FileOutputStream fos = new FileOutputStream(new File(root, "moonraker/moonraker/utils/sysfs_devs.py"));
            fos.write(str.getBytes(StandardCharsets.UTF_8));
            fos.close();

            str = readString(assets, "klipper/klippy/extras/resonance_tester.py");
            str = str.replace("${TEMP_PATH}", new File(KlipperApp.INSTANCE.getCacheDir(), "resonances").getAbsolutePath());
            fos = new FileOutputStream(new File(root, "klipper/klippy/extras/resonance_tester.py"));
            fos.write(str.getBytes(StandardCharsets.UTF_8));
            fos.close();

            str = readString(assets, "klipper/klippy/mcu.py");
            str = str.replace("${TTY_PATH}", "'" + new File(KlipperApp.INSTANCE.getFilesDir(), "serial").getAbsolutePath() + "'");
            fos = new FileOutputStream(new File(root, "klipper/klippy/mcu.py"));
            fos.write(str.getBytes(StandardCharsets.UTF_8));
            fos.close();
        } catch (IOException | JSONException | PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /** @noinspection ResultOfMethodCallIgnored*/
    private static void deleteRecur(File f) {
        if (f.isDirectory()) {
            for (File c : f.listFiles()) {
                deleteRecur(c);
            }
        }
        f.delete();
    }

    private static void unpack(AssetManager assets, JSONObject index, File root, String key) throws IOException {
        File dir = new File(root, key);
        deleteRecur(dir);

        JSONArray arr = index.optJSONArray(key);
        byte[] buffer = new byte[10240]; int c;
        for (int i = 0; i < arr.length(); i++) {
            String file = arr.optString(i);
            File into = new File(dir, file);
            into.getParentFile().mkdirs();

            InputStream in = assets.open(key + "/" + file);
            FileOutputStream fos = new FileOutputStream(into);
            while ((c = in.read(buffer)) != -1) {
                fos.write(buffer, 0, c);
            }
            in.close();
            fos.close();
        }
    }

    public static String readString(AssetManager assets, String key) throws IOException {
        InputStream in = assets.open(key);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buffer = new byte[10240]; int c;
        while ((c = in.read(buffer)) != -1) {
            bos.write(buffer, 0, c);
        }
        in.close();
        bos.close();

        return bos.toString();
    }
}
