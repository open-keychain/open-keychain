package org.sufficientlysecure.keychain.pgp;

import org.spongycastle.bcpg.CompressionAlgorithmTags;
import org.spongycastle.bcpg.HashAlgorithmTags;
import org.spongycastle.bcpg.SymmetricKeyAlgorithmTags;

public class PgpConstants {

    public static interface OpenKeychainSymmetricKeyAlgorithmTags extends SymmetricKeyAlgorithmTags {
        public static final int USE_PREFERRED = -1;
    }

    public static interface OpenKeychainHashAlgorithmTags extends HashAlgorithmTags {
        public static final int USE_PREFERRED = -1;
    }

    public static interface OpenKeychainCompressionAlgorithmTags extends CompressionAlgorithmTags {
        public static final int USE_PREFERRED = -1;
    }

    // most preferred is first
    public static final int[] PREFERRED_SYMMETRIC_ALGORITHMS = new int[]{
            SymmetricKeyAlgorithmTags.AES_256,
            SymmetricKeyAlgorithmTags.AES_192,
            SymmetricKeyAlgorithmTags.AES_128,
            SymmetricKeyAlgorithmTags.TWOFISH
    };

    public static final int[] PREFERRED_HASH_ALGORITHMS = new int[]{
            HashAlgorithmTags.SHA256,
            HashAlgorithmTags.SHA512,
            HashAlgorithmTags.SHA384,
            HashAlgorithmTags.SHA224,
            HashAlgorithmTags.RIPEMD160
    };

    public static final int[] PREFERRED_COMPRESSION_ALGORITHMS = new int[]{
            CompressionAlgorithmTags.ZLIB,
            CompressionAlgorithmTags.BZIP2,
            CompressionAlgorithmTags.ZIP
    };

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
     * kbsriram proposes 0xc0
     * we use 0x90, a good trade-off between usability and security against offline attacks
     */
    public static final int SECRET_KEY_ENCRYPTOR_S2K_COUNT = 0x90;
    public static final int SECRET_KEY_ENCRYPTOR_HASH_ALGO = HashAlgorithmTags.SHA256;
    public static final int SECRET_KEY_ENCRYPTOR_SYMMETRIC_ALGO = SymmetricKeyAlgorithmTags.AES_256;
    public static final int SECRET_KEY_SIGNATURE_HASH_ALGO = HashAlgorithmTags.SHA256;
    // NOTE: only SHA1 is supported for key checksum calculations.
    public static final int SECRET_KEY_SIGNATURE_CHECKSUM_HASH_ALGO = HashAlgorithmTags.SHA1;


}
