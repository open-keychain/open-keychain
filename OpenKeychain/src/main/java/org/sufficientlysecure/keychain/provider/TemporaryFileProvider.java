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

package org.sufficientlysecure.keychain.provider;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.ClipDescription;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.support.annotation.NonNull;

import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.util.DatabaseUtil;
import timber.log.Timber;

/**
 * TemporaryStorageProvider stores decrypted files inside the app's cache directory previously to
 * sharing them with other applications.
 * <p/>
 * Security:
 * - It is writable by OpenKeychain only (see Manifest), but exported for reading files
 * - It uses UUIDs as identifiers which makes predicting files from outside impossible
 * - Querying a number of files is not allowed, only querying single files
 * -> You can only open a file if you know the Uri containing the precise UUID, this Uri is only
 * revealed when the user shares a decrypted file with another app.
 * <p/>
 * Why is support lib's FileProvider not used?
 * Because granting Uri permissions temporarily does not work correctly. See
 * - https://code.google.com/p/android/issues/detail?id=76683
 * - https://github.com/nmr8acme/FileProvider-permission-bug
 * - http://stackoverflow.com/q/24467696
 * - http://stackoverflow.com/q/18249007
 * - Comments at http://www.blogc.at/2014/03/23/share-private-files-with-other-apps-fileprovider/
 */
public class TemporaryFileProvider extends ContentProvider {

