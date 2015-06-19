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
    private CanonicalizedSecretKey.SecretKeyType mSecretKeyType;
    private Passphrase mPassphrase;

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
    }

    protected WizardModel(Parcel in) {
        this.mName = in.readString();
        this.mEmail = in.readString();
        this.mAdditionalEmails = in.createStringArrayList();
        int tmpMSecretKeyType = in.readInt();
        this.mSecretKeyType = tmpMSecretKeyType == -1 ? null : CanonicalizedSecretKey.SecretKeyType.values()[tmpMSecretKeyType];
        this.mPassphrase = in.readParcelable(Passphrase.class.getClassLoader());
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
