package org.bouncycastle.openpgp.operator.jcajce;


import java.security.GeneralSecurityException;
import java.security.MessageDigest;

import org.bouncycastle.jcajce.util.NamedJcaJceHelper;
import org.bouncycastle.openpgp.PGPException;


public class JcaPGPContentDigest {
    private MessageDigest digest;

    private JcaPGPContentDigest(MessageDigest digest) {
        this.digest = digest;
    }

    public static JcaPGPContentDigest newInstance(String providerName, int hashAlgorithm) {
        OperatorHelper helper = new OperatorHelper(new NamedJcaJceHelper(providerName));

        try {
            MessageDigest digest = helper.createDigest(hashAlgorithm);
            return new JcaPGPContentDigest(digest);
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("Unknown algorithm!");
        } catch (PGPException e) {
            throw new IllegalArgumentException("Unknown algorithm!");
        }
    }

    public void update(byte b) {
        digest.update(b);
    }

    public void update(byte[] input) {
        digest.update(input, 0, input.length);
    }

    public void update(byte[] input, int offset, int len) {
        digest.update(input, offset, len);
    }

    public byte[] digest() {
        return digest.digest();
    }
}
