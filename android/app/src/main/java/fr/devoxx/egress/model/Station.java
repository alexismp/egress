package fr.devoxx.egress.model;

import android.text.TextUtils;

import com.firebase.client.DataSnapshot;

import java.util.Map;

public class Station {

    public static final String FIELD_NAME = "NOM";
    public static final String FIELD_LATITUDE = "LATITUDE";
    public static final String FIELD_LONGITUDE = "LONGITUDE";
    public static final String FIELD_OWNER = "owner";
    public static final String FIELD_OWNER_MAIL = "ownerMail";
    public static final String FIELD_WHEN = "when";

    private final String key;
    private final Map<String, Object> dataValues;

    public Station(DataSnapshot dataSnapshot) {
        this.dataValues = (Map<String, Object>) dataSnapshot.getValue();
        this.key = dataSnapshot.getKey();
    }

    public String getKey() {
        return key;
    }

    public String getName() {
        return (String) dataValues.get(FIELD_NAME);
    }

    public String getOwner() {
        return (String) dataValues.get(FIELD_OWNER);
    }

    public String getOwnerMail() {
        return (String) dataValues.get(FIELD_OWNER_MAIL);
    }

    public Double getLatitude() {
        String latitudeStr = (String) dataValues.get(FIELD_LATITUDE);
        return latitudeStr == null ? null : Double.valueOf(latitudeStr);
    }

    public Double getLongitude() {
        String longitudeStr = (String) dataValues.get(FIELD_LONGITUDE);
        return longitudeStr == null ? null : Double.valueOf(longitudeStr);
    }

    public boolean isFree() {
        return TextUtils.isEmpty(getOwner());
    }

    public boolean hasCoordinate() {
        return getLatitude() != null && getLongitude() != null;
    }
}
