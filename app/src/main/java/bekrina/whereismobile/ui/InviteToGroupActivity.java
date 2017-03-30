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
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.api.client.http.HttpStatusCodes;

import org.json.JSONException;
import org.json.JSONObject;

import bekrina.whereismobile.R;
import bekrina.whereismobile.services.ApiService;
import bekrina.whereismobile.util.SingletonNetwork;

import static bekrina.whereismobile.util.Constants.*;

public class InviteToGroupActivity extends AppCompatActivity implements ApiService.InviteStatusListener {
    public static final String TAG = InviteToGroupActivity.class.getName();

    private EditText mInvitationEmail;
    private ApiService mApiService;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_invite_to_group);

        mInvitationEmail = (EditText) findViewById(R.id.invitation_email_field);
        mApiService = ApiService.getInstance(this);

        final Button submit = (Button) findViewById(R.id.submit_invite);
        submit.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mApiService.inviteToGroup(InviteToGroupActivity.this,
                        mInvitationEmail.getText().toString(), InviteToGroupActivity.this);
            }
        });
    }

    @Override
    public void onInviteSent() {
        AlertDialog.Builder builder = new AlertDialog.Builder(InviteToGroupActivity.this);
        builder.setMessage(R.string.invite_sent_dialog_message)
                .setTitle(R.string.invite_sent_dialog_title);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Intent intent = new Intent(getBaseContext(), MapActivity.class);
                startActivity(intent);
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public void onInviteFailed(int statusCode) {
        AlertDialog.Builder builder = new AlertDialog.Builder(InviteToGroupActivity.this);
        builder.setMessage(R.string.invite_already_exists_dialog_message)
                .setTitle(R.string.invite_already_exists_dialog_title);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
