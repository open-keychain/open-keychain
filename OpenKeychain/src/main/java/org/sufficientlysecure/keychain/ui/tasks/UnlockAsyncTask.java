package org.sufficientlysecure.keychain.ui.tasks;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKeyRing;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.pgp.exception.PgpKeyNotFoundException;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.service.PassphraseCacheService;
import org.sufficientlysecure.keychain.util.Passphrase;

/**
 * Async task that handles the job of unlocking a specific key
 */
public class UnlockAsyncTask extends AsyncTask<Void, Void, Boolean> {

	private Context mContext;
	private OnUnlockAsyncTaskListener mOnUnlockAsyncTaskListener;
	private CanonicalizedSecretKeyRing mSecretRing = null;
	private Passphrase mPassphrase;
	private long mSubKeyId;

	/**
	 * Communication interface callback between this async task and its caller.
	 */
	public interface OnUnlockAsyncTaskListener {
		void onErrorCouldNotExtractKey();

		void onErrorWrongPassphrase();

		void onUnlockOperationSuccess();
	}

	public UnlockAsyncTask(Context context, Passphrase passphrase, long subKeyId,
	                       OnUnlockAsyncTaskListener listener) {
		mContext = context;
		mOnUnlockAsyncTaskListener = listener;
		mPassphrase = passphrase;
		mSubKeyId = subKeyId;
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		try {
			ProviderHelper helper = new ProviderHelper(mContext);
			mSecretRing = helper.getCanonicalizedSecretKeyRing(
					KeychainContract.KeyRings.buildUnifiedKeyRingsFindBySubkeyUri(mSubKeyId));
		} catch (ProviderHelper.NotFoundException e) {
			if (mOnUnlockAsyncTaskListener != null) {
				mOnUnlockAsyncTaskListener.onErrorCouldNotExtractKey();
			}
			this.cancel(true);
		}
	}

	protected Boolean doInBackground(Void... params) {
		try {
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				// never mind
			}
			// make sure this unlocks
			return mSecretRing.getSecretKey(mSubKeyId).unlock(mPassphrase);
		} catch (PgpGeneralException e) {
			return false;
		}
	}

	/**
	 * Handle a good or bad passphrase. This happens in the UI thread!
	 */
	@Override
	protected void onPostExecute(Boolean result) {
		super.onPostExecute(result);
		if (mOnUnlockAsyncTaskListener != null) {
			if (!result) {
				mOnUnlockAsyncTaskListener.onErrorWrongPassphrase();
				return;
			}

			// cache the new passphrase
			try {
				PassphraseCacheService.addCachedPassphrase(mContext,
						mSecretRing.getMasterKeyId(), mSubKeyId, mPassphrase,
						mSecretRing.getPrimaryUserIdWithFallback());
			} catch (PgpKeyNotFoundException e) {
				Log.e(Constants.TAG, "adding of a passphrase failed", e);
			}
			mOnUnlockAsyncTaskListener.onUnlockOperationSuccess();
		}
	}
}
