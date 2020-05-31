package org.bouncycastle.openpgp;

import org.bouncycastle.bcpg.PublicKeyAlgorithmTags;
import org.bouncycastle.openpgp.operator.PGPContentSigner;
import org.bouncycastle.openpgp.operator.PGPContentSignerBuilder;

import java.io.IOException;
import java.io.OutputStream;

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
     * Return the signature.
     *
     * @return byte[]
     * @throws PGPException
     */
    public byte[] getSignature() throws PGPException {
        if (contentSigner.getKeyAlgorithm() == PublicKeyAlgorithmTags.RSA_SIGN
                || contentSigner.getKeyAlgorithm() == PublicKeyAlgorithmTags.RSA_GENERAL
                || contentSigner.getKeyAlgorithm() == PublicKeyAlgorithmTags.EDDSA
                || contentSigner.getKeyAlgorithm() == PublicKeyAlgorithmTags.ECDSA
                || contentSigner.getKeyAlgorithm() == PublicKeyAlgorithmTags.DSA) {
            return contentSigner.getSignature();
        } else {
           throw new UnsupportedOperationException("Unsupported algorithm");
        }
    }
}
