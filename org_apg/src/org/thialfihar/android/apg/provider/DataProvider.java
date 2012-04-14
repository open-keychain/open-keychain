/*
 * Copyright (C) 2010 Thialfihar <thi@thialfihar.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.thialfihar.android.apg.provider;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;

import org.thialfihar.android.apg.Id;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;

public class DataProvider extends ContentProvider {
    public static final String AUTHORITY = "org.thialfihar.android.apg.provider";

    private static final int PUBLIC_KEY_RING = 101;
    private static final int PUBLIC_KEY_RING_ID = 102;
    private static final int PUBLIC_KEY_RING_BY_KEY_ID = 103;
    private static final int PUBLIC_KEY_RING_BY_EMAILS = 104;
    private static final int PUBLIC_KEY_RING_KEY = 111;
    private static final int PUBLIC_KEY_RING_KEY_RANK = 112;
    private static final int PUBLIC_KEY_RING_USER_ID = 121;
    private static final int PUBLIC_KEY_RING_USER_ID_RANK = 122;

    private static final int SECRET_KEY_RING = 201;
    private static final int SECRET_KEY_RING_ID = 202;
    private static final int SECRET_KEY_RING_BY_KEY_ID = 203;
    private static final int SECRET_KEY_RING_BY_EMAILS = 204;
    private static final int SECRET_KEY_RING_KEY = 211;
    private static final int SECRET_KEY_RING_KEY_RANK = 212;
    private static final int SECRET_KEY_RING_USER_ID = 221;
    private static final int SECRET_KEY_RING_USER_ID_RANK = 222;

    private static final int DATA_STREAM = 301;

    private static final String PUBLIC_KEY_RING_CONTENT_DIR_TYPE =
            "vnd.android.cursor.dir/vnd.thialfihar.apg.public.key_ring";
    private static final String PUBLIC_KEY_RING_CONTENT_ITEM_TYPE =
            "vnd.android.cursor.item/vnd.thialfihar.apg.public.key_ring";

    private static final String PUBLIC_KEY_CONTENT_DIR_TYPE =
            "vnd.android.cursor.dir/vnd.thialfihar.apg.public.key";
    private static final String PUBLIC_KEY_CONTENT_ITEM_TYPE =
            "vnd.android.cursor.item/vnd.thialfihar.apg.public.key";

    private static final String SECRET_KEY_RING_CONTENT_DIR_TYPE =
            "vnd.android.cursor.dir/vnd.thialfihar.apg.secret.key_ring";
    private static final String SECRET_KEY_RING_CONTENT_ITEM_TYPE =
            "vnd.android.cursor.item/vnd.thialfihar.apg.secret.key_ring";

    private static final String SECRET_KEY_CONTENT_DIR_TYPE =
            "vnd.android.cursor.dir/vnd.thialfihar.apg.secret.key";
    private static final String SECRET_KEY_CONTENT_ITEM_TYPE =
            "vnd.android.cursor.item/vnd.thialfihar.apg.secret.key";

    private static final String USER_ID_CONTENT_DIR_TYPE =
            "vnd.android.cursor.dir/vnd.thialfihar.apg.user_id";
    private static final String USER_ID_CONTENT_ITEM_TYPE =
            "vnd.android.cursor.item/vnd.thialfihar.apg.user_id";

    public static final String _ID = "_id";
    public static final String MASTER_KEY_ID = "master_key_id";
    public static final String KEY_ID = "key_id";
    public static final String USER_ID = "user_id";

    private static final UriMatcher mUriMatcher;

    private Database mDb;

    static {
        mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        mUriMatcher.addURI(AUTHORITY, "key_rings/public/key_id/*", PUBLIC_KEY_RING_BY_KEY_ID);
        mUriMatcher.addURI(AUTHORITY, "key_rings/public/emails/*", PUBLIC_KEY_RING_BY_EMAILS);

        mUriMatcher.addURI(AUTHORITY, "key_rings/public/*/keys", PUBLIC_KEY_RING_KEY);
        mUriMatcher.addURI(AUTHORITY, "key_rings/public/*/keys/#", PUBLIC_KEY_RING_KEY_RANK);

        mUriMatcher.addURI(AUTHORITY, "key_rings/public/*/user_ids", PUBLIC_KEY_RING_USER_ID);
        mUriMatcher.addURI(AUTHORITY, "key_rings/public/*/user_ids/#", PUBLIC_KEY_RING_USER_ID_RANK);

        mUriMatcher.addURI(AUTHORITY, "key_rings/public", PUBLIC_KEY_RING);
        mUriMatcher.addURI(AUTHORITY, "key_rings/public/*", PUBLIC_KEY_RING_ID);

        mUriMatcher.addURI(AUTHORITY, "key_rings/secret/key_id/*", SECRET_KEY_RING_BY_KEY_ID);
        mUriMatcher.addURI(AUTHORITY, "key_rings/secret/emails/*", SECRET_KEY_RING_BY_EMAILS);

        mUriMatcher.addURI(AUTHORITY, "key_rings/secret/*/keys", SECRET_KEY_RING_KEY);
        mUriMatcher.addURI(AUTHORITY, "key_rings/secret/*/keys/#", SECRET_KEY_RING_KEY_RANK);

        mUriMatcher.addURI(AUTHORITY, "key_rings/secret/*/user_ids", SECRET_KEY_RING_USER_ID);
        mUriMatcher.addURI(AUTHORITY, "key_rings/secret/*/user_ids/#", SECRET_KEY_RING_USER_ID_RANK);

        mUriMatcher.addURI(AUTHORITY, "key_rings/secret", SECRET_KEY_RING);
        mUriMatcher.addURI(AUTHORITY, "key_rings/secret/*", SECRET_KEY_RING_ID);

        mUriMatcher.addURI(AUTHORITY, "data/*", DATA_STREAM);
    }

    @Override
    public boolean onCreate() {
        mDb = new Database(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        // TODO: implement the others, then use them for the lists
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        HashMap<String, String> projectionMap = new HashMap<String, String>();

        int match = mUriMatcher.match(uri);
        int type;
        switch (match) {
            case PUBLIC_KEY_RING:
            case PUBLIC_KEY_RING_ID:
            case PUBLIC_KEY_RING_BY_KEY_ID:
            case PUBLIC_KEY_RING_BY_EMAILS:
            case PUBLIC_KEY_RING_KEY:
            case PUBLIC_KEY_RING_KEY_RANK:
            case PUBLIC_KEY_RING_USER_ID:
            case PUBLIC_KEY_RING_USER_ID_RANK:
                type = Id.database.type_public;
                break;

            case SECRET_KEY_RING:
            case SECRET_KEY_RING_ID:
            case SECRET_KEY_RING_BY_KEY_ID:
            case SECRET_KEY_RING_BY_EMAILS:
            case SECRET_KEY_RING_KEY:
            case SECRET_KEY_RING_KEY_RANK:
            case SECRET_KEY_RING_USER_ID:
            case SECRET_KEY_RING_USER_ID_RANK:
                type = Id.database.type_secret;
                break;

            default: {
                throw new IllegalArgumentException("Unknown URI " + uri);
            }
        }

        qb.appendWhere(KeyRings.TABLE_NAME + "." + KeyRings.TYPE + " = " + type);

        switch (match) {
            case PUBLIC_KEY_RING_ID:
            case SECRET_KEY_RING_ID: {
                qb.appendWhere(" AND " +
                               KeyRings.TABLE_NAME + "." + KeyRings.MASTER_KEY_ID + " = ");
                qb.appendWhereEscapeString(uri.getPathSegments().get(2));

                // break omitted intentionally
            }

            case PUBLIC_KEY_RING:
            case SECRET_KEY_RING: {
                qb.setTables(KeyRings.TABLE_NAME + " INNER JOIN " + Keys.TABLE_NAME + " ON " +
                             "(" + KeyRings.TABLE_NAME + "." + KeyRings._ID + " = " +
                             Keys.TABLE_NAME + "." + Keys.KEY_RING_ID + " AND " +
                             Keys.TABLE_NAME + "." + Keys.IS_MASTER_KEY + " = '1'" +
                             ") " +
                             " INNER JOIN " + UserIds.TABLE_NAME + " ON " +
                             "(" + Keys.TABLE_NAME + "." + Keys._ID + " = " +
                             UserIds.TABLE_NAME + "." + UserIds.KEY_ID + " AND " +
                             UserIds.TABLE_NAME + "." + UserIds.RANK + " = '0') ");

                projectionMap.put(_ID,
                                  KeyRings.TABLE_NAME + "." + KeyRings._ID);
                projectionMap.put(MASTER_KEY_ID,
                                  KeyRings.TABLE_NAME + "." + KeyRings.MASTER_KEY_ID);
                projectionMap.put(USER_ID,
                                  UserIds.TABLE_NAME + "." + UserIds.USER_ID);

                if (TextUtils.isEmpty(sortOrder)) {
                    sortOrder = UserIds.TABLE_NAME + "." + UserIds.USER_ID + " ASC";
                }

                break;
            }

            case SECRET_KEY_RING_BY_KEY_ID:
            case PUBLIC_KEY_RING_BY_KEY_ID: {
                qb.setTables(Keys.TABLE_NAME + " AS tmp INNER JOIN " +
                             KeyRings.TABLE_NAME + " ON (" +
                             KeyRings.TABLE_NAME + "." + KeyRings._ID + " = " +
                             "tmp." + Keys.KEY_RING_ID + ")" +
                             " INNER JOIN " + Keys.TABLE_NAME + " ON " +
                             "(" + KeyRings.TABLE_NAME + "." + KeyRings._ID + " = " +
                             Keys.TABLE_NAME + "." + Keys.KEY_RING_ID + " AND " +
                             Keys.TABLE_NAME + "." + Keys.IS_MASTER_KEY + " = '1'" +
                             ") " +
                             " INNER JOIN " + UserIds.TABLE_NAME + " ON " +
                             "(" + Keys.TABLE_NAME + "." + Keys._ID + " = " +
                             UserIds.TABLE_NAME + "." + UserIds.KEY_ID + " AND " +
                             UserIds.TABLE_NAME + "." + UserIds.RANK + " = '0') ");

                projectionMap.put(_ID,
                                  KeyRings.TABLE_NAME + "." + KeyRings._ID);
                projectionMap.put(MASTER_KEY_ID,
                                  KeyRings.TABLE_NAME + "." + KeyRings.MASTER_KEY_ID);
                projectionMap.put(USER_ID,
                                  UserIds.TABLE_NAME + "." + UserIds.USER_ID);

                qb.appendWhere(" AND tmp." + Keys.KEY_ID + " = ");
                qb.appendWhereEscapeString(uri.getPathSegments().get(3));

                break;
            }

            case SECRET_KEY_RING_BY_EMAILS:
            case PUBLIC_KEY_RING_BY_EMAILS: {
                qb.setTables(KeyRings.TABLE_NAME + " INNER JOIN " + Keys.TABLE_NAME + " ON " +
                             "(" + KeyRings.TABLE_NAME + "." + KeyRings._ID + " = " +
                             Keys.TABLE_NAME + "." + Keys.KEY_RING_ID + " AND " +
                             Keys.TABLE_NAME + "." + Keys.IS_MASTER_KEY + " = '1'" +
                             ") " +
                             " INNER JOIN " + UserIds.TABLE_NAME + " ON " +
                             "(" + Keys.TABLE_NAME + "." + Keys._ID + " = " +
                             UserIds.TABLE_NAME + "." + UserIds.KEY_ID + " AND " +
                             UserIds.TABLE_NAME + "." + UserIds.RANK + " = '0') ");

                projectionMap.put(_ID,
                                  KeyRings.TABLE_NAME + "." + KeyRings._ID);
                projectionMap.put(MASTER_KEY_ID,
                                  KeyRings.TABLE_NAME + "." + KeyRings.MASTER_KEY_ID);
                projectionMap.put(USER_ID,
                                  UserIds.TABLE_NAME + "." + UserIds.USER_ID);

                String emails = uri.getPathSegments().get(3);
                String chunks[] = emails.split(" *, *");
                boolean gotCondition = false;
                String emailWhere = "";
                for (int i = 0; i < chunks.length; ++i) {
                    if (chunks[i].length() == 0) {
                        continue;
                    }
                    if (i != 0) {
                        emailWhere += " OR ";
                    }
                    emailWhere += "tmp." + UserIds.USER_ID + " LIKE ";
                    // match '*<email>', so it has to be at the *end* of the user id
                    emailWhere += DatabaseUtils.sqlEscapeString("%<" + chunks[i] + ">");
                    gotCondition = true;
                }

                if (gotCondition) {
                    qb.appendWhere(" AND EXISTS (SELECT tmp." + UserIds._ID +
                                     " FROM " + UserIds.TABLE_NAME +
                                       " AS tmp WHERE tmp." + UserIds.KEY_ID + " = " +
                                           Keys.TABLE_NAME + "." + Keys._ID +
                                       " AND (" + emailWhere + "))");
                }

                break;
            }

            default: {
                throw new IllegalArgumentException("Unknown URI " + uri);
            }
        }

        qb.setProjectionMap(projectionMap);

        // If no sort order is specified use the default
        String orderBy;
        if (TextUtils.isEmpty(sortOrder)) {
            orderBy = null;
        } else {
            orderBy = sortOrder;
        }

        //System.out.println(qb.buildQuery(projection, selection, selectionArgs, null, null, sortOrder, null).replace("WHERE", "WHERE\n"));
        Cursor c = qb.query(mDb.db(), projection, selection, selectionArgs, null, null, orderBy);

        // Tell the cursor what uri to watch, so it knows when its source data changes
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public String getType(Uri uri) {
        switch (mUriMatcher.match(uri)) {
            case PUBLIC_KEY_RING:
            case PUBLIC_KEY_RING_BY_EMAILS:
                return PUBLIC_KEY_RING_CONTENT_DIR_TYPE;

            case PUBLIC_KEY_RING_ID:
                return PUBLIC_KEY_RING_CONTENT_ITEM_TYPE;

            case PUBLIC_KEY_RING_BY_KEY_ID:
                return PUBLIC_KEY_RING_CONTENT_ITEM_TYPE;

            case PUBLIC_KEY_RING_KEY:
                return PUBLIC_KEY_CONTENT_DIR_TYPE;

            case PUBLIC_KEY_RING_KEY_RANK:
                return PUBLIC_KEY_CONTENT_ITEM_TYPE;

            case PUBLIC_KEY_RING_USER_ID:
                return USER_ID_CONTENT_DIR_TYPE;

            case PUBLIC_KEY_RING_USER_ID_RANK:
                return USER_ID_CONTENT_ITEM_TYPE;

            case SECRET_KEY_RING:
            case SECRET_KEY_RING_BY_EMAILS:
                return SECRET_KEY_RING_CONTENT_DIR_TYPE;

            case SECRET_KEY_RING_ID:
                return SECRET_KEY_RING_CONTENT_ITEM_TYPE;

            case SECRET_KEY_RING_BY_KEY_ID:
                return SECRET_KEY_RING_CONTENT_ITEM_TYPE;

            case SECRET_KEY_RING_KEY:
                return SECRET_KEY_CONTENT_DIR_TYPE;

            case SECRET_KEY_RING_KEY_RANK:
                return SECRET_KEY_CONTENT_ITEM_TYPE;

            case SECRET_KEY_RING_USER_ID:
                return USER_ID_CONTENT_DIR_TYPE;

            case SECRET_KEY_RING_USER_ID_RANK:
                return USER_ID_CONTENT_ITEM_TYPE;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        // not supported
        return null;
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        // not supported
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        // not supported
        return 0;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        int match = mUriMatcher.match(uri);
        if (match != DATA_STREAM) {
            throw new FileNotFoundException();
        }
        String fileName = uri.getPathSegments().get(1);
        File file = new File(getContext().getFilesDir().getAbsolutePath(), fileName);
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
    }
}
