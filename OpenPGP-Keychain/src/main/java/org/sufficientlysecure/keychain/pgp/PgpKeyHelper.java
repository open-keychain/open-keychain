/*
 * Copyright (C) 2012-2013 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2010 Thialfihar <thi@thialfihar.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.sufficientlysecure.keychain.pgp;

import android.content.Context;
import org.spongycastle.bcpg.sig.KeyFlags;
import org.spongycastle.openpgp.*;
import org.spongycastle.util.encoders.Hex;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.util.IterableIterator;
import org.sufficientlysecure.keychain.util.Log;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PgpKeyHelper {

    private static final Pattern USER_ID_PATTERN = Pattern.compile("^(.*?)(?: \\((.*)\\))?(?: <(.*)>)?$");

    public static Date getCreationDate(PGPPublicKey key) {
        return key.getCreationTime();
    }

    public static Date getCreationDate(PGPSecretKey key) {
        return key.getPublicKey().getCreationTime();
    }

    @SuppressWarnings("unchecked")
    public static PGPPublicKey getMasterKey(PGPPublicKeyRing keyRing) {
        if (keyRing == null) {
            return null;
        }
        for (PGPPublicKey key : new IterableIterator<PGPPublicKey>(keyRing.getPublicKeys())) {
            if (key.isMasterKey()) {
                return key;
            }
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    public static PGPSecretKey getMasterKey(PGPSecretKeyRing keyRing) {
        if (keyRing == null) {
            return null;
        }
        for (PGPSecretKey key : new IterableIterator<PGPSecretKey>(keyRing.getSecretKeys())) {
            if (key.isMasterKey()) {
                return key;
            }
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    public static PGPSecretKey getKeyNum(PGPSecretKeyRing keyRing, long num) {
        long cnt = 0;
        if (keyRing == null) {
            return null;
        }
        for (PGPSecretKey key : new IterableIterator<PGPSecretKey>(keyRing.getSecretKeys())) {
            if (cnt == num) {
                return key;
            }
            cnt++;
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    public static Vector<PGPPublicKey> getEncryptKeys(PGPPublicKeyRing keyRing) {
        Vector<PGPPublicKey> encryptKeys = new Vector<PGPPublicKey>();

        for (PGPPublicKey key : new IterableIterator<PGPPublicKey>(keyRing.getPublicKeys())) {
            if (isEncryptionKey(key)) {
                encryptKeys.add(key);
            }
        }

        return encryptKeys;
    }

    @SuppressWarnings("unchecked")
    public static Vector<PGPSecretKey> getSigningKeys(PGPSecretKeyRing keyRing) {
        Vector<PGPSecretKey> signingKeys = new Vector<PGPSecretKey>();

        for (PGPSecretKey key : new IterableIterator<PGPSecretKey>(keyRing.getSecretKeys())) {
            if (isSigningKey(key)) {
                signingKeys.add(key);
            }
        }

        return signingKeys;
    }

    @SuppressWarnings("unchecked")
    public static Vector<PGPSecretKey> getCertificationKeys(PGPSecretKeyRing keyRing) {
        Vector<PGPSecretKey> signingKeys = new Vector<PGPSecretKey>();

        for (PGPSecretKey key : new IterableIterator<PGPSecretKey>(keyRing.getSecretKeys())) {
            if (isCertificationKey(key)) {
                signingKeys.add(key);
            }
        }

        return signingKeys;
    }

    public static Vector<PGPPublicKey> getUsableEncryptKeys(PGPPublicKeyRing keyRing) {
        Vector<PGPPublicKey> usableKeys = new Vector<PGPPublicKey>();
        Vector<PGPPublicKey> encryptKeys = getEncryptKeys(keyRing);
        PGPPublicKey masterKey = null;
        for (int i = 0; i < encryptKeys.size(); ++i) {
            PGPPublicKey key = encryptKeys.get(i);
            if (!isExpired(key) && !key.isRevoked()) {
                if (key.isMasterKey()) {
                    masterKey = key;
                } else {
                    usableKeys.add(key);
                }
            }
        }
        if (masterKey != null) {
            usableKeys.add(masterKey);
        }
        return usableKeys;
    }

    public static boolean isExpired(PGPPublicKey key) {
        Date creationDate = getCreationDate(key);
        Date expiryDate = getExpiryDate(key);
        Date now = new Date();
        if (now.compareTo(creationDate) >= 0
                && (expiryDate == null || now.compareTo(expiryDate) <= 0)) {
            return false;
        }
        return true;
    }

    public static Vector<PGPSecretKey> getUsableCertificationKeys(PGPSecretKeyRing keyRing) {
        Vector<PGPSecretKey> usableKeys = new Vector<PGPSecretKey>();
        Vector<PGPSecretKey> signingKeys = getCertificationKeys(keyRing);
        PGPSecretKey masterKey = null;
        for (int i = 0; i < signingKeys.size(); ++i) {
            PGPSecretKey key = signingKeys.get(i);
            if (key.isMasterKey()) {
                masterKey = key;
            } else {
                usableKeys.add(key);
            }
        }
        if (masterKey != null) {
            usableKeys.add(masterKey);
        }
        return usableKeys;
    }

    public static Vector<PGPSecretKey> getUsableSigningKeys(PGPSecretKeyRing keyRing) {
        Vector<PGPSecretKey> usableKeys = new Vector<PGPSecretKey>();
        Vector<PGPSecretKey> signingKeys = getSigningKeys(keyRing);
        PGPSecretKey masterKey = null;
        for (int i = 0; i < signingKeys.size(); ++i) {
            PGPSecretKey key = signingKeys.get(i);
            if (key.isMasterKey()) {
                masterKey = key;
            } else {
                usableKeys.add(key);
            }
        }
        if (masterKey != null) {
            usableKeys.add(masterKey);
        }
        return usableKeys;
    }

    public static Date getExpiryDate(PGPPublicKey key) {
        Date creationDate = getCreationDate(key);
        if (key.getValidDays() == 0) {
            // no expiry
            return null;
        }
        Calendar calendar = GregorianCalendar.getInstance();
        calendar.setTime(creationDate);
        calendar.add(Calendar.DATE, key.getValidDays());
        Date expiryDate = calendar.getTime();

        return expiryDate;
    }

    public static Date getExpiryDate(PGPSecretKey key) {
        return getExpiryDate(key.getPublicKey());
    }

    public static PGPPublicKey getEncryptPublicKey(Context context, long masterKeyId) {
        PGPPublicKeyRing keyRing = ProviderHelper.getPGPPublicKeyRingByMasterKeyId(context,
                masterKeyId);
        if (keyRing == null) {
            Log.e(Constants.TAG, "keyRing is null!");
            return null;
        }
        Vector<PGPPublicKey> encryptKeys = getUsableEncryptKeys(keyRing);
        if (encryptKeys.size() == 0) {
            Log.e(Constants.TAG, "encryptKeys is null!");
            return null;
        }
        return encryptKeys.get(0);
    }

    public static PGPSecretKey getCertificationKey(Context context, long masterKeyId) {
        PGPSecretKeyRing keyRing = ProviderHelper.getPGPSecretKeyRingByMasterKeyId(context,
                masterKeyId);
        if (keyRing == null) {
            return null;
        }
        Vector<PGPSecretKey> signingKeys = getUsableCertificationKeys(keyRing);
        if (signingKeys.size() == 0) {
            return null;
        }
        return signingKeys.get(0);
    }

    public static PGPSecretKey getSigningKey(Context context, long masterKeyId) {
        PGPSecretKeyRing keyRing = ProviderHelper.getPGPSecretKeyRingByMasterKeyId(context,
                masterKeyId);
        if (keyRing == null) {
            return null;
        }
        Vector<PGPSecretKey> signingKeys = getUsableSigningKeys(keyRing);
        if (signingKeys.size() == 0) {
            return null;
        }
        return signingKeys.get(0);
    }

    @SuppressWarnings("unchecked")
    public static String getMainUserId(PGPPublicKey key) {
        for (String userId : new IterableIterator<String>(key.getUserIDs())) {
            return userId;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static String getMainUserId(PGPSecretKey key) {
        for (String userId : new IterableIterator<String>(key.getUserIDs())) {
            return userId;
        }
        return null;
    }

    public static String getMainUserIdSafe(Context context, PGPPublicKey key) {
        String userId = getMainUserId(key);
        if (userId == null || userId.equals("")) {
            userId = context.getString(R.string.user_id_no_name);
        }
        return userId;
    }

    public static String getMainUserIdSafe(Context context, PGPSecretKey key) {
        String userId = getMainUserId(key);
        if (userId == null || userId.equals("")) {
            userId = context.getString(R.string.user_id_no_name);
        }
        return userId;
    }

    @SuppressWarnings("unchecked")
    public static boolean isEncryptionKey(PGPPublicKey key) {
        if (!key.isEncryptionKey()) {
            return false;
        }

        if (key.getVersion() <= 3) {
            // this must be true now
            return key.isEncryptionKey();
        }

        // special cases
        if (key.getAlgorithm() == PGPPublicKey.ELGAMAL_ENCRYPT) {
            return true;
        }

        if (key.getAlgorithm() == PGPPublicKey.RSA_ENCRYPT) {
            return true;
        }

        for (PGPSignature sig : new IterableIterator<PGPSignature>(key.getSignatures())) {
            if (key.isMasterKey() && sig.getKeyID() != key.getKeyID()) {
                continue;
            }
            PGPSignatureSubpacketVector hashed = sig.getHashedSubPackets();

            if (hashed != null
                    && (hashed.getKeyFlags() & (KeyFlags.ENCRYPT_COMMS | KeyFlags.ENCRYPT_STORAGE)) != 0) {
                return true;
            }

            PGPSignatureSubpacketVector unhashed = sig.getUnhashedSubPackets();

            if (unhashed != null
                    && (unhashed.getKeyFlags() & (KeyFlags.ENCRYPT_COMMS | KeyFlags.ENCRYPT_STORAGE)) != 0) {
                return true;
            }
        }
        return false;
    }

    public static boolean isEncryptionKey(PGPSecretKey key) {
        return isEncryptionKey(key.getPublicKey());
    }

    @SuppressWarnings("unchecked")
    public static boolean isSigningKey(PGPPublicKey key) {
        if (key.getVersion() <= 3) {
            return true;
        }

        // special case
        if (key.getAlgorithm() == PGPPublicKey.RSA_SIGN) {
            return true;
        }

        for (PGPSignature sig : new IterableIterator<PGPSignature>(key.getSignatures())) {
            if (key.isMasterKey() && sig.getKeyID() != key.getKeyID()) {
                continue;
            }
            PGPSignatureSubpacketVector hashed = sig.getHashedSubPackets();

            if (hashed != null && (hashed.getKeyFlags() & KeyFlags.SIGN_DATA) != 0) {
                return true;
            }

            PGPSignatureSubpacketVector unhashed = sig.getUnhashedSubPackets();

            if (unhashed != null && (unhashed.getKeyFlags() & KeyFlags.SIGN_DATA) != 0) {
                return true;
            }
        }

        return false;
    }

    public static boolean isSigningKey(PGPSecretKey key) {
        return isSigningKey(key.getPublicKey());
    }

    @SuppressWarnings("unchecked")
    public static boolean isCertificationKey(PGPPublicKey key) {
        if (key.getVersion() <= 3) {
            return true;
        }

        for (PGPSignature sig : new IterableIterator<PGPSignature>(key.getSignatures())) {
            if (key.isMasterKey() && sig.getKeyID() != key.getKeyID()) {
                continue;
            }
            PGPSignatureSubpacketVector hashed = sig.getHashedSubPackets();

            if (hashed != null && (hashed.getKeyFlags() & KeyFlags.CERTIFY_OTHER) != 0) {
                return true;
            }

            PGPSignatureSubpacketVector unhashed = sig.getUnhashedSubPackets();

            if (unhashed != null && (unhashed.getKeyFlags() & KeyFlags.CERTIFY_OTHER) != 0) {
                return true;
            }
        }

        return false;
    }

    public static boolean isCertificationKey(PGPSecretKey key) {
        return isCertificationKey(key.getPublicKey());
    }

    public static String getAlgorithmInfo(PGPPublicKey key) {
        return getAlgorithmInfo(key.getAlgorithm(), key.getBitStrength());
    }

    public static String getAlgorithmInfo(PGPSecretKey key) {
        return getAlgorithmInfo(key.getPublicKey());
    }

    public static String getAlgorithmInfo(int algorithm, int keySize) {
        String algorithmStr = null;

        switch (algorithm) {
            case PGPPublicKey.RSA_ENCRYPT:
            case PGPPublicKey.RSA_GENERAL:
            case PGPPublicKey.RSA_SIGN: {
                algorithmStr = "RSA";
                break;
            }

            case PGPPublicKey.DSA: {
                algorithmStr = "DSA";
                break;
            }

            case PGPPublicKey.ELGAMAL_ENCRYPT:
            case PGPPublicKey.ELGAMAL_GENERAL: {
                algorithmStr = "ElGamal";
                break;
            }

            default: {
                algorithmStr = "Unknown";
                break;
            }
        }
        return algorithmStr + ", " + keySize + " bit";
    }

    public static String getFingerPrint(Context context, long keyId) {
        PGPPublicKey key = ProviderHelper.getPGPPublicKeyByKeyId(context, keyId);
        // if it is no public key get it from your own keys...
        if (key == null) {
            PGPSecretKey secretKey = ProviderHelper.getPGPSecretKeyByKeyId(context, keyId);
            if (secretKey == null) {
                Log.e(Constants.TAG, "Key could not be found!");
                return null;
            }
            key = secretKey.getPublicKey();
        }

        return convertFingerprintToHex(key.getFingerprint(), true);
    }

    /**
     * Converts fingerprint to hex (optional: with whitespaces after 4 characters)
     * <p/>
     * Fingerprint is shown using lowercase characters. Studies have shown that humans can
     * better differentiate between numbers and letters when letters are lowercase.
     *
     * @param fingerprint
     * @param split       split into 4 character chunks
     * @return
     */
    public static String convertFingerprintToHex(byte[] fingerprint, boolean split) {
        String hexString = Hex.toHexString(fingerprint);
        if (split) {
            hexString = hexString.replaceAll("(.{4})(?!$)", "$1 ");
        }

        return hexString;
    }

    /**
     * Convert key id from long to 64 bit hex string
     * <p/>
     * V4: "The Key ID is the low-order 64 bits of the fingerprint"
     * <p/>
     * see http://tools.ietf.org/html/rfc4880#section-12.2
     *
     * @param keyId
     * @return
     */
    public static String convertKeyIdToHex(long keyId) {
        long upper = keyId >> 32;
        if (upper == 0) {
            // this is a short key id
            return convertKeyIdToHexShort(keyId);
        }
        return "0x" + convertKeyIdToHex32bit(keyId >> 32) + convertKeyIdToHex32bit(keyId);
    }

    public static String convertKeyIdToHexShort(long keyId) {
        return "0x" + convertKeyIdToHex32bit(keyId);
    }

    private static String convertKeyIdToHex32bit(long keyId) {
        String hexString = Long.toHexString(keyId & 0xffffffffL).toLowerCase(Locale.US);
        while (hexString.length() < 8) {
            hexString = "0" + hexString;
        }
        return hexString;
    }

    /**
     * Used in HkpKeyServer to convert hex encoded key ids back to long.
     *
     * @param hexString
     * @return
     */
    public static long convertHexToKeyId(String hexString) {
        int len = hexString.length();
        String s2 = hexString.substring(len - 8);
        String s1 = hexString.substring(0, len - 8);
        return ((!s1.isEmpty() ? Long.parseLong(s1, 16) << 32 : 0) | Long.parseLong(s2, 16));
    }

    /**
     * Splits userId string into naming part, email part, and comment part
     *
     * @param userId
     * @return array with naming (0), email (1), comment (2)
     */
    public static String[] splitUserId(String userId) {
        String[] result = new String[]{null, null, null};

        if (userId == null || userId.equals("")) {
            return result;
        }

        /*
         * User ID matching:
         * http://fiddle.re/t4p6f
         *
         * test cases:
         * "Max Mustermann (this is a comment) <max@example.com>"
         * "Max Mustermann <max@example.com>"
         * "Max Mustermann (this is a comment)"
         * "Max Mustermann [this is nothing]"
         */
        Matcher matcher = USER_ID_PATTERN.matcher(userId);
        if (matcher.matches()) {
            result[0] = matcher.group(1);
            result[1] = matcher.group(3);
            result[2] = matcher.group(2);
        }

        return result;
    }

}
