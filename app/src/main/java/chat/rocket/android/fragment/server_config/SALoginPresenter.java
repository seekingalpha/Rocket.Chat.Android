package chat.rocket.android.fragment.server_config;

import com.seekingalpha.sanetwork.LoginHelper;
import com.seekingalpha.sanetwork.TrackingHelper;
import com.seekingalpha.sanetwork.response.TokenResponse;

import chat.rocket.android.api.MethodCallHelper;
import chat.rocket.android.api.TwoStepAuthException;
import chat.rocket.android.helper.TextUtils;
import chat.rocket.core.repositories.LoginServiceConfigurationRepository;
import chat.rocket.core.repositories.PublicSettingRepository;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SALoginPresenter extends LoginPresenter {

    public static String user_id = null;

    private LoginHelper httpHelper;
    private final MethodCallHelper methodCallHelper;
    private TrackingHelper trackingHelper;
    private String email;

    public SALoginPresenter(
            LoginServiceConfigurationRepository loginServiceConfigurationRepository,
            PublicSettingRepository publicSettingRepository,
            MethodCallHelper methodCallHelper,
            LoginHelper httpHelper,
            TrackingHelper trackingHelper) {
        super(loginServiceConfigurationRepository, publicSettingRepository, methodCallHelper);
        this.httpHelper = httpHelper;
        this.methodCallHelper = methodCallHelper;
        this.trackingHelper = trackingHelper;
    }

    @Override
    public void login(String username, String password) {
        this.email = username;
        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            return;
        }

        view.showLoader();

        httpHelper.login(username, password).enqueue(callback);
    }

    private void proceedError(TokenResponse.ErrorLogin errorLogin){
        trackingHelper.wrongCredentialsEvent(email);
        if(errorLogin.getErrorCode() == 3){
            view.showErrorActivity();
        }else{
            view.showError(errorLogin.getMsg());
        }
        view.hideLoader();
    }

    private Callback<TokenResponse> callback = new Callback<TokenResponse>() {
        @Override
        public void onResponse(Call<TokenResponse> call, Response<TokenResponse> response) {
            TokenResponse tokenResponse = response.body();
            if(tokenResponse.getError() != null){
                proceedError(tokenResponse.getError());
                return;
            }

            methodCallHelper.loginWithToken(tokenResponse.getRcToken())
                    .continueWith(task -> {
                        if (task.isFaulted()) {
                            view.hideLoader();

                            final Exception error = task.getError();

                            if (error instanceof TwoStepAuthException) {
                                view.showTwoStepAuth();
                            } else {
                                view.showError(error.getMessage());
                            }
                        } else {
                            trackingHelper.setEmail(email);
                            trackingHelper.correctCredentialsEvent();
                        }
                        return null;
                    });
        }

        @Override
        public void onFailure(Call<TokenResponse> call, Throwable throwable) {
            view.hideLoader();
            view.showError(throwable.getMessage());
        }
    };
}
