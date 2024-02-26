package org.sufficientlysecure.keychain.remote;


import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.text.format.DateUtils;

import androidx.annotation.Nullable;
import org.openintents.openpgp.AutocryptPeerUpdate;
import org.openintents.openpgp.AutocryptPeerUpdate.PreferEncrypt;
import org.sufficientlysecure.keychain.AutocryptKeyStatus;
import org.sufficientlysecure.keychain.Autocrypt_peers;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.daos.AutocryptPeerDao;
import org.sufficientlysecure.keychain.daos.KeyWritableRepository;
import org.sufficientlysecure.keychain.model.GossipOrigin;
import org.sufficientlysecure.keychain.operations.results.SaveKeyringResult;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import timber.log.Timber;


public class AutocryptInteractor {
    private static final long AUTOCRYPT_DISCOURAGE_THRESHOLD_MILLIS = 35 * DateUtils.DAY_IN_MILLIS;

    private AutocryptPeerDao autocryptPeerDao;
    private KeyWritableRepository keyWritableRepository;

    private final String packageName;

    public static AutocryptInteractor getInstance(Context context, String packageName) {
        AutocryptPeerDao autocryptPeerDao = AutocryptPeerDao.getInstance(context);
        KeyWritableRepository keyWritableRepository = KeyWritableRepository.create(context);

        return new AutocryptInteractor(autocryptPeerDao, keyWritableRepository, packageName);
    }

    private AutocryptInteractor(AutocryptPeerDao autocryptPeerDao,
            KeyWritableRepository keyWritableRepository, String packageName) {
        this.autocryptPeerDao = autocryptPeerDao;
        this.keyWritableRepository = keyWritableRepository;
        this.packageName = packageName;
    }

    void updateAutocryptPeerState(String autocryptPeerId, AutocryptPeerUpdate autocryptPeerUpdate) {
        Autocrypt_peers currentAutocryptPeer = autocryptPeerDao.getAutocryptPeer(packageName, autocryptPeerId);
        Date effectiveDate = autocryptPeerUpdate.getEffectiveDate();

        // 1. If the message’s effective date is older than the peers[from-addr].autocrypt_timestamp value, then no changes are required, and the update process terminates.
        Date lastSeenKey = currentAutocryptPeer != null ? currentAutocryptPeer.getLast_seen_key() : null;
        if (lastSeenKey != null && effectiveDate.compareTo(lastSeenKey) <= 0) {
            return;
        }

        // 2. If the message’s effective date is more recent than peers[from-addr].last_seen then set peers[from-addr].last_seen to the message’s effective date.
        Date lastSeen = currentAutocryptPeer != null ? currentAutocryptPeer.getLast_seen() : null;
        if (lastSeen == null || effectiveDate.after(lastSeen)) {
            autocryptPeerDao.insertOrUpdateLastSeen(packageName, autocryptPeerId, effectiveDate);
        }

        // 3. If the Autocrypt header is unavailable, no further changes are required and the update process terminates.
        if (!autocryptPeerUpdate.hasKeyData()) {
            return;
        }

        SaveKeyringResult saveKeyringResult = parseAndImportAutocryptKeyData(autocryptPeerUpdate);
        if (saveKeyringResult == null) {
            return;
        }

        // 4. Set peers[from-addr].autocrypt_timestamp to the message’s effective date.
        // 5. Set peers[from-addr].public_key to the corresponding keydata value of the Autocrypt header.
        Long newMasterKeyId = saveKeyringResult.savedMasterKeyId;
        // 6. Set peers[from-addr].prefer_encrypt to the corresponding prefer-encrypt value of the Autocrypt header.
        boolean isMutual = autocryptPeerUpdate.getPreferEncrypt() == PreferEncrypt.MUTUAL;

        autocryptPeerDao.updateKey(packageName, autocryptPeerId, effectiveDate, newMasterKeyId, isMutual);
    }

