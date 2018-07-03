package org.sufficientlysecure.keychain.operations;


import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import android.content.Context;
import android.support.annotation.NonNull;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.daos.KeyMetadataDao;
import org.sufficientlysecure.keychain.daos.KeyWritableRepository;
import org.sufficientlysecure.keychain.keyimport.ParcelableKeyRing;
import org.sufficientlysecure.keychain.operations.results.ImportKeyResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.pgp.Progressable;
import org.sufficientlysecure.keychain.service.ImportKeyringParcel;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.util.Preferences;
import timber.log.Timber;


public class KeySyncOperation extends BaseReadWriteOperation<KeySyncParcel> {
    // time since last update after which a key should be updated again, in s
    private static final long KEY_STALE_THRESHOLD_MILLIS =
            Constants.DEBUG_KEYSERVER_SYNC ? 1 : TimeUnit.DAYS.toMillis(7);
    // Time taken by Orbot before a new circuit is created
    private static final int ORBOT_CIRCUIT_TIMEOUT_SECONDS =
            Constants.DEBUG_KEYSERVER_SYNC ? 2 : (int) TimeUnit.MINUTES.toSeconds(10);

    private final KeyMetadataDao keyMetadataDao;
    private final Preferences preferences;

    public KeySyncOperation(Context context, KeyWritableRepository databaseInteractor,
            Progressable progressable, AtomicBoolean cancellationSignal) {
        super(context, databaseInteractor, progressable, cancellationSignal);

        keyMetadataDao = KeyMetadataDao.create(context);
        preferences = Preferences.getPreferences(context);
    }

    @NonNull
    @Override
    public ImportKeyResult execute(KeySyncParcel input, CryptoInputParcel cryptoInput) {
        long staleKeyThreshold = System.currentTimeMillis() - (input.getRefreshAll() ? 0 : KEY_STALE_THRESHOLD_MILLIS);
        List<byte[]> staleKeyFingerprints =
                keyMetadataDao.getFingerprintsForKeysOlderThan(staleKeyThreshold, TimeUnit.MILLISECONDS);
        List<ParcelableKeyRing> staleKeyParcelableKeyRings = fingerprintListToParcelableKeyRings(staleKeyFingerprints);

        if (checkCancelled()) { // if we've already been cancelled
            return new ImportKeyResult(OperationResult.RESULT_CANCELLED, new OperationResult.OperationLog());
        }

        // no explicit proxy, retrieve from preferences. Check if we should do a staggered sync
        CryptoInputParcel cryptoInputParcel = CryptoInputParcel.createCryptoInputParcel();
        boolean reinsertAll = input.getRefreshAll();

        ImportKeyResult importKeyResult;
        if (!reinsertAll && preferences.getParcelableProxy().isTorEnabled()) {
            importKeyResult = staggeredUpdate(staleKeyParcelableKeyRings, cryptoInputParcel);
        } else {
            importKeyResult = directUpdate(staleKeyParcelableKeyRings, cryptoInputParcel, reinsertAll);
        }
        return importKeyResult;
    }

    private List<ParcelableKeyRing> fingerprintListToParcelableKeyRings(List<byte[]> staleKeyFingerprints) {
        ArrayList<ParcelableKeyRing> result = new ArrayList<>(staleKeyFingerprints.size());
        for (byte[] fingerprint : staleKeyFingerprints) {
            Timber.d("Keyserver sync: Updating %s", KeyFormattingUtils.beautifyKeyId(fingerprint));
            result.add(ParcelableKeyRing.createFromReference(fingerprint, null, null, null));
        }
        return result;
    }

    private ImportKeyResult directUpdate(List<ParcelableKeyRing> keyList, CryptoInputParcel cryptoInputParcel,
            boolean reinsertAll) {
        Timber.d("Starting normal update");
        ImportOperation importOp = new ImportOperation(mContext, mKeyWritableRepository, mProgressable, mCancelled);
        return importOp.execute(
                ImportKeyringParcel.createImportKeyringParcel(keyList, preferences.getPreferredKeyserver(), reinsertAll),
                cryptoInputParcel
        );
    }


    /**
     * will perform a staggered update of user's keys using delays to ensure new Tor circuits, as
     * performed by parcimonie. Relevant issue and method at:
     * https://github.com/open-keychain/open-keychain/issues/1337
     *
     * @return result of the sync
     */
    private ImportKeyResult staggeredUpdate(List<ParcelableKeyRing> keyList,
            CryptoInputParcel cryptoInputParcel) {
        Timber.d("Starting staggered update");
        // final int WEEK_IN_SECONDS = (int) TimeUnit.DAYS.toSeconds(7);
        // we are limiting our randomness to ORBOT_CIRCUIT_TIMEOUT_SECONDS for now
        final int WEEK_IN_SECONDS = 0;

        ImportOperation.KeyImportAccumulator accumulator
                = new ImportOperation.KeyImportAccumulator(keyList.size(), null);

        // so that the first key can be updated without waiting. This is so that there isn't a
        // large gap between a "Start Orbot" notification and the next key update
        boolean first = true;

        for (ParcelableKeyRing keyRing : keyList) {
            int waitTime;
            int staggeredTime = new Random().nextInt(1 + 2 * (WEEK_IN_SECONDS / keyList.size()));
            if (staggeredTime >= ORBOT_CIRCUIT_TIMEOUT_SECONDS) {
                waitTime = staggeredTime;
            } else {
                waitTime = ORBOT_CIRCUIT_TIMEOUT_SECONDS
                        + new Random().nextInt(1 + ORBOT_CIRCUIT_TIMEOUT_SECONDS);
            }

            if (first) {
                waitTime = 0;
                first = false;
            }

            Timber.d("Updating key with a wait time of %d seconds", waitTime);
            try {
                Thread.sleep(waitTime * 1000);
            } catch (InterruptedException e) {
                Timber.e(e, "Exception during sleep between key updates");
                // skip this one
                continue;
            }
            ArrayList<ParcelableKeyRing> keyWrapper = new ArrayList<>();
            keyWrapper.add(keyRing);
            if (checkCancelled()) {
                return new ImportKeyResult(ImportKeyResult.RESULT_CANCELLED,
                new OperationResult.OperationLog());
            }
            ImportKeyResult result =
                    new ImportOperation(mContext, mKeyWritableRepository, null, mCancelled)
                            .execute(
                                    ImportKeyringParcel.createImportKeyringParcel(
                                            keyWrapper,
                                            preferences.getPreferredKeyserver()
                                    ),
                                    cryptoInputParcel
                            );
            if (result.isPending()) {
                return result;
            }
            accumulator.accumulateKeyImport(result);
        }
        return accumulator.getConsolidatedResult();
    }

}
