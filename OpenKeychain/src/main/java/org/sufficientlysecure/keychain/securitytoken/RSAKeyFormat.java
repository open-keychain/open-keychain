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

package org.sufficientlysecure.keychain.securitytoken;

import org.bouncycastle.bcpg.PublicKeyAlgorithmTags;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel.SubkeyAdd;


// 4.3.3.6 Algorithm Attributes
public class RSAKeyFormat extends KeyFormat {
    private int mModulusLength;
    private int mExponentLength;
    private RSAAlgorithmFormat mRSAAlgorithmFormat;

    public RSAKeyFormat(int modulusLength,
                        int exponentLength,
                        RSAAlgorithmFormat rsaAlgorithmFormat) {
        super(KeyFormatType.RSAKeyFormatType);
        mModulusLength = modulusLength;
        mExponentLength = exponentLength;
        mRSAAlgorithmFormat = rsaAlgorithmFormat;
    }

    public int getModulusLength() {
        return mModulusLength;
    }

    public int getExponentLength() {
        return mExponentLength;
    }

    public static KeyFormat fromBytes(byte[] bytes) {
        if (bytes.length < 6) {
            throw new IllegalArgumentException("Bad length for RSA attributes");
        }
        return new RSAKeyFormat(bytes[1] << 8 | bytes[2],
                bytes[3] << 8 | bytes[4],
                RSAKeyFormat.RSAAlgorithmFormat.from(bytes[5]));
    }

    public RSAAlgorithmFormat getAlgorithmFormat() {
        return mRSAAlgorithmFormat;
    }

    public enum RSAAlgorithmFormat {
        STANDARD((byte) 0x00, false, false),
        STANDARD_WITH_MODULUS((byte) 0x01, false, true),
        CRT((byte) 0x02, true, false),
        CRT_WITH_MODULUS((byte) 0x03, true, true);

        private byte mImportFormat;
        private boolean mIncludeModulus;
        private boolean mIncludeCrt;

        RSAAlgorithmFormat(byte importFormat, boolean includeCrt, boolean includeModulus) {
            mImportFormat = importFormat;
            mIncludeModulus = includeModulus;
            mIncludeCrt = includeCrt;
        }

        public static RSAAlgorithmFormat from(byte importFormatByte) {
            for (RSAAlgorithmFormat format : values()) {
                if (format.mImportFormat == importFormatByte) {
                    return format;
                }
            }
            return null;
        }

        public byte getImportFormat() {
            return mImportFormat;
        }

        public boolean isIncludeModulus() {
            return mIncludeModulus;
        }

        public boolean isIncludeCrt() {
            return mIncludeCrt;
        }
    }

    public void addToSaveKeyringParcel(SaveKeyringParcel.Builder builder, int keyFlags) {
        builder.addSubkeyAdd(SubkeyAdd.createSubkeyAdd(SaveKeyringParcel.Algorithm.RSA,
                mModulusLength, null, keyFlags, 0L));
    }
}
