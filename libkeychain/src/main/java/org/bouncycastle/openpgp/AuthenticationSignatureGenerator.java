package org.bouncycastle.openpgp;

import org.bouncycastle.bcpg.MPInteger;
import org.bouncycastle.bcpg.PublicKeyAlgorithmTags;
import org.bouncycastle.bcpg.SignaturePacket;
import org.bouncycastle.openpgp.operator.PGPContentSigner;
import org.bouncycastle.openpgp.operator.PGPContentSignerBuilder;
import org.bouncycastle.util.BigIntegers;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;

/**
 * Generator for authentication signatures.
 */
public class AuthenticationSignatureGenerator {
    private OutputStream sigOut;
    private PGPContentSignerBuilder contentSignerBuilder;
    private PGPContentSigner contentSigner;
    private int sigType;

    /**
     * Create a signature generator built on the passed in contentSignerBuilder.
     *
     * @param contentSignerBuilder builder to produce PGPContentSigner objects for generating signatures.
     */
    public AuthenticationSignatureGenerator(PGPContentSignerBuilder contentSignerBuilder) {
        this.contentSignerBuilder = contentSignerBuilder;
    }

    /**
     * Initialise the generator for signing.
     *
     * @param signatureType
     * @param key
     * @throws PGPException
     */
    public void init(int signatureType, PGPPrivateKey key) throws PGPException {
        contentSigner = contentSignerBuilder.build(signatureType, key);
        sigOut = contentSigner.getOutputStream();
        sigType = contentSigner.getType();
    }

    public void update(byte b) {
        byteUpdate(b);
    }

    public void update(byte[] b) {
        update(b, 0, b.length);
    }

    public void update(byte[] b, int off, int len) {
        blockUpdate(b, off, len);
    }

    private void byteUpdate(byte b) {
        try {
            sigOut.write(b);
        } catch (IOException e) {
            throw new PGPRuntimeOperationException(e.getMessage(), e);
        }
    }

    private void blockUpdate(byte[] block, int off, int len) {
        try {
            sigOut.write(block, off, len);
        } catch (IOException e) {
            throw new PGPRuntimeOperationException(e.getMessage(), e);
        }
    }

    /**
     * Return a signature object containing the current signature state.
     *
     * @return PGPSignature
     * @throws PGPException
     */
    public PGPSignature generate() throws PGPException {
        MPInteger[] sigValues;

        if (contentSigner.getKeyAlgorithm() == PublicKeyAlgorithmTags.RSA_SIGN
                || contentSigner.getKeyAlgorithm() == PublicKeyAlgorithmTags.RSA_GENERAL) {
            sigValues = new MPInteger[1];
            sigValues[0] = new MPInteger(new BigInteger(1, contentSigner.getSignature()));
        } else if (contentSigner.getKeyAlgorithm() == PublicKeyAlgorithmTags.EDDSA) {
            byte[] sig = contentSigner.getSignature();

            sigValues = new MPInteger[2];

            sigValues[0] = new MPInteger(BigIntegers.fromUnsignedByteArray(sig, 0, 32));
            sigValues[1] = new MPInteger(BigIntegers.fromUnsignedByteArray(sig, 32, 32));
        } else {
            sigValues = PGPUtil.dsaSigToMpi(contentSigner.getSignature());
        }

        return new PGPSignature(new SignaturePacket(sigType, contentSigner.getKeyID(), contentSigner.getKeyAlgorithm(),
                contentSigner.getHashAlgorithm(), null, null, null, sigValues));
    }
}
