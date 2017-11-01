/*
 * Copyright (C) 2017 Christian Hagau <ach@hagau.se>
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

package org.sufficientlysecure.keychain.ssh.key;

import java.math.BigInteger;

public class SshRSAPublicKey extends SshPublicKey {
    public static final String KEY_ID = "ssh-rsa";

    private BigInteger mExponent;
    private BigInteger mModulus;

    public SshRSAPublicKey(BigInteger exponent, BigInteger modulus) {
        super(KEY_ID);
        mExponent = exponent;
        mModulus = modulus;
    }

    @Override
    protected void putData(SshEncodedData data) {
        data.putMPInt(mExponent);
        data.putMPInt(mModulus);
    }
}
