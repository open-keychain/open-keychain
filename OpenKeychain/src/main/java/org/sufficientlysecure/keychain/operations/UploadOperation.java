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

package org.sufficientlysecure.keychain.operations;


import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.keyimport.HkpKeyserverClient;
import org.sufficientlysecure.keychain.keyimport.KeyserverClient.AddKeyException;
import org.sufficientlysecure.keychain.keyimport.HkpKeyserverAddress;
import org.sufficientlysecure.keychain.operations.results.OperationResult.LogType;
import org.sufficientlysecure.keychain.operations.results.OperationResult.OperationLog;
import org.sufficientlysecure.keychain.operations.results.UploadResult;
import org.sufficientlysecure.keychain.pgp.CanonicalizedKeyRing;
import org.sufficientlysecure.keychain.pgp.CanonicalizedPublicKeyRing;
import org.sufficientlysecure.keychain.pgp.Progressable;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.provider.KeyRepository;
import org.sufficientlysecure.keychain.provider.KeyWritableRepository;
import org.sufficientlysecure.keychain.service.UploadKeyringParcel;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.service.input.RequiredInputParcel;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.util.ParcelableProxy;
import org.sufficientlysecure.keychain.util.Preferences;
import org.sufficientlysecure.keychain.network.orbot.OrbotHelper;
import timber.log.Timber;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * An operation class which implements the upload of a single key to a key server.
 */
public class UploadOperation extends BaseOperation<UploadKeyringParcel> {

    public UploadOperation(Context context, KeyRepository keyRepository,
            Progressable progressable, AtomicBoolean cancelled) {
        super(context, keyRepository, progressable, cancelled);
    }

    @NonNull
    public UploadResult execute(UploadKeyringParcel uploadInput, CryptoInputParcel cryptoInput) {
        OperationLog log = new OperationLog();

        log.add(LogType.MSG_UPLOAD, 0);
        updateProgress(R.string.progress_uploading, 0, 1);

        // Proxy priorities:
        // 1. explicit proxy
        // 2. orbot proxy state
        // 3. proxy from preferences
        ParcelableProxy parcelableProxy = cryptoInput.getParcelableProxy();
        if (parcelableProxy == null) {
            if (!OrbotHelper.isOrbotInRequiredState(mContext)) {
                return new UploadResult(log, RequiredInputParcel.createOrbotRequiredOperation(), cryptoInput);
            }

            parcelableProxy = Preferences.getPreferences(mContext).getParcelableProxy();
        }

        boolean proxyIsTor = parcelableProxy.isTorEnabled();

        if (proxyIsTor) {
            log.add(LogType.MSG_UPLOAD_PROXY_TOR, 1);
        } else if (parcelableProxy.getProxy() == Proxy.NO_PROXY) {
            log.add(LogType.MSG_UPLOAD_PROXY_DIRECT, 1);
        } else {
            log.add(LogType.MSG_UPLOAD_PROXY, 1, parcelableProxy.getProxy().toString());
        }

        HkpKeyserverAddress hkpKeyserver;
        {
            hkpKeyserver = uploadInput.getKeyserver();
            log.add(LogType.MSG_UPLOAD_SERVER, 1, hkpKeyserver.toString());
        }

        CanonicalizedPublicKeyRing keyring = getPublicKeyringFromInput(log, uploadInput);
        if (keyring == null) {
            return new UploadResult(UploadResult.RESULT_ERROR, log);
        }

        return uploadKeyRingToServer(log, hkpKeyserver, keyring, parcelableProxy);
    }

    @Nullable
    private CanonicalizedPublicKeyRing getPublicKeyringFromInput(OperationLog log, UploadKeyringParcel uploadInput) {
        try {
            Long masterKeyId = uploadInput.getMasterKeyId();
            if (masterKeyId != null) {
                log.add(LogType.MSG_UPLOAD_KEY, 0, KeyFormattingUtils.convertKeyIdToHex(masterKeyId));
                return mKeyRepository.getCanonicalizedPublicKeyRing(masterKeyId);
            }

            CanonicalizedKeyRing canonicalizedRing =
                    UncachedKeyRing.decodeFromData(uploadInput.getUncachedKeyringBytes())
                            .canonicalize(new OperationLog(), 0, true);
            if (!CanonicalizedPublicKeyRing.class.isInstance(canonicalizedRing)) {
                throw new IllegalArgumentException("keyring bytes must contain public key ring!");
            }
            log.add(LogType.MSG_UPLOAD_KEY, 0, KeyFormattingUtils.convertKeyIdToHex(canonicalizedRing.getMasterKeyId()));
            return (CanonicalizedPublicKeyRing) canonicalizedRing;

        } catch (KeyWritableRepository.NotFoundException e) {
            log.add(LogType.MSG_UPLOAD_ERROR_NOT_FOUND, 1);
            return null;
        } catch (IOException | PgpGeneralException e) {
            log.add(LogType.MSG_UPLOAD_ERROR_IO, 1);
            Timber.e(e, "error uploading key");
            return null;
        }

    }

    @NonNull
    private UploadResult uploadKeyRingToServer(
            OperationLog log, HkpKeyserverAddress hkpKeyserverAddress, CanonicalizedPublicKeyRing keyring,
            ParcelableProxy proxy) {

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ArmoredOutputStream aos = null;

        HkpKeyserverClient keyserverInteractor = HkpKeyserverClient.fromHkpKeyserverAddress(hkpKeyserverAddress);

        try {
            aos = new ArmoredOutputStream(bos);
            keyring.encode(aos);
            aos.close();

            String armoredKey = bos.toString("UTF-8");
            keyserverInteractor.add(armoredKey, proxy);

            updateProgress(R.string.progress_uploading, 1, 1);

            log.add(LogType.MSG_UPLOAD_SUCCESS, 1);
            return new UploadResult(UploadResult.RESULT_OK, log);
        } catch (IOException e) {
            Timber.e(e, "IOException");

            log.add(LogType.MSG_UPLOAD_ERROR_IO, 1);
            return new UploadResult(UploadResult.RESULT_ERROR, log);
        } catch (AddKeyException e) {
            Timber.e(e, "AddKeyException");

            log.add(LogType.MSG_UPLOAD_ERROR_UPLOAD, 1);
            return new UploadResult(UploadResult.RESULT_ERROR, log);
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

}