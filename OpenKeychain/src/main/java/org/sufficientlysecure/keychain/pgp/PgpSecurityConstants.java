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

import org.spongycastle.asn1.nist.NISTNamedCurves;
import org.spongycastle.bcpg.CompressionAlgorithmTags;
import org.spongycastle.bcpg.HashAlgorithmTags;
import org.spongycastle.bcpg.PublicKeyAlgorithmTags;
import org.spongycastle.bcpg.SymmetricKeyAlgorithmTags;

import java.util.Arrays;
import java.util.HashSet;

/**
 * NIST requirements for 2011-2030 (http://www.keylength.com/en/4/):
 * - RSA: 2048 bit
 * - ECC: 224 bit
 * - Symmetric: 3TDEA
 * - Digital Signature (hash A): SHA-224 - SHA-512
 *
 * Extreme Decisions for Yahoo's End-to-End:
 * https://github.com/yahoo/end-to-end/issues/31
 * https://gist.github.com/coruus/68a8c65571e2b4225a69
 */
public class PgpSecurityConstants {

    /**
     * Whitelist of accepted symmetric encryption algorithms
     * all other algorithms are rejected with OpenPgpDecryptionResult.RESULT_INSECURE
     */
    private static HashSet<Integer> sSymmetricAlgorithmsWhitelist = new HashSet<>(Arrays.asList(
            // General remarks: We try to keep the whitelist short to reduce attack surface
            // TODO: block IDEA?: Bad key schedule (weak keys), implementation difficulties (easy to make errors)
            SymmetricKeyAlgorithmTags.IDEA,
            SymmetricKeyAlgorithmTags.TRIPLE_DES, // a MUST in RFC
            SymmetricKeyAlgorithmTags.CAST5, // default in many gpg, pgp versions, 128 bit key
            // BLOWFISH: Twofish is the successor
            // SAFER: not used widely
            // DES: < 128 bit security
            SymmetricKeyAlgorithmTags.AES_128,
            SymmetricKeyAlgorithmTags.AES_192,
            SymmetricKeyAlgorithmTags.AES_256,
            SymmetricKeyAlgorithmTags.TWOFISH // 128 bit
            // CAMELLIA_128: not used widely
            // CAMELLIA_192: not used widely
            // CAMELLIA_256: not used widely
    ));

    public static boolean isSecureSymmetricAlgorithm(int id) {
        return sSymmetricAlgorithmsWhitelist.contains(id);
    }

    /**
     * Whitelist of accepted hash algorithms
     * all other algorithms are rejected with OpenPgpSignatureResult.RESULT_INSECURE
     *
     * coorus:
     * Implementations SHOULD use SHA-512 for RSA or DSA signatures. They SHOULD NOT use SHA-384.
     * ((cite to affine padding attacks; unproven status of RSA-PKCSv15))
     *
     * Implementations MUST NOT sign SHA-224 hashes. They SHOULD NOT accept signatures over SHA-224 hashes.
     * ((collision resistance of 112-bits))
     * Implementations SHOULD NOT sign SHA-256 hashes. They MUST NOT default to signing SHA-256 hashes.
     */
    private static HashSet<Integer> sHashAlgorithmsWhitelist = new HashSet<>(Arrays.asList(
            // MD5: broken
            HashAlgorithmTags.SHA1, // TODO: disable when SHA256 is widely deployed
            HashAlgorithmTags.RIPEMD160, // same security properties as SHA1, TODO: disable when SHA256 is widely deployed
            // DOUBLE_SHA: not used widely
            // MD2: not used widely
            // TIGER_192: not used widely
            // HAVAL_5_160: not used widely
            HashAlgorithmTags.SHA256, // compatibility for old Mailvelope versions
            HashAlgorithmTags.SHA384,
            HashAlgorithmTags.SHA512
            // SHA224: Not used widely, Yahoo argues against it
    ));

    public static boolean isSecureHashAlgorithm(int id) {
        return sHashAlgorithmsWhitelist.contains(id);
    }

    /**
     * Whitelist of accepted asymmetric algorithms in switch statement
     * all other algorithms are rejected with OpenPgpSignatureResult.RESULT_INSECURE or
     * OpenPgpDecryptionResult.RESULT_INSECURE
     *
     * coorus:
     * Implementations MUST NOT accept, or treat any signature as valid, by an RSA key with
     * bitlength less than 1023 bits.
     * Implementations MUST NOT accept any RSA keys with bitlength less than 2047 bits after January 1, 2016.
     */
    private static HashSet<String> sCurveWhitelist = new HashSet<>(Arrays.asList(
            NISTNamedCurves.getOID("P-256").getId(),
            NISTNamedCurves.getOID("P-384").getId(),
            NISTNamedCurves.getOID("P-521").getId()
    ));

    public static boolean isSecureKey(CanonicalizedPublicKey key) {
        switch (key.getAlgorithm()) {
            case PublicKeyAlgorithmTags.RSA_GENERAL: {
                return (key.getBitStrength() >= 2048);
            }
            // RSA_ENCRYPT, RSA_SIGN: deprecated in RFC 4880, use RSA_GENERAL with key flags
            case PublicKeyAlgorithmTags.ELGAMAL_ENCRYPT: {
                return (key.getBitStrength() >= 2048);
            }
            case PublicKeyAlgorithmTags.DSA: {
                return (key.getBitStrength() >= 2048);
            }
            case PublicKeyAlgorithmTags.ECDH:
            case PublicKeyAlgorithmTags.ECDSA: {
                return PgpSecurityConstants.sCurveWhitelist.contains(key.getCurveOid());
            }
            // ELGAMAL_GENERAL: deprecated in RFC 4880, use ELGAMAL_ENCRYPT
            // DIFFIE_HELLMAN: unsure
            default:
                return false;
        }
    }

