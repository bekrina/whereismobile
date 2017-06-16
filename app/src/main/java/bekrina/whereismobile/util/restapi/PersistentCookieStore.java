package bekrina.whereismobile.util.restapi;

import android.content.Context;
import android.content.SharedPreferences;

import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PersistentCookieStore implements CookieStore {
    private List <HttpCookie> inMemoryCookies = new ArrayList<>();
    private SharedPreferences sharedPreferencesCookies;

    public PersistentCookieStore(Context context) {
        sharedPreferencesCookies = context.getSharedPreferences("Cookies", 0);
        for (Map.Entry<String, ?> cookie : sharedPreferencesCookies.getAll().entrySet()) {
            inMemoryCookies.add(new HttpCookie(cookie.getKey(), (String) cookie.getValue()));
        }
    }

    @Override
    public void add(URI uri, HttpCookie cookie) {
        if (!inMemoryCookies.contains(cookie)) {
            inMemoryCookies.add(cookie);
        }
        sharedPreferencesCookies.edit().putString(cookie.getName(), cookie.getValue()).apply();
    }

    @Override
    public List<HttpCookie> get(URI uri) {
        return null;
    }

    @Override
    public List<HttpCookie> getCookies() {
        return null;
    }

    @Override
    public List<URI> getURIs() {
        return null;
    }

    @Override
    public boolean remove(URI uri, HttpCookie cookie) {
        return false;
    }

    @Override
    public boolean removeAll() {
        return false;
    }
}

