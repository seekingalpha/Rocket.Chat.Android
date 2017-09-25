package com.seekingalpha.sanetwork;

import com.seekingalpha.sanetwork.response.TokenResponse;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

import static com.seekingalpha.sanetwork.HeaderConstants.HEADER_ACCEPT_ENCODING;
import static com.seekingalpha.sanetwork.HeaderConstants.HEADER_CONTENT_TYPE_JSON;
import static com.seekingalpha.sanetwork.HeaderConstants.HEADER_FASTLY;
import static com.seekingalpha.sanetwork.HeaderConstants.HEADER_AUTHORIZATION;

public interface LoginApi {

    @Headers({
            HEADER_FASTLY,
            HEADER_ACCEPT_ENCODING,
            HEADER_AUTHORIZATION,
            HEADER_CONTENT_TYPE_JSON})
    @POST("/authentication/rc_mobile_login")
    Call<TokenResponse> login(
            @Body Map<String, String> body
    );
}