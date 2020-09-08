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


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.Security;
import java.util.HashMap;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.widget.Toast;

import androidx.annotation.Nullable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.sufficientlysecure.keychain.keysync.KeyserverSyncManager;
import org.sufficientlysecure.keychain.network.TlsCertificatePinning;
import org.sufficientlysecure.keychain.provider.TemporaryFileProvider;
import org.sufficientlysecure.keychain.util.PRNGFixes;
import org.sufficientlysecure.keychain.util.Preferences;
import timber.log.Timber;
import timber.log.Timber.DebugTree;


public class KeychainApplication extends Application {
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

        Preferences preferences = Preferences.getPreferences(this);
        if (preferences.isAppExecutedFirstTime()) {
            preferences.setAppExecutedFirstTime(false);
            preferences.setPrefVersionToCurrentVersion();
        }

        // Upgrade preferences as needed
        preferences.upgradePreferences();

        TlsCertificatePinning.addPinnedCertificate("keys.openpgp.org", getAssets(), "LetsEncryptCA.cer");
        TlsCertificatePinning.addPinnedCertificate("hkps.pool.sks-keyservers.net", getAssets(), "hkps.pool.sks-keyservers.net.CA.cer");
        TlsCertificatePinning.addPinnedCertificate("pgp.mit.edu", getAssets(), "pgp.mit.edu.cer");
        TlsCertificatePinning.addPinnedCertificate("keyserver.ubuntu.com", getAssets(), "LetsEncryptCA.cer");

        // only set up the rest on our main process
        if (!BuildConfig.APPLICATION_ID.equals(getProcessName())) {
            return;
        }

        KeyserverSyncManager.updateKeyserverSyncScheduleAsync(this, false);

        TemporaryFileProvider.scheduleCleanupImmediately(getApplicationContext());
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

    public static String getProcessName() {
        if (Build.VERSION.SDK_INT >= 28)
            return Application.getProcessName();

        // Using the same technique as Application.getProcessName() for older devices
        // Using reflection since ActivityThread is an internal API

        try {
            @SuppressLint("PrivateApi")
            Class<?> activityThread = Class.forName("android.app.ActivityThread");

            // Before API 18, the method was incorrectly named "currentPackageName", but it still returned the process name
            // See https://github.com/aosp-mirror/platform_frameworks_base/commit/b57a50bd16ce25db441da5c1b63d48721bb90687
            String methodName = Build.VERSION.SDK_INT >= 18 ? "currentProcessName" : "currentPackageName";

            Method getProcessName = activityThread.getDeclaredMethod(methodName);
            return (String) getProcessName.invoke(null);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
