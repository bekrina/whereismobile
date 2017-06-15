package bekrina.whereismobile.util.network;


import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RestApiFactory {
    private static RestApi restApi;

    private RestApiFactory() {};

    private static void createRestApi() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://rocky-river-45878.herokuapp.com/")
                .addConverterFactory(GsonConverterFactory.create())
                //.client(new OkHttpClient.Builder()
                //      .cookieJar(new JavaNetCookieJar(new CookieManager(
                //            new PersistentCookieStore(this), CookiePolicy.ACCEPT_ALL)))
                //  .build())
                .build();
        restApi = retrofit.create(RestApi.class);
    }

    public static synchronized RestApi getInstance() {
        if (restApi == null) {
            createRestApi();
        }
        return restApi;
    }
}
