/*
 * Copyright (C) 2017 Schürmann & Breitmoser GbR
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
import org.sufficientlysecure.keychain.ssh.utils.SshUtils;

import java.security.NoSuchAlgorithmException;

public class SshPublicKey {
    private final static String TAG = "SshPublicKey";

    private CanonicalizedPublicKey mPublicKey;

    public SshPublicKey(CanonicalizedPublicKey publicKey) {
        mPublicKey = publicKey;
    }

    public String getEncodedKey() throws PgpGeneralException, NoSuchAlgorithmException {
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
                throw new PgpGeneralException("Unknown key algorithm");
        }
    }

    private String encodeRSAKey(PGPPublicKey publicKey) {
        RSAPublicBCPGKey publicBCPGKey = (RSAPublicBCPGKey) publicKey.getPublicKeyPacket().getKey();

        SshRSAPublicKey pubkey = new SshRSAPublicKey(publicBCPGKey.getPublicExponent(), publicBCPGKey.getModulus());

        return pubkey.getPublicKeyBlob();
    }

    private String encodeECKey(PGPPublicKey publicKey) throws NoSuchAlgorithmException {
        ECPublicBCPGKey publicBCPGKey = (ECPublicBCPGKey) publicKey.getPublicKeyPacket().getKey();

        String curveName = SshUtils.getCurveName(mPublicKey.getCurveOid());
        SshECDSAPublicKey sshECDSAPublicKey = new SshECDSAPublicKey(curveName, publicBCPGKey.getEncodedPoint());

        return sshECDSAPublicKey.getPublicKeyBlob();
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
