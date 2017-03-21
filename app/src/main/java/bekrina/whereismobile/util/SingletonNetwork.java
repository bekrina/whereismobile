package bekrina.whereismobile.util;


import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import java.net.CookieHandler;
import java.net.CookieManager;

public class SingletonNetwork {
    private static SingletonNetwork sInstance;
    private RequestQueue mRequestQueue;
    private Context mCtx;
    private CookieManager mCookieManager;

    private SingletonNetwork(Context context) {
        mCtx = context;
        mRequestQueue = getRequestQueue();
        mCookieManager = new CookieManager();
        CookieHandler.setDefault(mCookieManager);
    }

    public static synchronized SingletonNetwork getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new SingletonNetwork(context);
        }
        return sInstance;
    }

    public RequestQueue getRequestQueue() {
        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(mCtx.getApplicationContext());
        }
        return mRequestQueue;
    }

    public <T> void addToRequestQueue(Request<T> req) {
        getRequestQueue().add(req);
    }

}
