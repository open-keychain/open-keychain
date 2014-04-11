/*
 * Copyright (C) 2012-2013 Dominik Schürmann <dominik@dominikschuermann.de>
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
import org.spongycastle.openpgp.PGPKeyRing;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPPublicKeyRing;
import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSecretKeyRing;
import org.spongycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.Id;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.service.KeychainIntentService;
import org.sufficientlysecure.keychain.ui.adapter.ImportKeysListEntry;
import org.sufficientlysecure.keychain.util.HkpKeyServer;
import org.sufficientlysecure.keychain.util.IterableIterator;
import org.sufficientlysecure.keychain.util.KeyServer.AddKeyException;
import org.sufficientlysecure.keychain.util.KeychainServiceListener;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.ProgressDialogUpdater;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class PgpImportExport {

    private Context mContext;
    private ProgressDialogUpdater mProgress;

    private KeychainServiceListener mKeychainServiceListener;

    private ProviderHelper mProviderHelper;

    public PgpImportExport(Context context, ProgressDialogUpdater progress) {
        super();
        this.mContext = context;
        this.mProgress = progress;
        this.mProviderHelper = new ProviderHelper(context);
    }

    public PgpImportExport(Context context,
                           ProgressDialogUpdater progress, KeychainServiceListener keychainListener) {
        super();
        this.mContext = context;
        this.mProgress = progress;
        this.mProviderHelper = new ProviderHelper(context);
        this.mKeychainServiceListener = keychainListener;
    }

    public void updateProgress(int message, int current, int total) {
        if (mProgress != null) {
            mProgress.setProgress(message, current, total);
        }
    }

    public void updateProgress(String message, int current, int total) {
        if (mProgress != null) {
            mProgress.setProgress(message, current, total);
        }
    }

    public void updateProgress(int current, int total) {
        if (mProgress != null) {
            mProgress.setProgress(current, total);
        }
    }

    public boolean uploadKeyRingToServer(HkpKeyServer server, PGPPublicKeyRing keyring) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ArmoredOutputStream aos = null;
        try {
            aos = new ArmoredOutputStream(bos);
            aos.write(keyring.getEncoded());
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
                if (aos != null) { aos.close(); }
                if (bos != null) { bos.close(); }
            } catch (IOException e) {
            }
        }
    }

    /**
     * Imports keys from given data. If keyIds is given only those are imported
     */
    public Bundle importKeyRings(List<ImportKeysListEntry> entries)
            throws PgpGeneralException, PGPException, IOException {
        Bundle returnData = new Bundle();

        updateProgress(R.string.progress_importing, 0, 100);

        int newKeys = 0;
        int oldKeys = 0;
        int badKeys = 0;

        int position = 0;
        try {
            for (ImportKeysListEntry entry : entries) {
                Object obj = PgpConversionHelper.BytesToPGPKeyRing(entry.getBytes());

                if (obj instanceof PGPKeyRing) {
                    PGPKeyRing keyring = (PGPKeyRing) obj;

                    int status = storeKeyRingInCache(keyring);

                    if (status == Id.return_value.error) {
                        throw new PgpGeneralException(
                                mContext.getString(R.string.error_saving_keys));
                    }

                    // update the counts to display to the user at the end
                    if (status == Id.return_value.updated) {
                        ++oldKeys;
                    } else if (status == Id.return_value.ok) {
                        ++newKeys;
                    } else if (status == Id.return_value.bad) {
                        ++badKeys;
                    }
                } else {
                    Log.e(Constants.TAG, "Object not recognized as PGPKeyRing!");
                }

                position++;
                updateProgress(position / entries.size() * 100, 100);
            }
        } catch (Exception e) {
            Log.e(Constants.TAG, "Exception on parsing key file!", e);
        }

        returnData.putInt(KeychainIntentService.RESULT_IMPORT_ADDED, newKeys);
        returnData.putInt(KeychainIntentService.RESULT_IMPORT_UPDATED, oldKeys);
        returnData.putInt(KeychainIntentService.RESULT_IMPORT_BAD, badKeys);

        return returnData;
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
                PGPPublicKeyRing publicKeyRing = mProviderHelper.getPGPPublicKeyRing(pubKeyMasterId);

                publicKeyRing.encode(arOutStream);
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
                PGPSecretKeyRing secretKeyRing = mProviderHelper.getPGPSecretKeyRing(secretKeyMasterId);
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

    /**
     * TODO: implement Id.return_value.updated as status when key already existed
     */
    @SuppressWarnings("unchecked")
    public int storeKeyRingInCache(PGPKeyRing keyring) {
        int status = Integer.MIN_VALUE; // out of bounds value (Id.return_value.*)
        try {
            if (keyring instanceof PGPSecretKeyRing) {
                PGPSecretKeyRing secretKeyRing = (PGPSecretKeyRing) keyring;
                boolean save = true;

                for (PGPSecretKey testSecretKey : new IterableIterator<PGPSecretKey>(
                        secretKeyRing.getSecretKeys())) {
                    if (!testSecretKey.isMasterKey()) {
                        if (testSecretKey.isPrivateKeyEmpty()) {
                            // this is bad, something is very wrong...
                            save = false;
                            status = Id.return_value.bad;
                        }
                    }
                }

                if (save) {
                    // TODO: preserve certifications
                    // (http://osdir.com/ml/encryption.bouncy-castle.devel/2007-01/msg00054.html ?)
                    PGPPublicKeyRing newPubRing = null;
                    for (PGPPublicKey key : new IterableIterator<PGPPublicKey>(
                            secretKeyRing.getPublicKeys())) {
                        if (newPubRing == null) {
                            newPubRing = new PGPPublicKeyRing(key.getEncoded(),
                                    new JcaKeyFingerprintCalculator());
                        }
                        newPubRing = PGPPublicKeyRing.insertPublicKey(newPubRing, key);
                    }
                    if (newPubRing != null) {
                        mProviderHelper.saveKeyRing(newPubRing);
                    }
                    mProviderHelper.saveKeyRing(secretKeyRing);
                    // TODO: remove status returns, use exceptions!
                    status = Id.return_value.ok;
                }
            } else if (keyring instanceof PGPPublicKeyRing) {
                PGPPublicKeyRing publicKeyRing = (PGPPublicKeyRing) keyring;
                mProviderHelper.saveKeyRing(publicKeyRing);
                // TODO: remove status returns, use exceptions!
                status = Id.return_value.ok;
            }
        } catch (IOException e) {
            status = Id.return_value.error;
        }

        return status;
    }

}
