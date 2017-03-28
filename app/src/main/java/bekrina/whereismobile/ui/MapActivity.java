package bekrina.whereismobile.ui;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import bekrina.whereismobile.R;
import bekrina.whereismobile.listeners.LocationsUpdatedListener;
import bekrina.whereismobile.services.LocationSavingService;
import bekrina.whereismobile.services.MembersLocationsService;
import bekrina.whereismobile.util.Constants;
import bekrina.whereismobile.util.SingletonNetwork;

import static bekrina.whereismobile.util.Constants.FIRST_NAME;
import static bekrina.whereismobile.util.Constants.GET_GROUPS_ACTION;
import static bekrina.whereismobile.util.Constants.GROUP_ENDPOINT;
import static bekrina.whereismobile.util.Constants.GROUP_IDENTITY;
import static bekrina.whereismobile.util.Constants.GROUP_INFO_PREFERENCES;
import static bekrina.whereismobile.util.Constants.GROUP_NAME;
import static bekrina.whereismobile.util.Constants.LAST_NAME;
import static bekrina.whereismobile.util.Constants.LAT;
import static bekrina.whereismobile.util.Constants.LNG;
import static bekrina.whereismobile.util.Constants.LOCATION_FASTEST_INTERVAL;
import static bekrina.whereismobile.util.Constants.LOCATION_INTERVAL;
import static bekrina.whereismobile.util.Constants.OFFSET;
import static bekrina.whereismobile.util.Constants.USER;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener, LocationsUpdatedListener {
    private static final String TAG = MapActivity.class.getName();

    private static final int MY_PERMISSIONS_REQUEST_FINE_LOCATION = 123;
    private static final int USER_IN_GROUP = 1;
    private static final int USER_HAS_NO_GROUP = 0;

    private GoogleMap mGoogleMap;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private Location mUserLocation;
    private Marker mUserMarker;
    // TODO: google sparseArray
    private Map<Integer, Marker> mMembersMarkers = new HashMap<>();

    private MenuItem mLeaveGroupItem;
    private MenuItem mCreateGroupItem;
    private MenuItem mJoinGroupItem;
    private MenuItem mGroupNameItem;
    private MenuItem mInviteToGroupItem;

    private Handler mHandler;

    private SingletonNetwork mNetwork;

    private MembersLocationsService mMembersLocationsService;
    private boolean mMembersLocationsBound;
    private ServiceConnection mMembersLocationServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MembersLocationsService.MembersLocationsBinder binder = (MembersLocationsService.MembersLocationsBinder) service;
            mMembersLocationsService = binder.getService(MapActivity.this);
            mMembersLocationsBound = true;
            Log.d(TAG, "MembersLocationsService connected");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mMembersLocationsBound = false;
            Log.d(TAG, "MembersLocationsService disconnected");
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_map);

        mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message inputMessage) {
                switch (inputMessage.what) {
                    case USER_HAS_NO_GROUP:
                        mLeaveGroupItem.setVisible(false);
                        mInviteToGroupItem.setVisible(false);
                        break;
                    case USER_IN_GROUP:
                        mCreateGroupItem.setVisible(false);
                        mJoinGroupItem.setVisible(false);

                        mGroupNameItem.setTitle(getSharedPreferences(Constants.GROUP_INFO_PREFERENCES, 0)
                                .getString(Constants.GROUP_NAME, ""));

                }
            }
        };
        mNetwork = SingletonNetwork.getInstance(this);

        MapFragment mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.map);
        //TODO: включить анимацию загрузки
        mapFragment.getMapAsync(this);

    }

    @Override
    protected void onStart() {
        super.onStart();

        connectToGoogleApi();
    }

    public void connectToGoogleApi() {
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .addApi(Auth.GOOGLE_SIGN_IN_API)
                    .addApi(AppIndex.API).build();
            mGoogleApiClient.connect();
        }

        if (mLocationRequest == null) {
            mLocationRequest = LocationRequest.create();
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            mLocationRequest.setInterval(LOCATION_INTERVAL);
            mLocationRequest.setFastestInterval(LOCATION_FASTEST_INTERVAL);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        connectToGoogleApi();
        processGroupStatus();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

            googleMap.setMyLocationEnabled(true);

            mGoogleMap = googleMap;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_FINE_LOCATION);
        }
        // TODO: выключить анимацию загрузки
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        //TODO: test this or change
                        startLocationUpdates();
                } else {
                    // TODO: show no permission screen
                }
                return;
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    //TODO: check this
/*    *//**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     *//*
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("Map Page") //  Define a title for the content shown.
                // Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }*/

    @Override
    public void onConnected(@Nullable Bundle bundle) {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_FINE_LOCATION);
            return;
        }
        // TODO: see RXJava
        startLocationUpdates();
    }

    public void updateUserMarker(String desc, Location lastLocation) {
        if (mMembersMarkers != null) {
            for (Marker memberMarker : mMembersMarkers.values()) {
                if (memberMarker.getPosition().latitude == lastLocation.getLatitude() &&
                        memberMarker.getPosition().longitude == lastLocation.getLongitude()) {
                    lastLocation.setLatitude(lastLocation.getLatitude() + OFFSET);
                    lastLocation.setLongitude(lastLocation.getLongitude() + OFFSET);
                }
            }
        }

        if (mUserMarker == null) {
            mUserMarker = mGoogleMap.addMarker(new MarkerOptions()
                    .title(desc)
                    .position(new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude())));
        } else {
            mUserMarker.setPosition(new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()));
            mUserMarker.setTitle(desc);
        }
    }

    public void updateMembersMarkers(List<JSONObject> membersLocations) {
        try {
            for (JSONObject location : membersLocations) {
                Location memberLocation = new Location("MembersLocationsService");
                memberLocation.setLatitude(location.getDouble(LAT));
                memberLocation.setLongitude(location.getDouble(LNG));
                String description = location.getJSONObject(USER).getString(FIRST_NAME) + " "
                        + location.getJSONObject(USER).getString(LAST_NAME);

                Marker memberMarker = mGoogleMap.addMarker(new MarkerOptions()
                        .title(description)
                        .position(new LatLng(memberLocation.getLatitude(), memberLocation.getLongitude())));
                mMembersMarkers.put(location.getJSONObject("user").getInt("id"), memberMarker);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Problem with getting info about member location:", e);
        }
    }

    protected void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, mLocationRequest, this);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        if (mUserLocation == null) {
            mUserLocation = location;
            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(),
                    location.getLongitude()), 10));
        } else {
            mUserLocation = location;
        }
        updateUserMarker(getString(R.string.user_location_marker), location);
    }

    public void processGroupStatus() {
        JsonArrayRequest groupRequest = new JsonArrayRequest(Request.Method.GET,
                GROUP_ENDPOINT + GET_GROUPS_ACTION, null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        Message message = new Message();
                        if (response.optJSONObject(0) == null) {
                            message.what = USER_HAS_NO_GROUP;
                        } else {
                            message.what = USER_IN_GROUP;
                            try {
                                updateGroupInfoPreferences(response.getJSONObject(0).getString(Constants.NAME),
                                        response.getJSONObject(0).getString(Constants.IDENTITY));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            Intent startUserLocationsIntent = new Intent(getBaseContext(),
                                    LocationSavingService.class);
                            startService(startUserLocationsIntent);
                            Intent startMembersLocationsIntent = new Intent(getBaseContext(),
                                    MembersLocationsService.class);
                            startService(startMembersLocationsIntent);
                            startMembersLocationsIntent = new Intent(getBaseContext(),
                                    MembersLocationsService.class);
                            bindService(startMembersLocationsIntent,
                                    mMembersLocationServiceConnection, BIND_AUTO_CREATE);
                        }
                        mHandler.sendMessage(message);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "tokenRequest.onErrorResponse:", error);
            }
        }) {
        };
        mNetwork.getRequestQueue().add(groupRequest);
    }

    public void updateGroupInfoPreferences(String name, String identity) {
        SharedPreferences preferences = getSharedPreferences(Constants.GROUP_INFO_PREFERENCES, 0);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(Constants.GROUP_NAME, name);
        editor.putString(Constants.GROUP_IDENTITY, identity);
        editor.apply();
    }
    @Override
    public void onConnectionSuspended(int i) {
        //TODO: complete this
        Log.d(TAG, "onConnectionSuspended:" + i);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed:" + connectionResult);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);

        mLeaveGroupItem = menu.findItem(R.id.leave_group_menu_item);
        mCreateGroupItem = menu.findItem(R.id.create_group_menu_item);
        mJoinGroupItem = menu.findItem(R.id.join_group_menu_item);
        mGroupNameItem = menu.findItem(R.id.group_name_menu_item);
        mInviteToGroupItem = menu.findItem(R.id.invite_to_group_menu_item);

        processGroupStatus();

        return true;
    }

    public void signOut() {
        mGoogleApiClient.connect();
        mGoogleApiClient.registerConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
            @Override
            public void onConnected(@Nullable Bundle bundle) {

                if (mGoogleApiClient.isConnected()) {
                    Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(
                            new ResultCallback<Status>() {
                                @Override
                                public void onResult(Status status) {
                                    Log.d(TAG, "signOut:onResult:" + status);
                                    // Remove cookies
                                    mNetwork.mCookieManager.getCookieStore().removeAll();
                                    // Remove information about group
                                    SharedPreferences sharedPreferences = getSharedPreferences(GROUP_INFO_PREFERENCES, 0);
                                    SharedPreferences.Editor editor = sharedPreferences.edit();
                                    editor.remove(GROUP_NAME);
                                    editor.remove(GROUP_IDENTITY);
                                    editor.apply();
                                    // Start login activity
                                    Intent loginActivityIntent = new Intent(getBaseContext(), LoginActivity.class);
                                    startActivity(loginActivityIntent);
                                }
                            });
                }
            }
            @Override
            public void onConnectionSuspended(int i) {
                Log.d(TAG, "Google API Client Connection Suspended");
            }
        });
    }
    //TODO: complete this method
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.group_name_menu_item:
                Intent groupInfoIntent = new Intent(getBaseContext(), GroupInfoActivity.class);
                startActivity(groupInfoIntent);
                return true;
            case R.id.create_group_menu_item:
                Intent createGroupIntent = new Intent(getBaseContext(), CreateGroupActivity.class);
                startActivity(createGroupIntent);
                return true;
            case R.id.invite_to_group_menu_item:
                Intent inviteIntent = new Intent(getBaseContext(), InviteToGroupActivity.class);
                startActivity(inviteIntent);
                return true;
            case R.id.join_group_menu_item:
                Intent joinIntent = new Intent(getBaseContext(), JoinGroupActivity.class);
                startActivity(joinIntent);
                return true;
            case R.id.sign_out_menu_item:
                signOut();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onMembersLocationsUpdate(List<JSONObject> locations) {
        updateMembersMarkers(locations);
    }
}
