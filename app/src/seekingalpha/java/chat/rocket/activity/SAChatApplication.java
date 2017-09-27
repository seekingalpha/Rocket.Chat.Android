package chat.rocket.activity;

import com.seekingalpha.sanetwork.TrackingHelper;

import chat.rocket.android.R;
import chat.rocket.android.RocketChatApplication;
import chat.rocket.android.helper.PreferenceHelper;

public class SAChatApplication extends RocketChatApplication{

    @Override
    public void onCreate() {
        super.onCreate();
        String host = getString(R.string.sa_http_host);
        PreferenceHelper preferenceHelper = new PreferenceHelper(this);
        TrackingHelper.init(this, host, preferenceHelper);
    }
}
