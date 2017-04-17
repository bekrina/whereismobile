package bekrina.whereismobile.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import bekrina.whereismobile.R;
import bekrina.whereismobile.listeners.JoinedToGroupListener;
import bekrina.whereismobile.services.ApiRequestsManager;

public class JoinGroupActivity extends AppCompatActivity implements JoinedToGroupListener {
    public static final String TAG = JoinGroupActivity.class.getName();

    private EditText mGroupIdentity;
    private ApiRequestsManager mApiRequestsManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_join_group);

        mGroupIdentity = (EditText) findViewById(R.id.group_identity_field);
        mApiRequestsManager = ApiRequestsManager.getInstance(this);

        final Button submit = (Button) findViewById(R.id.submit);

        submit.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String groupIdentity = mGroupIdentity.getText().toString();
                if (groupIdentity.equals("")) {
                    mGroupIdentity.setError("Empty identity");
                } else {
                    mGroupIdentity.setError(null);
                    mApiRequestsManager.joinGroup(groupIdentity, JoinGroupActivity.this);
                }
            }
        });
    }

    @Override
    public void onJoined() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.joined_dialog_message)
                .setTitle(R.string.joined_dialog_title);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Intent intent = new Intent(JoinGroupActivity.this, MapActivity.class);
                startActivity(intent);
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public void onJoinFailed(int statusCode) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.no_invite_for_user_dialog_message)
                .setTitle(R.string.no_invite_for_user_dialog_title);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
