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
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.helper.OtherHelper;
import org.sufficientlysecure.keychain.pgp.KeyRing;
import org.sufficientlysecure.keychain.provider.KeychainContract.Certs;
import org.sufficientlysecure.keychain.provider.KeychainContract.UserIds;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel;

import java.util.ArrayList;

public class UserIdsAdapter extends CursorAdapter implements AdapterView.OnItemClickListener {
    private LayoutInflater mInflater;

    private int mIndexUserId, mIndexRank;
    private int mVerifiedId, mIsRevoked, mIsPrimary;

    private final ArrayList<Boolean> mCheckStates;

    private SaveKeyringParcel mSaveKeyringParcel;

    public static final String[] USER_IDS_PROJECTION = new String[]{
            UserIds._ID,
            UserIds.USER_ID,
            UserIds.RANK,
            UserIds.VERIFIED,
            UserIds.IS_PRIMARY,
            UserIds.IS_REVOKED
    };

    public UserIdsAdapter(Context context, Cursor c, int flags, boolean showCheckBoxes,
                          SaveKeyringParcel saveKeyringParcel) {
        super(context, c, flags);
        mInflater = LayoutInflater.from(context);

        mCheckStates = showCheckBoxes ? new ArrayList<Boolean>() : null;
        mSaveKeyringParcel = saveKeyringParcel;

        initIndex(c);
    }

    public UserIdsAdapter(Context context, Cursor c, int flags, boolean showCheckBoxes) {
        this(context, c, flags, showCheckBoxes, null);
    }

    public UserIdsAdapter(Context context, Cursor c, int flags, SaveKeyringParcel saveKeyringParcel) {
        this(context, c, flags, false, saveKeyringParcel);
    }

    public UserIdsAdapter(Context context, Cursor c, int flags) {
        this(context, c, flags, false, null);
    }

    @Override
    public Cursor swapCursor(Cursor newCursor) {
        initIndex(newCursor);
        if (mCheckStates != null) {
            mCheckStates.clear();
            if (newCursor != null) {
                int count = newCursor.getCount();
                mCheckStates.ensureCapacity(count);
                // initialize to true (use case knowledge: we usually want to sign all uids)
                for (int i = 0; i < count; i++) {
                    newCursor.moveToPosition(i);
                    int verified = newCursor.getInt(mVerifiedId);
                    mCheckStates.add(verified != Certs.VERIFIED_SECRET);
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
            mVerifiedId = cursor.getColumnIndexOrThrow(UserIds.VERIFIED);
            mIsRevoked = cursor.getColumnIndexOrThrow(UserIds.IS_REVOKED);
            mIsPrimary = cursor.getColumnIndexOrThrow(UserIds.IS_PRIMARY);
        }
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        TextView vName = (TextView) view.findViewById(R.id.userId);
        TextView vAddress = (TextView) view.findViewById(R.id.address);
        TextView vComment = (TextView) view.findViewById(R.id.comment);
        ImageView vVerified = (ImageView) view.findViewById(R.id.certified);
        ImageView vHasChanges = (ImageView) view.findViewById(R.id.has_changes);

        String userId = cursor.getString(mIndexUserId);
        String[] splitUserId = KeyRing.splitUserId(userId);
        if (splitUserId[0] != null) {
            vName.setText(splitUserId[0]);
        } else {
            vName.setText(R.string.user_id_no_name);
        }
        if (splitUserId[1] != null) {
            vAddress.setText(splitUserId[1]);
            vAddress.setVisibility(View.VISIBLE);
        } else {
            vAddress.setVisibility(View.GONE);
        }
        if (splitUserId[2] != null) {
            vComment.setText(splitUserId[2]);
            vComment.setVisibility(View.VISIBLE);
        } else {
            vComment.setVisibility(View.GONE);
        }

        // show small star icon for primary user ids
        boolean isPrimary = cursor.getInt(mIsPrimary) != 0;
        boolean isRevoked = cursor.getInt(mIsRevoked) > 0;

        // for edit key
        if (mSaveKeyringParcel != null) {
            boolean changeUserId = (mSaveKeyringParcel.changePrimaryUserId != null
                    && mSaveKeyringParcel.changePrimaryUserId.equals(userId));
            boolean revoke = (mSaveKeyringParcel.revokeUserIds.contains(userId));

            if (changeUserId) {
                isPrimary = !isPrimary;
            }
            if (revoke) {
                if (!isRevoked) {
                    isRevoked = true;
                }
            }

            if (changeUserId || revoke) {
                vHasChanges.setVisibility(View.VISIBLE);
            } else {
                vHasChanges.setVisibility(View.GONE);
            }
        } else {
            vHasChanges.setVisibility(View.GONE);
        }

        if (isRevoked) {
            // set revocation icon (can this even be primary?)
            vVerified.setImageResource(R.drawable.key_certify_revoke);

            // disable and strike through text for revoked user ids
            vName.setEnabled(false);
            vAddress.setEnabled(false);
            vName.setText(OtherHelper.strikeOutText(vName.getText()));
            vAddress.setText(OtherHelper.strikeOutText(vAddress.getText()));
        } else {
            vName.setEnabled(true);
            vAddress.setEnabled(true);

            int verified = cursor.getInt(mVerifiedId);
            switch (verified) {
                case Certs.VERIFIED_SECRET:
                    vVerified.setImageResource(isPrimary
                            ? R.drawable.key_certify_primary_ok_depth0
                            : R.drawable.key_certify_ok_depth0);
                    break;
                case Certs.VERIFIED_SELF:
                    vVerified.setImageResource(isPrimary
                            ? R.drawable.key_certify_primary_ok_self
                            : R.drawable.key_certify_ok_self);
                    break;
                default:
                    vVerified.setImageResource(R.drawable.key_certify_error);
                    break;
            }
        }

        // don't care further if checkboxes aren't shown
        if (mCheckStates == null) {
            return;
        }

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
        vCheckBox.setClickable(false);
    }

    public void onItemClick(AdapterView<?> adapter, View view, int position, long id) {
        CheckBox box = ((CheckBox) view.findViewById(R.id.checkBox));
        if (box != null) {
            box.toggle();
        }
    }

    public ArrayList<String> getSelectedUserIds() {
        ArrayList<String> result = new ArrayList<String>();
        for (int i = 0; i < mCheckStates.size(); i++) {
            if (mCheckStates.get(i)) {
                mCursor.moveToPosition(i);
                result.add(mCursor.getString(mIndexUserId));
            }
        }
        return result;
    }

    public String getUserId(int position) {
        mCursor.moveToPosition(position);
        return mCursor.getString(mIndexUserId);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = mInflater.inflate(R.layout.view_key_userids_item, null);
        // only need to do this once ever, since mShowCheckBoxes is final
        view.findViewById(R.id.checkBox).setVisibility(mCheckStates != null ? View.VISIBLE : View.GONE);
        return view;
    }

    // Disable selection of items for lists without checkboxes, http://stackoverflow.com/a/4075045
    @Override
    public boolean areAllItemsEnabled() {
        if (mCheckStates == null && mSaveKeyringParcel == null) {
            return false;
        } else {
            return super.areAllItemsEnabled();
        }
    }

    // Disable selection of items for lists without checkboxes, http://stackoverflow.com/a/4075045
    @Override
    public boolean isEnabled(int position) {
        if (mCheckStates == null && mSaveKeyringParcel == null) {
            return false;
        } else {
            return super.isEnabled(position);
        }
    }

}
