package fr.devoxx.egress.internal;

import android.content.Context;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;

import java.io.IOException;

import rx.Observable;
import rx.Subscriber;

public class Observables {

    private Observables() {

    }

    public static Observable<String> fetchOauthToken(final Context context, final String account, final String scope) {
        return Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                if (!subscriber.isUnsubscribed()) {
                    try {
                        subscriber.onNext(GoogleAuthUtil.getToken(context, account, scope));
                        subscriber.onCompleted();
                    } catch (IOException | GoogleAuthException e) {
                        subscriber.onError(e);
                    }
                }
            }
        });
    }
}
