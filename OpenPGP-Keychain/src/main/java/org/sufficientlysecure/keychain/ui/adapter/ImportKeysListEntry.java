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

package org.sufficientlysecure.keychain.ui.adapter;

import android.os.Parcel;
import android.os.Parcelable;
import org.spongycastle.openpgp.PGPKeyRing;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPSecretKeyRing;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.pgp.PgpKeyHelper;
import org.sufficientlysecure.keychain.util.IterableIterator;
import org.sufficientlysecure.keychain.util.Log;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;

public class ImportKeysListEntry implements Serializable, Parcelable {
    private static final long serialVersionUID = -7797972103284992662L;
    public ArrayList<String> userIds;

    public long keyId;
    public boolean revoked;
    public Date date; // TODO: not displayed
    public String fingerPrint;
    public String hexKeyId;
    public int bitStrength;
    public String algorithm;
    public boolean secretKey;

    private boolean selected;

    private byte[] bytes = new byte[]{};

    public ImportKeysListEntry(ImportKeysListEntry b) {
        this.userIds = b.userIds;
        this.keyId = b.keyId;
        this.revoked = b.revoked;
        this.date = b.date;
        this.fingerPrint = b.fingerPrint;
        this.hexKeyId = b.hexKeyId;
        this.bitStrength = b.bitStrength;
        this.algorithm = b.algorithm;
        this.secretKey = b.secretKey;
        this.selected = b.selected;
        this.bytes = b.bytes;
    }

    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStringList(userIds);
        dest.writeLong(keyId);
        dest.writeByte((byte) (revoked ? 1 : 0));
        dest.writeSerializable(date);
        dest.writeString(fingerPrint);
        dest.writeString(hexKeyId);
        dest.writeInt(bitStrength);
        dest.writeString(algorithm);
        dest.writeByte((byte) (secretKey ? 1 : 0));
        dest.writeByte((byte) (selected ? 1 : 0));
        dest.writeInt(bytes.length);
        dest.writeByteArray(bytes);
    }

    public static final Creator<ImportKeysListEntry> CREATOR = new Creator<ImportKeysListEntry>() {
        public ImportKeysListEntry createFromParcel(final Parcel source) {
            ImportKeysListEntry vr = new ImportKeysListEntry();
            vr.userIds = new ArrayList<String>();
            source.readStringList(vr.userIds);
            vr.keyId = source.readLong();
            vr.revoked = source.readByte() == 1;
            vr.date = (Date) source.readSerializable();
            vr.fingerPrint = source.readString();
            vr.hexKeyId = source.readString();
            vr.bitStrength = source.readInt();
            vr.algorithm = source.readString();
            vr.secretKey = source.readByte() == 1;
            vr.selected = source.readByte() == 1;
            vr.bytes = new byte[source.readInt()];
            source.readByteArray(vr.bytes);

            return vr;
        }

        public ImportKeysListEntry[] newArray(final int size) {
            return new ImportKeysListEntry[size];
        }
    };

    public long getKeyId() {
        return keyId;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    /**
     * Constructor for later querying from keyserver
     */
    public ImportKeysListEntry() {
        // keys from keyserver are always public keys
        secretKey = false;
        // do not select by default
        selected = false;
        userIds = new ArrayList<String>();
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    /**
     * Constructor based on key object, used for import from NFC, QR Codes, files
     */
    @SuppressWarnings("unchecked")
    public ImportKeysListEntry(PGPKeyRing pgpKeyRing) {
        // save actual key object into entry, used to import it later
        try {
            this.bytes = pgpKeyRing.getEncoded();
        } catch (IOException e) {
            Log.e(Constants.TAG, "IOException on pgpKeyRing.getEncoded()", e);
        }

        // selected is default
        this.selected = true;

        if (pgpKeyRing instanceof PGPSecretKeyRing) {
            secretKey = true;
        } else {
            secretKey = false;
        }

        userIds = new ArrayList<String>();
        for (String userId : new IterableIterator<String>(pgpKeyRing.getPublicKey().getUserIDs())) {
            userIds.add(userId);
        }
        this.keyId = pgpKeyRing.getPublicKey().getKeyID();

        this.revoked = pgpKeyRing.getPublicKey().isRevoked();
        this.fingerPrint = PgpKeyHelper.convertFingerprintToHex(pgpKeyRing.getPublicKey()
                .getFingerprint(), true);
        this.hexKeyId = PgpKeyHelper.convertKeyIdToHex(keyId);
        this.bitStrength = pgpKeyRing.getPublicKey().getBitStrength();
        int algorithm = pgpKeyRing.getPublicKey().getAlgorithm();
        if (algorithm == PGPPublicKey.RSA_ENCRYPT || algorithm == PGPPublicKey.RSA_GENERAL
                || algorithm == PGPPublicKey.RSA_SIGN) {
            this.algorithm = "RSA";
        } else if (algorithm == PGPPublicKey.DSA) {
            this.algorithm = "DSA";
        } else if (algorithm == PGPPublicKey.ELGAMAL_ENCRYPT
                || algorithm == PGPPublicKey.ELGAMAL_GENERAL) {
            this.algorithm = "ElGamal";
        } else if (algorithm == PGPPublicKey.EC || algorithm == PGPPublicKey.ECDSA) {
            this.algorithm = "ECC";
        } else {
            // TODO: with resources
            this.algorithm = "unknown";
        }
    }
}