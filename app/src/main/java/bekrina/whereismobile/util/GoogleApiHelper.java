package bekrina.whereismobile.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationServices;

import static bekrina.whereismobile.util.Constants.GROUP;
import static bekrina.whereismobile.util.Constants.GROUP_INFO_PREFERENCES;

public class GoogleApiHelper {
    private static final String TAG = GoogleApiHelper.class.getName();
    private GoogleApiClient mGoogleApiClient;
    private Context mContext;
    private SingletonNetwork mNetwork;

    public GoogleApiHelper(Context context) {
        mContext = context;
        mNetwork = SingletonNetwork.getInstance(context);
    }

    public void connectToGoogleApi(GoogleApiClient.ConnectionCallbacks connectionListener,
                                   GoogleApiClient.OnConnectionFailedListener connectionFailedListener) {
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(mContext)
                    .addConnectionCallbacks(connectionListener)
                    .addOnConnectionFailedListener(connectionFailedListener)
                    .addApi(LocationServices.API)
                    .addApi(Auth.GOOGLE_SIGN_IN_API)
                    .addApi(AppIndex.API).build();
            mGoogleApiClient.connect();
        }
    }

    public void disconnectFromGoogleApi() {
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    public GoogleApiClient getGoogleApiClient() {
        return mGoogleApiClient;
    }

    public interface SignOutListener {
        void onSignOut();
    }

    public void signOut(final SignOutListener listener) {
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
                                    SharedPreferences sharedPreferences = mContext.getSharedPreferences(GROUP_INFO_PREFERENCES, 0);
                                    SharedPreferences.Editor editor = sharedPreferences.edit();
                                    editor.remove(GROUP);
                                    editor.apply();
                                    listener.onSignOut();
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
}
