package org.sufficientlysecure.keychain.operations;

import android.content.Context;

import org.sufficientlysecure.keychain.pgp.PassphraseCacheInterface;
import org.sufficientlysecure.keychain.pgp.Progressable;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.provider.ProviderHelper.NotFoundException;
import org.sufficientlysecure.keychain.service.PassphraseCacheService;

import java.util.concurrent.atomic.AtomicBoolean;

public class BaseOperation implements PassphraseCacheInterface {

    final public Context mContext;
    final public Progressable mProgressable;
    final public AtomicBoolean mCancelled;

    final public ProviderHelper mProviderHelper;

    // TODO do we really need the context in these operations?
    public BaseOperation(Context context, ProviderHelper providerHelper, Progressable progressable) {
        this.mContext = context;
        this.mProgressable = progressable;
        this.mProviderHelper = providerHelper;
        mCancelled = null;
    }

    public BaseOperation(Context context, ProviderHelper providerHelper, Progressable progressable, AtomicBoolean cancelled) {
        mContext = context;
        mProgressable = progressable;
        mProviderHelper = providerHelper;
        mCancelled = cancelled;
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

    protected boolean checkCancelled() {
        return mCancelled != null && mCancelled.get();
    }

    @Override
    public String getCachedPassphrase(long subKeyId) throws NoSecretKeyException {
        try {
            long masterKeyId = mProviderHelper.getMasterKeyId(subKeyId);
            return getCachedPassphrase(masterKeyId, subKeyId);
        } catch (NotFoundException e) {
            throw new PassphraseCacheInterface.NoSecretKeyException();
        }
    }

    @Override
    public String getCachedPassphrase(long masterKeyId, long subKeyId) throws NoSecretKeyException {
        try {
            return PassphraseCacheService.getCachedPassphrase(
                    mContext, masterKeyId, subKeyId);
        } catch (PassphraseCacheService.KeyNotFoundException e) {
            throw new PassphraseCacheInterface.NoSecretKeyException();
        }
    }

}
