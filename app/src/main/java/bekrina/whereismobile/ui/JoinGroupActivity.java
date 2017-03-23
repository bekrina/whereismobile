package bekrina.whereismobile.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.ServerError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;
import com.google.api.client.http.HttpStatusCodes;

import org.json.JSONObject;

import bekrina.whereismobile.R;
import bekrina.whereismobile.util.SingletonNetwork;

import static bekrina.whereismobile.util.Constants.GROUP_ENDPOINT;
import static bekrina.whereismobile.util.Constants.GROUP_INFO_PREFERENCES;
import static bekrina.whereismobile.util.Constants.JOIN_ACTION;

public class JoinGroupActivity extends AppCompatActivity {
    public static final String TAG = "JoinGroupActivity";

    private EditText mGroupIdentity;
    private SingletonNetwork mNetwork;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_join_group);

        mGroupIdentity = (EditText) findViewById(R.id.group_identity_field);
        mNetwork = SingletonNetwork.getInstance(getBaseContext());

        final Button submit = (Button) findViewById(R.id.submit_join);

        submit.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //TODO: check if user entered identity
                //TODO: change backend to return group
                //TODO: display group name and change shared prefs for current group
                SharedPreferences preferences = getSharedPreferences(GROUP_INFO_PREFERENCES, 0);
                StringRequest joinRequest = new StringRequest(Request.Method.POST,
                        GROUP_ENDPOINT + "/" + mGroupIdentity.getText()
                        + JOIN_ACTION, new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(JoinGroupActivity.this);
                        builder.setMessage(R.string.joined_dialog_message)
                                .setTitle(R.string.joined_dialog_title);
                        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Intent intent = new Intent(getBaseContext(), MapActivity.class);
                                startActivity(intent);
                            }
                        });
                        AlertDialog dialog = builder.create();
                        dialog.show();
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (error.networkResponse.statusCode == 400) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(JoinGroupActivity.this);
                            builder.setMessage(R.string.no_invite_for_user_dialog_message)
                                    .setTitle(R.string.no_invite_for_user_dialog_title);
                            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.dismiss();
                                }
                            });
                            AlertDialog dialog = builder.create();
                            dialog.show();
                        } else {
                            Log.e(TAG, "Error during joining group:", error);
                        }
                    }
                }) {
                    @Override
                    protected Response<String> parseNetworkResponse(NetworkResponse response) {
                        if (response.statusCode == HttpStatusCodes.STATUS_CODE_OK) {
                            return Response.success("",
                                    HttpHeaderParser.parseCacheHeaders(response));
                        } else {
                            Log.e(TAG, "Error during sending of invite, status code:" + response.statusCode);
                            return Response.error(new ServerError());
                        }
                    }
                };
                mNetwork.getRequestQueue().add(joinRequest);
            }
        });
    }
}
