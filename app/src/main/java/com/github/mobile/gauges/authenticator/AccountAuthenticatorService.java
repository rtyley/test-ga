package com.github.mobile.gauges.authenticator;

import static android.accounts.AccountManager.ACTION_AUTHENTICATOR_INTENT;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Authenticator service that returns a subclass of AbstractAccountAuthenticator in onBind()
 */
public class AccountAuthenticatorService extends Service {
    private static GaugesAccountAuthenticator sAccountAuthenticator = null;

    public IBinder onBind(Intent intent) {
        return intent.getAction().equals(ACTION_AUTHENTICATOR_INTENT) ? getAuthenticator().getIBinder() : null;
    }

    private GaugesAccountAuthenticator getAuthenticator() {
        if (sAccountAuthenticator == null)
            sAccountAuthenticator = new GaugesAccountAuthenticator(this);
        return sAccountAuthenticator;
    }

}