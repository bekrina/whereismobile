package bekrina.whereismobile.ui;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.AsyncTask;
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
import android.view.View;
import android.widget.ProgressBar;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.ui.IconGenerator;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import bekrina.whereismobile.R;
import bekrina.whereismobile.exceptions.NoCurrentUserException;
import bekrina.whereismobile.listeners.GroupStatusListener;
import bekrina.whereismobile.listeners.LeaveGroupListener;
import bekrina.whereismobile.listeners.LocationsUpdatedListener;
import bekrina.whereismobile.model.Group;
import bekrina.whereismobile.model.User;
import bekrina.whereismobile.services.RestManager;
import bekrina.whereismobile.services.LocationSavingService;
import bekrina.whereismobile.services.MembersLocationsService;
import bekrina.whereismobile.util.Constants;
import bekrina.whereismobile.util.GoogleApiHelper;

import static bekrina.whereismobile.util.Constants.LOCATION_FASTEST_INTERVAL;
import static bekrina.whereismobile.util.Constants.LOCATION_INTERVAL;
import static bekrina.whereismobile.util.Constants.OFFSET;
import static bekrina.whereismobile.util.Constants.PERMISSIONS_REQUEST_FINE_LOCATION;
import static bekrina.whereismobile.util.Constants.USER;
import static bekrina.whereismobile.util.Constants.USER_INFO_PREFERENCES;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback, LocationListener,
        LocationsUpdatedListener, GroupStatusListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LeaveGroupListener,
        GoogleApiHelper.SignOutListener {

    private static final String TAG = MapActivity.class.getName();

    private Marker mUserMarker;
    private Map<Integer, Marker> mMembersMarkers = new HashMap<>();

    private MenuItem mLeaveGroupItem;
    private MenuItem mCreateGroupItem;
    private MenuItem mJoinGroupItem;
    private MenuItem mGroupNameItem;
    private MenuItem mInviteToGroupItem;

    private RestManager mRestManager;
    private GoogleApiHelper mGoogleApiHelper;

    private boolean mInGroup = false;

    private GoogleMap mGoogleMap;

    private Location mUserLocation;

    private EditableClusterManager<EditableClusterItem> mClusterManager;

    private User currentUser;

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
        Log.d(TAG, "onCreate");

        setContentView(R.layout.activity_map);

        mRestManager = RestManager.getInstance(this);
        mGoogleApiHelper = new GoogleApiHelper(this);

        MapFragment mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.map);

        SharedPreferences sharedPreferences = getSharedPreferences(USER_INFO_PREFERENCES, 0);
        Gson gson = new Gson();
        String userJson = sharedPreferences.getString(USER, "");
        if (!userJson.equals("")) {
            currentUser = gson.fromJson(userJson, User.class);
        } else {
            Log.e(TAG, "onCreate: No current user in shared preferences");
            throw new NoCurrentUserException("No current user in shared preferences");
        }

        mapFragment.getMapAsync(this);

    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
        mGoogleApiHelper.connectToGoogleApi(this, this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart");
        mRestManager.processGroupStatus(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
        mGoogleApiHelper.disconnectFromGoogleApi();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            mGoogleMap = googleMap;
            mGoogleMap.setMyLocationEnabled(false);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_FINE_LOCATION);
        }

        mClusterManager = new EditableClusterManager<>(this, mGoogleMap);
        EditableClusterRenderer clusterRenderer = new EditableClusterRenderer(this, mGoogleMap, mClusterManager);
        clusterRenderer.setMinClusterSize(1);

        mClusterManager.setRenderer(clusterRenderer);

        mGoogleMap.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
            @Override
            public void onCameraIdle() {
                mClusterManager.onCameraIdle();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startLocationUpdates();
                } else {
                    Log.e(TAG, "onRequestPermissionResult: no permissions");
                    // TODO: show no permission screen
                }
                return;
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    public void updateUserMarker(Location lastLocation) {
        bekrina.whereismobile.model.Location location = new bekrina.whereismobile.model.Location(
                lastLocation.getLatitude(), lastLocation.getLongitude(), new Timestamp(System.currentTimeMillis()), currentUser);
        EditableClusterItem itemWithCurrentLocation = new EditableClusterItem(location, true);
        if (mClusterManager.hasItemOfUser(currentUser.getId())) {
            mClusterManager.updatePosition(itemWithCurrentLocation);
        } else {
            mClusterManager.addItem(itemWithCurrentLocation);
            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lastLocation.getLatitude(),
                    lastLocation.getLongitude()), 10));
        }
        mClusterManager.cluster();
    }

    public void updateMembersMarkers(List<bekrina.whereismobile.model.Location> membersLocations) {
        for (bekrina.whereismobile.model.Location location : membersLocations) {

            EditableClusterItem memberItemWithNewPosition = new EditableClusterItem(location, false);
            if (mClusterManager.hasItemOfUser(location.getUser().getId())) {
                mClusterManager.updatePosition(memberItemWithNewPosition);
            } else {
                mClusterManager.addItem(memberItemWithNewPosition);
            }
        }
        mClusterManager.cluster();
    }

    protected void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            LocationRequest locationRequest = LocationRequest.create();
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
        mRestManager.processGroupStatus(this);

        Gson gson = new Gson();
        Group group = gson.fromJson(getSharedPreferences(Constants.GROUP_INFO_PREFERENCES, 0)
                .getString(Constants.GROUP, ""), Group.class);
        if (group == null) {
            mGroupNameItem.setTitle(getString(R.string.no_group_menu_item));
        } else {
            mGroupNameItem.setTitle(group.getName());
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.group_name_menu_item:
                if (mInGroup) {
                    Intent groupInfoIntent = new Intent(getBaseContext(), GroupInfoActivity.class);
                    startActivity(groupInfoIntent);
                }
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
                mRestManager.leaveGroup(this);
                return true;
            case R.id.sign_out_menu_item:
                mGoogleApiHelper.signOut(this);
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        mUserLocation = location;
        updateUserMarker(location);
    }

    @Override
    public void onMembersLocationsUpdate(List<bekrina.whereismobile.model.Location> locations) {
        updateMembersMarkers(locations);
    }

    @Override
    public void onUserHasGroup(Group group) {
        mInGroup = true;

        mCreateGroupItem.setVisible(false);
        mJoinGroupItem.setVisible(false);
        mLeaveGroupItem.setVisible(true);
        mInviteToGroupItem.setVisible(true);

        mGroupNameItem.setTitle(group.getName());

        Intent startUserLocationsIntent = new Intent(this,
                LocationSavingService.class);
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

        Intent stopUserLocationsIntent = new Intent(this,
                LocationSavingService.class);
        stopService(stopUserLocationsIntent);
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
        mUserMarker = null;
        updateUserMarker(mUserLocation);
        mRestManager.processGroupStatus(this);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.user_left_group_dialog_message)
                .setTitle(R.string.user_left_group_dialog_title);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
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
}