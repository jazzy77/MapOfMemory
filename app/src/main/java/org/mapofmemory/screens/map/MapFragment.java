package org.mapofmemory.screens.map;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.lzyzsd.circleprogress.CircleProgress;
import com.github.lzyzsd.circleprogress.DonutProgress;
import com.hannesdorfmann.mosby3.mvp.MvpFragment;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;
import com.mancj.materialsearchbar.MaterialSearchBar;
import com.mancj.materialsearchbar.adapter.SuggestionsAdapter;
import com.miguelcatalan.materialsearchview.MaterialSearchView;
import com.ogaclejapan.smarttablayout.SmartTabLayout;
import com.ogaclejapan.smarttablayout.utils.v4.FragmentPagerItemAdapter;
import com.ogaclejapan.smarttablayout.utils.v4.FragmentPagerItems;
import com.orhanobut.dialogplus.DialogPlus;
import com.orhanobut.dialogplus.ViewHolder;

import net.gotev.speech.GoogleVoiceTypingDisabledException;
import net.gotev.speech.Speech;
import net.gotev.speech.SpeechDelegate;
import net.gotev.speech.SpeechRecognitionNotAvailable;

import org.mapofmemory.AppConfig;
import org.mapofmemory.BaseApplication;
import org.mapofmemory.MonumentInfoWindow;
import org.mapofmemory.R;
import org.mapofmemory.adapters.CustomSuggestionsAdapter;
import org.mapofmemory.adapters.SearchAdapter;
import org.mapofmemory.entities.MonumentEntity;
import org.mapofmemory.screens.main.MainActivity;
import org.mapofmemory.screens.monument.MonumentActivity;
import org.mapofmemory.screens.navigator.NavigatorActivity;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Marker;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.BindDrawable;
import butterknife.BindView;
import butterknife.ButterKnife;
import io.nlopez.smartlocation.OnLocationUpdatedListener;
import io.nlopez.smartlocation.SmartLocation;
import io.nlopez.smartlocation.location.config.LocationAccuracy;
import io.nlopez.smartlocation.location.config.LocationParams;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by The Tronuo on 27.01.2018.
 */

public class MapFragment extends MvpFragment<MapView, MapPresenter> implements MapView, Marker.OnMarkerClickListener, SmartTabLayout.OnTabClickListener, OnLocationUpdatedListener, SensorEventListener {
    @BindView(R.id.map)
    org.osmdroid.views.MapView map;
    @BindView(R.id.progress)
    ProgressBar progressBar;
    @BindDrawable(R.drawable.ic_blue_marker)
    Drawable blueMarker;
    @BindDrawable(R.drawable.ic_red_marker)
    Drawable redMarker;
    @BindDrawable(R.drawable.point)
    Drawable point;
    @BindView(R.id.viewpagertab)
    SmartTabLayout smartTabLayout;
    @BindView(R.id.viewpager)
    ViewPager viewPager;
    @BindView(R.id.searchBar)
    MaterialSearchBar searchBar;
    @BindView(R.id.donut_progress1)
    DonutProgress circleProgress;
    private DialogPlus dialogPlus;
    private Activity activity;
    private List<MonumentEntity> monuments;
    private MaterialSearchView searchView;
    private Marker userMarker;

    @Override
    public void onProgress(int progress, int total) {
        if (circleProgress.getVisibility() == View.GONE) {
            circleProgress.setVisibility(View.VISIBLE);
        }
        searchBar.setFocusable(false);
        searchBar.setClickable(false);
        int percentage = Math.round((progress * 100.0f) / (total * 1.0f));
        circleProgress.setProgress(percentage);
    }

    @Override
    public MapPresenter createPresenter() {
        return new MapPresenter(this, getContext(), getArguments().getInt("place_id"));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    public void search() {
        if (dialogPlus.isShowing()) {
            dialogPlus.dismiss();
        } else {
            dialogPlus.show();
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);
        ((ImageView) searchBar.findViewById(R.id.mt_nav)).setImageResource(R.drawable.ic_search);
        this.activity = getActivity();
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.getController().setZoom(18);
        map.setMaxZoomLevel(19);
        map.setMultiTouchControls(true);
        GeoPoint startPoint = new GeoPoint(getArguments().getDouble("lat"), getArguments().getDouble("lng"));
        map.getController().setCenter(startPoint);
        int statusBarHeight = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            statusBarHeight = getResources().getDimensionPixelSize(resourceId);
        }
        dialogPlus = DialogPlus.newDialog(getActivity())
                .setContentHolder(new ViewHolder(R.layout.view_search))
                .setExpanded(true, (int) (1.0f * getActivity().getWindowManager().getDefaultDisplay().getHeight()) - statusBarHeight)
                .create();
        getPresenter().loadMonuments();

        FragmentPagerItemAdapter adapter = new FragmentPagerItemAdapter(
                getActivity().getSupportFragmentManager(), FragmentPagerItems.with(getActivity())
                .add("Все", Fragment.class)
                .add("Личные", Fragment.class)
                .add("Коллективные", Fragment.class)
                .create());
        viewPager.setAdapter(adapter);
        smartTabLayout.setViewPager(viewPager);
        smartTabLayout.setOnTabClickListener(this);

        map.setOnTouchListener((v, event) -> {
            for (Marker marker1 : markers) {
                if (marker1.isInfoWindowShown()) marker1.closeInfoWindow();
            }
            return false;
        });


        SensorManager mSensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        Sensor accSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        mSensorManager.registerListener(this, accSensor, SensorManager.SENSOR_DELAY_NORMAL);

    }

