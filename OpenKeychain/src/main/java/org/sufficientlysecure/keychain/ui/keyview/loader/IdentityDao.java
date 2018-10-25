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

package org.sufficientlysecure.keychain.ui.keyview.loader;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.squareup.sqldelight.SqlDelightQuery;
import org.openintents.openpgp.util.OpenPgpApi;
import org.sufficientlysecure.keychain.KeychainDatabase;
import org.sufficientlysecure.keychain.daos.AutocryptPeerDao;
import org.sufficientlysecure.keychain.model.AutocryptPeer;
import org.sufficientlysecure.keychain.model.UserPacket;
import org.sufficientlysecure.keychain.model.UserPacket.UserId;
import org.sufficientlysecure.keychain.ui.util.PackageIconGetter;


public class IdentityDao {
    private final SupportSQLiteDatabase db;
    private final PackageIconGetter packageIconGetter;
    private final PackageManager packageManager;
    private final AutocryptPeerDao autocryptPeerDao;

    public static IdentityDao getInstance(Context context) {
        SupportSQLiteDatabase db = KeychainDatabase.getInstance(context).getWritableDatabase();
        PackageManager packageManager = context.getPackageManager();
        PackageIconGetter iconGetter = PackageIconGetter.getInstance(context);
        AutocryptPeerDao autocryptPeerDao = AutocryptPeerDao.getInstance(context);
        return new IdentityDao(db, packageManager, iconGetter, autocryptPeerDao);
    }

    private IdentityDao(SupportSQLiteDatabase db,
            PackageManager packageManager, PackageIconGetter iconGetter,
            AutocryptPeerDao autocryptPeerDao) {
        this.db = db;
        this.packageManager = packageManager;
        this.packageIconGetter = iconGetter;
        this.autocryptPeerDao = autocryptPeerDao;
    }

    public List<IdentityInfo> getIdentityInfos(long masterKeyId) {
        ArrayList<IdentityInfo> identities = new ArrayList<>();

        loadUserIds(identities, masterKeyId);
        correlateOrAddAutocryptPeers(identities, masterKeyId);

        return Collections.unmodifiableList(identities);
    }

    private void correlateOrAddAutocryptPeers(ArrayList<IdentityInfo> identities, long masterKeyId) {
        for (AutocryptPeer autocryptPeer : autocryptPeerDao.getAutocryptPeersForKey(masterKeyId)) {
            String packageName = autocryptPeer.package_name();
            String autocryptId = autocryptPeer.identifier();

            Drawable drawable = packageIconGetter.getDrawableForPackageName(packageName);
            Intent autocryptPeerIntent = getAutocryptPeerActivityIntentIfResolvable(packageName, autocryptId);

            UserIdInfo associatedUserIdInfo = findUserIdMatchingAutocryptPeer(identities, autocryptId);
            if (associatedUserIdInfo != null) {
                int position = identities.indexOf(associatedUserIdInfo);
                AutocryptPeerInfo autocryptPeerInfo = AutocryptPeerInfo
                        .create(masterKeyId, associatedUserIdInfo, autocryptId, packageName, drawable, autocryptPeerIntent);
                identities.set(position, autocryptPeerInfo);
            } else {
                AutocryptPeerInfo autocryptPeerInfo = AutocryptPeerInfo
                        .create(masterKeyId, autocryptId, packageName, drawable, autocryptPeerIntent);
                identities.add(autocryptPeerInfo);
            }
        }
    }

    private Intent getAutocryptPeerActivityIntentIfResolvable(String packageName, String autocryptPeer) {
        Intent intent = new Intent();
        intent.setAction("org.autocrypt.PEER_ACTION");
        intent.setPackage(packageName);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(OpenPgpApi.EXTRA_AUTOCRYPT_PEER_ID, autocryptPeer);

        List<ResolveInfo> resolveInfos = packageManager.queryIntentActivities(intent, 0);
        if (resolveInfos != null && !resolveInfos.isEmpty()) {
            return intent;
        } else {
            return null;
        }
    }

    private static UserIdInfo findUserIdMatchingAutocryptPeer(List<IdentityInfo> identities, String autocryptPeer) {
        for (IdentityInfo identityInfo : identities) {
            if (identityInfo instanceof UserIdInfo) {
                UserIdInfo userIdInfo = (UserIdInfo) identityInfo;
                if (autocryptPeer.equals(userIdInfo.getEmail())) {
                    return userIdInfo;
                }
            }
        }
        return null;
    }

    private void loadUserIds(ArrayList<IdentityInfo> identities, long... masterKeyId) {
        SqlDelightQuery query = UserPacket.FACTORY.selectUserIdsByMasterKeyId(masterKeyId);
        try (Cursor cursor = db.query(query)) {
            while (cursor.moveToNext()) {
                UserId userId = UserPacket.USER_ID_MAPPER.map(cursor);

                if (userId.name() != null || userId.email() != null) {
                    IdentityInfo identityInfo = UserIdInfo.create(
                            userId.master_key_id(), userId.rank(), userId.isVerified(), userId.is_primary(), userId.name(), userId.email(), userId.comment());
                    identities.add(identityInfo);
                }
            }
        }
    }

    public interface IdentityInfo {
        long getMasterKeyId();
        int getRank();
        boolean isVerified();
        boolean isPrimary();
    }

    @AutoValue
    public abstract static class UserIdInfo implements IdentityInfo {
        public abstract long getMasterKeyId();
        public abstract int getRank();
        public abstract boolean isVerified();
        public abstract boolean isPrimary();

        @Nullable
        public abstract String getName();
        @Nullable
        public abstract String getEmail();
        @Nullable
        public abstract String getComment();

        static UserIdInfo create(long masterKeyId, int rank, boolean isVerified, boolean isPrimary, String name, String email,
                String comment) {
            return new AutoValue_IdentityDao_UserIdInfo(masterKeyId, rank, isVerified, isPrimary, name, email, comment);
        }
    }

    @AutoValue
    public abstract static class AutocryptPeerInfo implements IdentityInfo {
        public abstract long getMasterKeyId();
        public abstract int getRank();
        public abstract boolean isVerified();
        public abstract boolean isPrimary();

        public abstract String getIdentity();
        public abstract String getPackageName();
        @Nullable
        public abstract Drawable getAppIcon();
        @Nullable
        public abstract UserIdInfo getUserIdInfo();
        @Nullable
        public abstract Intent getAutocryptPeerIntent();

        static AutocryptPeerInfo create(long masterKeyId, UserIdInfo userIdInfo, String autocryptPeer, String packageName,
                Drawable appIcon, Intent autocryptPeerIntent) {
            return new AutoValue_IdentityDao_AutocryptPeerInfo(masterKeyId, userIdInfo.getRank(), userIdInfo.isVerified(),
                    userIdInfo.isPrimary(), autocryptPeer, packageName, appIcon, userIdInfo, autocryptPeerIntent);
        }

        static AutocryptPeerInfo create(long masterKeyId, String autocryptPeer, String packageName, Drawable appIcon, Intent autocryptPeerIntent) {
            return new AutoValue_IdentityDao_AutocryptPeerInfo(masterKeyId,0, false, false, autocryptPeer, packageName, appIcon, null, autocryptPeerIntent);
        }
    }

}
