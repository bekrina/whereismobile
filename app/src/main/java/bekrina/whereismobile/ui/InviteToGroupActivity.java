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
import bekrina.whereismobile.listeners.InviteStatusListener;
import bekrina.whereismobile.services.ApiRequestsManager;
import bekrina.whereismobile.util.Validation;

public class InviteToGroupActivity extends AppCompatActivity implements InviteStatusListener {
    public static final String TAG = InviteToGroupActivity.class.getName();

    private EditText mInvitationEmail;
    private ApiRequestsManager mApiRequestsManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_invite_to_group);

        mInvitationEmail = (EditText) findViewById(R.id.invitation_email_field);
        mApiRequestsManager = ApiRequestsManager.getInstance(this);

        final Button submit = (Button) findViewById(R.id.submit_invite);
        submit.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (Validation.isValidEmail(mInvitationEmail.getText())) {
                    mInvitationEmail.setError(null);
                    mApiRequestsManager.inviteToGroup(InviteToGroupActivity.this,
                            mInvitationEmail.getText().toString(), InviteToGroupActivity.this);
                } else {
                    mInvitationEmail.setError("Email is not valid");
                }
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
