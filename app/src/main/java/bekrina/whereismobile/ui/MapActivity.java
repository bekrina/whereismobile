package bekrina.whereismobile.ui;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import bekrina.whereismobile.R;
import bekrina.whereismobile.listeners.LocationsUpdatedListener;
import bekrina.whereismobile.services.ApiService;
import bekrina.whereismobile.services.LocationSavingService;
import bekrina.whereismobile.services.MembersLocationsService;
import bekrina.whereismobile.util.Constants;
import bekrina.whereismobile.util.GoogleApiHelper;

import static bekrina.whereismobile.util.Constants.FIRST_NAME;
import static bekrina.whereismobile.util.Constants.LAST_NAME;
import static bekrina.whereismobile.util.Constants.LAT;
import static bekrina.whereismobile.util.Constants.LNG;
import static bekrina.whereismobile.util.Constants.LOCATION_FASTEST_INTERVAL;
import static bekrina.whereismobile.util.Constants.LOCATION_INTERVAL;
import static bekrina.whereismobile.util.Constants.OFFSET;
import static bekrina.whereismobile.util.Constants.PERMISSIONS_REQUEST_FINE_LOCATION;
import static bekrina.whereismobile.util.Constants.USER;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback, LocationListener,
        LocationsUpdatedListener, ApiService.GroupStatusListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, ApiService.LeaveGroupListener,
        GoogleApiHelper.SignOutListener{
    private static final String TAG = MapActivity.class.getName();

    private GoogleMap mGoogleMap;

    private Location mUserLocation;
    private Marker mUserMarker;
    // TODO: google sparseArray
    private Map<Integer, Marker> mMembersMarkers = new HashMap<>();

    private MenuItem mLeaveGroupItem;
    private MenuItem mCreateGroupItem;
    private MenuItem mJoinGroupItem;
    private MenuItem mGroupNameItem;
    private MenuItem mInviteToGroupItem;

    private ApiService mApiService;
    private GoogleApiHelper mGoogleApiHelper;

    Intent startUserLocationsIntent = new Intent(this,
            LocationSavingService.class);

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

        mApiService = ApiService.getInstance(this);
        mGoogleApiHelper = new GoogleApiHelper(this);

        MapFragment mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.map);
        //TODO: включить анимацию загрузки
        mapFragment.getMapAsync(this);

    }

    @Override
    protected void onStart() {
        super.onStart();

        mGoogleApiHelper.connectToGoogleApi(this, this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mGoogleApiHelper.disconnectFromGoogleApi();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        mGoogleApiHelper.connectToGoogleApi(this, this);
        mApiService.processGroupStatus(this);
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
                    PERMISSIONS_REQUEST_FINE_LOCATION);
        }
        // TODO: выключить анимацию загрузки
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_FINE_LOCATION: {
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

            LocationRequest locationRequest  = LocationRequest.create();
                locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                locationRequest.setInterval(LOCATION_INTERVAL);
                locationRequest.setFastestInterval(LOCATION_FASTEST_INTERVAL);

            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiHelper.getGoogleApiClient(), locationRequest, this);
        }
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

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        mApiService.processGroupStatus(this);

        String groupName = getSharedPreferences(Constants.GROUP_INFO_PREFERENCES, 0)
                .getString(Constants.GROUP_NAME, "");
        if (groupName.equals("")) {
            mGroupNameItem.setTitle(getString(R.string.no_group_menu_item));
        } else {
            mGroupNameItem.setTitle(groupName);
        }
        return true;
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
            case R.id.leave_group_menu_item:
                mApiService.leaveGroup(this);
                return true;
            case R.id.sign_out_menu_item:
                mGoogleApiHelper.signOut(this);
            default:
                return super.onOptionsItemSelected(item);
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

    @Override
    public void onMembersLocationsUpdate(List<JSONObject> locations) {
        updateMembersMarkers(locations);
    }

    @Override
    public void onUserHasGroup(String name, String identity) {
        mCreateGroupItem.setVisible(false);
        mJoinGroupItem.setVisible(false);
        mLeaveGroupItem.setVisible(true);
        mInviteToGroupItem.setVisible(true);

        mGroupNameItem.setTitle(getSharedPreferences(Constants.GROUP_INFO_PREFERENCES, 0)
                .getString(Constants.GROUP_NAME, ""));

        startService(startUserLocationsIntent);
        Intent startMembersLocationsIntent = new Intent(this,
                MembersLocationsService.class);
        startService(startMembersLocationsIntent);
        startMembersLocationsIntent = new Intent(this,
                MembersLocationsService.class);
        bindService(startMembersLocationsIntent,
                mMembersLocationServiceConnection, Activity.BIND_AUTO_CREATE);
    }

    @Override
    public void onUserWithoutGroup() {
        mLeaveGroupItem.setVisible(false);
        mInviteToGroupItem.setVisible(false);
        mCreateGroupItem.setVisible(true);
        mJoinGroupItem.setVisible(true);

        stopService(startUserLocationsIntent);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLeaveGroup() {
        mGoogleMap.clear();
        mMembersMarkers.clear();
        mGoogleMap.addMarker(new MarkerOptions()
                .position(mUserMarker.getPosition())
                .title(mUserMarker.getTitle()));
        mApiService.processGroupStatus(this);
    }

    @Override
    public void onLeaveGroupFailed() {
        //TODO: leave failed screen
    }

    @Override
    public void onSignOut() {
        // Start login activity
        Intent loginActivityIntent = new Intent(this, LoginActivity.class);
        startActivity(loginActivityIntent);
    }


    //TODO: check this
/*    */

    /**
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
}
