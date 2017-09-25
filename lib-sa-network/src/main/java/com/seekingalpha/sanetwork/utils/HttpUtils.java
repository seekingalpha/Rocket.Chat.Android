package com.seekingalpha.sanetwork.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.util.DisplayMetrics;

public class HttpUtils {
    public static String createUserAgent(Context context) {
        int height = 0;
        int width = 0;
        DisplayMetrics display = context.getResources().getDisplayMetrics();
        Configuration config = context.getResources().getConfiguration();

        // Always send screen dimension for portrait mode
        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            height = display.widthPixels;
            width = display.heightPixels;
        } else {
            width = display.widthPixels;
            height = display.heightPixels;
        }

        String appName = "";
        String appVersionName = "";
        String appVersionCode = "";

        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_CONFIGURATIONS);
            appName = packageInfo.packageName;
            appVersionName = packageInfo.versionName;
            appVersionCode = String.valueOf(packageInfo.versionCode);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return String.format("%1$s/%2$s(%3$s) (%4$s; U; Android Mobile %5$s; %6$s-%7$s; %13$s Build/%8$s; %9$s) %10$dX%11$d %12$s %13$s",
                appName, appVersionName, appVersionCode, System.getProperty("os.name", "Linux"),
                Build.VERSION.RELEASE, config.locale.getLanguage().toLowerCase(), config.locale.getCountry().toLowerCase(), Build.ID, Build.BRAND, width,
                height, Build.MANUFACTURER, Build.MODEL);
    }
}
