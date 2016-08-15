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

import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey;

public enum KeyType {
    SIGN(0, 0xB6, 0xCE, 0xC7),
    ENCRYPT(1, 0xB8, 0xCF, 0xC8),
    AUTH(2, 0xA4, 0xD0, 0xC9),;

    private final int mIdx;
    private final int mSlot;
    private final int mTimestampObjectId;
    private final int mFingerprintObjectId;

    KeyType(final int idx, final int slot, final int timestampObjectId, final int fingerprintObjectId) {
        this.mIdx = idx;
        this.mSlot = slot;
        this.mTimestampObjectId = timestampObjectId;
        this.mFingerprintObjectId = fingerprintObjectId;
    }

    public static KeyType from(final CanonicalizedSecretKey key) {
        if (key.canSign() || key.canCertify()) {
            return SIGN;
        } else if (key.canEncrypt()) {
            return ENCRYPT;
        } else if (key.canAuthenticate()) {
            return AUTH;
        }
        return null;
    }

    public int getIdx() {
        return mIdx;
    }

    public int getSlot() {
        return mSlot;
    }

    public int getTimestampObjectId() {
        return mTimestampObjectId;
    }

    public int getFingerprintObjectId() {
        return mFingerprintObjectId;
    }
}
