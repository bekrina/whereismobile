package bekrina.whereismobile.listeners;


import com.android.volley.VolleyError;

public interface RestListener {
    void onCookieRequestSuccessful(String response);
    void onCookieRequestError(VolleyError error);

    void onLoginSuccessfull(String response);
    void onLoginError(VolleyError error);
}
