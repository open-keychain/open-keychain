/*
 * Copyright (C) 2013-2014 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2013 Bahtiar 'kalkin' Gadimov
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
import android.app.ActivityOptions;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.CardView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.getbase.floatingactionbutton.FloatingActionButton;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.pgp.KeyRing;
import org.sufficientlysecure.keychain.pgp.exception.PgpKeyNotFoundException;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.service.KeychainIntentServiceHandler;
import org.sufficientlysecure.keychain.ui.util.FormattingUtils;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.ui.util.QrCodeUtils;
import org.sufficientlysecure.keychain.ui.widget.AspectRatioImageView;
import org.sufficientlysecure.keychain.util.ContactHelper;
import org.sufficientlysecure.keychain.util.ExportHelper;
import org.sufficientlysecure.keychain.util.Log;

import java.util.Date;
import java.util.HashMap;

public class ViewKeyActivity extends BaseActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    ExportHelper mExportHelper;
    ProviderHelper mProviderHelper;

    protected Uri mDataUri;

    private TextView mName;
    private TextView mStatusText;
    private ImageView mStatusImage;
    private RelativeLayout mBigToolbar;

    private ImageButton mActionEncryptFile;
    private ImageButton mActionEncryptText;
    private ImageButton mActionVerify;
    private ImageButton mActionNfc;
    private FloatingActionButton mFab;
    private AspectRatioImageView mPhoto;
    private ImageButton mQrCode;
    private CardView mQrCodeLayout;

    // NFC
    private NfcAdapter mNfcAdapter;
    private NfcAdapter.CreateNdefMessageCallback mNdefCallback;
    private NfcAdapter.OnNdefPushCompleteCallback mNdefCompleteCallback;
    private byte[] mNfcKeyringBytes;
    private static final int NFC_SENT = 1;

    private static final int LOADER_ID_UNIFIED = 0;

    private boolean mIsSecret = false;
    private boolean mHasEncrypt = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mExportHelper = new ExportHelper(this);
        mProviderHelper = new ProviderHelper(this);

        setTitle(null);

        mName = (TextView) findViewById(R.id.view_key_name);
        mStatusText = (TextView) findViewById(R.id.view_key_status);
        mStatusImage = (ImageView) findViewById(R.id.view_key_status_image);
        mBigToolbar = (RelativeLayout) findViewById(R.id.toolbar_big);

        mActionEncryptFile = (ImageButton) findViewById(R.id.view_key_action_encrypt_files);
        mActionEncryptText = (ImageButton) findViewById(R.id.view_key_action_encrypt_text);
        mActionVerify = (ImageButton) findViewById(R.id.view_key_action_verify);
        mActionNfc = (ImageButton) findViewById(R.id.view_key_action_nfc);
        mFab = (FloatingActionButton) findViewById(R.id.fab);
        mPhoto = (AspectRatioImageView) findViewById(R.id.view_key_photo);
        mQrCode = (ImageButton) findViewById(R.id.view_key_qr_code);
        mQrCodeLayout = (CardView) findViewById(R.id.view_key_qr_code_layout);

        mDataUri = getIntent().getData();
        if (mDataUri == null) {
            Log.e(Constants.TAG, "Data missing. Should be uri of key!");
            finish();
            return;
        }
        if (mDataUri.getHost().equals(ContactsContract.AUTHORITY)) {
            mDataUri = ContactHelper.dataUriFromContactUri(this, mDataUri);
            if (mDataUri == null) {
                Log.e(Constants.TAG, "Contact Data missing. Should be uri of key!");
                Toast.makeText(this, R.string.error_contacts_key_id_missing, Toast.LENGTH_LONG).show();
                finish();
                return;
            }
        }

        Log.i(Constants.TAG, "mDataUri: " + mDataUri.toString());

        mActionEncryptFile.setOnClickListener(new View.OnClickListener() {
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
        mActionVerify.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                certify(mDataUri);
            }
        });

        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mIsSecret) {
                    startSafeSlinger(mDataUri);
                } else {
                    scanQrCode();
                }
            }
        });

        mQrCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showQrCodeDialog();
            }
        });

        mActionNfc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                invokeNfcBeam();
            }
        });


        // Prepare the loaders. Either re-connect with an existing ones,
        // or start new ones.
        getSupportLoaderManager().initLoader(LOADER_ID_UNIFIED, null, this);

        initNfc(mDataUri);

        startFragment(savedInstanceState, mDataUri);
    }

    @Override
    protected void initLayout() {
        setContentView(R.layout.view_key_activity);
    }

    private void startFragment(Bundle savedInstanceState, Uri dataUri) {
        // However, if we're being restored from a previous state,
        // then we don't need to do anything and should return or else
        // we could end up with overlapping fragments.
        if (savedInstanceState != null) {
            return;
        }

        // Create an instance of the fragment
        ViewKeyFragment frag = ViewKeyFragment.newInstance(dataUri);

        // Add the fragment to the 'fragment_container' FrameLayout
        // NOTE: We use commitAllowingStateLoss() to prevent weird crashes!
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.view_key_fragment, frag)
                .commitAllowingStateLoss();
        // do it immediately!
        getSupportFragmentManager().executePendingTransactions();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.key_view, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        try {
            switch (item.getItemId()) {
                case android.R.id.home: {
                    Intent homeIntent = new Intent(this, MainActivity.class);
                    homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(homeIntent);
                    return true;
                }
                case R.id.menu_key_view_export_file: {
                    exportToFile(mDataUri, mExportHelper, mProviderHelper);
                    return true;
                }
                case R.id.menu_key_view_delete: {
                    deleteKey(mDataUri, mExportHelper);
                    return true;
                }
                case R.id.menu_key_view_advanced: {
                    Intent advancedIntent = new Intent(this, ViewKeyAdvActivity.class);
                    advancedIntent.setData(mDataUri);
                    startActivity(advancedIntent);
                    return true;
                }
                case R.id.menu_key_view_refresh: {
                    try {
                        updateFromKeyserver(mDataUri, mProviderHelper);
                    } catch (ProviderHelper.NotFoundException e) {
                        Notify.showNotify(this, R.string.error_key_not_found, Notify.Style.ERROR);
                    }
                    return true;
                }
                case R.id.menu_key_view_edit: {
                    editKey(mDataUri);
                    return true;
                }
            }
        } catch (ProviderHelper.NotFoundException e) {
            Notify.showNotify(this, R.string.error_key_not_found, Notify.Style.ERROR);
            Log.e(Constants.TAG, "Key not found", e);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem register = menu.findItem(R.id.menu_key_view_edit);
        register.setVisible(mIsSecret);
        return true;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void invokeNfcBeam() {
        // Check for available NFC Adapter
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter == null || !mNfcAdapter.isEnabled()) {
            Notify.createNotify(this, R.string.error_nfc_needed, Notify.LENGTH_LONG, Notify.Style.ERROR, new Notify.ActionListener() {
                @Override
                public void onAction() {
                    Intent intentSettings = new Intent(Settings.ACTION_NFC_SETTINGS);
                    startActivity(intentSettings);
                }
            }, R.string.menu_nfc_preferences).show();

            return;
        }

        if (!mNfcAdapter.isNdefPushEnabled()) {
            Notify.createNotify(this, R.string.error_beam_needed, Notify.LENGTH_LONG, Notify.Style.ERROR, new Notify.ActionListener() {
                @Override
                public void onAction() {
                    Intent intentSettings = new Intent(Settings.ACTION_NFCSHARING_SETTINGS);
                    startActivity(intentSettings);
                }
            }, R.string.menu_beam_preferences).show();

            return;
        }

        mNfcAdapter.invokeBeam(this);
    }

    private void scanQrCode() {
        Intent scanQrCode = new Intent(this, QrCodeScanActivity.class);
        scanQrCode.setAction(QrCodeScanActivity.ACTION_SCAN_WITH_RESULT);
        startActivityForResult(scanQrCode, 0);
    }

    private void showQrCodeDialog() {
        Intent qrCodeIntent = new Intent(this, QrCodeViewActivity.class);

        // create the transition animation - the images in the layouts
        // of both activities are defined with android:transitionName="qr_code"
        Bundle opts = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ActivityOptions options = ActivityOptions
                    .makeSceneTransitionAnimation(this, mQrCodeLayout, "qr_code");
            opts = options.toBundle();
        }

        qrCodeIntent.setData(mDataUri);
        ActivityCompat.startActivity(this, qrCodeIntent, opts);
    }

    private void exportToFile(Uri dataUri, ExportHelper exportHelper, ProviderHelper providerHelper)
            throws ProviderHelper.NotFoundException {
        Uri baseUri = KeychainContract.KeyRings.buildUnifiedKeyRingUri(dataUri);

        HashMap<String, Object> data = providerHelper.getGenericData(
                baseUri,
                new String[]{KeychainContract.Keys.MASTER_KEY_ID, KeychainContract.KeyRings.HAS_SECRET},
                new int[]{ProviderHelper.FIELD_TYPE_INTEGER, ProviderHelper.FIELD_TYPE_INTEGER});

        exportHelper.showExportKeysDialog(
                new long[]{(Long) data.get(KeychainContract.KeyRings.MASTER_KEY_ID)},
                Constants.Path.APP_DIR_FILE, ((Long) data.get(KeychainContract.KeyRings.HAS_SECRET) != 0)
        );
    }

    private void deleteKey(Uri dataUri, ExportHelper exportHelper) {
        // Message is received after key is deleted
        Handler returnHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                if (message.arg1 == KeychainIntentServiceHandler.MESSAGE_OKAY) {
                    setResult(RESULT_CANCELED);
                    finish();
                }
            }
        };

        exportHelper.deleteKey(dataUri, returnHandler);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // if a result has been returned, display a notify
        if (data != null && data.hasExtra(OperationResult.EXTRA_RESULT)) {
            OperationResult result = data.getParcelableExtra(OperationResult.EXTRA_RESULT);
            result.createNotify(this).show();
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void encrypt(Uri dataUri, boolean text) {
        // If there is no encryption key, don't bother.
        if (!mHasEncrypt) {
            Notify.showNotify(this, R.string.error_no_encrypt_subkey, Notify.Style.ERROR);
            return;
        }
        try {
            long keyId = new ProviderHelper(this)
                    .getCachedPublicKeyRing(dataUri)
                    .extractOrGetMasterKeyId();
            long[] encryptionKeyIds = new long[]{keyId};
            Intent intent;
            if (text) {
                intent = new Intent(this, EncryptTextActivity.class);
                intent.setAction(EncryptTextActivity.ACTION_ENCRYPT_TEXT);
                intent.putExtra(EncryptTextActivity.EXTRA_ENCRYPTION_KEY_IDS, encryptionKeyIds);
            } else {
                intent = new Intent(this, EncryptFilesActivity.class);
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
            throws ProviderHelper.NotFoundException {
        byte[] blob = (byte[]) providerHelper.getGenericData(
                KeychainContract.KeyRings.buildUnifiedKeyRingUri(dataUri),
                KeychainContract.Keys.FINGERPRINT, ProviderHelper.FIELD_TYPE_BLOB);
        String fingerprint = KeyFormattingUtils.convertFingerprintToHex(blob);

        Intent queryIntent = new Intent(this, ImportKeysActivity.class);
        queryIntent.setAction(ImportKeysActivity.ACTION_IMPORT_KEY_FROM_KEYSERVER_AND_RETURN_RESULT);
        queryIntent.putExtra(ImportKeysActivity.EXTRA_FINGERPRINT, fingerprint);

        startActivityForResult(queryIntent, 0);
    }

    private void certify(Uri dataUri) {
        long keyId = 0;
        try {
            keyId = new ProviderHelper(this)
                    .getCachedPublicKeyRing(dataUri)
                    .extractOrGetMasterKeyId();
        } catch (PgpKeyNotFoundException e) {
            Log.e(Constants.TAG, "key not found!", e);
        }
        Intent certifyIntent = new Intent(this, CertifyKeyActivity.class);
        certifyIntent.putExtra(CertifyKeyActivity.EXTRA_KEY_IDS, new long[]{keyId});
        startActivityForResult(certifyIntent, 0);
    }

    private void editKey(Uri dataUri) {
        Intent editIntent = new Intent(this, EditKeyActivity.class);
        editIntent.setData(KeychainContract.KeyRingData.buildSecretKeyRingUri(dataUri));
        startActivityForResult(editIntent, 0);
    }

    private void startSafeSlinger(Uri dataUri) {
        long keyId = 0;
        try {
            keyId = new ProviderHelper(this)
                    .getCachedPublicKeyRing(dataUri)
                    .extractOrGetMasterKeyId();
        } catch (PgpKeyNotFoundException e) {
            Log.e(Constants.TAG, "key not found!", e);
        }
        Intent safeSlingerIntent = new Intent(this, SafeSlingerActivity.class);
        safeSlingerIntent.putExtra(SafeSlingerActivity.EXTRA_MASTER_KEY_ID, keyId);
        startActivityForResult(safeSlingerIntent, 0);
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

    /**
     * NFC: Initialize NFC sharing if OS and device supports it
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void initNfc(final Uri dataUri) {
        // check if NFC Beam is supported (>= Android 4.1)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {

            // Implementation for the CreateNdefMessageCallback interface
            mNdefCallback = new NfcAdapter.CreateNdefMessageCallback() {
                @Override
                public NdefMessage createNdefMessage(NfcEvent event) {
                    /*
                     * When a device receives a push with an AAR in it, the application specified in the AAR is
                     * guaranteed to run. The AAR overrides the tag dispatch system. You can add it back in to
                     * guarantee that this activity starts when receiving a beamed message. For now, this code
                     * uses the tag dispatch system.
                     */
                    return new NdefMessage(NdefRecord.createMime(Constants.NFC_MIME,
                            mNfcKeyringBytes), NdefRecord.createApplicationRecord(Constants.PACKAGE_NAME));
                }
            };

            // Implementation for the OnNdefPushCompleteCallback interface
            mNdefCompleteCallback = new NfcAdapter.OnNdefPushCompleteCallback() {
                @Override
                public void onNdefPushComplete(NfcEvent event) {
                    // A handler is needed to send messages to the activity when this
                    // callback occurs, because it happens from a binder thread
                    mNfcHandler.obtainMessage(NFC_SENT).sendToTarget();
                }
            };

            // Check for available NFC Adapter
            mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
            if (mNfcAdapter != null) {
                /*
                 * Retrieve mNfcKeyringBytes here asynchronously (to not block the UI)
                 * and init nfc adapter afterwards.
                 * mNfcKeyringBytes can not be retrieved in createNdefMessage, because this process
                 * has no permissions to query the Uri.
                 */
                AsyncTask<Void, Void, Void> initTask =
                        new AsyncTask<Void, Void, Void>() {
                            protected Void doInBackground(Void... unused) {
                                try {
                                    Uri blobUri =
                                            KeychainContract.KeyRingData.buildPublicKeyRingUri(dataUri);
                                    mNfcKeyringBytes = (byte[]) mProviderHelper.getGenericData(
                                            blobUri,
                                            KeychainContract.KeyRingData.KEY_RING_DATA,
                                            ProviderHelper.FIELD_TYPE_BLOB);
                                } catch (ProviderHelper.NotFoundException e) {
                                    Log.e(Constants.TAG, "key not found!", e);
                                }

                                // no AsyncTask return (Void)
                                return null;
                            }

                            protected void onPostExecute(Void unused) {
                                // Register callback to set NDEF message
                                mNfcAdapter.setNdefPushMessageCallback(mNdefCallback,
                                        ViewKeyActivity.this);
                                // Register callback to listen for message-sent success
                                mNfcAdapter.setOnNdefPushCompleteCallback(mNdefCompleteCallback,
                                        ViewKeyActivity.this);
                            }
                        };

                initTask.execute();
            }
        }
    }

    /**
     * NFC: This handler receives a message from onNdefPushComplete
     */
    private final Handler mNfcHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case NFC_SENT:
                    Notify.showNotify(
                            ViewKeyActivity.this, R.string.nfc_successful, Notify.Style.INFO);
                    break;
            }
        }
    };

    // These are the rows that we will retrieve.
    static final String[] PROJECTION = new String[]{
            KeychainContract.KeyRings._ID,
            KeychainContract.KeyRings.MASTER_KEY_ID,
            KeychainContract.KeyRings.USER_ID,
            KeychainContract.KeyRings.IS_REVOKED,
            KeychainContract.KeyRings.EXPIRY,
            KeychainContract.KeyRings.VERIFIED,
            KeychainContract.KeyRings.HAS_ANY_SECRET,
            KeychainContract.KeyRings.FINGERPRINT,
            KeychainContract.KeyRings.HAS_ENCRYPT
    };

    static final int INDEX_MASTER_KEY_ID = 1;
    static final int INDEX_USER_ID = 2;
    static final int INDEX_IS_REVOKED = 3;
    static final int INDEX_EXPIRY = 4;
    static final int INDEX_VERIFIED = 5;
    static final int INDEX_HAS_ANY_SECRET = 6;
    static final int INDEX_FINGERPRINT = 7;
    static final int INDEX_HAS_ENCRYPT = 8;

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_ID_UNIFIED: {
                Uri baseUri = KeychainContract.KeyRings.buildUnifiedKeyRingUri(mDataUri);
                return new CursorLoader(this, baseUri, PROJECTION, null, null, null);
            }

            default:
                return null;
        }
    }

    @Override
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
                    // get name, email, and comment from USER_ID
                    String[] mainUserId = KeyRing.splitUserId(data.getString(INDEX_USER_ID));
                    if (mainUserId[0] != null) {
                        mName.setText(mainUserId[0]);
                    } else {
                        mName.setText(R.string.user_id_no_name);
                    }

                    String fingerprint = KeyFormattingUtils.convertFingerprintToHex(data.getBlob(INDEX_FINGERPRINT));

                    mIsSecret = data.getInt(INDEX_HAS_ANY_SECRET) != 0;
                    mHasEncrypt = data.getInt(INDEX_HAS_ENCRYPT) != 0;
                    boolean isRevoked = data.getInt(INDEX_IS_REVOKED) > 0;
                    boolean isExpired = !data.isNull(INDEX_EXPIRY)
                            && new Date(data.getLong(INDEX_EXPIRY) * 1000).before(new Date());
                    boolean isVerified = data.getInt(INDEX_VERIFIED) > 0;

                    AsyncTask<String, Void, Bitmap> photoTask =
                            new AsyncTask<String, Void, Bitmap>() {
                                protected Bitmap doInBackground(String... fingerprint) {
                                    return ContactHelper.photoFromFingerprint(getContentResolver(), fingerprint[0]);
                                }

                                protected void onPostExecute(Bitmap photo) {
                                    mPhoto.setImageBitmap(photo);
                                    mPhoto.setVisibility(View.VISIBLE);
                                }
                            };

                    // Note: order is important
                    int color;
                    if (isRevoked) {
                        mStatusText.setText(R.string.view_key_revoked);
                        mStatusImage.setVisibility(View.VISIBLE);
                        KeyFormattingUtils.setStatusImage(this, mStatusImage, mStatusText,
                                KeyFormattingUtils.STATE_REVOKED, R.color.icons, true);
                        color = getResources().getColor(R.color.android_red_light);

                        mActionEncryptFile.setVisibility(View.GONE);
                        mActionEncryptText.setVisibility(View.GONE);
                        mActionVerify.setVisibility(View.GONE);
                        mActionNfc.setVisibility(View.GONE);
                        mFab.setVisibility(View.GONE);
                        mQrCodeLayout.setVisibility(View.GONE);
                    } else if (isExpired) {
                        if (mIsSecret) {
                            mStatusText.setText(R.string.view_key_expired_secret);
                        } else {
                            mStatusText.setText(R.string.view_key_expired);
                        }
                        mStatusImage.setVisibility(View.VISIBLE);
                        KeyFormattingUtils.setStatusImage(this, mStatusImage, mStatusText,
                                KeyFormattingUtils.STATE_EXPIRED, R.color.icons, true);
                        color = getResources().getColor(R.color.android_red_light);

                        mActionEncryptFile.setVisibility(View.GONE);
                        mActionEncryptText.setVisibility(View.GONE);
                        mActionVerify.setVisibility(View.GONE);
                        mActionNfc.setVisibility(View.GONE);
                        mFab.setVisibility(View.GONE);
                        mQrCodeLayout.setVisibility(View.GONE);
                    } else if (mIsSecret) {
                        // re-create options menu to see edit button
                        supportInvalidateOptionsMenu();

                        mStatusText.setText(R.string.view_key_my_key);
                        mStatusImage.setVisibility(View.GONE);
                        color = getResources().getColor(R.color.primary);
                        photoTask.execute(fingerprint);
                        loadQrCode(fingerprint);
                        mQrCodeLayout.setVisibility(View.VISIBLE);

                        // and place leftOf qr code
                        RelativeLayout.LayoutParams nameParams = (RelativeLayout.LayoutParams)
                                mName.getLayoutParams();
                        // remove right margin
                        nameParams.setMargins(FormattingUtils.dpToPx(this, 48), 0, 0, 0);
                        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                            nameParams.setMarginEnd(0);
                        }
                        nameParams.addRule(RelativeLayout.LEFT_OF, R.id.view_key_qr_code_layout);
                        mName.setLayoutParams(nameParams);

                        RelativeLayout.LayoutParams statusParams = (RelativeLayout.LayoutParams)
                                mStatusText.getLayoutParams();
                        statusParams.setMargins(FormattingUtils.dpToPx(this, 48), 0, 0, 0);
                        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                            statusParams.setMarginEnd(0);
                        }
                        statusParams.addRule(RelativeLayout.LEFT_OF, R.id.view_key_qr_code_layout);
                        mStatusText.setLayoutParams(statusParams);

                        mActionEncryptFile.setVisibility(View.VISIBLE);
                        mActionEncryptText.setVisibility(View.VISIBLE);
                        mActionVerify.setVisibility(View.GONE);

                        // invokeBeam is available from API 21
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            mActionNfc.setVisibility(View.VISIBLE);
                        } else {
                            mActionNfc.setVisibility(View.GONE);
                        }
                        mFab.setVisibility(View.VISIBLE);
                        mFab.setIconDrawable(getResources().getDrawable(R.drawable.ic_repeat_white_24dp));
                    } else {
                        mActionEncryptFile.setVisibility(View.VISIBLE);
                        mActionEncryptText.setVisibility(View.VISIBLE);
                        mQrCodeLayout.setVisibility(View.GONE);
                        mActionNfc.setVisibility(View.GONE);

                        if (isVerified) {
                            mStatusText.setText(R.string.view_key_verified);
                            mStatusImage.setVisibility(View.VISIBLE);
                            KeyFormattingUtils.setStatusImage(this, mStatusImage, mStatusText,
                                    KeyFormattingUtils.STATE_VERIFIED, R.color.icons, true);
                            color = getResources().getColor(R.color.primary);
                            photoTask.execute(fingerprint);

                            mActionVerify.setVisibility(View.GONE);
                            mFab.setVisibility(View.GONE);
                        } else {
                            mStatusText.setText(R.string.view_key_unverified);
                            mStatusImage.setVisibility(View.VISIBLE);
                            KeyFormattingUtils.setStatusImage(this, mStatusImage, mStatusText,
                                    KeyFormattingUtils.STATE_UNVERIFIED, R.color.icons, true);
                            color = getResources().getColor(R.color.android_orange_light);

                            mActionVerify.setVisibility(View.VISIBLE);
                            mFab.setVisibility(View.VISIBLE);
                        }
                    }
                    mToolbar.setBackgroundColor(color);
                    mStatusBar.setBackgroundColor(color);
                    mBigToolbar.setBackgroundColor(color);

                    mStatusImage.setAlpha(80);

                    break;
                }
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }
}
