/*
 * Copyright (C) 2010-2014 Thialfihar <thi@thialfihar.org>
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

import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPPublicKeyRing;
import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSecretKeyRing;

public interface PgpKeyProvider {
    public static class NotFoundException extends Exception {
        public NotFoundException() {
        }

        public NotFoundException(String name) {
            super(name);
        }
    }

    public PGPPublicKeyRing getPublicKeyRingByMasterKeyId(long masterKeyId) throws NotFoundException;
    public PGPSecretKeyRing getSecretKeyRingByMasterKeyId(long masterKeyId) throws NotFoundException;
    public PGPPublicKeyRing getPublicKeyRingByKeyId(long keyId) throws NotFoundException;
    public PGPSecretKeyRing getSecretKeyRingByKeyId(long keyId) throws NotFoundException;
    public PGPPublicKey getPublicKeyByKeyId(long keyId) throws NotFoundException;
    public PGPSecretKey getSecretKeyByKeyId(long keyId) throws NotFoundException;
}
