/*
 * Copyright (C) 2015 Dominik Schürmann <dominik@dominikschuermann.de>
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
        if (mOnYubiKeyPinAsyncTaskListener != null) {
            mOnYubiKeyPinAsyncTaskListener.onYubiKeyPinTaskResult(pair.first, pair.second);
        }
    }

    public void setOnYubiKeyPinAsyncTaskListener(OnYubiKeyPinAsyncTaskListener onYubiKeyPinAsyncTaskListener) {
        mOnYubiKeyPinAsyncTaskListener = onYubiKeyPinAsyncTaskListener;
    }
}
