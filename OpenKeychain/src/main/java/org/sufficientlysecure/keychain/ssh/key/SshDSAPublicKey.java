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

public class SshDSAPublicKey extends SshPublicKey {
    public static final String KEY_ID = "ssh-dss";

    private BigInteger mP;
    private BigInteger mQ;
    private BigInteger mG;
    private BigInteger mY;

    public SshDSAPublicKey(BigInteger p, BigInteger q, BigInteger g, BigInteger y) {
        super(KEY_ID);
        mP = p;
        mQ = q;
        mG = g;
        mY = y;
    }

    @Override
    protected void putData(SshEncodedData data) {
        data.putMPInt(mP);
        data.putMPInt(mQ);
        data.putMPInt(mG);
        data.putMPInt(mY);
    }
}
