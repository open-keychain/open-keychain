/*
 * Copyright (C) 2014 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.sufficientlysecure.keychain.keyimport;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcel;

import org.sufficientlysecure.keychain.KeychainApplication;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * When sending large data (over 1MB) through Androids Binder IPC you get
 * JavaBinder  E  !!! FAILED BINDER TRANSACTION !!!
 * <p/>
 * To overcome this problem, we cache large Parcelables into a file in our private cache directory
 * instead of sending them through IPC.
 */
public class FileImportCache {

    private Context mContext;

    private static final String FILENAME = "key_import.pcl";
    private static final String BUNDLE_DATA = "data";

    public FileImportCache(Context context) {
        this.mContext = context;
    }

    public void writeCache(ArrayList<ParcelableKeyRing> selectedEntries) throws IOException {
        Bundle in = new Bundle();
        in.putParcelableArrayList(BUNDLE_DATA, selectedEntries);
        File cacheDir = mContext.getCacheDir();
        if (cacheDir == null) {
            // https://groups.google.com/forum/#!topic/android-developers/-694j87eXVU
            throw new IOException("cache dir is null!");
        }
        File tempFile = new File(mContext.getCacheDir(), FILENAME);

        FileOutputStream fos = new FileOutputStream(tempFile);
        Parcel p = Parcel.obtain(); // creating empty parcel object
        in.writeToParcel(p, 0); // saving bundle as parcel
        fos.write(p.marshall()); // writing parcel to file
        fos.flush();
        fos.close();
    }

    public List<ParcelableKeyRing> readCache() throws IOException {
        Parcel parcel = Parcel.obtain(); // creating empty parcel object
        Bundle out;
        File cacheDir = mContext.getCacheDir();
        if (cacheDir == null) {
            // https://groups.google.com/forum/#!topic/android-developers/-694j87eXVU
            throw new IOException("cache dir is null!");
        }

        File tempFile = new File(cacheDir, FILENAME);
        try {
            FileInputStream fis = new FileInputStream(tempFile);
            byte[] array = new byte[(int) fis.getChannel().size()];
            fis.read(array, 0, array.length);
            fis.close();

            parcel.unmarshall(array, 0, array.length);
            parcel.setDataPosition(0);
            out = parcel.readBundle(KeychainApplication.class.getClassLoader());
            out.putAll(out);

            return out.getParcelableArrayList(BUNDLE_DATA);
        } finally {
            parcel.recycle();
            // delete temp file
            tempFile.delete();
        }
    }
}
