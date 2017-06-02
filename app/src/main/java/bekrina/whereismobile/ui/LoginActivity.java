package bekrina.whereismobile.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.gson.Gson;

import org.json.JSONObject;

import java.io.IOException;
import java.util.Properties;

import bekrina.whereismobile.R;
import bekrina.whereismobile.model.User;
import bekrina.whereismobile.util.SingletonNetwork;

import static bekrina.whereismobile.util.Constants.LOGIN_ENDPOINT;
import static bekrina.whereismobile.util.Constants.USER;
import static bekrina.whereismobile.util.Constants.USER_INFO_PREFERENCES;

public class LoginActivity extends FragmentActivity implements
        GoogleApiClient.OnConnectionFailedListener,
        View.OnClickListener {
    public static final String TAG = LoginActivity.class.getName();

    private static final int RC_SIGN_IN = 9003;
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

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(CLIENT_ID_PROPERTY)
                .requestEmail()
                .build();

        // Build GoogleAPIClient with the Google Sign-In API and the above options.
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* AppCompatActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();
    }

    private void startSignInActivity() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            Log.d(TAG, "onActivityResult:GET_AUTH_CODE:success:" + result.getStatus().isSuccess());

            if (result.isSuccess()) {
                GoogleSignInAccount account = result.getSignInAccount();

                String authCode = account.getIdToken();
                final byte[] authCodeBytes = authCode.getBytes();

                JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, LOGIN_ENDPOINT,
                        null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Gson gson = new Gson();
                        if (gson.fromJson(response.toString(), User.class) != null) {
                            SharedPreferences sharedPreferences = getSharedPreferences(USER_INFO_PREFERENCES, 0);
                            sharedPreferences.edit().putString(USER, response.toString()).apply();
                            Intent intent = new Intent(LoginActivity.this, MapActivity.class);
                            startActivity(intent);
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                    }
                }) {
                    @Override
                    public byte[] getBody() {
                        return authCodeBytes;
                    }
                };

                // Add the request to the RequestQueue.
                mNetwork.getRequestQueue().add(request);
            }
        } else {
                //TODO: show error message
                Log.e(TAG, "OnActivityResult: wrong request code");
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
                startSignInActivity();
                break;
        }
    }
}
