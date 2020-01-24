/*
 * Copyright (C) 2018 Schürmann & Breitmoser GbR
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

package org.sufficientlysecure.keychain.securitytoken.operations;


import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import org.bouncycastle.asn1.cryptlib.CryptlibObjectIdentifiers;
import org.bouncycastle.asn1.x9.ECNamedCurveTable;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.jcajce.util.MessageDigestUtils;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.operator.PGPPad;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.encoders.Hex;
import org.sufficientlysecure.keychain.pgp.CanonicalizedPublicKey;
import org.sufficientlysecure.keychain.securitytoken.CardException;
import org.sufficientlysecure.keychain.securitytoken.CommandApdu;
import org.sufficientlysecure.keychain.securitytoken.ECKeyFormat;
import org.sufficientlysecure.keychain.securitytoken.KeyFormat;
import org.sufficientlysecure.keychain.securitytoken.ResponseApdu;
import org.sufficientlysecure.keychain.securitytoken.SecurityTokenConnection;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;


/** This class implements the PSO:DECIPHER operation, as specified in OpenPGP card spec / 7.2.11 (p52 in v3.0.1).
 *
 * See https://www.g10code.com/docs/openpgp-card-3.0.pdf
 */
public class PsoDecryptTokenOp {
    private final SecurityTokenConnection connection;
    private final JcaKeyFingerprintCalculator fingerprintCalculator;

    public static PsoDecryptTokenOp create(SecurityTokenConnection connection) {
        return new PsoDecryptTokenOp(connection, new JcaKeyFingerprintCalculator());
    }

    private PsoDecryptTokenOp(SecurityTokenConnection connection,
            JcaKeyFingerprintCalculator jcaKeyFingerprintCalculator) {
        this.connection = connection;
        this.fingerprintCalculator = jcaKeyFingerprintCalculator;
    }

    public byte[] verifyAndDecryptSessionKey(@NonNull byte[] encryptedSessionKeyMpi, CanonicalizedPublicKey publicKey)
            throws IOException {
        connection.verifyPinForOther();

        KeyFormat kf = connection.getOpenPgpCapabilities().getEncryptKeyFormat();
        switch (kf.keyFormatType()) {
            case RSAKeyFormatType:
                return decryptSessionKeyRsa(encryptedSessionKeyMpi);

            case ECKeyFormatType:
                return decryptSessionKeyEcdh(encryptedSessionKeyMpi, (ECKeyFormat) kf, publicKey);

            default:
                throw new CardException("Unknown encryption key type!");
        }
    }

    private byte[] decryptSessionKeyRsa(byte[] encryptedSessionKeyMpi) throws IOException {
        int mpiLength = getMpiLength(encryptedSessionKeyMpi);
        byte[] psoDecipherPayload = getRsaOperationPayload(encryptedSessionKeyMpi);

        CommandApdu command = connection.getCommandFactory().createDecipherCommand(psoDecipherPayload, mpiLength);
        ResponseApdu response = connection.communicate(command);

        if (!response.isSuccess()) {
            throw new CardException("Deciphering with Security token failed on receive", response.getSw());
        }

        return response.getData();
    }

    @VisibleForTesting
    public byte[] getRsaOperationPayload(byte[] encryptedSessionKeyMpi) throws IOException {
        int mpiLength = getMpiLength(encryptedSessionKeyMpi);
        if (mpiLength != encryptedSessionKeyMpi.length - 2) {
            throw new IOException("Malformed RSA session key!");
        }

        byte[] psoDecipherPayload = new byte[mpiLength + 1];
        psoDecipherPayload[0] = 0x00; // RSA Padding Indicator Byte
        System.arraycopy(encryptedSessionKeyMpi, 2, psoDecipherPayload, 1, mpiLength);
        return psoDecipherPayload;
    }

