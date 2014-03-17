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

package org.sufficientlysecure.keychain.ui.adapter;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.PgpKeyHelper;
import org.sufficientlysecure.keychain.provider.KeychainContract.UserIds;

import java.util.ArrayList;

public class ViewKeyUserIdsAdapter extends CursorAdapter {
    private LayoutInflater mInflater;

    private int mIndexUserId, mIndexRank;
    private int mVerifiedId;

    final private ArrayList<Boolean> mCheckStates;

    public ViewKeyUserIdsAdapter(Context context, Cursor c, int flags, boolean showCheckBoxes) {
        super(context, c, flags);

        mInflater = LayoutInflater.from(context);

        mCheckStates = showCheckBoxes ? new ArrayList<Boolean>() : null;

        initIndex(c);
    }
    public ViewKeyUserIdsAdapter(Context context, Cursor c, int flags) {
        this(context, c, flags, false);
    }

    @Override
    public Cursor swapCursor(Cursor newCursor) {
        initIndex(newCursor);
        if(mCheckStates != null) {
            mCheckStates.clear();
            if(newCursor != null) {
                int count = newCursor.getCount();
                mCheckStates.ensureCapacity(count);
                // initialize to true (use case knowledge: we usually want to sign all uids)
                for(int i = 0; i < count; i++) {
                    newCursor.moveToPosition(i);
                    int verified = newCursor.getInt(mVerifiedId);
                    mCheckStates.add(verified == 0);
                }
            }
        }

        return super.swapCursor(newCursor);
    }

    /**
     * Get column indexes for performance reasons just once in constructor and swapCursor. For a
     * performance comparison see http://stackoverflow.com/a/17999582
     *
     * @param cursor
     */
    private void initIndex(Cursor cursor) {
        if (cursor != null) {
            mIndexUserId = cursor.getColumnIndexOrThrow(UserIds.USER_ID);
            mIndexRank = cursor.getColumnIndexOrThrow(UserIds.RANK);
            mVerifiedId = cursor.getColumnIndexOrThrow("verified");
        }
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        TextView vRank = (TextView) view.findViewById(R.id.rank);
        TextView vUserId = (TextView) view.findViewById(R.id.userId);
        TextView vAddress = (TextView) view.findViewById(R.id.address);
        ImageView vVerified = (ImageView) view.findViewById(R.id.certified);

        vRank.setText(Integer.toString(cursor.getInt(mIndexRank)));

        String[] userId = PgpKeyHelper.splitUserId(cursor.getString(mIndexUserId));
        if (userId[0] != null) {
            vUserId.setText(userId[0]);
        } else {
            vUserId.setText(R.string.user_id_no_name);
        }
        vAddress.setText(userId[1]);

        int verified = cursor.getInt(mVerifiedId);
        // TODO introduce own resource for this :)
        if(verified > 0)
            vVerified.setImageResource(android.R.drawable.presence_online);
        else
            vVerified.setImageResource(android.R.drawable.presence_invisible);

        // don't care further if checkboxes aren't shown
        if(mCheckStates == null)
            return;

        final CheckBox vCheckBox = (CheckBox) view.findViewById(R.id.checkBox);
        final int position = cursor.getPosition();
        vCheckBox.setOnCheckedChangeListener(null);
        vCheckBox.setChecked(mCheckStates.get(position));
        vCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                mCheckStates.set(position, b);
            }
        });
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                vCheckBox.toggle();
            }
        });

    }

    public ArrayList<String> getSelectedUserIds() {
        ArrayList<String> result = new ArrayList<String>();
        for(int i = 0; i < mCheckStates.size(); i++) {
            if(mCheckStates.get(i)) {
                mCursor.moveToPosition(i);
                result.add(mCursor.getString(mIndexUserId));
            }
        }
        return result;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = mInflater.inflate(R.layout.view_key_userids_item, null);
        // only need to do this once ever, since mShowCheckBoxes is final
        view.findViewById(R.id.checkBox).setVisibility(mCheckStates != null ? View.VISIBLE : View.GONE);
        return view;
    }

}
