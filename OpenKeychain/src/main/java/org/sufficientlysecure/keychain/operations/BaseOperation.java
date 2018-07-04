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
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.os.CancellationSignal;

import org.sufficientlysecure.keychain.Constants.key;
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.pgp.PassphraseCacheInterface;
import org.sufficientlysecure.keychain.pgp.Progressable;
import org.sufficientlysecure.keychain.daos.KeyRepository;
import org.sufficientlysecure.keychain.service.PassphraseCacheService;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.util.Passphrase;

public abstract class BaseOperation<T extends Parcelable> implements PassphraseCacheInterface {

    final public Context mContext;
    final public Progressable mProgressable;
    final public CancellationSignal mCancelled;

    final public KeyRepository mKeyRepository;

    /** An abstract base class for all *Operation classes. It provides a number
     * of common methods for progress, cancellation and passphrase cache handling.
     *
     * An "operation" in this sense is a high level operation which is called
     * by the KeychainService or OpenPgpService services. Concrete
     * subclasses of this class should implement either a single or a group of
     * related operations. An operation must rely solely on its input
     * parameters for operation specifics. It should also write a log of its
     * operation using the OperationLog class, and return an OperationResult
     * subclass of its specific type.
     *
     * An operation must *not* throw exceptions of any kind, errors should be
     * handled as part of the OperationResult! Consequently, all handling of
     * errors in KeychainService and OpenPgpService should consist of
     * informational rather than operational means.
     *
     * Note that subclasses of this class should be either Android- or
     * BouncyCastle-related, and use as few imports from the other type as
     * possible.  A class with Pgp- prefix is considered BouncyCastle-related,
     * if there is no prefix it is considered Android-related.
     *
     */
    public BaseOperation(Context context, KeyRepository keyRepository, Progressable progressable) {
        this.mContext = context;
        this.mProgressable = progressable;
        this.mKeyRepository = keyRepository;
        mCancelled = null;
    }

    public BaseOperation(Context context, KeyRepository keyRepository,
            Progressable progressable, CancellationSignal cancelled) {
        mContext = context;
        mProgressable = progressable;
        mKeyRepository = keyRepository;
        mCancelled = cancelled;
    }

    @NonNull
    public abstract OperationResult execute(T input, CryptoInputParcel cryptoInput);

    public void updateProgress(@StringRes int message, int current, int total) {
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

    protected boolean checkCancelled() {
        return mCancelled != null && mCancelled.isCanceled();
    }

    protected void setPreventCancel () {
        if (mProgressable != null) {
            mProgressable.setPreventCancel();
        }
    }

    @Override
    public Passphrase getCachedPassphrase(long subKeyId) throws NoSecretKeyException {
        if (subKeyId != key.symmetric) {
            Long masterKeyId = mKeyRepository.getMasterKeyIdBySubkeyId(subKeyId);
            if (masterKeyId == null) {
                throw new PassphraseCacheInterface.NoSecretKeyException();
            }
            return getCachedPassphrase(masterKeyId, subKeyId);
        }
        return getCachedPassphrase(key.symmetric, key.symmetric);
    }

    @Override
    public Passphrase getCachedPassphrase(long masterKeyId, long subKeyId) throws NoSecretKeyException {
        try {
            return PassphraseCacheService.getCachedPassphrase(
                    mContext, masterKeyId, subKeyId);
        } catch (PassphraseCacheService.KeyNotFoundException e) {
            throw new PassphraseCacheInterface.NoSecretKeyException();
        }
    }

}
