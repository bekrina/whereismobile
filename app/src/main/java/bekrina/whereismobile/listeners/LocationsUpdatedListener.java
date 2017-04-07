package bekrina.whereismobile.listeners;


import org.json.JSONObject;

import java.util.List;

import bekrina.whereismobile.model.Location;

public interface LocationsUpdatedListener {
    void onMembersLocationsUpdate(List<Location> locations);
}
