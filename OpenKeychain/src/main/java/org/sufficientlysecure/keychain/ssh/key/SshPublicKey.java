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

import android.util.Base64;

public abstract class SshPublicKey {
    protected SshEncodedData mData;

    private String mKeyType;

    public SshPublicKey(String keytype) {
        mData = new SshEncodedData();
        mKeyType = keytype;
        mData.putString(mKeyType);
    }

    protected abstract void putData(SshEncodedData data);

    public String getPublicKeyBlob() {
        String publicKeyBlob = "";
        publicKeyBlob += mKeyType + " ";

        putData(mData);

        String keyBlob = Base64.encodeToString(mData.getBytes(), Base64.NO_WRAP);
        publicKeyBlob += keyBlob;

        return publicKeyBlob;
    }
}
