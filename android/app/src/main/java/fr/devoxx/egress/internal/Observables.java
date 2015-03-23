package fr.devoxx.egress.internal;

import android.content.Context;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.plus.Plus;

import java.io.IOException;

import fr.devoxx.egress.model.Player;
import rx.Observable;
import rx.Subscriber;

import static fr.devoxx.egress.internal.PlayServices.SCOPE_PROFILE;

public class Observables {

    private Observables() {

    }

    public static Observable<Player> fetchPlayerInfos(final Context context, final GoogleApiClient googleApiClient, final String mail){
        return Observable.combineLatest(
                Observables.fetchOauthToken(context, mail, SCOPE_PROFILE),
                Observables.fetchPlayerName(googleApiClient),
                Functions.buildPlayer(mail));
    }

    public static Observable<String> fetchOauthToken(final Context context, final String mail, final String scope) {
        return Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                if (!subscriber.isUnsubscribed()) {
                    try {
                        subscriber.onNext(GoogleAuthUtil.getToken(context, mail, scope));
                        subscriber.onCompleted();
                    } catch (IOException | GoogleAuthException e) {
                        subscriber.onError(e);
                    }
                }
            }
        });
    }

    public static Observable<String> fetchPlayerName(final GoogleApiClient googleApiClient) {
        return Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                if (!subscriber.isUnsubscribed()) {
                    subscriber.onNext(Plus.PeopleApi.getCurrentPerson(googleApiClient).getDisplayName());
                    subscriber.onCompleted();
                }
            }
        });
    }
}
