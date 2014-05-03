package org.sufficientlysecure.keychain.pgp;

import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.PGPPrivateKey;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPPublicKeyRing;
import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSignature;
import org.spongycastle.openpgp.PGPSignatureGenerator;
import org.spongycastle.openpgp.PGPSignatureSubpacketGenerator;
import org.spongycastle.openpgp.PGPSignatureSubpacketVector;
import org.spongycastle.openpgp.PGPUtil;
import org.spongycastle.openpgp.PGPV3SignatureGenerator;
import org.spongycastle.openpgp.operator.PBESecretKeyDecryptor;
import org.spongycastle.openpgp.operator.PublicKeyDataDecryptorFactory;
import org.spongycastle.openpgp.operator.jcajce.JcaPGPContentSignerBuilder;
import org.spongycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;
import org.spongycastle.openpgp.operator.jcajce.JcePublicKeyDataDecryptorFactoryBuilder;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralMsgIdException;
import org.sufficientlysecure.keychain.util.IterableIterator;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.util.List;

public class CachedSecretKey {

    // this is the parent key ring
    private final CachedSecretKeyRing mRing;

    private final PGPSecretKey mKey;
    private PGPPrivateKey mPrivateKey = null;

    CachedSecretKey(CachedSecretKeyRing ring, PGPSecretKey key) {
        mRing = ring;
        mKey = key;
    }

    public CachedSecretKeyRing getRing() {
        return mRing;
    }

    public void unlock(String passphrase) throws PgpGeneralException {
        try {
            PBESecretKeyDecryptor keyDecryptor = new JcePBESecretKeyDecryptorBuilder().setProvider(
                    Constants.BOUNCY_CASTLE_PROVIDER_NAME).build(passphrase.toCharArray());
            mPrivateKey = mKey.extractPrivateKey(keyDecryptor);
        } catch (PGPException e) {
            throw new PgpGeneralException("error extracting key!", e);
        }
        if(mPrivateKey == null) {
            throw new PgpGeneralException("error extracting key (bad passphrase?)");
        }
    }

    public PGPSignatureGenerator getSignatureGenerator(int hashAlgo, boolean cleartext)
            throws PgpGeneralException {
        if(mPrivateKey == null) {
            throw new PrivateKeyNotUnlockedException();
        }

        // content signer based on signing key algorithm and chosen hash algorithm
        JcaPGPContentSignerBuilder contentSignerBuilder = new JcaPGPContentSignerBuilder(
                mKey.getPublicKey().getAlgorithm(), hashAlgo)
                .setProvider(Constants.BOUNCY_CASTLE_PROVIDER_NAME);

        int signatureType;
        if (cleartext) {
            // for sign-only ascii text
            signatureType = PGPSignature.CANONICAL_TEXT_DOCUMENT;
        } else {
            signatureType = PGPSignature.BINARY_DOCUMENT;
        }

        try {
            PGPSignatureGenerator signatureGenerator = new PGPSignatureGenerator(contentSignerBuilder);
            signatureGenerator.init(signatureType, mPrivateKey);

            PGPSignatureSubpacketGenerator spGen = new PGPSignatureSubpacketGenerator();
            spGen.setSignerUserID(false, mRing.getPrimaryUserId());
            signatureGenerator.setHashedSubpackets(spGen.generate());
            return signatureGenerator;
        } catch(PGPException e) {
            throw new PgpGeneralException("Error initializing signature!", e);
        }
    }

    public PGPV3SignatureGenerator getV3SignatureGenerator(int hashAlgo, boolean cleartext)
            throws PgpGeneralException {
        if(mPrivateKey == null) {
            throw new PrivateKeyNotUnlockedException();
        }

        // content signer based on signing key algorithm and chosen hash algorithm
        JcaPGPContentSignerBuilder contentSignerBuilder = new JcaPGPContentSignerBuilder(
                mKey.getPublicKey().getAlgorithm(), hashAlgo)
                .setProvider(Constants.BOUNCY_CASTLE_PROVIDER_NAME);

        int signatureType;
        if (cleartext) {
            // for sign-only ascii text
            signatureType = PGPSignature.CANONICAL_TEXT_DOCUMENT;
        } else {
            signatureType = PGPSignature.BINARY_DOCUMENT;
        }

        try {
            PGPV3SignatureGenerator signatureV3Generator = new PGPV3SignatureGenerator(contentSignerBuilder);
            signatureV3Generator.init(signatureType, mPrivateKey);
            return signatureV3Generator;
        } catch(PGPException e) {
            throw new PgpGeneralException("Error initializing signature!", e);
        }
    }

    public PublicKeyDataDecryptorFactory getDecryptorFactory() {
        if(mPrivateKey == null) {
            throw new PrivateKeyNotUnlockedException();
        }
        return new JcePublicKeyDataDecryptorFactoryBuilder()
                .setProvider(Constants.BOUNCY_CASTLE_PROVIDER_NAME).build(mPrivateKey);
    }

    /**
     * Certify the given pubkeyid with the given masterkeyid.
     *
     * @param publicKeyRing    Keyring to add certification to.
     * @param userIds          User IDs to certify, must not be null or empty
     * @return A keyring with added certifications
     */
    public UncachedKeyRing certifyUserIds(CachedPublicKeyRing publicKeyRing, List<String> userIds)
            throws PgpGeneralMsgIdException, NoSuchAlgorithmException, NoSuchProviderException,
            PGPException, SignatureException {

        if(mPrivateKey == null) {
            throw new PrivateKeyNotUnlockedException();
        }

        // create a signatureGenerator from the supplied masterKeyId and passphrase
        PGPSignatureGenerator signatureGenerator;
        {
            // TODO: SHA256 fixed?
            JcaPGPContentSignerBuilder contentSignerBuilder = new JcaPGPContentSignerBuilder(
                    mKey.getPublicKey().getAlgorithm(), PGPUtil.SHA256)
                    .setProvider(Constants.BOUNCY_CASTLE_PROVIDER_NAME);

            signatureGenerator = new PGPSignatureGenerator(contentSignerBuilder);
            signatureGenerator.init(PGPSignature.DEFAULT_CERTIFICATION, mPrivateKey);
        }

        { // supply signatureGenerator with a SubpacketVector
            PGPSignatureSubpacketGenerator spGen = new PGPSignatureSubpacketGenerator();
            PGPSignatureSubpacketVector packetVector = spGen.generate();
            signatureGenerator.setHashedSubpackets(packetVector);
        }

        // get the master subkey (which we certify for)
        PGPPublicKey publicKey = publicKeyRing.getSubkey().getKey();

        // fetch public key ring, add the certification and return it
        for (String userId : new IterableIterator<String>(userIds.iterator())) {
            PGPSignature sig = signatureGenerator.generateCertification(userId, publicKey);
            publicKey = PGPPublicKey.addCertification(publicKey, userId, sig);
        }

        PGPPublicKeyRing ring = PGPPublicKeyRing.insertPublicKey(publicKeyRing.getRing(), publicKey);

        return new UncachedKeyRing(ring);
    }

    static class PrivateKeyNotUnlockedException extends RuntimeException {
        // this exception is a programming error which happens when an operation which requires
        // the private key is called without a previous call to unlock()
    }

}
