/*
 * Copyright (C) 2014 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2014 Vincent Breitmoser <v.breitmoser@mugenguild.com>
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

package org.sufficientlysecure.keychain.service;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import android.os.Parcelable;
import android.support.annotation.CheckResult;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;
import org.sufficientlysecure.keychain.keyimport.ParcelableHkpKeyserver;
import org.sufficientlysecure.keychain.pgp.WrappedUserAttribute;


@AutoValue
public abstract class CertifyActionsParcel implements Parcelable {
    public abstract long getMasterKeyId();
    public abstract ArrayList<CertifyAction> getCertifyActions();
    @Nullable
    public abstract ParcelableHkpKeyserver getParcelableKeyServer();

    public static Builder builder(long masterKeyId) {
        return new AutoValue_CertifyActionsParcel.Builder()
                .setMasterKeyId(masterKeyId)
                .setCertifyActions(new ArrayList<CertifyAction>());
    }

    @AutoValue.Builder
    public abstract static class Builder {
        abstract Builder setMasterKeyId(long masterKeyId);
        public abstract Builder setCertifyActions(ArrayList<CertifyAction> certifyActions);
        public abstract Builder setParcelableKeyServer(ParcelableHkpKeyserver uri);

        abstract ArrayList<CertifyAction> getCertifyActions();

        public void addAction(CertifyAction action) {
            getCertifyActions().add(action);
        }
        public void addActions(Collection<CertifyAction> certifyActions) {
            getCertifyActions().addAll(certifyActions);
        }

        public abstract CertifyActionsParcel build();
    }

    @AutoValue
    public abstract static class CertifyAction implements Parcelable {
        public abstract long getMasterKeyId();
        @Nullable
        public abstract ArrayList<String> getUserIds();
        @Nullable
        public abstract ArrayList<WrappedUserAttribute> getUserAttributes();

        public static CertifyAction createForUserIds(long masterKeyId, List<String> userIds) {
            return new AutoValue_CertifyActionsParcel_CertifyAction(masterKeyId, new ArrayList<>(userIds), null);
        }

        public static CertifyAction createForUserAttributes(long masterKeyId, List<WrappedUserAttribute> attributes) {
            return new AutoValue_CertifyActionsParcel_CertifyAction(masterKeyId, null, new ArrayList<>(attributes));
        }

        @CheckResult
        public CertifyAction withAddedUserIds(ArrayList<String> addedUserIds) {
            if (getUserAttributes() != null) {
                throw new IllegalStateException("Can't add user ids to user attribute certification parcel!");
            }
            ArrayList<String> prevUserIds = getUserIds();
            if (prevUserIds == null) {
                throw new IllegalStateException("Can't add user ids to user attribute certification parcel!");
            }

            ArrayList<String> userIds = new ArrayList<>(prevUserIds);
            userIds.addAll(addedUserIds);
            return new AutoValue_CertifyActionsParcel_CertifyAction(getMasterKeyId(), userIds, null);
        }
    }
}
