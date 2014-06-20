/*
 * Copyright (C) 2012-2014 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2010-2014 Thialfihar <thi@thialfihar.org>
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

package org.sufficientlysecure.keychain.pgp;

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;

import org.spongycastle.bcpg.ArmoredOutputStream;
import org.spongycastle.openpgp.PGPException;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.keyimport.HkpKeyserver;
import org.sufficientlysecure.keychain.keyimport.Keyserver.AddKeyException;
import org.sufficientlysecure.keychain.keyimport.ParcelableKeyRing;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.service.KeychainIntentService;
import org.sufficientlysecure.keychain.service.OperationResultParcel.OperationLog;
import org.sufficientlysecure.keychain.service.OperationResults.ImportResult;
import org.sufficientlysecure.keychain.service.OperationResults.SaveKeyringResult;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.ProgressScaler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class PgpImportExport {

    // TODO: is this really used?
    public interface KeychainServiceListener {
        boolean hasServiceStopped();
    }

    private Context mContext;
    private Progressable mProgressable;

    private KeychainServiceListener mKeychainServiceListener;

    private ProviderHelper mProviderHelper;

    public PgpImportExport(Context context, Progressable progressable) {
        super();
        this.mContext = context;
        this.mProgressable = progressable;
        this.mProviderHelper = new ProviderHelper(context);
    }

    public PgpImportExport(Context context,
                           Progressable progressable, KeychainServiceListener keychainListener) {
        super();
        this.mContext = context;
        this.mProgressable = progressable;
        this.mProviderHelper = new ProviderHelper(context);
        this.mKeychainServiceListener = keychainListener;
    }

    public void updateProgress(int message, int current, int total) {
        if (mProgressable != null) {
            mProgressable.setProgress(message, current, total);
        }
    }

    public void updateProgress(String message, int current, int total) {
        if (mProgressable != null) {
            mProgressable.setProgress(message, current, total);
        }
    }

    public void updateProgress(int current, int total) {
        if (mProgressable != null) {
            mProgressable.setProgress(current, total);
        }
    }

    public boolean uploadKeyRingToServer(HkpKeyserver server, WrappedPublicKeyRing keyring) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ArmoredOutputStream aos = null;
        try {
            aos = new ArmoredOutputStream(bos);
            keyring.encode(aos);
            aos.close();

            String armoredKey = bos.toString("UTF-8");
            server.add(armoredKey);

            return true;
        } catch (IOException e) {
            return false;
        } catch (AddKeyException e) {
            // TODO: tell the user?
            return false;
        } finally {
            try {
                if (aos != null) {
                    aos.close();
                }
                bos.close();
            } catch (IOException e) {
                // this is just a finally thing, no matter if it doesn't work out.
            }
        }
    }

    /** Imports keys from given data. If keyIds is given only those are imported */
    public ImportResult importKeyRings(List<ParcelableKeyRing> entries)
            throws PgpGeneralException, PGPException, IOException {

        updateProgress(R.string.progress_importing, 0, 100);

        int newKeys = 0, oldKeys = 0, badKeys = 0;

        int position = 0;
        int progSteps = 100 / entries.size();
        for (ParcelableKeyRing entry : entries) {
            try {
                UncachedKeyRing key = UncachedKeyRing.decodeFromData(entry.getBytes());

                String expectedFp = entry.getExpectedFingerprint();
                if(expectedFp != null) {
                    if(!PgpKeyHelper.convertFingerprintToHex(key.getFingerprint()).equals(expectedFp)) {
                        Log.d(Constants.TAG, "fingerprint: " + PgpKeyHelper.convertFingerprintToHex(key.getFingerprint()));
                        Log.d(Constants.TAG, "expected fingerprint: " + expectedFp);
                        Log.e(Constants.TAG, "Actual key fingerprint is not the same as expected!");
                        badKeys += 1;
                        continue;
                    } else {
                        Log.d(Constants.TAG, "Actual key fingerprint matches expected one.");
                    }
                }

                SaveKeyringResult result;
                if (key.isSecret()) {
                    result = mProviderHelper.saveSecretKeyRing(key,
                            new ProgressScaler(mProgressable, position, (position+1)*progSteps, 100));
                } else {
                    result = mProviderHelper.savePublicKeyRing(key,
                            new ProgressScaler(mProgressable, position, (position+1)*progSteps, 100));
                }
                if (!result.success()) {
                    badKeys += 1;
                } else if (result.updated()) {
                    oldKeys += 1;
                } else {
                    newKeys += 1;
                }

            } catch (PgpGeneralException e) {
                Log.e(Constants.TAG, "Encountered bad key on import!", e);
                ++badKeys;
            }
            // update progress
            position++;
        }

        OperationLog log = mProviderHelper.getLog();
        int resultType = 0;
        // special return case: no new keys at all
        if (badKeys == 0 && newKeys == 0 && oldKeys == 0) {
            resultType = ImportResult.RESULT_FAIL_NOTHING;
        } else {
            if (newKeys > 0) {
                resultType |= ImportResult.RESULT_OK_NEWKEYS;
            }
            if (oldKeys > 0) {
                resultType |= ImportResult.RESULT_OK_UPDATED;
            }
            if (badKeys > 0) {
                resultType |= ImportResult.RESULT_WITH_ERRORS;
                if (newKeys == 0 && oldKeys == 0) {
                    resultType |= ImportResult.RESULT_ERROR;
                }
            }
            if (log.containsWarnings()) {
                resultType |= ImportResult.RESULT_WITH_WARNINGS;
            }
        }

        return new ImportResult(resultType, log, newKeys, oldKeys, badKeys);

    }

    public Bundle exportKeyRings(ArrayList<Long> publicKeyRingMasterIds,
                                 ArrayList<Long> secretKeyRingMasterIds,
                                 OutputStream outStream) throws PgpGeneralException,
            PGPException, IOException {
        Bundle returnData = new Bundle();

        int masterKeyIdsSize = publicKeyRingMasterIds.size() + secretKeyRingMasterIds.size();
        int progress = 0;

        updateProgress(
                mContext.getResources().getQuantityString(R.plurals.progress_exporting_key,
                        masterKeyIdsSize), 0, 100);

        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            throw new PgpGeneralException(
                    mContext.getString(R.string.error_external_storage_not_ready));
        }
        // For each public masterKey id
        for (long pubKeyMasterId : publicKeyRingMasterIds) {
            progress++;
            // Create an output stream
            ArmoredOutputStream arOutStream = new ArmoredOutputStream(outStream);
            arOutStream.setHeader("Version", PgpHelper.getFullVersion(mContext));

            updateProgress(progress * 100 / masterKeyIdsSize, 100);

            try {
                WrappedPublicKeyRing ring = mProviderHelper.getWrappedPublicKeyRing(
                        KeychainContract.KeyRings.buildUnifiedKeyRingUri(pubKeyMasterId)
                );

                ring.encode(arOutStream);
            } catch (ProviderHelper.NotFoundException e) {
                Log.e(Constants.TAG, "key not found!", e);
                // TODO: inform user?
            }

            if (mKeychainServiceListener.hasServiceStopped()) {
                arOutStream.close();
                return null;
            }

            arOutStream.close();
        }

        // For each secret masterKey id
        for (long secretKeyMasterId : secretKeyRingMasterIds) {
            progress++;
            // Create an output stream
            ArmoredOutputStream arOutStream = new ArmoredOutputStream(outStream);
            arOutStream.setHeader("Version", PgpHelper.getFullVersion(mContext));

            updateProgress(progress * 100 / masterKeyIdsSize, 100);

            try {
                WrappedSecretKeyRing secretKeyRing =
                        mProviderHelper.getWrappedSecretKeyRing(secretKeyMasterId);
                secretKeyRing.encode(arOutStream);
            } catch (ProviderHelper.NotFoundException e) {
                Log.e(Constants.TAG, "key not found!", e);
                // TODO: inform user?
            }

            if (mKeychainServiceListener.hasServiceStopped()) {
                arOutStream.close();
                return null;
            }

            arOutStream.close();
        }

        returnData.putInt(KeychainIntentService.RESULT_EXPORT, masterKeyIdsSize);

        updateProgress(R.string.progress_done, 100, 100);

        return returnData;
    }

}
