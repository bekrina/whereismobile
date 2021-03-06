package bekrina.whereismobile.services;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import bekrina.whereismobile.listeners.LocationsUpdatedListener;
import bekrina.whereismobile.model.Group;
import bekrina.whereismobile.model.Location;
import bekrina.whereismobile.model.User;
import bekrina.whereismobile.util.SingletonNetwork;

import static bekrina.whereismobile.util.Constants.EMAIL;
import static bekrina.whereismobile.util.Constants.FIRST_NAME;
import static bekrina.whereismobile.util.Constants.GET_LOCATIONS_ACTION;
import static bekrina.whereismobile.util.Constants.GROUP;
import static bekrina.whereismobile.util.Constants.GROUP_ENDPOINT;
import static bekrina.whereismobile.util.Constants.GROUP_IDENTITY;
import static bekrina.whereismobile.util.Constants.GROUP_INFO_PREFERENCES;
import static bekrina.whereismobile.util.Constants.LAST_NAME;
import static bekrina.whereismobile.util.Constants.USER;


public class MembersLocationsService extends Service {
    private static final String TAG = MembersLocationsService.class.getName();
    private final IBinder mBinder = new MembersLocationsBinder();
    private SingletonNetwork mNetwork;
    private LocationsUpdatedListener mLocationsUpdatedListener;


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class MembersLocationsBinder extends Binder {
        public MembersLocationsService getService(LocationsUpdatedListener listener) {
            mLocationsUpdatedListener = listener;

            Runnable updateMembersLocations = new Runnable() {
                @Override
                public void run() {
                    MembersLocationsService.this.updateLocations();
                }
            };

            ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
            scheduledExecutorService.scheduleAtFixedRate(updateMembersLocations, 0, 1, TimeUnit.MINUTES);

            return MembersLocationsService.this;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        mNetwork = SingletonNetwork.getInstance(getBaseContext());
    }

    /*
     * Use only in MembersLocationBinder.getService() or after binding in activity
     */
    public void updateLocations() {
        SharedPreferences sharedPreferences = getSharedPreferences(GROUP_INFO_PREFERENCES, 0);
        Gson gson = new Gson();
        Group group = gson.fromJson(sharedPreferences.getString(GROUP, ""), Group.class);
        JsonArrayRequest locationsRequest = new JsonArrayRequest(Request.Method.GET,
                GROUP_ENDPOINT + "/" + group.getIdentity()
                        + GET_LOCATIONS_ACTION, null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        try {
                            List<Location> locations = new ArrayList<>();

                            GsonBuilder builder = new GsonBuilder();
                            builder.registerTypeAdapter(Timestamp.class,
                                    new JsonDeserializer<Timestamp>() {
                                public Timestamp deserialize(JsonElement json, Type typeOfT,
                                                             JsonDeserializationContext context)
                                        throws JsonParseException {
                                    return new Timestamp(json.getAsJsonPrimitive().getAsLong());
                                }
                            });
                            Gson gson = builder.create();

                            for (int i = 0; i < response.length(); i++) {
                                JSONObject jsonObjectLocation = response.getJSONObject(i);
                                Location location = gson.fromJson(jsonObjectLocation.toString(),
                                        Location.class);
                                locations.add(location);
                            }
                            Log.d(TAG, "Location received:");
                            mLocationsUpdatedListener.onMembersLocationsUpdate(locations);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Location not received:", error);
            }
        });
        mNetwork.addToRequestQueue(locationsRequest);
    }
}
