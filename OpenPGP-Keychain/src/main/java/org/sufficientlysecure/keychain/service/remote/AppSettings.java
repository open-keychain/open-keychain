/*
 * Copyright (C) 2013 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.sufficientlysecure.keychain.service.remote;

import org.spongycastle.bcpg.HashAlgorithmTags;
import org.spongycastle.openpgp.PGPEncryptedData;
import org.sufficientlysecure.keychain.Id;

public class AppSettings {
    private String packageName;
    private byte[] packageSignature;
    private long keyId = Id.key.none;
    private int encryptionAlgorithm;
    private int hashAlgorithm;
    private int compression;

    public AppSettings() {

    }

    public AppSettings(String packageName, byte[] packageSignature) {
        super();
        this.packageName = packageName;
        this.packageSignature = packageSignature;
        // defaults:
        this.encryptionAlgorithm = PGPEncryptedData.AES_256;
        this.hashAlgorithm = HashAlgorithmTags.SHA512;
        this.compression = Id.choice.compression.zlib;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public byte[] getPackageSignature() {
        return packageSignature;
    }

    public void setPackageSignature(byte[] packageSignature) {
        this.packageSignature = packageSignature;
    }

    public long getKeyId() {
        return keyId;
    }

    public void setKeyId(long scretKeyId) {
        this.keyId = scretKeyId;
    }

    public int getEncryptionAlgorithm() {
        return encryptionAlgorithm;
    }

    public void setEncryptionAlgorithm(int encryptionAlgorithm) {
        this.encryptionAlgorithm = encryptionAlgorithm;
    }

    public int getHashAlgorithm() {
        return hashAlgorithm;
    }

    public void setHashAlgorithm(int hashAlgorithm) {
        this.hashAlgorithm = hashAlgorithm;
    }

    public int getCompression() {
        return compression;
    }

    public void setCompression(int compression) {
        this.compression = compression;
    }

}
