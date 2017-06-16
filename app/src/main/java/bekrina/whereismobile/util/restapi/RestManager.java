package bekrina.whereismobile.util.restapi;

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
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.google.api.client.http.HttpStatusCodes;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import bekrina.whereismobile.listeners.CreateGroupListener;
import bekrina.whereismobile.listeners.GroupStatusListener;
import bekrina.whereismobile.listeners.InviteStatusListener;
import bekrina.whereismobile.listeners.JoinedToGroupListener;
import bekrina.whereismobile.listeners.LeaveGroupListener;
import bekrina.whereismobile.model.Group;
import bekrina.whereismobile.model.Invite;
import bekrina.whereismobile.util.Constants;
import bekrina.whereismobile.util.SingletonNetwork;
import retrofit2.Call;
import retrofit2.Callback;

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

        RestApiFactory.getApi(mContext).getGroup().enqueue(new Callback<Group>() {
            @Override
            public void onResponse(Call<Group> call, retrofit2.Response<Group> response) {
                if (response.body() == null) {
                    listener.onUserWithoutGroup();
                } else {
                    Group group = response.body();
                    updateGroupInfoPreferences(new Gson().toJson(response.body()));
                    listener.onUserHasGroup(group);
                }
            }
            @Override
            public void onFailure(Call<Group> call, Throwable t) {
                Log.e(TAG, "tokenRequest.onErrorResponse:", t);
                listener.onUserWithoutGroup();
            }
        });
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
