package fr.devoxx.egress.internal;

import android.accounts.Account;
import android.content.Context;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.plus.Plus;

import java.io.IOException;

import fr.devoxx.egress.model.Player;
import rx.Observable;
import rx.Subscriber;

public class Observables {

    private static final String GOOGLE_TYPE = "com.google";
    private static final String OAUTH2_PROFILE = "oauth2:profile";

    private Observables() {

    }

    public static Observable<Player> fetchPlayerInfos(final Context context, final GoogleApiClient googleApiClient, final String mail) {
        return Observable.combineLatest(
                Observables.fetchOauthToken(context, mail, OAUTH2_PROFILE),
                Observables.fetchPlayerName(googleApiClient),
                Functions.buildPlayer(mail));
    }

    public static Observable<String> fetchOauthToken(final Context context, final String mail, final String scope) {
        return Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                if (!subscriber.isUnsubscribed()) {
                    try {
                        Account account = new Account(mail, GOOGLE_TYPE);
                        cleanToken(account);
                        subscriber.onNext(GoogleAuthUtil.getToken(context, account, scope));
                        subscriber.onCompleted();
                    } catch (IOException | GoogleAuthException e) {
                        subscriber.onError(e);
                    }
                }
            }

            private void cleanToken(Account account) throws IOException, GoogleAuthException {
                String token = GoogleAuthUtil.getToken(context, account, scope);
                GoogleAuthUtil.clearToken(context, token);
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
