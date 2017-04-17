package bekrina.whereismobile.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.widget.EditText;
import android.widget.TextView;

import com.google.gson.Gson;

import bekrina.whereismobile.R;
import bekrina.whereismobile.model.Group;
import bekrina.whereismobile.util.Constants;

import static bekrina.whereismobile.util.Constants.GROUP;

public class GroupInfoActivity extends AppCompatActivity{
    private static final String TAG = GroupInfoActivity.class.getName();

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
        Gson gson = new Gson();
        Group group = gson.fromJson(preferences.getString(GROUP, ""), Group.class);
        if (group != null) {
            mGroupName.setText(group.getName());
            mGroupIdentity.setText(group.getIdentity());
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(getBaseContext(), MapActivity.class));
        finish();
    }
}
