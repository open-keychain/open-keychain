/*
 * Copyright (C) 2015 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2015 Vincent Breitmoser <v.breitmoser@mugenguild.com>
 * Copyright (C) 2015 Adithya Abraham Philip <adithyaphilip@gmail.com>
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


import java.util.Random;

import android.content.Context;
import android.support.annotation.NonNull;

import org.sufficientlysecure.keychain.operations.results.BenchmarkResult;
import org.sufficientlysecure.keychain.operations.results.DecryptVerifyResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult.OperationLog;
import org.sufficientlysecure.keychain.operations.results.SignEncryptResult;
import org.sufficientlysecure.keychain.pgp.PgpDecryptVerifyInputParcel;
import org.sufficientlysecure.keychain.pgp.PgpDecryptVerifyOperation;
import org.sufficientlysecure.keychain.pgp.PgpSignEncryptInputParcel;
import org.sufficientlysecure.keychain.pgp.PgpSignEncryptOperation;
import org.sufficientlysecure.keychain.pgp.Progressable;
import org.sufficientlysecure.keychain.pgp.SignEncryptParcel;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.service.BenchmarkInputParcel;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.util.Passphrase;
import org.sufficientlysecure.keychain.util.ProgressScaler;


public class BenchmarkOperation extends BaseOperation<BenchmarkInputParcel> {

    public BenchmarkOperation(Context context, ProviderHelper providerHelper, Progressable
            progressable) {
        super(context, providerHelper, progressable);
    }

    @NonNull
    @Override
    public BenchmarkResult execute(BenchmarkInputParcel consolidateInputParcel,
                                     CryptoInputParcel cryptoInputParcel) {
        OperationLog log = new OperationLog();

        // random data
        byte[] buf = new byte[1024];
        new Random().nextBytes(buf);

        Passphrase passphrase = new Passphrase("a");

        // encrypt
        SignEncryptResult encryptResult;
        {
            SignEncryptOperation op =
                    new SignEncryptOperation(mContext, mProviderHelper,
                            new ProgressScaler(mProgressable, 0, 10, 100), mCancelled);
            SignEncryptParcel input = new SignEncryptParcel();
            input.setSymmetricPassphrase(passphrase);
            input.setBytes(buf);
            encryptResult = op.execute(input, new CryptoInputParcel());
        }

        // decrypt
        DecryptVerifyResult decryptResult;
        {
            PgpDecryptVerifyOperation op =
                    new PgpDecryptVerifyOperation(mContext, mProviderHelper,
                            new ProgressScaler(mProgressable, 0, 10, 100));
            PgpDecryptVerifyInputParcel input = new PgpDecryptVerifyInputParcel(encryptResult.getResultBytes());
            input.setAllowSymmetricDecryption(true);
            decryptResult = op.execute(input, new CryptoInputParcel(passphrase));
        }

        return new BenchmarkResult(BenchmarkResult.RESULT_OK, log);
    }

}
