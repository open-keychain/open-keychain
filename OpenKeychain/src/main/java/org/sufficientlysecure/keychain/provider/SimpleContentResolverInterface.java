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


import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

/** This interface contains the principal methods for database access
 * from {#android.content.ContentResolver}. It is used to allow substitution
 * of a ContentResolver in DAOs.
 *
 * @see ApiDataAccessObject
 */
public interface SimpleContentResolverInterface {
    Cursor query(Uri contentUri, String[] projection, String selection, String[] selectionArgs, String sortOrder);

    Uri insert(Uri contentUri, ContentValues values);

    int update(Uri contentUri, ContentValues values, String where, String[] selectionArgs);

    int delete(Uri contentUri, String where, String[] selectionArgs);
}
