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


import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import androidx.annotation.Nullable;

import com.squareup.sqldelight.SqlDelightQuery;
import org.sufficientlysecure.keychain.AutocryptPeersModel.DeleteByIdentifier;
import org.sufficientlysecure.keychain.AutocryptPeersModel.DeleteByMasterKeyId;
import org.sufficientlysecure.keychain.AutocryptPeersModel.InsertPeer;
import org.sufficientlysecure.keychain.AutocryptPeersModel.UpdateGossipKey;
import org.sufficientlysecure.keychain.AutocryptPeersModel.UpdateKey;
import org.sufficientlysecure.keychain.AutocryptPeersModel.UpdateLastSeen;
import org.sufficientlysecure.keychain.KeychainDatabase;
import org.sufficientlysecure.keychain.model.AutocryptPeer;
import org.sufficientlysecure.keychain.model.AutocryptPeer.AutocryptKeyStatus;
import org.sufficientlysecure.keychain.model.AutocryptPeer.GossipOrigin;


public class AutocryptPeerDao extends AbstractDao {
    public static AutocryptPeerDao getInstance(Context context) {
        KeychainDatabase keychainDatabase = KeychainDatabase.getInstance(context);
        DatabaseNotifyManager databaseNotifyManager = DatabaseNotifyManager.create(context);

        return new AutocryptPeerDao(keychainDatabase, databaseNotifyManager);
    }

    private AutocryptPeerDao(KeychainDatabase database, DatabaseNotifyManager databaseNotifyManager) {
        super(database, databaseNotifyManager);
    }

    public Long getMasterKeyIdForAutocryptPeer(String autocryptId) {
        SqlDelightQuery query = AutocryptPeer.FACTORY.selectMasterKeyIdByIdentifier(autocryptId);
        try (Cursor cursor = getReadableDb().query(query)) {
            if (cursor.moveToFirst()) {
                return AutocryptPeer.FACTORY.selectMasterKeyIdByIdentifierMapper().map(cursor);
            }
        }
        return null;
    }

    @Nullable
    public AutocryptPeer getAutocryptPeer(String autocryptId) {
        List<AutocryptPeer> autocryptPeers = getAutocryptPeers(autocryptId);
        if (!autocryptPeers.isEmpty()) {
            return autocryptPeers.get(0);
        }
        return null;
    }

    private List<AutocryptPeer> getAutocryptPeers(String... autocryptId) {
        SqlDelightQuery query = AutocryptPeer.FACTORY.selectByIdentifiers(autocryptId);
        return mapAllRows(query, AutocryptPeer.PEER_MAPPER);
    }

    public List<AutocryptKeyStatus> getAutocryptKeyStatus(String[] autocryptIds) {
        SqlDelightQuery query = AutocryptPeer.FACTORY.selectAutocryptKeyStatus(autocryptIds);
        return mapAllRows(query, AutocryptPeer.KEY_STATUS_MAPPER);
    }

    private void ensureAutocryptPeerExists(String autocryptId) {
        InsertPeer insertStatement = new InsertPeer(getWritableDb());
        insertStatement.bind(autocryptId);
        insertStatement.executeInsert();
    }

    public void insertOrUpdateLastSeen(String autocryptId, Date date) {
        ensureAutocryptPeerExists(autocryptId);

        UpdateLastSeen updateStatement = new UpdateLastSeen(getWritableDb(), AutocryptPeer.FACTORY);
        updateStatement.bind(autocryptId, date);
        updateStatement.executeUpdateDelete();
    }

    public void updateKey(String autocryptId, Date effectiveDate,
            long masterKeyId,
            boolean isMutual) {
        ensureAutocryptPeerExists(autocryptId);

        UpdateKey updateStatement = new UpdateKey(getWritableDb(), AutocryptPeer.FACTORY);
        updateStatement.bind(autocryptId, effectiveDate, masterKeyId, isMutual);
        updateStatement.executeUpdateDelete();

        getDatabaseNotifyManager().notifyAutocryptUpdate(autocryptId, masterKeyId);
    }

    public void updateKeyGossip(String autocryptId, Date effectiveDate,
            long masterKeyId,
            GossipOrigin origin) {
        ensureAutocryptPeerExists(autocryptId);

        UpdateGossipKey updateStatement = new UpdateGossipKey(getWritableDb(), AutocryptPeer.FACTORY);
        updateStatement.bind(autocryptId, effectiveDate, masterKeyId, origin);
        updateStatement.executeUpdateDelete();

        getDatabaseNotifyManager().notifyAutocryptUpdate(autocryptId, masterKeyId);
    }

    public List<AutocryptPeer> getAutocryptPeersForKey(long masterKeyId) {
        ArrayList<AutocryptPeer> result = new ArrayList<>();
        SqlDelightQuery query = AutocryptPeer.FACTORY.selectByMasterKeyId(masterKeyId);
        try (Cursor cursor = getReadableDb().query(query)) {
            if (cursor.moveToNext()) {
                AutocryptPeer autocryptPeer = AutocryptPeer.PEER_MAPPER.map(cursor);
                result.add(autocryptPeer);
            }
        }
        return result;
    }

    public void deleteByIdentifier(String autocryptId) {
        Long masterKeyId = getMasterKeyIdForAutocryptPeer(autocryptId);
        DeleteByIdentifier deleteStatement = new DeleteByIdentifier(getReadableDb());
        deleteStatement.bind(autocryptId);
        deleteStatement.execute();
        if (masterKeyId != null) {
            getDatabaseNotifyManager().notifyAutocryptDelete(autocryptId, masterKeyId);
        }
    }

    public void deleteByMasterKeyId(long masterKeyId) {
        DeleteByMasterKeyId deleteStatement = new DeleteByMasterKeyId(getReadableDb());
        deleteStatement.bind(masterKeyId);
        deleteStatement.execute();
    }
}
