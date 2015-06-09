package org.sufficientlysecure.keychain.ui.keyunlock.Model;

import android.os.Parcel;
import android.os.Parcelable;

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
    }

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

    public WizardModel() {
    }

    protected WizardModel(Parcel in) {
        this.mName = in.readString();
        this.mEmail = in.readString();
        this.mAdditionalEmails = in.createStringArrayList();
        this.mPassword = in.readParcelable(Passphrase.class.getClassLoader());
    }

    public static final Parcelable.Creator<WizardModel> CREATOR = new Parcelable.
            Creator<WizardModel>() {
        public WizardModel createFromParcel(Parcel source) {
            return new WizardModel(source);
        }

        public WizardModel[] newArray(int size) {
            return new WizardModel[size];
        }
    };
}
