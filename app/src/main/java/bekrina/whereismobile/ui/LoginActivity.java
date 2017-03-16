package bekrina.whereismobile.ui;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.Properties;

import bekrina.whereismobile.R;

import static android.provider.AlarmClock.EXTRA_MESSAGE;
import static com.android.volley.Request.Method;

public class LoginActivity extends AppCompatActivity implements
        GoogleApiClient.OnConnectionFailedListener,
        View.OnClickListener {

    public static final String TAG = "ServerAuthCodeActivity";
    private static final int RC_GET_AUTH_CODE = 9003;

    private GoogleApiClient mGoogleApiClient;
    private TextView mAuthCodeTextView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);

        // Views
        mAuthCodeTextView = (TextView) findViewById(R.id.detail);

        // Button click listeners
        findViewById(R.id.sign_in_button).setOnClickListener(this);
        findViewById(R.id.sign_out_button).setOnClickListener(this);
        findViewById(R.id.disconnect_button).setOnClickListener(this);

        // [START configure_signin]
        // Configure sign-in to request offline access to the user's ID, basic
        // profile, and Google Drive. The first time you request a code you will
        // be able to exchange it for an access token and refresh token, which
        // you should store. In subsequent calls, the code will only result in
        // an access token. By asking for profile access (through
        // DEFAULT_SIGN_IN) you will also get an ID Token as a result of the
        // code exchange.

        Properties properties = new Properties();
        try {
            properties.load(getAssets().open("app.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        String serverClientId = properties.getProperty("client_id");
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                //.requestScopes(new Scope(Scopes.PROFILE))
                .requestServerAuthCode(serverClientId)
                .requestEmail()
                .requestProfile()
                .build();
        // [END configure_signin]

        // Build GoogleAPIClient with the Google Sign-In API and the above options.
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        final TextView mTextView = (TextView) findViewById(R.id.status);
        // Get unique cookie from server
        RequestQueue queue = Volley.newRequestQueue(this);
        StringRequest requestCookie = new StringRequest(Method.GET,
                "https://rocky-river-45878.herokuapp.com/api/config.json",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display the first 500 characters of the response string.
                        mTextView.setText("Response is: "+ response);
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        mTextView.setText("That didn't work!" + error);
                    }
                }
        );
        CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
        queue.add(requestCookie);
    }

    private void getAuthCode() {
        // Start the retrieval process for a server auth code.  If requested, ask for a refresh
        // token.  Otherwise, only get an access token if a refresh token has been previously
        // retrieved.  Getting a new access token for an existing grant does not require
        // user consent.
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_GET_AUTH_CODE);
    }

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

                // Show signed-in UI.
                mAuthCodeTextView.setText(getString(R.string.auth_code_fmt, authCode));
                updateUI(true);

                // TODO(user): send code to server and exchange for access/refresh/ID tokens.
                // [END get_auth_code]
                final byte[] authCodeBytes = authCode.getBytes();
                // Instantiate the RequestQueue.
                RequestQueue queue = Volley.newRequestQueue(this);
                String url = "https://rocky-river-45878.herokuapp.com/api/login/";

                JSONObject jsonBody = new JSONObject();
                try {
                    jsonBody.put("code", authCode);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                final TextView mTextView = (TextView) findViewById(R.id.status);
                final Intent intent = new Intent(this, MapActivity.class);
                // Request a string response from the provided URL.
                StringRequest tokenRequest = new StringRequest(Method.POST, url,
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                // Display the first 500 characters of the response string.
                                mTextView.setText("Response is: "+ response);


                                //EditText editText = (EditText) findViewById(R.id.editText);
                                //String message = editText.getText().toString();
                                intent.putExtra(EXTRA_MESSAGE, "message");

                                startActivity(intent);
                            }
                        }, new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                mTextView.setText("That didn't work!" + error);
                            }
                }) {
                    @Override
                    public byte[] getBody() throws AuthFailureError {
                        return authCodeBytes;
                    }
                };
                CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
                // Add the request to the RequestQueue.
                queue.add(tokenRequest);
            } else {
                final TextView mTextView = (TextView) findViewById(R.id.status);
                mTextView.setText("That didn't work!" + result.getStatus());
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
            //((TextView) findViewById(R.id.status)).setText(R.string.signed_out);
            //mAuthCodeTextView.setText(getString(R.string.auth_code_fmt, "null"));

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
            case R.id.sign_out_button:
                signOut();
                break;
            case R.id.disconnect_button:
                revokeAccess();
                break;
        }
    }
}
