package org.sufficientlysecure.keychain.pgp;

import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.PGPOnePassSignature;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPSignature;
import org.spongycastle.openpgp.operator.jcajce.JcaPGPContentVerifierBuilderProvider;
import org.spongycastle.openpgp.operator.jcajce.JcePublicKeyKeyEncryptionMethodGenerator;
import org.sufficientlysecure.keychain.Constants;

public class CachedPublicKey {

    // this is the parent key ring
    private final CachedPublicKeyRing mRing;

    private final PGPPublicKey mKey;

    CachedPublicKey(CachedPublicKeyRing ring, PGPPublicKey key) {
        mRing = ring;
        mKey = key;
    }

    public CachedPublicKeyRing getKeyRing() {
        return mRing;
    }

    JcePublicKeyKeyEncryptionMethodGenerator getPubKeyEncryptionGenerator() {
        return  new JcePublicKeyKeyEncryptionMethodGenerator(mKey);
    }

    public void initSignature(PGPSignature sig) throws PGPException {
        JcaPGPContentVerifierBuilderProvider contentVerifierBuilderProvider =
                new JcaPGPContentVerifierBuilderProvider()
                        .setProvider(Constants.BOUNCY_CASTLE_PROVIDER_NAME);
        sig.init(contentVerifierBuilderProvider, mKey);
    }

    public void initSignature(PGPOnePassSignature sig) throws PGPException {
        JcaPGPContentVerifierBuilderProvider contentVerifierBuilderProvider =
                new JcaPGPContentVerifierBuilderProvider()
                        .setProvider(Constants.BOUNCY_CASTLE_PROVIDER_NAME);
        sig.init(contentVerifierBuilderProvider, mKey);
    }

    public byte[] getFingerprint() {
        return mKey.getFingerprint();
    }

    // Note that this method has package visibility - no access outside the pgp package!
    PGPPublicKey getKey() {
        return mKey;
    }
}
