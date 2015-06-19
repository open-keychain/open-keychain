package org.sufficientlysecure.keychain.ui.keyunlock.wizard;


import android.content.Context;
import android.os.Bundle;

import org.spongycastle.bcpg.sig.KeyFlags;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey;
import org.sufficientlysecure.keychain.pgp.KeyRing;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel;
import org.sufficientlysecure.keychain.ui.keyunlock.base.BaseViewModel;
import org.sufficientlysecure.keychain.util.Passphrase;

import java.util.ArrayList;
import java.util.Iterator;

public class WizardConfirmationFragmentViewModel implements BaseViewModel {
    private SaveKeyringParcel mSaveKeyringParcel;
    private boolean mUseSmartCardSettings = false;
    private String mName;
    private String mEmail;
    private ArrayList<String> mAdditionalEmails;
    private Passphrase mPassphrase;
    private CanonicalizedSecretKey.SecretKeyType mSecretKeyType;


    @Override
    public void prepareViewModel(Bundle savedInstanceState, Bundle arguments, Context context) {

    }

    @Override
    public void saveViewModelState(Bundle outState) {

    }

    @Override
    public void restoreViewModelState(Bundle savedInstanceState) {

    }

    public void prepareKeyRingData() {
        if (mSaveKeyringParcel == null) {
            mSaveKeyringParcel = new SaveKeyringParcel();
            if (mUseSmartCardSettings) {
                mSaveKeyringParcel.mAddSubKeys.add(new SaveKeyringParcel.SubkeyAdd(SaveKeyringParcel.Algorithm.RSA,
                        2048, null, KeyFlags.SIGN_DATA | KeyFlags.CERTIFY_OTHER, 0L));
                mSaveKeyringParcel.mAddSubKeys.add(new SaveKeyringParcel.SubkeyAdd(SaveKeyringParcel.Algorithm.RSA,
                        2048, null, KeyFlags.ENCRYPT_COMMS | KeyFlags.ENCRYPT_STORAGE, 0L));
                mSaveKeyringParcel.mAddSubKeys.add(new SaveKeyringParcel.SubkeyAdd(SaveKeyringParcel.Algorithm.RSA,
                        2048, null, KeyFlags.AUTHENTICATION, 0L));
            } else {
                mSaveKeyringParcel.mAddSubKeys.add(new SaveKeyringParcel.SubkeyAdd(SaveKeyringParcel.Algorithm.RSA,
                        4096, null, KeyFlags.CERTIFY_OTHER, 0L));
                mSaveKeyringParcel.mAddSubKeys.add(new SaveKeyringParcel.SubkeyAdd(SaveKeyringParcel.Algorithm.RSA,
                        4096, null, KeyFlags.SIGN_DATA, 0L));
                mSaveKeyringParcel.mAddSubKeys.add(new SaveKeyringParcel.SubkeyAdd(SaveKeyringParcel.Algorithm.RSA,
                        4096, null, KeyFlags.ENCRYPT_COMMS | KeyFlags.ENCRYPT_STORAGE, 0L));
            }
            String userId = KeyRing.createUserId(
                    new KeyRing.UserId(mName, mEmail, null)
            );
            mSaveKeyringParcel.mAddUserIds.add(userId);
            mSaveKeyringParcel.mChangePrimaryUserId = userId;
            if (mAdditionalEmails != null && mAdditionalEmails.size() > 0) {
                for (String email : mAdditionalEmails) {
                    String thisUserId = KeyRing.createUserId(
                            new KeyRing.UserId(mName, email, null)
                    );
                    mSaveKeyringParcel.mAddUserIds.add(thisUserId);
                }
            }
            mSaveKeyringParcel.mNewUnlock =  new SaveKeyringParcel.ChangeUnlockParcel(mPassphrase);
        }
    }

    /**
     * Generates a string of the user's emails.
     *
     * @param mainEmail
     * @param additionalEmails
     * @return
     */
    CharSequence generateAdditionalEmails(CharSequence mainEmail, ArrayList<String> additionalEmails) {
        if (additionalEmails == null) {
            return mainEmail;
        }

        StringBuffer emails = new StringBuffer();
        emails.append(mainEmail);
        emails.append(", ");
        Iterator<?> it = additionalEmails.iterator();
        while (it.hasNext()) {
            Object next = it.next();
            emails.append(next);
            if (it.hasNext()) {
                emails.append(", ");
            }
        }
        return emails;
    }

    public SaveKeyringParcel getSaveKeyringParcel() {
        return mSaveKeyringParcel;
    }

    public void setSaveKeyringParcel(SaveKeyringParcel saveKeyringParcel) {
        mSaveKeyringParcel = saveKeyringParcel;
    }

    public boolean isUseSmartCardSettings() {
        return mUseSmartCardSettings;
    }

    public void setUseSmartCardSettings(boolean useSmartCardSettings) {
        mUseSmartCardSettings = useSmartCardSettings;
    }

    public CharSequence getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public CharSequence getEmail() {
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

    public void setPassphrase(Passphrase passphrase) {
        mPassphrase = passphrase;
    }

    public CanonicalizedSecretKey.SecretKeyType getSecretKeyType() {
        return mSecretKeyType;
    }

    public void setSecretKeyType(CanonicalizedSecretKey.SecretKeyType secretKeyType) {
        mSecretKeyType = secretKeyType;
    }
}
