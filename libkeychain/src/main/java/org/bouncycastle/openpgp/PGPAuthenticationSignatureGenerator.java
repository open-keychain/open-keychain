package org.bouncycastle.openpgp;

import org.bouncycastle.bcpg.MPInteger;
import org.bouncycastle.bcpg.PublicKeyAlgorithmTags;
import org.bouncycastle.bcpg.SignaturePacket;
import org.bouncycastle.bcpg.SignatureSubpacket;
import org.bouncycastle.openpgp.operator.PGPContentSigner;
import org.bouncycastle.openpgp.operator.PGPContentSignerBuilder;
import org.bouncycastle.util.BigIntegers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;

/**
 * Generator for PGP Signatures.
 */
public class PGPAuthenticationSignatureGenerator
{
    private OutputStream sigOut;
    private PGPContentSignerBuilder contentSignerBuilder;
    private PGPContentSigner contentSigner;
    private int             sigType;
    private byte            lastb;
    private int providedKeyAlgorithm = -1;

    /**
     * Create a signature generator built on the passed in contentSignerBuilder.
     *
     * @param contentSignerBuilder  builder to produce PGPContentSigner objects for generating signatures.
     */
    public PGPAuthenticationSignatureGenerator(
        PGPContentSignerBuilder contentSignerBuilder)
    {
        this.contentSignerBuilder = contentSignerBuilder;
    }

    /**
     * Initialise the generator for signing.
     *
     * @param signatureType
     * @param key
     * @throws PGPException
     */
    public void init(
        int             signatureType,
        PGPPrivateKey   key)
        throws PGPException
    {
        contentSigner = contentSignerBuilder.build(signatureType, key);
        sigOut = contentSigner.getOutputStream();
        sigType = contentSigner.getType();
        lastb = 0;

        if (providedKeyAlgorithm >= 0 && providedKeyAlgorithm != contentSigner.getKeyAlgorithm())
        {
            throw new PGPException("key algorithm mismatch");
        }
    }
    
    public void update(
        byte    b)
    {
        if (sigType == PGPSignature.CANONICAL_TEXT_DOCUMENT)
        {
            if (b == '\r')
            {
                byteUpdate((byte)'\r');
                byteUpdate((byte)'\n');
            }
            else if (b == '\n')
            {
                if (lastb != '\r')
                {
                    byteUpdate((byte)'\r');
                    byteUpdate((byte)'\n');
                }
            }
            else
            {
                byteUpdate(b);
            }
            
            lastb = b;
        }
        else
        {
            byteUpdate(b);
        }
    }
    
    public void update(
        byte[]    b)
    {
        this.update(b, 0, b.length);
    }
    
    public void update(
        byte[]  b,
        int     off,
        int     len)
    {
        if (sigType == PGPSignature.CANONICAL_TEXT_DOCUMENT)
        {
            int finish = off + len;
            
            for (int i = off; i != finish; i++)
            {
                this.update(b[i]);
            }
        }
        else
        {
            blockUpdate(b, off, len);
        }
    }

    private void byteUpdate(byte b)
    {
        try
        {
            sigOut.write(b);
        }
        catch (IOException e)
        {
            throw new PGPRuntimeOperationException(e.getMessage(), e);
        }
    }

    private void blockUpdate(byte[] block, int off, int len)
    {
        try
        {
            sigOut.write(block, off, len);
        }
        catch (IOException e)
        {
            throw new PGPRuntimeOperationException(e.getMessage(), e);
        }
    }

    /**
     * Return a signature object containing the current signature state.
     * 
     * @return PGPSignature
     * @throws PGPException
     */
    public PGPSignature generate()
        throws PGPException
    {
        MPInteger[]             sigValues;
        ByteArrayOutputStream   sOut = new ByteArrayOutputStream();
        SignatureSubpacket[]    hPkts, unhPkts;
        hPkts = new SignatureSubpacket[0];
        unhPkts = new SignatureSubpacket[0];

        try
        {
            ByteArrayOutputStream    hOut = new ByteArrayOutputStream();
            byte[]                            data = hOut.toByteArray();
            sOut.write(data);
        }
        catch (IOException e)
        {
            throw new PGPException("exception encoding hashed data.", e);
        }
        
        byte[]    trailer = sOut.toByteArray();

        blockUpdate(trailer, 0, trailer.length);

        if (contentSigner.getKeyAlgorithm() == PublicKeyAlgorithmTags.RSA_SIGN
            || contentSigner.getKeyAlgorithm() == PublicKeyAlgorithmTags.RSA_GENERAL)    // an RSA signature
        {
            sigValues = new MPInteger[1];
            sigValues[0] = new MPInteger(new BigInteger(1, contentSigner.getSignature()));
        }
        else if (contentSigner.getKeyAlgorithm() == PublicKeyAlgorithmTags.EDDSA)
        {
            byte[] sig = contentSigner.getSignature();

            sigValues = new MPInteger[2];

            sigValues[0] = new MPInteger(BigIntegers.fromUnsignedByteArray(sig, 0, 32));
            sigValues[1] = new MPInteger(BigIntegers.fromUnsignedByteArray(sig, 32, 32));
        }
        else
        {
            sigValues = PGPUtil.dsaSigToMpi(contentSigner.getSignature());
        }

        byte[]                        digest = contentSigner.getDigest();
        byte[]                        fingerPrint = new byte[2];

        fingerPrint[0] = digest[0];
        fingerPrint[1] = digest[1];
        
        return new PGPSignature(new SignaturePacket(sigType, contentSigner.getKeyID(), contentSigner.getKeyAlgorithm(), contentSigner.getHashAlgorithm(), hPkts, unhPkts, fingerPrint, sigValues));
    }
}
