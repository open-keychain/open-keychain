/*
 * Copyright (C) 2017 Christian Hagau <ach@hagau.se>
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

import org.bouncycastle.bcpg.DSAPublicBCPGKey;
import org.bouncycastle.bcpg.ECPublicBCPGKey;
import org.bouncycastle.bcpg.EdDSAPublicBCPGKey;
import org.bouncycastle.bcpg.RSAPublicBCPGKey;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.ssh.key.SshDSAPublicKey;
import org.sufficientlysecure.keychain.ssh.key.SshECDSAPublicKey;
import org.sufficientlysecure.keychain.ssh.key.SshEd25519PublicKey;
import org.sufficientlysecure.keychain.ssh.key.SshRSAPublicKey;

public class SshPublicKey {
    private final static String TAG = "SshPublicKey";

    private CanonicalizedPublicKey mPublicKey;

    public SshPublicKey(CanonicalizedPublicKey publicKey) {
        mPublicKey = publicKey;
    }

    public String getEncodedKey() throws PgpGeneralException {
        PGPPublicKey key = mPublicKey.getPublicKey();

        switch (key.getAlgorithm()) {
            case PGPPublicKey.RSA_GENERAL:
                return encodeRSAKey(key);
            case PGPPublicKey.ECDSA:
                return encodeECKey(key);
            case PGPPublicKey.EDDSA:
                return encodeEdDSAKey(key);
            case PGPPublicKey.DSA:
                return encodeDSAKey(key);
            default:
                break;
        }
        throw new PgpGeneralException("Unknown algorithm");
    }

    private String encodeRSAKey(PGPPublicKey publicKey) {
        RSAPublicBCPGKey publicBCPGKey = (RSAPublicBCPGKey) publicKey.getPublicKeyPacket().getKey();

        SshRSAPublicKey pubkey = new SshRSAPublicKey(publicBCPGKey.getPublicExponent(), publicBCPGKey.getModulus());

        return pubkey.getPublicKeyBlob();
    }

    private String encodeECKey(PGPPublicKey publicKey) {
        ECPublicBCPGKey publicBCPGKey = (ECPublicBCPGKey) publicKey.getPublicKeyPacket().getKey();

        String curveName = getCurveName(publicBCPGKey);
        SshECDSAPublicKey sshECDSAPublicKey = new SshECDSAPublicKey(curveName, publicBCPGKey.getEncodedPoint());

        return sshECDSAPublicKey.getPublicKeyBlob();
    }

    private String getCurveName(ECPublicBCPGKey publicBCPGKey) {
        String curveOid = publicBCPGKey.getCurveOID().getId();
        // see RFC5656 section 10.{1,2}
        switch (curveOid) {
            // REQUIRED curves
            case "1.2.840.10045.3.1.7":
                return "nistp256";
            case "1.3.132.0.34":
                return "nistp384";
            case "1.3.132.0.35":
                return "nistp521";

            // RECOMMENDED curves
            case "1.3.132.0.1":
                return     "1.3.132.0.1";
            case "1.2.840.10045.3.1.1":
                return  "1.2.840.10045.3.1.1";
            case "1.3.132.0.33":
                return     "1.3.132.0.33";
            case "1.3.132.0.26":
                return     "1.3.132.0.26";
            case "1.3.132.0.27":
                return     "1.3.132.0.27";
            case "1.3.132.0.16":
                return     "1.3.132.0.16";
            case "1.3.132.0.36":
                return     "1.3.132.0.36";
            case "1.3.132.0.37":
                return     "1.3.132.0.37";
            case "1.3.132.0.38":
                return     "1.3.132.0.38";

            default:
                return null;
        }
    }

    private String encodeEdDSAKey(PGPPublicKey publicKey) {
        EdDSAPublicBCPGKey publicBCPGKey = (EdDSAPublicBCPGKey) publicKey.getPublicKeyPacket().getKey();

        SshEd25519PublicKey pubkey = new SshEd25519PublicKey(publicBCPGKey.getEdDSAEncodedPoint());

        return pubkey.getPublicKeyBlob();
    }

    private String encodeDSAKey(PGPPublicKey publicKey) {
        DSAPublicBCPGKey publicBCPGKey = (DSAPublicBCPGKey) publicKey.getPublicKeyPacket().getKey();

        SshDSAPublicKey sshDSAPublicKey = new SshDSAPublicKey(publicBCPGKey.getP(),
                publicBCPGKey.getQ(),
                publicBCPGKey.getG(),
                publicBCPGKey.getY());

        return sshDSAPublicKey.getPublicKeyBlob();
    }
}
