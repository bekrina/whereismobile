package bekrina.whereismobile.ui;

import android.content.Context;

import com.google.android.gms.maps.GoogleMap;
import com.google.maps.android.MarkerManager;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.clustering.ClusterManager;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class EditableClusterManager<T extends EditableClusterItem> extends ClusterManager<T> {
    private MarkerManager mMarkerManager;

    private Map<Integer, EditableClusterItem> mClusterItems;

    public EditableClusterManager(Context context, GoogleMap map) {
        super(context, map);
        mClusterItems = new HashMap<>();
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
        if (mClusterItems.containsKey(item.getUserId())) {
            mClusterItems.get(item.getUserId()).updatePosition(item.getPosition());
        }
    }

    public boolean hasItemOfUser(int userId) {
        return mClusterItems.containsKey(userId);
    }
}
