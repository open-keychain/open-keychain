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

package org.sufficientlysecure.keychain.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.base.BaseActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

public class SettingsSmartPGPAuthoritiesActivity extends BaseActivity {

    public static final String EXTRA_SMARTPGP_AUTHORITIES = "smartpgp_authorities";

    private static final String KEYSTORE_FILE = "smartpgp_authorities.keystore";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        String authorities[] = intent.getStringArrayExtra(EXTRA_SMARTPGP_AUTHORITIES);
        loadFragment(savedInstanceState, authorities);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void initLayout() {
        setContentView(R.layout.smartpgp_authorities_preference);
    }

    private void loadFragment(Bundle savedInstanceState, String[] authorities) {
        // However, if we're being restored from a previous state,
        // then we don't need to do anything and should return or else
        // we could end up with overlapping fragments.
        if (savedInstanceState != null) {
            return;
        }

        SettingsSmartPGPAuthorityFragment fragment = SettingsSmartPGPAuthorityFragment.newInstance(authorities);

        // Add the fragment to the 'fragment_container' FrameLayout
        // NOTE: We use commitAllowingStateLoss() to prevent weird crashes!
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.smartpgp_authorities_settings_fragment_container, fragment)
                .commitAllowingStateLoss();
        // do it immediately!
        getSupportFragmentManager().executePendingTransactions();
    }

    public static final KeyStore readKeystore(final Context ctx) {
        try {
            final File kf = new File(ctx.getFilesDir(), KEYSTORE_FILE);
            final KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());

            ks.load(null, null);

            if (kf.exists()) {
                final FileInputStream fis = new FileInputStream(kf);
                ks.load(fis, null);
                fis.close();
            }

            return ks;
        } catch (Exception e) {
            return null;
        }
    }

    public static final void writeKeystore(final Context ctx, final KeyStore ks) {
        try {
            final File kf = new File(ctx.getFilesDir(), KEYSTORE_FILE);

            if (kf.exists()) {
                kf.delete();
            }

            final FileOutputStream fos = new FileOutputStream(kf);
            ks.store(fos, null);
            fos.flush();
            fos.close();
        } catch (Exception e) {
        }
    }
}
