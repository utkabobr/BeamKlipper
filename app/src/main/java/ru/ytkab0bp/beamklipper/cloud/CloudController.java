package ru.ytkab0bp.beamklipper.cloud;

import android.util.Log;

import com.google.gson.Gson;

import ru.ytkab0bp.beamklipper.KlipperApp;
import ru.ytkab0bp.beamklipper.KlipperInstance;
import ru.ytkab0bp.beamklipper.events.CloudFeaturesUpdatedEvent;
import ru.ytkab0bp.beamklipper.events.CloudLoginStateUpdatedEvent;
import ru.ytkab0bp.beamklipper.events.CloudNeedQREvent;
import ru.ytkab0bp.beamklipper.events.CloudUserInfoUpdatedEvent;
import ru.ytkab0bp.beamklipper.utils.Prefs;
import ru.ytkab0bp.beamklipper.utils.ViewUtils;
import ru.ytkab0bp.sapil.APICallback;
import ru.ytkab0bp.sapil.APIRequestHandle;

public class CloudController {
    private final static String TAG = "cloud";
    private final static long MIN_SYNC_FEATURES_DELTA = 12 * 60 * 60 * 1000L; // Once in 12 hours

    private static CloudAPI.UserInfo userInfo;
    private static CloudAPI.UserFeatures userFeatures;

    private static boolean isLoggingIn;
    private static APIRequestHandle beginLoginHandle;
    private static String loginSessionId;
    private static Runnable loginAutoCancel = () -> {
        loginSessionId = null;
        isLoggingIn = false;
        KlipperApp.EVENT_BUS.fireEvent(new CloudLoginStateUpdatedEvent());
    };
    private static Runnable loginCheck = new Runnable() {
        @Override
        public void run() {
            CloudAPI.INSTANCE.loginCheck(loginSessionId, new APICallback<CloudAPI.LoginState>() {
                @Override
                public void onResponse(CloudAPI.LoginState response) {
                    if (response.loggedIn) {
                        Prefs.setCloudAPIToken(response.bearer);
                        loadUserInfo();
                        ViewUtils.removeCallbacks(loginAutoCancel);
                    } else if (isLoggingIn) {
                        ViewUtils.postOnMainThread(loginCheck, 5000);
                    }
                }

                @Override
                public void onException(Exception e) {
                    Log.e(TAG, "Failed to check login state", e);

                    if (isLoggingIn) {
                        ViewUtils.postOnMainThread(loginCheck, 5000);
                    }
                }
            });
        }
    };

    private static Gson gson = new Gson();

    public static void initCached() {
        if (Prefs.getCloudCachedUserFeatures() != null) {
            userFeatures = gson.fromJson(Prefs.getCloudCachedUserFeatures(), CloudAPI.UserFeatures.class);
        }
        if (Prefs.getCloudAPIToken() != null && Prefs.getCloudCachedUserInfo() != null) {
            userInfo = gson.fromJson(Prefs.getCloudCachedUserInfo(), CloudAPI.UserInfo.class);
        }
    }

    public static void init() {
        long now = System.currentTimeMillis();
        boolean needSyncInfo = userFeatures == null || now - Prefs.getCloudLastFeaturesSync() > MIN_SYNC_FEATURES_DELTA;
        if (needSyncInfo) {
            checkUserFeatures();
        }

        if (Prefs.getCloudAPIToken() != null) {
            if (needSyncInfo || userInfo == null) {
                loadUserInfo();
            }
        }
    }

    private static void loadUserInfo() {
        CloudAPI.INSTANCE.userGetInfo(new APICallback<CloudAPI.UserInfo>() {
            @Override
            public void onResponse(CloudAPI.UserInfo response) {
                userInfo = response;

                if (userInfo.id.equals("null")) {
                    userInfo = null;
                    Prefs.setCloudAPIToken(null);
                    Prefs.setCloudCachedUserInfo(null);
                } else {
                    Prefs.setCloudCachedUserInfo(gson.toJson(userInfo));
                }
                KlipperApp.EVENT_BUS.fireEvent(new CloudUserInfoUpdatedEvent());
                if (isLoggingIn) {
                    isLoggingIn = false;
                    KlipperApp.EVENT_BUS.fireEvent(new CloudLoginStateUpdatedEvent());
                }
                Prefs.setCloudLastFeaturesSync(System.currentTimeMillis());
            }

            @Override
            public void onException(Exception e) {
                Log.e(TAG, "Failed to get user info", e);
                ViewUtils.postOnMainThread(CloudController::init, 15000);
            }
        });
    }

