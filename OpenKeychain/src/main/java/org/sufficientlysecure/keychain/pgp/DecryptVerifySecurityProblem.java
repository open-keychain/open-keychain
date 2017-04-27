package org.sufficientlysecure.keychain.pgp;


import java.io.Serializable;

import org.sufficientlysecure.keychain.pgp.SecurityProblem.InsecureSigningAlgorithm;
import org.sufficientlysecure.keychain.pgp.SecurityProblem.KeySecurityProblem;
import org.sufficientlysecure.keychain.pgp.SecurityProblem.EncryptionAlgorithmProblem;


public class DecryptVerifySecurityProblem implements Serializable {
    public final KeySecurityProblem encryptionKeySecurityProblem;
    public final KeySecurityProblem signingKeySecurityProblem;
    public final EncryptionAlgorithmProblem symmetricSecurityProblem;
    public final InsecureSigningAlgorithm signatureSecurityProblem;

    private DecryptVerifySecurityProblem(DecryptVerifySecurityProblemBuilder builder) {
        encryptionKeySecurityProblem = builder.encryptionKeySecurityProblem;
        signingKeySecurityProblem = builder.signingKeySecurityProblem;
        symmetricSecurityProblem = builder.symmetricSecurityProblem;
        signatureSecurityProblem = builder.signatureSecurityProblem;
    }

    static class DecryptVerifySecurityProblemBuilder {
        private KeySecurityProblem encryptionKeySecurityProblem;
        private KeySecurityProblem signingKeySecurityProblem;
        private EncryptionAlgorithmProblem symmetricSecurityProblem;
        private InsecureSigningAlgorithm signatureSecurityProblem;

        void addEncryptionKeySecurityProblem(KeySecurityProblem encryptionKeySecurityProblem) {
            this.encryptionKeySecurityProblem = encryptionKeySecurityProblem;
        }

        void addSigningKeyProblem(KeySecurityProblem keySecurityProblem) {
            this.signingKeySecurityProblem = keySecurityProblem;
        }

        void addSymmetricSecurityProblem(EncryptionAlgorithmProblem symmetricSecurityProblem) {
            this.symmetricSecurityProblem = symmetricSecurityProblem;
        }

        void addSignatureSecurityProblem(InsecureSigningAlgorithm signatureSecurityProblem) {
            this.signatureSecurityProblem = signatureSecurityProblem;
        }

        public DecryptVerifySecurityProblem build() {
            if (encryptionKeySecurityProblem == null && signingKeySecurityProblem == null &&
                    symmetricSecurityProblem == null && signatureSecurityProblem == null) {
                return null;
            }
            return new DecryptVerifySecurityProblem(this);
        }
    }
}
