/*
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

package org.sufficientlysecure.keychain.ssh;


import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.Nullable;

import org.bouncycastle.bcpg.PublicKeyAlgorithmTags;
import org.sufficientlysecure.keychain.daos.KeyRepository;
import org.sufficientlysecure.keychain.securitytoken.EcKeyFormat;
import org.sufficientlysecure.keychain.securitytoken.Iso7816TLV;
import org.sufficientlysecure.keychain.securitytoken.KeyFormat;
import org.sufficientlysecure.keychain.securitytoken.OpenPgpCapabilities;
import org.sufficientlysecure.keychain.securitytoken.ResponseApdu;
import org.sufficientlysecure.keychain.securitytoken.RsaKeyFormat;
import org.sufficientlysecure.keychain.securitytoken.SecurityTokenConnection;
import org.sufficientlysecure.keychain.ssh.key.SshECDSAPublicKey;
import org.sufficientlysecure.keychain.ssh.key.SshEd25519PublicKey;
import org.sufficientlysecure.keychain.ssh.key.SshRSAPublicKey;
import org.sufficientlysecure.keychain.ssh.utils.SshUtils;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import timber.log.Timber;


/**
 * Remembers authentication keys that live in the auth slot of a security token, but were never
 * bound as a subkey to the certificate (e.g. generated on-card and only used via gpg-agent's
 * sshcontrol). Such keys can still be used for SSH: the public key is read from the card once,
 * and signing is diverted to the card's INTERNAL AUTHENTICATE.
 *
 * The card is associated with a master key through its signature or encryption slot, whose
 * subkey is known to the database. That subkey is also used as "PIN anchor", since PIN handling
 * requires a divert-to-card subkey that exists in the database.
 */
public class CardAuthKeyStore {
    private static final String PREF_FILE = "card_auth_keys";

    private final SharedPreferences mPrefs;

    public static CardAuthKeyStore getInstance(Context context) {
        return new CardAuthKeyStore(context.getApplicationContext());
    }

    private CardAuthKeyStore(Context context) {
        mPrefs = context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
    }

    public static class CardAuthKey {
        public final long masterKeyId;
        public final long authKeyId;
        public final long pinAnchorKeyId;
        public final int algorithm;
        @Nullable
        public final String curveOid;
        public final String sshPublicKey;

        CardAuthKey(long masterKeyId, long authKeyId, long pinAnchorKeyId, int algorithm,
                @Nullable String curveOid, String sshPublicKey) {
            this.masterKeyId = masterKeyId;
            this.authKeyId = authKeyId;
            this.pinAnchorKeyId = pinAnchorKeyId;
            this.algorithm = algorithm;
            this.curveOid = curveOid;
            this.sshPublicKey = sshPublicKey;
        }
    }

    @Nullable
    public CardAuthKey get(long masterKeyId) {
        String prefix = masterKeyId + ".";
        if (!mPrefs.contains(prefix + "auth")) {
            return null;
        }
        return new CardAuthKey(masterKeyId,
                mPrefs.getLong(prefix + "auth", 0),
                mPrefs.getLong(prefix + "pin", 0),
                mPrefs.getInt(prefix + "algo", 0),
                mPrefs.getString(prefix + "curve", null),
                mPrefs.getString(prefix + "ssh", null));
    }

    private void put(CardAuthKey key) {
        String prefix = key.masterKeyId + ".";
        mPrefs.edit()
                .putLong(prefix + "auth", key.authKeyId)
                .putLong(prefix + "pin", key.pinAnchorKeyId)
                .putInt(prefix + "algo", key.algorithm)
                .putString(prefix + "curve", key.curveOid)
                .putString(prefix + "ssh", key.sshPublicKey)
                .apply();
    }

    private void remove(long masterKeyId) {
        String prefix = masterKeyId + ".";
        mPrefs.edit()
                .remove(prefix + "auth")
                .remove(prefix + "pin")
                .remove(prefix + "algo")
                .remove(prefix + "curve")
                .remove(prefix + "ssh")
                .apply();
    }

    /**
     * Returns true if a token presenting authKeyId may be used for a request whose subkey id is
     * requiredKeyId, i.e. requiredKeyId is the PIN anchor of a remembered card auth key.
     */
    public boolean isPinAnchorFor(long masterKeyId, long requiredKeyId, long tokenAuthKeyId) {
        CardAuthKey key = get(masterKeyId);
        return key != null && key.pinAnchorKeyId == requiredKeyId
                && key.authKeyId == tokenAuthKeyId;
    }

