package bekrina.whereismobile.util.restapi;


import android.content.Context;

import java.net.CookieManager;
import java.net.CookiePolicy;

import okhttp3.JavaNetCookieJar;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RestApiFactory {
    private static RestApi restApi;

    private RestApiFactory() {};

    private static void createRestApi(Context context) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://rocky-river-45878.herokuapp.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(new OkHttpClient.Builder()
                      .cookieJar(new JavaNetCookieJar(new CookieManager(
                            new PersistentCookieStore(context), CookiePolicy.ACCEPT_ALL)))
                  .build())
                .build();
        restApi = retrofit.create(RestApi.class);
    }

    public static synchronized RestApi getApi(Context context) {
        if (restApi == null) {
            createRestApi(context);
        }
        return restApi;
    }
}