    public static boolean isLoggingIn() {
        return isLoggingIn;
    }

    private static void beginLogin0() {
        beginLoginHandle = CloudAPI.INSTANCE.loginBegin(new APICallback<CloudAPI.LoginData>() {
            @Override
            public void onResponse(CloudAPI.LoginData response) {
                loginSessionId = response.sessionId;

                ViewUtils.postOnMainThread(loginAutoCancel, response.expiresAt * 1000L - System.currentTimeMillis());
                ViewUtils.postOnMainThread(loginCheck, 5000);
                ViewUtils.postOnMainThread(() -> KlipperApp.EVENT_BUS.fireEvent(new CloudNeedQREvent(response.url)));
            }

            @Override
            public void onException(Exception e) {
                ViewUtils.postOnMainThread(CloudController::beginLogin0, 15000);
            }
        });
    }

    public static void beginLogin() {
        isLoggingIn = true;
        KlipperApp.EVENT_BUS.fireEvent(new CloudLoginStateUpdatedEvent());
        beginLogin0();
    }

    public static void cancelLogin() {
        isLoggingIn = false;
        KlipperApp.EVENT_BUS.fireEvent(new CloudLoginStateUpdatedEvent());
        if (loginSessionId != null) {
            CloudAPI.INSTANCE.loginCancel(loginSessionId, response -> {});
        }
        if (beginLoginHandle != null && beginLoginHandle.isRunning()) {
            beginLoginHandle.cancel();
            beginLoginHandle = null;
        }
        ViewUtils.removeCallbacks(loginCheck);
        ViewUtils.removeCallbacks(loginAutoCancel);
        loginSessionId = null;
    }

    public static void logout() {
        for (KlipperInstance inst : KlipperInstance.getInstances()) {
            if (inst.remoteId != null) {
                CloudAPI.INSTANCE.remoteDeletePrinter(inst.remoteId, response -> {});
                inst.remoteId = null;
                inst.remoteToken = null;
                KlipperApp.DATABASE.update(inst);
            }
        }

        CloudAPI.INSTANCE.logout(response -> {});
        Prefs.setCloudAPIToken(null);
        userInfo = null;
        KlipperApp.EVENT_BUS.fireEvent(new CloudLoginStateUpdatedEvent());
        KlipperApp.EVENT_BUS.fireEvent(new CloudUserInfoUpdatedEvent());
    }

    public static void checkUserFeatures() {
        CloudAPI.INSTANCE.userGetFeatures(new APICallback<CloudAPI.UserFeatures>() {
            @Override
            public void onResponse(CloudAPI.UserFeatures response) {
                userFeatures = response;
                Prefs.setCloudCachedUserFeatures(gson.toJson(userFeatures));
                if (Prefs.getCloudAPIToken() == null) {
                    Prefs.setCloudLastFeaturesSync(System.currentTimeMillis());
                }
                KlipperApp.EVENT_BUS.fireEvent(new CloudFeaturesUpdatedEvent());
            }

            @Override
            public void onException(Exception e) {
                Log.e(TAG, "Failed to get user features", e);
                ViewUtils.postOnMainThread(CloudController::checkUserFeatures, 15000);
            }
        });
    }

    public static CloudAPI.UserInfo getUserInfo() {
        return userInfo;
    }

    public static CloudAPI.UserFeatures getUserFeatures() {
        return userFeatures;
    }

    public static boolean hasAccountFeatures() {
        return userFeatures != null && userFeatures.levels != null && !userFeatures.levels.isEmpty();
    }
}
