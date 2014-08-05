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
import android.graphics.PorterDuff;
import android.graphics.Typeface;
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
    private static final int INDEX_ID = 0;
    private static final int INDEX_USER_ID = 1;
    private static final int INDEX_RANK = 2;
    private static final int INDEX_VERIFIED = 3;
    private static final int INDEX_IS_PRIMARY = 4;
    private static final int INDEX_IS_REVOKED = 5;

    public UserIdsAdapter(Context context, Cursor c, int flags, boolean showCheckBoxes,
                          SaveKeyringParcel saveKeyringParcel) {
        super(context, c, flags);
        mInflater = LayoutInflater.from(context);

        mCheckStates = showCheckBoxes ? new ArrayList<Boolean>() : null;
        mSaveKeyringParcel = saveKeyringParcel;
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
        if (mCheckStates != null) {
            mCheckStates.clear();
            if (newCursor != null) {
                int count = newCursor.getCount();
                mCheckStates.ensureCapacity(count);
                // initialize to true (use case knowledge: we usually want to sign all uids)
                for (int i = 0; i < count; i++) {
                    newCursor.moveToPosition(i);
                    int verified = newCursor.getInt(INDEX_VERIFIED);
                    mCheckStates.add(verified != Certs.VERIFIED_SECRET);
                }
            }
        }

        return super.swapCursor(newCursor);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        TextView vName = (TextView) view.findViewById(R.id.user_id_item_name);
        TextView vAddress = (TextView) view.findViewById(R.id.user_id_item_address);
        TextView vComment = (TextView) view.findViewById(R.id.user_id_item_comment);
        ImageView vVerified = (ImageView) view.findViewById(R.id.user_id_item_certified);
        View vVerifiedLayout = view.findViewById(R.id.user_id_item_certified_layout);
        ImageView vEditImage = (ImageView) view.findViewById(R.id.user_id_item_edit_image);
        ImageView vDeleteButton = (ImageView) view.findViewById(R.id.user_id_item_delete_button);
        vDeleteButton.setVisibility(View.GONE); // not used

        String userId = cursor.getString(INDEX_USER_ID);
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

        boolean isPrimary = cursor.getInt(INDEX_IS_PRIMARY) != 0;
        boolean isRevoked = cursor.getInt(INDEX_IS_REVOKED) > 0;

        // for edit key
        if (mSaveKeyringParcel != null) {
            boolean changeAnyPrimaryUserId = (mSaveKeyringParcel.mChangePrimaryUserId != null);
            boolean changeThisPrimaryUserId = (mSaveKeyringParcel.mChangePrimaryUserId != null
                    && mSaveKeyringParcel.mChangePrimaryUserId.equals(userId));
            boolean revokeThisUserId = (mSaveKeyringParcel.mRevokeUserIds.contains(userId));

            // only if primary user id will be changed
            // (this is not triggered if the user id is currently the primary one)
            if (changeAnyPrimaryUserId) {
                // change _all_ primary user ids and set new one to true
                isPrimary = changeThisPrimaryUserId;
            }

            if (revokeThisUserId) {
                if (!isRevoked) {
                    isRevoked = true;
                }
            }

            vEditImage.setVisibility(View.VISIBLE);
            vVerifiedLayout.setVisibility(View.GONE);
        } else {
            vEditImage.setVisibility(View.GONE);
            vVerifiedLayout.setVisibility(View.VISIBLE);
        }

        if (isRevoked) {
            // set revocation icon (can this even be primary?)
            vVerified.setImageResource(R.drawable.status_signature_revoked_cutout);
            vVerified.setColorFilter(
                    mContext.getResources().getColor(R.color.bg_gray),
                    PorterDuff.Mode.SRC_IN);

            // disable and strike through text for revoked user ids
            vName.setEnabled(false);
            vAddress.setEnabled(false);
            vComment.setEnabled(false);
            vName.setText(OtherHelper.strikeOutText(vName.getText()));
            vAddress.setText(OtherHelper.strikeOutText(vAddress.getText()));
            vComment.setText(OtherHelper.strikeOutText(vComment.getText()));
        } else {
            vName.setEnabled(true);
            vAddress.setEnabled(true);
            vComment.setEnabled(true);

            if (isPrimary) {
                vName.setTypeface(null, Typeface.BOLD);
                vAddress.setTypeface(null, Typeface.BOLD);
            } else {
                vName.setTypeface(null, Typeface.NORMAL);
                vAddress.setTypeface(null, Typeface.NORMAL);
            }

            int isVerified = cursor.getInt(INDEX_VERIFIED);
            switch (isVerified) {
                case Certs.VERIFIED_SECRET:
                    vVerified.setImageResource(R.drawable.status_signature_verified_cutout);
                    vVerified.setColorFilter(
                            mContext.getResources().getColor(R.color.android_green_dark),
                            PorterDuff.Mode.SRC_IN);
                    break;
                case Certs.VERIFIED_SELF:
                    vVerified.setImageResource(R.drawable.status_signature_unverified_cutout);
                    vVerified.setColorFilter(
                            mContext.getResources().getColor(R.color.bg_gray),
                            PorterDuff.Mode.SRC_IN);
                    break;
                default:
                    vVerified.setImageResource(R.drawable.status_signature_invalid_cutout);
                    vVerified.setColorFilter(
                            mContext.getResources().getColor(R.color.android_red_dark),
                            PorterDuff.Mode.SRC_IN);
                    break;
            }
        }

        // don't care further if checkboxes aren't shown
        if (mCheckStates == null) {
            return;
        }

        final CheckBox vCheckBox = (CheckBox) view.findViewById(R.id.user_id_item_check_box);
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
        CheckBox box = ((CheckBox) view.findViewById(R.id.user_id_item_check_box));
        if (box != null) {
            box.toggle();
        }
    }

    public ArrayList<String> getSelectedUserIds() {
        ArrayList<String> result = new ArrayList<String>();
        for (int i = 0; i < mCheckStates.size(); i++) {
            if (mCheckStates.get(i)) {
                mCursor.moveToPosition(i);
                result.add(mCursor.getString(INDEX_USER_ID));
            }
        }
        return result;
    }

    public String getUserId(int position) {
        mCursor.moveToPosition(position);
        return mCursor.getString(INDEX_USER_ID);
    }

    public boolean getIsRevoked(int position) {
        mCursor.moveToPosition(position);
        return mCursor.getInt(INDEX_IS_REVOKED) > 0;
    }

    public boolean getIsRevokedPending(int position) {
        mCursor.moveToPosition(position);
        String userId = mCursor.getString(INDEX_USER_ID);

        boolean isRevokedPending = false;
        if (mSaveKeyringParcel != null) {
            if (mSaveKeyringParcel.mRevokeUserIds.contains(userId)) {
                isRevokedPending = true;
            }

        }

        return isRevokedPending;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = mInflater.inflate(R.layout.view_key_user_id_item, null);
        // only need to do this once ever, since mShowCheckBoxes is final
        view.findViewById(R.id.user_id_item_check_box).setVisibility(mCheckStates != null ? View.VISIBLE : View.GONE);
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
