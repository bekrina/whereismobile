package bekrina.whereismobile.ui;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

import java.sql.Date;
import java.util.Objects;

import bekrina.whereismobile.model.Location;


public class EditableClusterItem implements ClusterItem {
    private final int userId;
    private String title;
    private LatLng latLng;
    private Date date;

    public EditableClusterItem(Location location) {
        this.userId = location.getUser().getId();
        this.title = location.getUser().getFirstName() + "\n" + location.getUser().getLastName();
        this.latLng = new LatLng(location.getLatitude(), location.getLongitude());
        this.date = new Date(System.currentTimeMillis());
    }

    @Override
    public LatLng getPosition() {
        return latLng;
    }

    public void updatePosition(LatLng position) {
        latLng = position;
        this.date = new Date(System.currentTimeMillis());
    }

    public String getTitle() {
        return title;
    }

    public Date getDate() {
        return date;
    }

    public int getUserId() {
        return userId;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof EditableClusterItem)) {
            return  false;
        }
        EditableClusterItem item = (EditableClusterItem) object;
        if (item.getTitle().equals(title) && item.getUserId() == userId) {
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, title);
    }

}

