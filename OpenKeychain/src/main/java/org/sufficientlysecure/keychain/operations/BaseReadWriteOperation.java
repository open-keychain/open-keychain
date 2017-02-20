package org.sufficientlysecure.keychain.operations;


import java.util.concurrent.atomic.AtomicBoolean;

import android.content.Context;
import android.os.Parcelable;

import org.sufficientlysecure.keychain.pgp.Progressable;
import org.sufficientlysecure.keychain.provider.DatabaseReadWriteInteractor;


abstract class BaseReadWriteOperation<T extends Parcelable> extends BaseOperation<T> {
    final DatabaseReadWriteInteractor mDatabaseReadWriteInteractor;

    BaseReadWriteOperation(Context context,
            DatabaseReadWriteInteractor databaseInteractor,
            Progressable progressable) {
        super(context, databaseInteractor, progressable);

        mDatabaseReadWriteInteractor = databaseInteractor;
    }

    BaseReadWriteOperation(Context context, DatabaseReadWriteInteractor databaseInteractor,
            Progressable progressable, AtomicBoolean cancelled) {
        super(context, databaseInteractor, progressable, cancelled);

        mDatabaseReadWriteInteractor = databaseInteractor;
    }
}
