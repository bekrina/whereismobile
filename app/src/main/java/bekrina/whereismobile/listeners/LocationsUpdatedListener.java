package bekrina.whereismobile.listeners;


import org.json.JSONObject;

import java.util.List;

public interface LocationsUpdatedListener {
    void onMembersLocationsUpdate(List<JSONObject> locations);
}
