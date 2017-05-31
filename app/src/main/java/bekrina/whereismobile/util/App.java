package bekrina.whereismobile.util;


import android.app.Application;

import java.net.CookieManager;
import java.net.CookiePolicy;

import bekrina.whereismobile.util.network.PersistentCookieStore;
import bekrina.whereismobile.util.network.RestApi;
import okhttp3.JavaNetCookieJar;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class App extends Application {
    private static RestApi restApi;

    @Override
    public void onCreate() {
        super.onCreate();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://rocky-river-45878.herokuapp.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(new OkHttpClient.Builder()
                        .cookieJar(new JavaNetCookieJar(new CookieManager(
                                new PersistentCookieStore(this), CookiePolicy.ACCEPT_ALL)))
                        .build())
                .build();
        restApi = retrofit.create(RestApi.class);
    }

    public static RestApi getApi() {
        return restApi;
    }
}
