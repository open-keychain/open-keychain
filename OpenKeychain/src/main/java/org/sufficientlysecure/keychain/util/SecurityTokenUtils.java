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

package org.sufficientlysecure.keychain.util;

import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.encoders.Hex;
import org.sufficientlysecure.keychain.securitytoken.KeyFormat;
import org.sufficientlysecure.keychain.securitytoken.KeyType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.interfaces.RSAPrivateCrtKey;

public class SecurityTokenUtils {
    public static byte[] createPrivKeyTemplate(RSAPrivateCrtKey secretKey, KeyType slot,
                                               KeyFormat format) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream(),
                template = new ByteArrayOutputStream(),
                data = new ByteArrayOutputStream(),
                res = new ByteArrayOutputStream();

        int expLengthBytes = (format.getExponentLength() + 7) / 8;
        // Public exponent
        template.write(new byte[]{(byte) 0x91, (byte) expLengthBytes});
        writeBits(data, secretKey.getPublicExponent(), expLengthBytes);

        // Prime P, length 128
        template.write(Hex.decode("928180"));
        writeBits(data, secretKey.getPrimeP(), 128);

        // Prime Q, length 128
        template.write(Hex.decode("938180"));
        writeBits(data, secretKey.getPrimeQ(), 128);


        if (format.getAlgorithmFormat().isIncludeCrt()) {
            // Coefficient (1/q mod p), length 128
            template.write(Hex.decode("948180"));
            writeBits(data, secretKey.getCrtCoefficient(), 128);

            // Prime exponent P (d mod (p - 1)), length 128
            template.write(Hex.decode("958180"));
            writeBits(data, secretKey.getPrimeExponentP(), 128);

            // Prime exponent Q (d mod (1 - 1)), length 128
            template.write(Hex.decode("968180"));
            writeBits(data, secretKey.getPrimeExponentQ(), 128);
        }

        if (format.getAlgorithmFormat().isIncludeModulus()) {
            // Modulus, length 256, last item in private key template
            template.write(Hex.decode("97820100"));
            writeBits(data, secretKey.getModulus(), 256);
        }

        // Bundle up

        // Ext header list data
        // Control Reference Template to indicate the private key
        stream.write(slot.getSlot());
        stream.write(0);

        // Cardholder private key template
        stream.write(Hex.decode("7F48"));
        stream.write(encodeLength(template.size()));
        stream.write(template.toByteArray());

        // Concatenation of key data as defined in DO 7F48
        stream.write(Hex.decode("5F48"));
        stream.write(encodeLength(data.size()));
        stream.write(data.toByteArray());


        // Result tlv
        res.write(Hex.decode("4D"));
        res.write(encodeLength(stream.size()));
        res.write(stream.toByteArray());

        return res.toByteArray();
    }

    public static byte[] encodeLength(int len) {
        if (len < 0) {
            throw new IllegalArgumentException("length is negative");
        } else if (len >= 16777216) {
            throw new IllegalArgumentException("length is too big: " + len);
        }
        byte[] res;
        if (len < 128) {
            res = new byte[1];
            res[0] = (byte) len;
        } else if (len < 256) {
            res = new byte[2];
            res[0] = -127;
            res[1] = (byte) len;
        } else if (len < 65536) {
            res = new byte[3];
            res[0] = -126;
            res[1] = (byte) (len / 256);
            res[2] = (byte) (len % 256);
        } else {
            res = new byte[4];

            res[0] = -125;
            res[1] = (byte) (len / 65536);
            res[2] = (byte) (len / 256);
            res[3] = (byte) (len % 256);
        }
        return res;
    }

    public static void writeBits(ByteArrayOutputStream stream, BigInteger value, int width) {
        if (value.signum() == -1) {
            throw new IllegalArgumentException("value is negative");
        } else if (width <= 0) {
            throw new IllegalArgumentException("width <= 0");
        }

        byte[] prime = value.toByteArray();
        int stripIdx = 0;
        while (prime[stripIdx] == 0 && stripIdx + 1 < prime.length) {
            stripIdx++;
        }

        if (prime.length - stripIdx > width) {
            throw new IllegalArgumentException("not enough width to fit value: "
                    + prime.length + "/" + width);
        }
        byte[] res = new byte[width];
        int empty = width - (prime.length - stripIdx);

        System.arraycopy(prime, stripIdx, res, Math.max(0, empty), Math.min(prime.length, width));

        stream.write(res, 0, width);
        Arrays.fill(res, (byte) 0);
        Arrays.fill(prime, (byte) 0);
    }
}
