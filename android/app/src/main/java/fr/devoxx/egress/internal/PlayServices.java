package fr.devoxx.egress.internal;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;

import com.google.android.gms.auth.GooglePlayServicesAvailabilityException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.Scopes;

public class PlayServices {

    public static final int REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR = 2001;

    public static final String SCOPE_PROFILE = "oauth2:" + Scopes.PLUS_LOGIN;

    private PlayServices() {

    }

    /**
     * This method is a hook for background threads and async tasks that need to
     * provide the user a response UI when an exception occurs.
     */
    public static void handleException(Activity activity, final Throwable t) {
        if (t instanceof GooglePlayServicesAvailabilityException) {
            // The Google Play services APK is old, disabled, or not present.
            // Show a dialog created by Google Play services that allows
            // the user to update the APK
            int statusCode = ((GooglePlayServicesAvailabilityException) t).getConnectionStatusCode();
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(statusCode, activity,
                    REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR);
            dialog.show();
        } else if (t instanceof UserRecoverableAuthException) {
            // Unable to authenticate, such as when the user has not yet granted
            // the app access to the account, but the user can fix this.
            // Forward the user to an activity in Google Play services.
            Intent intent = ((UserRecoverableAuthException) t).getIntent();
            activity.startActivityForResult(intent, REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR);
        }
    }
}
