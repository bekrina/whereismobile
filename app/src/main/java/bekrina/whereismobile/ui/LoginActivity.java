package bekrina.whereismobile.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;

import bekrina.whereismobile.R;
import bekrina.whereismobile.model.User;
import bekrina.whereismobile.util.App;
import bekrina.whereismobile.util.SingletonNetwork;
import retrofit2.Call;
import retrofit2.Callback;

import static bekrina.whereismobile.util.Constants.USER;
import static bekrina.whereismobile.util.Constants.USER_INFO_PREFERENCES;

public class LoginActivity extends FragmentActivity implements
        GoogleApiClient.OnConnectionFailedListener,
        View.OnClickListener {
    public static final String TAG = LoginActivity.class.getName();

    private static final int RC_GET_AUTH_CODE = 9003;
    private static final String PROPERTIES_FILE = "app.properties";
    private static final String CLIENT_ID_PROPERTY = "client_id_debug";

    private GoogleApiClient mGoogleApiClient;
    private SingletonNetwork mNetwork;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);

        mNetwork = SingletonNetwork.getInstance(this);

        // Button click listeners
        findViewById(R.id.sign_in_button).setOnClickListener(this);

        // [START configure_signin]
        Properties properties = new Properties();
        try {
            properties.load(getAssets().open(PROPERTIES_FILE));
        } catch (IOException e) {
            e.printStackTrace();
        }

        String serverClientId = properties.getProperty(CLIENT_ID_PROPERTY);
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestServerAuthCode(serverClientId)
                .requestEmail()
                .requestProfile()
                .build();
        // [END configure_signin]

        // Build GoogleAPIClient with the Google Sign-In API and the above options.
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* AppCompatActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();
    }

    private void getAuthCode() {
        // Get unique cookie from server
        App.getApi().getAuthCode().enqueue(new Callback<HashMap>() {
            @Override
            public void onResponse(Call<HashMap> call, retrofit2.Response<HashMap> response) {
                Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
                startActivityForResult(signInIntent, RC_GET_AUTH_CODE);
            }

            @Override
            public void onFailure(Call<HashMap> call, Throwable t) {
                Log.e(TAG, "getAuthCode: ", t);
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_GET_AUTH_CODE) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            Log.d(TAG, "onActivityResult:GET_AUTH_CODE:success:" + result.getStatus().isSuccess());

            if (result.isSuccess()) {
                // [START get_auth_code]
                GoogleSignInAccount acct = result.getSignInAccount();
                String authCode = acct.getServerAuthCode();

                final byte[] authCodeBytes = authCode.getBytes();

                App.getApi().login(authCodeBytes).enqueue(new Callback<User>() {
                    @Override
                    public void onResponse(Call<User> call, retrofit2.Response<User> response) {
                        SharedPreferences sharedPreferences = getSharedPreferences(USER_INFO_PREFERENCES, 0);
                        sharedPreferences.edit().putString(USER, new Gson().toJson(response.body())).apply();
                        Intent intent = new Intent(LoginActivity.this, MapActivity.class);
                        startActivity(intent);
                    }

                    @Override
                    public void onFailure(Call<User> call, Throwable t) {
                        Log.e(TAG, "onActivityResult (login): ", t);
                    }
                });
            } else {
                //TODO: show error message
                Log.e(TAG, "OnActivityResult: wrong request code");
            }
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        // An unresolvable error has occurred and Google APIs (including Sign-In) will not
        // be available.
        Log.d(TAG, "onConnectionFailed:" + connectionResult);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sign_in_button:
                getAuthCode();
                break;
        }
    }
}
