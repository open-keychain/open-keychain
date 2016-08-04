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

import org.openintents.openpgp.util.OpenPgpUtils.UserId;
import org.sufficientlysecure.keychain.pgp.KeyRing;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing;
import org.sufficientlysecure.keychain.pgp.UncachedPublicKey;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class ImportKeysListEntry implements Serializable, Parcelable {
    private static final long serialVersionUID = -7797972103284992662L;

    private byte[] mEncodedRing;
    private ArrayList<String> mUserIds;
    private HashMap<String, HashSet<String>> mMergedUserIds;
    private ArrayList<Map.Entry<String, HashSet<String>>> mSortedUserIds;

    private String mKeyIdHex;
    private boolean mRevoked;
    private boolean mExpired;
    private Date mDate; // TODO: not displayed
    private String mFingerprintHex;
    private Integer mBitStrength;
    private String mCurveOid;
    private String mAlgorithm;
    private boolean mSecretKey;
    private UserId mPrimaryUserId;
    private String mKeybaseName;
    private String mFbUsername;
    private String mQuery;
    private ArrayList<String> mOrigins;
    private Integer mHashCode = null;

    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeSerializable(mPrimaryUserId);
        dest.writeStringList(mUserIds);
        dest.writeSerializable(mMergedUserIds);
        dest.writeByte((byte) (mRevoked ? 1 : 0));
        dest.writeByte((byte) (mExpired ? 1 : 0));
        dest.writeInt(mDate == null ? 0 : 1);
        if (mDate != null) {
            dest.writeLong(mDate.getTime());
        }
        dest.writeString(mFingerprintHex);
        dest.writeString(mKeyIdHex);
        dest.writeInt(mBitStrength == null ? 0 : 1);
        if (mBitStrength != null) {
            dest.writeInt(mBitStrength);
        }
        dest.writeString(mAlgorithm);
        dest.writeByte((byte) (mSecretKey ? 1 : 0));
        dest.writeString(mKeybaseName);
        dest.writeString(mFbUsername);
        dest.writeStringList(mOrigins);
    }

    public static final Creator<ImportKeysListEntry> CREATOR = new Creator<ImportKeysListEntry>() {
        public ImportKeysListEntry createFromParcel(final Parcel source) {
            ImportKeysListEntry vr = new ImportKeysListEntry();
            vr.mPrimaryUserId = (UserId) source.readSerializable();
            vr.mUserIds = new ArrayList<>();
            source.readStringList(vr.mUserIds);
            vr.mMergedUserIds = (HashMap<String, HashSet<String>>) source.readSerializable();
            vr.mRevoked = source.readByte() == 1;
            vr.mExpired = source.readByte() == 1;
            vr.mDate = source.readInt() != 0 ? new Date(source.readLong()) : null;
            vr.mFingerprintHex = source.readString();
            vr.mKeyIdHex = source.readString();
            vr.mBitStrength = source.readInt() != 0 ? source.readInt() : null;
            vr.mAlgorithm = source.readString();
            vr.mSecretKey = source.readByte() == 1;
            vr.mKeybaseName = source.readString();
            vr.mFbUsername = source.readString();
            vr.mOrigins = new ArrayList<>();
            source.readStringList(vr.mOrigins);

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

    public boolean hasSameKeyAs(ImportKeysListEntry other) {
        if (mFingerprintHex == null || other == null) {
            return false;
        }
        return mFingerprintHex.equals(other.mFingerprintHex);
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

    public boolean isRevokedOrExpired() {
        return mRevoked || mExpired;
    }

    public byte[] getEncodedRing() {
        return mEncodedRing;
    }

    public Date getDate() {
        return mDate;
    }

    public void setDate(Date date) {
        mDate = date;
    }

    public String getFingerprintHex() {
        return mFingerprintHex;
    }

    public void setFingerprintHex(String fingerprintHex) {
        mFingerprintHex = fingerprintHex;
    }

    public void setFingerprint(byte[] fingerprint) {
        mFingerprintHex = KeyFormattingUtils.convertFingerprintToHex(fingerprint);
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

    public String getKeybaseName() {
        return mKeybaseName;
    }

    public String getFbUsername() {
        return mFbUsername;
    }

    public void setKeybaseName(String keybaseName) {
        mKeybaseName = keybaseName;
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

    public ArrayList<String> getOrigins() {
        return mOrigins;
    }

    public void addOrigin(String origin) {
        mOrigins.add(origin);
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
        mOrigins = new ArrayList<>();
    }

    /**
     * Constructor based on key object, used for import from NFC, QR Codes, files
     */
    @SuppressWarnings("unchecked")
    public ImportKeysListEntry(Context context, UncachedKeyRing ring, byte[] encodedRing) {
        mEncodedRing = encodedRing;
        mSecretKey = ring.isSecret();
        UncachedPublicKey key = ring.getPublicKey();

        mHashCode = key.hashCode();

        setPrimaryUserId(key.getPrimaryUserIdWithFallback());
        setKeyId(key.getKeyId());
        setFingerprint(key.getFingerprint());

        // NOTE: Dont use maybe methods for now, they can be wrong.
        mRevoked = false; //key.isMaybeRevoked();
        mExpired = false; //key.isMaybeExpired();

        mBitStrength = key.getBitStrength();
        mCurveOid = key.getCurveOid();
        final int algorithm = key.getAlgorithm();
        mAlgorithm = KeyFormattingUtils.getAlgorithmInfo(context, algorithm, mBitStrength, mCurveOid);

        setUserIds(key.getUnorderedUserIds());
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

}
