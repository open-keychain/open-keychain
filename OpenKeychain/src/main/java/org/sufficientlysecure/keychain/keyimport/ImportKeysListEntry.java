/*
 * Copyright (C) 2013 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.sufficientlysecure.keychain.keyimport;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import org.sufficientlysecure.keychain.pgp.PgpKeyHelper;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing;
import org.sufficientlysecure.keychain.pgp.UncachedPublicKey;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;

public class ImportKeysListEntry implements Serializable, Parcelable {
    private static final long serialVersionUID = -7797972103284992662L;

    public ArrayList<String> userIds;
    public long keyId;
    public String keyIdHex;
    public boolean revoked;
    public boolean expired;
    public Date date; // TODO: not displayed
    public String fingerprintHex;
    public int bitStrength;
    public String algorithm;
    public boolean secretKey;
    public String mPrimaryUserId;
    private String mExtraData;
    private String mQuery;
    private String mOrigin;

    private boolean mSelected;

    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mPrimaryUserId);
        dest.writeStringList(userIds);
        dest.writeLong(keyId);
        dest.writeByte((byte) (revoked ? 1 : 0));
        dest.writeByte((byte) (expired ? 1 : 0));
        dest.writeSerializable(date);
        dest.writeString(fingerprintHex);
        dest.writeString(keyIdHex);
        dest.writeInt(bitStrength);
        dest.writeString(algorithm);
        dest.writeByte((byte) (secretKey ? 1 : 0));
        dest.writeByte((byte) (mSelected ? 1 : 0));
        dest.writeString(mExtraData);
        dest.writeString(mOrigin);
    }

    public static final Creator<ImportKeysListEntry> CREATOR = new Creator<ImportKeysListEntry>() {
        public ImportKeysListEntry createFromParcel(final Parcel source) {
            ImportKeysListEntry vr = new ImportKeysListEntry();
            vr.mPrimaryUserId = source.readString();
            vr.userIds = new ArrayList<String>();
            source.readStringList(vr.userIds);
            vr.keyId = source.readLong();
            vr.revoked = source.readByte() == 1;
            vr.expired = source.readByte() == 1;
            vr.date = (Date) source.readSerializable();
            vr.fingerprintHex = source.readString();
            vr.keyIdHex = source.readString();
            vr.bitStrength = source.readInt();
            vr.algorithm = source.readString();
            vr.secretKey = source.readByte() == 1;
            vr.mSelected = source.readByte() == 1;
            vr.mExtraData = source.readString();
            vr.mOrigin = source.readString();

            return vr;
        }

        public ImportKeysListEntry[] newArray(final int size) {
            return new ImportKeysListEntry[size];
        }
    };

    public String getKeyIdHex() {
        return keyIdHex;
    }

    public boolean isSelected() {
        return mSelected;
    }

    public void setSelected(boolean selected) {
        this.mSelected = selected;
    }

    public boolean isExpired() {
        return expired;
    }

    public void setExpired(boolean expired) {
        this.expired = expired;
    }

    public long getKeyId() {
        return keyId;
    }

    public void setKeyId(long keyId) {
        this.keyId = keyId;
    }

    public void setKeyIdHex(String keyIdHex) {
        this.keyIdHex = keyIdHex;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public void setRevoked(boolean revoked) {
        this.revoked = revoked;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getFingerprintHex() {
        return fingerprintHex;
    }

    public void setFingerprintHex(String fingerprintHex) {
        this.fingerprintHex = fingerprintHex;
    }

    public int getBitStrength() {
        return bitStrength;
    }

    public void setBitStrength(int bitStrength) {
        this.bitStrength = bitStrength;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public boolean isSecretKey() {
        return secretKey;
    }

    public void setSecretKey(boolean secretKey) {
        this.secretKey = secretKey;
    }

    public ArrayList<String> getUserIds() {
        return userIds;
    }

    public void setUserIds(ArrayList<String> userIds) {
        this.userIds = userIds;
    }

    public String getPrimaryUserId() {
        return mPrimaryUserId;
    }

    public void setPrimaryUserId(String uid) {
        mPrimaryUserId = uid;
    }

    public String getExtraData() {
        return mExtraData;
    }

    public void setExtraData(String extraData) {
        mExtraData = extraData;
    }

    public String getQuery() {
        return mQuery;
    }

    public void setQuery(String query) {
        mQuery = query;
    }

    public String getOrigin() {
        return mOrigin;
    }

    public void setOrigin(String origin) {
        mOrigin = origin;
    }

    /**
     * Constructor for later querying from keyserver
     */
    public ImportKeysListEntry() {
        // keys from keyserver are always public keys; from keybase too
        secretKey = false;
        // do not select by default
        mSelected = false;
        userIds = new ArrayList<String>();
    }

    /**
     * Constructor based on key object, used for import from NFC, QR Codes, files
     */
    @SuppressWarnings("unchecked")
    public ImportKeysListEntry(Context context, UncachedKeyRing ring) {
        // selected is default
        this.mSelected = true;

        secretKey = ring.isSecret();
        UncachedPublicKey key = ring.getPublicKey();

        mPrimaryUserId = key.getPrimaryUserId();
        userIds = key.getUnorderedUserIds();

        // if there was no user id flagged as primary, use the first one
        if (mPrimaryUserId == null) {
            mPrimaryUserId = userIds.get(0);
        }

        this.keyId = key.getKeyId();
        this.keyIdHex = PgpKeyHelper.convertKeyIdToHex(keyId);

        this.revoked = key.maybeRevoked();
        this.fingerprintHex = PgpKeyHelper.convertFingerprintToHex(key.getFingerprint());
        this.bitStrength = key.getBitStrength();
        final int algorithm = key.getAlgorithm();
        this.algorithm = PgpKeyHelper.getAlgorithmInfo(context, algorithm);
    }
}
