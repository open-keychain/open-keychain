/*
 * Copyright (C) 2012-2014 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2010-2014 Thialfihar <thi@thialfihar.org>
 * Copyright (C) 2015 Vincent Breitmoser <valodim@mugenguild.com>
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

package org.sufficientlysecure.keychain.operations;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;

import android.content.Context;
import android.support.annotation.NonNull;

import org.spongycastle.bcpg.ArmoredOutputStream;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.keyimport.HkpKeyserver;
import org.sufficientlysecure.keychain.keyimport.Keyserver.AddKeyException;
import org.sufficientlysecure.keychain.operations.results.OperationResult.LogType;
import org.sufficientlysecure.keychain.operations.results.OperationResult.OperationLog;
import org.sufficientlysecure.keychain.operations.results.UploadResult;
import org.sufficientlysecure.keychain.pgp.CanonicalizedKeyRing;
import org.sufficientlysecure.keychain.pgp.CanonicalizedPublicKeyRing;
import org.sufficientlysecure.keychain.pgp.Progressable;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.service.UploadKeyringParcel;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.service.input.RequiredInputParcel;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.Preferences;
import org.sufficientlysecure.keychain.util.orbot.OrbotHelper;


/**
 * An operation class which implements high level export operations.
 * This class receives a source and/or destination of keys as input and performs
 * all steps for this export.
 *
 * @see org.sufficientlysecure.keychain.ui.adapter.ImportKeysAdapter#getSelectedEntries()
 * For the export operation, the input consists of a set of key ids and
 * either the name of a file or an output uri to write to.
 */
public class UploadOperation extends BaseOperation<UploadKeyringParcel> {

    public UploadOperation(Context context, ProviderHelper providerHelper, Progressable
            progressable) {
        super(context, providerHelper, progressable);
    }

    public UploadOperation(Context context, ProviderHelper providerHelper,
            Progressable progressable, AtomicBoolean cancelled) {
        super(context, providerHelper, progressable, cancelled);
    }

    @NonNull
    public UploadResult execute(UploadKeyringParcel uploadInput, CryptoInputParcel cryptoInput) {
        Proxy proxy;
        if (cryptoInput.getParcelableProxy() == null) {
            // explicit proxy not set
            if (!OrbotHelper.isOrbotInRequiredState(mContext)) {
                return new UploadResult(null, RequiredInputParcel.createOrbotRequiredOperation(), cryptoInput);
            }
            proxy = Preferences.getPreferences(mContext).getProxyPrefs().parcelableProxy.getProxy();
        } else {
            proxy = cryptoInput.getParcelableProxy().getProxy();
        }

        HkpKeyserver hkpKeyserver = new HkpKeyserver(uploadInput.mKeyserver);
        try {
            CanonicalizedPublicKeyRing keyring;
            if (uploadInput.mMasterKeyId != null) {
                keyring = mProviderHelper.getCanonicalizedPublicKeyRing(
                        uploadInput.mMasterKeyId);
            } else if (uploadInput.mUncachedKeyringBytes != null) {
                CanonicalizedKeyRing canonicalizedRing =
                        UncachedKeyRing.decodeFromData(uploadInput.mUncachedKeyringBytes)
                                .canonicalize(new OperationLog(), 0, true);
                if ( ! CanonicalizedPublicKeyRing.class.isInstance(canonicalizedRing)) {
                    throw new AssertionError("keyring bytes must contain public key ring!");
                }
                keyring = (CanonicalizedPublicKeyRing) canonicalizedRing;
            } else {
                throw new AssertionError("key id or bytes must be non-null!");
            }
            return uploadKeyRingToServer(hkpKeyserver, keyring, proxy);
        } catch (ProviderHelper.NotFoundException e) {
            Log.e(Constants.TAG, "error uploading key", e);
            return new UploadResult(UploadResult.RESULT_ERROR, new OperationLog());
        } catch (IOException e) {
            e.printStackTrace();
            return new UploadResult(UploadResult.RESULT_ERROR, new OperationLog());
        } catch (PgpGeneralException e) {
            e.printStackTrace();
            return new UploadResult(UploadResult.RESULT_ERROR, new OperationLog());
        }
    }

    UploadResult uploadKeyRingToServer(HkpKeyserver server, CanonicalizedPublicKeyRing keyring, Proxy proxy) {

        mProgressable.setProgress(R.string.progress_uploading, 0, 1);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ArmoredOutputStream aos = null;
        OperationLog log = new OperationLog();
        log.add(LogType.MSG_BACKUP_UPLOAD_PUBLIC, 0, KeyFormattingUtils.convertKeyIdToHex(
                keyring.getPublicKey().getKeyId()
        ));

        try {
            aos = new ArmoredOutputStream(bos);
            keyring.encode(aos);
            aos.close();

            String armoredKey = bos.toString("UTF-8");
            server.add(armoredKey, proxy);

            log.add(LogType.MSG_BACKUP_UPLOAD_SUCCESS, 1);
            return new UploadResult(UploadResult.RESULT_OK, log);
        } catch (IOException e) {
            Log.e(Constants.TAG, "IOException", e);

            log.add(LogType.MSG_BACKUP_ERROR_KEY, 1);
            return new UploadResult(UploadResult.RESULT_ERROR, log);
        } catch (AddKeyException e) {
            Log.e(Constants.TAG, "AddKeyException", e);

            log.add(LogType.MSG_BACKUP_ERROR_UPLOAD, 1);
            return new UploadResult(UploadResult.RESULT_ERROR, log);
        } finally {
            mProgressable.setProgress(R.string.progress_uploading, 1, 1);
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

}