    void updateAutocryptPeerGossipState(String autocryptPeerId, AutocryptPeerUpdate autocryptPeerUpdate) {
        Autocrypt_peers currentAutocryptPeer = autocryptPeerDao.getAutocryptPeer(packageName, autocryptPeerId);
        Date effectiveDate = autocryptPeerUpdate.getEffectiveDate();

        // 1. If gossip-addr does not match any recipient in the mail’s To or Cc header, the update process terminates (i.e., header is ignored).
        // -> This should be taken care of in the mail client that sends us this data!

        // 2. If peers[gossip-addr].gossip_timestamp is more recent than the message’s effective date, then the update process terminates.
        Date lastSeenGossip = currentAutocryptPeer != null ? currentAutocryptPeer.getGossip_last_seen_key() : null;
        if (lastSeenGossip != null && lastSeenGossip.after(effectiveDate)) {
            return;
        }

        if (!autocryptPeerUpdate.hasKeyData()) {
            return;
        }

        SaveKeyringResult saveKeyringResult = parseAndImportAutocryptKeyData(autocryptPeerUpdate);
        if (saveKeyringResult == null) {
            return;
        }

        // 3. Set peers[gossip-addr].gossip_timestamp to the message’s effective date.
        // 4. Set peers[gossip-addr].gossip_key to the value of the keydata attribute.
        Long newMasterKeyId = saveKeyringResult.savedMasterKeyId;

        autocryptPeerDao.updateKeyGossip(packageName, autocryptPeerId, effectiveDate, newMasterKeyId,
                GossipOrigin.GOSSIP_HEADER);
    }

    @Nullable
    private SaveKeyringResult parseAndImportAutocryptKeyData(AutocryptPeerUpdate autocryptPeerUpdate) {
        UncachedKeyRing uncachedKeyRing = parseAutocryptKeyData(autocryptPeerUpdate);
        if (uncachedKeyRing != null) {
            return importAutocryptKeyData(uncachedKeyRing);
        }
        return null;
    }

    @Nullable
    private SaveKeyringResult importAutocryptKeyData(UncachedKeyRing uncachedKeyRing) {
        SaveKeyringResult saveKeyringResult = keyWritableRepository.savePublicKeyRing(uncachedKeyRing);
        if (!saveKeyringResult.success()) {
            Timber.e(Constants.TAG, "Error inserting key - ignoring!");
            return null;
        }
        return saveKeyringResult;
    }

    @Nullable
    private UncachedKeyRing parseAutocryptKeyData(AutocryptPeerUpdate autocryptPeerUpdate) {
        UncachedKeyRing uncachedKeyRing;
        try {
            uncachedKeyRing = UncachedKeyRing.decodeFromData(autocryptPeerUpdate.getKeyData());
        } catch (IOException | PgpGeneralException e) {
            Timber.e(Constants.TAG, "Error parsing public key! - Ignoring");
            return null;
        }
        if (uncachedKeyRing.isSecret()) {
            Timber.e(Constants.TAG, "Found secret key in autocrypt id! - Ignoring");
            return null;
        }
        return uncachedKeyRing;
    }

    public Map<String,AutocryptRecommendationResult> determineAutocryptRecommendations(String... autocryptIds) {
        Map<String,AutocryptRecommendationResult> result = new HashMap<>(autocryptIds.length);

        for (AutocryptKeyStatus autocryptKeyStatus : autocryptPeerDao.getAutocryptKeyStatus(packageName, autocryptIds)) {
            AutocryptRecommendationResult peerResult = determineAutocryptRecommendation(autocryptKeyStatus);
            result.put(peerResult.peerId, peerResult);
        }

        return Collections.unmodifiableMap(result);
    }

    public Map<String,AutocryptRecommendationResult> determineAutocryptRecommendationsLike(String query) {
        Map<String,AutocryptRecommendationResult> result = new HashMap<>();

        for (AutocryptKeyStatus autocryptKeyStatus : autocryptPeerDao.getAutocryptKeyStatusLike(packageName, query)) {
            AutocryptRecommendationResult peerResult = determineAutocryptRecommendation(autocryptKeyStatus);
            result.put(peerResult.peerId, peerResult);
        }

        return Collections.unmodifiableMap(result);
    }

