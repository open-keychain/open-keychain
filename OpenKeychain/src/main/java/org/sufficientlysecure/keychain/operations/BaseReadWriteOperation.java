package org.sufficientlysecure.keychain.operations;


import java.util.concurrent.atomic.AtomicBoolean;

import android.content.Context;
import android.os.Parcelable;

import org.sufficientlysecure.keychain.pgp.Progressable;
import org.sufficientlysecure.keychain.provider.KeyWritableRepository;


abstract class BaseReadWriteOperation<T extends Parcelable> extends BaseOperation<T> {
    final KeyWritableRepository mKeyWritableRepository;

    BaseReadWriteOperation(Context context,
            KeyWritableRepository databaseInteractor,
            Progressable progressable) {
        super(context, databaseInteractor, progressable);

        mKeyWritableRepository = databaseInteractor;
    }

    BaseReadWriteOperation(Context context, KeyWritableRepository databaseInteractor,
            Progressable progressable, AtomicBoolean cancelled) {
        super(context, databaseInteractor, progressable, cancelled);

        mKeyWritableRepository = databaseInteractor;
    }
}