    private static final String DB_NAME = "tempstorage.db";
    private static final String TABLE_FILES = "files";
    public static final String AUTHORITY = Constants.TEMP_FILE_PROVIDER_AUTHORITY;
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);
    private static final int DB_VERSION = 3;

    interface TemporaryFileColumns {
        String COLUMN_UUID = "id";
        String COLUMN_NAME = "name";
        String COLUMN_TIME = "time";
        String COLUMN_TYPE = "mimetype";
    }

    private static final String TEMP_FILES_DIR = "temp";
    private static File tempFilesDir;

    private static Pattern UUID_PATTERN = Pattern.compile("[a-fA-F0-9-]+");

    public static Uri createFile(Context context, String targetName, String mimeType) {
        ContentResolver contentResolver = context.getContentResolver();

        ContentValues contentValues = new ContentValues();
        contentValues.put(TemporaryFileColumns.COLUMN_NAME, targetName);
        contentValues.put(TemporaryFileColumns.COLUMN_TYPE, mimeType);
        contentValues.put(TemporaryFileColumns.COLUMN_TIME, System.currentTimeMillis());
        Uri resultUri = contentResolver.insert(CONTENT_URI, contentValues);

        scheduleCleanupAfterTtl();
        return resultUri;
    }

    public static Uri createFile(Context context, String targetName) {
        return createFile(context, targetName, null);
    }

    public static Uri createFile(Context context) {
        ContentValues contentValues = new ContentValues();
        return context.getContentResolver().insert(CONTENT_URI, contentValues);
    }

    public static int setName(Context context, Uri uri, String name) {
        ContentValues values = new ContentValues();
        values.put(TemporaryFileColumns.COLUMN_NAME, name);
        return context.getContentResolver().update(uri, values, null, null);
    }

    public static int setMimeType(Context context, Uri uri, String mimetype) {
        ContentValues values = new ContentValues();
        values.put(TemporaryFileColumns.COLUMN_TYPE, mimetype);
        return context.getContentResolver().update(uri, values, null, null);
    }

    private class TemporaryStorageDatabase extends SQLiteOpenHelper {

        public TemporaryStorageDatabase(Context context) {
            super(context, DB_NAME, null, DB_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_FILES + " (" +
                    TemporaryFileColumns.COLUMN_UUID + " TEXT PRIMARY KEY, " +
                    TemporaryFileColumns.COLUMN_NAME + " TEXT, " +
                    TemporaryFileColumns.COLUMN_TYPE + " TEXT, " +
                    TemporaryFileColumns.COLUMN_TIME + " INTEGER" +
                    ");");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Timber.d("Upgrading files db from %s to %s", oldVersion, newVersion);

            switch (oldVersion) {
                case 1:
                    db.execSQL("DROP TABLE IF EXISTS files");
                    db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_FILES + " (" +
                            TemporaryFileColumns.COLUMN_UUID + " TEXT PRIMARY KEY, " +
                            TemporaryFileColumns.COLUMN_NAME + " TEXT, " +
                            TemporaryFileColumns.COLUMN_TIME + " INTEGER" +
                            ");");
                case 2:
                    db.execSQL("ALTER TABLE files ADD COLUMN " + TemporaryFileColumns.COLUMN_TYPE + " TEXT");
            }
        }
    }

    private static TemporaryStorageDatabase db;

    private File getFile(Uri uri) throws FileNotFoundException {
        try {
            return getFile(uri.getLastPathSegment());
        } catch (NumberFormatException e) {
            throw new FileNotFoundException();
        }
    }

    private File getFile(String id) {
        Matcher m = UUID_PATTERN.matcher(id);
        if (!m.matches()) {
            throw new SecurityException("Can only open temporary files with UUIDs!");
        }

        return new File(tempFilesDir, id);
    }

    @Override
    public boolean onCreate() {
        db = new TemporaryStorageDatabase(getContext());
        tempFilesDir = new File(getContext().getCacheDir(), TEMP_FILES_DIR);
        return tempFilesDir.mkdirs();
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        if (uri.getLastPathSegment() == null) {
            throw new SecurityException("Listing temporary files is not allowed, only querying single files.");
        }

        File file;
        try {
            file = getFile(uri);
        } catch (FileNotFoundException e) {
            Timber.e("file not found!");
            return null;
        }

        Cursor fileName = db.getReadableDatabase().query(TABLE_FILES,
                new String[]{TemporaryFileColumns.COLUMN_NAME},
                TemporaryFileColumns.COLUMN_UUID + "=?",
                new String[]{uri.getLastPathSegment()}, null, null, null);
        if (fileName != null) {
            if (fileName.moveToNext()) {
                MatrixCursor cursor = new MatrixCursor(new String[]{
                        MediaStore.MediaColumns.DISPLAY_NAME,
                        MediaStore.MediaColumns.SIZE,
                        MediaStore.MediaColumns.DATA,
                });
                cursor.newRow()
                        .add(fileName.getString(0))
                        .add(file.length())
                        .add(file.getAbsolutePath());
                fileName.close();
                return cursor;
            }
            fileName.close();
        }
        return null;
    }

    @Override
    public String getType(Uri uri) {
        Cursor cursor = db.getReadableDatabase().query(TABLE_FILES,
                new String[]{TemporaryFileColumns.COLUMN_TYPE},
                TemporaryFileColumns.COLUMN_UUID + "=?",
                new String[]{uri.getLastPathSegment()}, null, null, null);
        if (cursor != null) {
            try {
                if (cursor.moveToNext()) {
                    if (!cursor.isNull(0)) {
                        return cursor.getString(0);
                    }
                }
            } finally {
                cursor.close();
            }
        }
        return "application/octet-stream";
    }

    @Override
    public String[] getStreamTypes(Uri uri, String mimeTypeFilter) {
        String type = getType(uri);
        if (ClipDescription.compareMimeTypes(type, mimeTypeFilter)) {
            return new String[]{type};
        }
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        String uuid = UUID.randomUUID().toString();
        values.put(TemporaryFileColumns.COLUMN_UUID, uuid);
        int insert = (int) db.getWritableDatabase().insert(TABLE_FILES, null, values);
        if (insert == -1) {
            Timber.e("Insert failed!");
            return null;
        }
        try {
            getFile(uuid).createNewFile();
        } catch (IOException e) {
            Timber.e("File creation failed!");
            return null;
        }
        return Uri.withAppendedPath(CONTENT_URI, uuid);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        if (uri == null) {
            return 0;
        }

        String fileUuidFromUri = uri.getLastPathSegment();
        if (fileUuidFromUri != null) {
            selection = DatabaseUtil.concatenateWhere(selection, TemporaryFileColumns.COLUMN_UUID + "=?");
            selectionArgs = DatabaseUtil.appendSelectionArgs(selectionArgs, new String[]{ fileUuidFromUri });
        }

        Cursor files = db.getReadableDatabase().query(TABLE_FILES, new String[]{TemporaryFileColumns.COLUMN_UUID}, selection,
                selectionArgs, null, null, null);
        if (files != null) {
            while (files.moveToNext()) {
                getFile(files.getString(0)).delete();
            }
            files.close();
            return db.getWritableDatabase().delete(TABLE_FILES, selection, selectionArgs);
        }
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        if (values.size() != 1) {
            throw new UnsupportedOperationException("Update supported only for one field at a time!");
        }
        if (!values.containsKey(TemporaryFileColumns.COLUMN_NAME) && !values.containsKey(TemporaryFileColumns.COLUMN_TYPE)) {
            throw new UnsupportedOperationException("Update supported only for name and type field!");
        }
        if (selection != null || selectionArgs != null) {
            throw new UnsupportedOperationException("Update supported only for plain uri!");
        }
        return db.getWritableDatabase().update(TABLE_FILES, values,
                TemporaryFileColumns.COLUMN_UUID + " = ?", new String[]{uri.getLastPathSegment()});
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        return openFileHelper(uri, mode);
    }

    public static void scheduleCleanupAfterTtl() {
        OneTimeWorkRequest cleanupWork = new OneTimeWorkRequest.Builder(CleanupWorker.class)
                .setInitialDelay(Constants.TEMPFILE_TTL, TimeUnit.MILLISECONDS).build();
        WorkManager.getInstance().enqueue(cleanupWork);
    }

    public static void scheduleCleanupImmediately() {
        OneTimeWorkRequest cleanupWork = new OneTimeWorkRequest.Builder(CleanupWorker.class).build();
        WorkManager workManager = WorkManager.getInstance();
        if (workManager != null) { // it's possible this is null, if this is called in onCreate of secondary processes
            workManager.enqueue(cleanupWork);
        }
    }

    public static class CleanupWorker extends Worker {
        @NonNull
        @Override
        public WorkerResult doWork() {
            Timber.d("Cleaning up temporary files…");

            ContentResolver contentResolver = getApplicationContext().getContentResolver();
            contentResolver.delete(
                    CONTENT_URI,
                    TemporaryFileColumns.COLUMN_TIME + " <= ?",
                    new String[]{Long.toString(System.currentTimeMillis() - Constants.TEMPFILE_TTL)}
            );

            return WorkerResult.SUCCESS;
        }
    }
}
