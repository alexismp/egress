package fr.devoxx.egress;

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.gms.common.AccountPicker;

import butterknife.ButterKnife;
import butterknife.OnClick;
import fr.devoxx.egress.internal.Observables;
import fr.devoxx.egress.internal.PlayServices;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

import static fr.devoxx.egress.internal.PlayServices.SCOPE_PROFILE;
import static rx.android.app.AppObservable.bindActivity;


public class LoginActivity extends Activity {

    private static final int REQUEST_CODE_PICK_ACCOUNT = 1001;

    private String playerMail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity);
        ButterKnife.inject(this);
    }

    @OnClick(R.id.sign_in_button)
    public void onSignInClicked() {
        String[] accountTypes = new String[]{"com.google"};
        Intent intent = AccountPicker.newChooseAccountIntent(null, null, accountTypes, false, null, null, null, null);
        startActivityForResult(intent, REQUEST_CODE_PICK_ACCOUNT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_PICK_ACCOUNT) {
            // Receiving a result from the AccountPicker
            if (resultCode == RESULT_OK) {
                playerMail = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                // With the account name acquired, go get the auth token
                fetchToken();
            } else if (resultCode == RESULT_CANCELED) {
                // The account picker dialog closed without selecting an account.
                // Notify users that they must pick an account to proceed.
                Toast.makeText(this, R.string.choose_account_mandatory, Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == PlayServices.REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR && resultCode == RESULT_OK) {
            // Receiving a result that follows a GoogleAuthException, try auth again
            fetchToken();
        }
    }

    private void fetchToken() {
        bindActivity(this, Observables.fetchOauthToken(this, playerMail, SCOPE_PROFILE))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<String>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable t) {
                        PlayServices.handleException(LoginActivity.this, t);
                    }

                    @Override
                    public void onNext(String token) {
                        Intent intent = new Intent(LoginActivity.this, MapsActivity.class);
                        intent.putExtra(MapsActivity.EXTRA_OAUTH_TOKEN, token);
                        startActivity(intent);
                    }
                });
    }


}
