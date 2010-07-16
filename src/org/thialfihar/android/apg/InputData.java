package org.thialfihar.android.apg;

import java.io.InputStream;

public class InputData {
    private PositionAwareInputStream mInputStream;
    private long mSize;

    InputData(InputStream inputStream, long size) {
        mInputStream = new PositionAwareInputStream(inputStream);
        mSize = size;
    }

    public InputStream getInputStream() {
        return mInputStream;
    }

    public long getSize() {
        return mSize;
    }

    public long getStreamPosition() {
        return mInputStream.position();
    }
}
