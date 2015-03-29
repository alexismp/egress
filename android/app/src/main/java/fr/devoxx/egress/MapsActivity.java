package fr.devoxx.egress;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Property;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.client.AuthData;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.MutableData;
import com.firebase.client.Transaction;
import com.firebase.client.ValueEventListener;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.melnykov.fab.FloatingActionButton;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import fr.devoxx.egress.internal.LatLngInterpolator;
import fr.devoxx.egress.internal.Preferences;
import fr.devoxx.egress.model.Player;
import fr.devoxx.egress.model.Station;
import icepick.Icepick;
import icepick.Icicle;
import timber.log.Timber;

import static android.widget.Toast.LENGTH_LONG;

public class MapsActivity extends FragmentActivity {

    public static final String EXTRA_PLAYER = "fr.devoxx.egress.EXTRA_PLAYER";

    private static final LatLng PARIS_GEO_POSITION = new LatLng(48.8534100, 2.3488000);
    private static final LatLngInterpolator latLngInterpolator = new LatLngInterpolator.LinearFixed();

    @InjectView(R.id.add_action) FloatingActionButton addActionButton;
    @InjectView(R.id.welcome) TextView welcomeView;
    @InjectView(R.id.status) TextView statusView;
    @InjectView(R.id.total_captures) TextView totalCapturesView;

    private GoogleMap map; // Might be null if Google Play services APK is not available.

    private GeoFire geoFire;
    private Firebase firebase;
    private GeoQuery geoQuery;

    private Circle circle;
    private boolean circleHasMoved = false;
    @Icicle LatLng lastCircleCenter;
    private int circleFillColor;
    private ObjectAnimator circleHintAnimator;

    private Map<String, Marker> displayedMarkersCache = new HashMap<>();
    private Map<Marker, Station> displayedStationsCache = new HashMap<>();

    private BitmapDescriptor freeMarkerIconDescriptor;
    private BitmapDescriptor pendingMarkerIconDescriptor;
    private BitmapDescriptor lockedMarkerIconDescriptor;
    private BitmapDescriptor ownedMarkerIconDescriptor;

    private float hideActionButtonOffset;

    private Marker selectedMarker;

    private StationValueEventListener stationValueEventListener = new StationValueEventListener();

    private Player player;
    private SupportMapFragment mapFragment;

