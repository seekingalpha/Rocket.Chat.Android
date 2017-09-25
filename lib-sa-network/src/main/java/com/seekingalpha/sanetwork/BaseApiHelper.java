package com.seekingalpha.sanetwork;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.seekingalpha.sanetwork.utils.UnzippingInterceptor;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public abstract class BaseApiHelper<T> {

    private T api;

    public BaseApiHelper(String host, Class<T> clazz) {
        api = createApi(host, clazz);
    }

    protected T createApi(String host, Class<T> clazz) {
        Gson gson = new GsonBuilder()
                .setLenient()
                .create();

        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .addInterceptor(new UnzippingInterceptor())
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(host)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
        return retrofit.create(clazz);
    }



    public T getApi() {
        return api;
    }
}
