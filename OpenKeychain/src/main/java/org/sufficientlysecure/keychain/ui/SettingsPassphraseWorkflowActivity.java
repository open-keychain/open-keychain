package org.sufficientlysecure.keychain.ui;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import android.widget.Toast;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.ChangePassphraseWorkflowOperation;
import org.sufficientlysecure.keychain.operations.results.ChangePassphraseWorkflowResult;
import org.sufficientlysecure.keychain.operations.results.CreateSecretKeyRingCacheResult;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKeyRing.SecretKeyRingType;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.provider.ProviderReader;
import org.sufficientlysecure.keychain.service.ChangePassphraseWorkflowParcel;
import org.sufficientlysecure.keychain.service.CreateSecretKeyRingCacheParcel;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.service.input.RequiredInputParcel;
import org.sufficientlysecure.keychain.ui.base.CryptoOperationHelper;
import org.sufficientlysecure.keychain.ui.passphrasedialog.PassphraseDialogActivity;
import org.sufficientlysecure.keychain.util.Passphrase;
import org.sufficientlysecure.keychain.util.Preferences;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class SettingsPassphraseWorkflowActivity extends AppCompatActivity {
    public static final String EXTRA_TO_SINGLE_PASSPHRASE_WORKFLOW = "to_single_passphrase_workflow";

    private static final int REQUEST_REPEAT_ASK_PASSPHRASE = 1;
    private static final int REQUEST_MASTER_PASSPHRASE_ONLY = 2;
    private static final int REQUEST_NO_SECRET_KEYS = 3;
    private static final int REQUEST_MASTER_PASSPHRASE_THEN_REPEAT = 4;

    private HashMap<Long, Passphrase> mPassphrases;
    private Passphrase mMasterPassphrase;

    private Iterator<Long> mIdsForRepeatAskPassphrase;
    private boolean mFinishedCollectingPassphrases;
    private boolean mToSinglePassphraseWorkflow;
    private Preferences mPreferences;
    private CryptoOperationHelper mCryptoOpHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_passphrase_workflow_activity);

        if (savedInstanceState == null) {
            mPreferences = Preferences.getPreferences(this);
            Intent intent = getIntent();
            mToSinglePassphraseWorkflow =
                    intent.getBooleanExtra(EXTRA_TO_SINGLE_PASSPHRASE_WORKFLOW, false);

            long[] masterKeyIds = getOwnMasterKeyIds();
            mPassphrases = new HashMap<>();
            List<Long> keysNeedingPassphrases = getKeysRequiringPassphrases(masterKeyIds);

            mIdsForRepeatAskPassphrase = keysNeedingPassphrases.iterator();
            if (mIdsForRepeatAskPassphrase.hasNext()) {
                askForMasterPassphrase(mToSinglePassphraseWorkflow);
            } else {
                askForPassphraseNoSecretKeys();
            }
        }
    }

    // Just ask for the master passphrase
    private void askForPassphraseNoSecretKeys() {
        Intent intent = new Intent(this, PassphraseDialogActivity.class);
        RequiredInputParcel requiredInput =
                RequiredInputParcel.createRequiredAppLockPassphrase();
        requiredInput.mSkipCaching = true;
        intent.putExtra(PassphraseDialogActivity.EXTRA_REQUIRED_INPUT, requiredInput);
        startActivityForResult(intent, REQUEST_NO_SECRET_KEYS);

    }

    private long[] getOwnMasterKeyIds() {
        ArrayList<Long> idList = new ArrayList<>();
        Cursor cursor = getContentResolver().query(KeyRings.buildUnifiedKeyRingsUri(),
                new String[]{ KeyRings.MASTER_KEY_ID, KeyRings.HAS_SECRET },
                KeyRings.HAS_SECRET + " != 0", null, null);

        while (cursor != null && cursor.moveToNext()) {
            idList.add(cursor.getLong(0));
        }
        if (cursor != null) {
            cursor.close();
        }

        long[] idArray = new long[idList.size()];
        for (int i = 0; i < idList.size(); i++) {
            idArray[i] = idList.get(i);
        }
        return idArray;
    }

    private List<Long> getKeysRequiringPassphrases(long[] masterKeyIds) {
        ProviderHelper providerHelper = new ProviderHelper(this);

        ArrayList<Long> keysNeedingPassphrases = new ArrayList<>();

        for (long masterKeyId : masterKeyIds) {
            try {
                SecretKeyRingType secretKeyRingType =
                        providerHelper.read().getCachedPublicKeyRing(masterKeyId).getSecretKeyringType();
                switch (secretKeyRingType) {
                    case PASSPHRASE_EMPTY: {
                        mPassphrases.put(masterKeyId, new Passphrase());
                        continue;
                    }
                    case PASSPHRASE: {
                        keysNeedingPassphrases.add(masterKeyId);
                        continue;
                    }
                    case UNAVAILABLE: {
                        continue;
                    }
                    default: {
                        throw new AssertionError("Unhandled keyring type");
                    }
                }
            } catch (ProviderReader.NotFoundException e) {
                Toast.makeText(getApplicationContext(), R.string.msg_backup_error_db, Toast.LENGTH_SHORT).show();
                this.finish();
            }
        }
        return keysNeedingPassphrases;
    }

    private void askForMasterPassphrase(boolean toSinglePassphraseWorkflow) {
        Intent intent = new Intent(this, PassphraseDialogActivity.class);
        if (toSinglePassphraseWorkflow) {
            // multi to single

            RequiredInputParcel requiredInput =
                    RequiredInputParcel.createRequiredAppLockPassphrase();
            requiredInput.mSkipCaching = true;
            intent.putExtra(PassphraseDialogActivity.EXTRA_REQUIRED_INPUT, requiredInput);
            startActivityForResult(intent, REQUEST_MASTER_PASSPHRASE_THEN_REPEAT);

        } else {
            // single to multi, only need one passphrase

            RequiredInputParcel requiredInput =
                    RequiredInputParcel.createRequiredAppLockPassphrase();
            requiredInput.mSkipCaching = true;
            intent.putExtra(PassphraseDialogActivity.EXTRA_REQUIRED_INPUT, requiredInput);
            startActivityForResult(intent, REQUEST_MASTER_PASSPHRASE_ONLY);
        }

    }

    private void askForPassphrase(long masterKeyId) {
        Intent intent = new Intent(this, PassphraseDialogActivity.class);

        if (mToSinglePassphraseWorkflow) {
            // multi to single, need all passphrases

            RequiredInputParcel requiredInput =
                    RequiredInputParcel.createRequiredKeyringPassphrase(masterKeyId);
            requiredInput.mSkipCaching = true;
            intent.putExtra(PassphraseDialogActivity.EXTRA_REQUIRED_INPUT, requiredInput);
            startActivityForResult(intent, REQUEST_REPEAT_ASK_PASSPHRASE);

        } else {
            // single to multi, only need one passphrase

            RequiredInputParcel requiredInput =
                    RequiredInputParcel.createRequiredKeyringPassphrase(masterKeyId);
            requiredInput.mSkipCaching = true;
            intent.putExtra(PassphraseDialogActivity.EXTRA_REQUIRED_INPUT, requiredInput);
            startActivityForResult(intent, REQUEST_MASTER_PASSPHRASE_ONLY);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_NO_SECRET_KEYS: {
                if (resultCode != RESULT_OK) {
                    this.finish();
                    return;
                }
                mPreferences.setUsesSinglePassphraseWorkflow(mToSinglePassphraseWorkflow);
                finish();

                break;
            }
            case REQUEST_MASTER_PASSPHRASE_ONLY: {
                if (resultCode != RESULT_OK) {
                    this.finish();
                    return;
                }
                CryptoInputParcel cryptoResult =
                        data.getParcelableExtra(PassphraseDialogActivity.RESULT_CRYPTO_INPUT);
                mMasterPassphrase = cryptoResult.getPassphrase();
                mFinishedCollectingPassphrases = true;
                break;
            }
            case REQUEST_MASTER_PASSPHRASE_THEN_REPEAT: {
                if (resultCode != RESULT_OK) {
                    this.finish();
                    return;
                }
                CryptoInputParcel cryptoResult =
                        data.getParcelableExtra(PassphraseDialogActivity.RESULT_CRYPTO_INPUT);
                mMasterPassphrase = cryptoResult.getPassphrase();
                if (mIdsForRepeatAskPassphrase.hasNext()) {
                    askForPassphrase(mIdsForRepeatAskPassphrase.next());
                } else {
                    mFinishedCollectingPassphrases = true;
                }
                break;
            }
            case REQUEST_REPEAT_ASK_PASSPHRASE: {
                if (resultCode != RESULT_OK) {
                    this.finish();
                    return;
                }

                // save the returned passphrase
                RequiredInputParcel requiredInput =
                        data.getParcelableExtra(PassphraseDialogActivity.EXTRA_REQUIRED_INPUT);
                CryptoInputParcel cryptoResult =
                        data.getParcelableExtra(PassphraseDialogActivity.RESULT_CRYPTO_INPUT);
                mPassphrases.put(requiredInput.getMasterKeyId(), cryptoResult.getPassphrase());

                if (mIdsForRepeatAskPassphrase.hasNext()) {
                    askForPassphrase(mIdsForRepeatAskPassphrase.next());
                } else {
                    // backup-code fragment is shown at onPostResume() to preserve state, refer to
                    // http://stackoverflow.com/questions/16265733/failure-delivering-result-onactivityforresult
                    mFinishedCollectingPassphrases = true;
                }
                break;
            }

            default: {
                super.onActivityResult(requestCode, resultCode, data);
            }
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        if (mFinishedCollectingPassphrases) {
            createCache();
        }

        mFinishedCollectingPassphrases = false;
    }

    private void createCache() {
        CryptoOperationHelper.Callback<CreateSecretKeyRingCacheParcel, CreateSecretKeyRingCacheResult> callback =
                new CryptoOperationHelper.Callback<CreateSecretKeyRingCacheParcel, CreateSecretKeyRingCacheResult>() {
                    Activity activity = SettingsPassphraseWorkflowActivity.this;

                    @Override
                    public CreateSecretKeyRingCacheParcel createOperationInput() {
                        return new CreateSecretKeyRingCacheParcel(ChangePassphraseWorkflowOperation.CACHE_FILE_NAME);
                    }

                    @Override
                    public void onCryptoOperationSuccess(CreateSecretKeyRingCacheResult result) {
                        startWorkflowChange();
                    }

                    @Override
                    public void onCryptoOperationCancelled() {
                        Toast.makeText(activity.getApplicationContext(),
                                R.string.create_cache_cancelled, Toast.LENGTH_LONG).show();
                        activity.finish();
                    }

                    @Override
                    public void onCryptoOperationError(CreateSecretKeyRingCacheResult result) {
                        Toast.makeText(activity.getApplicationContext(),
                                R.string.create_cache_error, Toast.LENGTH_LONG).show();
                        activity.finish();
                    }

                    @Override
                    public boolean onCryptoSetProgress(String msg, int progress, int max) {
                        return false;
                    }
                };

        mCryptoOpHelper =
                new CryptoOperationHelper<>(1, this, callback, R.string.progress_cache_secret_keys);

        mCryptoOpHelper.cryptoOperation();
    }

    private void startWorkflowChange() {
        CryptoOperationHelper.Callback<ChangePassphraseWorkflowParcel, ChangePassphraseWorkflowResult> callback =
                new CryptoOperationHelper.Callback<ChangePassphraseWorkflowParcel, ChangePassphraseWorkflowResult>() {
                    Activity activity = SettingsPassphraseWorkflowActivity.this;

                    @Override
                    public ChangePassphraseWorkflowParcel createOperationInput() {
                        return new ChangePassphraseWorkflowParcel(mPassphrases, mMasterPassphrase, mToSinglePassphraseWorkflow);
                    }

                    @Override
                    public void onCryptoOperationSuccess(ChangePassphraseWorkflowResult result) {
                        finishWorkflowChange();
                    }

                    @Override
                    public void onCryptoOperationCancelled() {
                        // should not happen!
                        Toast.makeText(activity.getApplicationContext(),
                                R.string.migrate_error_cancelled, Toast.LENGTH_LONG).show();
                        finish();
                    }

                    @Override
                    public void onCryptoOperationError(ChangePassphraseWorkflowResult result) {
                        Toast.makeText(activity.getApplicationContext(),
                                R.string.migrate_error_migrating, Toast.LENGTH_LONG).show();
                        // TODO: changing the workflow has failed, recover by placing back what we have cached
                    }

                    @Override
                    public boolean onCryptoSetProgress(String msg, int progress, int max) {
                        return false;
                    }
                };
        mCryptoOpHelper =
                new CryptoOperationHelper<>(1, this, callback, R.string.progress_change_workflow);

        mCryptoOpHelper.cryptoOperation();
    }

    private void finishWorkflowChange() {
        // TODO: changing the workflow has completed, flag that we don't need to recover

        mPreferences.setUsesSinglePassphraseWorkflow(mToSinglePassphraseWorkflow);
        finish();
    }
}
