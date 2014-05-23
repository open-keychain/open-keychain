package org.sufficientlysecure.keychain.pgp;

import org.spongycastle.bcpg.SignatureSubpacket;
import org.spongycastle.bcpg.SignatureSubpacketTags;
import org.spongycastle.bcpg.sig.RevocationReason;
import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.PGPObjectFactory;
import org.spongycastle.openpgp.PGPSignature;
import org.spongycastle.openpgp.PGPSignatureList;
import org.spongycastle.openpgp.operator.jcajce.JcaPGPContentVerifierBuilderProvider;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.util.Log;

import java.io.IOException;
import java.security.SignatureException;

public class WrappedSignature {

    public static final int DEFAULT_CERTIFICATION = PGPSignature.DEFAULT_CERTIFICATION;
    public static final int NO_CERTIFICATION = PGPSignature.NO_CERTIFICATION;
    public static final int CASUAL_CERTIFICATION = PGPSignature.CASUAL_CERTIFICATION;
    public static final int POSITIVE_CERTIFICATION = PGPSignature.POSITIVE_CERTIFICATION;
    public static final int CERTIFICATION_REVOCATION = PGPSignature.CERTIFICATION_REVOCATION;

    final PGPSignature mSig;

    protected WrappedSignature(PGPSignature sig) {
        mSig = sig;
    }

    public long getKeyId() {
        return mSig.getKeyID();
    }

    public int getKeyAlgorithm() {
        return mSig.getKeyAlgorithm();
    }

    public void init(WrappedPublicKey key) throws PgpGeneralException {
        try {
            JcaPGPContentVerifierBuilderProvider contentVerifierBuilderProvider =
                    new JcaPGPContentVerifierBuilderProvider()
                            .setProvider(Constants.BOUNCY_CASTLE_PROVIDER_NAME);
            mSig.init(contentVerifierBuilderProvider, key.getPublicKey());
        } catch(PGPException e) {
            throw new PgpGeneralException(e);
        }
    }

    public void update(byte[] data, int offset, int length) throws PgpGeneralException {
        try {
            mSig.update(data, offset, length);
        } catch(SignatureException e) {
            throw new PgpGeneralException(e);
        }
    }

    public void update(byte data) throws PgpGeneralException {
        try {
            mSig.update(data);
        } catch(SignatureException e) {
            throw new PgpGeneralException(e);
        }
    }

    public boolean verify() throws PgpGeneralException {
        try {
            return mSig.verify();
        } catch(SignatureException e) {
            throw new PgpGeneralException(e);
        } catch(PGPException e) {
            throw new PgpGeneralException(e);
        }
    }

    public boolean isRevocation() {
        return mSig.getHashedSubPackets().hasSubpacket(SignatureSubpacketTags.REVOCATION_REASON);
    }

    public String getRevocationReason() throws PgpGeneralException {
        if(!isRevocation()) {
            throw new PgpGeneralException("Not a revocation signature.");
        }
        SignatureSubpacket p = mSig.getHashedSubPackets().getSubpacket(
                SignatureSubpacketTags.REVOCATION_REASON);
        // For some reason, this is missing in SignatureSubpacketInputStream:146
        if (!(p instanceof RevocationReason)) {
            p = new RevocationReason(false, p.getData());
        }
        return ((RevocationReason) p).getRevocationDescription();
    }

    /** Verify a signature for this pubkey, after it has been initialized by the signer using
     * initSignature(). This method should probably move into a wrapped PGPSignature class
     * at some point.
     */
    public boolean verifySignature(WrappedPublicKey key, String uid) throws PgpGeneralException {
        try {
            return mSig.verifyCertification(uid, key.getPublicKey());
        } catch (SignatureException e) {
            throw new PgpGeneralException("Error!", e);
        } catch (PGPException e) {
            throw new PgpGeneralException("Error!", e);
        }
    }

    public static WrappedSignature fromBytes(byte[] data) {
        PGPObjectFactory factory = new PGPObjectFactory(data);
        PGPSignatureList signatures = null;
        try {
            if ((signatures = (PGPSignatureList) factory.nextObject()) == null || signatures.isEmpty()) {
                Log.e(Constants.TAG, "No signatures given!");
                return null;
            }
        } catch (IOException e) {
            Log.e(Constants.TAG, "Error while converting to PGPSignature!", e);
            return null;
        }

        return new WrappedSignature(signatures.get(0));
    }

}
