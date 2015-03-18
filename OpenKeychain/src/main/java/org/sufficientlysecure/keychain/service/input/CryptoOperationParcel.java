package org.sufficientlysecure.keychain.service.input;

import java.nio.ByteBuffer;
import java.util.Date;
import java.util.HashMap;

import android.os.Parcel;
import android.os.Parcelable;


/** This is a base class for the input of crypto operations.
 *
 */
public abstract class CryptoOperationParcel implements Parcelable {

    Date mOperationTime;

    // this map contains both decrypted session keys and signed hashes to be
    // used in the crypto operation described by this parcel.
    HashMap<ByteBuffer,byte[]> mCryptoData;

    protected CryptoOperationParcel(Date operationTime) {
        mOperationTime = operationTime;
    }

    protected CryptoOperationParcel(Parcel source) {
        mOperationTime = new Date(source.readLong());

        {
            int count = source.readInt();
            mCryptoData = new HashMap<>(count);
            for (int i = 0; i < count; i++) {
                byte[] key = source.createByteArray();
                byte[] value = source.createByteArray();
                mCryptoData.put(ByteBuffer.wrap(key), value);
            }
        }

    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(mOperationTime.getTime());

        dest.writeInt(mCryptoData.size());
        for (HashMap.Entry<ByteBuffer,byte[]> entry : mCryptoData.entrySet()) {
            dest.writeByteArray(entry.getKey().array());
            dest.writeByteArray(entry.getValue());
        }
    }

}
