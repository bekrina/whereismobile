package bekrina.whereismobile.ui;

import android.content.Context;
import android.text.Editable;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;

import java.util.ArrayList;
import java.util.List;

public class EditableClusterRenderer extends DefaultClusterRenderer<EditableClusterItem> {
    private List<EditableClusterItem> clusterItems;
    private final Context mContext;
    public EditableClusterRenderer(Context context, GoogleMap map,
                                    ClusterManager<EditableClusterItem> clusterManager) {
        super(context, map, clusterManager);
        mContext = context;
        clusterItems = new ArrayList<>();
    }

    @Override
    protected void onBeforeClusterItemRendered(EditableClusterItem item,
                                               MarkerOptions markerOptions) {
        if (clusterItems.contains(item)) {
            clusterItems.add(item);
        } else {
            clusterItems.remove(item);
            clusterItems.add(item);
        }
        final BitmapDescriptor markerDescriptor = BitmapDescriptorFactory.defaultMarker(
                BitmapDescriptorFactory.HUE_ORANGE);
        markerOptions.icon(markerDescriptor).snippet(item.getTitle());
    }

/*    @Override
     protected void onBeforeClusterRendered(Cluster<MyClusterItem> cluster,
                                            MarkerOptions markerOptions) {
        final BitmapDescriptor markerDescriptor = BitmapDescriptorFactory.defaultMarker(
                BitmapDescriptorFactory.HUE_AZURE);
         markerOptions.icon(markerDescriptor).snippet("test");
     }*/
}
