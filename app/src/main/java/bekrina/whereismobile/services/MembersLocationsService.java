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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import bekrina.whereismobile.listeners.LocationsUpdatedListener;
import bekrina.whereismobile.util.SingletonNetwork;

import static bekrina.whereismobile.util.Constants.*;


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
            startContinuousPolling();
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

    public void startContinuousPolling() {
        // TODO: logic for smart polling
        updateLocations();
    }

    public void updateLocations() {
        SharedPreferences sharedPreferences = getSharedPreferences(GROUP_INFO_PREFERENCES, 0);

        JsonArrayRequest locationsRequest = new JsonArrayRequest(Request.Method.GET,
                GROUP_ENDPOINT + "/" + sharedPreferences.getString(GROUP_IDENTITY, "")
                        + GET_LOCATIONS_ACTION, null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        try {
                        List<JSONObject> locations = new ArrayList<>();
                        for (int i = 0; i < response.length() - 1; i++) {
                                locations.add(response.getJSONObject(i));
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
