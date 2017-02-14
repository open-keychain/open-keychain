package org.sufficientlysecure.keychain.pgp;


import java.io.Serializable;

public abstract class SecurityProblem implements Serializable {

    public static abstract class KeySecurityProblem extends SecurityProblem {
        public final long masterKeyId;
        public final long subKeyId;
        public final int algorithm;

        private KeySecurityProblem(long masterKeyId, long subKeyId, int algorithm) {
            this.masterKeyId = masterKeyId;
            this.subKeyId = subKeyId;
            this.algorithm = algorithm;
        }
    }

    public static abstract class SymmetricAlgorithmProblem extends SecurityProblem {

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

    public static class InsecureHashAlgorithm extends SecurityProblem {
        public final int hashAlgorithm;

        InsecureHashAlgorithm(int hashAlgorithm) {
            this.hashAlgorithm = hashAlgorithm;
        }
    }

    public static class InsecureSymmetricAlgorithm extends SymmetricAlgorithmProblem {
        public final int symmetricAlgorithm;

        InsecureSymmetricAlgorithm(int symmetricAlgorithm) {
            this.symmetricAlgorithm = symmetricAlgorithm;
        }
    }

    public static class MissingMdc extends SymmetricAlgorithmProblem {

    }
}
