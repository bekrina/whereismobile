package bekrina.whereismobile.util.restapi;

import java.util.HashMap;
import java.util.Set;

import bekrina.whereismobile.model.Group;
import bekrina.whereismobile.model.User;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface RestApi {
    @GET("/api/group/getforcurrentuser")
    Call<Set<Group>> getGroup();

    @GET("/api/config.json")
    Call<HashMap> getAuthCode();

    @POST("/api/login")
    Call<User> login(@Body byte[] authCode);
}