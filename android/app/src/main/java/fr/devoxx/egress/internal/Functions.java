package fr.devoxx.egress.internal;

import com.google.android.gms.maps.model.LatLng;

import fr.devoxx.egress.model.Player;
import rx.functions.Func2;

public class Functions {

    public static Func2<String, String, Player> buildPlayer(final String mail) {
        return new Func2<String, String, Player>() {
            @Override
            public Player call(String token, String name) {
                return new Player(token, name, mail, 0);
            }
        };
    }

    public static double distance(LatLng originLatLng, LatLng destinationLatLng) {
        double earthRadius = 3958.75;
        double latDiff = Math.toRadians(destinationLatLng.latitude - originLatLng.latitude);
        double lngDiff = Math.toRadians(destinationLatLng.longitude - originLatLng.longitude);
        double a = Math.sin(latDiff / 2) * Math.sin(latDiff / 2) +
                Math.cos(Math.toRadians(originLatLng.latitude)) * Math.cos(Math.toRadians(destinationLatLng.latitude)) *
                        Math.sin(lngDiff / 2) * Math.sin(lngDiff / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = earthRadius * c;

        int meterConversion = 1609;

        return distance * meterConversion;
    }
}
