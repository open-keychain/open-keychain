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

package org.sufficientlysecure.keychain.ui.adapter;


import java.util.List;

import android.content.Context;
import android.graphics.Typeface;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewAnimator;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.model.UserPacket.UserId;
import org.sufficientlysecure.keychain.pgp.CanonicalizedKeyRing.VerificationStatus;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils.State;

// TODO move to RecyclerView
public class UserIdsAdapter extends BaseAdapter {
    private Context context;
    private List<UserId> data;
    private SaveKeyringParcel.Builder mSkpBuilder;
    private boolean mShowStatusImages;
    private LayoutInflater layoutInflater;

    public UserIdsAdapter(Context context, boolean showStatusImages) {
        super();

        this.context = context;
        this.layoutInflater = LayoutInflater.from(context);
        mShowStatusImages = showStatusImages;
    }

    @Override
    public int getCount() {
        return data != null ? data.size() : 0;
    }

    @Override
    public UserId getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return data.get(position).master_key_id();
    }

    public void setData(List<UserId> data) {
        this.data = data;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view;
        if (convertView != null) {
            view = convertView;
        } else {
            view = layoutInflater.inflate(R.layout.view_key_adv_user_id_item, parent, false);
        }

        TextView vName = view.findViewById(R.id.user_id_item_name);
        TextView vAddress = view.findViewById(R.id.user_id_item_address);
        TextView vComment = view.findViewById(R.id.user_id_item_comment);
        ImageView vVerified = view.findViewById(R.id.user_id_item_certified);
        ViewAnimator vVerifiedLayout = view.findViewById(R.id.user_id_icon_animator);

        ImageView vDeleteButton = view.findViewById(R.id.user_id_item_delete_button);
        vDeleteButton.setVisibility(View.GONE); // not used

        UserId userId = getItem(position);

        if (userId.name() != null) {
            vName.setText(userId.name());
        } else {
            vName.setText(R.string.user_id_no_name);
        }
        if (userId.email() != null) {
            vAddress.setText(userId.email());
            vAddress.setVisibility(View.VISIBLE);
        } else {
            vAddress.setVisibility(View.GONE);
        }
        if (userId.comment() != null) {
            vComment.setText(userId.comment());
            vComment.setVisibility(View.VISIBLE);
        } else {
            vComment.setVisibility(View.GONE);
        }

        boolean isPrimary = userId.is_primary();
        boolean isRevoked = userId.is_revoked();

        // for edit key
        if (mSkpBuilder != null) {
            String changePrimaryUserId = mSkpBuilder.getChangePrimaryUserId();
            boolean changeAnyPrimaryUserId = (changePrimaryUserId != null);
            boolean changeThisPrimaryUserId = (changeAnyPrimaryUserId && changePrimaryUserId.equals(userId.user_id()));
            boolean revokeThisUserId = (mSkpBuilder.getMutableRevokeUserIds().contains(userId.user_id()));

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

            vVerifiedLayout.setDisplayedChild(2);
        } else {
            vVerifiedLayout.setDisplayedChild(mShowStatusImages ? 1 : 0);
        }

        if (isRevoked) {
            // set revocation icon (can this even be primary?)
            KeyFormattingUtils.setStatusImage(context, vVerified, null, State.REVOKED, R.color.key_flag_gray);

            // disable revoked user ids
            vName.setEnabled(false);
            vAddress.setEnabled(false);
            vComment.setEnabled(false);
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

            VerificationStatus isVerified = getVerificationStatus(position);
            switch (isVerified) {
                case VERIFIED_SECRET:
                    KeyFormattingUtils.setStatusImage(context, vVerified, null, State.VERIFIED, KeyFormattingUtils.DEFAULT_COLOR);
                    break;
                case VERIFIED_SELF:
                    KeyFormattingUtils.setStatusImage(context, vVerified, null, State.UNVERIFIED, KeyFormattingUtils.DEFAULT_COLOR);
                    break;
                default:
                    KeyFormattingUtils.setStatusImage(context, vVerified, null, State.INVALID, KeyFormattingUtils.DEFAULT_COLOR);
                    break;
            }
        }

        return view;
    }

    public boolean getIsRevokedPending(int position) {
        String userId = getUserId(position);

        boolean isRevokedPending = false;
        if (mSkpBuilder != null) {
            if (mSkpBuilder.getMutableRevokeUserIds().contains(userId)) {
                isRevokedPending = true;
            }

        }

        return isRevokedPending;
    }

    /** Set this adapter into edit mode. This mode displays additional info for
     * each item from a supplied SaveKeyringParcel reference.
     *
     * Note that it is up to the caller to reload the underlying cursor after
     * updating the SaveKeyringParcel!
     *
     * @see SaveKeyringParcel
     *
     * @param saveKeyringParcel The parcel to get info from, or null to leave edit mode.
     */
    public void setEditMode(@Nullable SaveKeyringParcel.Builder saveKeyringParcel) {
        mSkpBuilder = saveKeyringParcel;
    }

    public String getUserId(int position) {
        return data.get(position).user_id();
    }

    public boolean getIsRevoked(int position) {
        return data.get(position).is_revoked();
    }

    public VerificationStatus getVerificationStatus(int position) {
        return data.get(position).verified();
    }
}
