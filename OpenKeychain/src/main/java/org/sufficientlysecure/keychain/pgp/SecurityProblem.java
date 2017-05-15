/*
 * Copyright (C) 2017 Vincent Breitmoser <v.breitmoser@mugenguild.com>
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


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.bouncycastle.util.encoders.Base64;


public abstract class SecurityProblem implements Serializable {

    public String getIdentifier() {
        if (!isIdentifiable()) {
            return null;
        }

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(out);
            oos.writeObject(this);
            oos.close();

            byte[] digest = MessageDigest.getInstance("SHA1").digest(out.toByteArray());
            return Base64.toBase64String(digest);
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public boolean isIdentifiable() {
        return false;
    }

    public static abstract class KeySecurityProblem extends SecurityProblem {
        public final long masterKeyId;
        public final long subKeyId;
        public final int algorithm;

        private KeySecurityProblem(long masterKeyId, long subKeyId, int algorithm) {
            this.masterKeyId = masterKeyId;
            this.subKeyId = subKeyId;
            this.algorithm = algorithm;
        }

        @Override
        public boolean isIdentifiable() {
            return true;
        }
    }

    public static abstract class EncryptionAlgorithmProblem extends SecurityProblem {
        @SuppressWarnings("unused") // used for identifying this specific problem
        private final byte[] sessionKey;

        private EncryptionAlgorithmProblem(byte[] sessionKey) {
            this.sessionKey = sessionKey;
        }

        @Override
        public boolean isIdentifiable() {
            return sessionKey != null;
        }
    }

    public static class InsecureBitStrength extends KeySecurityProblem {
        public final int bitStrength;

        InsecureBitStrength(long masterKeyId, long subKeyId, int algorithm, int bitStrength) {
            super(masterKeyId, subKeyId, algorithm);
            this.bitStrength = bitStrength;
        }
    }

    public static class NotWhitelistedCurve extends KeySecurityProblem {
        public final String curveOid;

        NotWhitelistedCurve(long masterKeyId, long subKeyId, String curveOid, int algorithm) {
            super(masterKeyId, subKeyId, algorithm);
            this.curveOid = curveOid;
        }
    }

    public static class UnidentifiedKeyProblem extends KeySecurityProblem {
        UnidentifiedKeyProblem(long masterKeyId, long subKeyId, int algorithm) {
            super(masterKeyId, subKeyId, algorithm);
        }
    }

    public static class InsecureSigningAlgorithm extends SecurityProblem {
        public final int hashAlgorithm;

        InsecureSigningAlgorithm(int hashAlgorithm) {
            this.hashAlgorithm = hashAlgorithm;
        }
    }

    public static class InsecureEncryptionAlgorithm extends EncryptionAlgorithmProblem {
        public final int symmetricAlgorithm;

        InsecureEncryptionAlgorithm(byte[] sessionKey, int symmetricAlgorithm) {
            super(sessionKey);
            this.symmetricAlgorithm = symmetricAlgorithm;
        }
    }

    public static class MissingMdc extends EncryptionAlgorithmProblem {
        MissingMdc(byte[] sessionKey) {
            super(sessionKey);
        }
    }
}
