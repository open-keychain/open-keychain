/*
 * Copyright (C) 2010-2014 Thialfihar <thi@thialfihar.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.sufficientlysecure.keychain.util;

import java.io.IOException;
import java.io.InputStream;

public class PositionAwareInputStream extends InputStream {
    private InputStream mStream;
    private long mPosition;

    public PositionAwareInputStream(InputStream in) {
        mStream = in;
        mPosition = 0;
    }

    @Override
    public int read() throws IOException {
        int ch = mStream.read();
        ++mPosition;
        return ch;
    }

    @Override
    public int available() throws IOException {
        return mStream.available();
    }

    @Override
    public void close() throws IOException {
        mStream.close();
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public int read(byte[] b) throws IOException {
        int result = mStream.read(b);
        mPosition += result;
        return result;
    }

    @Override
    public int read(byte[] b, int offset, int length) throws IOException {
        int result = mStream.read(b, offset, length);
        mPosition += result;
        return result;
    }

    @Override
    public synchronized void reset() throws IOException {
        mStream.reset();
        mPosition = 0;
    }

    @Override
    public long skip(long n) throws IOException {
        long result = mStream.skip(n);
        mPosition += result;
        return result;
    }

    public long position() {
        return mPosition;
    }
}
