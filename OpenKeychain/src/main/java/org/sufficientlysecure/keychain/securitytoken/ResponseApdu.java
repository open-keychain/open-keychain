/*
 * Copyright (C) 2016 Vincent Breitmoser <look@my.amazin.horse>
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

import java.util.Arrays;

import com.google.auto.value.AutoValue;


/** A response APDU as defined in ISO/IEC 7816-4. */
@AutoValue
@SuppressWarnings("WeakerAccess")
public abstract class ResponseApdu {
    private static final int APDU_SW_SUCCESS = 0x9000;

    public abstract byte[] getData();
    public abstract int getSw1();
    public abstract int getSw2();

    public static ResponseApdu fromBytes(byte[] apdu) {
        if (apdu.length < 2) {
            throw new IllegalArgumentException("Response apdu must be 2 bytes or larger!");
        }
        byte[] data = Arrays.copyOfRange(apdu, 0, apdu.length - 2);
        int sw1 = apdu[apdu.length -2] & 0xff;
        int sw2 = apdu[apdu.length -1] & 0xff;
        return new AutoValue_ResponseApdu(data, sw1, sw2);
    }

    public int getSw() {
        return (getSw1() << 8) | getSw2();
    }

    public boolean isSuccess() {
        return getSw() == APDU_SW_SUCCESS;
    }

    public byte[] toBytes() {
        byte[] data = getData();
        byte[] bytes = new byte[data.length + 2];
        System.arraycopy(data, 0, bytes, 0, data.length);

        bytes[bytes.length -2] = (byte) getSw1();
        bytes[bytes.length -1] = (byte) getSw2();

        return bytes;
    }
}
