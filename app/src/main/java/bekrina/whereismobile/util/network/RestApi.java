package bekrina.whereismobile.util.network;

import java.util.HashMap;

import bekrina.whereismobile.model.Group;
import bekrina.whereismobile.model.User;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface RestApi {
    @GET("api/group/getforcurrentuser")
    Call<Group> getGroup();

    @GET("api/config.json")
    Call<HashMap> getAuthCode();

    @POST("api/login")
    Call<User> login(@Body byte[] authCode);
}