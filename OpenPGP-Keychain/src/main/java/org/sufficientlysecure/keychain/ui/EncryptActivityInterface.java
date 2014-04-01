package org.sufficientlysecure.keychain.ui;


public interface EncryptActivityInterface {

    public boolean isModeSymmetric();

    public long getSignatureKey();
    public long[] getEncryptionKeys();

    public String getPassphrase();
    public String getPassphraseAgain();

}
