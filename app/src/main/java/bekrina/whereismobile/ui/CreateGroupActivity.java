package bekrina.whereismobile.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import bekrina.whereismobile.R;
import bekrina.whereismobile.services.ApiService;

public class CreateGroupActivity extends AppCompatActivity
        implements ApiService.CreateGroupListener {
    private static final String TAG = CreateGroupActivity.class.getName();

    private ApiService mApiService;
    private EditText mGroupNameView;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_group);

        mApiService = ApiService.getInstance(this);
        mGroupNameView = (EditText) findViewById(R.id.group_name_field);

        final Button button = (Button) findViewById(R.id.submit_group);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mApiService.createGroup(mGroupNameView.getText().toString(),
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
        //TODO: show sad screen
    }
}
