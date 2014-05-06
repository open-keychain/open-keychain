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

import java.io.InputStream;

/**
 * Wrapper to include size besides an InputStream
 */
public class InputData {
    private PositionAwareInputStream mInputStream;
    private long mSize;

    public InputData(InputStream inputStream, long size) {
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
