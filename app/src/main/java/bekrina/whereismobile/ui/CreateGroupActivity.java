package bekrina.whereismobile.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
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
import bekrina.whereismobile.util.SingletonNetwork;

public class CreateGroupActivity extends AppCompatActivity {
    public static final String GROUP_NAME_EXTRA = "group_name";
    public static final String GROUP_IDENTITY_EXTRA = "group_identity";

    private static final String IDENTITY = "identity";
    private static final String NAME = "name";
    private SingletonNetwork mSingletonNetwork;
    private EditText mGroupNameView;
    private Handler handler;
    private static final int RESPONSE_BAD = 0;
    private static final int RESPONSE_SUCCESSFULL = 1;
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_group);

        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message inputMessage) {
                switch (inputMessage.what) {
                    case (RESPONSE_SUCCESSFULL):
                        JSONObject group = (JSONObject) inputMessage.obj;
                        Intent intent = new Intent(getBaseContext(), GroupInfoActivity.class);
                        try {
                            intent.putExtra(GROUP_NAME_EXTRA, group.getString(NAME));
                            intent.putExtra(GROUP_IDENTITY_EXTRA, group.getString(IDENTITY));
                            startActivity(intent);
                        } catch (JSONException e) {
                            Log.e(this.getClass().getName(), "Exception during showing information about new group", e);
                        }

                        break;
                    case (RESPONSE_BAD):
                        //TODO: show message
                        break;
                }
            }
        };

        mSingletonNetwork = SingletonNetwork.getInstance(this);
        mGroupNameView = (EditText) findViewById(R.id.group_name_field);

        final Button button = (Button) findViewById(R.id.submit_group);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                JSONObject group = new JSONObject();
                try {
                    group.put(NAME, mGroupNameView.getText());
                    JsonObjectRequest createGroup = new JsonObjectRequest(Request.Method.PUT,
                            getString(R.string.api_url) + getString(R.string.create_group_url),
                            group, new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                Message message = new Message();
                                if (response.getString(IDENTITY) != null) {
                                    message.what = RESPONSE_SUCCESSFULL;
                                    message.obj = response;
                                } else {
                                    message.what = RESPONSE_BAD;
                                }
                                handler.sendMessage(message);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.e(this.getClass().getName(), "Error during \"get groups\" request", error);
                        }
                    }) {
                    };
                    mSingletonNetwork.getRequestQueue().add(createGroup);
                } catch (JSONException e) {
                    Log.e(this.getClass().getName(), "Error during JSONObject creation", e);
                    //TODO: show popup about error during group creation


                }

            }
        });
    }

}
