/*
 * Copyright (C) 2015 Dominik Schürmann <dominik@dominikschuermann.de>
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

import org.spongycastle.bcpg.CompressionAlgorithmTags;
import org.spongycastle.bcpg.HashAlgorithmTags;
import org.spongycastle.bcpg.SymmetricKeyAlgorithmTags;

import java.util.HashSet;

/**
 * NIST requirements for 2011-2030 (http://www.keylength.com/en/4/):
 * - RSA: 2048 bit
 * - ECC: 224 bit
 * - Symmetric: 3TDEA
 * - Digital Signature (hash A): SHA-224 - SHA-512
 */
public class PgpConstants {

//    public interface MIN_REQUIREMENT {
//        int MIN_BITS;
//        int BINDING_SIGNATURE_HASH_ALGO; // for User IDs, subkeys,...
//        int SYMMETRIC_ALGO;
//    }
    // https://tools.ietf.org/html/rfc6637#section-13


    // PgpDecryptVerify: Secure Algorithms Whitelist
    // all other algorithms will be rejected with OpenPgpDecryptionResult.RESULT_INSECURE
    public static HashSet<Integer> sSymmetricAlgorithmsWhitelist = new HashSet<>();
    static {
        sSymmetricAlgorithmsWhitelist.add(SymmetricKeyAlgorithmTags.AES_256);
        sSymmetricAlgorithmsWhitelist.add(SymmetricKeyAlgorithmTags.AES_192);
        sSymmetricAlgorithmsWhitelist.add(SymmetricKeyAlgorithmTags.AES_128);
        sSymmetricAlgorithmsWhitelist.add(SymmetricKeyAlgorithmTags.TWOFISH);
    }

    // all other algorithms will be rejected with OpenPgpSignatureResult.RESULT_INVALID_INSECURE
    public static HashSet<Integer> sHashAlgorithmsWhitelist = new HashSet<>();
    static {
        sHashAlgorithmsWhitelist.add(HashAlgorithmTags.SHA256);
        sHashAlgorithmsWhitelist.add(HashAlgorithmTags.SHA512);
        sHashAlgorithmsWhitelist.add(HashAlgorithmTags.SHA384);
        sHashAlgorithmsWhitelist.add(HashAlgorithmTags.SHA224);
        sHashAlgorithmsWhitelist.add(HashAlgorithmTags.SHA1);
        sHashAlgorithmsWhitelist.add(HashAlgorithmTags.RIPEMD160);
    }

    /*
     * Most preferred is first
     * These arrays are written as preferred algorithms into the keys on creation.
     * Other implementations may choose to honor this selection.
     */
    public static final int[] PREFERRED_SYMMETRIC_ALGORITHMS = new int[]{
            SymmetricKeyAlgorithmTags.AES_256,
            SymmetricKeyAlgorithmTags.AES_192,
            SymmetricKeyAlgorithmTags.AES_128,
            SymmetricKeyAlgorithmTags.TWOFISH
    };

    // NOTE: some implementations do not support SHA512, thus we choose SHA256 as default (Mailvelope?)
    public static final int[] PREFERRED_HASH_ALGORITHMS = new int[]{
            HashAlgorithmTags.SHA256,
            HashAlgorithmTags.SHA512,
            HashAlgorithmTags.SHA384,
            HashAlgorithmTags.SHA224,
    };

    /*
     * Prefer ZIP
     * "ZLIB provides no benefit over ZIP and is more malleable"
     * - (OpenPGP WG mailinglist: "[openpgp] Intent to deprecate: Insecure primitives")
     * BZIP2: very slow
     */
    public static final int[] PREFERRED_COMPRESSION_ALGORITHMS = new int[]{
            CompressionAlgorithmTags.ZIP,
            CompressionAlgorithmTags.ZLIB,
            CompressionAlgorithmTags.BZIP2
    };

    public static final int CERTIFY_HASH_ALGO = HashAlgorithmTags.SHA256;


    public static final int DEFAULT_SYMMETRIC_ALGORITHM = SymmetricKeyAlgorithmTags.AES_256;
    public interface OpenKeychainSymmetricKeyAlgorithmTags extends SymmetricKeyAlgorithmTags {
        int USE_DEFAULT = -1;
    }

    public static final int DEFAULT_HASH_ALGORITHM = HashAlgorithmTags.SHA256;
    public interface OpenKeychainHashAlgorithmTags extends HashAlgorithmTags {
        int USE_DEFAULT = -1;
    }

    public static final int DEFAULT_COMPRESSION_ALGORITHM = CompressionAlgorithmTags.ZIP;
    public interface OpenKeychainCompressionAlgorithmTags extends CompressionAlgorithmTags {
        int USE_DEFAULT = -1;
    }

    /*
     * Note: s2kcount is a number between 0 and 0xff that controls the
     * number of times to iterate the password hash before use. More
     * iterations are useful against offline attacks, as it takes more
     * time to check each password. The actual number of iterations is
     * rather complex, and also depends on the hash function in use.
     * Refer to Section 3.7.1.3 in rfc4880.txt. Bigger numbers give
     * you more iterations.  As a rough rule of thumb, when using
     * SHA256 as the hashing function, 0x10 gives you about 64
     * iterations, 0x20 about 128, 0x30 about 256 and so on till 0xf0,
     * or about 1 million iterations. The maximum you can go to is
     * 0xff, or about 2 million iterations.
     * from http://kbsriram.com/2013/01/generating-rsa-keys-with-bouncycastle.html
     *
     * Bouncy Castle default: 0x60
     * kbsriram proposes: 0xc0
     * OpenKeychain: 0x90
     */
    public static final int SECRET_KEY_ENCRYPTOR_S2K_COUNT = 0x90;
    public static final int SECRET_KEY_ENCRYPTOR_HASH_ALGO = HashAlgorithmTags.SHA256;
    public static final int SECRET_KEY_ENCRYPTOR_SYMMETRIC_ALGO = SymmetricKeyAlgorithmTags.AES_256;
    public static final int SECRET_KEY_BINDING_SIGNATURE_HASH_ALGO = HashAlgorithmTags.SHA256;
    // NOTE: only SHA1 is supported for key checksum calculations in OpenPGP,
    // see http://tools.ietf.org/html/rfc488 0#section-5.5.3
    public static final int SECRET_KEY_SIGNATURE_CHECKSUM_HASH_ALGO = HashAlgorithmTags.SHA1;

}
