package org.spongycastle.openpgp.operator;

import org.spongycastle.bcpg.S2K;
import org.spongycastle.openpgp.PGPException;

public abstract class PBESecretKeyDecryptor
{
    private char[] passphrase;
    private PGPDigestCalculatorProvider calculatorProvider;

    protected PBESecretKeyDecryptor(char[] passphrase, PGPDigestCalculatorProvider calculatorProvider)
    {
        this.passphrase = passphrase;
        this.calculatorProvider = calculatorProvider;
    }

    public PGPDigestCalculator getChecksumCalculator(int hashAlgorithm)
        throws PGPException
    {
        return calculatorProvider.get(hashAlgorithm);
    }

    public byte[] makeKeyFromPassPhrase(int keyAlgorithm, S2K s2k)
        throws PGPException
    {
        return PGPUtil.makeKeyFromPassPhrase(calculatorProvider, keyAlgorithm, s2k, passphrase);
    }

    public abstract byte[] recoverKeyData(int encAlgorithm, byte[] key, byte[] iv, byte[] keyData, int keyOff, int keyLen)
        throws PGPException;
}
