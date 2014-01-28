/*
 * Copyright (C) 2012-2013 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2010 Thialfihar <thi@thialfihar.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.sufficientlysecure.keychain.pgp;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import org.spongycastle.bcpg.ArmoredOutputStream;
import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.PGPKeyRing;
import org.spongycastle.openpgp.PGPObjectFactory;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPPublicKeyRing;
import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSecretKeyRing;
import org.spongycastle.openpgp.PGPUtil;
import org.spongycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.Id;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.service.KeychainIntentService;
import org.sufficientlysecure.keychain.util.HkpKeyServer;
import org.sufficientlysecure.keychain.util.InputData;
import org.sufficientlysecure.keychain.util.IterableIterator;
import org.sufficientlysecure.keychain.util.KeyServer.AddKeyException;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.PositionAwareInputStream;
import org.sufficientlysecure.keychain.util.ProgressDialogUpdater;

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;

public class PgpImportExport {
    private Context mContext;
    private ProgressDialogUpdater mProgress;

    public PgpImportExport(Context context, ProgressDialogUpdater progress) {
        super();
        this.mContext = context;
        this.mProgress = progress;
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
        ArmoredOutputStream aos = new ArmoredOutputStream(bos);
        try {
            aos.write(keyring.getEncoded());
            aos.close();

            String armouredKey = bos.toString("UTF-8");
            server.add(armouredKey);

            return true;
        } catch (IOException e) {
            return false;
        } catch (AddKeyException e) {
            // TODO: tell the user?
            return false;
        } finally {
            try {
                bos.close();
            } catch (IOException e) {
            }
        }
    }

    /**
     * Imports keys from given data. If keyIds is given only those are imported
     * 
     * @param data
     * @param keyIds
     * @return
     * @throws PgpGeneralException
     * @throws FileNotFoundException
     * @throws PGPException
     * @throws IOException
     */
    public Bundle importKeyRings(InputData data, ArrayList<Long> keyIds)
            throws PgpGeneralException, FileNotFoundException, PGPException, IOException {
        Bundle returnData = new Bundle();

        updateProgress(R.string.progress_importing_secret_keys, 0, 100);

        PositionAwareInputStream progressIn = new PositionAwareInputStream(data.getInputStream());

        // need to have access to the bufferedInput, so we can reuse it for the possible
        // PGPObject chunks after the first one, e.g. files with several consecutive ASCII
        // armour blocks
        BufferedInputStream bufferedInput = new BufferedInputStream(progressIn);
        int newKeys = 0;
        int oldKeys = 0;
        int badKeys = 0;
        try {

            // read all available blocks... (asc files can contain many blocks with BEGIN END)
            while (bufferedInput.available() > 0) {
                InputStream in = PGPUtil.getDecoderStream(bufferedInput);
                PGPObjectFactory objectFactory = new PGPObjectFactory(in);

                // go through all objects in this block
                Object obj;
                while ((obj = objectFactory.nextObject()) != null) {
                    Log.d(Constants.TAG, "Found class: " + obj.getClass());

                    if (obj instanceof PGPKeyRing) {
                        PGPKeyRing keyring = (PGPKeyRing) obj;

                        int status = Integer.MIN_VALUE; // out of bounds value

                        if (keyIds != null) {
                            if (keyIds.contains(keyring.getPublicKey().getKeyID())) {
                                status = storeKeyRingInCache(keyring);
                            } else {
                                Log.d(Constants.TAG, "not selected! key id: "
                                        + keyring.getPublicKey().getKeyID());
                            }
                        } else {
                            status = storeKeyRingInCache(keyring);
                        }

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

                        updateProgress((int) (100 * progressIn.position() / data.getSize()), 100);
                    } else {
                        Log.e(Constants.TAG, "Object not recognized as PGPKeyRing!");
                    }
                }
            }
        } catch (Exception e) {
            Log.e(Constants.TAG, "Exception on parsing key file!", e);
        }

        returnData.putInt(KeychainIntentService.RESULT_IMPORT_ADDED, newKeys);
        returnData.putInt(KeychainIntentService.RESULT_IMPORT_UPDATED, oldKeys);
        returnData.putInt(KeychainIntentService.RESULT_IMPORT_BAD, badKeys);

        updateProgress(R.string.progress_done, 100, 100);

        return returnData;
    }

    public Bundle exportKeyRings(ArrayList<Long> keyRingMasterKeyIds, int keyType,
            OutputStream outStream) throws PgpGeneralException, FileNotFoundException,
            PGPException, IOException {
        Bundle returnData = new Bundle();

        updateProgress(
                mContext.getResources().getQuantityString(R.plurals.progress_exporting_key,
                        keyRingMasterKeyIds.size()), 0, 100);

        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            throw new PgpGeneralException(
                    mContext.getString(R.string.error_external_storage_not_ready));
        }

        if (keyType == Id.type.secret_key) {
            ArmoredOutputStream outSec = new ArmoredOutputStream(outStream);
            outSec.setHeader("Version", PgpHelper.getFullVersion(mContext));

            for (int i = 0; i < keyRingMasterKeyIds.size(); ++i) {
                updateProgress(i * 100 / keyRingMasterKeyIds.size() / 2, 100);

                PGPSecretKeyRing secretKeyRing = ProviderHelper.getPGPSecretKeyRingByMasterKeyId(
                        mContext, keyRingMasterKeyIds.get(i));

                if (secretKeyRing != null) {
                    secretKeyRing.encode(outSec);
                }
            }
            outSec.close();
        } else {
            // export public keyrings...
            ArmoredOutputStream outPub = new ArmoredOutputStream(outStream);
            outPub.setHeader("Version", PgpHelper.getFullVersion(mContext));

            for (int i = 0; i < keyRingMasterKeyIds.size(); ++i) {
                // double the needed time if exporting both public and secret parts
                if (keyType == Id.type.secret_key) {
                    updateProgress(i * 100 / keyRingMasterKeyIds.size() / 2, 100);
                } else {
                    updateProgress(i * 100 / keyRingMasterKeyIds.size(), 100);
                }

                PGPPublicKeyRing publicKeyRing = ProviderHelper.getPGPPublicKeyRingByMasterKeyId(
                        mContext, keyRingMasterKeyIds.get(i));

                if (publicKeyRing != null) {
                    publicKeyRing.encode(outPub);
                }
            }
            outPub.close();
        }

        returnData.putInt(KeychainIntentService.RESULT_EXPORT, keyRingMasterKeyIds.size());

        updateProgress(R.string.progress_done, 100, 100);

        return returnData;
    }

    /**
     * TODO: implement Id.return_value.updated as status when key already existed
     * 
     * @param context
     * @param keyring
     * @return
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
                        if (PgpKeyHelper.isSecretKeyPrivateEmpty(testSecretKey)) {
                            // this is bad, something is very wrong...
                            save = false;
                            status = Id.return_value.bad;
                        }
                    }
                }

                if (save) {
                    ProviderHelper.saveKeyRing(mContext, secretKeyRing);
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
                    if (newPubRing != null)
                        ProviderHelper.saveKeyRing(mContext, newPubRing);
                    // TODO: remove status returns, use exceptions!
                    status = Id.return_value.ok;
                }
            } else if (keyring instanceof PGPPublicKeyRing) {
                PGPPublicKeyRing publicKeyRing = (PGPPublicKeyRing) keyring;
                ProviderHelper.saveKeyRing(mContext, publicKeyRing);
                // TODO: remove status returns, use exceptions!
                status = Id.return_value.ok;
            }
        } catch (IOException e) {
            status = Id.return_value.error;
        }

        return status;
    }

}
