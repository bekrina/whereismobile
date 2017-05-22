package bekrina.whereismobile.services;


import android.app.Application;

import retrofit2.Retrofit;

public class RestApiManager extends Application {
    private static RestApi restApi;
    private Retrofit retrofit;

    @Override
    public void onCreate() {
        super.onCreate();

        retrofit = new Retrofit.Builder()
                .baseUrl("https://rocky-river-45878.herokuapp.com/api/") //Базовая часть адреса
                .addConverterFactory(retrofit2.GsonConverterFactory.create()) //Конвертер, необходимый для преобразования JSON'а в объекты
                .build();
        restApi = retrofit.create(RestApi.class); //Создаем объект, при помощи которого будем выполнять запросы
    }

    public static RestApi getApi() {
        return restApi;
    }
}
