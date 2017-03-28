package bekrina.whereismobile.services;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.ServerError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.api.client.http.HttpStatusCodes;

import org.json.JSONException;
import org.json.JSONObject;

import bekrina.whereismobile.exceptions.PermissionRequiredException;
import bekrina.whereismobile.util.SingletonNetwork;

import static bekrina.whereismobile.util.Constants.*;

public class LocationSavingService extends Service implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {
    private static final String TAG = LocationSavingService.class.getName();

    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private LocationRequest mLocationRequest;
    private SingletonNetwork mSingletonNetwork;
    private final IBinder mBinder = new LocationBinder();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class LocationBinder extends Binder {
        public LocationSavingService getService() {
            return LocationSavingService.this;
        }
    }
    @Override
    public void onCreate() {
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .addApi(AppIndex.API).build();
            mGoogleApiClient.connect();
        }
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(LOCATION_INTERVAL);
        mLocationRequest.setFastestInterval(LOCATION_FASTEST_INTERVAL);

        mSingletonNetwork = SingletonNetwork.getInstance(getBaseContext());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    public Location getLastLocation() throws PermissionRequiredException {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            return LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        }
        throw new PermissionRequiredException("Location Service needs permission to get location data");
    }

    protected void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, mLocationRequest, this);
            return;
        }
    }

    @Override
    public void onDestroy() {
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        JSONObject locationObject = new JSONObject();
        try {
            locationObject.put(LAT, location.getLatitude());
            locationObject.put(LNG, location.getLongitude());

            SharedPreferences preferences = getSharedPreferences(GROUP_INFO_PREFERENCES, 0);

            JsonObjectRequest sendLocation = new JsonObjectRequest(Request.Method.PUT,
                    GROUP_ENDPOINT + "/" + preferences.getString(GROUP_IDENTITY, "")
                            + SAVE_LOCATION_ACTION,
                    locationObject, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    Log.d(TAG, "Success! Location saved");
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Location not saved:", error);
                }
            }) {
                @Override
                protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
                    if (response.statusCode == HttpStatusCodes.STATUS_CODE_OK) {
                        return Response.success(new JSONObject(),
                                HttpHeaderParser.parseCacheHeaders(response));
                    } else {
                        return Response.error(new ServerError());
                    }
                }
            };
            mSingletonNetwork.getRequestQueue().add(sendLocation);
        } catch (JSONException e) {
            Log.e(TAG, "Error during sending location:", e);
        }
    }
}
