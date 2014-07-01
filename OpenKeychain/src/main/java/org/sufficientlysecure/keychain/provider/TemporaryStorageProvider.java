package org.sufficientlysecure.keychain.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.util.DatabaseUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class TemporaryStorageProvider extends ContentProvider {

    private static final String DB_NAME = "tempstorage.db";
    private static final String TABLE_FILES = "files";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_TIME = "time";
    private static final Uri BASE_URI = Uri.parse("content://org.sufficientlysecure.keychain.tempstorage/");
    private static final int DB_VERSION = 1;

    public static Uri createFile(Context context, String targetName) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_NAME, targetName);
        return context.getContentResolver().insert(BASE_URI, contentValues);
    }

    public static int cleanUp(Context context) {
        return context.getContentResolver().delete(BASE_URI, COLUMN_TIME + "< ?",
                new String[]{Long.toString(System.currentTimeMillis() - Constants.TEMPFILE_TTL)});
    }

    private class TemporaryStorageDatabase extends SQLiteOpenHelper {

        public TemporaryStorageDatabase(Context context) {
            super(context, DB_NAME, null, DB_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_FILES + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_NAME + " TEXT, " +
                    COLUMN_TIME + " INTEGER" +
                    ");");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        }
    }

    private TemporaryStorageDatabase db;

    private File getFile(Uri uri) throws FileNotFoundException {
        try {
            return getFile(Integer.parseInt(uri.getLastPathSegment()));
        } catch (NumberFormatException e) {
            throw new FileNotFoundException();
        }
    }

    private File getFile(int id) {
        return new File(getContext().getCacheDir(), "temp/" + id);
    }

    @Override
    public boolean onCreate() {
        db = new TemporaryStorageDatabase(getContext());
        return new File(getContext().getCacheDir(), "temp").mkdirs();
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        File file;
        try {
            file = getFile(uri);
        } catch (FileNotFoundException e) {
            return null;
        }
        Cursor fileName = db.getReadableDatabase().query(TABLE_FILES, new String[]{COLUMN_NAME}, COLUMN_ID + "=?",
                new String[]{uri.getLastPathSegment()}, null, null, null);
        if (fileName != null) {
            if (fileName.moveToNext()) {
                MatrixCursor cursor =
                        new MatrixCursor(new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE, "_data"});
                cursor.newRow().add(fileName.getString(0)).add(file.length()).add(file.getAbsolutePath());
                fileName.close();
                return cursor;
            }
            fileName.close();
        }
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return "*/*";
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        if (!values.containsKey(COLUMN_TIME)) {
            values.put(COLUMN_TIME, System.currentTimeMillis());
        }
        int insert = (int) db.getWritableDatabase().insert(TABLE_FILES, null, values);
        try {
            getFile(insert).createNewFile();
        } catch (IOException e) {
            return null;
        }
        return Uri.withAppendedPath(BASE_URI, Long.toString(insert));
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        if (uri.getLastPathSegment() != null) {
            selection = DatabaseUtil.concatenateWhere(selection, COLUMN_ID + "=?");
            selectionArgs = DatabaseUtil.appendSelectionArgs(selectionArgs, new String[]{uri.getLastPathSegment()});
        }
        Cursor files = db.getReadableDatabase().query(TABLE_FILES, new String[]{COLUMN_ID}, selection,
                selectionArgs, null, null, null);
        if (files != null) {
            while (files.moveToNext()) {
                getFile(files.getInt(0)).delete();
            }
            files.close();
            return db.getWritableDatabase().delete(TABLE_FILES, selection, selectionArgs);
        }
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Update not supported");
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        return openFileHelper(uri, mode);
    }
}
