package org.sufficientlysecure.keychain.ui.keyunlock.Model;

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
    private Passphrase mPassword;
    private CanonicalizedSecretKey.SecretKeyType mSecretKeyType;

    public String getName() {
        return mName;
    }

    public void setName(String mName) {
        this.mName = mName;
    }

    public String getEmail() {
        return mEmail;
    }

    public void setEmail(String mEmail) {
        this.mEmail = mEmail;
    }

    public ArrayList<String> getAdditionalEmails() {
        return mAdditionalEmails;
    }

    public void setAdditionalEmails(ArrayList<String> mAdditionalEmails) {
        this.mAdditionalEmails = mAdditionalEmails;
    }

    public Passphrase getPassword() {
        return mPassword;
    }

    public void setPassword(Passphrase mPassword) {
        this.mPassword = mPassword;
    }

    public CanonicalizedSecretKey.SecretKeyType getSecretKeyType() {
        return mSecretKeyType;
    }

    public void setSecretKeyType(CanonicalizedSecretKey.SecretKeyType secretKeyType) {
        mSecretKeyType = secretKeyType;
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
        dest.writeParcelable(this.mPassword, 0);
        dest.writeInt(this.mSecretKeyType == null ? -1 : this.mSecretKeyType.ordinal());
    }

    protected WizardModel(Parcel in) {
        this.mName = in.readString();
        this.mEmail = in.readString();
        this.mAdditionalEmails = in.createStringArrayList();
        this.mPassword = in.readParcelable(Passphrase.class.getClassLoader());
        int tmpMSecretKeyType = in.readInt();
        this.mSecretKeyType = tmpMSecretKeyType == -1 ? null : CanonicalizedSecretKey.SecretKeyType.values()[tmpMSecretKeyType];
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
