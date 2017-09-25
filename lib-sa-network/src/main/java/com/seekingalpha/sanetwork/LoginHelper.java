package com.seekingalpha.sanetwork;

import com.seekingalpha.sanetwork.response.TokenResponse;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;

public class LoginHelper extends BaseApiHelper<LoginApi> {

    public LoginHelper(String host) {
        super(host, LoginApi.class);
    }

    public Call<TokenResponse> login(String email, String password) {
        Map<String, String> body = new HashMap<>();
        body.put("email", email);
        body.put("password", password);
        return getApi().login(body);
    }
}