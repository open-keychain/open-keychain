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

package org.sufficientlysecure.keychain.util;

import android.annotation.SuppressLint;
import android.app.Activity;

import org.spongycastle.bcpg.HashAlgorithmTags;
import org.spongycastle.openpgp.PGPEncryptedData;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;

import java.util.HashMap;

@SuppressLint("UseSparseArrays")
public class AlgorithmNames {
    Activity mActivity;

    HashMap<Integer, String> mEncryptionNames = new HashMap<Integer, String>();
    HashMap<Integer, String> mHashNames = new HashMap<Integer, String>();
    HashMap<Integer, String> mCompressionNames = new HashMap<Integer, String>();

    public AlgorithmNames(Activity context) {
        super();
        this.mActivity = context;

        mEncryptionNames.put(PGPEncryptedData.AES_128, "AES-128");
        mEncryptionNames.put(PGPEncryptedData.AES_192, "AES-192");
        mEncryptionNames.put(PGPEncryptedData.AES_256, "AES-256");
        mEncryptionNames.put(PGPEncryptedData.BLOWFISH, "Blowfish");
        mEncryptionNames.put(PGPEncryptedData.TWOFISH, "Twofish");
        mEncryptionNames.put(PGPEncryptedData.CAST5, "CAST5");
        mEncryptionNames.put(PGPEncryptedData.DES, "DES");
        mEncryptionNames.put(PGPEncryptedData.TRIPLE_DES, "Triple DES");
        mEncryptionNames.put(PGPEncryptedData.IDEA, "IDEA");

        mHashNames.put(HashAlgorithmTags.MD5, "MD5");
        mHashNames.put(HashAlgorithmTags.RIPEMD160, "RIPEMD-160");
        mHashNames.put(HashAlgorithmTags.SHA1, "SHA-1");
        mHashNames.put(HashAlgorithmTags.SHA224, "SHA-224");
        mHashNames.put(HashAlgorithmTags.SHA256, "SHA-256");
        mHashNames.put(HashAlgorithmTags.SHA384, "SHA-384");
        mHashNames.put(HashAlgorithmTags.SHA512, "SHA-512");

        mCompressionNames.put(Constants.choice.compression.none, mActivity.getString(R.string.choice_none)
                + " (" + mActivity.getString(R.string.compression_fast) + ")");
        mCompressionNames.put(Constants.choice.compression.zip,
                "ZIP (" + mActivity.getString(R.string.compression_fast) + ")");
        mCompressionNames.put(Constants.choice.compression.zlib,
                "ZLIB (" + mActivity.getString(R.string.compression_fast) + ")");
        mCompressionNames.put(Constants.choice.compression.bzip2,
                "BZIP2 (" + mActivity.getString(R.string.compression_very_slow) + ")");
    }

    public HashMap<Integer, String> getEncryptionNames() {
        return mEncryptionNames;
    }

    public void setEncryptionNames(HashMap<Integer, String> encryptionNames) {
        this.mEncryptionNames = encryptionNames;
    }

    public HashMap<Integer, String> getHashNames() {
        return mHashNames;
    }

    public void setHashNames(HashMap<Integer, String> hashNames) {
        this.mHashNames = hashNames;
    }

    public HashMap<Integer, String> getCompressionNames() {
        return mCompressionNames;
    }

    public void setCompressionNames(HashMap<Integer, String> compressionNames) {
        this.mCompressionNames = compressionNames;
    }

}
