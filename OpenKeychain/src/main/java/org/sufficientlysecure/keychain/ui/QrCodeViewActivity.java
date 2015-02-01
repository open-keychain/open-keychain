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

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.ImageView;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.ui.util.Notify.Style;
import org.sufficientlysecure.keychain.ui.util.QrCodeUtils;
import org.sufficientlysecure.keychain.util.Log;

public class QrCodeViewActivity extends BaseActivity {

    private ImageView mFingerprintQrCode;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inflate a "Done" custom action bar
        setFullScreenDialogClose(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // "Done"
                        finish();
                    }
                }
        );

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
                Notify.showNotify(this, R.string.error_key_not_found, Style.ERROR);
                finish();
            }

            String fingerprint = KeyFormattingUtils.convertFingerprintToHex(blob);
            String qrCodeContent = Constants.FINGERPRINT_SCHEME + ":" + fingerprint;

            // create a minimal size qr code, we can keep this in ram no problem
            final Bitmap qrCode = QrCodeUtils.getQRCodeBitmap(qrCodeContent, 0);

            mFingerprintQrCode.getViewTreeObserver().addOnGlobalLayoutListener(
                    new OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    // create actual bitmap in display dimensions
                    Bitmap scaled = Bitmap.createScaledBitmap(qrCode,
                            mFingerprintQrCode.getWidth(), mFingerprintQrCode.getWidth(), false);
                    mFingerprintQrCode.setImageBitmap(scaled);
                }
            });
        } catch (ProviderHelper.NotFoundException e) {
            Log.e(Constants.TAG, "key not found!", e);
            Notify.showNotify(this, R.string.error_key_not_found, Style.ERROR);
            finish();
        }
    }

    @Override
    protected void initLayout() {
        setContentView(R.layout.qr_code_activity);
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