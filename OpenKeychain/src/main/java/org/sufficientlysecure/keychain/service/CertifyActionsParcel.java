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

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.util.Map;

import org.sufficientlysecure.keychain.pgp.WrappedUserAttribute;
import org.sufficientlysecure.keychain.util.ParcelableProxy;


/**
 * This class is a a transferable representation for a number of keyrings to
 * be certified.
 */
public class CertifyActionsParcel implements Parcelable {

    // the master key id to certify with
    final public long mMasterKeyId;
    public CertifyLevel mLevel;

    public ArrayList<CertifyAction> mCertifyActions = new ArrayList<>();

    public String keyServerUri;

    public CertifyActionsParcel(long masterKeyId) {
        mMasterKeyId = masterKeyId;
        mLevel = CertifyLevel.DEFAULT;
    }

    public CertifyActionsParcel(Parcel source) {
        mMasterKeyId = source.readLong();
        // just like parcelables, this is meant for ad-hoc IPC only and is NOT portable!
        mLevel = CertifyLevel.values()[source.readInt()];
        keyServerUri = source.readString();

        mCertifyActions = (ArrayList<CertifyAction>) source.readSerializable();
    }

    public void add(CertifyAction action) {
        mCertifyActions.add(action);
    }

    @Override
    public void writeToParcel(Parcel destination, int flags) {
        destination.writeLong(mMasterKeyId);
        destination.writeInt(mLevel.ordinal());
        destination.writeString(keyServerUri);

        destination.writeSerializable(mCertifyActions);
    }

    public static final Creator<CertifyActionsParcel> CREATOR = new Creator<CertifyActionsParcel>() {
        public CertifyActionsParcel createFromParcel(final Parcel source) {
            return new CertifyActionsParcel(source);
        }

        public CertifyActionsParcel[] newArray(final int size) {
            return new CertifyActionsParcel[size];
        }
    };

    // TODO make this parcelable
    public static class CertifyAction implements Serializable {
        final public long mMasterKeyId;

        final public ArrayList<String> mUserIds;
        final public ArrayList<WrappedUserAttribute> mUserAttributes;

        public CertifyAction(long masterKeyId, List<String> userIds, List<WrappedUserAttribute> attributes) {
            mMasterKeyId = masterKeyId;
            mUserIds = userIds == null ? null : new ArrayList<>(userIds);
            mUserAttributes = attributes == null ? null : new ArrayList<>(attributes);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public String toString() {
        String out = "mMasterKeyId: " + mMasterKeyId + "\n";
        out += "mLevel: " + mLevel + "\n";
        out += "mCertifyActions: " + mCertifyActions + "\n";

        return out;
    }

    // All supported algorithms
    public enum CertifyLevel {
        DEFAULT, NONE, CASUAL, POSITIVE
    }

}
