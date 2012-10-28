/*
 * Copyright (C) 2012 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.thialfihar.android.apg.ui.widget;

import org.thialfihar.android.apg.Id;
import org.thialfihar.android.apg.R;
import org.thialfihar.android.apg.helper.OtherHelper;
import org.thialfihar.android.apg.helper.PGPHelper;
import org.thialfihar.android.apg.provider.ApgContract.PublicKeys;
import org.thialfihar.android.apg.provider.ApgContract.PublicUserIds;
import org.thialfihar.android.apg.provider.ApgContract.SecretKeys;
import org.thialfihar.android.apg.provider.ApgContract.SecretUserIds;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleCursorTreeAdapter;
import android.widget.TextView;

public class KeyListAdapter extends SimpleCursorTreeAdapter implements
        LoaderManager.LoaderCallbacks<Cursor> {
    private Context mContext;
    private LoaderManager mManager;
    private LayoutInflater mInflater;

    // Id.type.public_key / Id.type.secret_key
    protected int mKeyType;

    public KeyListAdapter(Context context, LoaderManager manager, Cursor groupCursor, int keyType) {
        super(context, groupCursor, -1, null, null, -1, null, null);
        mContext = context;
        mManager = manager;
        mInflater = LayoutInflater.from(context);
        mKeyType = keyType;
    }

    /**
     * Inflate new view for group items
     */
    @Override
    public View newGroupView(Context context, Cursor cursor, boolean isExpanded, ViewGroup parent) {
        return mInflater.inflate(R.layout.key_list_group_item, null);
    }

    /**
     * Binds TextViews from view to results from database group cursor.
     */
    @Override
    protected void bindGroupView(View view, Context context, Cursor cursor, boolean isExpanded) {
        int userIdIndex;
        if (mKeyType == Id.type.public_key) {
            userIdIndex = cursor.getColumnIndex(PublicUserIds.USER_ID);
        } else {
            userIdIndex = cursor.getColumnIndex(SecretUserIds.USER_ID);
        }

        TextView mainUserId = (TextView) view.findViewById(R.id.mainUserId);
        mainUserId.setText(R.string.unknownUserId);
        TextView mainUserIdRest = (TextView) view.findViewById(R.id.mainUserIdRest);
        mainUserIdRest.setText("");

        String userId = cursor.getString(userIdIndex);
        if (userId != null) {
            String[] userIdSplit = OtherHelper.splitUserId(userId);

            if (userIdSplit[1] != null) {
                mainUserIdRest.setText(userIdSplit[1]);
            }
            mainUserId.setText(userIdSplit[0]);
        }

        if (mainUserId.getText().length() == 0) {
            mainUserId.setText(R.string.unknownUserId);
        }

        if (mainUserIdRest.getText().length() == 0) {
            mainUserIdRest.setVisibility(View.GONE);
        }
    }

    /**
     * Inflate new view for child items
     */
    @Override
    public View newChildView(Context context, Cursor cursor, boolean isLastChild, ViewGroup parent) {

        return mInflater.inflate(R.layout.key_list_child_item_master_key, null);
    }

    @Override
    protected void bindChildView(View view, Context context, Cursor cursor, boolean isLastChild) {
        int keyIdIndex;
        if (mKeyType == Id.type.public_key) {
            keyIdIndex = cursor.getColumnIndex(PublicKeys.KEY_ID);
        } else {
            keyIdIndex = cursor.getColumnIndex(SecretKeys.KEY_ID);
        }

        TextView keyId = (TextView) view.findViewById(R.id.keyId);
        String keyIdStr = PGPHelper.getSmallFingerPrint(cursor.getLong(keyIdIndex));
        keyId.setText(keyIdStr);
    }

    /**
     * Given the group, we return a cursor for all the children within that group
     */
    @Override
    protected Cursor getChildrenCursor(Cursor groupCursor) {
        final long idGroup = groupCursor.getLong(groupCursor.getColumnIndex(BaseColumns._ID));
        Bundle bundle = new Bundle();
        bundle.putLong("idGroup", idGroup);
        int groupPos = groupCursor.getPosition();
        if (mManager.getLoader(groupPos) != null && !mManager.getLoader(groupPos).isReset()) {
            mManager.restartLoader(groupPos, bundle, this);
        } else {
            mManager.initLoader(groupPos, bundle, this);
        }
        return null;

        // OLD CODE:
        // Vector<KeyChild> children = mChildren.get(groupPosition);
        // if (children != null) {
        // return children;
        // }

        // mCursor.moveToPosition(groupPosition);
        // children = new Vector<KeyChild>();
        // Cursor c = mDatabase.query(Keys.TABLE_NAME, new String[] { Keys._ID, // 0
        // Keys.KEY_ID, // 1
        // Keys.IS_MASTER_KEY, // 2
        // Keys.ALGORITHM, // 3
        // Keys.KEY_SIZE, // 4
        // Keys.CAN_SIGN, // 5
        // Keys.CAN_ENCRYPT, // 6
        // }, Keys.KEY_RING_ID + " = ?", new String[] { mCursor.getString(0) }, null, null,
        // Keys.RANK + " ASC");

        // int masterKeyId = -1;
        // long fingerPrintId = -1;
        // for (int i = 0; i < c.getCount(); ++i) {
        // c.moveToPosition(i);
        // children.add(new KeyChild(c.getLong(1), c.getInt(2) == 1, c.getInt(3), c.getInt(4),
        // c.getInt(5) == 1, c.getInt(6) == 1));
        // if (i == 0) {
        // masterKeyId = c.getInt(0);
        // fingerPrintId = c.getLong(1);
        // }
        // }
        // c.close();
        //
        // if (masterKeyId != -1) {
        // children.insertElementAt(
        // new KeyChild(PGPHelper.getFingerPrint(KeyListActivity.this, fingerPrintId),
        // true), 0);
        // c = mDatabase.query(UserIds.TABLE_NAME, new String[] { UserIds.USER_ID, // 0
        // }, UserIds.KEY_ID + " = ? AND " + UserIds.RANK + " > 0", new String[] { ""
        // + masterKeyId }, null, null, UserIds.RANK + " ASC");
        //
        // for (int i = 0; i < c.getCount(); ++i) {
        // c.moveToPosition(i);
        // children.add(new KeyChild(c.getString(0)));
        // }
        // c.close();
        // }

        // mChildren.set(groupPosition, children);
        // return children;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int groupPos, Bundle bundle) {
        long idGroup = bundle.getLong("idGroup");

        Uri uri = null;
        String[] projection = null;
        String orderBy = null;
        if (mKeyType == Id.type.public_key) {
            projection = new String[] { PublicKeys._ID, // 0
                    PublicKeys.KEY_ID, // 1
                    PublicKeys.IS_MASTER_KEY, // 2
                    PublicKeys.ALGORITHM, // 3
                    PublicKeys.KEY_SIZE, // 4
                    PublicKeys.CAN_SIGN, // 5
                    PublicKeys.CAN_ENCRYPT, // 6
            };
            orderBy = PublicKeys.RANK + " ASC";

            uri = PublicKeys.buildPublicKeysUri(String.valueOf(idGroup));
        } else {
            projection = new String[] { SecretKeys._ID, // 0
                    SecretKeys.KEY_ID, // 1
                    SecretKeys.IS_MASTER_KEY, // 2
                    SecretKeys.ALGORITHM, // 3
                    SecretKeys.KEY_SIZE, // 4
                    SecretKeys.CAN_SIGN, // 5
                    SecretKeys.CAN_ENCRYPT, // 6
            };
            orderBy = SecretKeys.RANK + " ASC";
            
            uri = SecretKeys.buildSecretKeysUri(String.valueOf(idGroup));
        }
        return new CursorLoader(mContext, uri, projection, null, null, orderBy);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        setChildrenCursor(loader.getId(), cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }
}