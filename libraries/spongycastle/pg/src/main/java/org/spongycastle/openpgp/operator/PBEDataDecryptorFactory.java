package org.spongycastle.openpgp.operator;

import org.spongycastle.bcpg.S2K;
import org.spongycastle.openpgp.PGPException;

public abstract class PBEDataDecryptorFactory
    implements PGPDataDecryptorFactory
{
    private char[] passphrase;
    private PGPDigestCalculatorProvider calculatorProvider;

    protected PBEDataDecryptorFactory(char[] passphrase, PGPDigestCalculatorProvider calculatorProvider)
    {
        this.passphrase = passphrase;
        this.calculatorProvider = calculatorProvider;
    }

    public byte[] makeKeyFromPassPhrase(int keyAlgorithm, S2K s2k)
        throws PGPException
    {
        return PGPUtil.makeKeyFromPassPhrase(calculatorProvider, keyAlgorithm, s2k, passphrase);
    }

    public abstract byte[] recoverSessionData(int keyAlgorithm, byte[] key, byte[] seckKeyData)
        throws PGPException;
}
