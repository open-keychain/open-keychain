package org.bouncycastle.openpgp.operator.jcajce;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.InvalidKeyException;
import java.security.Provider;
import java.security.Signature;
import java.security.SignatureException;

import org.bouncycastle.asn1.ASN1Encoding;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.DigestInfo;
import org.bouncycastle.bcpg.PublicKeyAlgorithmTags;
import org.bouncycastle.jcajce.provider.util.DigestFactory;
import org.bouncycastle.jcajce.util.DefaultJcaJceHelper;
import org.bouncycastle.jcajce.util.NamedJcaJceHelper;
import org.bouncycastle.jcajce.util.ProviderJcaJceHelper;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPRuntimeOperationException;
import org.bouncycastle.openpgp.operator.PGPContentVerifier;
import org.bouncycastle.openpgp.operator.PGPContentVerifierBuilder;
import org.bouncycastle.openpgp.operator.PGPContentVerifierBuilderProvider;

public class JcaPGPRawDigestContentVerifierBuilderProvider
        implements PGPContentVerifierBuilderProvider
{
    private OperatorHelper helper = new OperatorHelper(new DefaultJcaJceHelper());
    private JcaPGPKeyConverter keyConverter = new JcaPGPKeyConverter();

    public JcaPGPRawDigestContentVerifierBuilderProvider()
    {
    }

    public JcaPGPRawDigestContentVerifierBuilderProvider setProvider(Provider provider)
    {
        this.helper = new OperatorHelper(new ProviderJcaJceHelper(provider));
        keyConverter.setProvider(provider);

        return this;
    }

    public JcaPGPRawDigestContentVerifierBuilderProvider setProvider(String providerName)
    {
        this.helper = new OperatorHelper(new NamedJcaJceHelper(providerName));
        keyConverter.setProvider(providerName);

        return this;
    }

    public PGPContentVerifierBuilder get(int keyAlgorithm, int hashAlgorithm)
            throws PGPException
    {
        return new JcaPGPRawDigestContentVerifierBuilder(keyAlgorithm, hashAlgorithm);
    }

    private class JcaPGPRawDigestContentVerifierBuilder
            implements PGPContentVerifierBuilder
    {
        private int hashAlgorithm;
        private int keyAlgorithm;
        private ByteArrayOutputStream outputStream;

        public JcaPGPRawDigestContentVerifierBuilder(int keyAlgorithm, int hashAlgorithm)
        {
            this.keyAlgorithm = keyAlgorithm;
            this.hashAlgorithm = hashAlgorithm;
        }

        public PGPContentVerifier build(final PGPPublicKey publicKey)
                throws PGPException
        {
            final Signature signature = helper.createSignature(keyAlgorithm);

            outputStream = new ByteArrayOutputStream();

            try
            {
                signature.initVerify(keyConverter.getPublicKey(publicKey));
            }
            catch (InvalidKeyException e)
            {
                throw new PGPException("invalid key.", e);
            }

            return new PGPContentVerifier()
            {
                public int getHashAlgorithm()
                {
                    return hashAlgorithm;
                }

                public int getKeyAlgorithm()
                {
                    return keyAlgorithm;
                }

                public long getKeyID()
                {
                    return publicKey.getKeyID();
                }

                public boolean verify(byte[] expected)
                {
                    try {
                        byte[] rawBytes = outputStream.toByteArray();
                        byte[] encodedBytes = applyDigestEncoding(keyAlgorithm, hashAlgorithm, rawBytes);

                        signature.update(encodedBytes);

                        return signature.verify(expected);
                    }
                    catch (SignatureException e)
                    {
                        throw new PGPRuntimeOperationException("unable to verify signature: " + e.getMessage(), e);
                    }
                    catch (IOException e)
                    {
                        throw new IllegalStateException(e);
                    }
                }

                public OutputStream getOutputStream()
                {
                    return outputStream;
                }
            };
        }
    }

    private static byte[] applyDigestEncoding(int keyAlgorithm, int hashAlgorithm, byte[] bytes) throws IOException {
        switch (keyAlgorithm) {
            case PublicKeyAlgorithmTags.RSA_GENERAL:
            case PublicKeyAlgorithmTags.RSA_SIGN:
                try {
                    String digestName = PGPUtil.getDigestName(hashAlgorithm);
                    ASN1ObjectIdentifier digestIdentifier = DigestFactory.getOID(digestName);
                    DigestInfo dInfo = new DigestInfo(
                            new AlgorithmIdentifier(digestIdentifier, DERNull.INSTANCE), bytes);

                    return dInfo.getEncoded(ASN1Encoding.DER);
                } catch (PGPException e) {
                    throw new IOException("Unknown hashing algorithm!", e);
                }

            default:
                return bytes;
        }
    }
}
