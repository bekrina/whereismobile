package bekrina.whereismobile.ui;

import android.content.Context;

import com.google.android.gms.maps.GoogleMap;
import com.google.maps.android.MarkerManager;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.clustering.ClusterManager;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class EditableClusterManager<T extends ClusterItem> extends ClusterManager<T> {
    private MarkerManager mMarkerManager;

    private Set<ClusterItem> mClusterItems;

    public EditableClusterManager(Context context, GoogleMap map, MarkerManager markerManager) {
        super(context, map, markerManager);
        mMarkerManager = markerManager;
        mClusterItems = new HashSet<>();
    }

    @Override
    public void addItem(T item) {
        if (mClusterItems.add(item)) {
            super.addItem(item);
        }
    }

    @Override
    public void addItems(Collection<T> items) {
        if (mClusterItems.addAll(items)) {
            super.addItems(items);
        }
    }

    @Override
    public void removeItem (T item) {
        mClusterItems.remove(item);
        super.removeItem(item);
        super.cluster();
    }

    public void replaceWithNewPosition(T item) {
        if (mClusterItems.contains(item)) {
            mClusterItems.remove(item);
            mClusterItems.add(item);
        }
    }
}
