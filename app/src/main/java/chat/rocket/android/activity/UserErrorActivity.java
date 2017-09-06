package chat.rocket.android.activity;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.os.Bundle;

import chat.rocket.android.LaunchUtil;
import chat.rocket.android.R;
import chat.rocket.android.SALaunchUtils;

public class UserErrorActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_error);
        try {
            findViewById(R.id.email).setOnClickListener(
                    view -> SALaunchUtils.showSendEmail(UserErrorActivity.this));
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        }
    }
}