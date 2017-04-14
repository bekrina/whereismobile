package bekrina.whereismobile.ui;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
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
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.algo.Algorithm;
import com.google.maps.android.clustering.algo.NonHierarchicalDistanceBasedAlgorithm;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.google.maps.android.ui.IconGenerator;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import bekrina.whereismobile.R;
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

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback, LocationListener,
        LocationsUpdatedListener, GroupStatusListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LeaveGroupListener,
        GoogleApiHelper.SignOutListener {

    private static final String TAG = MapActivity.class.getName();

    private MenuItem mLeaveGroupItem;
    private MenuItem mCreateGroupItem;
    private MenuItem mJoinGroupItem;
    private MenuItem mGroupNameItem;
    private MenuItem mInviteToGroupItem;

    private RestManager mRestManager;
    private GoogleApiHelper mGoogleApiHelper;

    private GoogleMap mGoogleMap;
    private ClusterManager<MyClusterItem> mClusterManager;
    private Algorithm<MyClusterItem> mClusterAlgorithm;

    private Location mUserLocation;

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

        //ProgressDialog.show(this, "Loading", "Loading the map...");
        //TODO: включить анимацию загрузки
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
            googleMap.setMyLocationEnabled(true);
            mGoogleMap = googleMap;mGoogleMap.getUiSettings().setMyLocationButtonEnabled(false);
            mGoogleMap.setMyLocationEnabled(false);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_FINE_LOCATION);
        }
        mClusterManager = new ClusterManager<>(this, mGoogleMap);
        mGoogleMap.setOnCameraChangeListener(mClusterManager);
        final CustomClusterRenderer renderer = new CustomClusterRenderer(this, mGoogleMap, mClusterManager);
        mClusterAlgorithm = new NonHierarchicalDistanceBasedAlgorithm<>();
        mClusterManager.setAlgorithm(mClusterAlgorithm);
        mClusterManager.setRenderer(renderer);
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

    public void updateUserMarker(Location lastLocation) {
        mUserLocation = lastLocation;

        //TODO: discuss: как правильно хранить данные? Может лучше создавать объект с текущей группой при старте приложения и хранить его в памяти?
        // в таком случае правильно ли хранить это в поле активити? или нужен отдельный класс-хендлер?
        //TODO: change this ASAP, all was hardcoded for testing. Need to use Model for current user.
/*        mUserLocation = new bekrina.whereismobile.model.Location(lastLocation.getLatitude(),
                lastLocation.getLongitude(), new Timestamp(System.currentTimeMillis()),
                new User("me", "me", "me", new Group("test", "test")));*/
        MyClusterItem clusterItem = new MyClusterItem(9999999, getString(R.string.user_location_marker),
                new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()),
                new Date(System.currentTimeMillis()));
        if (mClusterAlgorithm.getItems().isEmpty() || !mClusterAlgorithm.getItems().contains(clusterItem)) {
            mClusterManager.addItem(clusterItem);

            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lastLocation.getLatitude(),
                    lastLocation.getLongitude()), 10));
        } else {
            mClusterAlgorithm.getItems().remove(clusterItem);
            //mClusterManager.clearItems();
            //mClusterManager.setAlgorithm(mClusterAlgorithm);
            mClusterManager.addItem(clusterItem);
        }
        mClusterManager.cluster();

        /*IconGenerator generator = new IconGenerator(this);
        generator.setStyle(IconGenerator.STYLE_ORANGE);
        //generator.setColor(R.color.accent_orange);
        Bitmap icon = generator.makeIcon(desc);*/
    }

    public void updateMembersMarkers(List<bekrina.whereismobile.model.Location> membersLocations) {
        IconGenerator generator = new IconGenerator(this);
        generator.setStyle(IconGenerator.STYLE_BLUE);
        for (bekrina.whereismobile.model.Location location : membersLocations) {

            Date locationTime = new Date(location.getTimestamp().getTime());
/*            SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            String locationTimeFormatted = formatter.format(locationTime);*/

            MyClusterItem memberClusterItem = new MyClusterItem(location.getUser().getId(), location.getUser().getFirstName()
                    , new LatLng(location.getLatitude(), location.getLongitude()), locationTime);
            if (mClusterAlgorithm.getItems().isEmpty() || !mClusterAlgorithm.getItems().contains(memberClusterItem)) {
                mClusterManager.addItem(memberClusterItem);
                mClusterManager.addItem(memberClusterItem);
                mClusterManager.addItem(memberClusterItem);
            } else {
                mClusterAlgorithm.getItems().remove(memberClusterItem);
                //mClusterManager.clearItems();
                //mClusterManager.setAlgorithm(mClusterAlgorithm);
                mClusterManager.addItem(memberClusterItem);
                mClusterManager.addItem(memberClusterItem);
                mClusterManager.addItem(memberClusterItem);
                mClusterManager.addItem(memberClusterItem);
            }


            /*Marker memberMarker = mGoogleMap.addMarker(new MarkerOptions()
                    .icon(BitmapDescriptorFactory.fromBitmap(generator.makeIcon(location.getUser().getFirstName())))
                    .title(locationTimeFormatted)
                    .position(new LatLng(location.getLatitude(), location.getLongitude())));
            mMembersMarkers.put(location.getUser().getId(), memberMarker);*/
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
        updateUserMarker(location);
    }

    @Override
    public void onMembersLocationsUpdate(List<bekrina.whereismobile.model.Location> locations) {
        updateMembersMarkers(locations);
    }

    @Override
    public void onUserHasGroup(Group group) {
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
        mClusterManager.clearItems();
        updateUserMarker(mUserLocation);
        mRestManager.processGroupStatus(this);
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




    /*// Declare a variable for the cluster manager.
    private ClusterManager<MyItem> mClusterManager;

    private void setUpClusterer() {
        // Position the map.
        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(51.503186, -0.126446), 10));

        // Initialize the manager with the context and the map.
        // (Activity extends context, so we can pass 'this' in the constructor.)
        mClusterManager = new ClusterManager<MyItem>(this, mGoogleMap);

        // Point the map's listeners at the listeners implemented by the cluster
        // manager.
        getMap().setOnCameraIdleListener(mClusterManager);
        getMap().setOnMarkerClickListener(mClusterManager);

        // Add cluster items (markers) to the cluster manager.
        addItems();
    }

    private void addItems() {

        // Set some lat/lng coordinates to start with.
        double lat = 51.5145160;
        double lng = -0.1270060;

        // Add ten cluster items in close proximity, for purposes of this example.
        for (int i = 0; i < 10; i++) {
            double offset = i / 60d;
            lat = lat + offset;
            lng = lng + offset;
            MyItem offsetItem = new MyItem(lat, lng);
            mClusterManager.addItem(offsetItem);
        }
    }

}*/

class MyClusterItem implements ClusterItem {
    private final int userId;
    private final String title;
    private final LatLng latLng;
    private final Date date;

    public MyClusterItem(int userId, String title, LatLng latLng, Date date) {
        this.userId = userId;
        this.title = title;
        this.latLng = latLng;
        this.date = date;
    }

    @Override
    public LatLng getPosition() {
        return latLng;
    }

    public String getTitle() {
        return title;
    }

    public Date getDate() {
        return date;
    }

    public int getUserId() {
        return userId;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof MyClusterItem)) {
            return  false;
        }
        MyClusterItem item = (MyClusterItem) object;
        if (item.getTitle().equals(title) && item.getUserId() == userId) {
            return true;
        }
        return false;
    }

}

class CustomClusterRenderer extends DefaultClusterRenderer<MyClusterItem> {
    private final Context mContext;
    public CustomClusterRenderer(Context context, GoogleMap map,
                                 ClusterManager<MyClusterItem> clusterManager) {
        super(context, map, clusterManager);
        mContext = context;
    }

    @Override
    protected void onBeforeClusterItemRendered(MyClusterItem item,
                                               MarkerOptions markerOptions) {
        final BitmapDescriptor markerDescriptor = BitmapDescriptorFactory.defaultMarker(
                BitmapDescriptorFactory.HUE_ORANGE);
        markerOptions.icon(markerDescriptor).snippet(item.getTitle());
    }

/*    @Override
     protected void onBeforeClusterRendered(Cluster<MyClusterItem> cluster,
                                            MarkerOptions markerOptions) {
        final BitmapDescriptor markerDescriptor = BitmapDescriptorFactory.defaultMarker(
                BitmapDescriptorFactory.HUE_AZURE);
         markerOptions.icon(markerDescriptor).snippet("test");
     }*/
}
