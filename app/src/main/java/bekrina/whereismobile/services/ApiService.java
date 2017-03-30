package bekrina.whereismobile.services;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.ServerError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.google.api.client.http.HttpStatusCodes;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import bekrina.whereismobile.util.Constants;
import bekrina.whereismobile.util.SingletonNetwork;

import static bekrina.whereismobile.util.Constants.EMAIL;
import static bekrina.whereismobile.util.Constants.GET_GROUPS_ACTION;
import static bekrina.whereismobile.util.Constants.GROUP_ENDPOINT;
import static bekrina.whereismobile.util.Constants.GROUP_IDENTITY;
import static bekrina.whereismobile.util.Constants.GROUP_INFO_PREFERENCES;
import static bekrina.whereismobile.util.Constants.INVITE_ACTION;
import static bekrina.whereismobile.util.Constants.JOIN_ACTION;
import static bekrina.whereismobile.util.Constants.LEAVE_ACTION;

// TODO: is this name appropriate?
public class ApiService {
    private static ApiService mInstance;
    private SingletonNetwork mNetwork;
    private Context mContext;

    private static final String TAG = ApiService.class.getName();

    private ApiService(Context context) {
        mNetwork = SingletonNetwork.getInstance(context);
        mContext = context;
    }

    public static synchronized ApiService getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new ApiService(context);
            return mInstance;
        }
        return mInstance;
    }

    public interface JoinedToGroupListener {
        void onJoined();
        void onJoinFailed(int statusCode);
    }
    public void joinGroup(final Activity activity, String groupIdentity,
                          final JoinedToGroupListener listener) {
        //TODO: check if user entered identity
        //TODO: display group name and change shared prefs for current group
        SharedPreferences preferences = activity.getSharedPreferences(GROUP_INFO_PREFERENCES, 0);
        StringRequest joinRequest = new StringRequest(Request.Method.POST,
                GROUP_ENDPOINT + "/" + groupIdentity
                        + JOIN_ACTION, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                listener.onJoined();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                    listener.onJoinFailed(error.networkResponse.statusCode );
                    Log.e(TAG, "Error during joining group:", error);
            }
        }) {
            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse response) {
                if (response.statusCode == HttpStatusCodes.STATUS_CODE_OK) {
                    return Response.success("",
                            HttpHeaderParser.parseCacheHeaders(response));
                } else {
                    Log.e(TAG, "Error during joining group, status code:" + response.statusCode);
                    return Response.error(new ServerError());
                }
            }
        };
        mNetwork.getRequestQueue().add(joinRequest);
    }

    public interface GroupStatusListener {
        void onUserHasGroup(String name, String identity);
        void onUserWithoutGroup();
    }

    public void processGroupStatus(final GroupStatusListener listener) {
        JsonArrayRequest groupRequest = new JsonArrayRequest(Request.Method.GET,
                GROUP_ENDPOINT + GET_GROUPS_ACTION, null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        if (response.optJSONObject(0) == null) {
                            listener.onUserWithoutGroup();
                        } else {
                            try {
                                String name = response.getJSONObject(0).getString(Constants.NAME);
                                String identity = response.getJSONObject(0).getString(Constants.IDENTITY);
                                updateGroupInfoPreferences(name, identity);
                                listener.onUserHasGroup(name, identity);
                            } catch (JSONException e) {
                                Log.e(TAG, "getGroupStatus:", e);
                                listener.onUserWithoutGroup();
                            }
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "tokenRequest.onErrorResponse:", error);
            }
        }) {
        };
        mNetwork.getRequestQueue().add(groupRequest);
    }

    private void updateGroupInfoPreferences(String name, String identity) {
        SharedPreferences preferences = mContext.getSharedPreferences(Constants.GROUP_INFO_PREFERENCES, 0);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(Constants.GROUP_NAME, name);
        editor.putString(Constants.GROUP_IDENTITY, identity);
        editor.apply();
    }

    public interface InviteStatusListener {
        void onInviteSent();
        void onInviteFailed(int statusCode);
    }

    public void inviteToGroup(Activity activity, String emailToInvite,
                              final InviteStatusListener listener) {
        JSONObject invite = new JSONObject();
        try {
            invite.put(EMAIL, emailToInvite);
            SharedPreferences preferences = activity.getSharedPreferences(GROUP_INFO_PREFERENCES, 0);
            JsonObjectRequest createGroup = new JsonObjectRequest(Request.Method.POST,
                    GROUP_ENDPOINT + "/" + preferences.getString(GROUP_IDENTITY, "")
                            + INVITE_ACTION,
                    invite, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    listener.onInviteSent();
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                        listener.onInviteFailed(error.networkResponse.statusCode);
                        Log.e(TAG, "Error during sending of invite:", error);
                }
            }) {
                @Override
                protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
                    if (response.statusCode == HttpStatusCodes.STATUS_CODE_OK) {
                        return Response.success(new JSONObject(),
                                HttpHeaderParser.parseCacheHeaders(response));
                    } else {
                        Log.e(TAG, "Error during sending of invite, status code:" + response.statusCode);
                        return Response.error(new ServerError());
                    }
                }
            };
            mNetwork.getRequestQueue().add(createGroup);
        } catch (JSONException e) {
            Log.e(TAG, "Error during sending of invite:", e);
        }
    }

    public interface CreateGroupListener {
        void onGroupCreated();
        void onGroupCreationFailed();
    }

    public void createGroup(String groupName, final CreateGroupListener listener) {
        JSONObject group = new JSONObject();
        try {
            group.put(Constants.NAME, groupName);
            JsonObjectRequest createGroup = new JsonObjectRequest(Request.Method.PUT,
                    GROUP_ENDPOINT, group, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject group) {
                    try {
                        updateGroupInfoPreferences(group.getString(Constants.NAME),
                                group.getString(Constants.IDENTITY));
                        listener.onGroupCreated();
                    } catch (JSONException e) {
                        Log.e(TAG, "During new group sending:", e);
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e(TAG, "Error during \"get groups\" request", error);
                    listener.onGroupCreationFailed();
                }
            }) {
            };
            mNetwork.getRequestQueue().add(createGroup);
        } catch (JSONException e) {
            Log.e(TAG, "Error during JSONObject creation", e);
            //TODO: show popup about error during group creation
        }
    }

    public interface LeaveGroupListener {
        void onLeaveGroup();
        void onLeaveGroupFailed();
    }

    public void leaveGroup(final LeaveGroupListener listener) {
        String currentGroupIdentity = mContext.getSharedPreferences(GROUP_INFO_PREFERENCES, 0)
                .getString(GROUP_IDENTITY, "");
        StringRequest requestToLeave = new StringRequest(Request.Method.DELETE,
                GROUP_ENDPOINT + "/" + currentGroupIdentity + LEAVE_ACTION,
                new Response.Listener<String>() {
                @Override
                    public void onResponse(String response) {
                        updateGroupInfoPreferences("", "");
                        listener.onLeaveGroup();
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        listener.onLeaveGroupFailed();
                    }
                });
        mNetwork.addToRequestQueue(requestToLeave);
    }
}
