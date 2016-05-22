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

// 4.3.3.6 Algorithm Attributes
public class KeyFormat {
    private int mAlgorithmId;
    private int mModulusLength;
    private int mExponentLength;
    private AlgorithmFormat mAlgorithmFormat;

    public KeyFormat(byte[] bytes) {
        mAlgorithmId = bytes[0];
        mModulusLength = bytes[1] << 8 | bytes[2];
        mExponentLength = bytes[3] << 8 | bytes[4];
        mAlgorithmFormat = AlgorithmFormat.from(bytes[5]);

        if (mAlgorithmId != 1) { // RSA
            throw new IllegalArgumentException("Unsupported Algorithm id " + mAlgorithmId);
        }
    }

    public int getAlgorithmId() {
        return mAlgorithmId;
    }

    public int getModulusLength() {
        return mModulusLength;
    }

    public int getExponentLength() {
        return mExponentLength;
    }

    public AlgorithmFormat getAlgorithmFormat() {
        return mAlgorithmFormat;
    }

    public enum AlgorithmFormat {
        STANDARD(0, false, false),
        STANDARD_WITH_MODULUS(1, false, true),
        CRT(2, true, false),
        CRT_WITH_MODULUS(3, true, true);

        private int mValue;
        private boolean mIncludeModulus;
        private boolean mIncludeCrt;

        AlgorithmFormat(int value, boolean includeCrt, boolean includeModulus) {
            mValue = value;
            mIncludeModulus = includeModulus;
            mIncludeCrt = includeCrt;
        }

        public static AlgorithmFormat from(byte b) {
            for (AlgorithmFormat format : values()) {
                if (format.mValue == b) {
                    return format;
                }
            }
            return null;
        }

        public boolean isIncludeModulus() {
            return mIncludeModulus;
        }

        public boolean isIncludeCrt() {
            return mIncludeCrt;
        }
    }
}
