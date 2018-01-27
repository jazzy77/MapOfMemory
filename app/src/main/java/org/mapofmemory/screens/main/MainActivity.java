package org.mapofmemory.screens.main;

import android.Manifest;
import android.content.Context;
import android.graphics.Point;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.hannesdorfmann.mosby3.mvp.MvpActivity;
import com.jaredrummler.materialspinner.MaterialSpinner;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.single.PermissionListener;

import org.mapofmemory.R;
import org.mapofmemory.entities.PlaceEntity;
import org.mapofmemory.screens.map.MapFragment;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.BoundingBoxE6;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;

public class MainActivity extends MvpActivity<MainView, MainPresenter>
        implements NavigationView.OnNavigationItemSelectedListener, MainView {
    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.nav_view) NavigationView navigationView;
    @BindView(R.id.drawer_layout) DrawerLayout drawer;
    private MaterialSpinner spinner;
    private TextView placeTitleView;

    @NonNull
    @Override
    public MainPresenter createPresenter() {
        return new MainPresenter(getApplicationContext(), getIntent().getIntExtra("place_id", -1));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().load(getApplicationContext(), PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        spinner = (MaterialSpinner) navigationView.getHeaderView(0).findViewById(R.id.spinner);
        placeTitleView = (TextView) navigationView.getHeaderView(0).findViewById(R.id.place_title);
        navigationView.setNavigationItemSelectedListener(this);
        spinner.setOnItemSelectedListener((MaterialSpinner view, int position, long id, Object item) -> {getPresenter().changePlace(position);});
        getPresenter().loadPlaces();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onPlacesLoad(List<PlaceEntity> places) {
        String[] titles = new String[places.size()];
        for (int i = 0; i <= places.size() - 1; i++){
            titles[i] = places.get(i).getTitle();
        }
        spinner.setPadding(0, 0, 0, 0);
        spinner.setItems(titles);
    }

    @Override
    public void onPlaceSelected(PlaceEntity place) {
        spinner.setText(place.getTitle());
        placeTitleView.setText(place.getTitle());
        navigationView.getMenu().findItem(R.id.nav_about).setTitle("О " + place.getTitle());
    }

    @Override
    public void onMapFragment(double lat, double lng) {
        getSupportFragmentManager().beginTransaction().replace(R.id.frame, MapFragment.newInstance(lat, lng)).commit();
    }
}