package org.sufficientlysecure.keychain.javacard;

import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey;
import org.sufficientlysecure.keychain.util.Passphrase;

import java.io.IOException;

public interface JavacardDevice {

    Passphrase getPin();

    void setPin(final Passphrase pin);

    Passphrase getAdminPin();

    void setAdminPin(final Passphrase adminPin);

    void changeKey(CanonicalizedSecretKey secretKey, Passphrase passphrase) throws IOException;

    boolean containsKey(KeyType keyType) throws IOException;

    boolean keyMatchesFingerPrint(KeyType keyType, byte[] fingerprint) throws IOException;

    void connectToDevice() throws IOException;

    /**
     * Modifies the user's PW1 or PW3. Before sending, the new PIN will be validated for
     * conformance to the card's requirements for key length.
     *
     * @param pinType For PW1, this is 0x81. For PW3 (Admin PIN), mode is 0x83.
     * @param newPin  The new PW1 or PW3.
     */
    void nfcModifyPIN(PinType pinType, byte[] newPin) throws IOException;

    /**
     * Calls to calculate the signature and returns the MPI value
     *
     * @param encryptedSessionKey the encoded session key
     * @return the decoded session key
     */
    byte[] decryptSessionKey(byte[] encryptedSessionKey) throws IOException;

    /**
     * Return fingerprints of all keys from application specific data stored
     * on tag, or null if data not available.
     *
     * @return The fingerprints of all subkeys in a contiguous byte array.
     */
    byte[] getFingerprints() throws IOException;


    byte[] getAid() throws IOException;

    String getUserId() throws IOException;

    boolean isConnected();

    /**
     * Calls to calculate the signature and returns the MPI value
     *
     * @param hash the hash for signing
     * @return a big integer representing the MPI for the given hash
     */
    byte[] nfcCalculateSignature(byte[] hash, int hashAlgo) throws IOException;
}
