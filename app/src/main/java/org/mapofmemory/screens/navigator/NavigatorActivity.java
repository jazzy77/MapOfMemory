package org.mapofmemory.screens.navigator;

import android.Manifest;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.hannesdorfmann.mosby3.mvp.MvpActivity;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import org.mapofmemory.R;
import org.mapofmemory.entities.MonumentEntity;
import org.mapofmemory.screens.monument.MonumentView;
import org.osmdroid.bonuspack.routing.OSRMRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindDrawable;
import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class NavigatorActivity extends MvpActivity<NavigatorView, NavigatorPresenter> implements NavigatorView, LocationListener{
    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.map_view) MapView mapView;
    @BindDrawable(R.drawable.ic_blue_marker) Drawable blueMarker;
    @BindDrawable(R.drawable.ic_red_marker) Drawable redMarker;
    @BindView(R.id.distance) TextView distance;
    @BindView(R.id.block) RelativeLayout distanceBlock;
    private boolean isPermission = false;

    @NonNull
    @Override
    public NavigatorPresenter createPresenter() {
        return new NavigatorPresenter(this, getIntent().getStringExtra("monument_id"));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) onBackPressed();
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigator);
        ButterKnife.bind(this);
        getWindow().setStatusBarColor(getResources().getColor(R.color.colorPrimaryDark));
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getPresenter().loadMonument();
        getSupportActionBar().setTitle("Назад к описанию");
    }

    @Override
    public void onLoadMonument(MonumentEntity monumentEntity) {
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMaxZoomLevel(18);
        mapView.setMultiTouchControls(true);
        final GeoPoint startPoint = new GeoPoint(Float.parseFloat(monumentEntity.getLat()), Float.parseFloat(monumentEntity.getLng()));
        Marker startMarker = new Marker(mapView);
        startMarker.setPosition(startPoint);
        startMarker.setAnchor(Marker.ANCHOR_CENTER, 1.0f);
        startMarker.setInfoWindow(null);
        startMarker.setIcon(monumentEntity.getType().equals("1") ? redMarker : blueMarker);
        mapView.getOverlays().add(startMarker);
        mapView.getController().setZoom(18);
        mapView.getController().setCenter(startPoint);
        mapView.invalidate();
        mapView.setVisibility(View.VISIBLE);
        Dexter.withActivity(this)
                .withPermissions(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        if (report.areAllPermissionsGranted()){
                            isPermission = true;
                            LocationManager mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
                            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1 * 1000,
                                    0, NavigatorActivity.this);
                            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1 * 1000,
                                    0, NavigatorActivity.this);
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {

                    }
                })
                .check();
    }

    private void updateMap(double userLat, double userLng){
        final GeoPoint endPoint = new GeoPoint(Float.parseFloat(getPresenter().getMonument().getLat()), Float.parseFloat(getPresenter().getMonument().getLng()));
        Marker monumentMarker = new Marker(mapView);
        monumentMarker.setPosition(endPoint);
        monumentMarker.setAnchor(Marker.ANCHOR_CENTER, 1.0f);
        monumentMarker.setIcon(getPresenter().getMonument().getType().equals("1") ? redMarker : blueMarker);
        monumentMarker.setInfoWindow(null);

        Marker userMarker = new Marker(mapView);
        userMarker.setPosition(new GeoPoint(userLat, userLng));
        userMarker.setAnchor(Marker.ANCHOR_CENTER, 0);
        userMarker.setInfoWindow(null);
        userMarker.setIcon(getDrawable(R.drawable.user_marker));

        mapView.getOverlays().add(userMarker);
        mapView.getOverlays().add(monumentMarker);
        Location loc1 = new Location("");

        loc1.setLatitude(userLat);
        loc1.setLongitude(userLng);

        Location loc2 = new Location("");
        loc2.setLatitude(Double.parseDouble(getPresenter().getMonument().getLat()));
        loc2.setLongitude(Double.parseDouble(getPresenter().getMonument().getLng()));
        distance.setText((int)loc1.distanceTo(loc2) + " м");
        distanceBlock.setVisibility(View.VISIBLE);
        //Toast.makeText(this, loc1.distanceTo(loc2) + " meters", Toast.LENGTH_SHORT).show();
        //mapView.getController().setZoom(12);
        //mapView.getController().setCenter(new GeoPoint(userLat, userLng));
        Polyline line = new Polyline(getApplicationContext());
        line.setWidth(8f);
        line.setColor(Color.RED);
        List<GeoPoint> pts = new ArrayList<>();
        pts.add(new GeoPoint(userLat, userLng));
        pts.add(new GeoPoint(Double.parseDouble(getPresenter().getMonument().getLat()), Double.parseDouble(getPresenter().getMonument().getLng())));
        line.setPoints(pts);
        mapView.getOverlayManager().add(line);
        mapView.invalidate();
       /* Observable.just(1)
                .subscribeOn(Schedulers.io())
                .map(res -> {
                    RoadManager roadManager = new OSRMRoadManager(getApplicationContext());
                    ArrayList<GeoPoint> waypoints = new ArrayList<GeoPoint>();
                    waypoints.add(new GeoPoint(userLat, userLng));
                    waypoints.add(endPoint);
                    Road road = roadManager.getRoad(waypoints);
                    Polyline roadOverlay = RoadManager.buildRoadOverlay(road);
                    return roadOverlay;
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(roadOverlay -> {
                    mapView.getOverlays().add(roadOverlay);
                    mapView.invalidate();
                });*/
    }

    @Override
    public void onLocationChanged(Location location) {
        updateMap(location.getLatitude(), location.getLongitude());
        //updateMap(62.88785, 34.68005);
    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

}