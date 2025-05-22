package ru.ytkab0bp.beamklipper;

import android.util.Log;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import cz.msebera.android.httpclient.Header;
import ru.ytkab0bp.beamklipper.cloud.CloudController;
import ru.ytkab0bp.beamklipper.events.BeamServerDataUpdatedEvent;
import ru.ytkab0bp.beamklipper.utils.Prefs;

public class BeamServerData {
    private final static String TAG = "BeamServerData";
    // Using same API endpoint so it's always up-to-date
    private final static String DATA_URL = "https://beam3d.ru/slicebeam.php?act=get_data";
    private final static String RUSSIA_CHECK_URL = "https://beam3d.ru/check_russia.txt";
    private static AsyncHttpClient client = new AsyncHttpClient();

    public static BeamServerData SERVER_DATA;

    static {
        client.setUserAgent(String.format(Locale.ROOT, "BeamKlipper/%s-%d", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE));
        client.setEnableRedirects(true);
        client.setLoggingEnabled(false);
    }

    public List<String> boostySubscribers = new ArrayList<>();

    public BeamServerData(JSONObject obj) {
        JSONArray arr = obj.optJSONArray("boosty_subscribers");
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                boostySubscribers.add(arr.optString(i));
            }
        }
    }

    public static boolean isBoostyAvailable() {
        return !BuildConfig.IS_GOOGLE_PLAY || Prefs.isRussianIP();
    }

    public static boolean isCloudAvailable() {
        return isBoostyAvailable() && CloudController.hasAccountFeatures();
    }

    public static void load() {
        client.get(DATA_URL, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                String str = new String(responseBody, StandardCharsets.UTF_8);
                Prefs.setBeamServerData(str);
                Prefs.setLastCheckedInfo();

                try {
                    BeamServerData.SERVER_DATA = new BeamServerData(new JSONObject(str));
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }

                // Disable Boosty only for Google Play builds on non-Russian IP's
                if (BuildConfig.IS_GOOGLE_PLAY) {
                    client.get(RUSSIA_CHECK_URL, new AsyncHttpResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                            setIsRussia(new String(responseBody).equals("true"));
                        }

                        @Override
                        public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                            setIsRussia(false);
                        }

                        private void setIsRussia(boolean v) {
                            Prefs.setRussianIP(v);
                            KlipperApp.EVENT_BUS.fireEvent(new BeamServerDataUpdatedEvent());
                        }
                    });
                } else {
                    KlipperApp.EVENT_BUS.fireEvent(new BeamServerDataUpdatedEvent());
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Log.e(TAG, "Failed to update server data", error);
            }
        });
    }
}
