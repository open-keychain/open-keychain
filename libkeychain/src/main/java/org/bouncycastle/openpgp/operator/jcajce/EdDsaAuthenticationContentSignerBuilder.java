package org.bouncycastle.openpgp.operator.jcajce;

import org.bouncycastle.jcajce.provider.asymmetric.eddsa.EdDSAEngine;
import org.bouncycastle.jcajce.provider.asymmetric.eddsa.spec.EdDSANamedCurveTable;
import org.bouncycastle.jcajce.provider.asymmetric.eddsa.spec.EdDSAParameterSpec;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPRuntimeOperationException;
import org.bouncycastle.openpgp.operator.PGPContentSigner;
import org.bouncycastle.openpgp.operator.PGPContentSignerBuilder;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.security.*;

public class EdDsaAuthenticationContentSignerBuilder implements PGPContentSignerBuilder {
    private JcaPGPKeyConverter keyConverter = new JcaPGPKeyConverter();
    private int hashAlgorithm;
    private int keyAlgorithm;

    public EdDsaAuthenticationContentSignerBuilder(int keyAlgorithm, int hashAlgorithm) {
        this.keyAlgorithm = keyAlgorithm;
        this.hashAlgorithm = hashAlgorithm;
    }

    public EdDsaAuthenticationContentSignerBuilder setProvider(Provider provider) {
        keyConverter.setProvider(provider);
        return this;
    }

    public EdDsaAuthenticationContentSignerBuilder setProvider(String providerName) {
        keyConverter.setProvider(providerName);
        return this;
    }

    private Signature createSignature() throws NoSuchAlgorithmException {
        EdDSAParameterSpec spec = EdDSANamedCurveTable.getByName("Ed25519");
        return new EdDSAEngine(MessageDigest.getInstance(spec.getHashAlgorithm()));
    }

    public PGPContentSigner build(final int signatureType, final long keyID, final PrivateKey privateKey)
            throws PGPException {
        Signature signatureEdDsa;
        try {
            signatureEdDsa = createSignature();
        } catch (NoSuchAlgorithmException e) {
            throw new PGPException("unable to create Signature.", e);
        }
        final Signature signature = signatureEdDsa;

        final ByteArrayOutputStream dataOutputStream = new ByteArrayOutputStream();

        try {
            signature.initSign(privateKey);
        } catch (InvalidKeyException e) {
            throw new PGPException("invalid key.", e);
        }

        return new PGPContentSigner() {
            public int getType() {
                return signatureType;
            }

            public int getHashAlgorithm() {
                return hashAlgorithm;
            }

            public int getKeyAlgorithm() {
                return keyAlgorithm;
            }

            public long getKeyID() {
                return keyID;
            }

            public OutputStream getOutputStream() {
                return new SignatureOutputStream(signature);
            }

            public byte[] getSignature() {
                try {
                    return signature.sign();
                } catch (SignatureException e) {
                    throw new PGPRuntimeOperationException("Unable to create signature: " + e.getMessage(), e);
                }
            }

            public byte[] getDigest() {
                return null;
            }
        };
    }

    public PGPContentSigner build(final int signatureType, PGPPrivateKey privateKey) throws PGPException {
        if (privateKey instanceof JcaPGPPrivateKey) {
            return build(signatureType, privateKey.getKeyID(), ((JcaPGPPrivateKey) privateKey).getPrivateKey());
        } else {
            return build(signatureType, privateKey.getKeyID(), keyConverter.getPrivateKey(privateKey));
        }
    }
}
