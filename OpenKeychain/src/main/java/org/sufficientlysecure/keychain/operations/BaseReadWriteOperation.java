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
import android.support.v4.os.CancellationSignal;

import org.sufficientlysecure.keychain.pgp.Progressable;
import org.sufficientlysecure.keychain.daos.KeyWritableRepository;

public abstract class BaseReadWriteOperation<T extends Parcelable> extends BaseOperation<T> {
    protected final KeyWritableRepository mKeyWritableRepository;

    BaseReadWriteOperation(Context context,
            KeyWritableRepository databaseInteractor,
            Progressable progressable) {
        super(context, databaseInteractor, progressable);

        mKeyWritableRepository = databaseInteractor;
    }

    protected BaseReadWriteOperation(Context context, KeyWritableRepository databaseInteractor,
            Progressable progressable, CancellationSignal cancelled) {
        super(context, databaseInteractor, progressable, cancelled);

        mKeyWritableRepository = databaseInteractor;
    }
}
