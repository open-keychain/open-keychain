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


import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.provider.ContactsContract;
import android.support.annotation.IntDef;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
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
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.openintents.openpgp.util.OpenPgpUtils;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.keyimport.ParcelableKeyRing;
import org.sufficientlysecure.keychain.operations.results.EditKeyResult;
import org.sufficientlysecure.keychain.operations.results.ImportKeyResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.pgp.KeyRing;
import org.sufficientlysecure.keychain.pgp.exception.PgpKeyNotFoundException;
import org.sufficientlysecure.keychain.provider.CachedPublicKeyRing;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.provider.ProviderReader;
import org.sufficientlysecure.keychain.service.ChangeUnlockParcel;
import org.sufficientlysecure.keychain.service.ImportKeyringParcel;
import org.sufficientlysecure.keychain.ui.ViewKeyFragment.PostponeType;
import org.sufficientlysecure.keychain.ui.base.BaseSecurityTokenActivity;
import org.sufficientlysecure.keychain.ui.base.CryptoOperationHelper;
import org.sufficientlysecure.keychain.ui.dialog.SetPassphraseDialogFragment;
import org.sufficientlysecure.keychain.ui.util.FormattingUtils;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils.State;
import org.sufficientlysecure.keychain.ui.util.ContentDescriptionHint;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.ui.util.Notify.ActionListener;
import org.sufficientlysecure.keychain.ui.util.Notify.Style;
import org.sufficientlysecure.keychain.ui.util.QrCodeUtils;
import org.sufficientlysecure.keychain.util.ContactHelper;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.NfcHelper;
import org.sufficientlysecure.keychain.util.Passphrase;
import org.sufficientlysecure.keychain.util.Preferences;