    @Override
    public void onResume() {
        super.onResume();
        SmartLocation.with(BaseApplication.appContext).location().config((new LocationParams.Builder()).setAccuracy(LocationAccuracy.HIGH).setDistance(0.0F).setInterval(1000L).build())
                .start(MapFragment.this);
    }

    List<Marker> markers = new ArrayList<>();
    private int pos = 0;

    @Override
    public void onTabClicked(int position) {
        pos = position;
        if (searchBar.isSearchEnabled()) {
            searchBar.disableSearch();
        }
        if (position == 0) {
            showMonuments(getPresenter().getMonuments(), getPresenter().place.getImgRoot());
        } else if (position == 1) {
            showMonuments(Observable.fromIterable(getPresenter().getMonuments())
                    .filter(monumentEntity -> monumentEntity.getType().equals("1"))
                    .toList()
                    .blockingGet(), getPresenter().place.getImgRoot());
        } else if (position == 2) {
            showMonuments(Observable.fromIterable(getPresenter().getMonuments())
                    .filter(monumentEntity -> monumentEntity.getType().equals("2"))
                    .toList()
                    .blockingGet(), getPresenter().place.getImgRoot());
        }
    }

    private Disposable disp = null;
    private int z = 0;

    @Override
    public void showMonuments(List<MonumentEntity> monuments, String imgRoot) {
        //smartTabLayout.setVisibility(View.GONE);
        z++;
        for (Marker marker1 : markers) {
            if (marker1.isInfoWindowShown()) marker1.closeInfoWindow();
        }
        markers.clear();
        map.getOverlays().clear();
        map.getOverlayManager().clear();
        this.monuments = monuments;
        initSearchView();
        map.setVisibility(View.GONE);
        circleProgress.setVisibility(View.VISIBLE);


        disp = Flowable.fromIterable(monuments)
                .subscribeOn(Schedulers.io())
                .filter(monumentEntity -> {
                    GeoPoint startPoint = new GeoPoint(Double.parseDouble(monumentEntity.getLat()), Double.parseDouble(monumentEntity.getLng()));
                    Marker startMarker = new Marker(map);
                    startMarker.setOnMarkerClickListener(this);
                    startMarker.setPosition(startPoint);
                    startMarker.setTitle("Marker" + monumentEntity.getId());
                    MonumentInfoWindow monumentInfoWindow = new MonumentInfoWindow(map, monumentEntity.getImgs().size() != 0 ? imgRoot + monumentEntity.getImgs().get(0).getImg() : "", monumentEntity);
                    monumentInfoWindow.setOnWindowClickListener(new MonumentInfoWindow.OnWindowClickListener() {
                        @Override
                        public void onWindowClick(MonumentInfoWindow window) {
                            ImageView image = window.getImage();
                            ActivityOptionsCompat options =
                                    ActivityOptionsCompat.makeClipRevealAnimation(image, (int) image.getX(), (int) image.getY(), image.getWidth(), image.getHeight());
                            Intent newInt = new Intent(getContext(), MonumentActivity.class);
                            newInt.putExtra("monument_id", monumentEntity.getNum() + "");
                            newInt.putExtra("image_url", window.getImageUrl());
                            newInt.putExtra("name", monumentEntity.getName());
                            newInt.putExtra("type2", monumentEntity.getType2());
                            newInt.putExtra("descr", monumentEntity.getDesc());
                            startActivity(newInt, options.toBundle());
                        }

                        @Override
                        public void onButtonClick(MonumentInfoWindow window) {
                            Intent newInt = new Intent(getContext(), NavigatorActivity.class);
                            newInt.putExtra("monument_id", monumentEntity.getNum() + "");
                            newInt.putExtra("from_map", "");
                            startActivity(newInt);
                        }

                        @Override
                        public void onCloseClick() {
                            monumentInfoWindow.close();
                        }
                    });


                    startMarker.setInfoWindow(monumentInfoWindow);
                    startMarker.setAnchor(Marker.ANCHOR_BOTTOM, 1.0f);
                    startMarker.setIcon(monumentEntity.getType().equals("1") ? redMarker : blueMarker);
                    markers.add(startMarker);
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            onProgress(monuments.size() + markers.size(), z == 1 ? monuments.size() * 2 : monuments.size());
                        }
                    });
                    return true;
                })
                .toList()
                .filter(v -> {
                    map.getOverlays().addAll(markers);
                    return true;
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(monumentEntities -> {
                    map.invalidate();
                    map.setVisibility(View.VISIBLE);
                    searchBar.setVisibility(View.VISIBLE);
                    searchBar.setFocusable(true);
                    searchBar.setClickable(true);
                    smartTabLayout.setVisibility(View.VISIBLE);
                    circleProgress.setProgress(100);
                    circleProgress.setVisibility(View.GONE);
                });
    }

    @Override
    public boolean onMarkerClick(Marker marker, org.osmdroid.views.MapView mapView) {
        for (Marker marker1 : markers) {
            if (marker1.isInfoWindowShown()) marker1.closeInfoWindow();
        }
        marker.showInfoWindow();
        map.getController().setZoom(18);
        map.getController().setCenter(new GeoPoint(marker.getPosition().getLatitude(), marker.getPosition().getLongitude()));
        map.invalidate();
        return true;
    }

    public void initSearchView() {
        searchBar.findViewById(R.id.mt_search).setVisibility(View.GONE);
        List<String> suggestions = Observable.fromIterable(monuments)
                .filter(monumentEntity -> !monumentEntity.getKeywords().isEmpty() || !monumentEntity.getName().isEmpty())
                .map(monumentEntity -> monumentEntity.getType().equals("1") ? monumentEntity.getKeywords() : monumentEntity.getName())
                .toList()
                .blockingGet();
        suggestions = AppConfig.removeTheDuplicates(suggestions);
        final List<String> suggs = suggestions;
        CustomSuggestionsAdapter customSuggestionsAdapter = new CustomSuggestionsAdapter(getLayoutInflater());
        customSuggestionsAdapter.setOnSuggestionClickListener(new CustomSuggestionsAdapter.OnSuggestionClickListener() {
            @Override
            public void onClick(String text) {
                List<MonumentEntity> m = Observable.fromIterable(getPresenter().getMonuments())
                        .filter(monumentEntity -> {
                            return monumentEntity.getType().equals("1") ? monumentEntity.getKeywords().toLowerCase().contains(text.toString().toLowerCase()) : monumentEntity.getName().toLowerCase().contains(text.toString().toLowerCase());
                        })
                        .toList()
                        .blockingGet();
                searchBar.hideSuggestionsList();
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(searchBar.getWindowToken(), 0);
                //searchBar.
                Disposable ob = Observable.just(1)
                        .delay(300, TimeUnit.MILLISECONDS)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(res -> {
                            showMonuments(m, getPresenter().place.getImgRoot());
                            if (m.size() == 1) {
                                map.getController().setCenter(new GeoPoint(Float.parseFloat(m.get(0).getLat()), Float.parseFloat(m.get(0).getLng())));
                                map.invalidate();
                            }
                            searchBar.setText(text);
                            //searchBar.disableSearch();
                            /*onMarkerClick(Observable.fromIterable(markers)
                                    .filter(marker -> marker.getTitle().equals("Marker" + m.get(0).getId()))
                                    .blockingFirst(), map);*/
                        });
            }
        });

        customSuggestionsAdapter.setSuggestions(suggestions);
        searchBar.setCustomSuggestionAdapter(customSuggestionsAdapter);
        searchBar.addTextChangeListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                customSuggestionsAdapter.getFilter().filter(s);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        searchBar.findViewById(R.id.mt_clear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onTabClicked(0);
                viewPager.setCurrentItem(0);

            }
        });
        searchBar.setOnSearchActionListener(new MaterialSearchBar.OnSearchActionListener() {
            @Override
            public void onSearchStateChanged(boolean enabled) {
                if (!enabled) {
                    ((ImageView) searchBar.findViewById(R.id.mt_nav)).setImageResource(R.drawable.ic_search);
                } else {
                }
            }

            @Override
            public void onSearchConfirmed(CharSequence text) {
                List<MonumentEntity> m = Observable.fromIterable(getPresenter().getMonuments())
                        .filter(monumentEntity -> {
                            return monumentEntity.getType().equals("1") ? monumentEntity.getKeywords().toLowerCase().contains(text.toString().toLowerCase()) : monumentEntity.getName().toLowerCase().contains(text.toString().toLowerCase());
                        })
                        .toList()
                        .blockingGet();
                searchBar.hideSuggestionsList();
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(searchBar.getWindowToken(), 0);
                //searchBar.
                Disposable ob = Observable.just(1)
                        .delay(100, TimeUnit.MILLISECONDS)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(res -> {
                            showMonuments(m, getPresenter().place.getImgRoot());
                            if (m.size() == 1) {
                                map.getController().setCenter(new GeoPoint(Float.parseFloat(m.get(0).getLat()), Float.parseFloat(m.get(0).getLng())));
                                map.invalidate();
                            }
                            /*onMarkerClick(Observable.fromIterable(markers)
                                    .filter(marker -> marker.getTitle().equals("Marker" + m.get(0).getId()))
                                    .blockingFirst(), map);*/
                        });
            }

            @Override
            public void onButtonClicked(int buttonCode) {
                if (buttonCode == MaterialSearchBar.BUTTON_BACK) {
                    onTabClicked(pos);
                } else if (buttonCode == MaterialSearchBar.BUTTON_SPEECH) {
                    try {
                        // you must have android.permission.RECORD_AUDIO granted at this point
                        Speech.getInstance().startListening(new SpeechDelegate() {
                            @Override
                            public void onStartOfSpeech() {
                                Toast.makeText(getActivity(), "Говорите...", Toast.LENGTH_LONG).show();
                                Log.i("speech", "speech recognition is now active");
                            }

                            @Override
                            public void onSpeechRmsChanged(float value) {
                                Log.d("speech", "rms is now: " + value);
                            }

                            @Override
                            public void onSpeechPartialResults(List<String> results) {
                                StringBuilder str = new StringBuilder();
                                for (String res : results) {
                                    str.append(res).append(" ");
                                }

                                Log.i("speech", "partial result: " + str.toString().trim());
                            }

                            @Override
                            public void onSpeechResult(String result) {
                                searchBar.setText(result);
                                searchBar.performClick();
                                Log.i("speech", "result: " + result);
                            }
                        });
                    } catch (SpeechRecognitionNotAvailable exc) {
                        Log.e("speech", "Speech recognition is not available on this device!");
                        // You can prompt the user if he wants to install Google App to have
                        // speech recognition, and then you can simply call:
                        //
                        // SpeechUtil.redirectUserToGoogleAppOnPlayStore(this);
                        //
                        // to redirect the user to the Google App page on Play Store
                    } catch (GoogleVoiceTypingDisabledException exc) {
                        Log.e("speech", "Google voice typing must be enabled!");
                    }
                }
                //Toast.makeText(getActivity(), buttonCode + "!", Toast.LENGTH_LONG).show();
            }
        });
        //searchBar.setLastSuggestions(suggestions);
    }

    public static MapFragment newInstance(int placeId, double lat, double lng) {
        Bundle bundle = new Bundle();
        bundle.putInt("place_id", placeId);
        bundle.putDouble("lat", lat);
        bundle.putDouble("lng", lng);
        MapFragment myFragment = new MapFragment();
        myFragment.setArguments(bundle);
        return myFragment;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getPresenter().dispose();
        if (disp != null) disp.dispose();
    }

    @Override
    public void onLocationUpdated(Location location) {
        Log.w("log", "onLocationUpdatedLAN: " + location.getLatitude() + " " + location.getLongitude());

        if (userMarker != null) {
            map.getOverlays().remove(userMarker);
        }
        userMarker = new Marker(map);
        userMarker.setIcon(point);
        userMarker.setOnMarkerClickListener(new Marker.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker, org.osmdroid.views.MapView mapView) {
                return false;
            }
        });
        userMarker.setRotation(360);
        userMarker.setPosition(new GeoPoint(location.getLatitude(), location.getLongitude()));
        map.getOverlays().add(userMarker);
        map.invalidate();
    }

    private long millis = 0;
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ORIENTATION) {
            float[] gravity = event.values.clone();
            if (System.currentTimeMillis() - millis >= 50){
                millis = System.currentTimeMillis();
                if (userMarker != null){
                    userMarker.setRotation(gravity[0]);
                }
                map.invalidate();
                Log.d("Heading", gravity[0] + "");
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        SensorManager mSensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        mSensorManager.unregisterListener(this);
        SmartLocation.with(BaseApplication.appContext).location().stop();
    }

}
