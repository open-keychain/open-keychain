/*
 * Copyright (C) 2014 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2014 Vincent Breitmoser <v.breitmoser@mugenguild.com>
 * Copyright (C) 2016 Alex Fong Jie Wen <alexfongg@gmail.com>
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

package org.sufficientlysecure.keychain.service;

import android.os.Parcel;
import android.os.Parcelable;
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