public class ViewKeyActivity extends BaseSecurityTokenActivity implements
        LoaderManager.LoaderCallbacks<Cursor>,
        CryptoOperationHelper.Callback<ImportKeyringParcel, ImportKeyResult> {

    public static final String EXTRA_SECURITY_TOKEN_USER_ID = "security_token_user_id";
    public static final String EXTRA_SECURITY_TOKEN_AID = "security_token_aid";
    public static final String EXTRA_SECURITY_TOKEN_FINGERPRINTS = "security_token_fingerprints";

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({REQUEST_QR_FINGERPRINT, REQUEST_CERTIFY, REQUEST_DELETE})
    private @interface RequestType {}
    static final int REQUEST_QR_FINGERPRINT = 1;
    static final int REQUEST_CERTIFY = 2;
    static final int REQUEST_DELETE = 3;

    public static final String EXTRA_DISPLAY_RESULT = "display_result";
    public static final String EXTRA_LINKED_TRANSITION = "linked_transition";

    ProviderHelper mProviderHelper;

    protected Uri mDataUri;

    // For CryptoOperationHelper.Callback
    private String mKeyserver;
    private ArrayList<ParcelableKeyRing> mKeyList;
    private CryptoOperationHelper<ImportKeyringParcel, ImportKeyResult> mImportOpHelper;
    private CryptoOperationHelper<ChangeUnlockParcel, EditKeyResult> mEditOpHelper;
    private ChangeUnlockParcel mChangeUnlockParcel;

    private TextView mStatusText;
    private ImageView mStatusImage;
    private AppBarLayout mAppBarLayout;
    private CollapsingToolbarLayout mCollapsingToolbarLayout;

    private ImageButton mActionEncryptFile;
    private ImageButton mActionEncryptText;
    private ImageButton mActionNfc;
    private FloatingActionButton mFab;
    private ImageView mPhoto;
    private FrameLayout mPhotoLayout;
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

    private boolean mShowSecurityTokenAfterCreation = false;

    private MenuItem mRefreshItem;
    private boolean mIsRefreshing;
    private Animation mRotate, mRotateSpin;
    private View mRefresh;

    private long mMasterKeyId;
    private byte[] mFingerprint;
    private String mFingerprintString;

    private byte[] mSecurityTokenFingerprints;
    private String mSecurityTokenUserId;
    private byte[] mSecurityTokenAid;

    @SuppressLint("InflateParams")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mProviderHelper = new ProviderHelper(this);
        mImportOpHelper = new CryptoOperationHelper<>(1, this, this, null);

        setTitle(null);

        mStatusText = (TextView) findViewById(R.id.view_key_status);
        mStatusImage = (ImageView) findViewById(R.id.view_key_status_image);
        mAppBarLayout = (AppBarLayout) findViewById(R.id.app_bar_layout);
        mCollapsingToolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar);

        mActionEncryptFile = (ImageButton) findViewById(R.id.view_key_action_encrypt_files);
        mActionEncryptText = (ImageButton) findViewById(R.id.view_key_action_encrypt_text);
        mActionNfc = (ImageButton) findViewById(R.id.view_key_action_nfc);
        mFab = (FloatingActionButton) findViewById(R.id.fab);
        mPhoto = (ImageView) findViewById(R.id.view_key_photo);
        mPhotoLayout = (FrameLayout) findViewById(R.id.view_key_photo_layout);
        mQrCode = (ImageView) findViewById(R.id.view_key_qr_code);
        mQrCodeLayout = (CardView) findViewById(R.id.view_key_qr_code_layout);

        mRotateSpin = AnimationUtils.loadAnimation(this, R.anim.rotate_spin);

        //ContentDescriptionHint Listeners implemented

        ContentDescriptionHint.setup(mActionEncryptFile);
        ContentDescriptionHint.setup(mActionEncryptText);
        ContentDescriptionHint.setup(mActionNfc);
        ContentDescriptionHint.setup(mFab);


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
            mDataUri = new ContactHelper(this).dataUriFromContactUri(mDataUri);
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

        boolean linkedTransition = getIntent().getBooleanExtra(EXTRA_LINKED_TRANSITION, false);
        if (linkedTransition && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            postponeEnterTransition();
        }

        FragmentManager manager = getSupportFragmentManager();
        // Create an instance of the fragment
        final ViewKeyFragment frag = ViewKeyFragment.newInstance(mDataUri,
                linkedTransition ? PostponeType.LINKED : PostponeType.NONE);
        manager.beginTransaction()
                .replace(R.id.view_key_fragment, frag)
                .commit();

        if (Preferences.getPreferences(this).getExperimentalEnableKeybase()) {
            final ViewKeyKeybaseFragment keybaseFrag = ViewKeyKeybaseFragment.newInstance(mDataUri);
            manager.beginTransaction()
                    .replace(R.id.view_key_keybase_fragment, keybaseFrag)
                    .commit();
        }

        // need to postpone loading of the security token fragment until after mMasterKeyId
        // is available, but we mark here that this should be done
        mShowSecurityTokenAfterCreation = true;

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
            case R.id.menu_key_change_password: {
                changePassword();
                return true;
            }
            case R.id.menu_key_view_backup: {
                startBackupActivity();
                return true;
            }
            case R.id.menu_key_view_delete: {
                deleteKey();
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
                } catch (ProviderReader.NotFoundException e) {
                    Notify.create(this, R.string.error_key_not_found, Notify.Style.ERROR).show();
                }
                return true;
            }
            case R.id.menu_key_view_certify_fingerprint: {
                certifyFingerprint(mDataUri, false);
                return true;
            }
            case R.id.menu_key_view_certify_fingerprint_word: {
                certifyFingerprint(mDataUri, true);
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem backupKey = menu.findItem(R.id.menu_key_view_backup);
        backupKey.setVisible(mIsSecret);
        MenuItem changePassword = menu.findItem(R.id.menu_key_change_password);
        changePassword.setVisible(mIsSecret);

        MenuItem certifyFingerprint = menu.findItem(R.id.menu_key_view_certify_fingerprint);
        certifyFingerprint.setVisible(!mIsSecret && !mIsVerified && !mIsExpired && !mIsRevoked);
        MenuItem certifyFingerprintWord = menu.findItem(R.id.menu_key_view_certify_fingerprint_word);
        certifyFingerprintWord.setVisible(!mIsSecret && !mIsVerified && !mIsExpired && !mIsRevoked
                && Preferences.getPreferences(this).getExperimentalEnableWordConfirm());

        return true;
    }

    private void changePassword() {
        CryptoOperationHelper.Callback<ChangeUnlockParcel, EditKeyResult> editKeyCallback
                = new CryptoOperationHelper.Callback<ChangeUnlockParcel, EditKeyResult>() {
            @Override
            public ChangeUnlockParcel createOperationInput() {
                return mChangeUnlockParcel;
            }

            @Override
            public void onCryptoOperationSuccess(EditKeyResult result) {
                displayResult(result);
            }

            @Override
            public void onCryptoOperationCancelled() {

            }

            @Override
            public void onCryptoOperationError(EditKeyResult result) {
                displayResult(result);
            }

            @Override
            public boolean onCryptoSetProgress(String msg, int progress, int max) {
                return false;
            }
        };

        mEditOpHelper = new CryptoOperationHelper<>(2, this, editKeyCallback, R.string.progress_building_key);

        // Message is received after passphrase is cached
        Handler returnHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                if (message.what == SetPassphraseDialogFragment.MESSAGE_OKAY) {
                    Bundle data = message.getData();

                    // use new passphrase!
                    mChangeUnlockParcel = new ChangeUnlockParcel(
                            mMasterKeyId,
                            mFingerprint,
                            (Passphrase) data.getParcelable(SetPassphraseDialogFragment.MESSAGE_NEW_PASSPHRASE)
                    );

                    mEditOpHelper.cryptoOperation();
                }
            }
        };

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(returnHandler);

        SetPassphraseDialogFragment setPassphraseDialog = SetPassphraseDialogFragment.newInstance(
                messenger, R.string.title_change_passphrase);

        setPassphraseDialog.show(getSupportFragmentManager(), "setPassphraseDialog");
    }

    private void displayResult(OperationResult result) {
        result.createNotify(this).show();
    }

    private void scanQrCode() {
        Intent scanQrCode = new Intent(this, ImportKeysProxyActivity.class);
        scanQrCode.setAction(ImportKeysProxyActivity.ACTION_SCAN_WITH_RESULT);
        startActivityForResult(scanQrCode, REQUEST_QR_FINGERPRINT);
    }

    private void certifyFingerprint(Uri dataUri, boolean enableWordConfirm) {
        Intent intent = new Intent(this, CertifyFingerprintActivity.class);
        intent.setData(dataUri);
        intent.putExtra(CertifyFingerprintActivity.EXTRA_ENABLE_WORD_CONFIRM, enableWordConfirm);

        startActivityForResult(intent, REQUEST_CERTIFY);
    }

    private void certifyImmediate() {
        Intent intent = new Intent(this, CertifyKeyActivity.class);
        intent.putExtra(CertifyKeyActivity.EXTRA_KEY_IDS, new long[]{mMasterKeyId});

        startActivityForResult(intent, REQUEST_CERTIFY);
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


    private void startBackupActivity() {
        Intent intent = new Intent(this, BackupActivity.class);
        intent.putExtra(BackupActivity.EXTRA_MASTER_KEY_IDS, new long[] { mMasterKeyId });
        intent.putExtra(BackupActivity.EXTRA_SECRET, true);
        startActivity(intent);
    }

    private void deleteKey() {
        Intent deleteIntent = new Intent(this, DeleteKeyDialogActivity.class);

        deleteIntent.putExtra(DeleteKeyDialogActivity.EXTRA_DELETE_MASTER_KEY_IDS,
                new long[]{mMasterKeyId});
        deleteIntent.putExtra(DeleteKeyDialogActivity.EXTRA_HAS_SECRET, mIsSecret);
        if (mIsSecret) {
            // for upload in case key is secret
            deleteIntent.putExtra(DeleteKeyDialogActivity.EXTRA_KEYSERVER,
                    Preferences.getPreferences(this).getPreferredKeyserver());
        }

        startActivityForResult(deleteIntent, REQUEST_DELETE);
    }

    @Override
    protected void onActivityResult(@RequestType int requestCode, int resultCode, Intent data) {
        if (mImportOpHelper.handleActivityResult(requestCode, resultCode, data)) {
            return;
        }
        if (mEditOpHelper != null) {
            mEditOpHelper.handleActivityResult(requestCode, resultCode, data);
        }

        if (resultCode != Activity.RESULT_OK) {
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }

        switch (requestCode) {
            case REQUEST_QR_FINGERPRINT: {

                // If there is an EXTRA_RESULT, that's an error. Just show it.
                if (data.hasExtra(OperationResult.EXTRA_RESULT)) {
                    OperationResult result = data.getParcelableExtra(OperationResult.EXTRA_RESULT);
                    result.createNotify(this).show();
                    return;
                }

                String fp = data.getStringExtra(ImportKeysProxyActivity.EXTRA_FINGERPRINT);
                if (fp == null) {
                    Notify.create(this, R.string.error_scan_fp, Notify.LENGTH_LONG, Style.ERROR).show();
                    return;
                }
                if (mFingerprintString.equalsIgnoreCase(fp)) {
                    certifyImmediate();
                } else {
                    Notify.create(this, R.string.error_scan_match, Notify.LENGTH_LONG, Style.ERROR).show();
                }
                return;
            }

            case REQUEST_DELETE: {
                setResult(RESULT_OK, data);
                finish();
                return;
            }

            case REQUEST_CERTIFY: {
                if (data.hasExtra(OperationResult.EXTRA_RESULT)) {
                    OperationResult result = data.getParcelableExtra(OperationResult.EXTRA_RESULT);
                    result.createNotify(this).show();
                }
                return;
            }

        }

        super.onActivityResult(requestCode, resultCode, data);

    }

    @Override
    protected void doSecurityTokenInBackground() throws IOException {

        mSecurityTokenFingerprints = mSecurityTokenHelper.getFingerprints();
        mSecurityTokenUserId = mSecurityTokenHelper.getUserId();
        mSecurityTokenAid = mSecurityTokenHelper.getAid();
    }

    @Override
    protected void onSecurityTokenPostExecute() {

        long tokenId = KeyFormattingUtils.getKeyIdFromFingerprint(mSecurityTokenFingerprints);

        try {

            // if the security token matches a subkey in any key
            CachedPublicKeyRing ring = mProviderHelper.mReader.getCachedPublicKeyRing(
                    KeyRings.buildUnifiedKeyRingsFindBySubkeyUri(tokenId));
            byte[] candidateFp = ring.getFingerprint();

            // if the master key of that key matches this one, just show the token dialog
            if (KeyFormattingUtils.convertFingerprintToHex(candidateFp).equals(mFingerprintString)) {
                showSecurityTokenFragment(mSecurityTokenFingerprints, mSecurityTokenUserId, mSecurityTokenAid);
                return;
            }

            // otherwise, offer to go to that key
            final long masterKeyId = KeyFormattingUtils.getKeyIdFromFingerprint(candidateFp);
            Notify.create(this, R.string.snack_security_token_other, Notify.LENGTH_LONG,
                    Style.WARN, new ActionListener() {
                        @Override
                        public void onAction() {
                            Intent intent = new Intent(
                                    ViewKeyActivity.this, ViewKeyActivity.class);
                            intent.setData(KeyRings.buildGenericKeyRingUri(masterKeyId));
                            intent.putExtra(ViewKeyActivity.EXTRA_SECURITY_TOKEN_AID, mSecurityTokenAid);
                            intent.putExtra(ViewKeyActivity.EXTRA_SECURITY_TOKEN_USER_ID, mSecurityTokenUserId);
                            intent.putExtra(ViewKeyActivity.EXTRA_SECURITY_TOKEN_FINGERPRINTS, mSecurityTokenFingerprints);
                            startActivity(intent);
                            finish();
                        }
                    }, R.string.snack_security_token_view).show();
            // and if it's not found, offer import
        } catch (PgpKeyNotFoundException e) {
            Notify.create(this, R.string.snack_security_token_other, Notify.LENGTH_LONG,
                    Style.WARN, new ActionListener() {
                        @Override
                        public void onAction() {
                            Intent intent = new Intent(
                                    ViewKeyActivity.this, CreateKeyActivity.class);
                            intent.putExtra(ViewKeyActivity.EXTRA_SECURITY_TOKEN_AID, mSecurityTokenAid);
                            intent.putExtra(ViewKeyActivity.EXTRA_SECURITY_TOKEN_USER_ID, mSecurityTokenUserId);
                            intent.putExtra(ViewKeyActivity.EXTRA_SECURITY_TOKEN_FINGERPRINTS, mSecurityTokenFingerprints);
                            startActivity(intent);
                            finish();
                        }
                    }, R.string.snack_security_token_import).show();
        }
    }

    public void showSecurityTokenFragment(
            final byte[] tokenFingerprints, final String tokenUserId, final byte[] tokenAid) {

        new Handler().post(new Runnable() {
            @Override
            public void run() {
                ViewKeySecurityTokenFragment frag = ViewKeySecurityTokenFragment.newInstance(
                        mMasterKeyId, tokenFingerprints, tokenUserId, tokenAid);

                FragmentManager manager = getSupportFragmentManager();

                manager.popBackStack("security_token", FragmentManager.POP_BACK_STACK_INCLUSIVE);
                manager.beginTransaction()
                        .addToBackStack("security_token")
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
                    .mReader.getCachedPublicKeyRing(dataUri)
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

    private void startSafeSlinger(Uri dataUri) {
        long keyId = 0;
        try {
            keyId = new ProviderHelper(this)
                    .mReader.getCachedPublicKeyRing(dataUri)
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
    static final String[] PROJECTION = new String[]{
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

    /**
     * Calculate a reasonable color for the status bar based on the given toolbar color.
     * Style guides want the toolbar color to be a "700" on the Android scale and the status
     * bar should be the same color at "500", this is roughly 17 / 20th of the value in each
     * channel.
     * http://www.google.com/design/spec/style/color.html#color-color-palette
     */
    static public int getStatusBarBackgroundColor(int color) {
        int r = (color >> 16) & 0xff;
        int g = (color >> 8) & 0xff;
        int b = color & 0xff;

        r = r * 17 / 20;
        g = g * 17 / 20;
        b = b * 17 / 20;

        return (0xff << 24) | (r << 16) | (g << 8) | b;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        /* TODO better error handling? May cause problems when a key is deleted,
         * because the notification triggers faster than the activity closes.
         */

        // Swap the new cursor in. (The framework will take care of closing the
        // old cursor once we return.)
        switch (loader.getId()) {
            case LOADER_ID_UNIFIED: {
                // Avoid NullPointerExceptions...
                if (data.getCount() == 0) {
                    return;
                }

                if (data.moveToFirst()) {
                    // get name, email, and comment from USER_ID
                    OpenPgpUtils.UserId mainUserId = KeyRing.splitUserId(data.getString(INDEX_USER_ID));
                    if (mainUserId.name != null) {
                        mCollapsingToolbarLayout.setTitle(mainUserId.name);
                    } else {
                        mCollapsingToolbarLayout.setTitle(getString(R.string.user_id_no_name));
                    }

                    mMasterKeyId = data.getLong(INDEX_MASTER_KEY_ID);
                    mFingerprint = data.getBlob(INDEX_FINGERPRINT);
                    mFingerprintString = KeyFormattingUtils.convertFingerprintToHex(mFingerprint);

                    // if it wasn't shown yet, display token fragment
                    if (mShowSecurityTokenAfterCreation && getIntent().hasExtra(EXTRA_SECURITY_TOKEN_AID)) {
                        mShowSecurityTokenAfterCreation = false;
                        Intent intent = getIntent();
                        byte[] tokenFingerprints = intent.getByteArrayExtra(EXTRA_SECURITY_TOKEN_FINGERPRINTS);
                        String tokenUserId = intent.getStringExtra(EXTRA_SECURITY_TOKEN_USER_ID);
                        byte[] tokenAid = intent.getByteArrayExtra(EXTRA_SECURITY_TOKEN_AID);
                        showSecurityTokenFragment(tokenFingerprints, tokenUserId, tokenAid);
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
                                    return new ContactHelper(ViewKeyActivity.this)
                                            .loadPhotoByMasterKeyId(mMasterKeyId[0], true);
                                }

                                protected void onPostExecute(Bitmap photo) {
                                    if (photo == null) {
                                        return;
                                    }

                                    mPhoto.setImageBitmap(photo);
                                    mPhoto.setColorFilter(getResources().getColor(R.color.toolbar_photo_tint), PorterDuff.Mode.SRC_ATOP);
                                    mPhotoLayout.setVisibility(View.VISIBLE);
                                }
                            };

                    // Note: order is important
                    int color;
                    if (mIsRevoked) {
                        mStatusText.setText(R.string.view_key_revoked);
                        mStatusImage.setVisibility(View.VISIBLE);
                        KeyFormattingUtils.setStatusImage(this, mStatusImage, mStatusText,
                                State.REVOKED, R.color.icons, true);
                        // noinspection deprecation, fix requires api level 23
                        color = getResources().getColor(R.color.key_flag_red);

                        mActionEncryptFile.setVisibility(View.INVISIBLE);
                        mActionEncryptText.setVisibility(View.INVISIBLE);
                        mActionNfc.setVisibility(View.INVISIBLE);
                        hideFab();
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
                        // noinspection deprecation, fix requires api level 23
                        color = getResources().getColor(R.color.key_flag_red);

                        mActionEncryptFile.setVisibility(View.INVISIBLE);
                        mActionEncryptText.setVisibility(View.INVISIBLE);
                        mActionNfc.setVisibility(View.INVISIBLE);
                        hideFab();
                        mQrCodeLayout.setVisibility(View.GONE);
                    } else if (mIsSecret) {
                        mStatusText.setText(R.string.view_key_my_key);
                        mStatusImage.setVisibility(View.GONE);
                        // noinspection deprecation, fix requires api level 23
                        color = getResources().getColor(R.color.key_flag_green);
                        // reload qr code only if the fingerprint changed
                        if (!mFingerprintString.equals(mQrCodeLoaded)) {
                            loadQrCode(mFingerprintString);
                        }
                        photoTask.execute(mMasterKeyId);
                        mQrCodeLayout.setVisibility(View.VISIBLE);

                        // and place leftOf qr code
//                        RelativeLayout.LayoutParams nameParams = (RelativeLayout.LayoutParams)
//                                mName.getLayoutParams();
//                        // remove right margin
//                        nameParams.setMargins(FormattingUtils.dpToPx(this, 48), 0, 0, 0);
//                        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
//                            nameParams.setMarginEnd(0);
//                        }
//                        nameParams.addRule(RelativeLayout.LEFT_OF, R.id.view_key_qr_code_layout);
//                        mName.setLayoutParams(nameParams);

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
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                                && NfcAdapter.getDefaultAdapter(this) != null) {
                            mActionNfc.setVisibility(View.VISIBLE);
                        } else {
                            mActionNfc.setVisibility(View.INVISIBLE);
                        }
                        showFab();
                        // noinspection deprecation (no getDrawable with theme at current minApi level 15!)
                        mFab.setImageDrawable(getResources().getDrawable(R.drawable.ic_repeat_white_24dp));
                    } else {
                        mActionEncryptFile.setVisibility(View.VISIBLE);
                        mActionEncryptText.setVisibility(View.VISIBLE);
                        mQrCodeLayout.setVisibility(View.GONE);
                        mActionNfc.setVisibility(View.INVISIBLE);

                        if (mIsVerified) {
                            mStatusText.setText(R.string.view_key_verified);
                            mStatusImage.setVisibility(View.VISIBLE);
                            KeyFormattingUtils.setStatusImage(this, mStatusImage, mStatusText,
                                    State.VERIFIED, R.color.icons, true);
                            // noinspection deprecation, fix requires api level 23
                            color = getResources().getColor(R.color.key_flag_green);
                            photoTask.execute(mMasterKeyId);

                            hideFab();
                        } else {
                            mStatusText.setText(R.string.view_key_unverified);
                            mStatusImage.setVisibility(View.VISIBLE);
                            KeyFormattingUtils.setStatusImage(this, mStatusImage, mStatusText,
                                    State.UNVERIFIED, R.color.icons, true);
                            // noinspection deprecation, fix requires api level 23
                            color = getResources().getColor(R.color.key_flag_orange);

                            showFab();
                        }
                    }

                    if (mPreviousColor == 0 || mPreviousColor == color) {
                        mAppBarLayout.setBackgroundColor(color);
                        mCollapsingToolbarLayout.setContentScrimColor(color);
                        mCollapsingToolbarLayout.setStatusBarScrimColor(getStatusBarBackgroundColor(color));
                        mPreviousColor = color;
                    } else {
                        ObjectAnimator colorFade =
                                ObjectAnimator.ofObject(mAppBarLayout, "backgroundColor",
                                        new ArgbEvaluator(), mPreviousColor, color);
                        mCollapsingToolbarLayout.setContentScrimColor(color);
                        mCollapsingToolbarLayout.setStatusBarScrimColor(getStatusBarBackgroundColor(color));

                        colorFade.setDuration(1200);
                        colorFade.start();
                        mPreviousColor = color;
                    }

                    //noinspection deprecation
                    mStatusImage.setAlpha(80);

                    break;
                }
            }
        }
    }

    /**
     * Helper to show Fab, from http://stackoverflow.com/a/31047038
     */
    private void showFab() {
        CoordinatorLayout.LayoutParams p = (CoordinatorLayout.LayoutParams) mFab.getLayoutParams();
        p.setBehavior(new FloatingActionButton.Behavior());
        p.setAnchorId(R.id.app_bar_layout);
        mFab.setLayoutParams(p);
        mFab.setVisibility(View.VISIBLE);
    }

    /**
     * Helper to hide Fab, from http://stackoverflow.com/a/31047038
     */
    private void hideFab() {
        CoordinatorLayout.LayoutParams p = (CoordinatorLayout.LayoutParams) mFab.getLayoutParams();
        p.setBehavior(null); //should disable default animations
        p.setAnchorId(View.NO_ID); //should let you set visibility
        mFab.setLayoutParams(p);
        mFab.setVisibility(View.GONE);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    // CryptoOperationHelper.Callback functions


    private void updateFromKeyserver(Uri dataUri, ProviderHelper providerHelper)
            throws ProviderReader.NotFoundException {

        mIsRefreshing = true;
        mRefreshItem.setEnabled(false);
        mRefreshItem.setActionView(mRefresh);
        mRefresh.startAnimation(mRotate);

        byte[] blob = (byte[]) providerHelper.mReader.getGenericData(
                KeychainContract.KeyRings.buildUnifiedKeyRingUri(dataUri),
                KeychainContract.Keys.FINGERPRINT, Cursor.FIELD_TYPE_BLOB);
        String fingerprint = KeyFormattingUtils.convertFingerprintToHex(blob);

        ParcelableKeyRing keyEntry = new ParcelableKeyRing(fingerprint, null);
        ArrayList<ParcelableKeyRing> entries = new ArrayList<>();
        entries.add(keyEntry);
        mKeyList = entries;

        mKeyserver = Preferences.getPreferences(this).getPreferredKeyserver();

        mImportOpHelper.cryptoOperation();
    }

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

    @Override
    public boolean onCryptoSetProgress(String msg, int progress, int max) {
        return true;
    }

}