    private Set<String> pendingCaptures = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.maps_activity);
        ButterKnife.inject(this);
        mapFragment = SupportMapFragment.newInstance(new GoogleMapOptions().mapType(GoogleMap.MAP_TYPE_TERRAIN));
        getSupportFragmentManager().beginTransaction().
                replace(R.id.map, mapFragment).
                commit();
        player = getIntent().getParcelableExtra(EXTRA_PLAYER);
        welcomeView.setText(getString(R.string.welcome, player.name));
        Icepick.restoreInstanceState(this, savedInstanceState);
        setupActionButton();
        setupGeoFire();
        setUpMapIfNeeded();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Icepick.saveInstanceState(this, outState);
    }


    private void setupActionButton() {
        hideActionButtonOffset = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100, getResources().getDisplayMetrics());
        addActionButton.setTranslationY(hideActionButtonOffset);
    }

    private void setupGeoFire() {
        firebase = new Firebase("https://shining-inferno-9452.firebaseio.com");
        firebase.authWithOAuthToken("google", player.token, new Firebase.AuthResultHandler() {
            @Override
            public void onAuthenticated(AuthData authData) {
                Timber.d("Authenticated");
                getPlayerInfos();
            }

            @Override
            public void onAuthenticationError(FirebaseError firebaseError) {
                Timber.d("Authentication error");
                Toast.makeText(MapsActivity.this, R.string.error, LENGTH_LONG).show();
            }
        });
        firebase.child(".info").child("connected").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() == true) {
                    statusView.setText(R.string.connected);
                    statusView.setTextColor(getResources().getColor(R.color.connected));
                } else {
                    statusView.setText(R.string.disconnected);
                    statusView.setTextColor(getResources().getColor(R.color.disconnected));
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });
        geoFire = new GeoFire(firebase.child("_geofire"));
    }

    private void getPlayerInfos() {
        final MapsActivity context = MapsActivity.this;
        if (!Preferences.hasPlayerId(context)) {
            Firebase pushRef = firebase.child("players").push();
            pushRef.setValue(player, new Firebase.CompletionListener() {
                @Override
                public void onComplete(FirebaseError firebaseError, Firebase firebase) {
                    if (firebaseError == null) {
                        Preferences.setPlayerId(context, firebase.getKey());
                        firebase.addValueEventListener(new PlayerScoreListener());
                    }
                }
            });
        } else {
            firebase.child("players").
                    child(Preferences.getPlayerId(context)).
                    addValueEventListener(new PlayerScoreListener());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();

    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #map} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (map == null) {
            // Try to obtain the map from the SupportMapFragment.
            map = mapFragment.getMap();
            // Check if we were successful in obtaining the map.
            if (map != null) {
                setUpMap();
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #map} is not null.
     */
    private void setUpMap() {
        if (lastCircleCenter == null) {
            lastCircleCenter = PARIS_GEO_POSITION;
        }

        map.getUiSettings().setMapToolbarEnabled(false);

        freeMarkerIconDescriptor = BitmapDescriptorFactory.fromResource(R.drawable.ic_maps_train_station);
        pendingMarkerIconDescriptor = BitmapDescriptorFactory.fromResource(R.drawable.ic_maps_train_station_pending);
        lockedMarkerIconDescriptor = BitmapDescriptorFactory.fromResource(R.drawable.ic_maps_train_station_locked);
        ownedMarkerIconDescriptor = BitmapDescriptorFactory.fromResource(R.drawable.ic_maps_train_station_owned);
        setupCircleHint();

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(lastCircleCenter, 13));

        geoQuery = geoFire.queryAtLocation(new GeoLocation(lastCircleCenter.latitude, lastCircleCenter.longitude), 1);
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(final String key, final GeoLocation location) {
                Timber.d("On key entered " + location);
                firebase.child(key).addValueEventListener(stationValueEventListener);
            }

            @Override
            public void onKeyExited(String key) {
                Timber.d("On key exited " + key);
                firebase.child(key).removeEventListener(stationValueEventListener);
                Marker markerToRemove = displayedMarkersCache.remove(key);
                if (markerToRemove != null) {
                    displayedStationsCache.remove(markerToRemove);
                    markerToRemove.remove();
                }
            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {
                Timber.d("On key moved " + key);
            }

            @Override
            public void onGeoQueryReady() {
                Timber.d("Geo query ready");
            }

            @Override
            public void onGeoQueryError(FirebaseError error) {
                Timber.d("Geo query error " + error);
            }
        });

        map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                selectedMarker = null;
                lastCircleCenter = latLng;
                circleHasMoved = true;
                animateCircle(circle, latLng, latLngInterpolator);
                geoQuery.setCenter(new GeoLocation(latLng.latitude, latLng.longitude));
                addActionButton.animate().translationY(hideActionButtonOffset).start();
            }
        });
        map.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(Marker marker) {
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {
                selectedMarker = marker;
                TrainStationInfoWindowView infoWindowView = (TrainStationInfoWindowView) LayoutInflater.from(MapsActivity.this).
                        inflate(R.layout.train_station_info_window_view, null);
                Station station = displayedStationsCache.get(marker);
                infoWindowView.bind(station);
                addActionButton.animate().translationY(station.isFree() ? 0 : hideActionButtonOffset).start();
                return infoWindowView;
            }
        });
        map.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                selectedMarker = null;
                marker.hideInfoWindow();
                addActionButton.animate().translationY(hideActionButtonOffset).start();
            }
        });
    }

    private void setupCircleHint() {
        circleFillColor = getResources().getColor(R.color.circle_color);
        CircleOptions circleOptions = new CircleOptions()
                .center(lastCircleCenter)
                .fillColor(circleFillColor)
                .strokeColor(getResources().getColor(R.color.circle_color))
                .radius(1000);
        circle = map.addCircle(circleOptions);
    }

    void animateCircle(final Circle circle, LatLng finalPosition, final LatLngInterpolator latLngInterpolator) {
        if (circleHintAnimator != null) {
            circleHintAnimator.cancel();
        }
        TypeEvaluator<LatLng> typeEvaluator = new TypeEvaluator<LatLng>() {
            @Override
            public LatLng evaluate(float fraction, LatLng startValue, LatLng endValue) {
                return latLngInterpolator.interpolate(fraction, startValue, endValue);
            }
        };
        Property<Circle, LatLng> property = Property.of(Circle.class, LatLng.class, "center");
        circleHintAnimator = ObjectAnimator.ofObject(circle, property, typeEvaluator, finalPosition);
        circleHintAnimator.addListener(new Animator.AnimatorListener() {
            private boolean cancelled;

            @Override
            public void onAnimationStart(Animator animation) {
                for (Marker marker : displayedMarkersCache.values()) {
                    marker.setVisible(false);
                }
                circle.setFillColor(Color.TRANSPARENT);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!cancelled) {
                    circle.setFillColor(circleFillColor);
                    for (Marker marker : displayedMarkersCache.values()) {
                        marker.setVisible(true);
                    }
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                cancelled = true;
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        circleHintAnimator.setDuration(1000);
        circleHintAnimator.start();
    }

    @OnClick(R.id.add_action)
    public void onAddActionClicked() {
        final Station station = displayedStationsCache.get(selectedMarker);
        pendingCaptures.add(station.getKey());
        firebase.child(station.getKey()).runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                mutableData.child(Station.FIELD_OWNER).setValue(player.name);
                mutableData.child(Station.FIELD_OWNER_MAIL).setValue(player.mail);
                mutableData.child(Station.FIELD_WHEN).setValue(System.currentTimeMillis());
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(FirebaseError firebaseError, boolean committed, DataSnapshot dataSnapshot) {
                pendingCaptures.remove(station.getKey());
                if (firebaseError != null) {
                    Toast.makeText(MapsActivity.this, R.string.error, LENGTH_LONG).show();
                } else {
                    Station station = new Station(dataSnapshot);
                    if (player.mail.equals(station.getOwnerMail())) {
                        firebase.child("players").
                                child(Preferences.getPlayerId(MapsActivity.this)).
                                child(Player.FIELD_SCORE).setValue(++player.score);
                    }
                    handleStationUpdated(dataSnapshot);
                    addActionButton.animate().translationY(hideActionButtonOffset).start();
                }
            }
        });
    }

    private void handleStationUpdated(DataSnapshot dataSnapshot) {
        Station station = new Station(dataSnapshot);
        if (!station.hasCoordinate()) {
            return;
        }
        Marker marker = displayedMarkersCache.get(station.getKey());
        if (marker == null) {
            marker = map.addMarker(new MarkerOptions()
                    .position(new LatLng(station.getLatitude(), station.getLongitude()))
                    .visible(!circleHasMoved)
                    .title(station.getName()));
        }
        marker.setIcon(getMarkerIcon(station));

        displayedMarkersCache.put(dataSnapshot.getKey(), marker);
        displayedStationsCache.put(marker, station);
    }

    private class StationValueEventListener implements ValueEventListener {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            Station station = new Station(dataSnapshot);
            if (!station.hasCoordinate()) {
                return;
            }
            Marker marker = displayedMarkersCache.get(station.getKey());
            if (marker == null) {
                marker = map.addMarker(new MarkerOptions()
                        .position(new LatLng(station.getLatitude(), station.getLongitude()))
                        .visible(!circleHasMoved)
                        .title(station.getName()));
            }
            marker.setIcon(getMarkerIcon(station));

            displayedMarkersCache.put(dataSnapshot.getKey(), marker);
            displayedStationsCache.put(marker, station);
        }

        @Override
        public void onCancelled(FirebaseError firebaseError) {

        }
    }

    private class PlayerScoreListener implements ValueEventListener {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            Map<String, Object> mapPlayerInfos = (Map<String, Object>) dataSnapshot.getValue();
            player.score = (long) mapPlayerInfos.get(Player.FIELD_SCORE);
            totalCapturesView.setText(getString(R.string.total_captures, mapPlayerInfos.get(Player.FIELD_SCORE)));
        }

        @Override
        public void onCancelled(FirebaseError firebaseError) {

        }
    }

    private BitmapDescriptor getMarkerIcon(Station station) {
        if (pendingCaptures.contains(station.getKey())) {
            return pendingMarkerIconDescriptor;
        } else if (station.isFree()) {
            return freeMarkerIconDescriptor;
        } else {
            return player.mail.equalsIgnoreCase(station.getOwnerMail()) ? ownedMarkerIconDescriptor : lockedMarkerIconDescriptor;
        }
    }
}