    /**
     * Checks the connected token for an auth key that is not bound to its certificate, and if so
     * reads and remembers its public key. Errors are logged and otherwise ignored, this must never
     * break regular token handling.
     */
    public void updateFromToken(Context context, SecurityTokenConnection stConnection) {
        try {
            updateFromTokenOrThrow(context, stConnection);
        } catch (Exception e) {
            Timber.e(e, "Could not read unbound authentication key from security token");
        }
    }

    private void updateFromTokenOrThrow(Context context, SecurityTokenConnection stConnection)
            throws IOException, java.security.NoSuchAlgorithmException {
        OpenPgpCapabilities caps = stConnection.getOpenPgpCapabilities();
        byte[] fpAuth = caps.getFingerprintAuth();
        if (isEmptyFingerprint(fpAuth)) {
            return;
        }
        long authKeyId = KeyFormattingUtils.getKeyIdFromFingerprint(fpAuth);

        KeyRepository keyRepository = KeyRepository.create(context);
        long pinAnchorKeyId = 0;
        Long masterKeyId = null;
        for (byte[] fp : new byte[][] { caps.getFingerprintSign(), caps.getFingerprintEncrypt() }) {
            if (isEmptyFingerprint(fp)) {
                continue;
            }
            long keyId = KeyFormattingUtils.getKeyIdFromFingerprint(fp);
            masterKeyId = keyRepository.getMasterKeyIdBySubkeyId(keyId);
            if (masterKeyId != null) {
                pinAnchorKeyId = keyId;
                break;
            }
        }
        if (masterKeyId == null) {
            Timber.d("Security token does not belong to a known key, ignoring auth slot");
            return;
        }

        if (masterKeyId.equals(keyRepository.getMasterKeyIdBySubkeyId(authKeyId))) {
            // regular case: auth key is a bound subkey, nothing to remember
            remove(masterKeyId);
            return;
        }

        CardAuthKey existing = get(masterKeyId);
        if (existing != null && existing.authKeyId == authKeyId
                && existing.pinAnchorKeyId == pinAnchorKeyId) {
            return;
        }

        ResponseApdu response = stConnection.communicate(
                stConnection.getCommandFactory().createReadPublicKeyCommand(0xA4));
        if (!response.isSuccess()) {
            throw new IOException("Reading auth public key failed: "
                    + Integer.toHexString(response.getSw()));
        }
        Iso7816TLV template = Iso7816TLV.readSingle(response.getData(), true);

        KeyFormat format = caps.getAuthKeyFormat();
        int algorithm;
        String curveOid = null;
        String sshPublicKey;
        if (format instanceof RsaKeyFormat) {
            Iso7816TLV modulus = Iso7816TLV.findRecursive(template, 0x81);
            Iso7816TLV exponent = Iso7816TLV.findRecursive(template, 0x82);
            if (modulus == null || exponent == null) {
                throw new IOException("Malformed RSA public key template");
            }
            algorithm = PublicKeyAlgorithmTags.RSA_GENERAL;
            SshRSAPublicKey sshKey = new SshRSAPublicKey(
                    new BigInteger(1, exponent.mV), new BigInteger(1, modulus.mV));
            sshPublicKey = sshKey.getPublicKeyBlob();
        } else if (format instanceof EcKeyFormat) {
            EcKeyFormat ecFormat = (EcKeyFormat) format;
            Iso7816TLV point = Iso7816TLV.findRecursive(template, 0x86);
            if (point == null) {
                throw new IOException("Malformed EC public key template");
            }
            curveOid = ecFormat.curveOid().getId();
            if (ecFormat.isEdDsa()) {
                algorithm = PublicKeyAlgorithmTags.EDDSA;
                byte[] raw = point.mV;
                // some cards prefix the native point with 0x40, like OpenPGP does
                if (raw.length == 33 && raw[0] == 0x40) {
                    raw = Arrays.copyOfRange(raw, 1, raw.length);
                }
                sshPublicKey = new SshEd25519PublicKey(raw).getPublicKeyBlob();
            } else {
                algorithm = PublicKeyAlgorithmTags.ECDSA;
                SshECDSAPublicKey sshKey = new SshECDSAPublicKey(
                        SshUtils.getCurveName(curveOid), new BigInteger(1, point.mV));
                sshPublicKey = sshKey.getPublicKeyBlob();
            }
        } else {
            throw new IOException("Unsupported auth key format: " + format);
        }

        put(new CardAuthKey(
                masterKeyId, authKeyId, pinAnchorKeyId, algorithm, curveOid, sshPublicKey));
        Timber.i("Remembered unbound card auth key %s for master key %s",
                Long.toHexString(authKeyId), Long.toHexString(masterKeyId));
    }

    private static boolean isEmptyFingerprint(byte[] fp) {
        if (fp == null) {
            return true;
        }
        for (byte b : fp) {
            if (b != 0) {
                return false;
            }
        }
        return true;
    }
}
