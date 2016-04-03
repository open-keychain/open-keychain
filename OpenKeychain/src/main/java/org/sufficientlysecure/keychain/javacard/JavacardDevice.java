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
    void modifyPin(int pinType, byte[] newPin) throws IOException;

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
    byte[] calculateSignature(byte[] hash, int hashAlgo) throws IOException;

    boolean isFidesmoToken();

    /**
     * Return the fingerprint from application specific data stored on tag, or
     * null if it doesn't exist.
     *
     * @param idx Index of the key to return the fingerprint from.
     * @return The fingerprint of the requested key, or null if not found.
     */
    byte[] getMasterKeyFingerprint(int idx) throws IOException;

    /**
     * Resets security token, which deletes all keys and data objects.
     * This works by entering a wrong PIN and then Admin PIN 4 times respectively.
     * Afterwards, the token is reactivated.
     */
    void resetAndWipeToken() throws IOException;

    /**
     * Puts a key on the token in the given slot.
     *
     * @param slot The slot on the token where the key should be stored:
     *             0xB6: Signature Key
     *             0xB8: Decipherment Key
     *             0xA4: Authentication Key
     */
    void putKey(int slot, CanonicalizedSecretKey secretKey, Passphrase passphrase) throws IOException;

    /**
     * Stores a data object on the token. Automatically validates the proper PIN for the operation.
     * Supported for all data objects < 255 bytes in length. Only the cardholder certificate
     * (0x7F21) can exceed this length.
     *
     * @param dataObject The data object to be stored.
     * @param data       The data to store in the object
     */
    void putData(int dataObject, byte[] data) throws IOException;

    void setTransport(Transport mTransport);
}
