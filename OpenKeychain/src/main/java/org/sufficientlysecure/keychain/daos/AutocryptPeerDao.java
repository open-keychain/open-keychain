/*
 * Copyright (C) 2017 Schürmann & Breitmoser GbR
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

package org.sufficientlysecure.keychain.daos;


import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import android.content.Context;

import androidx.annotation.Nullable;
import org.sufficientlysecure.keychain.AutocryptKeyStatus;
import org.sufficientlysecure.keychain.AutocryptPeersQueries;
import org.sufficientlysecure.keychain.Autocrypt_peers;
import org.sufficientlysecure.keychain.KeychainDatabase;
import org.sufficientlysecure.keychain.SelectMasterKeyIdByIdentifier;
import org.sufficientlysecure.keychain.model.GossipOrigin;


public class AutocryptPeerDao extends AbstractDao {
    private final AutocryptPeersQueries autocryptPeersQueries =
            getDatabase().getAutocryptPeersQueries();

    public static AutocryptPeerDao getInstance(Context context) {
        KeychainDatabase keychainDatabase = KeychainDatabase.getInstance(context);
        DatabaseNotifyManager databaseNotifyManager = DatabaseNotifyManager.create(context);

        return new AutocryptPeerDao(keychainDatabase, databaseNotifyManager);
    }

    private AutocryptPeerDao(KeychainDatabase database,
            DatabaseNotifyManager databaseNotifyManager) {
        super(database, databaseNotifyManager);
    }

    public Long getMasterKeyIdForAutocryptPeer(String autocryptId) {
        SelectMasterKeyIdByIdentifier masterKeyId =
                autocryptPeersQueries.selectMasterKeyIdByIdentifier(autocryptId)
                        .executeAsOneOrNull();
        if (masterKeyId != null) {
            return masterKeyId.getMaster_key_id();
        }
        return null;
    }

    @Nullable
    public Autocrypt_peers getAutocryptPeer(String packageName, String autocryptId) {
        return autocryptPeersQueries.selectByIdentifiers(packageName,
                Collections.singleton(autocryptId)).executeAsOneOrNull();
    }

    private List<Autocrypt_peers> getAutocryptPeers(String packageName, String... autocryptId) {
        return autocryptPeersQueries.selectByIdentifiers(packageName, Arrays.asList(autocryptId))
                .executeAsList();
    }

    public List<AutocryptKeyStatus> getAutocryptKeyStatus(String packageName,
            String[] autocryptIds) {
        return autocryptPeersQueries.selectAutocryptKeyStatus(packageName,
                Arrays.asList(autocryptIds)).executeAsList();
    }

    public List<AutocryptKeyStatus> getAutocryptKeyStatusLike(String packageName,
            String query) {
        return autocryptPeersQueries.selectAutocryptKeyStatusLike(packageName, query).executeAsList();
    }

    private void ensureAutocryptPeerExists(String packageName, String autocryptId) {
        autocryptPeersQueries.insertPeer(packageName, autocryptId);
    }

    public void insertOrUpdateLastSeen(String packageName, String autocryptId, Date date) {
        ensureAutocryptPeerExists(packageName, autocryptId);
        autocryptPeersQueries.updateLastSeen(packageName, autocryptId, date);
    }

    public void updateKey(String packageName, String autocryptId, Date effectiveDate,
            long masterKeyId,
            boolean isMutual) {
        ensureAutocryptPeerExists(packageName, autocryptId);
        autocryptPeersQueries.updateKey(packageName, autocryptId, effectiveDate, masterKeyId,
                isMutual);
        getDatabaseNotifyManager().notifyAutocryptUpdate(autocryptId, masterKeyId);
    }

    public void updateKeyGossip(String packageName, String autocryptId, Date effectiveDate,
            long masterKeyId,
            GossipOrigin origin) {
        ensureAutocryptPeerExists(packageName, autocryptId);
        autocryptPeersQueries.updateGossipKey(packageName, autocryptId, effectiveDate, masterKeyId,
                origin);
        getDatabaseNotifyManager().notifyAutocryptUpdate(autocryptId, masterKeyId);
    }

    public List<Autocrypt_peers> getAutocryptPeersForKey(long masterKeyId) {
        return autocryptPeersQueries.selectByMasterKeyId(masterKeyId).executeAsList();
    }

    public void deleteByIdentifier(String packageName, String autocryptId) {
        Long masterKeyId = getMasterKeyIdForAutocryptPeer(autocryptId);
        autocryptPeersQueries.deleteByIdentifier(packageName, autocryptId);
        if (masterKeyId != null) {
            getDatabaseNotifyManager().notifyAutocryptDelete(autocryptId, masterKeyId);
        }
    }

    public void deleteByMasterKeyId(long masterKeyId) {
        autocryptPeersQueries.deleteByMasterKeyId(masterKeyId);
    }
}
