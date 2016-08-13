package org.sufficientlysecure.keychain.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.keyimport.ParcelableKeyRing;
import org.sufficientlysecure.keychain.operations.MigrateSymmetricOperation;
import org.sufficientlysecure.keychain.operations.results.MigrateSymmetricResult;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing;
import org.sufficientlysecure.keychain.pgp.UncachedPublicKey;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.service.MigrateSymmetricInputParcel;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.service.input.RequiredInputParcel;
import org.sufficientlysecure.keychain.ui.base.CryptoOperationHelper;
import org.sufficientlysecure.keychain.ui.dialog.CustomAlertDialogBuilder;
import org.sufficientlysecure.keychain.ui.passphrasedialog.PassphraseDialogActivity;
import org.sufficientlysecure.keychain.ui.util.ThemeChanger;
import org.sufficientlysecure.keychain.util.KeyringPassphrases;
import org.sufficientlysecure.keychain.util.KeyringPassphrases.SubKeyInfo;
import org.sufficientlysecure.keychain.util.ParcelableFileCache;
import org.sufficientlysecure.keychain.util.Passphrase;
import org.sufficientlysecure.keychain.util.Preferences;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Migrate consists of a consolidate, caching secret keyrings to a file, and actual migration
 * The tasks are run one after another, to prevent bugs when displaying the progress dialog
 *
 * Caching is required to provide a reliable source for keyrings when migrating.
 * The cache is created only if migration has not started.
 * The previously built cache is used otherwise
 */
public class MigrateSymmetricActivity extends FragmentActivity {
    private static final int REQUEST_REPEAT_PASSPHRASE = 0x00007008;
    private static final int REQUEST_CONSOLIDATE = 0x00007009;

    private List<KeyringPassphrases> mPassphrasesList;
    private Iterator<SubKeyInfo> mSubKeysForRepeatAskPassphrase;
    private CryptoOperationHelper mCryptoOpHelper;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPassphrasesList = new ArrayList<>();
        setContentView(R.layout.migrate_symmetric_activity);

