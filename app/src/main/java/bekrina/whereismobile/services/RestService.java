package bekrina.whereismobile.services;


import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;

import bekrina.whereismobile.listeners.RestListener;

public class RestService extends Service {
    RestListener mRestListener;

    RequestQueue mQueue = Volley.newRequestQueue(this);
    RestBinder mBinder = new RestBinder();

    public class RestBinder extends Binder {
        public RestService getService(RestListener listener) {
            mRestListener = listener;
            return RestService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
    }

    public void getCookie() {
        // Get unique cookie from server
        StringRequest requestCookie = new StringRequest(Request.Method.GET,
                "https://rocky-river-45878.herokuapp.com/api/config.json",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        mRestListener.onCookieRequestSuccessful(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        mRestListener.onCookieRequestError(error);
                    }
                }
        );
        mQueue.add(requestCookie);
    }

    public void login() {

    }
}
