package org.apg.provider;

import org.apg.ApgService;
import org.apg.Constants;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class ApgServiceBlobProvider extends ContentProvider {
    
    private static final String TAG = "ApgServiceBlobProvider";
    
    public static final Uri CONTENT_URI = Uri.parse("content://org.apg.provider.apgserviceblobprovider");
    
    private static final String COLUMN_KEY = "key";
    
    private static final String STORE_PATH = Constants.path.APP_DIR+"/ApgServiceBlobs";
    
    private ApgServiceBlobDatabase mDb = null;

    public ApgServiceBlobProvider() {
        if(ApgService.LOCAL_LOGD) Log.d(TAG, "Constructor called");
        File dir = new File(STORE_PATH);
        dir.mkdirs();
        if(ApgService.LOCAL_LOGD) Log.d(TAG, "Constructor finished");
    }

    @Override
    public int delete(Uri arg0, String arg1, String[] arg2) {
        if(ApgService.LOCAL_LOGD) Log.d(TAG, "delete() called");
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getType(Uri arg0) {
        if(ApgService.LOCAL_LOGD) Log.d(TAG, "getType() called");
        // not needed for now
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues ignored) {
        if(ApgService.LOCAL_LOGD) Log.d(TAG, "insert() called");
        // ContentValues are actually ignored, because we want to store a blob with no more information 
        // but have to create an record with the password generated here first
        
        ContentValues vals = new ContentValues();
        
        // Insert a random key in the database. This has to provided by the caller when updating or
        // getting the blob
        String password = UUID.randomUUID().toString();
        vals.put(COLUMN_KEY, password);
        
        Uri insertedUri = mDb.insert(vals);
        return Uri.withAppendedPath(insertedUri, password);
    }

    @Override
    public boolean onCreate() {
        if(ApgService.LOCAL_LOGD) Log.d(TAG, "onCreate() called");
        mDb = new ApgServiceBlobDatabase(getContext());
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public Cursor query(Uri arg0, String[] arg1, String arg2, String[] arg3, String arg4) {
        if(ApgService.LOCAL_LOGD) Log.d(TAG, "query() called");
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int update(Uri arg0, ContentValues arg1, String arg2, String[] arg3) {
        if(ApgService.LOCAL_LOGD) Log.d(TAG, "update() called");
        // TODO Auto-generated method stub
        return 0;
    }
    
    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws SecurityException, FileNotFoundException {
        if(ApgService.LOCAL_LOGD) Log.d(TAG, "openFile() called");
        if(ApgService.LOCAL_LOGD) Log.d(TAG, "... with uri: "+uri.toString());
        if(ApgService.LOCAL_LOGD) Log.d(TAG, "... with mode: "+mode);
        
        List<String> segments = uri.getPathSegments();
        if(segments.size() < 2) {
            throw new SecurityException("Password not found in URI");
        }
        String id = segments.get(0);
        String key = segments.get(1);
        
        if(ApgService.LOCAL_LOGD) Log.d(TAG, "... got id: "+id);
        if(ApgService.LOCAL_LOGD) Log.d(TAG, "... and key: "+key);

        // get the data
        Cursor result = mDb.query(id, key);
        
        if(result.getCount() == 0) {
            // either the key is wrong or no id exists
            throw new FileNotFoundException("No file found with that ID and/or password");
        }
        
        File targetFile = new File(STORE_PATH, id);
        if(mode.equals("w")) {
            if(ApgService.LOCAL_LOGD) Log.d(TAG, "... will try to open file w");
            if( !targetFile.exists() ) {
                try {
                    targetFile.createNewFile();
                } catch (IOException e) {
                    Log.e(TAG, "... got IEOException on creating new file", e);
                    throw new FileNotFoundException("Could not create file to write to");
                }
            }
            return ParcelFileDescriptor.open(targetFile, ParcelFileDescriptor.MODE_WRITE_ONLY | ParcelFileDescriptor.MODE_TRUNCATE );
        } else if(mode.equals("r")) {
            if(ApgService.LOCAL_LOGD) Log.d(TAG, "... will try to open file r");
            if( !targetFile.exists() ) {
                throw new FileNotFoundException("Error: Could not find the file requested");
            }
            return ParcelFileDescriptor.open(targetFile, ParcelFileDescriptor.MODE_READ_ONLY);
        }

        return null;
    }

}
