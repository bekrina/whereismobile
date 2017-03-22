package bekrina.whereismobile.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import bekrina.whereismobile.R;
import bekrina.whereismobile.util.Constants;

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
        SharedPreferences preferences = getSharedPreferences(Constants.GROUP_INFO_PREFERENCES, 0);
        mGroupName.setText(preferences.getString(Constants.GROUP_NAME, ""));
        mGroupIdentity.setText(preferences.getString(Constants.GROUP_IDENTITY, ""));
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(getBaseContext(), MapActivity.class));
        finish();
    }
}
