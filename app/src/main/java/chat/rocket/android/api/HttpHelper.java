package chat.rocket.android.api;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import chat.rocket.android.R;
import chat.rocket.android.api.rest.Http;
import chat.rocket.android.api.rest.TokenResponse;
import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.internal.http.RealResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import okio.GzipSource;
import okio.Okio;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class HttpHelper {

    private Http http;
    private Retrofit retrofit;

    public HttpHelper(Context context) {
        String host = context.getString(R.string.sa_http_host);

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

    private class UnzippingInterceptor implements Interceptor {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Response response = chain.proceed(chain.request());
            return unzip(response);
        }

        private Response unzip(final Response response) throws IOException {

            if (response.body() == null) {
                return response;
            }

            GzipSource responseBody = new GzipSource(response.body().source());
            Headers strippedHeaders = response.headers().newBuilder()
                    .removeAll("Content-Encoding")
                    .removeAll("Content-Length")
                    .build();
            return response.newBuilder()
                    .headers(strippedHeaders)
                    .body(new RealResponseBody(strippedHeaders, Okio.buffer(responseBody)))
                    .build();
        }
    }
}