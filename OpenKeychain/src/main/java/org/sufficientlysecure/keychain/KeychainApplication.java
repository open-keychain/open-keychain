/*
 * Copyright (C) 2017 Schürmann & Breitmoser GbR
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.sufficientlysecure.keychain;


import java.security.Security;
import java.util.HashMap;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import androidx.annotation.Nullable;
import android.widget.Toast;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.sufficientlysecure.keychain.analytics.AnalyticsManager;
import org.sufficientlysecure.keychain.network.TlsCertificatePinning;
import org.sufficientlysecure.keychain.provider.TemporaryFileProvider;
import org.sufficientlysecure.keychain.service.ContactSyncAdapterService;
import org.sufficientlysecure.keychain.keysync.KeyserverSyncManager;
import org.sufficientlysecure.keychain.util.PRNGFixes;
import org.sufficientlysecure.keychain.util.Preferences;
import timber.log.Timber;
import timber.log.Timber.DebugTree;


public class KeychainApplication extends Application {
    AnalyticsManager analyticsManager;

    /**
     * Called when the application is starting, before any activity, service, or receiver objects
     * (excluding content providers) have been created.
     */
    @Override
    public void onCreate() {
        super.onCreate();

        /*
         * Sets our own Bouncy Castle library as preferred security provider
         *
         * because Android's default provider config has BC at position 3,
         * we need to remove it and insert BC again at position 1 (above OpenSSLProvider!)
         *
         * (insertProviderAt() position starts from 1)
         */
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
        Security.insertProviderAt(new BouncyCastleProvider(), 1);

        /*
         * apply RNG fixes
         *
         * among other things, executes Security.insertProviderAt(new
         * LinuxPRNGSecureRandomProvider(), 1) for Android <= SDK 17
         */
        PRNGFixes.apply();
        Timber.d("Bouncy Castle set and PRNG Fixes applied!");

        updateLoggingStatus();

        /*
        if (Constants.DEBUG) {
            Provider[] providers = Security.getProviders();
            Log.d(Constants.TAG, "Installed Security Providers:");
            for (Provider p : providers) {
                Log.d(Constants.TAG, "provider class: " + p.getClass().getName());
            }
        }
        */

        // Add OpenKeychain account to Android to link contacts with keys and keyserver sync
        createAccountIfNecessary(this);

        Preferences preferences = Preferences.getPreferences(this);
        if (preferences.isAppExecutedFirstTime()) {
            preferences.setAppExecutedFirstTime(false);

            ContactSyncAdapterService.enableContactsSync(this);

            preferences.setPrefVersionToCurrentVersion();
        }

        // Upgrade preferences as needed
        preferences.upgradePreferences();

        TlsCertificatePinning.addPinnedCertificate("keys.openpgp.org", getAssets(), "LetsEncryptCA.cer");
        TlsCertificatePinning.addPinnedCertificate("hkps.pool.sks-keyservers.net", getAssets(), "hkps.pool.sks-keyservers.net.CA.cer");
        TlsCertificatePinning.addPinnedCertificate("pgp.mit.edu", getAssets(), "pgp.mit.edu.cer");
        TlsCertificatePinning.addPinnedCertificate("keyserver.ubuntu.com", getAssets(), "LetsEncryptCA.cer");

        KeyserverSyncManager.updateKeyserverSyncScheduleAsync(this, false);

        TemporaryFileProvider.scheduleCleanupImmediately(getApplicationContext());

        analyticsManager = AnalyticsManager.getInstance(getApplicationContext());
        analyticsManager.initialize(this);
    }

    /**
     * @return the OpenKeychain contact/keyserver sync account if it exists or was successfully
     * created, null otherwise
     */
    public static @Nullable Account createAccountIfNecessary(Context context) {
        try {
            AccountManager manager = AccountManager.get(context);
            Account[] accounts = manager.getAccountsByType(Constants.ACCOUNT_TYPE);

            Account account = new Account(Constants.ACCOUNT_NAME, Constants.ACCOUNT_TYPE);
            if (accounts.length == 0) {
                if (!manager.addAccountExplicitly(account, null, null)) {
                    Timber.d("error when adding account via addAccountExplicitly");
                    return null;
                } else {
                    return account;
                }
            } else {
                return accounts[0];
            }
        } catch (SecurityException e) {
            Timber.e(e, "SecurityException when adding the account");
            Toast.makeText(context, R.string.reinstall_openkeychain, Toast.LENGTH_LONG).show();
            return null;
        }
    }

    public static HashMap<String,Bitmap> qrCodeCache = new HashMap<>();

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);

        if (level >= TRIM_MEMORY_UI_HIDDEN) {
            qrCodeCache.clear();
        }
    }

    private void updateLoggingStatus() {
        Timber.uprootAll();
        boolean enableDebugLogging = Constants.DEBUG;
        if (enableDebugLogging) {
            Timber.plant(new DebugTree());
        }
    }

    public AnalyticsManager getAnalyticsManager() {
        return analyticsManager;
    }
}
