/*
 * Copyright (C) 2012-2013 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2011 Markus Doits <markus.doits@googlemail.com>
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

package org.sufficientlysecure.keychain.provider;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.provider.KeychainServiceBlobContract.Blobs;
import org.sufficientlysecure.keychain.provider.KeychainServiceBlobContract.BlobsColumns;
import org.sufficientlysecure.keychain.util.Log;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.BaseColumns;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class KeychainServiceBlobProvider extends ContentProvider {
    private static final String STORE_PATH = Constants.Path.APP_DIR + "/ApgBlobs";

    private KeychainServiceBlobDatabase mBlobDatabase = null;

    public KeychainServiceBlobProvider() {
        File dir = new File(STORE_PATH);
        dir.mkdirs();
    }

    @Override
    public boolean onCreate() {
        mBlobDatabase = new KeychainServiceBlobDatabase(getContext());
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public Uri insert(Uri uri, ContentValues ignored) {
        // ContentValues are actually ignored, because we want to store a blob with no more
        // information but have to create an record with the password generated here first
        ContentValues vals = new ContentValues();

        // Insert a random key in the database. This has to provided by the caller when updating or
        // getting the blob
        String password = UUID.randomUUID().toString();
        vals.put(BlobsColumns.KEY, password);

        SQLiteDatabase db = mBlobDatabase.getWritableDatabase();
        long newRowId = db.insert(KeychainServiceBlobDatabase.TABLE, null, vals);
        Uri insertedUri = ContentUris.withAppendedId(Blobs.CONTENT_URI, newRowId);

        return Uri.withAppendedPath(insertedUri, password);
    }

    /** {@inheritDoc} */
    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws SecurityException,
            FileNotFoundException {
        Log.d(Constants.TAG, "openFile() called with uri: " + uri.toString() + " and mode: " + mode);

        List<String> segments = uri.getPathSegments();
        if (segments.size() < 2) {
            throw new SecurityException("Password not found in URI");
        }
        String id = segments.get(0);
        String key = segments.get(1);

        Log.d(Constants.TAG, "Got id: " + id + " and key: " + key);

        // get the data
        SQLiteDatabase db = mBlobDatabase.getReadableDatabase();
        Cursor result = db.query(KeychainServiceBlobDatabase.TABLE, new String[] { BaseColumns._ID },
                BaseColumns._ID + " = ? and " + BlobsColumns.KEY + " = ?",
                new String[] { id, key }, null, null, null);

        if (result.getCount() == 0) {
            // either the key is wrong or no id exists
            throw new FileNotFoundException("No file found with that ID and/or password");
        }

        File targetFile = new File(STORE_PATH, id);
        if (mode.equals("w")) {
            Log.d(Constants.TAG, "Try to open file w");
            if (!targetFile.exists()) {
                try {
                    targetFile.createNewFile();
                } catch (IOException e) {
                    Log.e(Constants.TAG, "Got IEOException on creating new file", e);
                    throw new FileNotFoundException("Could not create file to write to");
                }
            }
            return ParcelFileDescriptor.open(targetFile, ParcelFileDescriptor.MODE_WRITE_ONLY
                    | ParcelFileDescriptor.MODE_TRUNCATE);
        } else if (mode.equals("r")) {
            Log.d(Constants.TAG, "Try to open file r");
            if (!targetFile.exists()) {
                throw new FileNotFoundException("Error: Could not find the file requested");
            }
            return ParcelFileDescriptor.open(targetFile, ParcelFileDescriptor.MODE_READ_ONLY);
        }

        return null;
    }

    /** {@inheritDoc} */
    @Override
    public String getType(Uri uri) {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    /** {@inheritDoc} */
    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

}
