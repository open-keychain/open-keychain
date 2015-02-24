/*
 * Copyright (C) 2014 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2014 Vincent Breitmoser <v.breitmoser@mugenguild.com>
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

import android.annotation.TargetApi;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.compatibility.ClipboardReflection;
import org.sufficientlysecure.keychain.compatibility.DialogFragmentWorkaround;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.pgp.exception.PgpKeyNotFoundException;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.provider.KeychainContract.UserPackets;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.provider.ProviderHelper.NotFoundException;
import org.sufficientlysecure.keychain.ui.adapter.UserIdsAdapter;
import org.sufficientlysecure.keychain.ui.dialog.ShareNfcDialogFragment;
import org.sufficientlysecure.keychain.ui.dialog.UserIdInfoDialogFragment;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.ui.util.QrCodeUtils;
import org.sufficientlysecure.keychain.util.Log;

import java.io.IOException;
import java.util.Date;

public class ViewKeyFragment extends LoaderFragment implements
        LoaderManager.LoaderCallbacks<Cursor> {

    public static final String ARG_DATA_URI = "uri";

    private View mActionEdit;
    private View mActionEditDivider;
    private View mActionEncryptFiles;
    private View mActionEncryptText;
    private View mActionEncryptTextText;
    private View mActionCertify;
    private View mActionCertifyText;
    private ImageView mActionCertifyImage;
    private View mActionUpdate;

    private TextView mFingerprint;
    private ImageView mFingerprintQrCode;
    private View mFingerprintShareButton;
    private View mFingerprintClipboardButton;
    private View mKeyShareButton;
    private View mKeyClipboardButton;
    private ImageButton mKeySafeSlingerButton;
    private View mNfcHelpButton;
    private View mNfcPrefsButton;
    private View mKeyUploadButton;
    private ListView mUserIds;

    private static final int LOADER_ID_UNIFIED = 0;
    private static final int LOADER_ID_USER_IDS = 1;

    // conservative attitude
    private boolean mHasEncrypt = true;

    private UserIdsAdapter mUserIdsAdapter;

    private Uri mDataUri;

    ProviderHelper mProviderHelper;

    /**
     * Creates new instance of this fragment
     */
    public static ViewKeyFragment newInstance(Uri dataUri) {
        ViewKeyFragment frag = new ViewKeyFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_DATA_URI, dataUri);

        frag.setArguments(args);

        return frag;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup superContainer, Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, superContainer, savedInstanceState);
        View view = inflater.inflate(R.layout.view_key_fragment, getContainer());

        mProviderHelper = new ProviderHelper(getActivity());

        mUserIds = (ListView) view.findViewById(R.id.view_key_user_ids);
        mActionEdit = view.findViewById(R.id.view_key_action_edit);
        mActionEditDivider = view.findViewById(R.id.view_key_action_edit_divider);
        mActionEncryptText = view.findViewById(R.id.view_key_action_encrypt_text);
        mActionEncryptTextText = view.findViewById(R.id.view_key_action_encrypt_text_text);
        mActionEncryptFiles = view.findViewById(R.id.view_key_action_encrypt_files);
        mActionCertify = view.findViewById(R.id.view_key_action_certify);
        mActionCertifyText = view.findViewById(R.id.view_key_action_certify_text);
        mActionCertifyImage = (ImageView) view.findViewById(R.id.view_key_action_certify_image);
        // make certify image gray, like action icons
        mActionCertifyImage.setColorFilter(getResources().getColor(R.color.tertiary_text_light),
                PorterDuff.Mode.SRC_IN);
        mActionUpdate = view.findViewById(R.id.view_key_action_update);

        mUserIds.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                showUserIdInfo(position);
            }
        });

        mFingerprint = (TextView) view.findViewById(R.id.view_key_fingerprint);
        mFingerprintQrCode = (ImageView) view.findViewById(R.id.view_key_fingerprint_qr_code_image);
        mFingerprintShareButton = view.findViewById(R.id.view_key_action_fingerprint_share);
        mFingerprintClipboardButton = view.findViewById(R.id.view_key_action_fingerprint_clipboard);
        mKeyShareButton = view.findViewById(R.id.view_key_action_key_share);
        mKeyClipboardButton = view.findViewById(R.id.view_key_action_key_clipboard);
        mKeySafeSlingerButton = (ImageButton) view.findViewById(R.id.view_key_action_key_safeslinger);
        mNfcHelpButton = view.findViewById(R.id.view_key_action_nfc_help);
        mNfcPrefsButton = view.findViewById(R.id.view_key_action_nfc_prefs);
        mKeyUploadButton = view.findViewById(R.id.view_key_action_upload);

        mKeySafeSlingerButton.setColorFilter(getResources().getColor(R.color.tertiary_text_light),
                PorterDuff.Mode.SRC_IN);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            mNfcPrefsButton.setVisibility(View.VISIBLE);
        } else {
            mNfcPrefsButton.setVisibility(View.GONE);
        }

        mFingerprintQrCode.setOnClickListener(new View.OnClickListener() {
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
        mKeySafeSlingerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSafeSlinger(mDataUri);
            }
        });
        mNfcHelpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showNfcHelpDialog();
            }
        });
        mNfcPrefsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showNfcPrefs();
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
            if (fingerprintOnly) {
                byte[] data = (byte[]) providerHelper.getGenericData(
                        KeyRings.buildUnifiedKeyRingUri(dataUri),
                        KeychainContract.Keys.FINGERPRINT, ProviderHelper.FIELD_TYPE_BLOB);
                String fingerprint = KeyFormattingUtils.convertFingerprintToHex(data);
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
                Notify.showNotify(getActivity(), message, Notify.Style.OK);
            } else {
                // Android will fail with android.os.TransactionTooLargeException if key is too big
                // see http://www.lonestarprod.com/?p=34
                if (content.length() >= 86389) {
                    Notify.showNotify(getActivity(), R.string.key_too_big_for_sharing,
                            Notify.Style.ERROR);
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
                startActivity(Intent.createChooser(sendIntent, title));
            }
        } catch (PgpGeneralException | IOException e) {
            Log.e(Constants.TAG, "error processing key!", e);
            Notify.showNotify(getActivity(), R.string.error_key_processing, Notify.Style.ERROR);
        } catch (ProviderHelper.NotFoundException e) {
            Log.e(Constants.TAG, "key not found!", e);
            Notify.showNotify(getActivity(), R.string.error_key_not_found, Notify.Style.ERROR);
        }
    }

    private void showQrCodeDialog() {
        Intent qrCodeIntent = new Intent(getActivity(), QrCodeViewActivity.class);
        qrCodeIntent.setData(mDataUri);
        startActivity(qrCodeIntent);
    }

    private void showNfcHelpDialog() {
        ShareNfcDialogFragment dialog = ShareNfcDialogFragment.newInstance();
        dialog.show(getActivity().getSupportFragmentManager(), "shareNfcDialog");
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private void showNfcPrefs() {
        Intent intentSettings = new Intent(
                Settings.ACTION_NFCSHARING_SETTINGS);
        startActivity(intentSettings);
    }

    private void showUserIdInfo(final int position) {
        final boolean isRevoked = mUserIdsAdapter.getIsRevoked(position);
        final int isVerified = mUserIdsAdapter.getIsVerified(position);

        DialogFragmentWorkaround.INTERFACE.runnableRunDelayed(new Runnable() {
            public void run() {
                UserIdInfoDialogFragment dialogFragment =
                        UserIdInfoDialogFragment.newInstance(isRevoked, isVerified);

                dialogFragment.show(getActivity().getSupportFragmentManager(), "userIdInfoDialog");
            }
        });
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

        mActionEncryptFiles.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                encrypt(mDataUri, false);
            }
        });
        mActionEncryptText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                encrypt(mDataUri, true);
            }
        });
        mActionCertify.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                certify(mDataUri);
            }
        });
        mActionEdit.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                editKey(mDataUri);
            }
        });
        mActionUpdate.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                try {
                    updateFromKeyserver(mDataUri, new ProviderHelper(getActivity()));
                } catch (NotFoundException e) {
                    Notify.showNotify(getActivity(), R.string.error_key_not_found, Notify.Style.ERROR);
                }
            }
        });

        mUserIdsAdapter = new UserIdsAdapter(getActivity(), null, 0);
        mUserIds.setAdapter(mUserIdsAdapter);

        // Prepare the loaders. Either re-connect with an existing ones,
        // or start new ones.
        getLoaderManager().initLoader(LOADER_ID_UNIFIED, null, this);
        getLoaderManager().initLoader(LOADER_ID_USER_IDS, null, this);
    }

    static final String[] UNIFIED_PROJECTION = new String[]{
            KeyRings._ID, KeyRings.MASTER_KEY_ID, KeyRings.HAS_ANY_SECRET,
            KeyRings.USER_ID, KeyRings.FINGERPRINT,
            KeyRings.ALGORITHM, KeyRings.KEY_SIZE, KeyRings.CREATION, KeyRings.EXPIRY,
            KeyRings.IS_REVOKED, KeyRings.HAS_ENCRYPT,

    };
    static final int INDEX_UNIFIED_MASTER_KEY_ID = 1;
    static final int INDEX_UNIFIED_HAS_ANY_SECRET = 2;
    static final int INDEX_UNIFIED_USER_ID = 3;
    static final int INDEX_UNIFIED_FINGERPRINT = 4;
    static final int INDEX_UNIFIED_ALGORITHM = 5;
    static final int INDEX_UNIFIED_KEY_SIZE = 6;
    static final int INDEX_UNIFIED_CREATION = 7;
    static final int INDEX_UNIFIED_EXPIRY = 8;
    static final int INDEX_UNIFIED_IS_REVOKED = 9;
    static final int INDEX_UNIFIED_HAS_ENCRYPT = 10;

    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        setContentShown(false);

        switch (id) {
            case LOADER_ID_UNIFIED: {
                Uri baseUri = KeyRings.buildUnifiedKeyRingUri(mDataUri);
                return new CursorLoader(getActivity(), baseUri, UNIFIED_PROJECTION, null, null, null);
            }
            case LOADER_ID_USER_IDS: {
                Uri baseUri = UserPackets.buildUserIdsUri(mDataUri);
                return new CursorLoader(getActivity(), baseUri,
                        UserIdsAdapter.USER_IDS_PROJECTION, null, null, null);
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

                    if (data.getInt(INDEX_UNIFIED_HAS_ANY_SECRET) != 0) {
                        // edit button
                        mActionEdit.setVisibility(View.VISIBLE);
                        mActionEditDivider.setVisibility(View.VISIBLE);
                    } else {
                        // edit button
                        mActionEdit.setVisibility(View.GONE);
                        mActionEditDivider.setVisibility(View.GONE);
                    }

                    // If this key is revoked, it cannot be used for anything!
                    if (data.getInt(INDEX_UNIFIED_IS_REVOKED) != 0) {
                        mActionEdit.setEnabled(false);
                        mActionCertify.setEnabled(false);
                        mActionCertifyText.setEnabled(false);
                        mActionEncryptText.setEnabled(false);
                        mActionEncryptTextText.setEnabled(false);
                        mActionEncryptFiles.setEnabled(false);
                    } else {
                        mActionEdit.setEnabled(true);

                        Date expiryDate = new Date(data.getLong(INDEX_UNIFIED_EXPIRY) * 1000);
                        if (!data.isNull(INDEX_UNIFIED_EXPIRY) && expiryDate.before(new Date())) {
                            mActionCertify.setEnabled(false);
                            mActionCertifyText.setEnabled(false);
                            mActionEncryptText.setEnabled(false);
                            mActionEncryptTextText.setEnabled(false);
                            mActionEncryptFiles.setEnabled(false);
                        } else {
                            mActionCertify.setEnabled(true);
                            mActionCertifyText.setEnabled(true);
                            mActionEncryptText.setEnabled(true);
                            mActionEncryptTextText.setEnabled(true);
                            mActionEncryptFiles.setEnabled(true);
                        }
                    }

                    mHasEncrypt = data.getInt(INDEX_UNIFIED_HAS_ENCRYPT) != 0;

                    break;
                }
            }

            case LOADER_ID_USER_IDS:
                mUserIdsAdapter.swapCursor(data);
                break;

        }
        setContentShown(true);
    }

    /**
     * This is called when the last Cursor provided to onLoadFinished() above is about to be closed.
     * We need to make sure we are no longer using it.
     */
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
            case LOADER_ID_USER_IDS:
                mUserIdsAdapter.swapCursor(null);
                break;
        }
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
                        String qrCodeContent = Constants.FINGERPRINT_SCHEME + ":" + fingerprint;
                        // render with minimal size
                        return QrCodeUtils.getQRCodeBitmap(qrCodeContent, 0);
                    }

                    protected void onPostExecute(Bitmap qrCode) {
                        // only change view, if fragment is attached to activity
                        if (ViewKeyFragment.this.isAdded()) {

                            // scale the image up to our actual size. we do this in code rather
                            // than let the ImageView do this because we don't require filtering.
                            Bitmap scaled = Bitmap.createScaledBitmap(qrCode,
                                    mFingerprintQrCode.getHeight(), mFingerprintQrCode.getHeight(),
                                    false);
                            mFingerprintQrCode.setImageBitmap(scaled);

                            // simple fade-in animation
                            AlphaAnimation anim = new AlphaAnimation(0.0f, 1.0f);
                            anim.setDuration(200);
                            mFingerprintQrCode.startAnimation(anim);
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

    private void encrypt(Uri dataUri, boolean text) {
        // If there is no encryption key, don't bother.
        if (!mHasEncrypt) {
            Notify.showNotify(getActivity(), R.string.error_no_encrypt_subkey, Notify.Style.ERROR);
            return;
        }
        try {
            long keyId = new ProviderHelper(getActivity())
                    .getCachedPublicKeyRing(dataUri)
                    .extractOrGetMasterKeyId();
            long[] encryptionKeyIds = new long[]{keyId};
            Intent intent;
            if (text) {
                intent = new Intent(getActivity(), EncryptTextActivity.class);
                intent.setAction(EncryptTextActivity.ACTION_ENCRYPT_TEXT);
                intent.putExtra(EncryptTextActivity.EXTRA_ENCRYPTION_KEY_IDS, encryptionKeyIds);
            } else {
                intent = new Intent(getActivity(), EncryptFilesActivity.class);
                intent.setAction(EncryptFilesActivity.ACTION_ENCRYPT_DATA);
                intent.putExtra(EncryptFilesActivity.EXTRA_ENCRYPTION_KEY_IDS, encryptionKeyIds);
            }
            // used instead of startActivity set actionbar based on callingPackage
            startActivityForResult(intent, 0);
        } catch (PgpKeyNotFoundException e) {
            Log.e(Constants.TAG, "key not found!", e);
        }
    }

    private void updateFromKeyserver(Uri dataUri, ProviderHelper providerHelper)
            throws NotFoundException {
        byte[] blob = (byte[]) providerHelper.getGenericData(
                KeyRings.buildUnifiedKeyRingUri(dataUri),
                KeychainContract.Keys.FINGERPRINT, ProviderHelper.FIELD_TYPE_BLOB);
        String fingerprint = KeyFormattingUtils.convertFingerprintToHex(blob);

        Intent queryIntent = new Intent(getActivity(), ImportKeysActivity.class);
        queryIntent.setAction(ImportKeysActivity.ACTION_IMPORT_KEY_FROM_KEYSERVER_AND_RETURN_RESULT);
        queryIntent.putExtra(ImportKeysActivity.EXTRA_FINGERPRINT, fingerprint);

        startActivityForResult(queryIntent, 0);
    }

    private void certify(Uri dataUri) {
        long keyId = 0;
        try {
            keyId = new ProviderHelper(getActivity())
                    .getCachedPublicKeyRing(dataUri)
                    .extractOrGetMasterKeyId();
        } catch (PgpKeyNotFoundException e) {
            Log.e(Constants.TAG, "key not found!", e);
        }
        Intent certifyIntent = new Intent(getActivity(), CertifyKeyActivity.class);
        certifyIntent.putExtra(CertifyKeyActivity.EXTRA_KEY_IDS, new long[]{keyId});
        startActivityForResult(certifyIntent, 0);
    }

    private void editKey(Uri dataUri) {
        Intent editIntent = new Intent(getActivity(), EditKeyActivity.class);
        editIntent.setData(KeychainContract.KeyRingData.buildSecretKeyRingUri(dataUri));
        startActivityForResult(editIntent, 0);
    }

}
