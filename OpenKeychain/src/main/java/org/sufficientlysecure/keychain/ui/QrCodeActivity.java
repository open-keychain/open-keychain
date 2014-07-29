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

import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.ImageView;

import com.devspark.appmsg.AppMsg;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.helper.ActionBarHelper;
import org.sufficientlysecure.keychain.pgp.PgpKeyHelper;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.QrCodeUtils;

public class QrCodeActivity extends ActionBarActivity {

    private ImageView mFingerprintQrCode;

    private static final int QR_CODE_SIZE = 1000;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inflate a "Done" custom action bar
        ActionBarHelper.setOneButtonView(getSupportActionBar(),
                R.string.btn_okay, R.drawable.ic_action_done,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // "Done"
                        finish();
                    }
                }
        );

        setContentView(R.layout.qr_code_activity);

        Uri dataUri = getIntent().getData();
        if (dataUri == null) {
            Log.e(Constants.TAG, "Data missing. Should be Uri of key!");
            finish();
            return;
        }

        mFingerprintQrCode = (ImageView) findViewById(R.id.qr_code_image);

        mFingerprintQrCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        ProviderHelper providerHelper = new ProviderHelper(this);
        try {
            byte[] blob = (byte[]) providerHelper.getGenericData(
                    KeychainContract.KeyRings.buildUnifiedKeyRingUri(dataUri),
                    KeychainContract.KeyRings.FINGERPRINT, ProviderHelper.FIELD_TYPE_BLOB);
            if (blob == null) {
                Log.e(Constants.TAG, "key not found!");
                AppMsg.makeText(this, R.string.error_key_not_found, AppMsg.STYLE_ALERT).show();
                finish();
            }

            String fingerprint = PgpKeyHelper.convertFingerprintToHex(blob);
            String qrCodeContent = Constants.FINGERPRINT_SCHEME + ":" + fingerprint;
            mFingerprintQrCode.setImageBitmap(QrCodeUtils.getQRCodeBitmap(qrCodeContent, QR_CODE_SIZE));
        } catch (ProviderHelper.NotFoundException e) {
            Log.e(Constants.TAG, "key not found!", e);
            AppMsg.makeText(this, R.string.error_key_not_found, AppMsg.STYLE_ALERT).show();
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // custom activity transition to get zoom in effect
        this.overridePendingTransition(R.anim.qr_code_zoom_enter, android.R.anim.fade_out);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // custom activity transition to get zoom out effect
        this.overridePendingTransition(0, R.anim.qr_code_zoom_exit);
    }

}