package org.sufficientlysecure.keychain.util;


import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import android.os.Parcel;

import com.ryanharter.auto.value.parcel.TypeAdapter;


public class ByteMapParcelAdapter implements TypeAdapter<Map<ByteBuffer,byte[]>> {
    @Override
    public Map<ByteBuffer, byte[]> fromParcel(Parcel source) {
        int count = source.readInt();
        Map<ByteBuffer,byte[]> result = new HashMap<>(count);
        for (int i = 0; i < count; i++) {
            byte[] key = source.createByteArray();
            byte[] value = source.createByteArray();
            result.put(ByteBuffer.wrap(key), value);
        }
        return Collections.unmodifiableMap(result);
    }

    @Override
    public void toParcel(Map<ByteBuffer, byte[]> value, Parcel dest) {
        dest.writeInt(value.size());
        for (Map.Entry<ByteBuffer, byte[]> entry : value.entrySet()) {
            dest.writeByteArray(entry.getKey().array());
            dest.writeByteArray(entry.getValue());
        }
    }
}