    private byte[] decryptSessionKeyEcdh(byte[] encryptedSessionKeyMpi, ECKeyFormat eckf, CanonicalizedPublicKey publicKey)
            throws IOException {
        int mpiLength = getMpiLength(encryptedSessionKeyMpi);
        byte[] encryptedPoint = Arrays.copyOfRange(encryptedSessionKeyMpi, 2, mpiLength + 2);

        byte[] psoDecipherPayload = getEcDecipherPayload(eckf, encryptedPoint);

        byte[] dataLen;
        if (psoDecipherPayload.length < 128) {
            dataLen = new byte[]{(byte) psoDecipherPayload.length};
        } else {
            dataLen = new byte[]{(byte) 0x81, (byte) psoDecipherPayload.length};
        }
        psoDecipherPayload = Arrays.concatenate(Hex.decode("86"), dataLen, psoDecipherPayload);

        if (psoDecipherPayload.length < 128) {
            dataLen = new byte[]{(byte) psoDecipherPayload.length};
        } else {
            dataLen = new byte[]{(byte) 0x81, (byte) psoDecipherPayload.length};
        }
        psoDecipherPayload = Arrays.concatenate(Hex.decode("7F49"), dataLen, psoDecipherPayload);

        if (psoDecipherPayload.length < 128) {
            dataLen = new byte[]{(byte) psoDecipherPayload.length};
        } else {
            dataLen = new byte[]{(byte) 0x81, (byte) psoDecipherPayload.length};
        }
        psoDecipherPayload = Arrays.concatenate(Hex.decode("A6"), dataLen, psoDecipherPayload);

        CommandApdu command = connection.getCommandFactory().createDecipherCommand(
                psoDecipherPayload, encryptedPoint.length);
        ResponseApdu response = connection.communicate(command);

        if (!response.isSuccess()) {
            throw new CardException("Deciphering with Security token failed on receive", response.getSw());
        }

        /* From 3.x OpenPGP card specification :
           In case of ECDH the card supports a partial decrypt only.
           With its own private key and the given public key the card calculates a shared secret
           in compliance with the Elliptic Curve Key Agreement Scheme from Diffie-Hellman.
           The shared secret is returned in the response, all other calculation for deciphering
           are done outside of the card.

           The shared secret obtained is a KEK (Key Encryption Key) that is used to wrap the
           session key.

           From rfc6637#section-13 :
           This document explicitly discourages the use of algorithms other than AES as a KEK algorithm.
       */
        byte[] keyEncryptionKey = response.getData();

        final byte[] keyEnc = new byte[encryptedSessionKeyMpi[mpiLength + 2]];

        System.arraycopy(encryptedSessionKeyMpi, 2 + mpiLength + 1, keyEnc, 0, keyEnc.length);

        try {
            final MessageDigest kdf = MessageDigest.getInstance(MessageDigestUtils.getDigestName(publicKey.getSecurityTokenHashAlgorithm()));

            kdf.update(new byte[]{(byte) 0, (byte) 0, (byte) 0, (byte) 1});
            kdf.update(keyEncryptionKey);
            kdf.update(publicKey.createUserKeyingMaterial(fingerprintCalculator));

            byte[] kek = kdf.digest();
            Cipher c = Cipher.getInstance("AESWrap");

            c.init(Cipher.UNWRAP_MODE, new SecretKeySpec(kek, 0, publicKey.getSecurityTokenSymmetricKeySize() / 8, "AES"));

            Key paddedSessionKey = c.unwrap(keyEnc, "Session", Cipher.SECRET_KEY);

            Arrays.fill(kek, (byte) 0);

            return PGPPad.unpadSessionData(paddedSessionKey.getEncoded());
        } catch (NoSuchAlgorithmException e) {
            throw new CardException("Unknown digest/encryption algorithm!");
        } catch (NoSuchPaddingException e) {
            throw new CardException("Unknown padding algorithm!");
        } catch (PGPException e) {
            throw new CardException(e.getMessage());
        } catch (InvalidKeyException e) {
            throw new CardException("Invalid KEK!");
        }
    }

    private byte[] getEcDecipherPayload(ECKeyFormat eckf, byte[] encryptedPoint) throws CardException {
        // TODO is this the right curve?
        if (CryptlibObjectIdentifiers.curvey25519.equals(eckf.getCurveOID())) {
            return Arrays.copyOfRange(encryptedPoint, 1, 33);
        } else {
            X9ECParameters x9Params = ECNamedCurveTable.getByOID(eckf.getCurveOID());
            ECPoint p = x9Params.getCurve().decodePoint(encryptedPoint);
            if (!p.isValid()) {
                throw new CardException("Invalid EC point!");
            }

            return p.getEncoded(false);
        }
    }

    private int getMpiLength(byte[] multiPrecisionInteger) {
        return ((((multiPrecisionInteger[0] & 0xff) << 8) + (multiPrecisionInteger[1] & 0xff)) + 7) / 8;
    }
}
