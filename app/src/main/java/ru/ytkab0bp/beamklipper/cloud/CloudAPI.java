package ru.ytkab0bp.beamklipper.cloud;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.ytkab0bp.beamklipper.BuildConfig;
import ru.ytkab0bp.beamklipper.utils.Prefs;
import ru.ytkab0bp.sapil.APICallback;
import ru.ytkab0bp.sapil.APILibrary;
import ru.ytkab0bp.sapil.APIRequestHandle;
import ru.ytkab0bp.sapil.APIRunner;
import ru.ytkab0bp.sapil.Arg;
import ru.ytkab0bp.sapil.Method;

public interface CloudAPI extends APIRunner {
    CloudAPI INSTANCE = APILibrary.newRunner(CloudAPI.class, new RunnerConfig() {
        private final Map<String, String> headers = new HashMap<>();

        @Override
        public String getBaseURL() {
            return "https://api.beam3d.ru/v1/";
        }

        @Override
        public String getDefaultUserAgent() {
            return "BeamKlipper v" + BuildConfig.VERSION_NAME + "/" + BuildConfig.VERSION_CODE;
        }

        @Override
        public Map<String, String> getDefaultHeaders() {
            headers.clear();
            if (Prefs.getCloudAPIToken() != null) {
                headers.put("Authorization", "Bearer " + Prefs.getCloudAPIToken());
            }
            return headers;
        }
    });

    /**
     * Begins login flow, returns auth link
     */
    @Method("login/begin")
    APIRequestHandle loginBegin(APICallback<LoginData> callback);

    /**
     * Checks new login state by session id
     */
    @Method("login/check")
    void loginCheck(@Arg("sessionId") String sessionId, APICallback<LoginState> callback);

    /**
     * Cancels login flow
     */
    @Method("login/cancel")
    void loginCancel(@Arg("sessionId") String sessionId, APICallback<Boolean> callback);

    /**
     * Gets current user info
     * <p>
     * Requires authorization
     */
    @Method("user/getInfo")
    void userGetInfo(APICallback<UserInfo> callback);

    /**
     * Gets user features
     */
    @Method("user/getFeatures")
    void userGetFeatures(APICallback<UserFeatures> callback);

    /**
     * Gets list of remote printers
     * <p>
     * Requires authorization
     */
    @Method("remote/getPrinters")
    void remoteGetPrinters(APICallback<List<RemotePrinter>> callback);

    /**
     * Creates new remote printer
     * @param name Name of the printer
     * <p>
     * Requires authorization
     */
    @Method("remote/createPrinter")
    void remoteCreatePrinter(@Arg("name") String name, APICallback<RemotePrinter> callback);

    /**
     * Deletes remote printer
     * @param id Printer id
     * <p>
     * Requires authorization
     */
    @Method("remote/deletePrinter")
    void remoteDeletePrinter(@Arg("id") String id, APICallback<Boolean> callback);

    /**
     * Destroys token
     * <p>
     * Requires authorization
     */
    @Method("logout")
    void logout(APICallback<Boolean> callback);

    final class LoginData {
        /**
         * Url that should be clicked by the user to authorize
         */
        public String url;

        /**
         * Session identifier
         */
        public String sessionId;

        /**
         * Time at which session should be considered expired if not logged in
         */
        public long expiresAt;
    }

    final class LoginState {
        /**
         * If user is now logged in
         */
        public boolean loggedIn;

        /**
         * Bearer token if auth was successful
         */
        public String bearer;
    }

    final class UserFeatures {
        /**
         * Which level is required for early access
         */
        public int earlyAccessLevel;

        /**
         * Which level is required for data sync
         */
        public int syncRequiredLevel;

        /**
         * Which level is required for AI model generator
         */
        public int aiGeneratorRequiredLevel;

        /**
         * Models per month max
         */
        public int aiGeneratorModelsPerMonth;

        /**
         * Level required for remote access
         */
        public int remoteAccessLevel;

        /**
         * How many printers you can use with access
         */
        public int remoteAccessPrintersLimit;

        /**
         * Url at which user should be redirected for info about how to restore a subscription
         */
        public String alreadySubscribedInfoUrl;

        /**
         * List of subscription levels
         */
        public List<SubscriptionLevel> levels = new ArrayList<>();
    }

    final class SubscriptionLevel {
        /**
         * Int representation
         */
        public int level;

        /**
         * Title of this level
         */
        public String title;

        /**
         * Price of this level
         */
        public String price;

        /**
         * Url at which user should be redirected for purchase
         */
        public String subscribeOrUpgradeUrl;

        /**
         * Url at which user should be redirected for managing the subscription
         */
        public String manageUrl;
    }


    final class UserInfo {
        /**
         * User's id
         */
        public String id;

        /**
         * User's display name
         */
        public String displayName;

        /**
         * User's avatar. Could be null
         */
        @Nullable
        public String avatarUrl;

        /**
         * Current subscription level
         */
        public int currentLevel;
    }

    final class RemotePrinter {
        /**
         * Printer identifier, UUID
         */
        public String id;

        /**
         * Name of the printer
         */
        public String name;

        /**
         * Websocket token
         */
        public String token;

        /**
         * Publicly accessible URL
         */
        public String publicUrl;
    }
}
