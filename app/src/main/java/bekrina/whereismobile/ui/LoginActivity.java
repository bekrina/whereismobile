package bekrina.whereismobile.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;

import java.io.IOException;
import java.util.Properties;

import bekrina.whereismobile.R;
import bekrina.whereismobile.util.SingletonNetwork;

import static android.provider.AlarmClock.EXTRA_MESSAGE;

public class LoginActivity extends AppCompatActivity implements
        GoogleApiClient.OnConnectionFailedListener,
        View.OnClickListener {

    public static final String TAG = "LoginActivity";
    private static final int RC_GET_AUTH_CODE = 9003;
    private static final String PROPERTIES_FILE = "app.properties";
    private static final String CLIENT_ID_PROPERTY = "client_id";

    private GoogleApiClient mGoogleApiClient;
    private TextView mAuthCodeTextView;
    private SingletonNetwork mNetwork;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);

        mNetwork = SingletonNetwork.getInstance(this);

        // Views
        mAuthCodeTextView = (TextView) findViewById(R.id.detail);

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
        final TextView mTextView = (TextView) findViewById(R.id.status);
        // Get unique cookie from server
        StringRequest requestCookie = new StringRequest(Request.Method.GET,
                getString(R.string.api_url) + getString(R.string.initial_cookie_url),
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
                        startActivityForResult(signInIntent, RC_GET_AUTH_CODE);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                mTextView.setText(getString(R.string.error_during_login));
            }
        }
        );
        mNetwork.getRequestQueue().add(requestCookie);

    }
/*
    private void signOut() {
        Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        Log.d(TAG, "signOut:onResult:" + status);
                        updateUI(false);
                    }
                });
    }

    private void revokeAccess() {
        Auth.GoogleSignInApi.revokeAccess(mGoogleApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        Log.d(TAG, "revokeAccess:onResult:" + status);
                        updateUI(false);
                    }
                });
    }*/


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

                // Show signed-in UI.
                mAuthCodeTextView.setText(getString(R.string.auth_code_fmt, authCode));
                updateUI(true);
                // [END get_auth_code]

                final byte[] authCodeBytes = authCode.getBytes();

                final TextView mTextView = (TextView) findViewById(R.id.status);

                // Request a string response from the provided URL.
                StringRequest tokenRequest = new StringRequest(Request.Method.POST,
                        getString(R.string.api_url) + getString(R.string.login_url),
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                mTextView.setText(getString(R.string.login_successfull));
                                Intent intent = new Intent(LoginActivity.this, MapActivity.class);
                                intent.putExtra(EXTRA_MESSAGE, "message");

                                startActivity(intent);
                            }
                        }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        mTextView.setText(getString(R.string.error_during_login));
                    }
                }) {
                    @Override
                    public byte[] getBody() throws AuthFailureError {
                        return authCodeBytes;
                    }
                };
                // Add the request to the RequestQueue.
                mNetwork.getRequestQueue().add(tokenRequest);
            } else {
                final TextView mTextView = (TextView) findViewById(R.id.status);
                mTextView.setText(getString(R.string.login_failed));
                // Show signed-out UI.
                updateUI(false);
            }
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        // An unresolvable error has occurred and Google APIs (including Sign-In) will not
        // be available.
        Log.d(TAG, "onConnectionFailed:" + connectionResult);
    }

    private void updateUI(boolean signedIn) {
        if (signedIn) {
            ((TextView) findViewById(R.id.status)).setText(R.string.signed_in);

            findViewById(R.id.sign_in_button).setVisibility(View.GONE);
            findViewById(R.id.sign_out_and_disconnect).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.sign_in_button).setVisibility(View.VISIBLE);
            findViewById(R.id.sign_out_and_disconnect).setVisibility(View.GONE);
        }
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