        Preferences prefs = Preferences.getPreferences(this);
        if (!prefs.isPartiallyMigrated()) {
            Intent consolidateIntent = new Intent(this, ConsolidateDialogActivity.class);
            consolidateIntent.putExtra(ConsolidateDialogActivity.EXTRA_CONSOLIDATE_RECOVERY, false);
            startActivityForResult(consolidateIntent, REQUEST_CONSOLIDATE);
        } else {
            showDialog();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    private void showDialog() {
        final ContextThemeWrapper theme = ThemeChanger.getDialogThemeWrapper(this);
        CustomAlertDialogBuilder dialog = new CustomAlertDialogBuilder(theme);
        LayoutInflater inflater = LayoutInflater.from(theme);
        @SuppressLint("InflateParams") // for dialog's use
        View view = inflater.inflate(R.layout.migrate_symmetric_dialog, null);

        dialog.setView(view)
                .setCancelable(false)
                .setTitle(R.string.title_migrate_symmetric)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        ArrayList<SubKeyInfo> subKeyInfos = new ArrayList<>();
                        try {
                            ParcelableFileCache<ParcelableKeyRing> secretKeyRings =
                                    new ParcelableFileCache<>(getApplication(), MigrateSymmetricOperation.CACHE_FILE_NAME);
                            ParcelableFileCache.IteratorWithSize<ParcelableKeyRing> itSecrets = secretKeyRings.readCache(false);
                            while(itSecrets.hasNext()) {
                                ParcelableKeyRing ring = itSecrets.next();
                                subKeyInfos.addAll(getSubKeyInfosFromRing(UncachedKeyRing.decodeFromData(ring.mBytes)));
                            }
                        } catch (IOException | PgpGeneralException e) {
                            // not a problem, cache will be created when dialog is started again
                            Toast.makeText(getApplicationContext(),
                                    R.string.migrate_error_accessing_cache, Toast.LENGTH_SHORT).show();
                            ActivityCompat.finishAffinity(MigrateSymmetricActivity.this);
                        }

                        mSubKeysForRepeatAskPassphrase = subKeyInfos.iterator();
                        if (mSubKeysForRepeatAskPassphrase.hasNext()) {
                            startPassphraseActivity();
                        } else {
                            startMigration(new ArrayList<KeyringPassphrases>());
                        }
                    }
                }).show();
    }

    private List<SubKeyInfo> getSubKeyInfosFromRing(UncachedKeyRing ring) throws IOException {
        ArrayList<SubKeyInfo> list = new ArrayList<>();
        for (Iterator<UncachedPublicKey> keyIt = ring.getPublicKeys(); keyIt.hasNext(); ) {
            UncachedPublicKey key = keyIt.next();
            list.add(new SubKeyInfo(ring.getMasterKeyId(),
                    key.getKeyId(),
                    new ParcelableKeyRing(ring.getEncoded())));
        }
        return list;
    }

    private void createCache() {
        CryptoOperationHelper.Callback<MigrateSymmetricInputParcel.CreateSecretCacheParcel, MigrateSymmetricResult> callback =
                new CryptoOperationHelper.Callback<MigrateSymmetricInputParcel.CreateSecretCacheParcel, MigrateSymmetricResult>() {
                    Activity activity = MigrateSymmetricActivity.this;

                    @Override
                    public MigrateSymmetricInputParcel.CreateSecretCacheParcel createOperationInput() {
                        return new MigrateSymmetricInputParcel.CreateSecretCacheParcel();
                    }

                    @Override
                    public void onCryptoOperationSuccess(MigrateSymmetricResult result) {
                        Preferences.getPreferences(activity).setPartiallyMigrated(true);
                        showDialog();
                    }

                    @Override
                    public void onCryptoOperationCancelled() {
                        Toast.makeText(activity.getApplicationContext(),
                                R.string.migrate_error_cancelled, Toast.LENGTH_SHORT).show();
                        ActivityCompat.finishAffinity(activity);
                    }

                    @Override
                    public void onCryptoOperationError(MigrateSymmetricResult result) {
                        Toast.makeText(activity.getApplicationContext(),
                                R.string.migrate_error_cache_keys, Toast.LENGTH_SHORT).show();
                        ActivityCompat.finishAffinity(activity);
                    }

                    @Override
                    public boolean onCryptoSetProgress(String msg, int progress, int max) {
                        return false;
                    }
                };

        mCryptoOpHelper =
                new CryptoOperationHelper<>(1, this, callback, R.string.progress_migrate_cache_keys);

        mCryptoOpHelper.cryptoOperation();
    }


    private void startPassphraseActivity() {
        SubKeyInfo keyInfo = mSubKeysForRepeatAskPassphrase.next();
        ParcelableKeyRing parcelableKeyRing = keyInfo.mKeyRing;
        long subKeyId = keyInfo.mSubKeyId;
        long masterKeyId = keyInfo.mMasterKeyId;

        Intent intent = new Intent(this, PassphraseDialogActivity.class);

        // try using last entered passphrase if appropriate
        if (!mPassphrasesList.isEmpty()) {
            KeyringPassphrases prevKeyring = mPassphrasesList.get(mPassphrasesList.size() - 1);
            Passphrase passphrase = prevKeyring.getSingleSubkeyPassphrase();

            boolean sameMasterKey = masterKeyId == prevKeyring.mMasterKeyId;
            boolean prevSubKeysHaveSamePassphrase = prevKeyring.subKeysHaveSamePassphrase();
            if(sameMasterKey && prevSubKeysHaveSamePassphrase) {
                intent.putExtra(PassphraseDialogActivity.EXTRA_PASSPHRASE_TO_TRY, passphrase);
            }
        }

        RequiredInputParcel requiredInput = RequiredInputParcel.
                createRequiredImportKeyPassphrase(masterKeyId, subKeyId, parcelableKeyRing);
        requiredInput.mSkipCaching = true;
        intent.putExtra(PassphraseDialogActivity.EXTRA_REQUIRED_INPUT, requiredInput);
        startActivityForResult(intent, REQUEST_REPEAT_PASSPHRASE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONSOLIDATE : {
                if (resultCode != Activity.RESULT_OK) {
                    Toast.makeText(this, R.string.migrate_error_consolidating,
                            Toast.LENGTH_SHORT).show();
                    ActivityCompat.finishAffinity(this);
                    return;
                }

                if (!Preferences.getPreferences(getApplicationContext()).isPartiallyMigrated()) {
                    createCache();
                } else {
                    showDialog();
                }
                return;
            }
            case REQUEST_REPEAT_PASSPHRASE : {
                if (resultCode != Activity.RESULT_OK) {
                    // TODO: give the user a shortcut to clear db & start anew instead?
                    Toast.makeText(this, R.string.migrate_cancelled_dialog,
                            Toast.LENGTH_SHORT).show();
                    showDialog();
                    return;
                }

                RequiredInputParcel requiredParcel = data.getParcelableExtra(PassphraseDialogActivity.EXTRA_REQUIRED_INPUT);
                CryptoInputParcel cryptoParcel = data.getParcelableExtra(PassphraseDialogActivity.RESULT_CRYPTO_INPUT);
                long masterKeyId = requiredParcel.getMasterKeyId();
                long subKeyId = requiredParcel.getSubKeyId();
                Passphrase passphrase = cryptoParcel.getPassphrase();

                // save passphrase if one is returned
                // could be stripped or diverted to card otherwise
                if(passphrase != null) {
                    boolean isNewKeyRing = (mPassphrasesList.isEmpty() ||
                            mPassphrasesList.get(mPassphrasesList.size() - 1).mMasterKeyId != masterKeyId);

                    if (isNewKeyRing) {
                        KeyringPassphrases newKeyring = new KeyringPassphrases(masterKeyId, null);
                        newKeyring.mSubkeyPassphrases.put(subKeyId, passphrase);
                        mPassphrasesList.add(newKeyring);
                    } else {
                        KeyringPassphrases prevKeyring = mPassphrasesList.get(mPassphrasesList.size() - 1);
                        prevKeyring.mSubkeyPassphrases.put(subKeyId, passphrase);
                    }
                }

                // check next subkey
                if (mSubKeysForRepeatAskPassphrase.hasNext()) {
                    startPassphraseActivity();
                } else {
                    startMigration(mPassphrasesList);
                }

                return;
            }
            default: {
                super.onActivityResult(requestCode, resultCode, data);
                if (mCryptoOpHelper != null) {
                    mCryptoOpHelper.handleActivityResult(requestCode, resultCode, data);
                }
            }
        }
    }

    private void startMigration(final List<KeyringPassphrases> passphrasesList) {
        CryptoOperationHelper.Callback<MigrateSymmetricInputParcel, MigrateSymmetricResult> callback =
                new CryptoOperationHelper.Callback<MigrateSymmetricInputParcel, MigrateSymmetricResult>() {
                    Activity activity = MigrateSymmetricActivity.this;

                    @Override
                    public MigrateSymmetricInputParcel createOperationInput() {
                        return new MigrateSymmetricInputParcel(passphrasesList);
                    }

                    @Override
                    public void onCryptoOperationSuccess(MigrateSymmetricResult result) {
                        finishSuccessfulMigration();
                    }

                    @Override
                    public void onCryptoOperationCancelled() {
                        Toast.makeText(activity.getApplicationContext(),
                                R.string.migrate_error_cancelled, Toast.LENGTH_SHORT).show();
                        ActivityCompat.finishAffinity(activity);
                    }

                    @Override
                    public void onCryptoOperationError(MigrateSymmetricResult result) {
                        Toast.makeText(activity.getApplicationContext(),
                                R.string.migrate_error_migrating, Toast.LENGTH_SHORT).show();
                        ActivityCompat.finishAffinity(activity);
                    }

                    @Override
                    public boolean onCryptoSetProgress(String msg, int progress, int max) {
                        return false;
                    }
                };
        mCryptoOpHelper =
                new CryptoOperationHelper<>(1, this, callback, R.string.progress_migrating);

        mCryptoOpHelper.cryptoOperation();
    }

    private void finishSuccessfulMigration() {
        Preferences.getPreferences(this).setUsingS2k(false);
        Preferences.getPreferences(this).setPartiallyMigrated(false);

        // show the main activity to indicate success
        Intent mainActivityIntent = new Intent(this, MainActivity.class);
        mainActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        // TODO: show success on snackbar
        startActivity(mainActivityIntent);
        finish();
    }

}
