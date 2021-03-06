package bekrina.whereismobile.services;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import bekrina.whereismobile.listeners.CreateGroupListener;
import bekrina.whereismobile.listeners.GroupStatusListener;
import bekrina.whereismobile.listeners.InviteStatusListener;
import bekrina.whereismobile.listeners.JoinedToGroupListener;
import bekrina.whereismobile.listeners.LeaveGroupListener;
import bekrina.whereismobile.model.Group;
import bekrina.whereismobile.model.Invite;
import bekrina.whereismobile.ui.LoginActivity;
import bekrina.whereismobile.util.Constants;
import bekrina.whereismobile.util.SingletonNetwork;

import static bekrina.whereismobile.util.Constants.GET_GROUPS_ACTION;
import static bekrina.whereismobile.util.Constants.GROUP;
import static bekrina.whereismobile.util.Constants.GROUP_ENDPOINT;
import static bekrina.whereismobile.util.Constants.GROUP_INFO_PREFERENCES;
import static bekrina.whereismobile.util.Constants.INVITE_ACTION;
import static bekrina.whereismobile.util.Constants.JOIN_ACTION;
import static bekrina.whereismobile.util.Constants.LEAVE_ACTION;

public class RestManager {
    private static RestManager mInstance;
    private SingletonNetwork mNetwork;
    private Context mContext;

    private static final String TAG = RestManager.class.getName();

    private RestManager(Context context) {
        mNetwork = SingletonNetwork.getInstance(context);
        mContext = context;
    }

    public static synchronized RestManager getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new RestManager(context);
            return mInstance;
        }
        return mInstance;
    }

    public void joinGroup(String groupIdentity,
                          final JoinedToGroupListener listener) {
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

    public void processGroupStatus(final GroupStatusListener listener) {
        JsonArrayRequest groupRequest = new JsonArrayRequest(Request.Method.GET,
                GROUP_ENDPOINT + GET_GROUPS_ACTION, null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        if (response.optJSONObject(0) == null) {
                            listener.onUserWithoutGroup();
                        } else {
                            Gson gson = new Gson();
                            Group group = gson.fromJson(response.optJSONObject(0).toString(), Group.class);
                            updateGroupInfoPreferences(response.optJSONObject(0).toString());
                            listener.onUserHasGroup(group);
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "tokenRequest.onErrorResponse:", error);
                        listener.onUserWithoutGroup();
                }
        }) {
        };
        mNetwork.getRequestQueue().add(groupRequest);
    }

    private void updateGroupInfoPreferences(String jsonGroup) {
        SharedPreferences preferences = mContext.getSharedPreferences(Constants.GROUP_INFO_PREFERENCES, 0);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(GROUP, jsonGroup);
        editor.apply();
    }

    public void inviteToGroup(Activity activity, String emailToInvite,
                              final InviteStatusListener listener) {
        Invite invite = new Invite();
        try {
            invite.setEmail(emailToInvite);
            SharedPreferences preferences = activity.getSharedPreferences(GROUP_INFO_PREFERENCES, 0);
            Gson gson = new Gson();
            //TODO: http://stackoverflow.com/questions/9593409/how-to-convert-pojo-to-json-and-vice-versa
            String groupIdentity = gson.fromJson(preferences.getString(GROUP, ""), Group.class).getIdentity();
            JsonObjectRequest inviteToGroup = new JsonObjectRequest(Request.Method.POST,
                    GROUP_ENDPOINT + "/" + groupIdentity
                            + INVITE_ACTION,
                    new JSONObject(gson.toJson(invite, Invite.class)), new Response.Listener<JSONObject>() {
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
            mNetwork.getRequestQueue().add(inviteToGroup);
        } catch (JSONException e) {
            Log.e(TAG, "Error during sending of invite:", e);
        }
    }

    public void createGroup(String groupName, final CreateGroupListener listener) {
        try {
            final Gson gson = new Gson();
            Group newGroup = new Group(groupName);
            JsonObjectRequest createGroup = new JsonObjectRequest(Request.Method.PUT,
                    GROUP_ENDPOINT, new JSONObject(gson.toJson(newGroup)),
                    new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject group) {
                        updateGroupInfoPreferences(group.toString());
                        listener.onGroupCreated();
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
            listener.onGroupCreationFailed();
        }
    }

    public void leaveGroup(final LeaveGroupListener listener) {
        Gson gson = new Gson();
        Group group = gson.fromJson(mContext.getSharedPreferences(GROUP_INFO_PREFERENCES, 0).getString(GROUP, ""), Group.class);
        if (group != null) {
            StringRequest requestToLeave = new StringRequest(Request.Method.DELETE,
                    GROUP_ENDPOINT + "/" + group.getIdentity() + LEAVE_ACTION,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            updateGroupInfoPreferences("");
                            listener.onLeaveGroup();
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    listener.onLeaveGroupFailed();
                }
            });
            mNetwork.addToRequestQueue(requestToLeave);
        } else {
            listener.onLeaveGroupFailed();
        }
    }
}
