/*
 * Copyright (C) 2010-2014 Thialfihar <thi@thialfihar.org>
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
