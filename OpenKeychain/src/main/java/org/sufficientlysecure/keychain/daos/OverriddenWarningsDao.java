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


import android.content.Context;

import org.sufficientlysecure.keychain.KeychainDatabase;


public class OverriddenWarningsDao extends AbstractDao {
    public static OverriddenWarningsDao create(Context context) {
        KeychainDatabase database = KeychainDatabase.getInstance(context);
        DatabaseNotifyManager databaseNotifyManager = DatabaseNotifyManager.create(context);

        return new OverriddenWarningsDao(database, databaseNotifyManager);
    }

    private OverriddenWarningsDao(KeychainDatabase db,
            DatabaseNotifyManager databaseNotifyManager) {
        super(db, databaseNotifyManager);
    }

    public boolean isWarningOverridden(String identifier) {
        return getDatabase().getOverriddenWarningsQueries().selectCountByIdentifier(identifier)
                .executeAsOne() > 0;
    }

    public void putOverride(String identifier) {
        getDatabase().getOverriddenWarningsQueries().insertIdentifier(identifier);
    }

    public void deleteOverride(String identifier) {
        getDatabase().getOverriddenWarningsQueries().deleteByIdentifier(identifier);
    }
}
