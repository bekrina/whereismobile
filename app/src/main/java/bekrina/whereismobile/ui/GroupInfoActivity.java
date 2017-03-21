package bekrina.whereismobile.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import bekrina.whereismobile.R;

public class GroupInfoActivity extends AppCompatActivity{
    private TextView mGroupName;
    private TextView mGroupIdentity;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_info);

        mGroupName = (TextView) findViewById(R.id.group_name);
        mGroupIdentity = (TextView) findViewById(R.id.group_identity);
    }

    @Override
    public void onStart() {
        super.onStart();
        String groupName = getIntent().getStringExtra(CreateGroupActivity.GROUP_NAME_EXTRA);
        mGroupName.setText(groupName);
        String groupIdentity = getIntent().getStringExtra(CreateGroupActivity.GROUP_IDENTITY_EXTRA);
        mGroupIdentity.setText(groupIdentity);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(getBaseContext(), MapActivity.class));
        finish();
    }
}
