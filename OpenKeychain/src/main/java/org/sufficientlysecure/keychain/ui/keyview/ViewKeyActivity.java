/*
 * Copyright (C) 2017 Schürmann & Breitmoser GbR
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

package org.sufficientlysecure.keychain.ui.keyview;


import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.Collections;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityOptions;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.provider.ContactsContract;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
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

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.keyimport.HkpKeyserverAddress;
import org.sufficientlysecure.keychain.keyimport.ParcelableKeyRing;
import org.sufficientlysecure.keychain.model.SubKey.UnifiedKeyInfo;
import org.sufficientlysecure.keychain.operations.results.EditKeyResult;
import org.sufficientlysecure.keychain.operations.results.ImportKeyResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey.SecretKeyType;
import org.sufficientlysecure.keychain.daos.KeyRepository;
import org.sufficientlysecure.keychain.daos.KeyRepository.NotFoundException;
import org.sufficientlysecure.keychain.securitytoken.SecurityTokenConnection;
import org.sufficientlysecure.keychain.service.ChangeUnlockParcel;
import org.sufficientlysecure.keychain.service.ImportKeyringParcel;
import org.sufficientlysecure.keychain.service.PassphraseCacheService;
import org.sufficientlysecure.keychain.service.input.RequiredInputParcel;
import org.sufficientlysecure.keychain.ui.BackupActivity;
import org.sufficientlysecure.keychain.ui.CertifyFingerprintActivity;
import org.sufficientlysecure.keychain.ui.CertifyKeyActivity;
import org.sufficientlysecure.keychain.ui.DeleteKeyDialogActivity;
import org.sufficientlysecure.keychain.ui.EncryptFilesActivity;
import org.sufficientlysecure.keychain.ui.EncryptTextActivity;
import org.sufficientlysecure.keychain.ui.ImportKeysProxyActivity;
import org.sufficientlysecure.keychain.ui.MainActivity;
import org.sufficientlysecure.keychain.ui.PassphraseDialogActivity;
import org.sufficientlysecure.keychain.ui.QrCodeViewActivity;
import org.sufficientlysecure.keychain.ui.SafeSlingerActivity;
import org.sufficientlysecure.keychain.ui.ViewKeyAdvActivity;
import org.sufficientlysecure.keychain.ui.ViewKeyKeybaseFragment;
import org.sufficientlysecure.keychain.ui.base.BaseSecurityTokenActivity;
import org.sufficientlysecure.keychain.ui.base.CryptoOperationHelper;
import org.sufficientlysecure.keychain.ui.dialog.SetPassphraseDialogFragment;
import org.sufficientlysecure.keychain.ui.util.ContentDescriptionHint;
import org.sufficientlysecure.keychain.ui.util.FormattingUtils;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils.State;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.ui.util.Notify.Style;
import org.sufficientlysecure.keychain.ui.util.QrCodeUtils;
import org.sufficientlysecure.keychain.util.ContactHelper;
import org.sufficientlysecure.keychain.util.Preferences;
import timber.log.Timber;


public class ViewKeyActivity extends BaseSecurityTokenActivity implements
        CryptoOperationHelper.Callback<ImportKeyringParcel, ImportKeyResult> {
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({REQUEST_QR_FINGERPRINT, REQUEST_BACKUP, REQUEST_CERTIFY, REQUEST_DELETE})
    private @interface RequestType {
    }

    static final int REQUEST_QR_FINGERPRINT = 1;
    static final int REQUEST_BACKUP = 2;
    static final int REQUEST_CERTIFY = 3;
    static final int REQUEST_DELETE = 4;

    public static final String EXTRA_MASTER_KEY_ID = "master_key_id";
    public static final String EXTRA_DISPLAY_RESULT = "display_result";
    public static final String EXTRA_LINKED_TRANSITION = "linked_transition";

    KeyRepository keyRepository;

    // For CryptoOperationHelper.Callback
    private CryptoOperationHelper<ImportKeyringParcel, ImportKeyResult> importOpHelper;
    private CryptoOperationHelper<ChangeUnlockParcel, EditKeyResult> editOpHelper;
    private ChangeUnlockParcel changeUnlockParcel;

    private TextView statusText;
    private ImageView statusImage;
    private AppBarLayout appBarLayout;
    private CollapsingToolbarLayout collapsingToolbarLayout;

    private ImageButton actionEncryptFile;
    private ImageButton actionEncryptText;
    private FloatingActionButton floatingActionButton;
    private ImageView photoView;
    private FrameLayout photoLayout;
    private ImageView qrCodeView;
    private CardView qrCodeLayout;

    private byte[] qrCodeLoaded;

    private UnifiedKeyInfo unifiedKeyInfo;

    private MenuItem refreshItem;
    private boolean isRefreshing;
    private Animation rotate, rotateSpin;
    private View refreshView;

    public static Intent getViewKeyActivityIntent(@NonNull Context context, long masterKeyId) {
        Intent viewIntent = new Intent(context, ViewKeyActivity.class);
        viewIntent.putExtra(ViewKeyActivity.EXTRA_MASTER_KEY_ID, masterKeyId);
        return viewIntent;
    }

    @SuppressLint("InflateParams")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        keyRepository = KeyRepository.create(this);
        importOpHelper = new CryptoOperationHelper<>(1, this, this, null);

        setTitle(null);

        statusText = findViewById(R.id.view_key_status);
        statusImage = findViewById(R.id.view_key_status_image);
        appBarLayout = findViewById(R.id.app_bar_layout);
        collapsingToolbarLayout = findViewById(R.id.collapsing_toolbar);

        actionEncryptFile = findViewById(R.id.view_key_action_encrypt_files);
        actionEncryptText = findViewById(R.id.view_key_action_encrypt_text);
        floatingActionButton = findViewById(R.id.fab);
        photoView = findViewById(R.id.view_key_photo);
        photoLayout = findViewById(R.id.view_key_photo_layout);
        qrCodeView = findViewById(R.id.view_key_qr_code);
        qrCodeLayout = findViewById(R.id.view_key_qr_code_layout);

        rotateSpin = AnimationUtils.loadAnimation(this, R.anim.rotate_spin);

        //ContentDescriptionHint Listeners implemented

        ContentDescriptionHint.setup(actionEncryptFile);
        ContentDescriptionHint.setup(actionEncryptText);
        ContentDescriptionHint.setup(floatingActionButton);


        rotateSpin.setAnimationListener(new AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                refreshItem.getActionView().clearAnimation();
                refreshItem.setActionView(null);
                refreshItem.setEnabled(true);

                // this is a deferred call
                supportInvalidateOptionsMenu();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        rotate = AnimationUtils.loadAnimation(this, R.anim.rotate);
        rotate.setRepeatCount(Animation.INFINITE);
        rotate.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {

            }

            @Override
            public void onAnimationRepeat(Animation animation) {
                if (!isRefreshing) {
                    refreshItem.getActionView().clearAnimation();
                    refreshItem.getActionView().startAnimation(rotateSpin);
                }
            }
        });
        refreshView = getLayoutInflater().inflate(R.layout.indeterminate_progress, null);

        long masterKeyId;
        Intent intent = getIntent();
        Uri dataUri = intent.getData();
        if (intent.hasExtra(EXTRA_MASTER_KEY_ID)) {
            masterKeyId = intent.getLongExtra(EXTRA_MASTER_KEY_ID, 0L);
        } else if (dataUri != null && dataUri.getHost().equals(ContactsContract.AUTHORITY)) {
            Long contactMasterKeyId = new ContactHelper(this).masterKeyIdFromContactsDataUri(dataUri);
            if (contactMasterKeyId == null) {
                Timber.e("Contact Data missing. Should be uri of key!");
                Toast.makeText(this, R.string.error_contacts_key_id_missing, Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            masterKeyId = contactMasterKeyId;
        } else {
            throw new IllegalArgumentException("Missing required extra master_key_id or contact uri");
        }

        actionEncryptFile.setOnClickListener(v -> encrypt(false));
        actionEncryptText.setOnClickListener(v -> encrypt(true));

        floatingActionButton.setOnClickListener(v -> {
            if (unifiedKeyInfo.has_any_secret()) {
                startSafeSlinger();
            } else {
                scanQrCode();
            }
        });

        qrCodeLayout.setOnClickListener(v -> showQrCodeDialog());

        UnifiedKeyInfoViewModel viewModel = ViewModelProviders.of(this).get(UnifiedKeyInfoViewModel.class);
        viewModel.setMasterKeyId(masterKeyId);
        viewModel.getUnifiedKeyInfoLiveData(getApplicationContext()).observe(this, this::onLoadUnifiedKeyInfo);

        if (savedInstanceState == null && intent.hasExtra(EXTRA_DISPLAY_RESULT)) {
            OperationResult result = intent.getParcelableExtra(EXTRA_DISPLAY_RESULT);
            result.createNotify(this).show();
        }

        // Fragments are stored, no need to recreate those
        if (savedInstanceState != null) {
            return;
        }

        FragmentManager manager = getSupportFragmentManager();

        ViewKeyFragment frag = ViewKeyFragment.newInstance();
        manager.beginTransaction().replace(R.id.view_key_fragment, frag, "view_key_fragment").commit();

        if (Preferences.getPreferences(this).getExperimentalEnableKeybase()) {
            final ViewKeyKeybaseFragment keybaseFrag = ViewKeyKeybaseFragment.newInstance();
            manager.beginTransaction().replace(R.id.view_key_keybase_fragment, keybaseFrag).commit();
        }
    }

    @Override
    protected void initLayout() {
        setContentView(R.layout.view_key_activity);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.key_view, menu);
        refreshItem = menu.findItem(R.id.menu_key_view_refresh);
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
                startPassphraseActivity(REQUEST_BACKUP);
                return true;
            }
            case R.id.menu_key_view_skt: {
                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra(MainActivity.EXTRA_INIT_FRAG, MainActivity.ID_TRANSFER);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;
            }
            case R.id.menu_key_view_delete: {
                deleteKey();
                return true;
            }
            case R.id.menu_key_view_advanced: {
                Intent advancedIntent = new Intent(this, ViewKeyAdvActivity.class);
                advancedIntent.putExtra(ViewKeyAdvActivity.EXTRA_MASTER_KEY_ID, unifiedKeyInfo.master_key_id());
                startActivity(advancedIntent);
                return true;
            }
            case R.id.menu_key_view_refresh: {
                updateFromKeyserver();
                return true;
            }
            case R.id.menu_key_view_certify_fingerprint: {
                certifyFingerprint();
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (unifiedKeyInfo == null) {
            return false;
        }
        MenuItem backupKey = menu.findItem(R.id.menu_key_view_backup);
        backupKey.setVisible(unifiedKeyInfo.has_any_secret());
        menu.findItem(R.id.menu_key_view_skt).setVisible(unifiedKeyInfo.has_any_secret());
        MenuItem changePassword = menu.findItem(R.id.menu_key_change_password);
        changePassword.setVisible(unifiedKeyInfo.has_any_secret());

        MenuItem certifyFingerprint = menu.findItem(R.id.menu_key_view_certify_fingerprint);
        certifyFingerprint.setVisible(
                !unifiedKeyInfo.has_any_secret() && !unifiedKeyInfo.is_verified() && !unifiedKeyInfo.is_expired() &&
                        !unifiedKeyInfo.is_revoked());

        return true;
    }

    private void changePassword() {
        CryptoOperationHelper.Callback<ChangeUnlockParcel, EditKeyResult> editKeyCallback
                = new CryptoOperationHelper.Callback<ChangeUnlockParcel, EditKeyResult>() {
            @Override
            public ChangeUnlockParcel createOperationInput() {
                return changeUnlockParcel;
            }

            @Override
            public void onCryptoOperationSuccess(EditKeyResult result) {
                displayResult(result);
                long masterKeyId = unifiedKeyInfo.master_key_id();
                PassphraseCacheService.clearCachedPassphrase(getApplicationContext(), masterKeyId, masterKeyId);
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

        editOpHelper = new CryptoOperationHelper<>(2, this, editKeyCallback, R.string.progress_building_key);

        // Message is received after passphrase is cached
        Handler returnHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                if (message.what == SetPassphraseDialogFragment.MESSAGE_OKAY) {
                    Bundle data = message.getData();

                    // use new passphrase!
                    changeUnlockParcel = ChangeUnlockParcel.createChangeUnlockParcel(
                            unifiedKeyInfo.master_key_id(), unifiedKeyInfo.fingerprint(),
                            data.getParcelable(SetPassphraseDialogFragment.MESSAGE_NEW_PASSPHRASE)
                    );

                    editOpHelper.cryptoOperation();
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

    private void certifyFingerprint() {
        Intent intent = new Intent(this, CertifyFingerprintActivity.class);
        intent.putExtra(CertifyFingerprintActivity.EXTRA_MASTER_KEY_ID, unifiedKeyInfo.master_key_id());

        startActivityForResult(intent, REQUEST_CERTIFY);
    }

    private void certifyImmediate() {
        Intent intent = new Intent(this, CertifyKeyActivity.class);
        intent.putExtra(CertifyKeyActivity.EXTRA_KEY_IDS, new long[] { unifiedKeyInfo.master_key_id() });

        startActivityForResult(intent, REQUEST_CERTIFY);
    }

    private void showQrCodeDialog() {
        Intent qrCodeIntent = new Intent(this, QrCodeViewActivity.class);

        // create the transition animation - the images in the layouts
        // of both activities are defined with android:transitionName="qr_code"
        Bundle opts = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ActivityOptions options = ActivityOptions
                    .makeSceneTransitionAnimation(this, qrCodeLayout, "qr_code");
            opts = options.toBundle();
        }

        qrCodeIntent.putExtra(QrCodeViewActivity.EXTRA_MASTER_KEY_ID, unifiedKeyInfo.master_key_id());
        ActivityCompat.startActivity(this, qrCodeIntent, opts);
    }

    private void startPassphraseActivity(int requestCode) {

        if (keyHasPassphrase()) {
            Intent intent = new Intent(this, PassphraseDialogActivity.class);
            long masterKeyId = unifiedKeyInfo.master_key_id();
            RequiredInputParcel requiredInput =
                    RequiredInputParcel.createRequiredDecryptPassphrase(masterKeyId, masterKeyId);
            requiredInput.mSkipCaching = true;
            intent.putExtra(PassphraseDialogActivity.EXTRA_REQUIRED_INPUT, requiredInput);
            startActivityForResult(intent, requestCode);
        } else {
            startBackupActivity();
        }
    }

    private boolean keyHasPassphrase() {
        try {
            long masterKeyId = unifiedKeyInfo.master_key_id();
            SecretKeyType secretKeyType = keyRepository.getSecretKeyType(masterKeyId);
            switch (secretKeyType) {
                // all of these make no sense to ask
                case PASSPHRASE_EMPTY:
                case GNU_DUMMY:
                case DIVERT_TO_CARD:
                case UNAVAILABLE:
                    return false;
                default:
                    return true;
            }
        } catch (NotFoundException e) {
            return false;
        }
    }

    private void startBackupActivity() {
        Intent intent = new Intent(this, BackupActivity.class);
        intent.putExtra(BackupActivity.EXTRA_MASTER_KEY_IDS, new long[] { unifiedKeyInfo.master_key_id() });
        intent.putExtra(BackupActivity.EXTRA_SECRET, true);
        startActivity(intent);
    }

    private void deleteKey() {
        Intent deleteIntent = new Intent(this, DeleteKeyDialogActivity.class);

        deleteIntent.putExtra(DeleteKeyDialogActivity.EXTRA_DELETE_MASTER_KEY_IDS,
                new long[]{ unifiedKeyInfo.master_key_id() });
        deleteIntent.putExtra(DeleteKeyDialogActivity.EXTRA_HAS_SECRET, unifiedKeyInfo.has_any_secret());
        if (unifiedKeyInfo.has_any_secret()) {
            // for upload in case key is secret
            deleteIntent.putExtra(DeleteKeyDialogActivity.EXTRA_KEYSERVER,
                    Preferences.getPreferences(this).getPreferredKeyserver());
        }

        startActivityForResult(deleteIntent, REQUEST_DELETE);
    }

    @Override
    protected void onActivityResult(@RequestType int requestCode, int resultCode, Intent data) {
        if (importOpHelper.handleActivityResult(requestCode, resultCode, data)) {
            return;
        }
        if (editOpHelper != null) {
            editOpHelper.handleActivityResult(requestCode, resultCode, data);
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

                byte[] fingerprint = data.getByteArrayExtra(ImportKeysProxyActivity.EXTRA_FINGERPRINT);
                if (fingerprint == null) {
                    Notify.create(this, R.string.error_scan_fp, Notify.LENGTH_LONG, Style.ERROR).show();
                    return;
                }
                if (Arrays.equals(unifiedKeyInfo.fingerprint(), fingerprint)) {
                    certifyImmediate();
                } else {
                    Notify.create(this, R.string.error_scan_match, Notify.LENGTH_LONG, Style.ERROR).show();
                }
                return;
            }

            case REQUEST_BACKUP: {
                startBackupActivity();
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
    protected void onSecurityTokenPostExecute(SecurityTokenConnection stConnection) {
        super.onSecurityTokenPostExecute(stConnection);
        finish();
    }

    private void encrypt(boolean text) {
        // If there is no encryption key, don't bother.
        if (!unifiedKeyInfo.has_encrypt_key()) {
            Notify.create(this, R.string.error_no_encrypt_subkey, Notify.Style.ERROR).show();
            return;
        }
        long[] encryptionKeyIds = new long[] { unifiedKeyInfo.master_key_id() };
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
    }

    private void startSafeSlinger() {
        Intent safeSlingerIntent = new Intent(this, SafeSlingerActivity.class);
        safeSlingerIntent.putExtra(SafeSlingerActivity.EXTRA_MASTER_KEY_ID, unifiedKeyInfo.master_key_id());
        startActivityForResult(safeSlingerIntent, 0);
    }

    /**
     * Load QR Code asynchronously and with a fade in animation
     */
    private void loadQrCode(final byte[] fingerprint) {
        AsyncTask<Void, Void, Bitmap> loadTask =
                new AsyncTask<Void, Void, Bitmap>() {
                    protected Bitmap doInBackground(Void... unused) {
                        String fingerprintStr = KeyFormattingUtils.convertFingerprintToHex(fingerprint);
                        Uri uri = new Uri.Builder()
                                .scheme(Constants.FINGERPRINT_SCHEME)
                                .opaquePart(fingerprintStr)
                                .build();
                        // render with minimal size
                        return QrCodeUtils.getQRCodeBitmap(uri, 0);
                    }

                    protected void onPostExecute(Bitmap qrCode) {
                        qrCodeLoaded = fingerprint;
                        // scale the image up to our actual size. we do this in code rather
                        // than let the ImageView do this because we don't require filtering.
                        Bitmap scaled = Bitmap.createScaledBitmap(qrCode,
                                ViewKeyActivity.this.qrCodeView.getHeight(), ViewKeyActivity.this.qrCodeView.getHeight(),
                                false);
                        ViewKeyActivity.this.qrCodeView.setImageBitmap(scaled);

                        // simple fade-in animation
                        AlphaAnimation anim = new AlphaAnimation(0.0f, 1.0f);
                        anim.setDuration(200);
                        ViewKeyActivity.this.qrCodeView.startAnimation(anim);
                    }
                };

        loadTask.execute();
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

    private void onLoadUnifiedKeyInfo(UnifiedKeyInfo unifiedKeyInfo) {
        if (unifiedKeyInfo == null) {
            return;
        }

        this.unifiedKeyInfo = unifiedKeyInfo;

        String name = unifiedKeyInfo.name();
        boolean isAnonymousKey = name == null && unifiedKeyInfo.email() == null;
        if (isAnonymousKey) {
            String readableKeyId = KeyFormattingUtils.beautifyKeyId(unifiedKeyInfo.master_key_id());
            collapsingToolbarLayout.setTitle(readableKeyId);
        } else {
            collapsingToolbarLayout.setTitle(name != null ? name : getString(R.string.user_id_no_name));
        }

        // if the refresh animation isn't playing
        if (!rotate.hasStarted() && !rotateSpin.hasStarted()) {
            // re-create options menu based on isSecret, isVerified
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

                        photoView.setImageBitmap(photo);
                        photoView.setColorFilter(ContextCompat.getColor(ViewKeyActivity.this, R.color.toolbar_photo_tint),
                                PorterDuff.Mode.SRC_ATOP);
                        photoLayout.setVisibility(View.VISIBLE);
                    }
                };

        boolean showStatusText = unifiedKeyInfo.is_secure() && !unifiedKeyInfo.is_expired() && !unifiedKeyInfo.is_revoked();
        if (showStatusText) {
            statusText.setVisibility(View.VISIBLE);

            if (unifiedKeyInfo.has_any_secret()) {
                statusText.setText(R.string.view_key_my_key);
            } else if (unifiedKeyInfo.is_verified()) {
                statusText.setText(R.string.view_key_verified);
            } else {
                statusText.setText(R.string.view_key_unverified);
            }
        } else {
            statusText.setVisibility(View.GONE);
        }

        // Note: order is important
        int color;
        if (unifiedKeyInfo.is_revoked()) {
            statusImage.setVisibility(View.VISIBLE);
            KeyFormattingUtils.setStatusImage(this, statusImage, statusText,
                    State.REVOKED, R.color.icons, true);
            color = ContextCompat.getColor(this, R.color.key_flag_red);

            actionEncryptFile.setVisibility(View.INVISIBLE);
            actionEncryptText.setVisibility(View.INVISIBLE);
            hideFab();
            qrCodeLayout.setVisibility(View.GONE);
        } else if (unifiedKeyInfo.is_expired()) {
            statusImage.setVisibility(View.VISIBLE);
            KeyFormattingUtils.setStatusImage(this, statusImage, statusText,
                    State.EXPIRED, R.color.icons, true);
            color = ContextCompat.getColor(this, R.color.key_flag_red);

            actionEncryptFile.setVisibility(View.INVISIBLE);
            actionEncryptText.setVisibility(View.INVISIBLE);
            hideFab();
            qrCodeLayout.setVisibility(View.GONE);
        } else if (!unifiedKeyInfo.is_secure()) {
            statusImage.setVisibility(View.VISIBLE);
            KeyFormattingUtils.setStatusImage(this, statusImage, statusText,
                    State.INSECURE, R.color.icons, true);
            color = ContextCompat.getColor(this, R.color.key_flag_red);

            actionEncryptFile.setVisibility(View.INVISIBLE);
            actionEncryptText.setVisibility(View.INVISIBLE);
            hideFab();
            qrCodeLayout.setVisibility(View.GONE);
        } else if (unifiedKeyInfo.has_any_secret()) {
            statusImage.setVisibility(View.GONE);
            color = ContextCompat.getColor(this, R.color.key_flag_green);
            // reload qr code only if the fingerprint changed
            if (!Arrays.equals(unifiedKeyInfo.fingerprint(), qrCodeLoaded)) {
                loadQrCode(unifiedKeyInfo.fingerprint());
            }
            photoTask.execute(unifiedKeyInfo.master_key_id());
            qrCodeLayout.setVisibility(View.VISIBLE);

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
                    statusText.getLayoutParams();
            statusParams.setMargins(FormattingUtils.dpToPx(this, 48), 0, 0, 0);
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                statusParams.setMarginEnd(0);
            }
            statusParams.addRule(RelativeLayout.LEFT_OF, R.id.view_key_qr_code_layout);
            statusText.setLayoutParams(statusParams);

            actionEncryptFile.setVisibility(View.VISIBLE);
            actionEncryptText.setVisibility(View.VISIBLE);

            showFab();
            floatingActionButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_repeat_white_24dp));
        } else {
            actionEncryptFile.setVisibility(View.VISIBLE);
            actionEncryptText.setVisibility(View.VISIBLE);
            qrCodeLayout.setVisibility(View.GONE);

            if (unifiedKeyInfo.is_verified()) {
                statusText.setText(R.string.view_key_verified);
                statusImage.setVisibility(View.VISIBLE);
                KeyFormattingUtils.setStatusImage(this, statusImage, statusText,
                        State.VERIFIED, R.color.icons, true);
                color = ContextCompat.getColor(this, R.color.key_flag_green);
                photoTask.execute(unifiedKeyInfo.master_key_id());

                hideFab();
            } else {
                statusText.setText(R.string.view_key_unverified);
                statusImage.setVisibility(View.VISIBLE);
                KeyFormattingUtils.setStatusImage(this, statusImage, statusText,
                        State.UNVERIFIED, R.color.icons, true);
                color = ContextCompat.getColor(this, R.color.key_flag_orange);

                showFab();
            }
        }

        if (mPreviousColor == 0 || mPreviousColor == color) {
            appBarLayout.setBackgroundColor(color);
            collapsingToolbarLayout.setContentScrimColor(color);
            collapsingToolbarLayout.setStatusBarScrimColor(getStatusBarBackgroundColor(color));
            mPreviousColor = color;
        } else {
            ObjectAnimator colorFade =
                    ObjectAnimator.ofObject(appBarLayout, "backgroundColor",
                            new ArgbEvaluator(), mPreviousColor, color);
            collapsingToolbarLayout.setContentScrimColor(color);
            collapsingToolbarLayout.setStatusBarScrimColor(getStatusBarBackgroundColor(color));

            colorFade.setDuration(1200);
            colorFade.start();
            mPreviousColor = color;
        }

        statusImage.setAlpha(80);
    }

    /**
     * Helper to show Fab, from http://stackoverflow.com/a/31047038
     */
    private void showFab() {
        CoordinatorLayout.LayoutParams p = (CoordinatorLayout.LayoutParams) floatingActionButton.getLayoutParams();
        p.setBehavior(new FloatingActionButton.Behavior());
        p.setAnchorId(R.id.app_bar_layout);
        floatingActionButton.setLayoutParams(p);
        floatingActionButton.setVisibility(View.VISIBLE);
    }

    /**
     * Helper to hide Fab, from http://stackoverflow.com/a/31047038
     */
    private void hideFab() {
        CoordinatorLayout.LayoutParams p = (CoordinatorLayout.LayoutParams) floatingActionButton.getLayoutParams();
        p.setBehavior(null); //should disable default animations
        p.setAnchorId(View.NO_ID); //should let you set visibility
        floatingActionButton.setLayoutParams(p);
        floatingActionButton.setVisibility(View.GONE);
    }

    // CryptoOperationHelper.Callback functions


    private void updateFromKeyserver() {
        if (unifiedKeyInfo == null) {
            return;
        }

        isRefreshing = true;
        refreshItem.setEnabled(false);
        refreshItem.setActionView(refreshView);
        refreshView.startAnimation(rotate);

        importOpHelper.cryptoOperation();
    }

    @Override
    public ImportKeyringParcel createOperationInput() {
        HkpKeyserverAddress preferredKeyserver = Preferences.getPreferences(this).getPreferredKeyserver();

        ParcelableKeyRing keyEntry = ParcelableKeyRing.createFromReference(unifiedKeyInfo.fingerprint(), null, null, null);

        return ImportKeyringParcel.createImportKeyringParcel(Collections.singletonList(keyEntry), preferredKeyserver);
    }

    @Override
    public void onCryptoOperationSuccess(ImportKeyResult result) {
        isRefreshing = false;
        result.createNotify(this).show();
    }

    @Override
    public void onCryptoOperationCancelled() {
        isRefreshing = false;
    }

    @Override
    public void onCryptoOperationError(ImportKeyResult result) {
        isRefreshing = false;
        result.createNotify(this).show();
    }

    @Override
    public boolean onCryptoSetProgress(String msg, int progress, int max) {
        return true;
    }

}

