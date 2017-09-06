package chat.rocket.android;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import chat.rocket.android.activity.UserErrorActivity;

public class SALaunchUtils {

    public static void showUserErrorActivity(Context context) {
        Intent intent = new Intent(context, UserErrorActivity.class);
        context.startActivity(intent);
    }

    public static void showSendEmail(Context context) {
        String subject = "Subscriptions";
        String body = "";
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri data = Uri.parse("mailto:?subject=" + subject + "&body=" + body);
        intent.setData(data);
        context.startActivity(intent);
    }
}
