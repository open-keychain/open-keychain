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

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.CardView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.getbase.floatingactionbutton.FloatingActionButton;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.keyimport.ParcelableKeyRing;
import org.sufficientlysecure.keychain.operations.results.CertifyResult;
import org.sufficientlysecure.keychain.operations.results.ImportKeyResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.pgp.KeyRing;
import org.sufficientlysecure.keychain.pgp.exception.PgpKeyNotFoundException;
import org.sufficientlysecure.keychain.provider.CachedPublicKeyRing;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.service.ImportKeyringParcel;
import org.sufficientlysecure.keychain.service.KeychainService;
import org.sufficientlysecure.keychain.service.ServiceProgressHandler;
import org.sufficientlysecure.keychain.service.ServiceProgressHandler.MessageStatus;
import org.sufficientlysecure.keychain.service.PassphraseCacheService;
import org.sufficientlysecure.keychain.ui.base.BaseNfcActivity;
import org.sufficientlysecure.keychain.ui.base.CryptoOperationHelper;
import org.sufficientlysecure.keychain.ui.dialog.DeleteKeyDialogFragment;
import org.sufficientlysecure.keychain.ui.util.FormattingUtils;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils.State;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.ui.util.Notify.ActionListener;
import org.sufficientlysecure.keychain.ui.util.Notify.Style;
import org.sufficientlysecure.keychain.ui.util.QrCodeUtils;
import org.sufficientlysecure.keychain.util.ContactHelper;
import org.sufficientlysecure.keychain.util.ExportHelper;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.NfcHelper;
import org.sufficientlysecure.keychain.util.Preferences;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class ViewKeyActivity extends BaseNfcActivity implements
        LoaderManager.LoaderCallbacks<Cursor>,
        CryptoOperationHelper.Callback<ImportKeyringParcel, ImportKeyResult> {

    public static final String EXTRA_NFC_USER_ID = "nfc_user_id";
    public static final String EXTRA_NFC_AID = "nfc_aid";
    public static final String EXTRA_NFC_FINGERPRINTS = "nfc_fingerprints";

    static final int REQUEST_QR_FINGERPRINT = 1;
    static final int REQUEST_DELETE = 2;
    static final int REQUEST_EXPORT = 3;
    public static final String EXTRA_DISPLAY_RESULT = "display_result";

    ExportHelper mExportHelper;
    ProviderHelper mProviderHelper;

    protected Uri mDataUri;

    // For CryptoOperationHelper.Callback
    private String mKeyserver;
    private ArrayList<ParcelableKeyRing> mKeyList;
    private CryptoOperationHelper<ImportKeyringParcel, ImportKeyResult> mOperationHelper;

    private TextView mName;
    private TextView mStatusText;
    private ImageView mStatusImage;
    private RelativeLayout mBigToolbar;

    private ImageButton mActionEncryptFile;
    private ImageButton mActionEncryptText;
    private ImageButton mActionNfc;
    private FloatingActionButton mFab;
    private ImageView mPhoto;
    private ImageView mQrCode;
    private CardView mQrCodeLayout;

    private String mQrCodeLoaded;

    // NFC
    private NfcHelper mNfcHelper;

    private static final int LOADER_ID_UNIFIED = 0;

    private boolean mIsSecret = false;
    private boolean mHasEncrypt = false;
    private boolean mIsVerified = false;
    private boolean mIsRevoked = false;
    private boolean mIsExpired = false;

    private boolean mShowYubikeyAfterCreation = false;

    private MenuItem mRefreshItem;
    private boolean mIsRefreshing;
    private Animation mRotate, mRotateSpin;
    private View mRefresh;
    private String mFingerprint;
    private long mMasterKeyId;

    @SuppressLint("InflateParams")
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
        mActionNfc = (ImageButton) findViewById(R.id.view_key_action_nfc);
        mFab = (FloatingActionButton) findViewById(R.id.fab);
        mPhoto = (ImageView) findViewById(R.id.view_key_photo);
        mQrCode = (ImageView) findViewById(R.id.view_key_qr_code);
        mQrCodeLayout = (CardView) findViewById(R.id.view_key_qr_code_layout);

        mRotateSpin = AnimationUtils.loadAnimation(this, R.anim.rotate_spin);
        mRotateSpin.setAnimationListener(new AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mRefreshItem.getActionView().clearAnimation();
                mRefreshItem.setActionView(null);
                mRefreshItem.setEnabled(true);

                // this is a deferred call
                supportInvalidateOptionsMenu();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        mRotate = AnimationUtils.loadAnimation(this, R.anim.rotate);
        mRotate.setRepeatCount(Animation.INFINITE);
        mRotate.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {

            }

            @Override
            public void onAnimationRepeat(Animation animation) {
                if (!mIsRefreshing) {
                    mRefreshItem.getActionView().clearAnimation();
                    mRefreshItem.getActionView().startAnimation(mRotateSpin);
                }
            }
        });
        mRefresh = getLayoutInflater().inflate(R.layout.indeterminate_progress, null);

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

        Log.i(Constants.TAG, "mDataUri: " + mDataUri);

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

        mQrCodeLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showQrCodeDialog();
            }
        });

        mActionNfc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mNfcHelper.invokeNfcBeam();
            }
        });

        // Prepare the loaders. Either re-connect with an existing ones,
        // or start new ones.
        getSupportLoaderManager().initLoader(LOADER_ID_UNIFIED, null, this);

        mNfcHelper = new NfcHelper(this, mProviderHelper);
        mNfcHelper.initNfc(mDataUri);

        if (savedInstanceState == null && getIntent().hasExtra(EXTRA_DISPLAY_RESULT)) {
            OperationResult result = getIntent().getParcelableExtra(EXTRA_DISPLAY_RESULT);
            result.createNotify(this).show();
        }

        // Fragments are stored, no need to recreate those
        if (savedInstanceState != null) {
            return;
        }

        FragmentManager manager = getSupportFragmentManager();
        // Create an instance of the fragment
        final ViewKeyFragment frag = ViewKeyFragment.newInstance(mDataUri);
        manager.beginTransaction()
                .replace(R.id.view_key_fragment, frag)
                .commit();

        // need to postpone loading of the yubikey fragment until after mMasterKeyId
        // is available, but we mark here that this should be done
        mShowYubikeyAfterCreation = true;

    }

    @Override
    protected void initLayout() {
        setContentView(R.layout.view_key_activity);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.key_view, menu);
        mRefreshItem = menu.findItem(R.id.menu_key_view_refresh);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                Intent homeIntent = new Intent(this, MainActivity.class);
                homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(homeIntent);
                return true;
            }
            case R.id.menu_key_view_export_file: {
                try {
                    if (PassphraseCacheService.getCachedPassphrase(this, mMasterKeyId, mMasterKeyId) != null) {
                        exportToFile(mDataUri, mExportHelper, mProviderHelper);
                        return true;
                    }

                    startPassphraseActivity(REQUEST_EXPORT);
                } catch (PassphraseCacheService.KeyNotFoundException e) {
                    // This happens when the master key is stripped
                    exportToFile(mDataUri, mExportHelper, mProviderHelper);
                }
                return true;
            }
            case R.id.menu_key_view_delete: {
                try {
                    if (PassphraseCacheService.getCachedPassphrase(this, mMasterKeyId, mMasterKeyId) != null) {
                        deleteKey();
                        return true;
                    }

                    startPassphraseActivity(REQUEST_DELETE);
                } catch (PassphraseCacheService.KeyNotFoundException e) {
                    // This happens when the master key is stripped
                    deleteKey();
                }
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
                    Notify.create(this, R.string.error_key_not_found, Notify.Style.ERROR).show();
                }
                return true;
            }
            case R.id.menu_key_view_edit: {
                editKey(mDataUri);
                return true;
            }
            case R.id.menu_key_view_certify_fingerprint: {
                certifyFingeprint(mDataUri);
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem editKey = menu.findItem(R.id.menu_key_view_edit);
        editKey.setVisible(mIsSecret);
        MenuItem certifyFingerprint = menu.findItem(R.id.menu_key_view_certify_fingerprint);
        certifyFingerprint.setVisible(!mIsSecret && !mIsVerified && !mIsExpired && !mIsRevoked);

        return true;
    }


    private void scanQrCode() {
        Intent scanQrCode = new Intent(this, ImportKeysProxyActivity.class);
        scanQrCode.setAction(ImportKeysProxyActivity.ACTION_SCAN_WITH_RESULT);
        startActivityForResult(scanQrCode, REQUEST_QR_FINGERPRINT);
    }

    private void certifyFingeprint(Uri dataUri) {
        Intent intent = new Intent(this, CertifyFingerprintActivity.class);
        intent.setData(dataUri);

        startCertifyIntent(intent);
    }

    private void certifyImmediate() {
        Intent intent = new Intent(this, CertifyKeyActivity.class);
        intent.putExtra(CertifyKeyActivity.EXTRA_KEY_IDS, new long[]{mMasterKeyId});

        startCertifyIntent(intent);
    }

    private void startCertifyIntent(Intent intent) {
        // Message is received after signing is done in KeychainService
        ServiceProgressHandler saveHandler = new ServiceProgressHandler(this) {
            @Override
            public void handleMessage(Message message) {
                // handle messages by standard KeychainIntentServiceHandler first
                super.handleMessage(message);

                if (message.arg1 == MessageStatus.OKAY.ordinal()) {
                    Bundle data = message.getData();
                    CertifyResult result = data.getParcelable(CertifyResult.EXTRA_RESULT);

                    result.createNotify(ViewKeyActivity.this).show();
                }
            }
        };
        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(saveHandler);
        intent.putExtra(KeychainService.EXTRA_MESSENGER, messenger);

        startActivityForResult(intent, 0);
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

    private void startPassphraseActivity(int requestCode) {
        Intent intent = new Intent(this, PassphraseDialogActivity.class);
        intent.putExtra(PassphraseDialogActivity.EXTRA_SUBKEY_ID, mMasterKeyId);
        startActivityForResult(intent, requestCode);
    }

    private void exportToFile(Uri dataUri, ExportHelper exportHelper, ProviderHelper providerHelper) {
        try {
            Uri baseUri = KeychainContract.KeyRings.buildUnifiedKeyRingUri(dataUri);

            HashMap<String, Object> data = providerHelper.getGenericData(
                    baseUri,
                    new String[] {KeychainContract.Keys.MASTER_KEY_ID, KeychainContract.KeyRings.HAS_SECRET},
                    new int[] {ProviderHelper.FIELD_TYPE_INTEGER, ProviderHelper.FIELD_TYPE_INTEGER});

            exportHelper.showExportKeysDialog(
                    new long[] {(Long) data.get(KeychainContract.KeyRings.MASTER_KEY_ID)},
                    Constants.Path.APP_DIR_FILE, ((Long) data.get(KeychainContract.KeyRings.HAS_SECRET) != 0)
            );
        } catch (ProviderHelper.NotFoundException e) {
            Notify.create(this, R.string.error_key_not_found, Notify.Style.ERROR).show();
            Log.e(Constants.TAG, "Key not found", e);
        }
    }

    private void deleteKey() {
        // Message is received after key is deleted
        Handler returnHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                if (message.arg1 == MessageStatus.OKAY.ordinal()) {
                    setResult(RESULT_CANCELED);
                    finish();
                }
            }
        };

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(returnHandler);
        DeleteKeyDialogFragment deleteKeyDialog = DeleteKeyDialogFragment.newInstance(messenger,
                new long[] {mMasterKeyId});
        deleteKeyDialog.show(getSupportFragmentManager(), "deleteKeyDialog");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mOperationHelper != null) {
            mOperationHelper.handleActivityResult(requestCode, resultCode, data);
        }

        if (requestCode == REQUEST_QR_FINGERPRINT && resultCode == Activity.RESULT_OK) {

            // If there is an EXTRA_RESULT, that's an error. Just show it.
            if (data.hasExtra(OperationResult.EXTRA_RESULT)) {
                OperationResult result = data.getParcelableExtra(OperationResult.EXTRA_RESULT);
                result.createNotify(this).show();
                return;
            }

            String fp = data.getStringExtra(ImportKeysProxyActivity.EXTRA_FINGERPRINT);
            if (fp == null) {
                Notify.create(this, "Error scanning fingerprint!",
                        Notify.LENGTH_LONG, Notify.Style.ERROR).show();
                return;
            }
            if (mFingerprint.equalsIgnoreCase(fp)) {
                certifyImmediate();
            } else {
                Notify.create(this, "Fingerprints did not match!",
                        Notify.LENGTH_LONG, Notify.Style.ERROR).show();
            }

            return;
        }

        if (requestCode == REQUEST_DELETE && resultCode == Activity.RESULT_OK) {
            deleteKey();
        }

        if (requestCode == REQUEST_EXPORT && resultCode == Activity.RESULT_OK) {
            exportToFile(mDataUri, mExportHelper, mProviderHelper);
        }

        if (data != null && data.hasExtra(OperationResult.EXTRA_RESULT)) {
            OperationResult result = data.getParcelableExtra(OperationResult.EXTRA_RESULT);
            result.createNotify(this).show();
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void onNfcPerform() throws IOException {

        final byte[] nfcFingerprints = nfcGetFingerprints();
        final String nfcUserId = nfcGetUserId();
        final byte[] nfcAid = nfcGetAid();

        long yubiKeyId = KeyFormattingUtils.getKeyIdFromFingerprint(nfcFingerprints);

        try {

            // if the yubikey matches a subkey in any key
            CachedPublicKeyRing ring = mProviderHelper.getCachedPublicKeyRing(
                    KeyRings.buildUnifiedKeyRingsFindBySubkeyUri(yubiKeyId));
            byte[] candidateFp = ring.getFingerprint();

            // if the master key of that key matches this one, just show the yubikey dialog
            if (KeyFormattingUtils.convertFingerprintToHex(candidateFp).equals(mFingerprint)) {
                showYubiKeyFragment(nfcFingerprints, nfcUserId, nfcAid);
                return;
            }

            // otherwise, offer to go to that key
            final long masterKeyId = KeyFormattingUtils.getKeyIdFromFingerprint(candidateFp);
            Notify.create(this, R.string.snack_yubi_other, Notify.LENGTH_LONG,
                    Style.WARN, new ActionListener() {
                        @Override
                        public void onAction() {
                            Intent intent = new Intent(
                                    ViewKeyActivity.this, ViewKeyActivity.class);
                            intent.setData(KeyRings.buildGenericKeyRingUri(masterKeyId));
                            intent.putExtra(ViewKeyActivity.EXTRA_NFC_AID, nfcAid);
                            intent.putExtra(ViewKeyActivity.EXTRA_NFC_USER_ID, nfcUserId);
                            intent.putExtra(ViewKeyActivity.EXTRA_NFC_FINGERPRINTS, nfcFingerprints);
                            startActivity(intent);
                            finish();
                        }
                    }, R.string.snack_yubikey_view).show();

            // and if it's not found, offer import
        } catch (PgpKeyNotFoundException e) {
            Notify.create(this, R.string.snack_yubi_other, Notify.LENGTH_LONG,
                    Style.WARN, new ActionListener() {
                        @Override
                        public void onAction() {
                            Intent intent = new Intent(
                                    ViewKeyActivity.this, CreateKeyActivity.class);
                            intent.putExtra(ViewKeyActivity.EXTRA_NFC_AID, nfcAid);
                            intent.putExtra(ViewKeyActivity.EXTRA_NFC_USER_ID, nfcUserId);
                            intent.putExtra(ViewKeyActivity.EXTRA_NFC_FINGERPRINTS, nfcFingerprints);
                            startActivity(intent);
                            finish();
                        }
                    }, R.string.snack_yubikey_import).show();
        }

    }

    public void showYubiKeyFragment(
            final byte[] nfcFingerprints, final String nfcUserId, final byte[] nfcAid) {

        new Handler().post(new Runnable() {
            @Override
            public void run() {
                ViewKeyYubiKeyFragment frag = ViewKeyYubiKeyFragment.newInstance(
                        mMasterKeyId, nfcFingerprints, nfcUserId, nfcAid);

                FragmentManager manager = getSupportFragmentManager();

                manager.popBackStack("yubikey", FragmentManager.POP_BACK_STACK_INCLUSIVE);
                manager.beginTransaction()
                        .addToBackStack("yubikey")
                        .replace(R.id.view_key_fragment, frag)
                                // if this is called while the activity wasn't resumed, just forget it happened
                        .commitAllowingStateLoss();
            }
        });

    }

    private void encrypt(Uri dataUri, boolean text) {
        // If there is no encryption key, don't bother.
        if (!mHasEncrypt) {
            Notify.create(this, R.string.error_no_encrypt_subkey, Notify.Style.ERROR).show();
            return;
        }
        try {
            long keyId = new ProviderHelper(this)
                    .getCachedPublicKeyRing(dataUri)
                    .extractOrGetMasterKeyId();
            long[] encryptionKeyIds = new long[] {keyId};
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

        mIsRefreshing = true;
        mRefreshItem.setEnabled(false);
        mRefreshItem.setActionView(mRefresh);
        mRefresh.startAnimation(mRotate);

        byte[] blob = (byte[]) providerHelper.getGenericData(
                KeychainContract.KeyRings.buildUnifiedKeyRingUri(dataUri),
                KeychainContract.Keys.FINGERPRINT, ProviderHelper.FIELD_TYPE_BLOB);
        String fingerprint = KeyFormattingUtils.convertFingerprintToHex(blob);

        ParcelableKeyRing keyEntry = new ParcelableKeyRing(fingerprint, null, null);
        ArrayList<ParcelableKeyRing> entries = new ArrayList<>();
        entries.add(keyEntry);
        mKeyList = entries;

        // search config
        {
            Preferences prefs = Preferences.getPreferences(this);
            Preferences.CloudSearchPrefs cloudPrefs =
                    new Preferences.CloudSearchPrefs(true, true, prefs.getPreferredKeyserver());
            mKeyserver = cloudPrefs.keyserver;
        }

        mOperationHelper = new CryptoOperationHelper<>(
                this, this, R.string.progress_importing);

        mOperationHelper.cryptoOperation();
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
                        mQrCodeLoaded = fingerprint;
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


    // These are the rows that we will retrieve.
    static final String[] PROJECTION = new String[] {
            KeychainContract.KeyRings._ID,
            KeychainContract.KeyRings.MASTER_KEY_ID,
            KeychainContract.KeyRings.USER_ID,
            KeychainContract.KeyRings.IS_REVOKED,
            KeychainContract.KeyRings.IS_EXPIRED,
            KeychainContract.KeyRings.VERIFIED,
            KeychainContract.KeyRings.HAS_ANY_SECRET,
            KeychainContract.KeyRings.FINGERPRINT,
            KeychainContract.KeyRings.HAS_ENCRYPT
    };

    static final int INDEX_MASTER_KEY_ID = 1;
    static final int INDEX_USER_ID = 2;
    static final int INDEX_IS_REVOKED = 3;
    static final int INDEX_IS_EXPIRED = 4;
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

    int mPreviousColor = 0;

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
                    KeyRing.UserId mainUserId = KeyRing.splitUserId(data.getString(INDEX_USER_ID));
                    if (mainUserId.name != null) {
                        mName.setText(mainUserId.name);
                    } else {
                        mName.setText(R.string.user_id_no_name);
                    }

                    mMasterKeyId = data.getLong(INDEX_MASTER_KEY_ID);
                    mFingerprint = KeyFormattingUtils.convertFingerprintToHex(data.getBlob(INDEX_FINGERPRINT));

                    // if it wasn't shown yet, display yubikey fragment
                    if (mShowYubikeyAfterCreation && getIntent().hasExtra(EXTRA_NFC_AID)) {
                        mShowYubikeyAfterCreation = false;
                        Intent intent = getIntent();
                        byte[] nfcFingerprints = intent.getByteArrayExtra(EXTRA_NFC_FINGERPRINTS);
                        String nfcUserId = intent.getStringExtra(EXTRA_NFC_USER_ID);
                        byte[] nfcAid = intent.getByteArrayExtra(EXTRA_NFC_AID);
                        showYubiKeyFragment(nfcFingerprints, nfcUserId, nfcAid);
                    }

                    mIsSecret = data.getInt(INDEX_HAS_ANY_SECRET) != 0;
                    mHasEncrypt = data.getInt(INDEX_HAS_ENCRYPT) != 0;
                    mIsRevoked = data.getInt(INDEX_IS_REVOKED) > 0;
                    mIsExpired = data.getInt(INDEX_IS_EXPIRED) != 0;
                    mIsVerified = data.getInt(INDEX_VERIFIED) > 0;

                    // if the refresh animation isn't playing
                    if (!mRotate.hasStarted() && !mRotateSpin.hasStarted()) {
                        // re-create options menu based on mIsSecret, mIsVerified
                        supportInvalidateOptionsMenu();
                        // this is done at the end of the animation otherwise
                    }

                    AsyncTask<Long, Void, Bitmap> photoTask =
                            new AsyncTask<Long, Void, Bitmap>() {
                                protected Bitmap doInBackground(Long... mMasterKeyId) {
                                    return ContactHelper.loadPhotoByMasterKeyId(getContentResolver(), mMasterKeyId[0], true);
                                }

                                protected void onPostExecute(Bitmap photo) {
                                    mPhoto.setImageBitmap(photo);
                                    mPhoto.setVisibility(View.VISIBLE);
                                }
                            };

                    // Note: order is important
                    int color;
                    if (mIsRevoked) {
                        mStatusText.setText(R.string.view_key_revoked);
                        mStatusImage.setVisibility(View.VISIBLE);
                        KeyFormattingUtils.setStatusImage(this, mStatusImage, mStatusText,
                                State.REVOKED, R.color.icons, true);
                        color = getResources().getColor(R.color.android_red_light);

                        mActionEncryptFile.setVisibility(View.GONE);
                        mActionEncryptText.setVisibility(View.GONE);
                        mActionNfc.setVisibility(View.GONE);
                        mFab.setVisibility(View.GONE);
                        mQrCodeLayout.setVisibility(View.GONE);
                    } else if (mIsExpired) {
                        if (mIsSecret) {
                            mStatusText.setText(R.string.view_key_expired_secret);
                        } else {
                            mStatusText.setText(R.string.view_key_expired);
                        }
                        mStatusImage.setVisibility(View.VISIBLE);
                        KeyFormattingUtils.setStatusImage(this, mStatusImage, mStatusText,
                                State.EXPIRED, R.color.icons, true);
                        color = getResources().getColor(R.color.android_red_light);

                        mActionEncryptFile.setVisibility(View.GONE);
                        mActionEncryptText.setVisibility(View.GONE);
                        mActionNfc.setVisibility(View.GONE);
                        mFab.setVisibility(View.GONE);
                        mQrCodeLayout.setVisibility(View.GONE);
                    } else if (mIsSecret) {
                        mStatusText.setText(R.string.view_key_my_key);
                        mStatusImage.setVisibility(View.GONE);
                        color = getResources().getColor(R.color.primary);
                        // reload qr code only if the fingerprint changed
                        if (!mFingerprint.equals(mQrCodeLoaded)) {
                            loadQrCode(mFingerprint);
                        }
                        photoTask.execute(mMasterKeyId);
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

                        if (mIsVerified) {
                            mStatusText.setText(R.string.view_key_verified);
                            mStatusImage.setVisibility(View.VISIBLE);
                            KeyFormattingUtils.setStatusImage(this, mStatusImage, mStatusText,
                                    State.VERIFIED, R.color.icons, true);
                            color = getResources().getColor(R.color.primary);
                            photoTask.execute(mMasterKeyId);

                            mFab.setVisibility(View.GONE);
                        } else {
                            mStatusText.setText(R.string.view_key_unverified);
                            mStatusImage.setVisibility(View.VISIBLE);
                            KeyFormattingUtils.setStatusImage(this, mStatusImage, mStatusText,
                                    State.UNVERIFIED, R.color.icons, true);
                            color = getResources().getColor(R.color.android_orange_light);

                            mFab.setVisibility(View.VISIBLE);
                        }
                    }

                    if (mPreviousColor == 0 || mPreviousColor == color) {
                        mStatusBar.setBackgroundColor(color);
                        mBigToolbar.setBackgroundColor(color);
                        mPreviousColor = color;
                    } else {
                        ObjectAnimator colorFade1 =
                                ObjectAnimator.ofObject(mStatusBar, "backgroundColor",
                                        new ArgbEvaluator(), mPreviousColor, color);
                        ObjectAnimator colorFade2 =
                                ObjectAnimator.ofObject(mBigToolbar, "backgroundColor",
                                        new ArgbEvaluator(), mPreviousColor, color);

                        colorFade1.setDuration(1200);
                        colorFade2.setDuration(1200);
                        colorFade1.start();
                        colorFade2.start();
                        mPreviousColor = color;
                    }

                    //noinspection deprecation
                    mStatusImage.setAlpha(80);

                    break;
                }
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    // CryptoOperationHelper.Callback functions

    @Override
    public ImportKeyringParcel createOperationInput() {
        return new ImportKeyringParcel(mKeyList, mKeyserver);
    }

    @Override
    public void onCryptoOperationSuccess(ImportKeyResult result) {
        mIsRefreshing = false;
        result.createNotify(this).show();
    }

    @Override
    public void onCryptoOperationCancelled() {
        mIsRefreshing = false;
    }

    @Override
    public void onCryptoOperationError(ImportKeyResult result) {
        mIsRefreshing = false;
        result.createNotify(this).show();
    }
}