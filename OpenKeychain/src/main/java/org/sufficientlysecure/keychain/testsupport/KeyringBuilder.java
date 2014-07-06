package org.sufficientlysecure.keychain.testsupport;

import org.spongycastle.bcpg.CompressionAlgorithmTags;
import org.spongycastle.bcpg.HashAlgorithmTags;
import org.spongycastle.bcpg.MPInteger;
import org.spongycastle.bcpg.PublicKeyAlgorithmTags;
import org.spongycastle.bcpg.PublicKeyPacket;
import org.spongycastle.bcpg.RSAPublicBCPGKey;
import org.spongycastle.bcpg.SignaturePacket;
import org.spongycastle.bcpg.SignatureSubpacket;
import org.spongycastle.bcpg.SignatureSubpacketTags;
import org.spongycastle.bcpg.SymmetricKeyAlgorithmTags;
import org.spongycastle.bcpg.UserIDPacket;
import org.spongycastle.bcpg.sig.Features;
import org.spongycastle.bcpg.sig.IssuerKeyID;
import org.spongycastle.bcpg.sig.KeyExpirationTime;
import org.spongycastle.bcpg.sig.KeyFlags;
import org.spongycastle.bcpg.sig.PreferredAlgorithms;
import org.spongycastle.bcpg.sig.SignatureCreationTime;
import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPPublicKeyRing;
import org.spongycastle.openpgp.PGPSignature;
import org.spongycastle.openpgp.operator.bc.BcKeyFingerprintCalculator;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing;

import java.math.BigInteger;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Created by art on 05/07/14.
 */
public class KeyringBuilder {


    private static final BigInteger modulus = new BigInteger(
            "cbab78d90d5f2cc0c54dd3c3953005a1" +
                    "e6b521f1ffa5465a102648bf7b91ec72" +
                    "f9c180759301587878caeb7333215620" +
                    "9f81ca5b3b94309d96110f6972cfc56a" +
                    "37fd6279f61d71f19b8f64b288e33829" +
                    "9dce133520f5b9b4253e6f4ba31ca36a" +
                    "fd87c2081b15f0b283e9350e370e181a" +
                    "23d31379101f17a23ae9192250db6540" +
                    "2e9cab2a275bc5867563227b197c8b13" +
                    "6c832a94325b680e144ed864fb00b9b8" +
                    "b07e13f37b40d5ac27dae63cd6a470a7" +
                    "b40fa3c7479b5b43e634850cc680b177" +
                    "8dd6b1b51856f36c3520f258f104db2f" +
                    "96b31a53dd74f708ccfcefccbe420a90" +
                    "1c37f1f477a6a4b15f5ecbbfd93311a6" +
                    "47bcc3f5f81c59dfe7252e3cd3be6e27"
            , 16
    );

    private static final BigInteger exponent = new BigInteger("010001", 16);

    public static UncachedKeyRing ring1() {
        return ringForModulus(new Date(1404566755000L), "OpenKeychain User (NOT A REAL KEY) <openkeychain@example.com>");
    }

    public static UncachedKeyRing ring2() {
        return ringForModulus(new Date(1404566755000L), "OpenKeychain User (NOT A REAL KEY) <openkeychain@example.com>");
    }

