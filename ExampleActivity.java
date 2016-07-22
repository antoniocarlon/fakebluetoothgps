package gps.fake;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import gps.fake.FakeBluetoothGPS;

public class ExampleActivity extends FragmentActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
    private Marker target;
    private FakeBluetoothGPS fakeBluetoothGPS = new FakeBluetoothGPS();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        fakeBluetoothGPS.start();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setAllGesturesEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.getUiSettings().setMapToolbarEnabled(false);
        mMap.setMyLocationEnabled(true);

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(final LatLng latLng) {
                setNewPosition(latLng);
            }
        });
    }

    private void setNewPosition(LatLng latLng) {
        if (target == null) {
            target = mMap.addMarker(new MarkerOptions().position(latLng).draggable(true));
        } else {
            target.setPosition(latLng);
        }

        fakeBluetoothGPS.move(latLng);
    }
}
