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

package org.sufficientlysecure.keychain.util;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.KeychainApplication;
import org.sufficientlysecure.keychain.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * When sending large data (over 1MB) through Androids Binder IPC you get
 * JavaBinder  E  !!! FAILED BINDER TRANSACTION !!!
 * <p/>
 * To overcome this problem, we cache large Parcelables into a file in our private cache directory
 * instead of sending them through IPC.
 */
public class FileImportCache<E extends Parcelable> {

    private Context mContext;

    private static final String FILENAME = "key_import.pcl";

    public FileImportCache(Context context) {
        this.mContext = context;
    }

    public void writeCache(ArrayList<E> selectedEntries) throws IOException {
        writeCache(selectedEntries.iterator());
    }

    public void writeCache(Iterator<E> it) throws IOException {

        File cacheDir = mContext.getCacheDir();
        if (cacheDir == null) {
            // https://groups.google.com/forum/#!topic/android-developers/-694j87eXVU
            throw new IOException("cache dir is null!");
        }

        File tempFile = new File(mContext.getCacheDir(), FILENAME);

        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(tempFile));

        while (it.hasNext()) {
            E ring = it.next();
            Parcel p = Parcel.obtain(); // creating empty parcel object
            p.writeParcelable(ring, 0); // saving bundle as parcel
            oos.writeObject(p.marshall()); // writing parcel to file
            p.recycle();
        }

        oos.close();

    }

    public List<E> readCacheIntoList() throws IOException {
        ArrayList<E> result = new ArrayList<E>();
        Iterator<E> it = readCache();
        while (it.hasNext()) {
            result.add(it.next());
        }
        return result;
    }

    public Iterator<E> readCache() throws IOException {

        File cacheDir = mContext.getCacheDir();
        if (cacheDir == null) {
            // https://groups.google.com/forum/#!topic/android-developers/-694j87eXVU
            throw new IOException("cache dir is null!");
        }

        final File tempFile = new File(cacheDir, FILENAME);
        final ObjectInputStream ois = new ObjectInputStream(new FileInputStream(tempFile));

        return new Iterator<E>() {

            E mRing = null;
            boolean closed = false;

            private void readNext() {
                if (mRing != null || closed) {
                    Log.e(Constants.TAG, "err!");
                    return;
                }

                try {
                    if (ois.available() == 0) {
                        return;
                    }

                    byte[] data = (byte[]) ois.readObject();
                    Log.e(Constants.TAG, "bla");
                    if (data == null) {
                        if (!closed) {
                            closed = true;
                            ois.close();
                            tempFile.delete();
                        }
                        return;
                    }

                    Parcel parcel = Parcel.obtain(); // creating empty parcel object
                    parcel.unmarshall(data, 0, data.length);
                    parcel.setDataPosition(0);
                    mRing = parcel.readParcelable(KeychainApplication.class.getClassLoader());
                    parcel.recycle();
                } catch (ClassNotFoundException e) {
                    Log.e(Constants.TAG, "Encountered ClassNotFoundException during cache read, this is a bug!");
                } catch (IOException e) {
                    Log.e(Constants.TAG, "Encountered IOException during cache read!");
                }

            }

            @Override
            public boolean hasNext() {
                readNext();
                return mRing != null;
            }

            @Override
            public E next() {
                readNext();
                try {
                    return mRing;
                } finally {
                    mRing = null;
                }
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }

            @Override
            public void finalize() throws Throwable {
                super.finalize();
                if (!closed) {
                    try {
                        ois.close();
                        tempFile.delete();
                    } catch (IOException e) {
                        // never mind
                    }
                }
            }

        };
    }
}
