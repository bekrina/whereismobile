package bekrina.whereismobile.ui;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

import java.sql.Date;
import java.util.Objects;


public class EditableClusterItem implements ClusterItem {
    private final int userId;
    private final String title;
    private final LatLng latLng;
    private final Date date;

    public EditableClusterItem(int userId, String title, LatLng latLng, Date date) {
        this.userId = userId;
        this.title = title;
        this.latLng = latLng;
        this.date = date;
    }

    @Override
    public LatLng getPosition() {
        return latLng;
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

