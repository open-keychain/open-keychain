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


import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.support.annotation.Nullable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.format.DateFormat;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.model.SubKey;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel.SubkeyChange;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;

public class SubkeysAdapter extends BaseAdapter {
    private final Context context;
    private final LayoutInflater layoutInflater;

    private List<SubKey> data;
    private SaveKeyringParcel.Builder mSkpBuilder;

    private ColorStateList mDefaultTextColor;

    public SubkeysAdapter(Context context) {
        this.context = context;
        layoutInflater = LayoutInflater.from(context);
    }

    public void setData(List<SubKey> data) {
        this.data = data;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return data != null ? data.size() : 0;
    }

    @Override
    public SubKey getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return data.get(position).key_id();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view;
        if (convertView != null) {
            view = convertView;
        } else {
            view = layoutInflater.inflate(R.layout.view_key_adv_subkey_item, parent, false);
        }

        if (mDefaultTextColor == null) {
            TextView keyId = view.findViewById(R.id.subkey_item_key_id);
            mDefaultTextColor = keyId.getTextColors();
        }

        TextView vKeyId = view.findViewById(R.id.subkey_item_key_id);
        TextView vKeyDetails = view.findViewById(R.id.subkey_item_details);
        TextView vKeyExpiry = view.findViewById(R.id.subkey_item_expiry);
        ImageView vCertifyIcon = view.findViewById(R.id.subkey_item_ic_certify);
        ImageView vSignIcon = view.findViewById(R.id.subkey_item_ic_sign);
        ImageView vEncryptIcon = view.findViewById(R.id.subkey_item_ic_encrypt);
        ImageView vAuthenticateIcon = view.findViewById(R.id.subkey_item_ic_authenticate);
        ImageView vEditImage = view.findViewById(R.id.subkey_item_edit_image);
        ImageView vStatus = view.findViewById(R.id.subkey_item_status);

        // not used:
        ImageView deleteImage = view.findViewById(R.id.subkey_item_delete_button);
        deleteImage.setVisibility(View.GONE);

        SubKey subKey = getItem(position);

        vKeyId.setText(KeyFormattingUtils.beautifyKeyId(subKey.key_id()));

        // may be set with additional "stripped" later on
        SpannableStringBuilder algorithmStr = new SpannableStringBuilder();
        algorithmStr.append(KeyFormattingUtils.getAlgorithmInfo(
                context,
                subKey.algorithm(),
                subKey.key_size(),
                subKey.key_curve_oid()
        ));

