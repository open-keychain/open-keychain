/*
 * Copyright (C) 2017 Christian Hagau <ach@hagau.se>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.sufficientlysecure.keychain.ssh.key;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;

public class SshEncodedData {
    private ByteArrayOutputStream mData;

    public SshEncodedData() {
        this(64);
    }

    public SshEncodedData(int initialLength) {
        mData = new ByteArrayOutputStream(initialLength);
    }

    public void putString(String string) {
        byte[] buffer = string.getBytes();
        putString(buffer);
    }

    public void putString(byte[] buffer) {
        putUInt32(buffer.length);
        mData.write(buffer, 0, buffer.length);
    }

    public void putMPInt(BigInteger mpInt) {
        byte buffer[] = mpInt.toByteArray();
        if ((buffer.length == 1) && (buffer[0] == 0)) {
            putUInt32(0);
        } else {
            putString(buffer);
        }
    }

    public void putUInt32(int uInt) {
        mData.write(uInt >> 24);
        mData.write(uInt >> 16);
        mData.write(uInt >> 8);
        mData.write(uInt);
    }

    public void putByte(byte octet) {
        mData.write(octet);
    }

    public void putBoolean(boolean flag) {
        if (flag) {
            mData.write(1);
        } else {
            mData.write(0);
        }
    }

    public byte[] getBytes() {

        return mData.toByteArray();
    }
}