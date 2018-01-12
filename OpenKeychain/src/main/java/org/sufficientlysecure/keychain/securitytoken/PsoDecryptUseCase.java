package org.sufficientlysecure.keychain.securitytoken;


import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.support.annotation.NonNull;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import org.bouncycastle.asn1.nist.NISTNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.jcajce.util.MessageDigestUtils;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.operator.PGPPad;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.encoders.Hex;
import org.sufficientlysecure.keychain.pgp.CanonicalizedPublicKey;


public class PsoDecryptUseCase {
    private final SecurityTokenConnection connection;
    private final JcaKeyFingerprintCalculator fingerprintCalculator;

    public static PsoDecryptUseCase create(SecurityTokenConnection connection) {
        return new PsoDecryptUseCase(connection);
    }

    private PsoDecryptUseCase(SecurityTokenConnection connection) {
        this.connection = connection;
        this.fingerprintCalculator = new JcaKeyFingerprintCalculator();
    }

    public byte[] decryptSessionKey(@NonNull byte[] encryptedSessionKey,
            CanonicalizedPublicKey publicKey)
            throws IOException {
        final KeyFormat kf = connection.getOpenPgpCapabilities().getFormatForKeyType(KeyType.ENCRYPT);

        connection.verifyPinForOther();

        byte[] data;
        byte[] dataLen;
        int pLen = 0;

        X9ECParameters x9Params;

        switch (kf.keyFormatType()) {
            case RSAKeyFormatType:
                data = Arrays.copyOfRange(encryptedSessionKey, 2, encryptedSessionKey.length);
                if (data[0] != 0) {
                    data = Arrays.prepend(data, (byte) 0x00);
                }
                break;

            case ECKeyFormatType:
                pLen = ((((encryptedSessionKey[0] & 0xff) << 8) + (encryptedSessionKey[1] & 0xff)) + 7) / 8;
                data = new byte[pLen];

                System.arraycopy(encryptedSessionKey, 2, data, 0, pLen);

                final ECKeyFormat eckf = (ECKeyFormat) kf;
                x9Params = NISTNamedCurves.getByOID(eckf.getCurveOID());

                final ECPoint p = x9Params.getCurve().decodePoint(data);
                if (!p.isValid()) {
                    throw new CardException("Invalid EC point!");
                }

                data = p.getEncoded(false);

                if (data.length < 128) {
                    dataLen = new byte[]{(byte) data.length};
                } else {
                    dataLen = new byte[]{(byte) 0x81, (byte) data.length};
                }
                data = Arrays.concatenate(Hex.decode("86"), dataLen, data);

                if (data.length < 128) {
                    dataLen = new byte[]{(byte) data.length};
                } else {
                    dataLen = new byte[]{(byte) 0x81, (byte) data.length};
                }
                data = Arrays.concatenate(Hex.decode("7F49"), dataLen, data);

                if (data.length < 128) {
                    dataLen = new byte[]{(byte) data.length};
                } else {
                    dataLen = new byte[]{(byte) 0x81, (byte) data.length};
                }
                data = Arrays.concatenate(Hex.decode("A6"), dataLen, data);
                break;

            default:
                throw new CardException("Unknown encryption key type!");
        }

        CommandApdu command = connection.getCommandFactory().createDecipherCommand(data);
        ResponseApdu response = connection.communicate(command);

        if (!response.isSuccess()) {
            throw new CardException("Deciphering with Security token failed on receive", response.getSw());
        }

        switch (connection.getOpenPgpCapabilities().getFormatForKeyType(KeyType.ENCRYPT).keyFormatType()) {
            case RSAKeyFormatType:
                return response.getData();

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
            case ECKeyFormatType:
                data = response.getData();

                final byte[] keyEnc = new byte[encryptedSessionKey[pLen + 2]];

                System.arraycopy(encryptedSessionKey, 2 + pLen + 1, keyEnc, 0, keyEnc.length);

                try {
                    final MessageDigest kdf = MessageDigest.getInstance(MessageDigestUtils.getDigestName(publicKey.getSecurityTokenHashAlgorithm()));

                    kdf.update(new byte[]{(byte) 0, (byte) 0, (byte) 0, (byte) 1});
                    kdf.update(data);
                    kdf.update(publicKey.createUserKeyingMaterial(fingerprintCalculator));

                    final byte[] kek = kdf.digest();
                    final Cipher c = Cipher.getInstance("AESWrap");

                    c.init(Cipher.UNWRAP_MODE, new SecretKeySpec(kek, 0, publicKey.getSecurityTokenSymmetricKeySize() / 8, "AES"));

                    final Key paddedSessionKey = c.unwrap(keyEnc, "Session", Cipher.SECRET_KEY);

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

            default:
                throw new CardException("Unknown encryption key type!");
        }
    }
}
