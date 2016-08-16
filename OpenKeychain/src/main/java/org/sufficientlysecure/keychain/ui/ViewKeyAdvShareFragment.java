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


import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
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
import android.view.View.OnLayoutChangeListener;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import org.openintents.openpgp.util.OpenPgpUtils;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.KeyRing;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.pgp.exception.PgpKeyNotFoundException;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.provider.ProviderReader;
import org.sufficientlysecure.keychain.provider.TemporaryFileProvider;
import org.sufficientlysecure.keychain.ui.util.FormattingUtils;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.ui.util.Notify.Style;
import org.sufficientlysecure.keychain.ui.util.QrCodeUtils;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.NfcHelper;


public class ViewKeyAdvShareFragment extends LoaderFragment implements
        LoaderManager.LoaderCallbacks<Cursor> {

    public static final String ARG_DATA_URI = "uri";

    private ImageView mQrCode;
    private CardView mQrCodeLayout;
    private TextView mFingerprintView;

    NfcHelper mNfcHelper;

    private static final int LOADER_ID_UNIFIED = 0;

    private Uri mDataUri;

    private byte[] mFingerprint;
    private String mUserId;
    private Bitmap mQrCodeBitmapCache;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup superContainer, Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, superContainer, savedInstanceState);
        View view = inflater.inflate(R.layout.view_key_adv_share_fragment, getContainer());

        ProviderHelper providerHelper = new ProviderHelper(ViewKeyAdvShareFragment.this.getActivity());
        mNfcHelper = new NfcHelper(getActivity(), providerHelper);

        mFingerprintView = (TextView) view.findViewById(R.id.view_key_fingerprint);
        mQrCode = (ImageView) view.findViewById(R.id.view_key_qr_code);

        // We cache the QR code bitmap in its smallest possible size, then scale
        // it manually for the correct size whenever the layout of the ImageView
        // changes.  The fingerprint qr code loader which runs in the background
        // just calls requestLayout when it is finished, this way the loader and
        // background task are disconnected from any layouting the ImageView may
        // undergo. Please note how these six lines are perfectly right-aligned.
        mQrCode.addOnLayoutChangeListener(new OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop,
                    int oldRight,
                    int oldBottom) {
                // bitmap scaling is expensive, avoid doing it if we already have the correct size!
                int mCurrentWidth = 0, mCurrentHeight = 0;
                if (mQrCodeBitmapCache != null) {
                    if (mCurrentWidth == mQrCode.getWidth() && mCurrentHeight == mQrCode.getHeight()) {
                        return;
                    }
                    mCurrentWidth = mQrCode.getWidth();
                    mCurrentHeight = mQrCode.getHeight();
                    // scale the image up to our actual size. we do this in code rather
                    // than let the ImageView do this because we don't require filtering.
                    Bitmap scaled = Bitmap.createScaledBitmap(mQrCodeBitmapCache,
                            mCurrentWidth, mCurrentHeight, false);
                    mQrCode.setImageBitmap(scaled);
                }
            }
        });
        mQrCodeLayout = (CardView) view.findViewById(R.id.view_key_qr_code_layout);
        mQrCodeLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showQrCodeDialog();
            }
        });

        View vFingerprintShareButton = view.findViewById(R.id.view_key_action_fingerprint_share);
        View vFingerprintClipboardButton = view.findViewById(R.id.view_key_action_fingerprint_clipboard);
        View vKeyShareButton = view.findViewById(R.id.view_key_action_key_share);
        View vKeyNfcButton = view.findViewById(R.id.view_key_action_key_nfc);
        View vKeyClipboardButton = view.findViewById(R.id.view_key_action_key_clipboard);
        ImageButton vKeySafeSlingerButton = (ImageButton) view.findViewById(R.id.view_key_action_key_safeslinger);
        View vKeyUploadButton = view.findViewById(R.id.view_key_action_upload);
        vKeySafeSlingerButton.setColorFilter(FormattingUtils.getColorFromAttr(getActivity(), R.attr.colorTertiaryText),
                PorterDuff.Mode.SRC_IN);

        vFingerprintShareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareFingerprint(false);
            }
        });
        vFingerprintClipboardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareFingerprint(true);
            }
        });
        vKeyShareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareKey(false);
            }
        });
        vKeyClipboardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareKey(true);
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            vKeyNfcButton.setVisibility(View.VISIBLE);
            vKeyNfcButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mNfcHelper.invokeNfcBeam();
                }
            });
        } else {
            vKeyNfcButton.setVisibility(View.GONE);
        }

        vKeySafeSlingerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSafeSlinger(mDataUri);
            }
        });
        vKeyUploadButton.setOnClickListener(new View.OnClickListener() {
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
                    .read().getCachedPublicKeyRing(dataUri)
                    .extractOrGetMasterKeyId();
        } catch (PgpKeyNotFoundException e) {
            Log.e(Constants.TAG, "key not found!", e);
        }
        Intent safeSlingerIntent = new Intent(getActivity(), SafeSlingerActivity.class);
        safeSlingerIntent.putExtra(SafeSlingerActivity.EXTRA_MASTER_KEY_ID, keyId);
        startActivityForResult(safeSlingerIntent, 0);
    }

    private void shareKey(boolean toClipboard) {
        Activity activity = getActivity();
        if (activity == null || mFingerprint == null) {
            return;
        }
        ProviderHelper providerHelper = new ProviderHelper(activity);

        try {
            String content = providerHelper.write().getKeyRingAsArmoredString(
                    KeychainContract.KeyRingData.buildPublicKeyRingUri(mDataUri));

            if (toClipboard) {
                ClipboardManager clipMan = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipMan == null) {
                    Notify.create(activity, R.string.error_clipboard_copy, Style.ERROR);
                    return;
                }

                ClipData clip = ClipData.newPlainText(Constants.CLIPBOARD_LABEL, content);
                clipMan.setPrimaryClip(clip);

                Notify.create(activity, R.string.key_copied_to_clipboard, Notify.Style.OK).show();
                return;
            }

            // let user choose application
            Intent sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.setType(Constants.MIME_TYPE_KEYS);

            // NOTE: Don't use Intent.EXTRA_TEXT to send the key
            // better send it via a Uri!
            // example: Bluetooth Share will convert text/plain sent via Intent.EXTRA_TEXT to HTML
            try {
                TemporaryFileProvider shareFileProv = new TemporaryFileProvider();

                String filename = KeyFormattingUtils.convertFingerprintToHex(mFingerprint);
                OpenPgpUtils.UserId mainUserId = KeyRing.splitUserId(mUserId);
                if (mainUserId.name != null) {
                    filename = mainUserId.name;
                }
                Uri contentUri = TemporaryFileProvider.createFile(activity, filename + Constants.FILE_EXTENSION_ASC);

                BufferedWriter contentWriter = new BufferedWriter(new OutputStreamWriter(
                        new ParcelFileDescriptor.AutoCloseOutputStream(
                                shareFileProv.openFile(contentUri, "w"))));
                contentWriter.write(content);
                contentWriter.close();

                sendIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            } catch (FileNotFoundException e) {
                Log.e(Constants.TAG, "Error creating temporary key share file!", e);
                // no need for a snackbar because one sharing option doesn't work
                // Notify.create(getActivity(), R.string.error_temp_file, Notify.Style.ERROR).show();
            }

            String title = getString(R.string.title_share_key);
            Intent shareChooser = Intent.createChooser(sendIntent, title);

            startActivity(shareChooser);
        } catch (PgpGeneralException | IOException e) {
            Log.e(Constants.TAG, "error processing key!", e);
            Notify.create(activity, R.string.error_key_processing, Notify.Style.ERROR).show();
        } catch (ProviderReader.NotFoundException e) {
            Log.e(Constants.TAG, "key not found!", e);
            Notify.create(activity, R.string.error_key_not_found, Notify.Style.ERROR).show();
        }
    }

    private void shareFingerprint(boolean toClipboard) {
        Activity activity = getActivity();
        if (activity == null || mFingerprint == null) {
            return;
        }

        String content;
        String fingerprint = KeyFormattingUtils.convertFingerprintToHex(mFingerprint);
        if (!toClipboard) {
            content = Constants.FINGERPRINT_SCHEME + ":" + fingerprint;
        } else {
            content = fingerprint;
        }

        if (toClipboard) {
            ClipboardManager clipMan = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipMan == null) {
                Notify.create(activity, R.string.error_clipboard_copy, Style.ERROR);
                return;
            }

            ClipData clip = ClipData.newPlainText(Constants.CLIPBOARD_LABEL, content);
            clipMan.setPrimaryClip(clip);

            Notify.create(activity, R.string.fingerprint_copied_to_clipboard, Notify.Style.OK).show();
            return;
        }

        // let user choose application
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, content);
        sendIntent.setType("text/plain");

        String title = getString(R.string.title_share_fingerprint_with);
        Intent shareChooser = Intent.createChooser(sendIntent, title);

        startActivity(shareChooser);
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
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

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

        // Prepare the loaders. Either re-connect with an existing ones,
        // or start new ones.
        getLoaderManager().initLoader(LOADER_ID_UNIFIED, null, this);

        // Prepare the NfcHelper
        mNfcHelper.initNfc(mDataUri);
    }

    static final String[] UNIFIED_PROJECTION = new String[]{
            KeyRings._ID, KeyRings.FINGERPRINT, KeyRings.USER_ID
    };

    static final int INDEX_UNIFIED_FINGERPRINT = 1;
    static final int INDEX_UNIFIED_USER_ID = 2;

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
        // Avoid NullPointerExceptions...
        if (data == null || data.getCount() == 0) {
            return;
        }
        // Swap the new cursor in. (The framework will take care of closing the
        // old cursor once we return.)
        switch (loader.getId()) {
            case LOADER_ID_UNIFIED: {
                if (data.moveToFirst()) {

                    byte[] fingerprintBlob = data.getBlob(INDEX_UNIFIED_FINGERPRINT);
                    setFingerprint(fingerprintBlob);

                    mUserId = data.getString(INDEX_UNIFIED_USER_ID);

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
        mFingerprint = null;
        mQrCodeBitmapCache = null;
    }

    /**
     * Load QR Code asynchronously and with a fade in animation
     */
    private void setFingerprint(byte[] fingerprintBlob) {
        mFingerprint = fingerprintBlob;

        final String fingerprint = KeyFormattingUtils.convertFingerprintToHex(fingerprintBlob);
        mFingerprintView.setText(KeyFormattingUtils.colorizeFingerprint(fingerprint));

        if (mQrCodeBitmapCache != null) {
            return;
        }

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
                        // cache for later, and if we are attached request re-layout
                        mQrCodeBitmapCache = qrCode;

                        if (ViewKeyAdvShareFragment.this.isAdded()) {
                            mQrCode.requestLayout();

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
        long keyId;
        try {
            keyId = new ProviderHelper(getActivity())
                    .read().getCachedPublicKeyRing(mDataUri)
                    .extractOrGetMasterKeyId();
        } catch (PgpKeyNotFoundException e) {
            Log.e(Constants.TAG, "key not found!", e);
            Notify.create(getActivity(), "key not found", Style.ERROR).show();
            return;
        }
        Intent uploadIntent = new Intent(getActivity(), UploadKeyActivity.class);
        uploadIntent.setData(mDataUri);
        uploadIntent.putExtra(MultiUserIdsFragment.EXTRA_KEY_IDS, new long[]{keyId});
        startActivityForResult(uploadIntent, 0);
    }


}