    /**
     * These array is written as a list of preferred encryption algorithms into keys created by us.
     * Other implementations may choose to honor this selection.
     * (Most preferred is first)
     *
     * REASON: See corresponding whitelist. AES received most cryptanalysis over the years
     * and is still secure!
     */
    public static final int[] PREFERRED_SYMMETRIC_ALGORITHMS = new int[]{
            SymmetricKeyAlgorithmTags.AES_256,
            SymmetricKeyAlgorithmTags.AES_192,
            SymmetricKeyAlgorithmTags.AES_128,
    };

    /**
     * These array is written as a list of preferred hash algorithms into keys created by us.
     * Other implementations may choose to honor this selection.
     * (Most preferred is first)
     *
     * REASON: See corresponding whitelist. If possible use SHA-512, this is state of the art!
     */
    public static final int[] PREFERRED_HASH_ALGORITHMS = new int[]{
            HashAlgorithmTags.SHA512,
    };

    /**
     * These array is written as a list of preferred compression algorithms into keys created by us.
     * Other implementations may choose to honor this selection.
     * (Most preferred is first)
     *
     * REASON: See DEFAULT_COMPRESSION_ALGORITHM
     */
    public static final int[] PREFERRED_COMPRESSION_ALGORITHMS = new int[]{
            CompressionAlgorithmTags.ZIP,
    };

    /**
     * Hash algorithm used to certify public keys
     */
    public static final int CERTIFY_HASH_ALGO = HashAlgorithmTags.SHA512;


    /**
     * Always use AES-256! We always ignore the preferred encryption algos of the recipient!
     *
     * coorus:
     * Implementations SHOULD ignore the symmetric algorithm preferences of a recipient's public key;
     * in particular, implementations MUST NOT choose an algorithm forbidden by this
     * document because a recipient prefers it.
     *
     * NEEDCITE downgrade attacks on TLS, other protocols
     */
    public static final int DEFAULT_SYMMETRIC_ALGORITHM = SymmetricKeyAlgorithmTags.AES_256;

    public interface OpenKeychainSymmetricKeyAlgorithmTags extends SymmetricKeyAlgorithmTags {
        int USE_DEFAULT = -1;
    }

    /**
     * Always use SHA-512!  We always ignore the preferred hash algos of the recipient!
     *
     * coorus:
     * Implementations MUST ignore the hash algorithm preferences of a recipient when signing
     * a message to a recipient. The difficulty of forging a signature under a given key,
     * using generic attacks on hash functions, is the difficulty of the weakest hash signed by that key.
     *
     * Implementations MUST default to using SHA-512 for RSA signatures,
     *
     * and either SHA-512 or the matched instance of SHA-2 for ECDSA signatures.
     * TODO: Ed25519
     * CITE: zooko's hash function table CITE: distinguishers on SHA-256
     */
    public static final int DEFAULT_HASH_ALGORITHM = HashAlgorithmTags.SHA512;

    public interface OpenKeychainHashAlgorithmTags extends HashAlgorithmTags {
        int USE_DEFAULT = -1;
    }

    /**
     * Compression is disabled by default.
     *
     * The default compression algorithm is only used if explicitly enabled in the activity's
     * overflow menu or via the OpenPGP API's extra OpenPgpApi.EXTRA_ENABLE_COMPRESSION
     *
     * REASON: Enabling compression can lead to a sidechannel. Consider a voting that is done via
     * OpenPGP. Compression can lead to different ciphertext lengths based on the user's voting.
     * This has happened in a voting done by Wikipedia (Google it).
     *
     * ZLIB: the format provides no benefits over DEFLATE, and is more malleable
     * BZIP2: very slow
     */
    public static final int DEFAULT_COMPRESSION_ALGORITHM = CompressionAlgorithmTags.ZIP;

    public interface OpenKeychainCompressionAlgorithmTags extends CompressionAlgorithmTags {
        int USE_DEFAULT = -1;
    }

    /**
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
     * Yahoo's End-to-End: 96=0x60 (65536 iterations) (https://github.com/yahoo/end-to-end/blob/master/src/javascript/crypto/e2e/openpgp/keyring.js)
     */
    public static final int SECRET_KEY_ENCRYPTOR_S2K_COUNT = 0x90;
    public static final int SECRET_KEY_ENCRYPTOR_HASH_ALGO = HashAlgorithmTags.SHA512;
    public static final int SECRET_KEY_ENCRYPTOR_SYMMETRIC_ALGO = SymmetricKeyAlgorithmTags.AES_256;
    public static final int SECRET_KEY_BINDING_SIGNATURE_HASH_ALGO = HashAlgorithmTags.SHA512;
    // NOTE: only SHA1 is supported for key checksum calculations in OpenPGP,
    // see http://tools.ietf.org/html/rfc488 0#section-5.5.3
    public static final int SECRET_KEY_SIGNATURE_CHECKSUM_HASH_ALGO = HashAlgorithmTags.SHA1;

}
