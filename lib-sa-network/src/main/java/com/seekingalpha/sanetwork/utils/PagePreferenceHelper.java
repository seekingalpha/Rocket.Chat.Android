package com.seekingalpha.sanetwork.utils;

public interface PagePreferenceHelper {

    String getLoginKey();

    String getGroupChatKey(String key);

    String getDirectMessageKey(String key);

    void storeEmail(String userId);

    String getEmail();

    String getMainKey();
}