    /** Determines Autocrypt "ui-recommendation", according to spec.
     * See https://autocrypt.org/level1.html#recommendations-for-single-recipient-messages
     */
    private AutocryptRecommendationResult determineAutocryptRecommendation(
            AutocryptKeyStatus autocryptKeyStatus) {
        AutocryptRecommendationResult keyRecommendation = determineAutocryptKeyRecommendation(autocryptKeyStatus);
        if (keyRecommendation != null) return keyRecommendation;

        AutocryptRecommendationResult gossipRecommendation = determineAutocryptGossipRecommendation(autocryptKeyStatus);
        if (gossipRecommendation != null) return gossipRecommendation;

        return new AutocryptRecommendationResult(autocryptKeyStatus.getIdentifier(), AutocryptState.DISABLE, null, false);
    }

    @Nullable
    private AutocryptRecommendationResult determineAutocryptKeyRecommendation(
            AutocryptKeyStatus autocryptKeyStatus) {
        Long masterKeyId = autocryptKeyStatus.getMaster_key_id();
        boolean hasKey = masterKeyId != null;
        boolean isRevoked = Boolean.TRUE.equals(autocryptKeyStatus.getKey_is_revoked());
        boolean isExpired = autocryptKeyStatus.getKey_is_expired_int() != 0;
        if (!hasKey || isRevoked || isExpired) {
            return null;
        }

        Date lastSeen = autocryptKeyStatus.getLast_seen();
        Date lastSeenKey = autocryptKeyStatus.getLast_seen_key();
        boolean isVerified = autocryptKeyStatus.getKey_is_verified();
        boolean isLastSeenOlderThanDiscourageTimespan = lastSeen != null && lastSeenKey != null &&
                lastSeenKey.getTime() < (lastSeen.getTime() - AUTOCRYPT_DISCOURAGE_THRESHOLD_MILLIS);
        if (isLastSeenOlderThanDiscourageTimespan) {
            return new AutocryptRecommendationResult(autocryptKeyStatus.getIdentifier(), AutocryptState.DISCOURAGED_OLD, masterKeyId, isVerified);
        }

        boolean isMutual = autocryptKeyStatus.is_mutual();
        if (isMutual) {
            return new AutocryptRecommendationResult(autocryptKeyStatus.getIdentifier(), AutocryptState.MUTUAL, masterKeyId, isVerified);
        } else {
            return new AutocryptRecommendationResult(autocryptKeyStatus.getIdentifier(), AutocryptState.AVAILABLE, masterKeyId, isVerified);
        }
    }

    @Nullable
    private AutocryptRecommendationResult determineAutocryptGossipRecommendation(
            AutocryptKeyStatus autocryptKeyStatus) {
        boolean gossipHasKey = autocryptKeyStatus.getGossip_master_key_id() != null;
        boolean gossipIsRevoked =
                Boolean.TRUE.equals(autocryptKeyStatus.getGossip_key_is_revoked());
        boolean gossipIsExpired = autocryptKeyStatus.getGossip_key_is_expired_int() != 0;
        boolean isVerified = autocryptKeyStatus.getGossip_key_is_verified();

        if (!gossipHasKey || gossipIsRevoked || gossipIsExpired) {
            return null;
        }

        Long masterKeyId = autocryptKeyStatus.getGossip_master_key_id();
        return new AutocryptRecommendationResult(autocryptKeyStatus.getIdentifier(), AutocryptState.DISCOURAGED_GOSSIP, masterKeyId, isVerified);
    }

    public void updateKeyGossipFromSignature(String autocryptId, Date effectiveDate, long masterKeyId) {
        autocryptPeerDao.updateKeyGossip(packageName, autocryptId, effectiveDate, masterKeyId, GossipOrigin.SIGNATURE);
    }

    public void updateKeyGossipFromDedup(String autocryptId, long masterKeyId) {
        autocryptPeerDao.updateKeyGossip(packageName, autocryptId, new Date(), masterKeyId, GossipOrigin.DEDUP);
    }

    public static class AutocryptRecommendationResult {
        public final String peerId;
        public final Long masterKeyId;
        public final AutocryptState autocryptState;
        public final boolean isVerified;

        AutocryptRecommendationResult(String peerId, AutocryptState autocryptState, Long masterKeyId,
                boolean isVerified) {
            this.peerId = peerId;
            this.autocryptState = autocryptState;
            this.masterKeyId = masterKeyId;
            this.isVerified = isVerified;
        }

    }

    public enum AutocryptState {
        DISABLE, DISCOURAGED_OLD, DISCOURAGED_GOSSIP, AVAILABLE, MUTUAL
    }
}
