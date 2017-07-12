package bekrina.whereismobile.util.restapi;


import android.content.Context;

import com.franmontiel.persistentcookiejar.ClearableCookieJar;
import com.franmontiel.persistentcookiejar.PersistentCookieJar;
import com.franmontiel.persistentcookiejar.cache.SetCookieCache;
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Cookie;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RestApiFactory {
    private static final String JSESSIONID = "JSESSIONID";
    private static final String BASE_URL = "https://rocky-river-45878.herokuapp.com/";
    private static final String LOGIN_ENDPOINT = "api/login";

    private static RestApi restApi;

    private RestApiFactory() {};

    private static void createRestApi(Context context) {
        final ClearableCookieJar cookieJar =
                new PersistentCookieJar(new SetCookieCache(), new SharedPrefsCookiePersistor(context));

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        // set your desired log level
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder();
        httpClientBuilder.cookieJar(cookieJar)
                         .connectTimeout(130, TimeUnit.SECONDS)
                         .interceptors().add(new Interceptor() {
                                @Override
                                public Response intercept(Chain chain) throws IOException {
                                    Request request = chain.request();
                                    HttpUrl httpUrl = new HttpUrl.Builder()
                                            .scheme("https")
                                            .host("rocky-river-45878.herokuapp.com")
                                            .addPathSegment("api")
                                            .addPathSegment("login")
                                            .build();
                                    List<Cookie> cookies = cookieJar.loadForRequest(httpUrl);
                                    for (Cookie cookie : cookies) {
                                     if (cookie.name().equals(JSESSIONID)) {
                                         Request newRequest = request.newBuilder().addHeader(JSESSIONID, cookie.value()).build();
                                         return chain.proceed(newRequest);
                                        }
                                     }
                                return chain.proceed(request);
                                }
                            });
        httpClientBuilder.interceptors().add(logging);
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(httpClientBuilder.build())
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
