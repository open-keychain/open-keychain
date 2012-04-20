/*
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

import org.thialfihar.android.apg.ApgService2;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;

public class ApgServiceBlobDatabase extends SQLiteOpenHelper {
    
    private static final String TAG = "ApgServiceBlobDatabase";

    private static final int VERSION = 1;
    private static final String NAME = "apg_service_blob_data";
    private static final String TABLE = "data";
        
    public ApgServiceBlobDatabase(Context context) {
        super(context, NAME, null, VERSION);
        if(ApgService2.LOCAL_LOGD) Log.d(TAG, "constructor called");
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        if(ApgService2.LOCAL_LOGD) Log.d(TAG, "onCreate() called");
        db.execSQL("create table " + TABLE + " ( _id integer primary key autoincrement," +
        		"key text not null)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if(ApgService2.LOCAL_LOGD) Log.d(TAG, "onUpgrade() called");
        // no upgrade necessary yet
    }
    
    public Uri insert(ContentValues vals) {
        if(ApgService2.LOCAL_LOGD) Log.d(TAG, "insert() called");
        SQLiteDatabase db = this.getWritableDatabase();
        long newId = db.insert(TABLE, null, vals);
        return ContentUris.withAppendedId(ApgServiceBlobProvider.CONTENT_URI, newId);
    }
    
    public Cursor query(String id, String key) {
        if(ApgService2.LOCAL_LOGD) Log.d(TAG, "query() called");
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE, new String[] {"_id"},
                "_id = ? and key = ?", new String[] {id, key},
                null, null, null);
    }
}
