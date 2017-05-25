/*
 * Copyright (C) 2016 Nikita Mikhailov <nikita.s.mikhailov@gmail.com>
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

    public RSAAlgorithmFormat getAlgorithmFormat() {
        return mRSAAlgorithmFormat;
    }

    public enum RSAAlgorithmFormat {
        STANDARD((byte) 0, false, false),
        STANDARD_WITH_MODULUS((byte) 1, false, true),
        CRT((byte) 2, true, false),
        CRT_WITH_MODULUS((byte) 3, true, true);

        private byte mValue;
        private boolean mIncludeModulus;
        private boolean mIncludeCrt;

        RSAAlgorithmFormat(byte value, boolean includeCrt, boolean includeModulus) {
            mValue = value;
            mIncludeModulus = includeModulus;
            mIncludeCrt = includeCrt;
        }

        public static RSAAlgorithmFormat from(byte b) {
            for (RSAAlgorithmFormat format : values()) {
                if (format.mValue == b) {
                    return format;
                }
            }
            return null;
        }

        public byte getValue() {
            return mValue;
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
