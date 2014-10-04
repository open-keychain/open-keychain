/*
 * Copyright (C) 2014 Dominik Schürmann <dominik@dominikschuermann.de>
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

import android.content.Intent;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.ImageView;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.CanonicalizedPublicKeyRing;
import org.sufficientlysecure.keychain.provider.CachedPublicKeyRing;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.service.results.OperationResult;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.ui.widget.ExchangeKeySpinner;
import org.sufficientlysecure.keychain.ui.widget.KeySpinner;
import org.sufficientlysecure.keychain.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import edu.cmu.cylab.starslinger.exchange.ExchangeActivity;
import edu.cmu.cylab.starslinger.exchange.ExchangeConfig;

public class AddKeysActivity extends ActionBarActivity {

    ExchangeKeySpinner mSafeSlingerKeySpinner;
    View mActionSafeSlinger;
    ImageView mActionSafeSlingerIcon;
    View mActionQrCode;
    View mActionSearchCloud;

    ProviderHelper mProviderHelper;

    long mExchangeMasterKeyId = Constants.key.none;

    private static final int REQUEST_CODE_SAFESLINGER = 1;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mProviderHelper = new ProviderHelper(this);

        setContentView(R.layout.add_key_activity);

        mSafeSlingerKeySpinner = (ExchangeKeySpinner) findViewById(R.id.add_keys_safeslinger_key_spinner);
        mActionSafeSlinger = findViewById(R.id.add_keys_safeslinger);
        mActionSafeSlingerIcon = (ImageView) findViewById(R.id.add_keys_safeslinger_icon);
        // make certify image gray, like action icons
        mActionSafeSlingerIcon.setColorFilter(getResources().getColor(R.color.tertiary_text_light),
                PorterDuff.Mode.SRC_IN);
        mActionQrCode = findViewById(R.id.add_keys_qr_code);
        mActionSearchCloud = findViewById(R.id.add_keys_search_cloud);

        mSafeSlingerKeySpinner.setOnKeyChangedListener(new KeySpinner.OnKeyChangedListener() {
            @Override
            public void onKeyChanged(long masterKeyId) {
                mExchangeMasterKeyId = masterKeyId;
            }
        });

        mActionSafeSlinger.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startExchange();
            }
        });

        mActionQrCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startQrCode();
            }
        });

        mActionSearchCloud.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchCloud();
            }
        });

    }

    private void startExchange() {
        if (mExchangeMasterKeyId == 0) {
            Notify.showNotify(this, getString(R.string.select_key_for_exchange),
                    Notify.Style.ERROR);
        } else {
            // retrieve public key blob and start SafeSlinger
            Uri uri = KeychainContract.KeyRingData.buildPublicKeyRingUri(mExchangeMasterKeyId);
            try {
                byte[] keyBlob = (byte[]) mProviderHelper.getGenericData(
                        uri, KeychainContract.KeyRingData.KEY_RING_DATA, ProviderHelper.FIELD_TYPE_BLOB);

                Intent slingerIntent = new Intent(this, ExchangeActivity.class);
                slingerIntent.putExtra(ExchangeConfig.extra.USER_DATA, keyBlob);
                slingerIntent.putExtra(ExchangeConfig.extra.HOST_NAME, Constants.SAFESLINGER_SERVER);
                startActivityForResult(slingerIntent, REQUEST_CODE_SAFESLINGER);
            } catch (ProviderHelper.NotFoundException e) {
                Log.e(Constants.TAG, "personal key not found", e);
            }
        }
    }

    private void startQrCode() {

    }

    private void searchCloud() {
        Intent importIntent = new Intent(this, ImportKeysActivity.class);
        startActivity(importIntent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // if a result has been returned, display a notify
        if (data != null && data.hasExtra(OperationResult.EXTRA_RESULT)) {
            OperationResult result = data.getParcelableExtra(OperationResult.EXTRA_RESULT);
            result.createNotify(this).show();
        } else {
            switch (requestCode) {
                case REQUEST_CODE_SAFESLINGER:
                    switch (resultCode) {
                        case ExchangeActivity.RESULT_EXCHANGE_OK:
                            // import exchanged keys
                            Intent importIntent = new Intent(this, ImportKeysActivity.class);
                            importIntent.setAction(ImportKeysActivity.ACTION_IMPORT_KEY);
                            importIntent.putExtra(ImportKeysActivity.EXTRA_KEY_BYTES, getSlingedKeys(data));
                            startActivity(importIntent);
                            break;
                        case ExchangeActivity.RESULT_EXCHANGE_CANCELED:
                            // handle canceled result
                            // ...
                            break;
                    }
                    break;
            }
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private static byte[] getSlingedKeys(Intent data) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        Bundle extras = data.getExtras();
        if (extras != null) {
            byte[] d;
            int i = 0;
            do {
                d = extras.getByteArray(ExchangeConfig.extra.MEMBER_DATA + i);
                if (d != null) {
                    try {
                        out.write(d);
                    } catch (IOException e) {
                        Log.e(Constants.TAG, "IOException", e);
                    }
                    i++;
                }
            } while (d != null);
        }

        return out.toByteArray();
    }
}
