package chat.rocket.android.api.rest;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.HeaderMap;
import retrofit2.http.POST;

public interface Http {

    @POST("/authentication/rc_mobile_login")
    Call<TokenResponse> login(
            @HeaderMap  Map<String, String> headers,
            @Body Map<String, String> body
    );
}