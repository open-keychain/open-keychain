/*
 * Copyright (C) 2014 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2014 Vincent Breitmoser <v.breitmoser@mugenguild.com>
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

package org.sufficientlysecure.keychain.pgp;

import org.spongycastle.bcpg.sig.KeyFlags;
import org.spongycastle.openpgp.PGPSecretKey;

import java.io.IOException;
import java.io.OutputStream;

public class UncachedSecretKey extends UncachedPublicKey {

    public static final int CERTIFY_OTHER = KeyFlags.CERTIFY_OTHER;
    public static final int SIGN_DATA = KeyFlags.SIGN_DATA;
    public static final int ENCRYPT_COMMS = KeyFlags.ENCRYPT_COMMS;
    public static final int ENCRYPT_STORAGE = KeyFlags.ENCRYPT_STORAGE;
    public static final int AUTHENTICATION = KeyFlags.AUTHENTICATION;

    final PGPSecretKey mSecretKey;

    public UncachedSecretKey(PGPSecretKey secretKey) {
        super(secretKey.getPublicKey());
        mSecretKey = secretKey;
    }

    @Deprecated
    public PGPSecretKey getSecretKeyExternal() {
        return mSecretKey;
    }

    public void encodeSecretKey(OutputStream os) throws IOException {
        mSecretKey.encode(os);
    }

}
