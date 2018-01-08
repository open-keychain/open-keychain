package org.sufficientlysecure.keychain.remote;


import java.io.IOException;
import java.util.Date;

import android.content.Context;

import org.openintents.openpgp.AutocryptPeerUpdate;
import org.openintents.openpgp.AutocryptPeerUpdate.PreferEncrypt;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.provider.AutocryptPeerDataAccessObject;
import org.sufficientlysecure.keychain.provider.KeyWritableRepository;
import org.sufficientlysecure.keychain.util.Log;


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

    void updateAutocryptPeerState(String autocryptPeerId, AutocryptPeerUpdate autocryptPeerUpdate)
            throws PgpGeneralException, IOException {
        if (autocryptPeerUpdate == null) {
            return;
        }

        Long newMasterKeyId;
        if (autocryptPeerUpdate.hasKeyData()) {
            UncachedKeyRing uncachedKeyRing = UncachedKeyRing.decodeFromData(autocryptPeerUpdate.getKeyData());
            if (uncachedKeyRing.isSecret()) {
                Log.e(Constants.TAG, "Found secret key in autocrypt id! - Ignoring");
                return;
            }
            // this will merge if the key already exists - no worries!
            keyWritableRepository.savePublicKeyRing(uncachedKeyRing);
            newMasterKeyId = uncachedKeyRing.getMasterKeyId();
        } else {
            newMasterKeyId = null;
        }

        Date effectiveDate = autocryptPeerUpdate.getEffectiveDate();
        autocryptPeerDao.updateLastSeen(autocryptPeerId, effectiveDate);

        if (newMasterKeyId == null) {
            return;
        }

        Date lastSeenKey = autocryptPeerDao.getLastSeenKey(autocryptPeerId);
        if (lastSeenKey == null || effectiveDate.after(lastSeenKey)) {
            boolean isMutual = autocryptPeerUpdate.getPreferEncrypt() == PreferEncrypt.MUTUAL;
            autocryptPeerDao.updateKey(autocryptPeerId, effectiveDate, newMasterKeyId, isMutual);
        }
    }


    void updateAutocryptPeerGossipState(String autocryptPeerId, AutocryptPeerUpdate autocryptPeerUpdate)
            throws PgpGeneralException, IOException {
        if (autocryptPeerUpdate == null) {
            return;
        }

        Long newMasterKeyId;
        if (autocryptPeerUpdate.hasKeyData()) {
            UncachedKeyRing uncachedKeyRing = UncachedKeyRing.decodeFromData(autocryptPeerUpdate.getKeyData());
            if (uncachedKeyRing.isSecret()) {
                Log.e(Constants.TAG, "Found secret key in autocrypt id! - Ignoring");
                return;
            }
            // this will merge if the key already exists - no worries!
            keyWritableRepository.savePublicKeyRing(uncachedKeyRing);
            newMasterKeyId = uncachedKeyRing.getMasterKeyId();
        } else {
            newMasterKeyId = null;
        }

        Date lastSeen = autocryptPeerDao.getLastSeen(autocryptPeerId);
        Date effectiveDate = autocryptPeerUpdate.getEffectiveDate();
        if (newMasterKeyId == null) {
            return;
        }

        Date lastSeenKey = autocryptPeerDao.getLastSeenKey(autocryptPeerId);
        if (lastSeenKey != null && effectiveDate.before(lastSeenKey)) {
            return;
        }

        if (lastSeen == null || effectiveDate.after(lastSeen)) {
            autocryptPeerDao.updateKeyGossip(autocryptPeerId, effectiveDate, newMasterKeyId);
        }
    }
}
