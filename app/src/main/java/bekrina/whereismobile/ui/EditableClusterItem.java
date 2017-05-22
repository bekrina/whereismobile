package bekrina.whereismobile.ui;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

import java.sql.Date;
import java.util.Objects;

import bekrina.whereismobile.model.Location;


public class EditableClusterItem implements ClusterItem {
    private final int userId;
    private final String title;
    private LatLng latLng;
    private final Date date;
    private final boolean isCurrentUser;

    public EditableClusterItem(Location location, boolean isCurrentUser) {
        this.isCurrentUser = isCurrentUser;
        this.userId = location.getUser().getId();
        this.title = location.getUser().getFirstName();
        this.latLng = new LatLng(location.getLatitude(), location.getLongitude());
        if (location.getTimestamp() != null) {
            this.date = new Date(location.getTimestamp().getTime());
        } else {
            this.date = new Date(System.currentTimeMillis());
        }
    }

    @Override
    public LatLng getPosition() {
        return latLng;
    }

    public void setPosition(LatLng position) {
        latLng = position;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public String getSnippet() {
        return null;
    }

    public Date getDate() {
        return date;
    }

    public int getUserId() {
        return userId;
    }

    public boolean isCurrentUser() {
        return isCurrentUser;
    }
}

