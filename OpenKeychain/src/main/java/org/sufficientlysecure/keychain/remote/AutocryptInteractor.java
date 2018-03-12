package org.sufficientlysecure.keychain.remote;


import java.io.IOException;
import java.util.Date;

import android.content.Context;
import android.support.annotation.Nullable;

import org.openintents.openpgp.AutocryptPeerUpdate;
import org.openintents.openpgp.AutocryptPeerUpdate.PreferEncrypt;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.operations.results.SaveKeyringResult;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.provider.AutocryptPeerDataAccessObject;
import org.sufficientlysecure.keychain.provider.KeyWritableRepository;
import timber.log.Timber;


class AutocryptInteractor {

    private AutocryptPeerDataAccessObject autocryptPeerDao;
    private KeyWritableRepository keyWritableRepository;

    public static AutocryptInteractor getInstance(Context context, AutocryptPeerDataAccessObject autocryptPeerentityDao) {
        KeyWritableRepository keyWritableRepository = KeyWritableRepository.create(context);

        return new AutocryptInteractor(autocryptPeerentityDao, keyWritableRepository);
    }

    private AutocryptInteractor(AutocryptPeerDataAccessObject autocryptPeerDao,
            KeyWritableRepository keyWritableRepository) {
        this.autocryptPeerDao = autocryptPeerDao;
        this.keyWritableRepository = keyWritableRepository;
    }

    void updateAutocryptPeerState(String autocryptPeerId, AutocryptPeerUpdate autocryptPeerUpdate) {
        Date effectiveDate = autocryptPeerUpdate.getEffectiveDate();

        // 1. If the message’s effective date is older than the peers[from-addr].autocrypt_timestamp value, then no changes are required, and the update process terminates.
        Date lastSeenAutocrypt = autocryptPeerDao.getLastSeenKey(autocryptPeerId);
        if (lastSeenAutocrypt != null && lastSeenAutocrypt.after(effectiveDate)) {
            return;
        }

        // 2. If the message’s effective date is more recent than peers[from-addr].last_seen then set peers[from-addr].last_seen to the message’s effective date.
        Date lastSeen = autocryptPeerDao.getLastSeen(autocryptPeerId);
        if (lastSeen == null || lastSeen.after(effectiveDate)) {
            autocryptPeerDao.updateLastSeen(autocryptPeerId, effectiveDate);
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

        autocryptPeerDao.updateKey(autocryptPeerId, effectiveDate, newMasterKeyId, isMutual);
    }

    void updateAutocryptPeerGossipState(String autocryptPeerId, AutocryptPeerUpdate autocryptPeerUpdate) {
        Date effectiveDate = autocryptPeerUpdate.getEffectiveDate();

        // 1. If gossip-addr does not match any recipient in the mail’s To or Cc header, the update process terminates (i.e., header is ignored).
        // -> This should be taken care of in the mail client that sends us this data!

        // 2. If peers[gossip-addr].gossip_timestamp is more recent than the message’s effective date, then the update process terminates.
        Date lastSeenGossip = autocryptPeerDao.getLastSeenGossip(autocryptPeerId);
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

        autocryptPeerDao.updateKeyGossipFromAutocrypt(autocryptPeerId, effectiveDate, newMasterKeyId);
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
}
