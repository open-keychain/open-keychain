package org.sufficientlysecure.keychain.securitytoken.operations;


import java.io.IOException;

import org.sufficientlysecure.keychain.securitytoken.CommandApdu;
import org.sufficientlysecure.keychain.securitytoken.ResponseApdu;
import org.sufficientlysecure.keychain.securitytoken.SecurityTokenConnection;
import org.sufficientlysecure.keychain.util.Passphrase;


public class GenerateKeyTokenOp {
    private final SecurityTokenConnection connection;

    public static GenerateKeyTokenOp create(SecurityTokenConnection connection) {
        return new GenerateKeyTokenOp(connection);
    }

    private GenerateKeyTokenOp(SecurityTokenConnection connection) {
        this.connection = connection;
    }

    /**
     * Generates a key on the card in the given slot. If the slot is 0xB6 (the signature key),
     * this command also has the effect of resetting the digital signature counter.
     * NOTE: This does not set the key fingerprint data object! After calling this command, you
     * must construct a public key packet using the returned public key data objects, compute the
     * key fingerprint, and store it on the card using: putData(0xC8, key.getFingerprint())
     *
     * @param slot The slot on the card where the key should be generated:
     *             0xB6: Signature Key
     *             0xB8: Decipherment Key
     *             0xA4: Authentication Key
     * @return the public key data objects, in TLV format. For RSA this will be the public modulus
     * (0x81) and exponent (0x82). These may come out of order; proper TLV parsing is required.
     */
    public byte[] generateKey(Passphrase adminPin, int slot) throws IOException {
        if (slot != 0xB6 && slot != 0xB8 && slot != 0xA4) {
            throw new IOException("Invalid key slot");
        }

        connection.verifyAdminPin(adminPin);

        CommandApdu apdu = connection.getCommandFactory().createGenerateKeyCommand(slot);
        ResponseApdu response = connection.communicate(apdu);

        if (!response.isSuccess()) {
            throw new IOException("On-card key generation failed");
        }

        return response.getData();
    }
}
