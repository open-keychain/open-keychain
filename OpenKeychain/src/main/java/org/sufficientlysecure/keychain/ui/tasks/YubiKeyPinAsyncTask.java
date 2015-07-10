package org.sufficientlysecure.keychain.ui.tasks;

import android.os.AsyncTask;
import android.util.Pair;

import org.sufficientlysecure.keychain.util.Passphrase;

import java.security.SecureRandom;

public class YubiKeyPinAsyncTask extends AsyncTask<Void, Void, Pair<Passphrase, Passphrase>> {

	private OnYubiKeyPinAsyncTaskListener mOnYubiKeyPinAsyncTaskListener;

	public interface OnYubiKeyPinAsyncTaskListener {
			void onYubiKeyPinTaskResult(Passphrase pin, Passphrase adminPin);
	}

	public YubiKeyPinAsyncTask(OnYubiKeyPinAsyncTaskListener onYubiKeyPinAsyncTaskListener) {
		mOnYubiKeyPinAsyncTaskListener = onYubiKeyPinAsyncTaskListener;
	}


	@Override
	protected Pair<Passphrase, Passphrase> doInBackground(Void... unused) {
		SecureRandom secureRandom = new SecureRandom();
		// min = 6, we choose 6
		String pin = "" + secureRandom.nextInt(9)
				+ secureRandom.nextInt(9)
				+ secureRandom.nextInt(9)
				+ secureRandom.nextInt(9)
				+ secureRandom.nextInt(9)
				+ secureRandom.nextInt(9);
		// min = 8, we choose 10, but 6 are equals the PIN
		String adminPin = pin + secureRandom.nextInt(9)
				+ secureRandom.nextInt(9)
				+ secureRandom.nextInt(9)
				+ secureRandom.nextInt(9);

		return new Pair<>(new Passphrase(pin), new Passphrase(adminPin));
	}

	@Override
	protected void onPostExecute(Pair<Passphrase, Passphrase> pair) {
		if(mOnYubiKeyPinAsyncTaskListener != null) {
			mOnYubiKeyPinAsyncTaskListener.onYubiKeyPinTaskResult(pair.first, pair.second);
		}
	}
}
