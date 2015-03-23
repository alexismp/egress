package fr.devoxx.egress.model;

import android.os.Parcel;
import android.os.Parcelable;

public class Player implements Parcelable {
    public final String token;
    public final String name;
    public final String mail;

    public Player(String token, String name, String mail) {
        this.token = token;
        this.name = name;
        this.mail = mail;
    }

    protected Player(Parcel in) {
        token = in.readString();
        name = in.readString();
        mail = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(token);
        dest.writeString(name);
        dest.writeString(mail);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<Player> CREATOR = new Parcelable.Creator<Player>() {
        @Override
        public Player createFromParcel(Parcel in) {
            return new Player(in);
        }

        @Override
        public Player[] newArray(int size) {
            return new Player[size];
        }
    };
}
