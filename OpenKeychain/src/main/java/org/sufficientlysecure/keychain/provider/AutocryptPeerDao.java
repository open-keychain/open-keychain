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

package org.sufficientlysecure.keychain.provider;


import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.content.Context;
import android.database.Cursor;
import android.support.annotation.Nullable;

import com.squareup.sqldelight.SqlDelightQuery;
import org.sufficientlysecure.keychain.AutocryptPeersModel.DeleteByIdentifier;
import org.sufficientlysecure.keychain.AutocryptPeersModel.DeleteByMasterKeyId;
import org.sufficientlysecure.keychain.AutocryptPeersModel.InsertPeer;
import org.sufficientlysecure.keychain.AutocryptPeersModel.UpdateGossipKey;
import org.sufficientlysecure.keychain.AutocryptPeersModel.UpdateKey;
import org.sufficientlysecure.keychain.AutocryptPeersModel.UpdateLastSeen;
import org.sufficientlysecure.keychain.model.AutocryptPeer;
import org.sufficientlysecure.keychain.model.AutocryptPeer.AutocryptKeyStatus;
import org.sufficientlysecure.keychain.model.AutocryptPeer.GossipOrigin;


public class AutocryptPeerDao {
    private final SupportSQLiteDatabase db;
    private final DatabaseNotifyManager databaseNotifyManager;

    public static AutocryptPeerDao getInstance(Context context) {
        KeychainDatabase keychainDatabase = new KeychainDatabase(context);
        DatabaseNotifyManager databaseNotifyManager = DatabaseNotifyManager.create(context);

        return new AutocryptPeerDao(keychainDatabase.getWritableDatabase(), databaseNotifyManager);
    }

    private AutocryptPeerDao(SupportSQLiteDatabase writableDatabase, DatabaseNotifyManager databaseNotifyManager) {
        this.db = writableDatabase;
        this.databaseNotifyManager = databaseNotifyManager;
    }

    public Long getMasterKeyIdForAutocryptPeer(String autocryptId) {
        SqlDelightQuery query = AutocryptPeer.FACTORY.selectMasterKeyIdByIdentifier(autocryptId);
        try (Cursor cursor = db.query(query)) {
            if (cursor.moveToFirst()) {
                return AutocryptPeer.FACTORY.selectMasterKeyIdByIdentifierMapper().map(cursor);
            }
        }
        return null;
    }

    @Nullable
    public AutocryptPeer getAutocryptPeer(String packageName, String autocryptId) {
        List<AutocryptPeer> autocryptPeers = getAutocryptPeers(packageName, autocryptId);
        if (!autocryptPeers.isEmpty()) {
            return autocryptPeers.get(0);
        }
        return null;
    }

    public List<AutocryptPeer> getAutocryptPeers(String packageName, String... autocryptId) {
        ArrayList<AutocryptPeer> result = new ArrayList<>(autocryptId.length);
        SqlDelightQuery query = AutocryptPeer.FACTORY.selectByIdentifiers(packageName, autocryptId);
        try (Cursor cursor = db.query(query)) {
            if (cursor.moveToNext()) {
                AutocryptPeer autocryptPeer = AutocryptPeer.PEER_MAPPER.map(cursor);
                result.add(autocryptPeer);
            }
        }
        return result;
    }

    public List<AutocryptKeyStatus> getAutocryptKeyStatus(String packageName, String[] autocryptIds) {
        ArrayList<AutocryptKeyStatus> result = new ArrayList<>(autocryptIds.length);
        SqlDelightQuery query = AutocryptPeer.FACTORY.selectAutocryptKeyStatus(packageName, autocryptIds, System.currentTimeMillis());
        try (Cursor cursor = db.query(query)) {
            if (cursor.moveToNext()) {
                AutocryptKeyStatus autocryptPeer = AutocryptPeer.KEY_STATUS_MAPPER.map(cursor);
                result.add(autocryptPeer);
            }
        }
        return result;
    }

    public void insertOrUpdateLastSeen(String packageName, String autocryptId, Date date) {
        UpdateLastSeen updateStatement = new UpdateLastSeen(db, AutocryptPeer.FACTORY);
        updateStatement.bind(packageName, autocryptId, date);
        int updated = updateStatement.executeUpdateDelete();

        if (updated == 0) {
            InsertPeer insertStatement = new InsertPeer(db, AutocryptPeer.FACTORY);
            insertStatement.bind(packageName, autocryptId, date);
            insertStatement.executeInsert();
        }
    }

    public void updateKey(String packageName, String autocryptId, Date effectiveDate, long masterKeyId,
            boolean isMutual) {
        UpdateKey updateStatement = new UpdateKey(db, AutocryptPeer.FACTORY);
        updateStatement.bind(packageName, autocryptId, effectiveDate, masterKeyId, isMutual);
        int rowsUpdated = updateStatement.executeUpdateDelete();
        if (rowsUpdated == 0) {
            throw new IllegalStateException("No rows updated! Was this peer inserted before the update?");
        }
        databaseNotifyManager.notifyAutocryptUpdate(autocryptId, masterKeyId);
    }

    public void updateKeyGossip(String packageName, String autocryptId, Date effectiveDate, long masterKeyId,
            GossipOrigin origin) {
        UpdateGossipKey updateStatement = new UpdateGossipKey(db, AutocryptPeer.FACTORY);
        updateStatement.bind(packageName, autocryptId, effectiveDate, masterKeyId, origin);
        int rowsUpdated = updateStatement.executeUpdateDelete();
        if (rowsUpdated == 0) {
            throw new IllegalStateException("No rows updated! Was this peer inserted before the update?");
        }
        databaseNotifyManager.notifyAutocryptUpdate(autocryptId, masterKeyId);
    }

    public List<AutocryptPeer> getAutocryptPeersForKey(long masterKeyId) {
        ArrayList<AutocryptPeer> result = new ArrayList<>();
        SqlDelightQuery query = AutocryptPeer.FACTORY.selectByMasterKeyId(masterKeyId);
        try (Cursor cursor = db.query(query)) {
            if (cursor.moveToNext()) {
                AutocryptPeer autocryptPeer = AutocryptPeer.PEER_MAPPER.map(cursor);
                result.add(autocryptPeer);
            }
        }
        return result;
    }

    public void deleteByIdentifier(String packageName, String autocryptId) {
        Long masterKeyId = getMasterKeyIdForAutocryptPeer(autocryptId);
        DeleteByIdentifier deleteStatement = new DeleteByIdentifier(db);
        deleteStatement.bind(packageName, autocryptId);
        deleteStatement.execute();
        if (masterKeyId != null) {
            databaseNotifyManager.notifyAutocryptDelete(autocryptId, masterKeyId);
        }
    }

    public void deleteByMasterKeyId(long masterKeyId) {
        DeleteByMasterKeyId deleteStatement = new DeleteByMasterKeyId(db);
        deleteStatement.bind(masterKeyId);
        deleteStatement.execute();
    }
}
