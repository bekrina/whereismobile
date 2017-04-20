package bekrina.whereismobile.ui;

import android.content.Context;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.maps.android.MarkerManager;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.clustering.ClusterManager;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static bekrina.whereismobile.util.Constants.OFFSET;

public class EditableClusterManager<T extends EditableClusterItem> extends ClusterManager<T> {
    private GoogleMap mGoogleMap;

    private Map<Integer, T> mClusterItems;

    public EditableClusterManager(Context context, GoogleMap map) {
        super(context, map);
        mClusterItems = new HashMap<>();
        mGoogleMap = map;
    }

    @Override
    public void addItem(T item) {
        if (!mClusterItems.containsKey(item.getUserId())) {
            mClusterItems.put(item.getUserId(), item);
            super.addItem(item);
        }
    }

    @Override
    public void addItems(Collection<T> items) {
        for (T item : items) {
            if (!mClusterItems.containsKey(item.getUserId())) {
                mClusterItems.put(item.getUserId(), item);
                super.addItem(item);
            }
        }
    }

    @Override
    public void removeItem (T item) {
        mClusterItems.remove(item.getUserId());
        super.removeItem(item);
    }

    public void updatePosition(T item) {
        /*for (Marker marker : getMarkerCollection().getMarkers()) {
            if (Math.abs(item.getPosition().latitude - marker.getPosition().latitude) <= OFFSET ||
                    Math.abs(item.getPosition().longitude - marker.getPosition().longitude) <= OFFSET ) {
                item.setPosition(new LatLng(item.getPosition().latitude + OFFSET,
                        item.getPosition().longitude + OFFSET));
            }
        }*/

        mClusterItems.put(item.getUserId(), item);


        super.clearItems();
        super.cluster();
        super.addItems(mClusterItems.values());

        //super.getAlgorithm().addItems(mClusterItems.values());
    }

    public boolean hasItemOfUser(int userId) {
        return mClusterItems.containsKey(userId);
    }
}
