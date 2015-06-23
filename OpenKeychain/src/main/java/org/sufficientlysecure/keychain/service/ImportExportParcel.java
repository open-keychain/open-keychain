package org.sufficientlysecure.keychain.service;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Empty class, simply serves as a base class for ImportKeyringParcel and ExportKeyringParcel
 */
public class ImportExportParcel implements Parcelable {
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

    }
}
