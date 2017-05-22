package bekrina.whereismobile.services;

import bekrina.whereismobile.model.Group;
import retrofit2.Call;
import retrofit2.http.GET;

public interface RestApi {
    @GET("group/getforcurrentuser")
    Call<Group> getGroup();
}
