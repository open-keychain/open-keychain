package org.sufficientlysecure.keychain.ui;


import android.support.v4.app.Fragment;

import org.sufficientlysecure.keychain.util.Passphrase;


public abstract class EncryptModeFragment extends Fragment {

    public abstract boolean isAsymmetric();

    public abstract long getAsymmetricSigningKeyId();
    public abstract long[] getAsymmetricEncryptionKeyIds();
    public abstract String[] getAsymmetricEncryptionUserIds();

    public abstract Passphrase getSymmetricPassphrase();

}
