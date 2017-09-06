package chat.rocket.android.fragment.server_config;

import chat.rocket.android.api.HttpHelper;
import chat.rocket.android.api.MethodCallHelper;
import chat.rocket.android.api.TwoStepAuthException;
import chat.rocket.android.api.rest.TokenResponse;
import chat.rocket.android.helper.TextUtils;
import chat.rocket.core.repositories.LoginServiceConfigurationRepository;
import chat.rocket.core.repositories.PublicSettingRepository;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SALoginPresenter extends LoginPresenter {

    private HttpHelper httpHelper;
    private final MethodCallHelper methodCallHelper;

    public SALoginPresenter(
            LoginServiceConfigurationRepository loginServiceConfigurationRepository,
            PublicSettingRepository publicSettingRepository,
            MethodCallHelper methodCallHelper,
            HttpHelper httpHelper) {
        super(loginServiceConfigurationRepository, publicSettingRepository, methodCallHelper);
        this.httpHelper = httpHelper;
        this.methodCallHelper = methodCallHelper;
    }

    @Override
    public void login(String username, String password) {
        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            return;
        }

        view.showLoader();

        httpHelper.login(username, password).enqueue(callback);
    }

    private void proceedError(TokenResponse.ErrorLogin errorLogin){
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
