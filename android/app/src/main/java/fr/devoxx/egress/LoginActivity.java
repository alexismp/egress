package fr.devoxx.egress;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.plus.Plus;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import fr.devoxx.egress.internal.Observables;
import fr.devoxx.egress.model.Player;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

import static rx.android.app.AppObservable.bindActivity;


public class LoginActivity extends Activity {

    private static final int RC_SIGN_IN = 1001;

    private GoogleApiClient googleApiClient;
    private boolean signInClicked;
    private ConnectionResult connectionResult;
    private boolean intentInProgress;

    @InjectView(R.id.sign_in_button) SignInButton signInButton;
    @InjectView(R.id.sign_in_progress) ProgressBar signInProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity);
        ButterKnife.inject(this);
        signInButton.setVisibility(View.VISIBLE);
        signInProgress.setVisibility(View.GONE);
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle bundle) {
                        goToMapsScreen();
                    }

                    @Override
                    public void onConnectionSuspended(int i) {

                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                        if (!intentInProgress) {
                            // Store the ConnectionResult so that we can use it later when the user clicks
                            // 'sign-in'.
                            connectionResult = result;

                            if (signInClicked) {
                                // The user has already clicked 'sign-in' so we attempt to resolve all
                                // errors until the user is signed in, or they cancel.
                                resolveSignInError();
                            }
                        }
                    }
                })
                .addApi(Plus.API)
                .addScope(Plus.SCOPE_PLUS_LOGIN)
                .build();
    }

    @Override
    protected void onStart() {
        super.onStart();
        googleApiClient.connect();
    }

    @Override
    protected void onStop() {
        googleApiClient.disconnect();
        super.onStop();
    }

    @OnClick(R.id.sign_in_button)
    public void onSignInClicked() {
        signInClicked = true;
        if (connectionResult != null) {
            resolveSignInError();
        } else {
            goToMapsScreen();
        }
    }

    private void resolveSignInError() {
        if (connectionResult.hasResolution()) {
            try {
                intentInProgress = true;
                startIntentSenderForResult(connectionResult.getResolution().getIntentSender(), RC_SIGN_IN, null, 0, 0, 0);
            } catch (IntentSender.SendIntentException e) {
                // The intent was canceled before it was sent.  Return to the default
                // state and attempt to connect to get an updated ConnectionResult.
                intentInProgress = false;
                googleApiClient.connect();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_SIGN_IN) {
            if (resultCode != RESULT_OK) {
                signInClicked = false;
            }

            intentInProgress = false;

            if (!googleApiClient.isConnecting()) {
                googleApiClient.connect();
            }
        }
    }

    private void goToMapsScreen() {
        signInButton.setVisibility(View.GONE);
        signInProgress.setVisibility(View.VISIBLE);
        final String mail = Plus.AccountApi.getAccountName(googleApiClient);
        bindActivity(this, Observables.fetchPlayerInfos(LoginActivity.this, googleApiClient, mail))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Player>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        Toast.makeText(LoginActivity.this, R.string.error, Toast.LENGTH_LONG).show();
                        signInButton.setVisibility(View.VISIBLE);
                        signInProgress.setVisibility(View.GONE);
                    }

                    @Override
                    public void onNext(Player player) {
                        signInButton.setVisibility(View.GONE);
                        signInProgress.setVisibility(View.GONE);
                        Intent intent = new Intent(LoginActivity.this, MapsActivity.class);
                        intent.putExtra(MapsActivity.EXTRA_PLAYER, player);
                        startActivity(intent);
                        finish();
                    }
                });
    }

}
