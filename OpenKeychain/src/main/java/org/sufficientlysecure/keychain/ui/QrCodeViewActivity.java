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

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.widget.CardView;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.ImageView;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.network.KeyExportSocket;
import org.sufficientlysecure.keychain.pgp.exception.PgpKeyNotFoundException;
import org.sufficientlysecure.keychain.provider.KeyRepository;
import org.sufficientlysecure.keychain.ui.base.BaseActivity;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.ui.util.Notify.Style;
import org.sufficientlysecure.keychain.ui.util.QrCodeUtils;
import org.sufficientlysecure.keychain.util.Log;

public class QrCodeViewActivity extends BaseActivity implements KeyExportSocket.ExportKeyListener {
    public static String EXTRA_QR_CODE_CONTENT = "qr_code_content";
    public static String EXTRA_TITLE_RES_ID = "title_res_id";
    public static String EXTRA_EXPORT_PRIVATE_KEY = "export_private_key";
    public static String EXTRA_WHITE_TOOLBAR = "white_toolbar";

    private ImageView mQrCode;
    private CardView mQrCodeLayout;
    private boolean mWhiteToolbar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        String qrCodeContent = intent.getStringExtra(EXTRA_QR_CODE_CONTENT);
        int titleResId = intent.getIntExtra(EXTRA_TITLE_RES_ID, -1);
        boolean exportPrivateKey = intent.getBooleanExtra(EXTRA_EXPORT_PRIVATE_KEY, false);
        mWhiteToolbar = intent.getBooleanExtra(EXTRA_WHITE_TOOLBAR, false);

        Uri dataUri = getIntent().getData();
        if (dataUri == null && qrCodeContent == null) {
            Log.e(Constants.TAG, "Data missing. Should be Uri of key!");
            ActivityCompat.finishAfterTransition(QrCodeViewActivity.this);
            return;
        }

        initView();

        if (titleResId > 0) {
            setTitle(titleResId);
        }

        if (exportPrivateKey) {
            KeyExportSocket.setListener(this);
        }

        mQrCode = (ImageView) findViewById(R.id.qr_code_image);
        mQrCodeLayout = (CardView) findViewById(R.id.qr_code_image_layout);

        mQrCodeLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityCompat.finishAfterTransition(QrCodeViewActivity.this);
            }
        });

        KeyRepository keyRepository = KeyRepository.createDatabaseInteractor(this);
        try {
            final Bitmap qrCode;

            if (dataUri != null) {
                byte[] blob = keyRepository.getCachedPublicKeyRing(dataUri).getFingerprint();
                if (blob == null) {
                    Log.e(Constants.TAG, "key not found!");
                    Notify.create(this, R.string.error_key_not_found, Style.ERROR).show();
                    ActivityCompat.finishAfterTransition(QrCodeViewActivity.this);
                }

                Uri uri = new Uri.Builder()
                        .scheme(Constants.FINGERPRINT_SCHEME)
                        .opaquePart(KeyFormattingUtils.convertFingerprintToHex(blob))
                        .build();
                // create a minimal size qr code, we can keep this in ram no problem
                qrCode = QrCodeUtils.getQRCodeBitmap(uri, 0);
            } else {
                qrCode = QrCodeUtils.getQRCodeBitmap(qrCodeContent, 0);
            }

            mQrCode.getViewTreeObserver().addOnGlobalLayoutListener(
                    new OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            // create actual bitmap in display dimensions
                            Bitmap scaled = Bitmap.createScaledBitmap(qrCode,
                                    mQrCode.getWidth(), mQrCode.getWidth(), false);
                            mQrCode.setImageBitmap(scaled);
                        }
                    });
        } catch (PgpKeyNotFoundException e) {
            Log.e(Constants.TAG, "key not found!", e);
            Notify.create(this, R.string.error_key_not_found, Style.ERROR).show();
            ActivityCompat.finishAfterTransition(QrCodeViewActivity.this);
        }
    }

    private void initView() {
        setContentView(mWhiteToolbar ? R.layout.qr_code_white_activity : R.layout.qr_code_activity);
        initToolbar();

        // Inflate a "Done" custom action bar
        setFullScreenDialogClose(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // "Done"
                        ActivityCompat.finishAfterTransition(QrCodeViewActivity.this);
                    }
                }, !mWhiteToolbar
        );
    }

    @Override
    public void loadKey() {
        setResult(Activity.RESULT_OK);
        finish();
    }

    @Override
    public void showConnectionDetails(String connectionDetails) {
        // not used here
    }

    @Override
    public void keyExported() {
        // not used here
    }

}