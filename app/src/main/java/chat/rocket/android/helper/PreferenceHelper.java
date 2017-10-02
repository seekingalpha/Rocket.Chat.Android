package chat.rocket.android.helper;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.seekingalpha.sanetwork.utils.PagePreferenceHelper;

public class PreferenceHelper implements PagePreferenceHelper {

    private static final String MAIN_KEY = "MainKey";
    private static final String USER_ID_KEY = "UserIdKey";
    private static final String LOGIN_KEY = "LoginKey";
    private static final String GROUP_CHAT_KEY = "GroupChatKey";
    private static final String DIRECT_MESSAGE_KEY = "DirectMessageKey";

    private SharedPreferences sp;

    public PreferenceHelper(Context context) {
        sp = PreferenceManager.getDefaultSharedPreferences(context);
    }

    private void putString(String key, String value) {
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(key, value);
        editor.apply();
    }

    private String getString(String key) {
        return sp.getString(key, null);
    }

    @Override
    public String getLoginKey() {
        return getPageKey(LOGIN_KEY);
    }

    @Override
    public String getGroupChatKey(String key) {
        return getPageKey(GROUP_CHAT_KEY + key);
    }

    @Override
    public String getDirectMessageKey(String key) {
        return getPageKey(DIRECT_MESSAGE_KEY + key);
    }

    @Override
    public void storeEmail(String userId) {
        putString(USER_ID_KEY, userId);
    }

    @Override
    public String getEmail() {
        return getString(USER_ID_KEY);
    }

    @Override
    public String getMainKey() {
        return getPageKey(MAIN_KEY);
    }

    private String getPageKey(String page) {
        String str = getString(page);
        if (str == null) {
            str = java.util.UUID.randomUUID().toString();
            putString(page, str);
        }
        return str;
    }
}
