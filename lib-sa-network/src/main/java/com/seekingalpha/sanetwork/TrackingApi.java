package com.seekingalpha.sanetwork;

import com.seekingalpha.sanetwork.response.MoneResponse;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;

import static com.seekingalpha.sanetwork.HeaderConstants.HEADER_ACCEPT;
import static com.seekingalpha.sanetwork.HeaderConstants.HEADER_AUTHORIZATION;
import static com.seekingalpha.sanetwork.HeaderConstants.HEADER_CONTENT_TYPE_URL;

public interface TrackingApi {

    @FormUrlEncoded
    @Headers({
            HEADER_ACCEPT,
            HEADER_AUTHORIZATION,
            HEADER_CONTENT_TYPE_URL})
    @POST("/mone")
    Call<MoneResponse> mone(
            @Header("User-agent") String userAgent,
            @Field("mone") String action
    );

    @FormUrlEncoded
    @Headers({
            HEADER_ACCEPT,
            HEADER_AUTHORIZATION,
            HEADER_CONTENT_TYPE_URL})
    @POST("/mone_event")
    Call<MoneResponse> moneEvent(
            @Header("User-agent") String userAgent,
            @Field("version") String version,
            @Field("key") String key,
            @Field("source") String source,
            @Field("action") String action,
            @Field("type") String type,
            @Field("data") String data
    );
}
