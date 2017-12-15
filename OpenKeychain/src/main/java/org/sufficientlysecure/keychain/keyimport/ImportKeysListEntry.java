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

package org.sufficientlysecure.keychain.keyimport;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import org.openintents.openpgp.util.OpenPgpUtils.UserId;
import org.sufficientlysecure.keychain.pgp.KeyRing;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing;
import org.sufficientlysecure.keychain.pgp.UncachedPublicKey;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class ImportKeysListEntry implements Serializable, Parcelable {
    private static final long serialVersionUID = -7797972103284992662L;

    private ParcelableKeyRing mParcelableKeyRing;

    private ArrayList<String> mUserIds;
    private HashMap<String, HashSet<String>> mMergedUserIds;
    private ArrayList<Map.Entry<String, HashSet<String>>> mSortedUserIds;

    private String mKeyIdHex;

    private boolean mSecretKey;
    private boolean mRevoked;
    private boolean mExpired;
    private boolean mSecure;
    private boolean mUpdated;

    private Date mDate;
    private byte[] mFingerprint;
    private Integer mBitStrength;
    private String mCurveOid;
    private String mAlgorithm;

    private UserId mPrimaryUserId;
    private HkpKeyserverAddress mKeyserver;
    private String mKeybaseName;
    private String mFbUsername;

    private String mQuery;
    private Integer mHashCode = null;

    public ParcelableKeyRing getParcelableKeyRing() {
        return mParcelableKeyRing;
    }

    public void setParcelableKeyRing(ParcelableKeyRing parcelableKeyRing) {
        this.mParcelableKeyRing = parcelableKeyRing;
    }

    public boolean hasSameKeyAs(ImportKeysListEntry other) {
        if (mFingerprint == null || other == null || other.mFingerprint == null) {
            return false;
        }
        return Arrays.equals(mFingerprint, other.mFingerprint);
    }

    public String getKeyIdHex() {
        return mKeyIdHex;
    }

    public void setKeyIdHex(String keyIdHex) {
        mKeyIdHex = keyIdHex;
    }

    public void setKeyId(long keyId) {
        mKeyIdHex = KeyFormattingUtils.convertKeyIdToHex(keyId);
    }

    public boolean isExpired() {
        return mExpired;
    }

    public void setExpired(boolean expired) {
        mExpired = expired;
    }

    public boolean isRevoked() {
        return mRevoked;
    }

    public void setRevoked(boolean revoked) {
        mRevoked = revoked;
    }

    public boolean isSecure() {
        return mSecure;
    }

    public void setSecure(boolean secure) {
        mSecure = secure;
    }

    public boolean isRevokedOrExpiredOrInsecure() {
        return mRevoked || mExpired || !mSecure;
    }

    public Date getDate() {
        return mDate;
    }

    public boolean isUpdated() {
        return mUpdated;
    }

    public void setUpdated(boolean updated) {
        mUpdated = updated;
    }

    public void setDate(Date date) {
        mDate = date;
    }

    public void setFingerprint(byte[] fingerprint) {
        mFingerprint = fingerprint;
    }

    public byte[] getFingerprint() {
        return mFingerprint;
    }

    public Integer getBitStrength() {
        return mBitStrength;
    }

    public String getCurveOid() {
        return mCurveOid;
    }

    public void setBitStrength(int bitStrength) {
        mBitStrength = bitStrength;
    }

    public String getAlgorithm() {
        return mAlgorithm;
    }

    public void setAlgorithm(String algorithm) {
        mAlgorithm = algorithm;
    }

    public boolean isSecretKey() {
        return mSecretKey;
    }

    public void setSecretKey(boolean secretKey) {
        mSecretKey = secretKey;
    }

    public UserId getPrimaryUserId() {
        return mPrimaryUserId;
    }

    public void setPrimaryUserId(String userId) {
        mPrimaryUserId = KeyRing.splitUserId(userId);
    }

    public void setPrimaryUserId(UserId primaryUserId) {
        mPrimaryUserId = primaryUserId;
    }

    public HkpKeyserverAddress getKeyserver() {
        return mKeyserver;
    }

    public void setKeyserver(HkpKeyserverAddress keyserver) {
        mKeyserver = keyserver;
    }

    public String getKeybaseName() {
        return mKeybaseName;
    }

    public void setKeybaseName(String keybaseName) {
        mKeybaseName = keybaseName;
    }

    public String getFbUsername() {
        return mFbUsername;
    }

    public void setFbUsername(String fbUsername) {
        mFbUsername = fbUsername;
    }

    public String getQuery() {
        return mQuery;
    }

    public void setQuery(String query) {
        mQuery = query;
    }

    public int hashCode() {
        return mHashCode != null ? mHashCode : super.hashCode();
    }

    public List<String> getUserIds() {
        // To ensure choerency, use methods of this class to edit the list
        return Collections.unmodifiableList(mUserIds);
    }

    public ArrayList<Map.Entry<String, HashSet<String>>> getSortedUserIds() {
        if (mSortedUserIds == null)
            sortMergedUserIds();

        return mSortedUserIds;
    }

    public void setUserIds(ArrayList<String> userIds) {
        mUserIds = userIds;
        updateMergedUserIds();
    }

    public boolean addUserIds(List<String> userIds) {
        boolean modified = false;
        for (String uid : userIds) {
            if (!mUserIds.contains(uid)) {
                mUserIds.add(uid);
                modified = true;
            }
        }

        if (modified)
            updateMergedUserIds();

        return modified;
    }

    public ArrayList<String> getKeybaseUserIds() {
        ArrayList<String> keybaseUserIds = new ArrayList<>();
        for (String s : mUserIds) {
            if (s.contains(":"))
                keybaseUserIds.add(s);
        }
        return keybaseUserIds;
    }

    /**
     * Constructor for later querying from keyserver
     */
    public ImportKeysListEntry() {
        // keys from keyserver are always public keys; from keybase too
        mSecretKey = false;

        mUserIds = new ArrayList<>();
    }

    /**
     * Constructor based on key object, used for import from NFC, QR Codes, files
     */
    public ImportKeysListEntry(Context ctx, UncachedKeyRing ring) {
        mSecretKey = ring.isSecret();

        UncachedPublicKey key = ring.getPublicKey();
        setPrimaryUserId(key.getPrimaryUserIdWithFallback());
        setKeyId(key.getKeyId());
        setFingerprint(key.getFingerprint());

        // NOTE: Dont use maybe methods for now, they can be wrong.
        mRevoked = false; //key.isMaybeRevoked();
        mExpired = false; //key.isMaybeExpired();
        mSecure = true;

        mBitStrength = key.getBitStrength();
        mCurveOid = key.getCurveOid();
        int algorithm = key.getAlgorithm();
        mAlgorithm = KeyFormattingUtils.getAlgorithmInfo(ctx, algorithm, mBitStrength, mCurveOid);
        mHashCode = key.hashCode();

        setUserIds(key.getUnorderedUserIds());

        try {
            byte[] encoded = ring.getEncoded();
            mParcelableKeyRing = ParcelableKeyRing.createFromEncodedBytes(encoded);
        } catch (IOException ignored) {
        }
    }

    private void updateMergedUserIds() {
        mMergedUserIds = new HashMap<>();
        for (String userId : mUserIds) {
            UserId userIdSplit = KeyRing.splitUserId(userId);

            // TODO: comment field?

            if (userIdSplit.name != null) {
                if (userIdSplit.email != null) {
                    if (!mMergedUserIds.containsKey(userIdSplit.name)) {
                        HashSet<String> emails = new HashSet<>();
                        emails.add(userIdSplit.email);
                        mMergedUserIds.put(userIdSplit.name, emails);
                    } else {
                        mMergedUserIds.get(userIdSplit.name).add(userIdSplit.email);
                    }
                } else {
                    // name only
                    mMergedUserIds.put(userIdSplit.name, new HashSet<String>());
                }
            } else {
                // fallback
                mMergedUserIds.put(userId, new HashSet<String>());
            }
        }

        mSortedUserIds = null;
    }

    private void sortMergedUserIds() {
        mSortedUserIds = new ArrayList<>(mMergedUserIds.entrySet());

        Collections.sort(mSortedUserIds, new Comparator<Map.Entry<String, HashSet<String>>>() {
            @Override
            public int compare(Map.Entry<String, HashSet<String>> entry1,
                               Map.Entry<String, HashSet<String>> entry2) {

                // sort keybase UserIds after non-Keybase
                boolean e1IsKeybase = entry1.getKey().contains(":");
                boolean e2IsKeybase = entry2.getKey().contains(":");
                if (e1IsKeybase != e2IsKeybase) {
                    return (e1IsKeybase) ? 1 : -1;
                }
                return entry1.getKey().compareTo(entry2.getKey());
            }
        });
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(mParcelableKeyRing, flags);
        dest.writeSerializable(mPrimaryUserId);
        dest.writeStringList(mUserIds);
        dest.writeSerializable(mMergedUserIds);
        dest.writeByte((byte) (mRevoked ? 1 : 0));
        dest.writeByte((byte) (mExpired ? 1 : 0));
        dest.writeByte((byte) (mUpdated ? 1 : 0));
        dest.writeInt(mDate == null ? 0 : 1);
        if (mDate != null) {
            dest.writeLong(mDate.getTime());
        }
        dest.writeByteArray(mFingerprint);
        dest.writeString(mKeyIdHex);
        dest.writeInt(mBitStrength == null ? 0 : 1);
        if (mBitStrength != null) {
            dest.writeInt(mBitStrength);
        }
        dest.writeString(mAlgorithm);
        dest.writeByte((byte) (mSecretKey ? 1 : 0));
        dest.writeParcelable(mKeyserver, flags);
        dest.writeString(mKeybaseName);
        dest.writeString(mFbUsername);
    }

    public static final Creator<ImportKeysListEntry> CREATOR = new Creator<ImportKeysListEntry>() {
        public ImportKeysListEntry createFromParcel(final Parcel source) {
            ImportKeysListEntry vr = new ImportKeysListEntry();

            vr.mParcelableKeyRing = source.readParcelable(ParcelableKeyRing.class.getClassLoader());
            vr.mPrimaryUserId = (UserId) source.readSerializable();
            vr.mUserIds = new ArrayList<>();
            source.readStringList(vr.mUserIds);
            vr.mMergedUserIds = (HashMap<String, HashSet<String>>) source.readSerializable();
            vr.mRevoked = source.readByte() == 1;
            vr.mExpired = source.readByte() == 1;
            vr.mUpdated = source.readByte() == 1;
            vr.mDate = source.readInt() != 0 ? new Date(source.readLong()) : null;
            vr.mFingerprint = source.createByteArray();
            vr.mKeyIdHex = source.readString();
            vr.mBitStrength = source.readInt() != 0 ? source.readInt() : null;
            vr.mAlgorithm = source.readString();
            vr.mSecretKey = source.readByte() == 1;
            vr.mKeyserver = source.readParcelable(HkpKeyserverAddress.class.getClassLoader());
            vr.mKeybaseName = source.readString();
            vr.mFbUsername = source.readString();

            return vr;
        }

        public ImportKeysListEntry[] newArray(final int size) {
            return new ImportKeysListEntry[size];
        }
    };

    public int describeContents() {
        return 0;
    }

}
