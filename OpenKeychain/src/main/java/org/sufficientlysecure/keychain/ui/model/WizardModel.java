/*
 * Copyright (C) 2015 Dominik Schürmann <dominik@dominikschuermann.de>
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
package org.sufficientlysecure.keychain.ui.model;

import android.os.Parcel;
import android.os.Parcelable;

import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey;
import org.sufficientlysecure.keychain.util.Passphrase;

import java.util.ArrayList;

/**
 * Wizard Data Model
 */
public class WizardModel implements Parcelable {
    private String mName;
    private String mEmail;
    private ArrayList<String> mAdditionalEmails;
    private CanonicalizedSecretKey.SecretKeyType mSecretKeyType;
    private Passphrase mPassphrase;
    private boolean mFirstTime = true;
    private boolean mCreateYubiKey = false;
    private Passphrase mYubiKeyPin;
    private Passphrase mYubiKeyAdminPin;

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public String getEmail() {
        return mEmail;
    }

    public void setEmail(String email) {
        mEmail = email;
    }

    public ArrayList<String> getAdditionalEmails() {
        return mAdditionalEmails;
    }

    public void setAdditionalEmails(ArrayList<String> additionalEmails) {
        mAdditionalEmails = additionalEmails;
    }

    public Passphrase getPassphrase() {
        return mPassphrase;
    }

    public void setPassphrase(Passphrase password) {
        this.mPassphrase = password;
    }

    public CanonicalizedSecretKey.SecretKeyType getSecretKeyType() {
        return mSecretKeyType;
    }

    public void setSecretKeyType(CanonicalizedSecretKey.SecretKeyType secretKeyType) {
        mSecretKeyType = secretKeyType;
    }

    public boolean isFirstTime() {
        return mFirstTime;
    }

    public void setFirstTime(boolean firstTime) {
        mFirstTime = firstTime;
    }

    public boolean isCreateYubiKey() {
        return mCreateYubiKey;
    }

    public void setCreateYubiKey(boolean createYubiKey) {
        mCreateYubiKey = createYubiKey;
    }

    public Passphrase getYubiKeyPin() {
        return mYubiKeyPin;
    }

    public void setYubiKeyPin(Passphrase yubiKeyPin) {
        mYubiKeyPin = yubiKeyPin;
    }

    public Passphrase getYubiKeyAdminPin() {
        return mYubiKeyAdminPin;
    }

    public void setYubiKeyAdminPin(Passphrase yubiKeyAdminPin) {
        mYubiKeyAdminPin = yubiKeyAdminPin;
    }

    public WizardModel() {

    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.mName);
        dest.writeString(this.mEmail);
        dest.writeStringList(this.mAdditionalEmails);
        dest.writeInt(this.mSecretKeyType == null ? -1 : this.mSecretKeyType.ordinal());
        dest.writeParcelable(this.mPassphrase, 0);
        dest.writeByte(mFirstTime ? (byte) 1 : (byte) 0);
        dest.writeByte(mCreateYubiKey ? (byte) 1 : (byte) 0);
        dest.writeParcelable(this.mYubiKeyPin, 0);
        dest.writeParcelable(this.mYubiKeyAdminPin, 0);
    }

    protected WizardModel(Parcel in) {
        this.mName = in.readString();
        this.mEmail = in.readString();
        this.mAdditionalEmails = in.createStringArrayList();
        int tmpMSecretKeyType = in.readInt();
        this.mSecretKeyType = tmpMSecretKeyType == -1 ? null : CanonicalizedSecretKey.SecretKeyType.values()[tmpMSecretKeyType];
        this.mPassphrase = in.readParcelable(Passphrase.class.getClassLoader());
        this.mFirstTime = in.readByte() != 0;
        this.mCreateYubiKey = in.readByte() != 0;
        this.mYubiKeyPin = in.readParcelable(Passphrase.class.getClassLoader());
        this.mYubiKeyAdminPin = in.readParcelable(Passphrase.class.getClassLoader());
    }

    public static final Creator<WizardModel> CREATOR = new Creator<WizardModel>() {
        public WizardModel createFromParcel(Parcel source) {
            return new WizardModel(source);
        }

        public WizardModel[] newArray(int size) {
            return new WizardModel[size];
        }
    };
}
