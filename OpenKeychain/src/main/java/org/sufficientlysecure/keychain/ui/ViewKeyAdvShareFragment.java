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

import android.app.ActivityOptions;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.compatibility.ClipboardReflection;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.pgp.exception.PgpKeyNotFoundException;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.provider.KeychainContract.Keys;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.provider.TemporaryStorageProvider;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.ui.util.QrCodeUtils;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.NfcHelper;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.io.FileNotFoundException;


public class ViewKeyAdvShareFragment extends LoaderFragment implements
        LoaderManager.LoaderCallbacks<Cursor> {

    public static final String ARG_DATA_URI = "uri";

    private TextView mFingerprint;
    private ImageView mQrCode;
    private CardView mQrCodeLayout;
    private View mFingerprintShareButton;
    private View mFingerprintClipboardButton;
    private View mKeyShareButton;
    private View mKeyClipboardButton;
    private View mKeyNfcButton;
    private ImageButton mKeySafeSlingerButton;
    private View mKeyUploadButton;

    ProviderHelper mProviderHelper;
    NfcHelper mNfcHelper;

    private static final int LOADER_ID_UNIFIED = 0;

    private Uri mDataUri;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup superContainer, Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, superContainer, savedInstanceState);
        View view = inflater.inflate(R.layout.view_key_adv_share_fragment, getContainer());

        mProviderHelper = new ProviderHelper(ViewKeyAdvShareFragment.this.getActivity());
        mNfcHelper = new NfcHelper(getActivity(), mProviderHelper);

        mFingerprint = (TextView) view.findViewById(R.id.view_key_fingerprint);
        mQrCode = (ImageView) view.findViewById(R.id.view_key_qr_code);
        mQrCodeLayout = (CardView) view.findViewById(R.id.view_key_qr_code_layout);
        mFingerprintShareButton = view.findViewById(R.id.view_key_action_fingerprint_share);
        mFingerprintClipboardButton = view.findViewById(R.id.view_key_action_fingerprint_clipboard);
        mKeyShareButton = view.findViewById(R.id.view_key_action_key_share);
        mKeyNfcButton = view.findViewById(R.id.view_key_action_key_nfc);
        mKeyClipboardButton = view.findViewById(R.id.view_key_action_key_clipboard);
        mKeySafeSlingerButton = (ImageButton) view.findViewById(R.id.view_key_action_key_safeslinger);
        mKeyUploadButton = view.findViewById(R.id.view_key_action_upload);

        mKeySafeSlingerButton.setColorFilter(getResources().getColor(R.color.tertiary_text_light),
                PorterDuff.Mode.SRC_IN);

        mQrCodeLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showQrCodeDialog();
            }
        });

        mFingerprintShareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                share(mDataUri, mProviderHelper, true, false);
            }
        });
        mFingerprintClipboardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                share(mDataUri, mProviderHelper, true, true);
            }
        });
        mKeyShareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                share(mDataUri, mProviderHelper, false, false);
            }
        });
        mKeyClipboardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                share(mDataUri, mProviderHelper, false, true);
            }
        });

        mKeyNfcButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mNfcHelper.invokeNfcBeam();
            }
        });

        mKeySafeSlingerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSafeSlinger(mDataUri);
            }
        });
        mKeyUploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadToKeyserver();
            }
        });

        return root;
    }

    private void startSafeSlinger(Uri dataUri) {
        long keyId = 0;
        try {
            keyId = new ProviderHelper(getActivity())
                    .getCachedPublicKeyRing(dataUri)
                    .extractOrGetMasterKeyId();
        } catch (PgpKeyNotFoundException e) {
            Log.e(Constants.TAG, "key not found!", e);
        }
        Intent safeSlingerIntent = new Intent(getActivity(), SafeSlingerActivity.class);
        safeSlingerIntent.putExtra(SafeSlingerActivity.EXTRA_MASTER_KEY_ID, keyId);
        startActivityForResult(safeSlingerIntent, 0);
    }

    private void share(Uri dataUri, ProviderHelper providerHelper, boolean fingerprintOnly,
                       boolean toClipboard) {
        try {
            String content;
            byte[] fingerprintData = (byte[]) providerHelper.getGenericData(
                    KeyRings.buildUnifiedKeyRingUri(dataUri),
                    Keys.FINGERPRINT, ProviderHelper.FIELD_TYPE_BLOB);
            if (fingerprintOnly) {
                String fingerprint = KeyFormattingUtils.convertFingerprintToHex(fingerprintData);
                if (!toClipboard) {
                    content = Constants.FINGERPRINT_SCHEME + ":" + fingerprint;
                } else {
                    content = fingerprint;
                }
            } else {
                Uri uri = KeychainContract.KeyRingData.buildPublicKeyRingUri(dataUri);
                // get public keyring as ascii armored string
                content = providerHelper.getKeyRingAsArmoredString(uri);
            }

            if (toClipboard) {
                ClipboardReflection.copyToClipboard(getActivity(), content);
                String message;
                if (fingerprintOnly) {
                    message = getResources().getString(R.string.fingerprint_copied_to_clipboard);
                } else {
                    message = getResources().getString(R.string.key_copied_to_clipboard);
                }
                Notify.create(getActivity(), message, Notify.Style.OK).show();
            } else {
                // Android will fail with android.os.TransactionTooLargeException if key is too big
                // see http://www.lonestarprod.com/?p=34
                if (content.length() >= 86389) {
                    Notify.create(getActivity(), R.string.key_too_big_for_sharing,
                            Notify.Style.ERROR).show();
                    return;
                }

                // let user choose application
                Intent sendIntent = new Intent(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, content);
                sendIntent.setType("text/plain");

                String title;
                if (fingerprintOnly) {
                    title = getResources().getString(R.string.title_share_fingerprint_with);
                } else {
                    title = getResources().getString(R.string.title_share_key);
                }
                Intent shareChooser = Intent.createChooser(sendIntent, title);

                // Bluetooth Share will convert text/plain sent via EXTRA_TEXT to HTML
                // Add replacement extra to send a text/plain file instead.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    try {
                        String primaryUserId = UncachedKeyRing.decodeFromData(content.getBytes()).
                                getPublicKey().getPrimaryUserIdWithFallback();

                        TemporaryStorageProvider shareFileProv = new TemporaryStorageProvider();
                        Uri contentUri = TemporaryStorageProvider.createFile(getActivity(),
                                primaryUserId + Constants.FILE_EXTENSION_ASC);

                        BufferedWriter contentWriter = new BufferedWriter(new OutputStreamWriter(
                                new ParcelFileDescriptor.AutoCloseOutputStream(
                                        shareFileProv.openFile(contentUri, "w"))));
                        contentWriter.write(content);
                        contentWriter.close();

                        // create replacement extras inside try{}:
                        // if file creation fails, just don't add the replacements
                        Bundle replacements = new Bundle();
                        shareChooser.putExtra(Intent.EXTRA_REPLACEMENT_EXTRAS, replacements);

                        Bundle bluetoothExtra = new Bundle(sendIntent.getExtras());
                        replacements.putBundle("com.android.bluetooth", bluetoothExtra);

                        bluetoothExtra.putParcelable(Intent.EXTRA_STREAM, contentUri);
                    } catch (FileNotFoundException e) {
                        Log.e(Constants.TAG, "error creating temporary Bluetooth key share file!", e);
                        Notify.create(getActivity(), R.string.error_bluetooth_file, Notify.Style.ERROR).show();
                    }
                }

                startActivity(shareChooser);
            }
        } catch (PgpGeneralException | IOException e) {
            Log.e(Constants.TAG, "error processing key!", e);
            Notify.create(getActivity(), R.string.error_key_processing, Notify.Style.ERROR).show();
        } catch (ProviderHelper.NotFoundException e) {
            Log.e(Constants.TAG, "key not found!", e);
            Notify.create(getActivity(), R.string.error_key_not_found, Notify.Style.ERROR).show();
        }
    }

    private void showQrCodeDialog() {
        Intent qrCodeIntent = new Intent(getActivity(), QrCodeViewActivity.class);

        // create the transition animation - the images in the layouts
        // of both activities are defined with android:transitionName="qr_code"
        Bundle opts = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ActivityOptions options = ActivityOptions
                    .makeSceneTransitionAnimation(getActivity(), mQrCodeLayout, "qr_code");
            opts = options.toBundle();
        }

        qrCodeIntent.setData(mDataUri);
        ActivityCompat.startActivity(getActivity(), qrCodeIntent, opts);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Uri dataUri = getArguments().getParcelable(ARG_DATA_URI);
        if (dataUri == null) {
            Log.e(Constants.TAG, "Data missing. Should be Uri of key!");
            getActivity().finish();
            return;
        }

        loadData(dataUri);
    }

    private void loadData(Uri dataUri) {
        mDataUri = dataUri;

        Log.i(Constants.TAG, "mDataUri: " + mDataUri.toString());

        // Prepare the loaders. Either re-connect with an existing ones,
        // or start new ones.
        getLoaderManager().initLoader(LOADER_ID_UNIFIED, null, this);

        // Prepare the NfcHelper
        mNfcHelper.initNfc(mDataUri);
    }

    static final String[] UNIFIED_PROJECTION = new String[] {
            KeyRings._ID, KeyRings.MASTER_KEY_ID, KeyRings.HAS_ANY_SECRET,
            KeyRings.USER_ID, KeyRings.FINGERPRINT,
            KeyRings.ALGORITHM, KeyRings.KEY_SIZE, KeyRings.CREATION, KeyRings.IS_EXPIRED,

    };
    static final int INDEX_UNIFIED_MASTER_KEY_ID = 1;
    static final int INDEX_UNIFIED_HAS_ANY_SECRET = 2;
    static final int INDEX_UNIFIED_USER_ID = 3;
    static final int INDEX_UNIFIED_FINGERPRINT = 4;
    static final int INDEX_UNIFIED_ALGORITHM = 5;
    static final int INDEX_UNIFIED_KEY_SIZE = 6;
    static final int INDEX_UNIFIED_CREATION = 7;
    static final int INDEX_UNIFIED_ID_EXPIRED = 8;

    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        setContentShown(false);
        switch (id) {
            case LOADER_ID_UNIFIED: {
                Uri baseUri = KeyRings.buildUnifiedKeyRingUri(mDataUri);
                return new CursorLoader(getActivity(), baseUri, UNIFIED_PROJECTION, null, null, null);
            }

            default:
                return null;
        }
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        /* TODO better error handling? May cause problems when a key is deleted,
         * because the notification triggers faster than the activity closes.
         */
        // Avoid NullPointerExceptions...
        if (data.getCount() == 0) {
            return;
        }
        // Swap the new cursor in. (The framework will take care of closing the
        // old cursor once we return.)
        switch (loader.getId()) {
            case LOADER_ID_UNIFIED: {
                if (data.moveToFirst()) {

                    byte[] fingerprintBlob = data.getBlob(INDEX_UNIFIED_FINGERPRINT);
                    String fingerprint = KeyFormattingUtils.convertFingerprintToHex(fingerprintBlob);
                    mFingerprint.setText(KeyFormattingUtils.colorizeFingerprint(fingerprint));

                    loadQrCode(fingerprint);

                    break;
                }
            }

        }
        setContentShown(true);
    }

    /**
     * This is called when the last Cursor provided to onLoadFinished() above is about to be closed.
     * We need to make sure we are no longer using it.
     */
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    /**
     * Load QR Code asynchronously and with a fade in animation
     *
     * @param fingerprint
     */
    private void loadQrCode(final String fingerprint) {
        AsyncTask<Void, Void, Bitmap> loadTask =
                new AsyncTask<Void, Void, Bitmap>() {
                    protected Bitmap doInBackground(Void... unused) {
                        Uri uri = new Uri.Builder()
                                .scheme(Constants.FINGERPRINT_SCHEME)
                                .opaquePart(fingerprint)
                                .build();
                        // render with minimal size
                        return QrCodeUtils.getQRCodeBitmap(uri, 0);
                    }

                    protected void onPostExecute(Bitmap qrCode) {
                        // only change view, if fragment is attached to activity
                        if (ViewKeyAdvShareFragment.this.isAdded()) {

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
                    }
                };

        loadTask.execute();
    }

    private void uploadToKeyserver() {
        Intent uploadIntent = new Intent(getActivity(), UploadKeyActivity.class);
        uploadIntent.setData(mDataUri);
        startActivityForResult(uploadIntent, 0);
    }


}
