package bekrina.whereismobile.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.support.v4.content.ContextCompat;
import android.text.Layout;
import android.view.Gravity;
import android.view.View;
import android.widget.GridLayout;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.google.maps.android.ui.IconGenerator;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import bekrina.whereismobile.R;

public class EditableClusterRenderer extends DefaultClusterRenderer<EditableClusterItem> {
    private final Context mContext;
    public EditableClusterRenderer(Context context, GoogleMap map,
                                    ClusterManager<EditableClusterItem> clusterManager) {
        super(context, map, clusterManager);
        mContext = context;
    }

    @Override
    protected void onBeforeClusterItemRendered(EditableClusterItem item,
                                               MarkerOptions markerOptions) {
        IconGenerator generator = new IconGenerator(mContext);
        if(item.isCurrentUser()) {
        generator.setStyle(IconGenerator.STYLE_ORANGE);
        } else {
            generator.setStyle(IconGenerator.STYLE_BLUE);
        }

        Bitmap icon = generator.makeIcon(item.getTitle());
        BitmapDescriptor markerDescriptor = BitmapDescriptorFactory.fromBitmap(icon);

        DateFormat simpleDateFormat = SimpleDateFormat.getDateTimeInstance();
        String formattedTime = simpleDateFormat.format(item.getDate());
        markerOptions.title(formattedTime);
        markerOptions.icon(markerDescriptor);
    }

    @Override
     protected void onBeforeClusterRendered(Cluster<EditableClusterItem> cluster,
                                            MarkerOptions markerOptions) {
        IconGenerator generator = new IconGenerator(mContext);
        generator.setBackground(ContextCompat.getDrawable(mContext, R.drawable.blue_circle));

        TextView title = new TextView(mContext);
        title.setText(String.valueOf(cluster.getSize()));
        title.setTextColor(ContextCompat.getColor(mContext, R.color.white));
        title.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
        title.setWidth(ContextCompat.getDrawable(mContext, R.drawable.orange_circle).getIntrinsicWidth());
        title.setHeight(ContextCompat.getDrawable(mContext, R.drawable.orange_circle).getIntrinsicHeight());

        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);

        generator.setTextAppearance(mContext, R.color.white);
        generator.setContentView(title);

        for (EditableClusterItem item : cluster.getItems()) {
            if (item.isCurrentUser()) {
                generator.setBackground(ContextCompat.getDrawable(mContext, R.drawable.orange_circle));
            }
        }
        Bitmap icon = generator.makeIcon(String.valueOf(cluster.getSize()));
        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(icon));
     }
}
