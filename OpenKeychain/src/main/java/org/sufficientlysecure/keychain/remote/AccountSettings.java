/*
 * Copyright (C) 2013-2014 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.sufficientlysecure.keychain.remote;

import org.spongycastle.bcpg.CompressionAlgorithmTags;
import org.spongycastle.bcpg.HashAlgorithmTags;
import org.spongycastle.openpgp.PGPEncryptedData;
import org.sufficientlysecure.keychain.Constants;

public class AccountSettings {
    private String mAccountName;
    private long mKeyId = Constants.key.none;
    private int mEncryptionAlgorithm;
    private int mHashAlgorithm;
    private int mCompression;

    public AccountSettings() {

    }

    public AccountSettings(String accountName) {
        super();
        this.mAccountName = accountName;

        // defaults:
        this.mEncryptionAlgorithm = PGPEncryptedData.AES_256;
        this.mHashAlgorithm = HashAlgorithmTags.SHA512;
        this.mCompression = CompressionAlgorithmTags.ZLIB;
    }

    public String getAccountName() {
        return mAccountName;
    }

    public void setAccountName(String mAccountName) {
        this.mAccountName = mAccountName;
    }

    public long getKeyId() {
        return mKeyId;
    }

    public void setKeyId(long scretKeyId) {
        this.mKeyId = scretKeyId;
    }

    public int getEncryptionAlgorithm() {
        return mEncryptionAlgorithm;
    }

    public void setEncryptionAlgorithm(int encryptionAlgorithm) {
        this.mEncryptionAlgorithm = encryptionAlgorithm;
    }

    public int getHashAlgorithm() {
        return mHashAlgorithm;
    }

    public void setHashAlgorithm(int hashAlgorithm) {
        this.mHashAlgorithm = hashAlgorithm;
    }

    public int getCompression() {
        return mCompression;
    }

    public void setCompression(int compression) {
        this.mCompression = compression;
    }

}
