package lk.kishan.carelink.network;

import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    public static final String BASE_URL = "http://192.168.8.102:8080/";

    private static Retrofit retrofit;
    public static String AUTH_TOKEN = null;

    public static Retrofit getRetrofitInstance() {
        if (retrofit == null) {

            OkHttpClient client = new OkHttpClient.Builder().addInterceptor(new Interceptor() {
                @Override
                public Response intercept(Chain chain) throws IOException {
                    Request originalRequest = chain.request();

                    if (AUTH_TOKEN != null) {
                        Request newRequest = originalRequest.newBuilder()
                                .header("Authorization", "Bearer " + AUTH_TOKEN)
                                .build();
                        return chain.proceed(newRequest);
                    }

                    return chain.proceed(originalRequest);
                }
            }).build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}