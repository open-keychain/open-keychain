/*
 * Copyright (C) 2017 Schürmann & Breitmoser GbR
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


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import org.sufficientlysecure.keychain.KeychainApplication;
import timber.log.Timber;

/**
 * When sending large data (over 1MB) through Androids Binder IPC you get
 * JavaBinder  E  !!! FAILED BINDER TRANSACTION !!!
 * <p/>
 * To overcome this problem, we cache large Parcelables into a file in our private cache directory
 * instead of sending them through IPC.
 */
public class ParcelableFileCache<E extends Parcelable> {

    private Context mContext;

    private final String mFilename;

    public ParcelableFileCache(Context context, String filename) {
        mContext = context;
        mFilename = filename;
    }

    public void writeCache(int numEntries, Iterator<E> it) throws IOException {
        DataOutputStream oos = getOutputStream();

        try {
            oos.writeInt(numEntries);
            while (it.hasNext()) {
                writeParcelable(it.next(), oos);
            }
        } finally {
            oos.close();
        }
    }

    public void writeCache(E obj) throws IOException {
        DataOutputStream oos = getOutputStream();

        try {
            oos.writeInt(1);
            writeParcelable(obj, oos);
        } finally {
            oos.close();
        }
    }

    private void writeParcelable(E obj, DataOutputStream oos) throws IOException {
        Parcel p = Parcel.obtain(); // creating empty parcel object
        p.writeParcelable(obj, 0); // saving bundle as parcel
        byte[] buf = p.marshall();
        oos.writeInt(buf.length);
        oos.write(buf);
        p.recycle();
    }

    private DataOutputStream getOutputStream() throws IOException {
        File cacheDir = mContext.getCacheDir();
        if (cacheDir == null) {
            // https://groups.google.com/forum/#!topic/android-developers/-694j87eXVU
            throw new IOException("cache dir is null!");
        }

        File tempFile = new File(mContext.getCacheDir(), mFilename);
        return new DataOutputStream(new FileOutputStream(tempFile));
    }

    /**
     * Reads from cache file and deletes it afterward. Convenience function for readCache(boolean).
     *
     * @return an IteratorWithSize object containing entries read from the cache file
     * @throws IOException
     */
    public IteratorWithSize<E> readCache() throws IOException {
        return readCache(true);
    }

    /**
     * Reads entries from a cache file and returns an IteratorWithSize object containing the entries
     *
     * @param deleteAfterRead if true, the cache file will be deleted after being read
     * @return an IteratorWithSize object containing entries read from the cache file
     * @throws IOException if cache directory/parcel import file does not exist, or a read error
     *                     occurs
     */
    public IteratorWithSize<E> readCache(final boolean deleteAfterRead) throws IOException {

        File cacheDir = mContext.getCacheDir();
        if (cacheDir == null) {
            // https://groups.google.com/forum/#!topic/android-developers/-694j87eXVU
            throw new IOException("cache dir is null!");
        }

        final File tempFile = new File(cacheDir, mFilename);
        final DataInputStream ois;
        try {
            ois = new DataInputStream(new FileInputStream(tempFile));
        } catch (FileNotFoundException e) {
            Timber.e(e, "parcel import file not existing");
            throw new IOException(e);
        }

        final int numEntries = ois.readInt();

        return new IteratorWithSize<E>() {

            E mRing = null;
            boolean closed = false;
            byte[] buf = new byte[512];

            public int getSize() {
                return numEntries;
            }

            private void readNext() {
                if (mRing != null || closed) {
                    return;
                }

                try {

                    int length = ois.readInt();
                    while (buf.length < length) {
                        buf = new byte[buf.length * 2];
                    }
                    ois.readFully(buf, 0, length);

                    Parcel parcel = Parcel.obtain(); // creating empty parcel object
                    parcel.unmarshall(buf, 0, length);
                    parcel.setDataPosition(0);
                    mRing = parcel.readParcelable(KeychainApplication.class.getClassLoader());
                    parcel.recycle();
                } catch (EOFException e) {
                    // aight
                    close();
                } catch (IOException e) {
                    Timber.e(e, "Encountered IOException during cache read!");
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
                close();
                super.finalize();
            }

            private void close() {
                if (!closed) {
                    try {
                        ois.close();
                        if (deleteAfterRead) {
                            //noinspection ResultOfMethodCallIgnored
                            tempFile.delete();
                        }
                    } catch (IOException e) {
                        // nvm
                    }
                }
                closed = true;
            }


        };
    }

    public boolean delete() throws IOException {
        File cacheDir = mContext.getCacheDir();
        if (cacheDir == null) {
            // https://groups.google.com/forum/#!topic/android-developers/-694j87eXVU
            throw new IOException("cache dir is null!");
        }

        final File tempFile = new File(cacheDir, mFilename);
        return tempFile.delete();
    }

}
