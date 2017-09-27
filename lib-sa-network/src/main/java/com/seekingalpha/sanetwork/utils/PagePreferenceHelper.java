package com.seekingalpha.sanetwork.utils;

public interface PagePreferenceHelper {

    String getLoginKey();

    String getGroupChatKey();

    String getDirectMessageKey();

    void storeEmail(String userId);

    String getEmail();

    String getMainKey();
}
