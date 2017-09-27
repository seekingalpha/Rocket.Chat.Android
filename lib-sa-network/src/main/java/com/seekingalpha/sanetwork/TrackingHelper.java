package com.seekingalpha.sanetwork;

import android.content.Context;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.seekingalpha.sanetwork.response.MoneResponse;
import com.seekingalpha.sanetwork.utils.HttpUtils;
import com.seekingalpha.sanetwork.utils.PagePreferenceHelper;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.LinkedList;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Converter;
import retrofit2.Response;
import retrofit2.Retrofit;

public class TrackingHelper extends BaseApiHelper<TrackingApi> {

    private static TrackingHelper trackingHelper;

    public static TrackingHelper getInstance() {
        return trackingHelper;
    }

    public static void init(Context context, String host, PagePreferenceHelper preferenceHelper) {
        trackingHelper = new TrackingHelper(context, host, preferenceHelper);
    }

    private static final String TAG = "TrackingHelper";
    private static final String VERSION = "2";

    private static final String SOURCE_ROADBLOCK = "roadblock";
    private static final String ACTION_WRONG_CREDENTIALS = "wrong_credentials";
    private static final String TYPE_CREDENTIALS = "credentials";

    private static final String ACTION_SUCCESS = "success";

    private static final String SOURCE_DRAWER_MENU = "drawer_menu";
    private static final String ACTION_OPEN = "open";
    private static final String TYPE_CLICK = "click";

    private static final String ACTION_LOGOUT = "logout";
    private static final String SOURCE_MESSAGE = "message";
    private static final String ACTION_SENT = "sent";
    private static final String TYPE_DIRECT_MSG = "direct_msg";
    private static final String TYPE_GROUP = "group";

    private String userAgent;
    private Context context;
    private String email;
    private String currentPageKey = "";
    private String currentPageUrl = "";
    private String referrer = "";
    private String referrerKey = "";
    private PagePreferenceHelper preferenceHelper;

    private TrackingHelper(Context context, String host, PagePreferenceHelper preferenceHelper) {
        super(host, TrackingApi.class);
        email = preferenceHelper.getEmail();
        this.context = context;
        userAgent = HttpUtils.createUserAgent(context);
        this.preferenceHelper = preferenceHelper;
    }

    @Override
    protected TrackingApi createApi(String host, Class<TrackingApi> clazz) {

        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(host)
                .client(client)
                .addConverterFactory(new NullOnEmptyConverterFactory())
                .build();
        return retrofit.create(clazz);
    }

    public Call<MoneResponse> mone(String url, String pageKey) {
        referrer = currentPageUrl;
        referrerKey = currentPageKey;
        currentPageUrl = url;
        currentPageKey = pageKey;
        String strMone = createMone(email, url, pageKey, referrer, referrerKey);
        Log.i("TrackingHelper", "Mone: " + strMone);
        return getApi().mone(userAgent, strMone);
    }

    public Call<MoneResponse> moneEvent(String source, String action, String type) {
        return moneEvent(source, action, type, "{}");
    }

    public Call<MoneResponse> moneEvent(String source, String action, String type, String data) {
        Log.i("TrackingHelper", "MoneEvent: \n" + " PageKey: " + currentPageKey);
        return getApi().moneEvent(userAgent, VERSION, currentPageKey, source, action, type, data);
    }

    public void wrongCredentialsEvent(String email) {
        EmailData emailData = new EmailData(email);
        Gson gson = new Gson();
        moneEvent(SOURCE_ROADBLOCK, ACTION_WRONG_CREDENTIALS, TYPE_CREDENTIALS, gson.toJson(emailData))
                .enqueue(callback);
    }

    public void correctCredentialsEvent() {
        moneEvent(SOURCE_ROADBLOCK, ACTION_SUCCESS, TYPE_CREDENTIALS)
                .enqueue(callback);
        mainScreen();
    }

    public void openMenuEvent() {

        moneEvent(SOURCE_DRAWER_MENU, ACTION_OPEN, TYPE_CLICK)
                .enqueue(callback);
    }

    public void logsOutMenuEvent() {

        moneEvent(SOURCE_DRAWER_MENU, ACTION_LOGOUT, TYPE_CLICK)
                .enqueue(callback);
    }

    public void sendsDirectMessageEvent() {

        moneEvent(SOURCE_MESSAGE, ACTION_SENT, TYPE_DIRECT_MSG)
                .enqueue(callback);
    }

    public void sendsDirectGroupEvent(String data) {

        moneEvent(SOURCE_MESSAGE, ACTION_SENT, TYPE_GROUP, data)
                .enqueue(callback);
    }

    public void mainScreen() {
        mone("/chat/", preferenceHelper.getMainKey()).enqueue(callback);
    }

    public void loginScreen() {
        mone("/roadblock/step_1/", preferenceHelper.getLoginKey()).enqueue(callback);
    }

    public void groupChatScreen(String chatName) {
        mone("/chat/group/" + chatName, preferenceHelper.getGroupChatKey()).enqueue(callback);
    }

    public void directMessageScreen(String name) {
        mone("/chat/direct_msg/" + name, preferenceHelper.getDirectMessageKey()).enqueue(callback);
    }

    private String createMone(String email, String url, String pageKey, String referrer, String referrerKey) {
        String deviceId = getDeviceID(context);
        LinkedList<String> list = new LinkedList<>();

        list.add(VERSION);
        list.add("");
        list.add(pageKey);
        list.add(referrerKey);
        list.add(referrer);
        list.add(url);
        list.add("");
        list.add(deviceId);
        list.add("");
        list.add(email);
        list.add("");
        list.add(email);
        list.add("");
        list.add("");
        list.add("");
        list.add("");
        list.add("");
        list.add("{}");
        list.add("{}");
        list.add("{}");
        list.add("{}");
        list.add("{}");
        list.add("{}");
        list.add("{}");
        list.add("{}");
        return TextUtils.join(";;;", list);
    }

    public static String getDeviceID(Context context) {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    public void setEmail(String email) {
        preferenceHelper.storeEmail(email);
        this.email = email;
    }

    private Callback<MoneResponse> callback = new Callback<MoneResponse>() {
        @Override
        public void onResponse(Call<MoneResponse> call, Response<MoneResponse> response) {
            Log.i(TAG, "Success: " + call.request().url());
        }

        @Override
        public void onFailure(Call<MoneResponse> call, Throwable throwable) {
            Log.i(TAG, "Failure: " + call.request().url());
        }
    };

    private class EmailData {
        private String email;

        public EmailData(String email) {
            this.email = email;
        }
    }

    class NullOnEmptyConverterFactory extends Converter.Factory {

        @Override
        public Converter<ResponseBody, MoneResponse> responseBodyConverter(Type type, Annotation[] annotations, final Retrofit retrofit) {
            return new Converter<ResponseBody, MoneResponse>() {
                @Override
                public MoneResponse convert(ResponseBody responseBody) throws IOException {
                    responseBody.close();
                    return null;
                }
            };
        }
    }
}