    private static UncachedKeyRing ringForModulus(Date date, String userIdString) {

        try {
            PGPPublicKey publicKey = createPgpPublicKey(modulus, date);
            UserIDPacket userId = createUserId(userIdString);
            SignaturePacket signaturePacket = createSignaturePacket(date);

            byte[] publicKeyEncoded = publicKey.getEncoded();
            byte[] userIdEncoded = userId.getEncoded();
            byte[] signaturePacketEncoded = signaturePacket.getEncoded();
            byte[] encodedRing = TestDataUtil.concatAll(
                    publicKeyEncoded,
                    userIdEncoded,
                    signaturePacketEncoded
            );

            PGPPublicKeyRing pgpPublicKeyRing = new PGPPublicKeyRing(
                    encodedRing, new BcKeyFingerprintCalculator());

            return UncachedKeyRing.decodeFromData(pgpPublicKeyRing.getEncoded());

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static SignaturePacket createSignaturePacket(Date date) {
        int signatureType = PGPSignature.POSITIVE_CERTIFICATION;
        long keyID = 0x15130BCF071AE6BFL;
        int keyAlgorithm = SignaturePacket.RSA_GENERAL;
        int hashAlgorithm = HashAlgorithmTags.SHA1;

        SignatureSubpacket[] hashedData = new SignatureSubpacket[]{
                new SignatureCreationTime(false, date),
                new KeyFlags(false, KeyFlags.CERTIFY_OTHER + KeyFlags.SIGN_DATA),
                new KeyExpirationTime(false, TimeUnit.DAYS.toSeconds(2)),
                new PreferredAlgorithms(SignatureSubpacketTags.PREFERRED_SYM_ALGS, false, new int[]{
                        SymmetricKeyAlgorithmTags.AES_256,
                        SymmetricKeyAlgorithmTags.AES_192,
                        SymmetricKeyAlgorithmTags.AES_128,
                        SymmetricKeyAlgorithmTags.CAST5,
                        SymmetricKeyAlgorithmTags.TRIPLE_DES
                }),
                new PreferredAlgorithms(SignatureSubpacketTags.PREFERRED_HASH_ALGS, false, new int[]{
                        HashAlgorithmTags.SHA256,
                        HashAlgorithmTags.SHA1,
                        HashAlgorithmTags.SHA384,
                        HashAlgorithmTags.SHA512,
                        HashAlgorithmTags.SHA224
                }),
                new PreferredAlgorithms(SignatureSubpacketTags.PREFERRED_COMP_ALGS, false, new int[]{
                        CompressionAlgorithmTags.ZLIB,
                        CompressionAlgorithmTags.BZIP2,
                        CompressionAlgorithmTags.ZIP
                }),
                new Features(false, Features.FEATURE_MODIFICATION_DETECTION),
                // can't do keyserver prefs
        };
        SignatureSubpacket[] unhashedData = new SignatureSubpacket[]{
                new IssuerKeyID(false, new BigInteger("15130BCF071AE6BF", 16).toByteArray())
        };
        byte[] fingerPrint = new BigInteger("522c", 16).toByteArray();
        MPInteger[] signature = new MPInteger[]{
                new MPInteger(new BigInteger(
                        "b065c071d3439d5610eb22e5b4df9e42" +
                                "ed78b8c94f487389e4fc98e8a75a043f" +
                                "14bf57d591811e8e7db2d31967022d2e" +
                                "e64372829183ec51d0e20c42d7a1e519" +
                                "e9fa22cd9db90f0fd7094fd093b78be2" +
                                "c0db62022193517404d749152c71edc6" +
                                "fd48af3416038d8842608ecddebbb11c" +
                                "5823a4321d2029b8993cb017fa8e5ad7" +
                                "8a9a618672d0217c4b34002f1a4a7625" +
                                "a514b6a86475e573cb87c64d7069658e" +
                                "627f2617874007a28d525e0f87d93ca7" +
                                "b15ad10dbdf10251e542afb8f9b16cbf" +
                                "7bebdb5fe7e867325a44e59cad0991cb" +
                                "239b1c859882e2ebb041b80e5cdc3b40" +
                                "ed259a8a27d63869754c0881ccdcb50f" +
                                "0564fecdc6966be4a4b87a3507a9d9be", 16
                ))
        };
        return new SignaturePacket(signatureType,
                keyID,
                keyAlgorithm,
                hashAlgorithm,
                hashedData,
                unhashedData,
                fingerPrint,
                signature);
    }

    private static PGPPublicKey createPgpPublicKey(BigInteger modulus, Date date) throws PGPException {
        PublicKeyPacket publicKeyPacket = new PublicKeyPacket(PublicKeyAlgorithmTags.RSA_GENERAL, date, new RSAPublicBCPGKey(modulus, exponent));
        return new PGPPublicKey(
                publicKeyPacket, new BcKeyFingerprintCalculator());
    }

    private static UserIDPacket createUserId(String userId) {
        return new UserIDPacket(userId);
    }

}
