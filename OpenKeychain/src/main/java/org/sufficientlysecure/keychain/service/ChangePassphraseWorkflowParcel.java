package org.sufficientlysecure.keychain.service;

import android.os.Parcel;
import android.os.Parcelable;
import org.sufficientlysecure.keychain.util.KeyringPassphrases;
import org.sufficientlysecure.keychain.util.ParcelableHashMap;
import org.sufficientlysecure.keychain.util.ParcelableLong;
import org.sufficientlysecure.keychain.util.Passphrase;

import java.util.HashMap;

public class ChangePassphraseWorkflowParcel implements Parcelable {
    public final HashMap<Long, Passphrase> mPassphrases;
    public boolean mToSinglePassphraseWorkflow;
    public final Passphrase mMasterPassphrase;

    public ChangePassphraseWorkflowParcel(HashMap<Long, Passphrase> passphrases,
                                          Passphrase masterPassphrase,
                                          boolean toSinglePassphraseWorkflow) {
        mPassphrases = passphrases;
        mMasterPassphrase = masterPassphrase;
        mToSinglePassphraseWorkflow = toSinglePassphraseWorkflow;
    }

    protected ChangePassphraseWorkflowParcel(Parcel in) {
        ParcelableHashMap<ParcelableLong, Passphrase> parcelableHashMap =
                in.readParcelable(ParcelableHashMap.class.getClassLoader());
        mPassphrases = ParcelableHashMap.toHashMap(parcelableHashMap);
        mMasterPassphrase = in.readParcelable(Passphrase.class.getClassLoader());
        mToSinglePassphraseWorkflow = in.readInt() == 1;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(ParcelableHashMap.toParcelableHashMap(mPassphrases), 0);
        dest.writeParcelable(mMasterPassphrase, 0);
        dest.writeInt(mToSinglePassphraseWorkflow ? 1 : 0);
    }

    public static final Creator<ChangePassphraseWorkflowParcel> CREATOR = new Parcelable.Creator<ChangePassphraseWorkflowParcel>() {
        @Override
        public ChangePassphraseWorkflowParcel createFromParcel(Parcel in) {
            return new ChangePassphraseWorkflowParcel(in);
        }

        @Override
        public ChangePassphraseWorkflowParcel[] newArray(int size) {
            return new ChangePassphraseWorkflowParcel[size];
        }
    };

}
