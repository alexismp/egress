package fr.devoxx.egress;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Property;
import android.view.LayoutInflater;
import android.view.View;

import com.firebase.client.AuthData;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.HashMap;
import java.util.Map;

import fr.devoxx.egress.internal.LatLngInterpolator;
import timber.log.Timber;

public class MapsActivity extends FragmentActivity {

    public static final String EXTRA_OAUTH_TOKEN = "fr.devoxx.egress.EXTRA_OAUTH_TOKEN";

    private static final LatLng PARIS_GEO_POSITION = new LatLng(48.8534100, 2.3488000);
    private static final LatLngInterpolator latLngInterpolator = new LatLngInterpolator.LinearFixed();

    private GoogleMap map; // Might be null if Google Play services APK is not available.

    private GeoFire geoFire;
    private Firebase firebase;
    private GeoQuery geoQuery;

    private Circle circle;
    private int circleFillColor;
    private ObjectAnimator circleHintAnimator;

    private Map<String, Marker> markersCache = new HashMap<>();
    private BitmapDescriptor markerIconDescriptor;
    private Map<Marker, DataSnapshot> dataSnapshotCache = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_activity);
        initGeoFire();
        setUpMapIfNeeded();
    }

    private void initGeoFire() {
        String token = getIntent().getStringExtra(EXTRA_OAUTH_TOKEN);
        firebase = new Firebase("https://shining-inferno-9452.firebaseio.com");
        firebase.authWithOAuthToken("google", token, new Firebase.AuthResultHandler() {
            @Override
            public void onAuthenticated(AuthData authData) {
                Timber.d("Authenticated");
            }

            @Override
            public void onAuthenticationError(FirebaseError firebaseError) {
                Timber.d("Authentication error");
            }
        });
        geoFire = new GeoFire(firebase.child("_geofire"));
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
            map = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
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
        markerIconDescriptor = BitmapDescriptorFactory.fromResource(R.drawable.ic_maps_train_station);
        setupCircleHint();

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(PARIS_GEO_POSITION, 13));

        geoQuery = geoFire.queryAtLocation(new GeoLocation(48.8534100, 2.3488000), 1);
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(final String key, final GeoLocation location) {
                Timber.d("On key entered " + location);
                firebase.child(key).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        Map<String, Object> dataValues = (Map<String, Object>) dataSnapshot.getValue();
                        Marker marker = map.addMarker(new MarkerOptions()
                                .position(new LatLng(location.latitude, location.longitude))
                                .visible(PARIS_GEO_POSITION.equals(circle.getCenter()))
                                .title((String) dataValues.get("NOM"))
                                .icon(markerIconDescriptor));
                        markersCache.put(key, marker);
                        dataSnapshotCache.put(marker, dataSnapshot);
                    }

                    @Override
                    public void onCancelled(FirebaseError firebaseError) {

                    }
                });
            }

            @Override
            public void onKeyExited(String key) {
                Timber.d("On key exited " + key);
                markersCache.remove(key).remove();
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
                animateCircle(circle, latLng, latLngInterpolator);
                geoQuery.setCenter(new GeoLocation(latLng.latitude, latLng.longitude));
            }
        });
        map.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(Marker marker) {
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {
                TrainStationInfoWindowView infoWindowView = (TrainStationInfoWindowView) LayoutInflater.from(MapsActivity.this).
                        inflate(R.layout.train_station_info_window_view, null);
                infoWindowView.bind(dataSnapshotCache.get(marker));
                return infoWindowView;
            }
        });
        map.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                marker.hideInfoWindow();
            }
        });
    }

    private void setupCircleHint() {
        circleFillColor = getResources().getColor(R.color.circle_color);
        CircleOptions circleOptions = new CircleOptions()
                .center(PARIS_GEO_POSITION)
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
                for (Marker marker : markersCache.values()) {
                    marker.setVisible(true);
                }
                circle.setFillColor(Color.TRANSPARENT);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!cancelled) {
                    circle.setFillColor(circleFillColor);
                    for (Marker marker : markersCache.values()) {
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
}
