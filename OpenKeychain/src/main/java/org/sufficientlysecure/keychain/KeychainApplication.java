/*
 * Copyright (C) 2012-2016 Dominik Schürmann <dominik@dominikschuermann.de>
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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.widget.Toast;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.sufficientlysecure.keychain.provider.KeychainDatabase;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.provider.TemporaryFileProvider;
import org.sufficientlysecure.keychain.service.ContactSyncAdapterService;
import org.sufficientlysecure.keychain.service.KeyserverSyncAdapterService;
import org.sufficientlysecure.keychain.ui.ConsolidateDialogActivity;
import org.sufficientlysecure.keychain.ui.MigrateSymmetricActivity;
import org.sufficientlysecure.keychain.ui.passphrasedialog.PassphraseDialogActivity;
import org.sufficientlysecure.keychain.ui.util.FormattingUtils;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.PRNGFixes;
import org.sufficientlysecure.keychain.util.Preferences;
import org.sufficientlysecure.keychain.util.TlsHelper;

import java.security.Security;
import java.util.HashMap;


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
        Log.d(Constants.TAG, "Bouncy Castle set and PRNG Fixes applied!");

        /*
        if (Constants.DEBUG) {
            Provider[] providers = Security.getProviders();
            Log.d(Constants.TAG, "Installed Security Providers:");
            for (Provider p : providers) {
                Log.d(Constants.TAG, "provider class: " + p.getClass().getName());
            }
        }
        */

        // Create OpenKeychain directory on sdcard if not existing
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            if (!Constants.Path.APP_DIR.exists() && !Constants.Path.APP_DIR.mkdirs()) {
                // ignore this for now, it's not crucial
                // that the directory doesn't exist at this point
            }
        }

        brandGlowEffect(getApplicationContext(),
                FormattingUtils.getColorFromAttr(getApplicationContext(), R.attr.colorPrimary));

        // Add OpenKeychain account to Android to link contacts with keys and keyserver sync
        createAccountIfNecessary(this);

        if (Preferences.getKeyserverSyncEnabled(this)) {
            // will update a keyserver sync if the interval has changed
            KeyserverSyncAdapterService.enableKeyserverSync(this);
        }

        Preferences prefs = Preferences.getPreferences(this);

        // if first time, enable keyserver and contact sync
        if (prefs.isFirstTime()) {
            KeyserverSyncAdapterService.enableKeyserverSync(this);
            ContactSyncAdapterService.enableContactsSync(this);
        }

        // Update keyserver list as needed
        prefs.upgradePreferences(this);

        TlsHelper.addPinnedCertificate("hkps.pool.sks-keyservers.net", getAssets(), "hkps.pool.sks-keyservers.net.CA.cer");
        TlsHelper.addPinnedCertificate("pgp.mit.edu", getAssets(), "pgp.mit.edu.cer");
        TlsHelper.addPinnedCertificate("api.keybase.io", getAssets(), "api.keybase.io.CA.cer");

        TemporaryFileProvider.cleanUp(this);

        if (!checkConsolidateRecovery()) {
            // force DB upgrade, https://github.com/open-keychain/open-keychain/issues/1334
            new KeychainDatabase(this).getReadableDatabase().close();
        }

        // migrate is first attempted when upgrading the db
        // if migrate isn't completed, try again when any activity is created
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            Preferences mPreferences = Preferences.getPreferences(getApplicationContext());

            @Override
            public void onActivityCreated(Activity activity, Bundle bundle) {
                if (mPreferences.isUsingS2k() &&
                        !(activity instanceof MigrateSymmetricActivity
                                || activity instanceof PassphraseDialogActivity
                                || activity instanceof ConsolidateDialogActivity)) {
                    if (new ProviderHelper(activity).hasSecretKeys()) {
                        Intent intent = new Intent(activity, MigrateSymmetricActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        activity.startActivity(intent);
                    } else {
                        mPreferences.setUsingS2k(false);
                    }
                }
            }

            @Override
            public void onActivityStarted(Activity activity) {

            }

            @Override
            public void onActivityResumed(Activity activity) {

            }

            @Override
            public void onActivityPaused(Activity activity) {

            }

            @Override
            public void onActivityStopped(Activity activity) {

            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {

            }

            @Override
            public void onActivityDestroyed(Activity activity) {

            }
        });
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
                    Log.d(Constants.TAG, "error when adding account via addAccountExplicitly");
                    return null;
                } else {
                    return account;
                }
            } else {
                return accounts[0];
            }
        } catch (SecurityException e) {
            Log.e(Constants.TAG, "SecurityException when adding the account", e);
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

    /**
     * Restart consolidate process if it has been interruped before
     */
    public boolean checkConsolidateRecovery() {
        if (Preferences.getPreferences(this).getCachedConsolidate()) {
            Intent consolidateIntent = new Intent(this, ConsolidateDialogActivity.class);
            consolidateIntent.putExtra(ConsolidateDialogActivity.EXTRA_CONSOLIDATE_RECOVERY, true);
            consolidateIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(consolidateIntent);
            return true;
        } else {
            return false;
        }
    }

    static void brandGlowEffect(Context context, int brandColor) {
        // no hack on Android 5
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            try {
                // terrible hack to brand the edge overscroll glow effect
                // https://gist.github.com/menny/7878762#file-brandgloweffect_full-java

                //glow
                int glowDrawableId = context.getResources().getIdentifier("overscroll_glow", "drawable", "android");
                Drawable androidGlow = context.getResources().getDrawable(glowDrawableId);
                androidGlow.setColorFilter(brandColor, PorterDuff.Mode.SRC_IN);
                //edge
                int edgeDrawableId = context.getResources().getIdentifier("overscroll_edge", "drawable", "android");
                Drawable androidEdge = context.getResources().getDrawable(edgeDrawableId);
                androidEdge.setColorFilter(brandColor, PorterDuff.Mode.SRC_IN);
            } catch (Exception ignored) {
            }
        }
    }
}
