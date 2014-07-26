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

    private ArrayList<String> mUserIds;
    private long mKeyId;
    private String mKeyIdHex;
    private boolean mRevoked;
    private boolean mExpired;
    private Date mDate; // TODO: not displayed
    private String mFingerprintHex;
    private int mBitStrength;
    private String mAlgorithm;
    private boolean mSecretKey;
    private String mPrimaryUserId;
    private String mExtraData;
    private String mQuery;
    private String mOrigin;
    private Integer mHashCode = null;

    private boolean mSelected;

    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mPrimaryUserId);
        dest.writeStringList(mUserIds);
        dest.writeLong(mKeyId);
        dest.writeByte((byte) (mRevoked ? 1 : 0));
        dest.writeByte((byte) (mExpired ? 1 : 0));
        dest.writeSerializable(mDate);
        dest.writeString(mFingerprintHex);
        dest.writeString(mKeyIdHex);
        dest.writeInt(mBitStrength);
        dest.writeString(mAlgorithm);
        dest.writeByte((byte) (mSecretKey ? 1 : 0));
        dest.writeByte((byte) (mSelected ? 1 : 0));
        dest.writeString(mExtraData);
        dest.writeString(mOrigin);
    }

    public static final Creator<ImportKeysListEntry> CREATOR = new Creator<ImportKeysListEntry>() {
        public ImportKeysListEntry createFromParcel(final Parcel source) {
            ImportKeysListEntry vr = new ImportKeysListEntry();
            vr.mPrimaryUserId = source.readString();
            vr.mUserIds = new ArrayList<String>();
            source.readStringList(vr.mUserIds);
            vr.mKeyId = source.readLong();
            vr.mRevoked = source.readByte() == 1;
            vr.mExpired = source.readByte() == 1;
            vr.mDate = (Date) source.readSerializable();
            vr.mFingerprintHex = source.readString();
            vr.mKeyIdHex = source.readString();
            vr.mBitStrength = source.readInt();
            vr.mAlgorithm = source.readString();
            vr.mSecretKey = source.readByte() == 1;
            vr.mSelected = source.readByte() == 1;
            vr.mExtraData = source.readString();
            vr.mOrigin = source.readString();

            return vr;
        }

        public ImportKeysListEntry[] newArray(final int size) {
            return new ImportKeysListEntry[size];
        }
    };

    public int hashCode() {
        if (mHashCode != null) {
            return mHashCode;
        }
        return super.hashCode();
    }

    public String getKeyIdHex() {
        return mKeyIdHex;
    }

    public boolean isSelected() {
        return mSelected;
    }

    public void setSelected(boolean selected) {
        this.mSelected = selected;
    }

    public boolean isExpired() {
        return mExpired;
    }

    public void setExpired(boolean expired) {
        this.mExpired = expired;
    }

    public long getKeyId() {
        return mKeyId;
    }

    public void setKeyId(long keyId) {
        this.mKeyId = keyId;
    }

    public void setKeyIdHex(String keyIdHex) {
        this.mKeyIdHex = keyIdHex;
    }

    public boolean isRevoked() {
        return mRevoked;
    }

    public void setRevoked(boolean revoked) {
        this.mRevoked = revoked;
    }

    public Date getDate() {
        return mDate;
    }

    public void setDate(Date date) {
        this.mDate = date;
    }

    public String getFingerprintHex() {
        return mFingerprintHex;
    }

    public void setFingerprintHex(String fingerprintHex) {
        this.mFingerprintHex = fingerprintHex;
    }

    public int getBitStrength() {
        return mBitStrength;
    }

    public void setBitStrength(int bitStrength) {
        this.mBitStrength = bitStrength;
    }

    public String getAlgorithm() {
        return mAlgorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.mAlgorithm = algorithm;
    }

    public boolean isSecretKey() {
        return mSecretKey;
    }

    public void setSecretKey(boolean secretKey) {
        this.mSecretKey = secretKey;
    }

    public ArrayList<String> getUserIds() {
        return mUserIds;
    }

    public void setUserIds(ArrayList<String> userIds) {
        this.mUserIds = userIds;
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
        mSecretKey = false;
        // do not select by default
        mSelected = false;
        mUserIds = new ArrayList<String>();
    }

    /**
     * Constructor based on key object, used for import from NFC, QR Codes, files
     */
    @SuppressWarnings("unchecked")
    public ImportKeysListEntry(Context context, UncachedKeyRing ring) {
        // selected is default
        this.mSelected = true;

        mSecretKey = ring.isSecret();
        UncachedPublicKey key = ring.getPublicKey();

        mHashCode = key.hashCode();

        mPrimaryUserId = key.getPrimaryUserIdWithFallback();
        mUserIds = key.getUnorderedUserIds();

        // if there was no user id flagged as primary, use the first one
        if (mPrimaryUserId == null) {
            mPrimaryUserId = mUserIds.get(0);
        }

        this.mKeyId = key.getKeyId();
        this.mKeyIdHex = PgpKeyHelper.convertKeyIdToHex(mKeyId);

        this.mRevoked = key.isRevoked();
        this.mFingerprintHex = PgpKeyHelper.convertFingerprintToHex(key.getFingerprint());
        this.mBitStrength = key.getBitStrength();
        final int algorithm = key.getAlgorithm();
        this.mAlgorithm = PgpKeyHelper.getAlgorithmInfo(context, algorithm);
    }
}
