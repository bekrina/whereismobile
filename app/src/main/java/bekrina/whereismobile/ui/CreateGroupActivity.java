package bekrina.whereismobile.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import bekrina.whereismobile.R;
import bekrina.whereismobile.util.Constants;
import bekrina.whereismobile.util.SingletonNetwork;

import static bekrina.whereismobile.util.Constants.*;

public class CreateGroupActivity extends AppCompatActivity {
    private static final String TAG = "CreateGroupActivity";
    private SingletonNetwork mSingletonNetwork;
    private EditText mGroupNameView;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_group);

        mSingletonNetwork = SingletonNetwork.getInstance(this);
        mGroupNameView = (EditText) findViewById(R.id.group_name_field);

        final Button button = (Button) findViewById(R.id.submit_group);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                JSONObject group = new JSONObject();
                try {
                    group.put(Constants.NAME, mGroupNameView.getText());
                    JsonObjectRequest createGroup = new JsonObjectRequest(Request.Method.PUT,
                            GROUP_ENDPOINT, group, new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject group) {
                            try {
                            Intent intent = new Intent(getBaseContext(), GroupInfoActivity.class);
                            SharedPreferences preferences = getSharedPreferences(Constants.GROUP_INFO_PREFERENCES, 0);
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.putString(Constants.GROUP_NAME, group.getString(Constants.NAME));
                            editor.putString(Constants.GROUP_IDENTITY, group.getString(Constants.IDENTITY));
                            editor.apply();
                            startActivity(intent);
                            } catch (JSONException e) {
                                Log.e(TAG, "During new group sending:", e);
                            }
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.e(TAG, "Error during \"get groups\" request", error);
                        }
                    }) {
                    };
                    mSingletonNetwork.getRequestQueue().add(createGroup);
                } catch (JSONException e) {
                    Log.e(TAG, "Error during JSONObject creation", e);
                    //TODO: show popup about error during group creation
                }
            }
        });
    }

}
