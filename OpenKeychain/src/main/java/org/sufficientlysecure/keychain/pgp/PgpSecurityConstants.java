/*
 * Copyright (C) 2017 Schürmann & Breitmoser GbR
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


import java.util.Arrays;
import java.util.HashSet;

import androidx.annotation.Nullable;

import org.bouncycastle.asn1.nist.NISTNamedCurves;
import org.bouncycastle.asn1.teletrust.TeleTrusTNamedCurves;
import org.bouncycastle.bcpg.CompressionAlgorithmTags;
import org.bouncycastle.bcpg.HashAlgorithmTags;
import org.bouncycastle.bcpg.PublicKeyAlgorithmTags;
import org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags;
import org.bouncycastle.crypto.ec.CustomNamedCurves;
import org.sufficientlysecure.keychain.pgp.SecurityProblem.InsecureBitStrength;
import org.sufficientlysecure.keychain.pgp.SecurityProblem.InsecureSigningAlgorithm;
import org.sufficientlysecure.keychain.pgp.SecurityProblem.InsecureEncryptionAlgorithm;
import org.sufficientlysecure.keychain.pgp.SecurityProblem.KeySecurityProblem;
import org.sufficientlysecure.keychain.pgp.SecurityProblem.NotSecureCurve;
import org.sufficientlysecure.keychain.pgp.SecurityProblem.EncryptionAlgorithmProblem;
import org.sufficientlysecure.keychain.pgp.SecurityProblem.UnidentifiedKeyProblem;


public class PgpSecurityConstants {

    /**
     * List of secure symmetric encryption algorithms
     * all other algorithms are rejected with OpenPgpDecryptionResult.RESULT_INSECURE
     */
    private static HashSet<Integer> sSecureSymmetricAlgorithms = new HashSet<>(Arrays.asList(
            // General remarks: We try to keep the list short to reduce attack surface
            // SymmetricKeyAlgorithmTags.IDEA, // Bad key schedule (weak keys), implementation difficulties (easy to make errors)
            SymmetricKeyAlgorithmTags.TRIPLE_DES, // RFC4880: "MUST implement TripleDES"
            SymmetricKeyAlgorithmTags.CAST5, // default in many gpg, pgp versions, 128 bit key, RFC4880: "SHOULD implement AES-128 and CAST5"
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

    public static EncryptionAlgorithmProblem checkSecureSymmetricAlgorithm(int id, byte[] sessionKey) {
        if (!sSecureSymmetricAlgorithms.contains(id)) {
            return new InsecureEncryptionAlgorithm(sessionKey, id);
        }
        return null;
    }

    /**
     * List of secure hash algorithms
     * all other algorithms are rejected with OpenPgpSignatureResult.RESULT_INSECURE
     */
    private static HashSet<Integer> sSecureHashAlgorithms = new HashSet<>(Arrays.asList(
            // MD5: broken
            HashAlgorithmTags.SHA1, //  RFC4880: "MUST implement SHA-1", TODO: disable when SHA256 is widely deployed
            HashAlgorithmTags.RIPEMD160, // same security properties as SHA1, TODO: disable when SHA256 is widely deployed
            // DOUBLE_SHA: not used widely
            // MD2: not used widely
            // TIGER_192: not used widely
            // HAVAL_5_160: not used widely
            HashAlgorithmTags.SHA256, // compatibility for old Mailvelope versions
            HashAlgorithmTags.SHA384, // affine padding attacks; unproven status of RSA-PKCSv15
            HashAlgorithmTags.SHA512
            // SHA224: issues with collision resistance of 112-bits, Not used widely
    ));

    static InsecureSigningAlgorithm checkSignatureAlgorithmForSecurityProblems(int hashAlgorithm) {
        if (!sSecureHashAlgorithms.contains(hashAlgorithm)) {
            return new InsecureSigningAlgorithm(hashAlgorithm);
        }
        return null;
    }

    /**
     * List of secure asymmetric algorithms in switch statement
     * all other algorithms are rejected with OpenPgpSignatureResult.RESULT_INSECURE or
     * OpenPgpDecryptionResult.RESULT_INSECURE
     */
    private static HashSet<String> sSecureCurves = new HashSet<>(Arrays.asList(
            NISTNamedCurves.getOID("P-256").getId(),
            NISTNamedCurves.getOID("P-384").getId(),
            NISTNamedCurves.getOID("P-521").getId(),
            CustomNamedCurves.getOID("secp256k1").getId(),
            TeleTrusTNamedCurves.getOID("brainpoolP256r1").getId(),
            TeleTrusTNamedCurves.getOID("brainpoolP384r1").getId(),
            TeleTrusTNamedCurves.getOID("brainpoolP512r1").getId(),
            CustomNamedCurves.getOID("curve25519").getId()
    ));

    static KeySecurityProblem checkForSecurityProblems(CanonicalizedPublicKey key) {
        long masterKeyId = key.getKeyRing().getMasterKeyId();
        long subKeyId = key.getKeyId();
        int algorithm = key.getAlgorithm();
        Integer bitStrength = key.getBitStrength();
        String curveOid = key.getCurveOid();

        return getKeySecurityProblem(masterKeyId, subKeyId, algorithm, bitStrength, curveOid);
    }

    @Nullable
    public static KeySecurityProblem getKeySecurityProblem(long masterKeyId, long subKeyId, int algorithm,
                                                           Integer bitStrength, String curveOid) {
        switch (algorithm) {
            case PublicKeyAlgorithmTags.RSA_GENERAL: {
                if (bitStrength < 2048) {
                    return new InsecureBitStrength(masterKeyId, subKeyId, algorithm, bitStrength);
                }
                return null;
            }
            // RSA_ENCRYPT, RSA_SIGN: deprecated in RFC 4880, use RSA_GENERAL with key flags
            case PublicKeyAlgorithmTags.ELGAMAL_ENCRYPT: {
                if (bitStrength < 2048) {
                    return new InsecureBitStrength(masterKeyId, subKeyId, algorithm, bitStrength);
                }
                return null;
            }
            case PublicKeyAlgorithmTags.DSA: {
                if (bitStrength < 2048) {
                    return new InsecureBitStrength(masterKeyId, subKeyId, algorithm, bitStrength);
                }
                return null;
            }
            case PublicKeyAlgorithmTags.ECDH:
            case PublicKeyAlgorithmTags.ECDSA: {
                if (!PgpSecurityConstants.sSecureCurves.contains(curveOid)) {
                    return new NotSecureCurve(masterKeyId, subKeyId, curveOid, algorithm);
                }
                return null;
            }
            case PublicKeyAlgorithmTags.EDDSA: {
                return null;
            }
            // ELGAMAL_GENERAL: deprecated in RFC 4880, use ELGAMAL_ENCRYPT
            // DIFFIE_HELLMAN: deprecated
            default:
                return new UnidentifiedKeyProblem(masterKeyId, subKeyId, algorithm);
        }
    }

    /**
     * These array is written as a list of preferred encryption algorithms into keys created by us.
     * Other implementations may choose to honor this selection.
     * (Most preferred is first)
     */
    public static final int[] PREFERRED_SYMMETRIC_ALGORITHMS = new int[]{
            SymmetricKeyAlgorithmTags.AES_256, // AES received most cryptanalysis over the years and is still secure!
            SymmetricKeyAlgorithmTags.AES_192,
            SymmetricKeyAlgorithmTags.AES_128,
    };

    /**
     * These array is written as a list of preferred hash algorithms into keys created by us.
     * Other implementations may choose to honor this selection.
     * (Most preferred is first)
     */
    public static final int[] PREFERRED_HASH_ALGORITHMS = new int[]{
            HashAlgorithmTags.SHA512, // If possible use SHA-512, this is state of the art!
    };

    /**
     * These array is written as a list of preferred compression algorithms into keys created by us.
     * Other implementations may choose to honor this selection.
     * (Most preferred is first)
     * <p>
     * REASON: See DEFAULT_COMPRESSION_ALGORITHM
     */
    public static final int[] PREFERRED_COMPRESSION_ALGORITHMS = new int[]{
            CompressionAlgorithmTags.ZIP,
            // ZLIB: the format provides no benefits over DEFLATE, and is more malleable
            // BZIP2: very slow
    };

    /**
     * Hash algorithm used to certify public keys
     */
    public static final int CERTIFY_HASH_ALGO = HashAlgorithmTags.SHA512;


    /**
     * Always use AES-256!
     * We always ignore the preferred encryption algos of the recipient!
     */
    public static final int DEFAULT_SYMMETRIC_ALGORITHM = SymmetricKeyAlgorithmTags.AES_256;

    public interface OpenKeychainSymmetricKeyAlgorithmTags extends SymmetricKeyAlgorithmTags {
        int USE_DEFAULT = -1;
    }

    /**
     * Always use SHA-512!
     * We always ignore the preferred hash algos of the recipient!
     */
    public static final int DEFAULT_HASH_ALGORITHM = HashAlgorithmTags.SHA512;

    public interface OpenKeychainHashAlgorithmTags extends HashAlgorithmTags {
        int USE_DEFAULT = -1;
    }

    /**
     * Compression is disabled by default.
     * <p>
     * The default compression algorithm is only used if explicitly enabled in the activity's
     * overflow menu or via the OpenPGP API's extra OpenPgpApi.EXTRA_ENABLE_COMPRESSION
     * <p>
     * REASON: Enabling compression can lead to a sidechannel. Consider a voting that is done via
     * OpenPGP. Compression can lead to different ciphertext lengths based on the user's voting.
     * This has happened in a voting done by Wikipedia (Google it).
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
     * <p>
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
