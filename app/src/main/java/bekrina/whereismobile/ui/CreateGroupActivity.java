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
import bekrina.whereismobile.listeners.CreateGroupListener;
import bekrina.whereismobile.services.ApiRequestsManager;

public class CreateGroupActivity extends AppCompatActivity
        implements CreateGroupListener {
    //TODO: объединить похожие активити с создать родительский класс
    private static final String TAG = CreateGroupActivity.class.getName();

    private ApiRequestsManager mApiRequestsManager;
    private EditText mGroupNameView;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_group);

        mApiRequestsManager = ApiRequestsManager.getInstance(this);
        mGroupNameView = (EditText) findViewById(R.id.group_name_field);

        final Button button = (Button) findViewById(R.id.submit);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mApiRequestsManager.createGroup(mGroupNameView.getText().toString(),
                        CreateGroupActivity.this);
            }
        });
    }

    @Override
    public void onGroupCreated() {
        Intent intent = new Intent(this, GroupInfoActivity.class);
        startActivity(intent);
    }

    @Override
    public void onGroupCreationFailed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(CreateGroupActivity.this);
        builder.setMessage(R.string.group_not_created_dialog_message)
                .setTitle(R.string.group_not_created_dialog_title);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