        SubkeyChange change = mSkpBuilder != null ? mSkpBuilder.getSubkeyChange(subKey.key_id()) : null;
        if (change != null && (change.getDummyStrip() || change.getMoveKeyToSecurityToken())) {
            if (change.getDummyStrip()) {
                algorithmStr.append(", ");
                final SpannableString boldStripped = new SpannableString(
                        context.getString(R.string.key_stripped)
                );
                boldStripped.setSpan(new StyleSpan(Typeface.BOLD), 0, boldStripped.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                algorithmStr.append(boldStripped);
            }
            if (change.getMoveKeyToSecurityToken()) {
                algorithmStr.append(", ");
                final SpannableString boldDivert = new SpannableString(
                        context.getString(R.string.key_divert)
                );
                boldDivert.setSpan(new StyleSpan(Typeface.BOLD), 0, boldDivert.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                algorithmStr.append(boldDivert);
            }
        } else {
            switch (subKey.has_secret()) {
                case GNU_DUMMY:
                    algorithmStr.append(", ");
                    algorithmStr.append(context.getString(R.string.key_stripped));
                    break;
                case DIVERT_TO_CARD:
                    algorithmStr.append(", ");
                    algorithmStr.append(context.getString(R.string.key_divert));
                    break;
                case PASSPHRASE_EMPTY:
                    algorithmStr.append(", ");
                    algorithmStr.append(context.getString(R.string.key_no_passphrase));
                    break;
                case UNAVAILABLE:
                    // don't show this on pub keys
                    //algorithmStr += ", " + context.getString(R.string.key_unavailable);
                    break;
            }
        }
        vKeyDetails.setText(algorithmStr, TextView.BufferType.SPANNABLE);

        boolean isMasterKey = subKey.rank() == 0;
        if (isMasterKey) {
            vKeyId.setTypeface(null, Typeface.BOLD);
        } else {
            vKeyId.setTypeface(null, Typeface.NORMAL);
        }

        // Set icons according to properties
        vCertifyIcon.setVisibility(subKey.can_certify() ? View.VISIBLE : View.GONE);
        vEncryptIcon.setVisibility(subKey.can_encrypt() ? View.VISIBLE : View.GONE);
        vSignIcon.setVisibility(subKey.can_sign() ? View.VISIBLE : View.GONE);
        vAuthenticateIcon.setVisibility(subKey.can_authenticate() ? View.VISIBLE : View.GONE);

        boolean isRevoked = subKey.is_revoked();

        Date expiryDate = null;
        if (subKey.expires()) {
            expiryDate = new Date(subKey.expiry() * 1000);
        }

        // for edit key
        if (mSkpBuilder != null) {
            boolean revokeThisSubkey = (mSkpBuilder.getMutableRevokeSubKeys().contains(subKey.key_id()));

            if (revokeThisSubkey) {
                if (!isRevoked) {
                    isRevoked = true;
                }
            }

            SaveKeyringParcel.SubkeyChange subkeyChange = mSkpBuilder.getSubkeyChange(subKey.key_id());
            if (subkeyChange != null) {
                if (subkeyChange.getExpiry() == null || subkeyChange.getExpiry() == 0L) {
                    expiryDate = null;
                } else {
                    expiryDate = new Date(subkeyChange.getExpiry() * 1000);
                }
            }

            vEditImage.setVisibility(View.VISIBLE);
        } else {
            vEditImage.setVisibility(View.GONE);
        }

        boolean isExpired;
        if (expiryDate != null) {
            isExpired = expiryDate.before(new Date());
            Calendar expiryCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            expiryCal.setTime(expiryDate);
            // convert from UTC to time zone of device
            expiryCal.setTimeZone(TimeZone.getDefault());

            vKeyExpiry.setText(context.getString(R.string.label_expiry) + ": "
                    + DateFormat.getDateFormat(context).format(expiryCal.getTime()));
        } else {
            isExpired = false;

            vKeyExpiry.setText(context.getString(R.string.label_expiry) + ": " + context.getString(R.string.none));
        }

        // if key is expired or revoked...
        boolean isInvalid = isRevoked || isExpired || !subKey.is_secure();
        if (isInvalid) {
            vStatus.setVisibility(View.VISIBLE);

            vCertifyIcon.setColorFilter(
                    context.getResources().getColor(R.color.key_flag_gray),
                    PorterDuff.Mode.SRC_IN);
            vSignIcon.setColorFilter(
                    context.getResources().getColor(R.color.key_flag_gray),
                    PorterDuff.Mode.SRC_IN);
            vEncryptIcon.setColorFilter(
                    context.getResources().getColor(R.color.key_flag_gray),
                    PorterDuff.Mode.SRC_IN);
            vAuthenticateIcon.setColorFilter(
                    context.getResources().getColor(R.color.key_flag_gray),
                    PorterDuff.Mode.SRC_IN);

            if (isRevoked) {
                vStatus.setImageResource(R.drawable.status_signature_revoked_cutout_24dp);
                vStatus.setColorFilter(
                        context.getResources().getColor(R.color.key_flag_gray),
                        PorterDuff.Mode.SRC_IN);
            } else if (isExpired) {
                vStatus.setImageResource(R.drawable.status_signature_expired_cutout_24dp);
                vStatus.setColorFilter(
                        context.getResources().getColor(R.color.key_flag_gray),
                        PorterDuff.Mode.SRC_IN);
            } else if (!subKey.is_secure()) {
                vStatus.setImageResource(R.drawable.status_signature_invalid_cutout_24dp);
                vStatus.setColorFilter(
                        context.getResources().getColor(R.color.key_flag_gray),
                        PorterDuff.Mode.SRC_IN);
            }
        } else {
            vStatus.setVisibility(View.GONE);

            vKeyId.setTextColor(mDefaultTextColor);
            vKeyDetails.setTextColor(mDefaultTextColor);
            vKeyExpiry.setTextColor(mDefaultTextColor);

            vCertifyIcon.clearColorFilter();
            vSignIcon.clearColorFilter();
            vEncryptIcon.clearColorFilter();
            vAuthenticateIcon.clearColorFilter();
        }
        vKeyId.setEnabled(!isInvalid);
        vKeyDetails.setEnabled(!isInvalid);
        vKeyExpiry.setEnabled(!isInvalid);

        return view;
    }

    // Disable selection of items, http://stackoverflow.com/a/4075045
    @Override
    public boolean areAllItemsEnabled() {
        return mSkpBuilder != null && super.areAllItemsEnabled();
    }

    // Disable selection of items, http://stackoverflow.com/a/4075045
    @Override
    public boolean isEnabled(int position) {
        return mSkpBuilder != null && super.isEnabled(position);
    }

    /** Set this adapter into edit mode. This mode displays additional info for
     * each item from a supplied SaveKeyringParcel reference.
     *
     * Note that it is up to the caller to reload the underlying cursor after
     * updating the SaveKeyringParcel!
     *
     * @see SaveKeyringParcel
     *
     * @param builder The parcel to get info from, or null to leave edit mode.
     */
    public void setEditMode(@Nullable SaveKeyringParcel.Builder builder) {
        mSkpBuilder = builder;
        notifyDataSetChanged();
    }

}
