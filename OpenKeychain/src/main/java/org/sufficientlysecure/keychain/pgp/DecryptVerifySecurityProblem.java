/*
 * Copyright (C) 2017 Vincent Breitmoser <look@my.amazin.horse>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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

    public SecurityProblem getPrioritySecurityProblem() {
        if (encryptionKeySecurityProblem != null) {
            return encryptionKeySecurityProblem;
        } else if (signingKeySecurityProblem != null) {
            return signingKeySecurityProblem;
        } else if (symmetricSecurityProblem != null) {
            return symmetricSecurityProblem;
        } else if (signatureSecurityProblem != null) {
            return signatureSecurityProblem;
        } else {
            throw new IllegalStateException("No security problem?");
        }
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
