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

public class SshEd25519PublicKey extends SshPublicKey {
    public static final String KEY_ID = "ssh-ed25519";

    private byte[] mAbyte;

    public SshEd25519PublicKey(byte[] aByte) {
        super(KEY_ID);
        mAbyte = aByte;
    }

    @Override
    protected void putData(SshEncodedData data) {
        data.putString(mAbyte);
    }
}
