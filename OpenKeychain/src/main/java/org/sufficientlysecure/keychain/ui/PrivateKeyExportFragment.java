/*
 * Copyright (C) 2017 Tobias Schülke
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
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.ImageView;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.network.KeyExportSocket;
import org.sufficientlysecure.keychain.operations.results.ExportResult;
import org.sufficientlysecure.keychain.provider.TemporaryFileProvider;
import org.sufficientlysecure.keychain.service.BackupKeyringParcel;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.ui.base.CryptoOperationFragment;
import org.sufficientlysecure.keychain.ui.util.QrCodeUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static android.app.Activity.RESULT_OK;

public class PrivateKeyExportFragment
        extends CryptoOperationFragment<BackupKeyringParcel, ExportResult>
        implements KeyExportSocket.ExportKeyListener {

    public static final String ARG_MASTER_KEY_IDS = "master_key_ids";
    private static final String ARG_CONNECTION_DETAILS = "connection_details";

    private static final int REQUEST_CONNECTION = 23;

    private ImageView mQrCode;

    private Activity mActivity;
    private String mConnectionDetails;
    private long mMasterKeyId;
    private Uri mCachedUri;

    private KeyExportSocket mSocket;

    public static PrivateKeyExportFragment newInstance(long masterKeyId) {
        PrivateKeyExportFragment frag = new PrivateKeyExportFragment();

        Bundle args = new Bundle();
        args.putLong(ARG_MASTER_KEY_IDS, masterKeyId);
        frag.setArguments(args);

        return frag;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        mActivity = (Activity) context;
        mActivity.setTitle(R.string.title_export_private_key);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSocket = KeyExportSocket.getInstance(this);

        if (savedInstanceState != null) {
            mConnectionDetails = savedInstanceState.getString(ARG_CONNECTION_DETAILS);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(ARG_CONNECTION_DETAILS, mConnectionDetails);
    }

    @Override
    public void onStop() {
        super.onStop();

        if (mActivity.isFinishing()) {
            mSocket.close(false);
        }
    }

    @Override
    public View onCreateView(LayoutInflater i, ViewGroup c, Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext())
                .inflate(R.layout.private_key_export_fragment,c, false);

        Bundle args = getArguments();
        mMasterKeyId = args.getLong(ARG_MASTER_KEY_IDS);

        mQrCode = (ImageView) view.findViewById(R.id.private_key_export_qr_image);
        Button helpButton = (Button) view.findViewById(R.id.private_key_export_help_button);

        mQrCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showQrCodeDialog();
            }
        });

        helpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HelpActivity.startHelpActivity(mActivity,
                        HelpActivity.TAB_FAQ, R.string.help_tab_faq_headline_transfer_key);
            }
        });

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (savedInstanceState != null) {
            loadQrCode(mConnectionDetails);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CONNECTION) {
            KeyExportSocket.setListener(this);
            if (resultCode == RESULT_OK) {
                loadKey();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void showQrCodeDialog() {
        Intent qrCodeIntent = new Intent(mActivity, QrCodeViewActivity.class);

        // create the transition animation - the images in the layouts
        // of both activities are defined with android:transitionName="qr_code"
        Bundle opts = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ActivityOptions options = ActivityOptions
                    .makeSceneTransitionAnimation(mActivity, mQrCode, "qr_code");
            opts = options.toBundle();
        }

        qrCodeIntent.putExtra(QrCodeViewActivity.EXTRA_QR_CODE_CONTENT, mConnectionDetails);
        qrCodeIntent.putExtra(QrCodeViewActivity.EXTRA_TITLE_RES_ID,
                R.string.title_export_private_key);
        qrCodeIntent.putExtra(QrCodeViewActivity.EXTRA_EXPORT_PRIVATE_KEY, true);
        qrCodeIntent.putExtra(QrCodeViewActivity.EXTRA_WHITE_TOOLBAR, true);
        startActivityForResult(qrCodeIntent, REQUEST_CONNECTION, opts);
    }

    /**
     * Load QR Code asynchronously and with a fade in animation
     */
    private void loadQrCode(String connectionDetails) {
        mConnectionDetails = connectionDetails;
        mQrCode.setImageBitmap(null);

        AsyncTask<Void, Void, Bitmap> loadTask =
                new AsyncTask<Void, Void, Bitmap>() {
                    protected Bitmap doInBackground(Void... unused) {
                        // render with minimal size
                        return QrCodeUtils.getQRCodeBitmap(mConnectionDetails, 0);
                    }

                    protected void onPostExecute(Bitmap qrCode) {
                        // scale the image up to our actual size. we do this in code rather
                        // than let the ImageView do this because we don't require filtering.
                        Bitmap scaled = Bitmap.createScaledBitmap(qrCode,
                                mQrCode.getHeight(), mQrCode.getHeight(),
                                false);
                        mQrCode.setImageBitmap(scaled);

                        // simple fade-in animation
                        AlphaAnimation anim = new AlphaAnimation(0.0f, 1.0f);
                        anim.setDuration(200);
                        mQrCode.startAnimation(anim);
                    }
                };

        loadTask.execute();
    }

    private void createExport() {
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String filename = Constants.FILE_ENCRYPTED_BACKUP_PREFIX + date
                + Constants.FILE_EXTENSION_ENCRYPTED_BACKUP_SECRET;

        if (mCachedUri == null) {
            mCachedUri = TemporaryFileProvider.createFile(mActivity, filename,
                    Constants.MIME_TYPE_ENCRYPTED_ALTERNATE);

            cryptoOperation(new CryptoInputParcel());
            return;
        }

        mSocket.writeKey(mActivity, mCachedUri);
    }

    @Nullable
    @Override
    public BackupKeyringParcel createOperationInput() {
        return new BackupKeyringParcel(new long[] {mMasterKeyId}, true, false, false, mCachedUri);
    }

    @Override
    public void onCryptoOperationSuccess(ExportResult result) {
        createExport();
    }

    @Override
    public void onCryptoOperationError(ExportResult result) {
        result.createNotify(getActivity()).show();
        mCachedUri = null;
    }

    @Override
    public void onCryptoOperationCancelled() {
        mCachedUri = null;
    }

    @Override
    public void showConnectionDetails(String connectionDetails) {
        loadQrCode(connectionDetails);
    }

    @Override
    public void loadKey() {
        createExport();
    }

    @Override
    public void keyExported() {
        mActivity.finish();
    }
}
