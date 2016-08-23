/*
 * Copyright (C) 2016 Alex Fong Jie Wen <alexfongg@gmail.com>
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
import org.sufficientlysecure.keychain.operations.results.MigrateSymmetricResult;
import org.sufficientlysecure.keychain.pgp.Progressable;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.service.MigrateSymmetricInputParcel;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;

public class MigrateSymmetricOperation extends BaseOperation<MigrateSymmetricInputParcel> {
    public static final String CACHE_FILE_NAME = "migrate_to_symmetric.pcl";

    public MigrateSymmetricOperation(Context context, ProviderHelper providerHelper, Progressable
            progressable) {
        super(context, providerHelper, progressable);
    }

    @NonNull
    @Override
    public MigrateSymmetricResult execute(MigrateSymmetricInputParcel migrateParcel,
                                          CryptoInputParcel cryptoInputParcel) {
        mProgressable.setPreventCancel();
        return mProviderHelper.migrateSymmetricOperation(mProgressable, CACHE_FILE_NAME, migrateParcel.mKeyringPassphrasesList);
    }

}
