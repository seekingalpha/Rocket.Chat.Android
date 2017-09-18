package com.seekingalpha.sanetwork;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.seekingalpha.sanetwork.response.TokenResponse;
import com.seekingalpha.sanetwork.utils.UnzippingInterceptor;

import java.util.HashMap;
import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class HttpHelper {

    private Http http;
    private Retrofit retrofit;

    public HttpHelper(String host) {

        Gson gson = new GsonBuilder()
                .setLenient()
                .create();

        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .addInterceptor(new UnzippingInterceptor())
                .build();

        retrofit = new Retrofit.Builder()
                .baseUrl(host)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
        http = retrofit.create(Http.class);
    }

    public Call<TokenResponse> login(String email, String password) {
        Map<String, String> header = new HashMap<>();
        header.put("Fastly-Debug", "1");
        header.put("Accept-Encoding", "gzip, deflate, sdch");
        header.put("Authorization", "Basic c2Vla2luZ2FscGhhOmlwdmlwdg==");
        header.put("Content-type","application/json");

        Map<String, String> body = new HashMap<>();
        body.put("email", email);
        body.put("password", password);
        return http.login(header, body);
    }
}