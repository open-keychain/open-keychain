package org.sufficientlysecure.keychain.service;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

public class ExportKeyringParcel extends ImportExportParcel implements Parcelable {
    public String mKeyserver;
    public Uri mCanonicalizedPublicKeyringUri;

    public boolean mExportSecret;
    public long mMasterKeyIds[];
    public String mOutputFile;
    public Uri mOutputUri;
    public ExportType mExportType;

    public enum ExportType {
        UPLOAD_KEYSERVER,
        EXPORT_FILE,
        EXPORT_URI
    }

    public ExportKeyringParcel(String keyserver, Uri keyringUri) {
        mExportType = ExportType.UPLOAD_KEYSERVER;
        mKeyserver = keyserver;
        mCanonicalizedPublicKeyringUri = keyringUri;
    }

    public ExportKeyringParcel(long[] masterKeyIds, boolean exportSecret, String outputFile) {
        mExportType = ExportType.EXPORT_FILE;
        mMasterKeyIds = masterKeyIds;
        mExportSecret = exportSecret;
        mOutputFile = outputFile;
    }

    public ExportKeyringParcel(long[] masterKeyIds, boolean exportSecret, Uri outputUri) {
        mExportType = ExportType.EXPORT_URI;
        mMasterKeyIds = masterKeyIds;
        mExportSecret = exportSecret;
        mOutputUri = outputUri;
    }

    protected ExportKeyringParcel(Parcel in) {
        mKeyserver = in.readString();
        mCanonicalizedPublicKeyringUri = (Uri) in.readValue(Uri.class.getClassLoader());
        mExportSecret = in.readByte() != 0x00;
        mOutputFile = in.readString();
        mOutputUri = (Uri) in.readValue(Uri.class.getClassLoader());
        mExportType = (ExportType) in.readValue(ExportType.class.getClassLoader());
        mMasterKeyIds = in.createLongArray();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mKeyserver);
        dest.writeValue(mCanonicalizedPublicKeyringUri);
        dest.writeByte((byte) (mExportSecret ? 0x01 : 0x00));
        dest.writeString(mOutputFile);
        dest.writeValue(mOutputUri);
        dest.writeValue(mExportType);
        dest.writeLongArray(mMasterKeyIds);
    }

    public static final Parcelable.Creator<ExportKeyringParcel> CREATOR = new Parcelable.Creator<ExportKeyringParcel>() {
        @Override
        public ExportKeyringParcel createFromParcel(Parcel in) {
            return new ExportKeyringParcel(in);
        }

        @Override
        public ExportKeyringParcel[] newArray(int size) {
            return new ExportKeyringParcel[size];
        }
    };
}