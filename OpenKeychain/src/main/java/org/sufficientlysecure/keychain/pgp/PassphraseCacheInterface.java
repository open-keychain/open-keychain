/*
 * Copyright (C) 2015 Dominik Schürmann <dominik@dominikschuermann.de>
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

import org.sufficientlysecure.keychain.util.Passphrase;

public interface PassphraseCacheInterface {
    public static class NoSecretKeyException extends Exception {
        public NoSecretKeyException() {
        }
    }


    public Passphrase getCachedPassphrase(long masterKeyId) throws NoSecretKeyException;

    public Passphrase getCachedSubkeyPassphrase(long masterKeyId, long subKeyId) throws NoSecretKeyException;

}
