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


import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.util.Arrays;

import android.support.annotation.VisibleForTesting;

import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.securitytoken.CardException;
import org.sufficientlysecure.keychain.securitytoken.CommandApdu;
import org.sufficientlysecure.keychain.securitytoken.ECKeyFormat;
import org.sufficientlysecure.keychain.securitytoken.KeyFormat;
import org.sufficientlysecure.keychain.securitytoken.KeyType;
import org.sufficientlysecure.keychain.securitytoken.OpenPgpCapabilities;
import org.sufficientlysecure.keychain.securitytoken.RSAKeyFormat;
import org.sufficientlysecure.keychain.securitytoken.ResponseApdu;
import org.sufficientlysecure.keychain.securitytoken.SecurityTokenConnection;
import org.sufficientlysecure.keychain.securitytoken.SecurityTokenUtils;
import org.sufficientlysecure.keychain.util.Passphrase;


public class SecurityTokenChangeKeyTokenOp {
    private static final byte[] BLANK_FINGERPRINT = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

    private final SecurityTokenConnection connection;

    public static SecurityTokenChangeKeyTokenOp create(SecurityTokenConnection stConnection) {
        return new SecurityTokenChangeKeyTokenOp(stConnection);
    }

    private SecurityTokenChangeKeyTokenOp(SecurityTokenConnection connection) {
        this.connection = connection;
    }

    public void changeKey(CanonicalizedSecretKey secretKey, Passphrase passphrase, Passphrase adminPin) throws IOException {
        long keyGenerationTimestamp = secretKey.getCreationTime().getTime() / 1000;
        byte[] timestampBytes = ByteBuffer.allocate(4).putInt((int) keyGenerationTimestamp).array();
        KeyType keyType = KeyType.from(secretKey);

        if (keyType == null) {
            throw new IOException("Inappropriate key flags for smart card key.");
        }

        // Slot is empty, or contains this key already. PUT KEY operation is safe
        boolean canPutKey = isSlotEmpty(keyType)
                || keyMatchesFingerPrint(keyType, secretKey.getFingerprint());

        if (!canPutKey) {
            throw new IOException(String.format("Key slot occupied; card must be reset to put new %s key.",
                    keyType.toString()));
        }

        putKey(keyType, secretKey, passphrase, adminPin);
        putData(adminPin, keyType.getFingerprintObjectId(), secretKey.getFingerprint());
        putData(adminPin, keyType.getTimestampObjectId(), timestampBytes);
    }

    /**
     * Puts a key on the token in the given slot.
     *
     * @param slot The slot on the token where the key should be stored:
     *             0xB6: Signature Key
     *             0xB8: Decipherment Key
     *             0xA4: Authentication Key
     */
    @VisibleForTesting
    void putKey(KeyType slot, CanonicalizedSecretKey secretKey, Passphrase passphrase, Passphrase adminPin)
            throws IOException {
        RSAPrivateCrtKey crtSecretKey;
        ECPrivateKey ecSecretKey;
        ECPublicKey ecPublicKey;

        connection.verifyAdminPin(adminPin);

        // Now we're ready to communicate with the token.
        byte[] keyBytes;

        try {
            secretKey.unlock(passphrase);

            OpenPgpCapabilities openPgpCapabilities = connection.getOpenPgpCapabilities();

            setKeyAttributes(adminPin, slot, SecurityTokenUtils.attributesFromSecretKey(slot, secretKey,
                    openPgpCapabilities.getFormatForKeyType(slot)));

            KeyFormat formatForKeyType = openPgpCapabilities.getFormatForKeyType(slot);
            switch (formatForKeyType.keyFormatType()) {
                case RSAKeyFormatType:
                    if (!secretKey.isRSA()) {
                        throw new IOException("Security Token not configured for RSA key.");
                    }
                    crtSecretKey = secretKey.getSecurityTokenRSASecretKey();

                    // Should happen only rarely; all GnuPG keys since 2006 use public exponent 65537.
                    if (!crtSecretKey.getPublicExponent().equals(new BigInteger("65537"))) {
                        throw new IOException("Invalid public exponent for smart Security Token.");
                    }

                    keyBytes = SecurityTokenUtils.createRSAPrivKeyTemplate(crtSecretKey, slot,
                            (RSAKeyFormat) formatForKeyType);
                    break;

                case ECKeyFormatType:
                    if (!secretKey.isEC()) {
                        throw new IOException("Security Token not configured for EC key.");
                    }

                    secretKey.unlock(passphrase);
                    ecSecretKey = secretKey.getSecurityTokenECSecretKey();
                    ecPublicKey = secretKey.getSecurityTokenECPublicKey();

                    keyBytes = SecurityTokenUtils.createECPrivKeyTemplate(ecSecretKey, ecPublicKey, slot,
                            (ECKeyFormat) formatForKeyType);
                    break;

                default:
                    throw new IOException("Key type unsupported by security token.");
            }
        } catch (PgpGeneralException e) {
            throw new IOException(e.getMessage());
        }

        CommandApdu apdu = connection.getCommandFactory().createPutKeyCommand(keyBytes);
        ResponseApdu response = connection.communicate(apdu);

        if (!response.isSuccess()) {
            throw new CardException("Key export to Security Token failed", response.getSw());
        }
    }

    private void setKeyAttributes(Passphrase adminPin, KeyType keyType, byte[] data) throws IOException {
        if (!connection.getOpenPgpCapabilities().isAttributesChangable()) {
            return;
        }

        putData(adminPin, keyType.getAlgoAttributeSlot(), data);

        connection.refreshConnectionCapabilities();
    }

    /**
     * Stores a data object on the token. Automatically validates the proper PIN for the operation.
     * Supported for all data objects < 255 bytes in length. Only the cardholder certificate
     * (0x7F21) can exceed this length.
     *
     * @param dataObject The data object to be stored.
     * @param data       The data to store in the object
     */
    private void putData(Passphrase adminPin, int dataObject, byte[] data) throws IOException {
        if (data.length > 254) {
            throw new IOException("Cannot PUT DATA with length > 254");
        }
        // TODO use admin pin regardless, if we have it?
        if (dataObject == 0x0101 || dataObject == 0x0103) {
            connection.verifyPinForOther();
        } else {
            connection.verifyAdminPin(adminPin);
        }

        CommandApdu command = connection.getCommandFactory().createPutDataCommand(dataObject, data);
        ResponseApdu response = connection.communicate(command);

        if (!response.isSuccess()) {
            throw new CardException("Failed to put data.", response.getSw());
        }
    }

    private boolean isSlotEmpty(KeyType keyType) throws IOException {
        // Note: special case: This should not happen, but happens with
        // https://github.com/FluffyKaon/OpenPGP-Card, thus for now assume true
        if (connection.getOpenPgpCapabilities().getKeyFingerprint(keyType) == null) {
            return true;
        }

        return keyMatchesFingerPrint(keyType, BLANK_FINGERPRINT);
    }

    private boolean keyMatchesFingerPrint(KeyType keyType, byte[] expectedFingerprint) throws IOException {
        byte[] actualFp = connection.getOpenPgpCapabilities().getKeyFingerprint(keyType);
        return Arrays.equals(actualFp, expectedFingerprint);
    }
}
